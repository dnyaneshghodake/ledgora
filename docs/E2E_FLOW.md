# Ledgora — End-to-End Flow (E2E)

This document describes the end-to-end operational flow of Ledgora based on the current codebase.

It is intended for developers and testers to understand:

- request lifecycle (UI → controller → service → repositories)
- tenant resolution and isolation
- transaction posting and ledger invariants
- batch lifecycle
- EOD (End-of-Day) and Day Begin ceremonies
- settlement processing

> This project is primarily a **server-rendered JSP UI** with Spring MVC controllers (not a pure REST API). A few JSON endpoints exist for AJAX lookups.

## 1) Runtime: how requests move through the system

### 1.1 HTTP → Spring Security → Controller

Incoming requests pass through the Spring Security filter chain.

Key aspects:

- Form login is used (custom login page + processing URL) (`src/main/java/com/ledgora/config/SecurityConfig.java:76-89`).
- CSRF is enabled using cookie-based CSRF tokens; the H2 console is excluded (`SecurityConfig.java:59-63`).
- Authorization:
  - `/admin/**` requires `ADMIN` (`SecurityConfig.java:69`) and is also protected at controller level via `@PreAuthorize` (`src/main/java/com/ledgora/config/AdminController.java:35-36`).

### 1.2 Tenant context resolution

Tenant context is stored in both:

- **HTTP session** (e.g., `tenantId`, `tenantScope`, `availableTenants`, etc.)
- **ThreadLocal tenant context** via `TenantContextHolder`

Tenant switching:

- Legacy GET tenant switch is blocked: `GET /tenant/switch/{tenantId}` (`src/main/java/com/ledgora/tenant/controller/TenantController.java:32-38`).
- Supported action is POST: `POST /tenant/switch` (`TenantController.java:40-82`).
  - Requires `tenantScope == MULTI` in session (`TenantController.java:44-48`).
  - Validates the requested tenant is in `availableTenants` (`TenantController.java:50-66`).
  - Updates session attributes + ThreadLocal tenant context (`TenantController.java:68-77`).

EOD and batch flows explicitly resolve tenant from ThreadLocal first, then fall back to session, and fail hard if not set (e.g., `EodController.resolveCurrentTenant(...)` in `src/main/java/com/ledgora/eod/controller/EodController.java:130-144`; similar logic in `BatchController.resolveTenantId(...)` `src/main/java/com/ledgora/batch/controller/BatchController.java:131-145`).

## 2) Core banking invariants (non-negotiable)

### 2.1 Ledger immutability

Ledger is the system of record:

- `LedgerJournal` is immutable (`src/main/java/com/ledgora/ledger/entity/LedgerJournal.java:21`).
- `LedgerEntry` is immutable (`src/main/java/com/ledgora/ledger/entity/LedgerEntry.java:31`).

No updates should occur to these rows; corrections should be made using compensating/reversal transactions.

### 2.2 Double-entry correctness

The platform expects that for each posted event:

- SUM(debits) == SUM(credits)

This is illustrated in seed data via `createBalancedJournal(...)` (`src/main/java/com/ledgora/config/DataInitializer.java:851-904`).

### 2.3 Account balance is a cache

`Account.balance` is explicitly described as a performance cache; true balance is derived from the ledger (`src/main/java/com/ledgora/account/entity/Account.java:52-58`).

Operationally, always treat `ledger_entries` aggregation as authoritative.

## 3) Login and onboarding flow

### 3.1 Endpoints

- `GET /` redirects to `/login` (`src/main/java/com/ledgora/auth/controller/AuthController.java:25-28`)
- `GET /login` renders login page (`AuthController.java:30-42`)
- `POST /perform_login` handles authentication (`src/main/java/com/ledgora/config/SecurityConfig.java:76-81`)

### 3.2 Default seeded logins

Seeded in `DataInitializer` (`src/main/java/com/ledgora/config/DataInitializer.java:253-330`). Examples:

- `admin/admin123` (MULTI-tenant)
- `teller1/teller123`
- `maker1/maker123`, `checker1/checker123`

## 4) Account inquiry & AJAX lookup (UI + JSON)

### 4.1 Accounts UI endpoints

From `AccountController` (`src/main/java/com/ledgora/account/controller/AccountController.java`):

- `GET /accounts` list/filter/search (`AccountController.java:57-77`)
- `GET /accounts/create` form (`AccountController.java:79-84`)
- `POST /accounts/create` create (`AccountController.java:86-103`)
- `GET /accounts/{id}` view (includes balances/ownership/liens/recent txns/audit projections) (`AccountController.java:105-220`)
- `GET /accounts/{id}/edit`, `POST /accounts/{id}/edit` (`AccountController.java:222-266`)
- `POST /accounts/{id}/status` status change (`AccountController.java:268-278`)
- Maker-checker:
  - `POST /accounts/{id}/approve` (`AccountController.java:283-292`)
  - `POST /accounts/{id}/reject` (`AccountController.java:297-306`)

### 4.2 JSON endpoints (used by UI)

These are **AJAX** helpers (not public banking APIs):

- `GET /accounts/api/search?q=...` (`AccountController.java:312-328`)
- `GET /accounts/api/lookup?accountNumber=...` (`AccountController.java:334-368`)

