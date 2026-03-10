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

1. **Validate** business rules (business day OPEN, sufficient funds, freeze level, holiday calendar, idempotency)
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
- **NEW: approved-but-unposted vouchers** (authFlag=Y, postFlag=N, cancelFlag=N) — must be posted or cancelled
- **NEW: posted voucher debit/credit balance** — SUM(totalDebit) must equal SUM(totalCredit) for posted vouchers on the date
- ledger integrity for date (debits == credits from ledger_entries)
- pending approval requests
- transactions pending approval (PENDING_APPROVAL status)
- open batches exist (must be closed first)
- tenant GL balanced via `CbsGlBalanceService`

### 7.2 EOD run

`EodValidationService.runEod(tenantId)` performs (`EodValidationService.java:141-168`):

1. Validate pre-checks
2. `tenantService.startDayClosing(tenantId)` (blocks posting)
3. Close all batches for business date (`batchService.closeAllBatches(...)`)
4. Settle all closed batches (`batchService.settleAllBatches(...)`)
5. Close day and advance date (`tenantService.closeDayAndAdvance(tenantId)`)

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
10. Validate EOD via `/eod/validate` then run EOD via `/eod/run`
11. After EOD, use `/eod/day-begin` to open the day
12. Test voucher detail view: go to `/vouchers/{id}` for any voucher

For additional data-level checks, use H2 console and the SQL in `README.md`.