The lookup endpoint also calls the CBS balance engine for available balance and lien totals (`AccountController.java:354-366`).

## 5) Transaction posting flow (Target: Transaction → Voucher → Ledger)

The target E2E posting flow is:

```
Transaction (initiated by maker)
    ↓
Voucher (DR leg + CR leg, with voucher_number, linked via transaction_id FK)
    ↓  authorize (maker-checker or system-auto)
LedgerJournal + LedgerEntry (immutable, created on voucher post)
    ↓
Batch (totals updated on post)
    ↓
EOD / Settlement
```

### 5.1 UI endpoints

From `TransactionController` (`src/main/java/com/ledgora/transaction/controller/TransactionController.java`):

- Deposit: `GET /transactions/deposit`, `POST /transactions/deposit`
- Withdraw: `GET /transactions/withdraw`, `POST /transactions/withdraw`
- Transfer: `GET /transactions/transfer`, `POST /transactions/transfer`
- List: `GET /transactions`
- View: `GET /transactions/{id}` (includes ledger entries)

### 5.2 Parameter tamper resistance

Before delegating to service methods, the controller validates:

- transaction type is valid and matches the operation
- amount range and scale (max 2 decimals)
- account number format

See `validateFinancialParams(...)` (`TransactionController.java:171-211`).

### 5.3 Internal posting flow (how vouchers are created)

`TransactionService` delegates to `VoucherService` for all ledger posting. The private helper `postVoucher(...)` in `TransactionService` implements the target flow:

1. **Validate** business rules (hard transaction ceiling, business day OPEN, sufficient funds, freeze level, holiday calendar, idempotency)
   - **Hard ceiling check** — `HardTransactionCeilingService.enforceHardCeiling()` runs BEFORE any persistence. If amount exceeds `hard_transaction_limits.absolute_max_amount` for the tenant+channel, throws `GovernanceException(HARD_LIMIT_EXCEEDED)`. No role can bypass. Violations logged to `audit_logs` with action `HARD_LIMIT_EXCEEDED` and metric `ledgora.hard_limit.blocked` incremented.
   - **Velocity fraud check** — `VelocityFraudEngine.evaluateVelocity()` queries the past 60-minute transaction history for the account. If count or cumulative amount exceeds `velocity_limits` thresholds: blocks transaction, freezes account to `UNDER_REVIEW`, creates `FraudAlert` record, emits `ledgora.velocity.blocked` metric, logs `VELOCITY_BREACH_*` audit event. Limits resolved per-account first, then tenant-wide default.
2. **Create Transaction** row (status = COMPLETED or PENDING_APPROVAL based on approval policy)
3. **Create Voucher pair** (DR leg + CR leg) via `VoucherService.createVoucher(...)`:
   - Each voucher is linked to the transaction via `transaction_id` FK
   - `voucherNumber` is generated: `<TENANT_CODE>-<BRANCH_CODE>-<YYYYMMDD>-<6-digit scroll>`
   - Scroll number is concurrency-safe (PESSIMISTIC_WRITE lock on `scroll_sequences`)
   - `totalDebit` / `totalCredit` auto-populated from DR/CR direction
   - Shadow balance updated on create (not actual balance)
   - Voucher `postingDate` must equal `tenant.currentBusinessDate` (enforced)
4. **Authorize** voucher (system-auto for STP, or manual checker for approval flow):
   - `VoucherService.systemAuthorizeVoucher(...)` for auto-authorized transactions
   - `VoucherService.authorizeVoucher(...)` for checker approval (maker ≠ checker enforced)
5. **Post** voucher via `VoucherService.postVoucher(...)`:
   - Creates immutable `LedgerJournal` + `LedgerEntry` (marked `@Immutable`)
   - Updates actual balance via `CbsBalanceEngine`
   - Updates GL balance via `GlBalanceService`
   - Validates batch is OPEN before posting
   - Marks `postFlag = Y` on voucher
6. **Update** Account balance cache + batch totals

### 5.4 Voucher lifecycle states

Ground truth is the flag triple `(authFlag, postFlag, cancelFlag)`. A derived `VoucherStatus` enum provides convenience:

| Flags | VoucherStatus | Meaning |
|---|---|---|
| auth=N, post=N, cancel=N | `DRAFT` | Created by maker, awaiting authorization |
| auth=Y, post=N, cancel=N | `APPROVED` | Authorized by checker, awaiting posting |
| auth=Y, post=Y, cancel=N | `POSTED` | Posted to ledger (immutable entries exist) |
| cancel=Y | `REVERSED` | Cancelled via compensating reversal voucher |

### 5.5 Voucher number format

Format: `<TENANT_CODE>-<BRANCH_CODE>-<YYYYMMDD>-<6-digit sequence>`

Example: `TENANT-001-HQ001-20250130-000001`

- Sequence resets per branch per business date
- Concurrency-safe via `PESSIMISTIC_WRITE` lock on `scroll_sequences` table

### 5.6 Reversal flow

Only POSTED vouchers can be reversed. `VoucherService.cancelVoucher(...)`:

1. Creates a new voucher with reversed DR/CR direction
2. Links to original via `reversalOfVoucher` FK
3. Carries the same `transaction_id` FK as original
4. If original was posted → auto-posts reversal (new immutable ledger entries)
5. If original was unposted → reverses shadow balance only
6. Marks original `cancelFlag = Y`

**Never** updates existing ledger entries. Corrections are always compensating entries.

## 5A) Inter-Branch Transfer (IBT) posting flow

When a transfer crosses branch boundaries (source account branch ≠ destination account branch), the system enforces CBS-grade inter-branch clearing via `IbtService` and `InterBranchClearingService`.

### 5A.1 IBT detection

In `TransactionService.postTransferLedger()`, branch resolution happens automatically:

```
Branch sourceBranch = resolveBranch(sourceAccount, poster);
Branch destBranch   = resolveBranch(destAccount, poster);
crossBranch = sourceBranch.id != destBranch.id
```

If `crossBranch == true`, the system routes through the 4-voucher clearing flow. **Direct cross-branch posting is strictly prohibited** — CBS standard enforced by `IbtService`.

### 5A.2 IBT governance pre-validation

Before any voucher is created, `IbtService.validateBranchesForIbt()` checks:

- Both branches must be ACTIVE (`Branch.isActive = true`)
- Both branches must have clearing GL mappings configured (either in `branch_gl_mappings` config table or via seeded IBC-OUT/IBC-IN accounts)
- Source and destination must be different branches
- Clearing GL must be branch-specific (no global clearing account)

Failure throws `GovernanceException` with descriptive error codes (`IBT_BRANCH_INACTIVE`, `IBT_CLEARING_GL_NOT_CONFIGURED`, etc.).

### 5A.3 4-voucher clearing flow

```
┌─────── Branch A (source) ───────┐    ┌─────── Branch B (destination) ──┐
│                                  │    │                                  │
│ V1 DR: Customer Account    amt   │    │ V3 DR: IBC_IN_B           amt   │
│ V2 CR: IBC_OUT_A           amt   │    │ V4 CR: Customer Account   amt   │
│                                  │    │                                  │
│ Branch A: DR=amt, CR=amt ✅      │    │ Branch B: DR=amt, CR=amt ✅      │
└──────────────────────────────────┘    └──────────────────────────────────┘
```

All 4 vouchers share the same `transaction_id` FK. The posting sequence:

1. Create `InterBranchTransfer` record (status = `INITIATED`)
2. Post Branch A leg: DR Customer, CR IBC_OUT → mark transfer `SENT`
3. Post Branch B leg: DR IBC_IN, CR Customer → mark transfer `RECEIVED`
4. Validate exactly 4 vouchers created via `IbtService.validateIbtVoucherCount()`

### 5A.4 Atomicity guarantee

The entire 4-voucher posting runs inside the `@Transactional` boundary of `postTransferLedger()`. If any voucher post fails (e.g., destination account frozen, insufficient clearing GL), Spring rolls back **all** 4 voucher inserts, all ledger entries, the `InterBranchTransfer` record, and batch total updates. No partial ledger commit is possible.

### 5A.5 IBT reversal governance

IBT reversal must reverse **both** branch legs together. `IbtService.validateFullReversalRequired()` detects partial reversals (some vouchers cancelled, others not) and throws `GovernanceException` with code `IBT_PARTIAL_REVERSAL_BLOCKED`.

### 5A.6 Clearing GL configuration

Clearing GL resolution is configuration-driven via `branch_gl_mappings` table:

| Column | Purpose |
|---|---|
| `tenant_id` | Tenant FK (multi-tenant isolation) |
| `branch_id` | Branch FK |
| `clearing_gl_code` | GL code for clearing (e.g., `2910`) |
| `ibc_out_account_number` | IBC-OUT account for this branch |
| `ibc_in_account_number` | IBC-IN account for this branch |

Falls back to seeded `IBC-OUT-<branchCode>` / `IBC-IN-<branchCode>` accounts for backward compatibility.

### 5A.7 InterBranchTransfer lifecycle

```
INITIATED → SENT → RECEIVED → SETTLED
                              ↘ FAILED
```

- `INITIATED`: Transfer record created, no posting yet
- `SENT`: Branch A leg posted (DR Customer, CR IBC_OUT)
- `RECEIVED`: Branch B leg posted (DR IBC_IN, CR Customer)
- `SETTLED`: Clearing settlement completed during EOD/settlement
- `FAILED`: One or both legs failed; requires investigation

EOD blocks if any transfers are not `SETTLED` or `FAILED`.

### 5A.8 IBT UI endpoints

From `IbtController` (`src/main/java/com/ledgora/ibt/controller/IbtController.java`):

- `GET /ibt` — paginated IBT list with status/date/branch filters (MAKER, CHECKER, OPERATIONS, ADMIN, MANAGER, AUDITOR). Uses `JpaSpecificationExecutor` for composable filter queries against `InterBranchTransfer` (canonical aggregate — no Transaction table derivation).
- `GET /ibt/create` — IBT initiation form (MAKER, ADMIN, MANAGER, TELLER). Pre-validates cross-branch via AJAX account lookup showing branch codes.
- `POST /ibt/create` — validates source ≠ destination branch, delegates to `TransactionService.transfer()` which auto-detects cross-branch and routes through 4-voucher IBC clearing. Redirects to `/ibt/{transactionId}`.
- `GET /ibt/{id}` — IBT detail view (MAKER, CHECKER, OPERATIONS, ADMIN, MANAGER, AUDITOR). Accepts both IBT ID (from list) and Transaction ID (from create redirect). Uses 2-query strategy for N+1 prevention:
  - Query 1: `InterBranchTransferRepository.findByIdWithGraph()` — JOIN FETCH for IBT + fromBranch + toBranch + referenceTransaction + createdBy + approvedBy + tenant
  - Query 2: `VoucherRepository.findByTransactionIdWithGraph()` — JOIN FETCH for vouchers + branch + account + ledgerEntry + glAccount + maker + checker
  - Total: 2 SQL SELECTs, zero lazy loading in JSP. Vouchers grouped by branch (Branch A / Branch B) for visual clarity.
- `GET /ibt/reconciliation` — CBS-grade reconciliation dashboard (OPERATIONS, ADMIN, MANAGER, AUDITOR). Read-only. Shows:
  - KPI cards: unsettled count (INITIATED+SENT+RECEIVED), failed count, clearing GL net balance (from `CLEARING_ACCOUNT` type — not voucher derivation), clearing status (BALANCED/IMBALANCE)
  - Clearing net status alert (red if non-zero, green if balanced — maps to EOD gate)
  - Aging table: top 5 oldest unsettled IBTs with color-coded age (T+1 blue, T+2 yellow/ESCALATE, T+3+ red/CRITICAL)
  - Per-branch clearing account balances (IBC-OUT/IBC-IN with ZERO/NON-ZERO badges)

## 5B) Suspense GL parking flow

When a posting partially fails (e.g., debit leg succeeds but credit leg fails due to account freeze), the system routes the failed leg to a Suspense GL to preserve double-entry integrity. This is managed by `SuspenseResolutionService`.

### 5B.1 Suspense routing architecture

```
Normal Posting:
  DR Customer Account  ──→  CR Destination Account    (balanced, no suspense)

Failed Posting (credit leg fails):
  DR Customer Account  ──→  CR SUSPENSE GL            (parked temporarily)
  Transaction.status = PARKED
  SuspenseCase created with reason_code
  Metric: ledgora.suspense.created incremented

Correction (operations team resolves):
  DR SUSPENSE GL       ──→  CR Destination Account    (clears suspense to zero)
  SuspenseCase.status = RESOLVED
```

### 5B.2 Suspense account resolution

`SuspenseResolutionService.resolveSuspenseAccount(tenantId, channel)` resolves in order:

1. Channel-specific mapping from `suspense_gl_mappings` table
2. Default (channel=null) mapping from `suspense_gl_mappings` table
3. Fallback: any account with `accountType = SUSPENSE_ACCOUNT` for the tenant

Failure throws `GovernanceException` with code `SUSPENSE_ACCOUNT_MISSING`.

### 5B.3 SuspenseCase lifecycle

```
OPEN → RESOLVED   (retry credit posting succeeded)
     → REVERSED   (debit leg also reversed)
```

Each `SuspenseCase` tracks:
- The original transaction that partially failed
- The successfully posted voucher (debit leg)
- The suspense voucher (credit to suspense GL)
- The intended account (where credit should have gone)
- Reason code (`ACCOUNT_FROZEN`, `ACCOUNT_INACTIVE`, `POSTING_EXCEPTION`, `TIMEOUT`)
- Resolution: maker-checker enforced, resolution voucher linked

### 5B.4 Resolution operations

Operations team can:
- **Retry credit posting** — creates correction voucher (DR Suspense GL, CR intended account), marks case `RESOLVED`
- **Reverse debit** — cancels the original debit voucher, marks case `REVERSED`

Both operations enforce maker-checker: resolver must differ from checker.

### 5B.5 EOD enforcement

EOD blocks if:
- Any `SUSPENSE_ACCOUNT` type account has non-zero balance
- Any `SuspenseCase` is in `OPEN` status with amount exceeding tolerance threshold (default: 0)

### 5B.6 Observability

- Micrometer counter: `ledgora.suspense.created` — incremented on each new suspense case
- Audit events: `SUSPENSE_CASE_CREATED`, `SUSPENSE_CASE_RESOLVED`, `SUSPENSE_CASE_REVERSED`

## 6) Batch lifecycle

### 6.1 How batches are determined

Batches group transactions per tenant + channel + business date.

Channel → BatchType mapping (`src/main/java/com/ledgora/batch/service/BatchService.java:199-207`):

- `ATM` → `ATM`
- `ONLINE`, `MOBILE` → `ONLINE`
- `TELLER` → `BRANCH_CASH`
- `BATCH` → `BATCH`

### 6.2 Batch invariants

- Only `OPEN` batches are mutable (`BatchService.updateBatchTotals(...)` rejects non-OPEN, `src/main/java/com/ledgora/batch/service/BatchService.java:72-78`).
- A batch must be balanced (debit == credit) to close and to settle (`BatchService.validateBatchClose(...)` `BatchService.java:134-144`; `settleAllBatches(...)` `BatchService.java:159-173`).

### 6.3 Batch UI endpoints

From `BatchController` (`src/main/java/com/ledgora/batch/controller/BatchController.java`):

- `GET /batches` dashboard (`BatchController.java:35-53`)
- `POST /batches/{id}/close` close one (`BatchController.java:59-69`)
- `POST /batches/close-all` close all for current business date (`BatchController.java:74-86`)
- `POST /batches/settle-all` settle all closed batches for current business date (`BatchController.java:92-104`)
- `POST /batches/open` open/get-or-create open batch for channel (`BatchController.java:111-126`)

## 7) EOD (End-of-Day) and Day Begin

Ledgora models day lifecycle with per-tenant `dayStatus` (`OPEN`, `DAY_CLOSING`, `CLOSED`) stored in `Tenant.dayStatus` (`src/main/java/com/ledgora/tenant/entity/Tenant.java:34-37`).

### 7.1 EOD validation (pre-check)

`EodValidationService.validateEod(...)` runs checks and returns error strings (empty list means OK). It checks:

- unauthorized vouchers (authFlag=N, cancelFlag=N)
- unposted vouchers (postFlag=N, cancelFlag=N)
- approved-but-unposted vouchers (authFlag=Y, postFlag=N, cancelFlag=N) — must be posted or cancelled
- posted voucher debit/credit balance — SUM(totalDebit) must equal SUM(totalCredit) for posted vouchers on the date
- ledger integrity for date (debits == credits from ledger_entries)
- pending approval requests
- transactions pending approval (PENDING_APPROVAL status)
- unsettled inter-branch transfers (all IBC transfers must be SETTLED or FAILED) via `InterBranchClearingService`
- **clearing GL net-zero** — SUM(balance) of all `CLEARING_ACCOUNT` type accounts must equal 0 per tenant, via `IbtService.validateClearingGlNetZero()`
- **suspense GL balance** — SUM(balance) of all `SUSPENSE_ACCOUNT` type accounts must equal 0 per tenant, via `SuspenseResolutionService.validateSuspenseAccountBalance()`
- **suspense cases** — no open `SuspenseCase` records with amount exceeding tolerance threshold (default: 0), via `SuspenseResolutionService.validateSuspenseForEod()`
- tenant GL balanced via `CbsGlBalanceService`

### 7.2 EOD run (crash-safe state machine)

`EodValidationService.runEod(tenantId)` delegates to `EodStateMachineService.executeEod()` which executes each phase in its own `@Transactional(propagation = REQUIRES_NEW)` boundary. Crash between phases is safe — on restart, the process resumes from the last committed phase.

**Phase progression:**

```
VALIDATED → DAY_CLOSING → BATCH_CLOSED → SETTLED → DATE_ADVANCED
```

| Phase | What it does | Commits independently |
|---|---|---|
| `VALIDATED` | Run all EOD pre-checks (`validateEod()`) | ✅ |
| `DAY_CLOSING` | `startDayClosing()` + re-validate (TOCTOU) | ✅ |
| `BATCH_CLOSED` | `closeAllBatches()` | ✅ |
| `SETTLED` | `settleAllBatches()` | ✅ |
| `DATE_ADVANCED` | `closeDayAndAdvance()` + mark COMPLETED | ✅ |

**State tracking:** `eod_processes` table with unique constraint `(tenant_id, business_date)`:
- `status`: RUNNING → COMPLETED or FAILED
- `phase`: current/last phase
- `last_updated`: used for stuck detection (> 30 minutes = stuck alert)

**Safety guarantees:**
- **Double execution prevented**: unique constraint blocks second EOD for same date
- **Crash recovery**: on restart, `findIncompleteProcesses()` returns all RUNNING processes for resumption
- **Stuck detection**: `findStuckProcesses()` returns processes idle > 30 minutes
- **Failed retry**: FAILED processes can be retried — they resume from the failed phase

**Audit events:** `EOD_STARTED`, `EOD_COMPLETED`, `EOD_FAILED` logged to `audit_logs` with `entity_type = EOD_PROCESS`.

### 7.3 EOD UI endpoints

From `EodController` (`src/main/java/com/ledgora/eod/controller/EodController.java`):

- `GET /eod/validate` shows validation errors (`EodController.java:75-90`)
- `GET /eod/run` form (`EodController.java:92-99`)
- `POST /eod/run` executes `eodValidationService.runEod(...)` (`EodController.java:101-116`)
- `GET /eod/status` summary (`EodController.java:118-128`)

### 7.4 Day Begin (opening the next day)

After EOD closes and advances the business date, the next day must be explicitly opened.

`DayBeginService`:

- Validates the day is CLOSED and checks some previous-day conditions (`src/main/java/com/ledgora/eod/service/DayBeginService.java:68-118`).
- `openDay(...)` runs validation and then calls `tenantService.openDay(tenantId)` (`DayBeginService.java:125-153`).

Day Begin UI endpoints (same controller):

- `GET /eod/day-begin`
- `POST /eod/day-begin`

See `EodController.java:44-71`.

## 8) Settlement flow (operational)

Settlement is a separate operational workflow from EOD screens.

### 8.1 What settlement does

`SettlementService.processSettlement(date)` is a per-tenant settlement/EOD-like workflow (`src/main/java/com/ledgora/settlement/service/SettlementService.java:88-233`). It:

1. Marks tenant as DAY_CLOSING (`SettlementService.java:117-122`)
2. Flushes `PENDING` transactions to `COMPLETED` for that date (`SettlementService.java:123-132`)
3. Validates ledger debits == credits for that date (`SettlementService.java:133-142`)
4. Generates a trial balance (`SettlementService.java:143-152`)
5. Closes and validates batches (`SettlementService.java:154-157`)
6. Settles batches (`SettlementService.java:158-162`)
7. Recomputes balances and creates `SettlementEntry` rows per account (`SettlementService.java:163-200`)
8. Advances tenant business date (`SettlementService.java:209-214`)

### 8.2 Settlement UI endpoints

- `GET /settlements` list + filter (`src/main/java/com/ledgora/settlement/controller/SettlementController.java:24-35`)
- `GET /settlements/process` form (`SettlementController.java:45-49`)
- `POST /settlements/process` executes settlement (`SettlementController.java:51-65`)
- `GET /settlement/dashboard` operational dashboard (`src/main/java/com/ledgora/settlement/controller/SettlementDashboardController.java:39-75`)

## 9) Voucher UI endpoints and security

### 9.1 Voucher endpoints

From `VoucherController` (`src/main/java/com/ledgora/voucher/controller/VoucherController.java`):

- `GET /vouchers` — inquiry/search (MAKER, CHECKER, AUDITOR, ADMIN, MANAGER, TELLER, OPERATIONS)
- `GET /vouchers/{id}` — **detail view** showing header, linked transaction, ledger entries, batch, maker/checker, status timeline (same roles)
- `GET /vouchers/create` — create form (MAKER, ADMIN, MANAGER, TELLER)
- `POST /vouchers/create` — create DR+CR voucher pair (same roles)
- `GET /vouchers/pending` — pending authorization list (CHECKER, ADMIN, MANAGER)
- `POST /vouchers/{id}/authorize` — authorize a voucher (CHECKER, ADMIN, MANAGER)
- `POST /vouchers/{id}/reject` — reject/cancel a voucher (CHECKER, ADMIN, MANAGER)
- `GET /vouchers/posted` — posted vouchers (all operational roles)
- `GET /vouchers/cancelled` — cancelled/reversed vouchers (all operational roles)

### 9.2 Role-based access control

All voucher endpoints are protected by `@PreAuthorize`:

| Role | Can Create | Can Authorize | Can Reject | Can View |
|---|---|---|---|---|
| MAKER | ✅ | ❌ | ❌ | ✅ (own) |
| CHECKER | ❌ | ✅ | ✅ | ✅ |
| AUDITOR | ❌ | ❌ | ❌ | ✅ (read-only) |
| ADMIN | ✅ | ✅ | ✅ | ✅ |
| MANAGER | ✅ | ✅ | ✅ | ✅ |
| TELLER | ✅ | ❌ | ❌ | ✅ |

Maker-checker enforcement: a checker cannot authorize their own voucher (enforced in `VoucherService.authorizeVoucher(...)`).

## 9A) Governance Dashboards (read-only operational visibility)

All governance dashboards are read-only — no record mutation. They provide CBS-grade operational monitoring for pre-EOD checks, fraud detection, and audit compliance.

### 9A.1 Suspense GL Dashboard

`GET /suspense/dashboard` — `SuspenseDashboardController` (OPERATIONS, ADMIN, MANAGER, AUDITOR)

- KPI cards: open cases, resolved cases, suspense GL net balance (`SUSPENSE_ACCOUNT` type), open exposure (sum of open case amounts)
- GL Control: RED if `suspenseGlNetBalance != 0` ("EOD will block"), ORANGE if GL ≠ case exposure (mismatch), GREEN if healthy
- Aging table: top 10 oldest OPEN cases with color-coded age (T+1 blue, T+2 yellow/ESCALATE, T+3+ red/CRITICAL)
- Performance: ≤3 SELECTs. JOIN FETCH on aging query prevents N+1.

### 9A.2 Clearing Settlement Engine

`GET /clearing/engine` — `ClearingEngineController` (OPERATIONS, ADMIN, MANAGER)

- Settlement readiness gate: `settlementReady = (unsettledCount == 0 AND clearingGlNet == 0)`
- KPI cards: unsettled IBT count, failed count, clearing GL net balance
- Unsettled transfers table: top 20 oldest with branch codes and color-coded aging
- Does NOT execute settlement — read-only monitoring only
- Performance: ≤3 SELECTs. Reuses existing `findOldestUnsettledByTenantId` (JOIN FETCH).

### 9A.3 Hard Transaction Ceiling Monitor

`GET /risk/hard-ceiling` — `HardCeilingDashboardController` (OPERATIONS, ADMIN, MANAGER, AUDITOR)

- KPI: today's violation count (uses tenant business date, not system clock)
- Enforcement status: CLEAN (0), MONITOR (1-3), ALERT (>3)
- Recent violations table: last 20 `HARD_LIMIT_EXCEEDED` audit events with timestamp, user, entity, details
- Data source: `audit_logs` table with `action = 'HARD_LIMIT_EXCEEDED'`
- Performance: 2 SELECTs. No N+1 (AuditLog has no lazy associations).

### 9A.4 Velocity Fraud Risk Monitor

`GET /risk/velocity` — `VelocityFraudDashboardController` (OPERATIONS, ADMIN, MANAGER, AUDITOR)

- Fraud pressure level: LOW (0 alerts), MEDIUM (1-5), HIGH (>5)
- KPI cards: open fraud alerts, accounts under review (`UNDER_REVIEW` status)
- Recent alerts table: last 20 FraudAlert records with account, type, observed count/amount, threshold, status
- Data source: `fraud_alerts` table + `accounts` table (UNDER_REVIEW count)
- Performance: 3 SELECTs. No N+1 (FraudAlert scalar fields only in JSP).

### 9A.5 Enterprise Audit Log Explorer

`GET /audit/explorer` — `AuditExplorerController` (ADMIN, AUDITOR)

- Searchable/filterable: date range, action (LIKE), username (LIKE), entity type (LIKE), entity ID (exact)
- Paginated table: timestamp, username, action, entity, entity ID, old value (truncated), new value (truncated), IP address
- Uses `JpaSpecificationExecutor` for composable filter queries — single paginated SELECT
- All filters optional; tenant isolation always enforced as base predicate
- Performance: 1 paginated SELECT (COUNT + data). No N+1.

### 9A.6 EOD Performance Stress Test Harness

**Active only in `stress` profile** — `@Profile("stress")` on all beans. Never instantiated in dev/prod.

**Endpoint:** `POST /stress/eod` — `StressTestController` (ADMIN only)

Request body:
```json
{
  "tenantId": 1,
  "accounts": 100,
  "transactions": 1000,
  "ibtRatio": 30
}
```

**Phase 1 — Load Generation** (`EodLoadGeneratorService`):
- Creates N accounts across existing branches (round-robin distribution)
- Generates deposits + cross-branch IBT transfers via `TransactionService.deposit()` and `.transfer()`
- All governance controls fire: hard ceiling, velocity, idempotency, batch totals, double-entry
- Uses deterministic random seed (42) for reproducibility
- Progress logged every 500 transactions

**Phase 2 — EOD Execution** (`EodPerformanceRunner`):
- Captures pre-EOD counts: transactions, vouchers, ledger entries, IBT records, suspense cases
- Clears Hibernate statistics, executes `eodValidationService.runEod(tenantId)`, captures wall-clock time
- Post-EOD captures: `prepareStatementCount`, `entityLoadCount`, `queryExecutionCount`
- Validates: clearing GL net = 0, suspense GL net = 0, no exception

**Output:** `EodPerformanceResult` DTO returned as JSON + logged as structured console summary.

**Configuration:** `application-stress.properties` — H2 isolated DB (`ledgora_stress`), `hibernate.generate_statistics=true`, SQL logging suppressed, HikariCP pool=20.

**Activation:** `mvn spring-boot:run -Dspring-boot.run.profiles=stress`

### 9A.7 Additional Stress & Diagnostics Harnesses

All active only in `stress` profile. ADMIN role required. No production logic modified.

**Query Plan Analyzer** — `GET /diagnostics/query-plans`
- Runs EXPLAIN on 6 critical SQL queries (EOD, IBT, suspense, audit)
- Risk classification: HIGH (table scan, no index), MEDIUM, LOW
- Validates that production performance indexes are effective

**Lock Contention Simulator** — `POST /stress/lock-contention`
- N concurrent threads posting transfers + optional parallel EOD
- Detects deadlocks, lock timeouts, slow transactions (>2s)
- CountDownLatch synchronization maximizes contention

**Deadlock Simulator** — `POST /stress/deadlock`
- Provokes cross-account lock ordering deadlock (A→B vs B→A)
- Post-deadlock recovery verification: ledger balanced, no partial vouchers, batch totals intact

**Production Load Generator** — `POST /stress/load`
- Token-bucket rate limiter (Semaphore, refill per second)
- Workload mix: 40% deposit, 30% withdrawal, 25% transfer, 5% IBT
- Captures P50/P95/P99 latency, error classification (balance/velocity/ceiling/lock)

**Chaos EOD Tester** — `POST /stress/chaos-eod`
- Manufactures FAILED EodProcess at target phase, then triggers resume
- Tests EodStateMachineService crash recovery without modifying it
- Validates: ledger balanced, no duplicates, no stuck RUNNING, clearing/suspense GL zero

## 10) Suggested E2E verification checklist

A quick manual verification (dev/H2):

1. Start app (`mvn spring-boot:run`)
2. Login as `admin/admin123`
3. Go to `/accounts` and open an account detail; verify the Balances tab shows available balance
4. Perform:
   - Deposit via `/transactions/deposit`
   - Transfer via `/transactions/transfer`
5. Verify vouchers were created: open H2 console and run:
   ```sql
   select id, voucher_number, dr_cr, transaction_amount, total_debit, total_credit,
          auth_flag, post_flag, cancel_flag, transaction_id
   from vouchers
   order by id desc;
   ```
6. Verify voucher → ledger link:
   ```sql
   select v.voucher_number, v.dr_cr, v.transaction_amount,
          le.entry_type, le.amount, le.gl_account_code
   from vouchers v
   left join ledger_entries le on le.id = v.ledger_entry_id
   where v.post_flag = 'Y'
   order by v.id desc;
   ```
7. Verify voucher debit/credit balance for the day:
   ```sql
   select sum(total_debit) as total_dr, sum(total_credit) as total_cr,
          case when sum(total_debit) = sum(total_credit) then 'BALANCED' else 'UNBALANCED' end as status
   from vouchers
   where post_flag = 'Y' and cancel_flag = 'N'
     and posting_date = CURRENT_DATE;
   ```
8. Go to `/batches` and verify batches exist for your channels
9. Close/settle batches using UI actions
10. **Test inter-branch transfer (IBT) via dedicated UI:**
    - Go to `/ibt/create` and select accounts at different branches (e.g., BR001 and BR002)
    - Verify the cross-branch detection banner appears (green "Cross-branch detected" indicator)
    - Submit the transfer — system redirects to `/ibt/{id}` detail view
    - On the detail view, verify:
      - IBT record shows status progression (INITIATED → SENT → RECEIVED)
      - 4 vouchers displayed grouped by branch (Branch A: DR Customer + CR IBC_OUT; Branch B: DR IBC_IN + CR Customer)
      - Clearing GL panel shows IBC-OUT/IBC-IN balances
    - Go to `/ibt` list and verify the new transfer appears with correct status badge
    - Go to `/ibt/reconciliation` and verify:
      - KPI cards show correct unsettled/failed counts
      - Clearing GL net balance (should be 0 if both legs posted)
      - Clearing status shows BALANCED (green)
    - Also verify via H2 console:
      ```sql
      select v.id, v.voucher_number, v.dr_cr, v.transaction_amount,
             v.account_id, a.branch_code, v.narration
      from vouchers v
      join accounts a on a.id = v.account_id
      where v.transaction_id = (select max(id) from transactions where transaction_type = 'TRANSFER')
      order by v.id;
      ```
    - Verify clearing accounts net to zero:
      ```sql
      select a.account_number, a.balance, a.branch_code
      from accounts a
      where a.account_number like 'IBC-%'
      order by a.account_number;
      ```
    - Verify InterBranchTransfer record:
      ```sql
      select id, status, amount, business_date from inter_branch_transfers order by id desc;
      ```
11. Validate EOD via `/eod/validate` then run EOD via `/eod/run`
12. After EOD, use `/eod/day-begin` to open the day
13. Test voucher detail view: go to `/vouchers/{id}` for any voucher
14. **Test Suspense Dashboard:** go to `/suspense/dashboard` — verify KPI cards show 0 open cases and suspense GL balanced (green)
15. **Test Clearing Engine:** go to `/clearing/engine` — verify settlement readiness shows green if no unsettled IBTs and clearing GL net = 0
16. **Test Hard Ceiling Monitor:** go to `/risk/hard-ceiling` — verify today's violation count and enforcement status badge
17. **Test Velocity Fraud Monitor:** go to `/risk/velocity` — verify pressure level (LOW if no open alerts) and accounts under review count
18. **Test Audit Explorer:** go to `/audit/explorer` — verify filter panel works (try filtering by action `VOUCHER_POSTED`), verify pagination
19. **Test Stress Harness (stress profile only):** start app with `--spring.profiles.active=stress`, then `POST /stress/eod` with `{"tenantId":1,"accounts":50,"transactions":200,"ibtRatio":30}`. Verify JSON response includes `success: true`, `clearingGlZero: true`, `suspenseGlZero: true`, and check console for structured performance summary.
20. **Test Query Plan Analyzer:** `GET /diagnostics/query-plans` — verify all 6 queries return `riskLevel: "LOW"` (indexes effective)
21. **Test Lock Contention:** `POST /stress/lock-contention` with `{"tenantId":1,"threads":4,"transactionsPerThread":20,"triggerEod":false}` — verify `deadlockCount: 0` or low, check `lockWaitOccurrences`
22. **Test Deadlock Simulation:** `POST /stress/deadlock` with `{"tenantId":1,"accountA":"SAV-1001-0001","accountB":"SAV-1002-0001","rounds":2}` — verify `systemRecovered: true`, `ledgerBalanced: true`
23. **Test Production Load:** `POST /stress/load` with `{"tenantId":1,"threads":5,"targetTps":20,"durationSeconds":10,"ibtRatio":15}` — verify `actualTps` near target, check `p95LatencyMs`
24. **Test Chaos EOD:** `POST /stress/chaos-eod` with `{"tenantId":1,"crashAfterPhase":"DAY_CLOSING"}` — verify `resumeSucceeded: true`, `ledgerBalanced: true`, `noStuckRunning: true`

For additional data-level checks, use H2 console and the SQL in `README.md`, `docs/ibt-audit-sql-pack.sql`, `docs/suspense-audit-sql-pack.sql`, `docs/hard-ceiling-audit-sql-pack.sql`, `docs/velocity-fraud-audit-sql-pack.sql`, `docs/eod-state-machine-audit-sql-pack.sql`, and `docs/balance-reconciliation-audit-sql-pack.sql`.
