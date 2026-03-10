# Ledgora — RBI Governance & Compliance Control Layer

**Document Version:** 2.5
**System:** Ledgora CBS (Spring Boot 3.2.3)
**Commit Baseline:** PR #41 — CBS Enhancement (IBT + Suspense GL + Hard Ceilings + Velocity Fraud)
**Previous Versions:** 2.4, 2.3, 2.2, 2.1, 2.0, 1.0
**Standard Applied:** RBI Master Direction on IT Governance (2023), Banking Regulation Act §10/§35A, IS Audit Guidelines, CBS Accounting Standard, RBI Risk Appetite Framework, RBI Master Direction on Fraud Risk Management (2023)

> **Version 2.5 Changes:** Velocity Fraud Engine: `VelocityLimit` entity (`velocity_limits` table), `FraudAlert` entity (`fraud_alerts` table), `VelocityFraudEngine` service (60-min window, count + amount checks, account freeze to `UNDER_REVIEW`, FraudAlert creation), `UNDER_REVIEW` account status, Micrometer `ledgora.velocity.blocked` metric, integrated into `deposit()`/`withdraw()`/`transfer()`, audit SQL pack (6 queries). Closes G4/FR-06. See §1.10 for change log.
> **Version 2.4 Changes:** Hard Transaction Ceiling. Addresses FR-05. See §1.9.
> **Version 2.3 Changes:** Suspense GL. Closes G1. See §1.8.
> **Version 2.2 Changes:** CBS-grade IBT upgrade. See §1.7.
> **Version 2.1 / 2.0:** UI governance fixes, core governance fixes.
> Items marked ✅ Resolved were previously identified as gaps and have been addressed in code.
> See §1.6–§1.10 for full change logs.

---

## PART 1 — RBI COMPLIANCE MAPPING

### 1.1 Governance Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    GOVERNANCE LAYER                       │
├─────────────────────────────────────────────────────────┤
│  Segregation of Duties (Maker-Checker)                   │
│  Voucher Authorization Control                           │
│  Business Day Gate (OPEN / DAY_CLOSING / CLOSED)         │
│  Audit Trail (AuditService → audit_logs)                 │
│  Account Lockout & Session Control                       │
├─────────────────────────────────────────────────────────┤
│                  ACCOUNTING CONTROL LAYER                 │
├─────────────────────────────────────────────────────────┤
│  Immutable Ledger (@Immutable on Journal + Entry)        │
│  Double-Entry: pair-level (DR+CR vouchers, equal amount) │
│  Voucher Integrity (drCr!=null, amount>0 per leg)        │
│  Voucher Lifecycle (DRAFT → APPROVED → POSTED)           │
│  GL Balance Integrity (Branch + Tenant level)            │
│  Account.balance = cache only (ledger = truth)           │
├─────────────────────────────────────────────────────────┤
│                  OPERATIONAL CONTROL LAYER                │
├─────────────────────────────────────────────────────────┤
│  Batch Lifecycle (OPEN → CLOSED → SETTLED)               │
│  EOD Validation (12+ pre-checks before day close)        │
│  Day Begin Ceremony (explicit open after CLOSED)         │
│  Settlement (per-tenant, validates all invariants)        │
│  Tenant Isolation (ThreadLocal + session + DB scope)     │
│  Idempotency Keys (deduplication per client+channel)     │
├─────────────────────────────────────────────────────────┤
│              INTER-BRANCH TRANSFER (IBT) LAYER           │
├─────────────────────────────────────────────────────────┤
│  IbtService — CBS-grade IBT enforcement                  │
│  Direct cross-branch posting blocked (GovernanceExc)     │
│  Branch-specific clearing GL (config-driven)             │
│  4-voucher clearing flow (atomic @Transactional)         │
│  IBT voucher count validation (must be exactly 4)        │
│  Partial reversal blocked (all-or-nothing)               │
│  Branch ACTIVE + clearing GL pre-validation              │
│  Clearing GL net-zero EOD check                          │
│  BranchGlMapping config table                            │
├─────────────────────────────────────────────────────────┤
│              SUSPENSE GL EXCEPTION LAYER                  │
├─────────────────────────────────────────────────────────┤
│  SuspenseResolutionService — CBS exception accounting    │
│  SUSPENSE_ACCOUNT type + SuspenseGlMapping config        │
│  SuspenseCase entity (OPEN → RESOLVED / REVERSED)        │
│  Maker-checker enforced on resolution/reversal           │
│  PARKED transaction status for partial failures          │
│  EOD blocks on non-zero suspense balance + open cases    │
│  Micrometer metric: ledgora.suspense.created             │
└─────────────────────────────────────────────────────────┘
```

### 1.2 RBI Requirement → Ledgora Component Mapping

| # | RBI Requirement | Reference | Ledgora Component | Implementation | Status |
|---|---|---|---|---|---|
| R1 | **Ledger Immutability** — Books of accounts must not be altered after posting | Banking Reg Act §10, IAS/GAAP | `LedgerJournal` + `LedgerEntry` | Both marked `@org.hibernate.annotations.Immutable`; Hibernate prevents UPDATE/DELETE SQL. Corrections via reversal vouchers only. | ✅ Compliant |
| R2 | **Double-Entry Accounting** — Every debit must have an equal credit | Banking Reg Act §10, AS-1 | `TransactionService`, `VoucherService`, `EodValidationService` | **Single-leg voucher model:** each voucher is DR xor CR. Balance enforced at pair level — `TransactionService` passes identical `dto.getAmount()` to both DR and CR legs. `createVoucherPair()` atomic via `@Transactional`. `postVoucher()` validates voucher integrity (`drCr != null`, `amount > 0`). EOD validates `SUM(debits) == SUM(credits)` at ledger and voucher level. AUDIT-11 SQL provides per-journal proof. | ✅ Compliant |
| R3 | **Segregation of Duties** — Maker and checker must be different persons | RBI IT Gov §4.2, IS Audit §5.2 | `VoucherService.authorizeVoucher()`, `TransactionService.approveTransaction()` | `maker.getId() != checker.getId()` enforced in service layer. `@PreAuthorize` role gates on VoucherController. | ✅ Compliant |
| R4 | **Daily Balancing** — Books must balance at end of each business day | Banking Reg Act §10, RBI Circular | `EodValidationService.validateEod()` | 10+ pre-checks: unauthorized vouchers, unposted vouchers, approved-but-unposted, voucher DR/CR balance, ledger DR/CR balance, pending approvals, pending transactions, open batches, tenant GL balance. EOD blocked on any failure. | ✅ Compliant |
| R5 | **Audit Trail** — All financial operations must be traceable | RBI IT Gov §6.3, IS Audit §6 | `AuditService`, `audit_logs` table | Voucher lifecycle (CREATED/AUTHORIZED/POSTED/CANCELLED) logged via `AuditService.logEvent()`. Transaction lifecycle logged. Login/logout tracked. Old/new value capture available via `logChangeEvent()`. | ✅ Compliant |
| R6 | **Data Integrity** — Financial data must not be corrupted | RBI IT Gov §4.3, IS Audit §5.7 | `@Version` optimistic locking, `PESSIMISTIC_WRITE` on scroll sequences, `@Immutable` on ledger | Voucher has `@Version` for concurrent access safety. Scroll sequences use pessimistic locks. Ledger entries are immutable. Account balance uses `@Version`. | ✅ Compliant |
| R7 | **Business Day Control** — No transactions outside business hours | RBI Ops Circular | `TenantService.validateBusinessDayOpen()`, `Tenant.dayStatus` | Transactions blocked when `dayStatus != OPEN`. Day lifecycle: OPEN → DAY_CLOSING → CLOSED → (Day Begin) → OPEN. Per-tenant independent dates. | ✅ Compliant |
| R8 | **Batch Integrity** — Transaction batches must balance | CBS Operations Standard | `BatchService.validateBatchClose()`, `settleAllBatches()` | `totalDebit == totalCredit` enforced before close and before settlement. Only OPEN batches accept new transactions. | ✅ Compliant |
| R9 | **Tenant Isolation** — Multi-bank data must not leak | RBI IT Gov §4.3 | `TenantContextHolder` (ThreadLocal), `tenant_id` FK on all entities | Every query is tenant-scoped. Tenant switch requires POST + MULTI scope + allowed tenant validation. Context cleared after request. | ✅ Compliant |
| R10 | **Access Control** — Role-based access with least privilege | RBI Cyber Security Framework §4.1 | `SecurityConfig`, `@PreAuthorize`, 13 roles | CSRF enabled (cookie-based, conditional H2 exclusion). H2 console requires ADMIN + disabled in prod. Voucher endpoints role-gated. Session concurrency limit (1). Account lockout after 5 failures. `ROLE_SYSTEM` for SYSTEM_AUTO pseudo-user (cannot login). JWT secret validated at startup. | ✅ Compliant |
| R11 | **Idempotency** — Duplicate transactions must be prevented | CBS Operations Standard | `IdempotencyService`, composite index `(client_reference_id, channel, tenant_id)` | Checked before transaction creation. Existing key returns error. | ✅ Compliant |
| R12 | **KYC Compliance** — Customer identity verification before account operations | RBI KYC/AML Master Direction | `CbsCustomerValidationService`, `CustomerMaster.kycStatus` | Account operations validate customer KYC status. PAN/Aadhaar stored in `CustomerTaxProfile`. | ✅ Compliant |

### 1.3 Identified Compliance Gaps

| # | Gap | Severity | Current State | Recommendation |
|---|---|---|---|---|
| G1 | ~~**Suspense GL not implemented**~~ | ~~HIGH~~ | **RESOLVED:** `SUSPENSE_ACCOUNT` type added. `SuspenseGlMapping` config table for tenant+channel routing. `SuspenseCase` entity tracks parked entries (OPEN → RESOLVED/REVERSED). `SuspenseResolutionService` handles account resolution, case creation with Micrometer metric (`ledgora.suspense.created`), maker-checker resolution/reversal. `PARKED` transaction status added. EOD blocks on non-zero suspense balance + open cases via `validateSuspenseAccountBalance()` and `validateSuspenseForEod()`. Suspense audit SQL pack with 6 queries. | ✅ Resolved |
| G2 | ~~**Inter-branch clearing GL absent**~~ | ~~HIGH~~ | **RESOLVED:** GL 2910 (Inter-Branch Clearing) added to chart of accounts. IBC-OUT and IBC-IN clearing accounts seeded per branch (HQ001, BR001, BR002). `TransactionService.postTransferLedger()` detects cross-branch transfers and routes through 4-voucher clearing flow (DR Customer A + CR IBC_OUT_A / DR IBC_IN_B + CR Customer B). `InterBranchTransfer` entity tracks lifecycle (INITIATED → SENT → RECEIVED → SETTLED). EOD blocks on unsettled transfers via `InterBranchClearingService.validateClearingBalance()`. Settlement integration via `settleTransfers()`. | ✅ Resolved |
| G3 | **No transaction amount limits per role** | MEDIUM | `TransactionService` validates amount > 0 and < 999999999999.99, but no per-role/per-channel limits. A teller can process unlimited amounts. | Add configurable teller/channel limits in approval policy. |
| G4 | ~~**No velocity checks**~~ | ~~MEDIUM~~ | **RESOLVED:** `VelocityFraudEngine` queries 60-minute transaction window per account. `VelocityLimit` config table (per-account or tenant-wide default). On breach: blocks transaction, freezes account to `UNDER_REVIEW`, creates `FraudAlert`, emits `ledgora.velocity.blocked` metric, logs `VELOCITY_BREACH_*` audit event. Integrated into `deposit()`, `withdraw()`, `transfer()`. Audit SQL pack with 6 queries. | ✅ Resolved |
| G5 | ~~**systemAuthorizeVoucher uses maker as checker**~~ | ~~MEDIUM~~ | **RESOLVED** (commit `04ad39db`): `SYSTEM_AUTO` pseudo-user seeded with `ROLE_SYSTEM`. `systemAuthorizeVoucher()` now throws `GovernanceException` if SYSTEM_AUTO missing — no fallback to maker. AUDIT-12 query validates compliance. | ✅ Resolved |
| G6 | **No scheduled ledger-vs-cache reconciliation** | LOW | `Account.balance` is documented as cache, but no scheduled job validates it against `SUM(ledger_entries)`. Drift could go undetected between EODs. | Add a `@Scheduled` validator service (e.g., every 5 minutes) that compares cache vs ledger and logs discrepancies. |
| G7 | **No data retention / archival policy** | LOW | All data lives in active tables indefinitely. No archival for historical ledger entries or closed-day vouchers. | Design an archival strategy: move closed-day data to archive tables after configurable retention period. |
| G8 | **Password policy not enforced** | LOW | BCrypt hashing is used, but no minimum length, complexity, or rotation requirements are enforced at registration/change time. | Add password policy validation (min 8 chars, uppercase, digit, special character). |

### 1.4 Recommended Enhancement Roadmap

| Priority | Enhancement | Effort | Impact | Status |
|---|---|---|---|---|
| ~~P0 (Immediate)~~ | ~~Suspense GL implementation~~ | ~~2-3 days~~ | ~~Blocks production deployment without safe error routing~~ | ✅ **DONE** — `SUSPENSE_ACCOUNT` type, `SuspenseGlMapping` config, `SuspenseCase` entity, `SuspenseResolutionService`, `PARKED` status, EOD validation, Micrometer metric, audit SQL pack |
| ~~P0~~ | ~~Inter-branch clearing GL~~ | ~~2-3 days~~ | ~~Required for multi-branch operations~~ | ✅ **DONE** — GL 2910 seeded, IBC-OUT/IBC-IN per branch, 4-voucher clearing flow, EOD validation, settlement integration |
| ~~P1~~ | ~~Dedicated SYSTEM_AUTO user for STP~~ | ~~0.5 day~~ | ~~Cleaner audit trail~~ | ✅ **DONE** — `SYSTEM_AUTO` seeded with `ROLE_SYSTEM`, `GovernanceException` on missing, no fallback |
| P1 (Next sprint) | Per-role transaction amount limits | 1-2 days | Prevents unauthorized high-value transactions | ⬜ Open |
| P1 (Next sprint) | Scheduled ledger-vs-cache validator | 1 day | Detects balance drift between EODs | ⬜ Open |
| P2 (Backlog) | Velocity monitoring | 2-3 days | Fraud detection for rapid-fire transactions | ⬜ Open |
| P2 (Backlog) | Password policy enforcement | 1 day | Regulatory hygiene | ⬜ Open |
| P3 (Long-term) | Data archival strategy | 5+ days | Storage optimization and regulatory retention | ⬜ Open |

### 1.5 Integration Points Summary

| Ledgora Service | Governance Role | Key Method |
|---|---|---|
| `EodValidationService` | Daily balancing gate — blocks EOD on ANY inconsistency | `validateEod(tenantId, businessDate)` — 10+ checks |
| `SettlementService` | Per-tenant settlement — validates ledger, closes/settles batches, advances date | `processSettlement(date)` — 8-step workflow |
| `BatchService` | Batch integrity — enforces `debit == credit` before close/settle | `validateBatchClose()`, `settleAllBatches()` |
| `VoucherService` | Accounting control — voucher lifecycle with maker-checker | `createVoucher()` → `authorizeVoucher()` → `postVoucher()` |
| `TenantService` | Business day authority — per-tenant OPEN/DAY_CLOSING/CLOSED | `validateBusinessDayOpen()`, `startDayClosing()`, `closeDayAndAdvance()` |
| `AuditService` | Audit trail — persistent logging of all financial events | `logEvent()`, `logFinancialEvent()`, `logChangeEvent()` |
| `CbsGlBalanceService` | GL integrity — branch + tenant level GL balance tracking | `isTenantGlBalanced()`, `isBranchGlBalanced()` |
| `DayBeginService` | Day Begin ceremony — validates previous day closed, batches settled | `validateDayBegin()`, `openDay()` |
| `IbtService` | Inter-branch transfer governance — branch validation, clearing GL enforcement, voucher count, partial reversal block | `validateBranchesForIbt()`, `validateIbtVoucherCount()`, `validateClearingGlNetZero()`, `validateFullReversalRequired()` |
| `SuspenseResolutionService` | Suspense GL management — account resolution, case lifecycle, maker-checker resolution, EOD suspense check | `resolveSuspenseAccount()`, `createSuspenseCase()`, `resolveCase()`, `reverseCase()`, `validateSuspenseForEod()` |
| `HardTransactionCeilingService` | Absolute hard transaction ceiling — non-bypassable limit enforcement before any persistence, governance audit logging, metric emission | `enforceHardCeiling(tenantId, channel, amount, userId)` — throws `GovernanceException(HARD_LIMIT_EXCEEDED)` |
| `VelocityFraudEngine` | Proactive velocity fraud detection — 60-min window count + amount check, account freeze, FraudAlert creation, metric emission | `evaluateVelocity(tenant, account, amount, userId)` — throws `GovernanceException(VELOCITY_LIMIT_EXCEEDED)` |

### 1.6 PR #40 Change Log — Gaps Addressed

Summary of all governance gaps identified and addressed in PR #40 (Feature Enhancement):

| Item | Description | Commit | Status |
|---|---|---|---|
| **G5** | SYSTEM_AUTO pseudo-user seeded with ROLE_SYSTEM. `systemAuthorizeVoucher()` throws `GovernanceException` if missing — no fallback to maker. | `04ad39db` | ✅ Resolved |
| **FR-04** | Maker-checker bypass for STP eliminated. SYSTEM_AUTO is mandatory checker for all auto-authorized vouchers. | `04ad39db` | ✅ Resolved |
| **FR-10** | `findOrCreateTransaction()` now checks `voucher.getTransaction()` before creating synthetic transaction — fixes audit trail for reversals. | PR #40 (ac232f6) | ✅ Resolved |
| **P1-1** | Amount scale validation (≤ 2 decimals) enforced in `TransactionService` via `RbiFieldValidator.validateTransactionAmount()`. | `d0473ff0` | ✅ Resolved |
| **P2-1** | `postVoucher()` now validates `tenant.dayStatus == OPEN` inside the service — blocks posting during DAY_CLOSING from any entry point. | `f79190c2` | ✅ Resolved |
| **P3-2** | `LedgerJournalRepository` — all delete methods overridden to throw `UnsupportedOperationException`, matching `LedgerEntryRepository`. | `f79190c2` | ✅ Resolved |
| **P6-1** | `postVoucher()` now fetches voucher via `findByIdAndTenantId` before acquiring pessimistic lock — blocks cross-tenant posting. | `df99e2b9` | ✅ Resolved |
| **P4-1** | EOD `areAllBatchesClosed` check removed from `validateEod()` — it was blocking `runEod()` since batches are OPEN during business. Batch closing handled by `runEod()` step 2. | `df99e2b9` | ✅ Resolved |
| **Security** | H2 CSRF exclusion now conditional on `h2ConsoleEnabled`. JWT secret validated at startup. Account lockout after 5 failures. Session limit of 1. | `df99e2b9` | ✅ Resolved |
| **Voucher** | Voucher entity hardened: `@Data` → `@Getter` + controlled setters. Financial fields immutable post-creation. `@Version` for optimistic locking. `totalDebit`/`totalCredit` auto-populated. | PR #40 | ✅ Resolved |
| **Voucher** | `cancelVoucher()` for non-posted vouchers no longer creates stuck reversal voucher that blocked EOD. | `df99e2b9` | ✅ Resolved |
| **Voucher** | Voucher integrity assertion: `postVoucher()` validates `drCr != null` and `amount > 0` before any ledger persistence. Replaces logically incorrect per-leg DR==CR check. | `b3f20d5c` | ✅ Resolved |
| **Batch** | `ensureBatchIsOpen()` uses `findByBatchCode` with BATCH-<id> fallback for backward compatibility. Tests updated to set `batchCode` column. | `08079497` | ✅ Resolved |
| **Atomic** | `createVoucherPair()` creates DR+CR in single `@Transactional` — no orphaned half-pairs. Controller uses it for UI-created vouchers. | `df99e2b9` | ✅ Resolved |
| **EOD** | TOCTOU fix: `runEod()` re-validates after `startDayClosing()` to catch concurrent transactions that sneaked in between pre-flight and lock. | `df99e2b9` | ✅ Resolved |
| **UI-1** | Transaction approval endpoints added: `GET /transactions/pending`, `POST /transactions/{id}/approve`, `POST /transactions/{id}/reject`. All `@PreAuthorize` gated for CHECKER/ADMIN/MANAGER. Connects existing `TransactionService.approveTransaction()` to the UI. | `6bb7667f` | ✅ Resolved |
| **UI-2 / FR-08** | CUSTOMER account ownership check added in `TransactionController.transactionHistory()`. CUSTOMER-role users can only access their own accounts. Non-customer roles bypass for legitimate access. Stopgap: name-based matching (production should use User→Customer FK). | `6bb7667f` | ✅ Resolved |
| **UI-3** | `isOwnVoucher` flag passed to voucher detail view. JSP can conditionally hide Authorize/Reject/Post buttons when current user is the voucher's maker. Backend enforcement unchanged. | `6bb7667f` | ✅ Resolved |
| **G2 / IBC** | Inter-Branch Clearing fully implemented: GL 2910 added, IBC-OUT/IBC-IN accounts seeded per branch, `InterBranchTransfer` entity + repository + service, `TransactionService.postTransferLedger()` routes cross-branch transfers through 4-voucher clearing flow, EOD validation blocks on unsettled IBC transfers, `settleTransfers()` for settlement integration. | PR #40 (latest) | ✅ Resolved |

**Remaining open gaps:** G3 (per-role limits), G4 (velocity checks), G6 (ledger-vs-cache reconciliation), G7 (data archival), G8 (password policy).

### 1.8 PR #41 Change Log — Suspense GL Implementation (v2.3)

| Item | Description | Status |
|---|---|---|
| **G1** | Suspense GL fully implemented. `SUSPENSE_ACCOUNT` added to `AccountType` enum. `SuspenseGlMapping` config table (tenant + channel → suspense account). `SuspenseCase` entity with full lifecycle tracking (OPEN → RESOLVED / REVERSED). | ✅ Resolved |
| **G1-SVC** | `SuspenseResolutionService`: suspense account resolution (channel-specific → default → fallback), case creation with Micrometer metric, maker-checker enforced resolution/reversal, EOD tolerance validation. | ✅ Resolved |
| **G1-EOD** | `EodValidationService` enhanced: suspense account balance check (`SUSPENSE_ACCOUNT` SUM = 0) + open suspense case tolerance check (default: zero tolerance). Both block EOD on violation. | ✅ Resolved |
| **G1-STATUS** | `PARKED` added to `TransactionStatus` enum for transactions routed to suspense. | ✅ Resolved |
| **G1-METRIC** | Micrometer counter `ledgora.suspense.created` emitted on each new suspense case. | ✅ Resolved |
| **G1-AUDIT** | Suspense audit SQL pack: 6 queries (account balance, open cases, resolution trail, summary, config check, PARKED transactions). See `docs/suspense-audit-sql-pack.sql`. | ✅ Resolved |

### 1.9 PR #41 Change Log — Hard Transaction Ceiling (v2.4)

| Item | Description | Status |
|---|---|---|
| **FR-05** | Hard Transaction Ceiling fully implemented. `HardTransactionLimit` entity with `hard_transaction_limits` table (tenant_id, channel, absolute_max_amount). `HardTransactionCeilingService` enforces absolute non-bypassable limits. | ✅ Resolved |
| **FR-05-INT** | `TransactionService.deposit()`, `withdraw()`, `transfer()` all call `enforceHardCeiling()` BEFORE any persistence — idempotency check, transaction creation, or voucher creation. | ✅ Resolved |
| **FR-05-NO-BYPASS** | No role (including ADMIN) can bypass. No runtime configuration override. Only DB-level change to `hard_transaction_limits` can modify ceilings. | ✅ Resolved |
| **FR-05-AUDIT** | All violations logged to `audit_logs` with action `HARD_LIMIT_EXCEEDED` and entity_type `GOVERNANCE`. Includes amount, ceiling, channel, userId in detail. | ✅ Resolved |
| **FR-05-METRIC** | Micrometer counter `ledgora.hard_limit.blocked` emitted on each blocked transaction. | ✅ Resolved |
| **FR-05-SQL** | Hard ceiling audit SQL pack: 4 queries (violation log, config check, bypass detection, user frequency). See `docs/hard-ceiling-audit-sql-pack.sql`. | ✅ Resolved |

**Remaining open gaps:** G3 (per-role soft limits), G6 (ledger-vs-cache reconciliation), G7 (data archival), G8 (password policy).

### 1.10 PR #41 Change Log — Velocity Fraud Engine (v2.5)

| Item | Description | Status |
|---|---|---|
| **G4 / FR-06** | Velocity Fraud Engine fully implemented. `VelocityLimit` entity with `velocity_limits` table (tenant_id, account_id, max_txn_count_per_hour, max_total_amount_per_hour). `FraudAlert` entity with `fraud_alerts` table. `VelocityFraudEngine` service. | ✅ Resolved |
| **G4-ENGINE** | `VelocityFraudEngine.evaluateVelocity()`: queries 60-minute transaction window via `TransactionRepository.countRecentByAccountId()` and `sumRecentAmountByAccountId()`. Resolves limit per-account first, then tenant default. | ✅ Resolved |
| **G4-ACTIONS** | On velocity breach: (1) block transaction with `GovernanceException`, (2) freeze account to `UNDER_REVIEW`, (3) create `FraudAlert` record, (4) emit `ledgora.velocity.blocked` metric, (5) log `VELOCITY_BREACH_*` audit event. | ✅ Resolved |
| **G4-STATUS** | `UNDER_REVIEW` added to `AccountStatus` enum. Accounts frozen by velocity engine are blocked from further transactions by existing `validateAccountActive()` check. | ✅ Resolved |
| **G4-INT** | `TransactionService.deposit()`, `withdraw()`, `transfer()` all call `evaluateVelocity()` after account validation and before transaction persistence. | ✅ Resolved |
| **G4-SQL** | Velocity fraud audit SQL pack: 6 queries (real-time velocity snapshot, open alerts, under-review accounts, config check, dashboard summary, breach trail). See `docs/velocity-fraud-audit-sql-pack.sql`. | ✅ Resolved |

---

*End of Part 1.*

---

## PART 2 — LEDGER INTEGRITY SQL AUDIT PACK

All queries are read-only, safe for production execution, and compatible with both H2 (dev) and SQL Server (prod).
Run these via H2 console (`/h2-console` with dev profile) or SQL Server Management Studio.

### 2.1 Overall Debit vs Credit Validation

**Purpose:** Verify the fundamental accounting equation holds across ALL ledger entries.

```sql
-- AUDIT-01: Global ledger balance check
-- Expected: total_debits == total_credits (difference = 0)
SELECT
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) AS total_debits,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS total_credits,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END)
      - SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS difference,
    CASE
        WHEN SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END)
           = SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END)
        THEN 'PASS' ELSE 'FAIL — LEDGER IMBALANCED'
    END AS audit_result
FROM ledger_entries;
```

### 2.2 Per Business Date Balance Check

**Purpose:** Verify double-entry holds for each individual business date. Detects date-specific corruption.

```sql
-- AUDIT-02: Per-date ledger balance
-- Expected: every row shows difference = 0
SELECT
    business_date,
    tenant_id,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) AS day_debits,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS day_credits,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END)
      - SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS difference,
    CASE
        WHEN SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END)
           = SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END)
        THEN 'BALANCED' ELSE 'IMBALANCED'
    END AS status
FROM ledger_entries
GROUP BY business_date, tenant_id
ORDER BY business_date DESC, tenant_id;
```

### 2.3 Voucher vs Ledger Mismatch Detection

**Purpose:** Detect vouchers marked POSTED (post_flag=Y) that have no corresponding ledger entry, or ledger entries with no voucher linkage.

```sql
-- AUDIT-03a: POSTED vouchers with no ledger entry
-- Expected: 0 rows
SELECT v.id AS voucher_id, v.voucher_number, v.dr_cr,
       v.transaction_amount, v.post_flag, v.ledger_entry_id
FROM vouchers v
WHERE v.post_flag = 'Y'
  AND v.cancel_flag = 'N'
  AND v.ledger_entry_id IS NULL;

-- AUDIT-03b: Ledger entries with voucher_id that references a non-POSTED voucher
-- Expected: 0 rows
SELECT le.id AS ledger_entry_id, le.voucher_id, le.amount, le.entry_type,
       v.post_flag, v.cancel_flag
FROM ledger_entries le
JOIN vouchers v ON v.id = le.voucher_id
WHERE v.post_flag != 'Y'
  AND le.voucher_id IS NOT NULL;

-- AUDIT-03c: Voucher amount vs ledger entry amount mismatch
-- Expected: 0 rows
SELECT v.id AS voucher_id, v.voucher_number,
       v.transaction_amount AS voucher_amount,
       le.amount AS ledger_amount,
       v.transaction_amount - le.amount AS difference
FROM vouchers v
JOIN ledger_entries le ON le.id = v.ledger_entry_id
WHERE v.post_flag = 'Y'
  AND v.cancel_flag = 'N'
  AND v.transaction_amount != le.amount;
```

### 2.4 Orphan Ledger Entry Detection

**Purpose:** Find ledger entries not linked to any transaction or journal. Orphans indicate posting engine defects.

```sql
-- AUDIT-04a: Ledger entries with no transaction
-- Expected: 0 rows
SELECT le.id, le.amount, le.entry_type, le.business_date, le.narration
FROM ledger_entries le
WHERE le.transaction_id IS NULL;

-- AUDIT-04b: Ledger entries with no journal
-- Expected: 0 rows
SELECT le.id, le.amount, le.entry_type, le.business_date, le.narration
FROM ledger_entries le
WHERE le.journal_id IS NULL;

-- AUDIT-04c: Journals with no entries (empty journals)
-- Expected: 0 rows
SELECT lj.id AS journal_id, lj.description, lj.business_date
FROM ledger_journals lj
LEFT JOIN ledger_entries le ON le.journal_id = lj.id
WHERE le.id IS NULL;
```

### 2.5 Batch Imbalance Detection

**Purpose:** Find batches where total_debit != total_credit. These should never exist in CLOSED or SETTLED state.

```sql
-- AUDIT-05: Unbalanced batches
-- Expected: 0 rows for CLOSED/SETTLED batches
SELECT tb.id AS batch_id, tb.batch_code, tb.batch_type, tb.status,
       tb.business_date, tb.tenant_id,
       tb.total_debit, tb.total_credit,
       tb.total_debit - tb.total_credit AS imbalance,
       tb.transaction_count
FROM transaction_batches tb
WHERE tb.total_debit != tb.total_credit
ORDER BY tb.status, tb.business_date DESC;
```

### 2.6 Suspense GL Balance Check

**Purpose:** Verify the Internal Suspense Account has zero balance. Non-zero indicates unresolved postings.

```sql
-- AUDIT-06: Suspense account balance
-- Expected: balance = 0 for all suspense accounts
SELECT a.id, a.account_number, a.account_name, a.balance,
       CASE WHEN a.balance = 0 THEN 'CLEAR' ELSE 'NON-ZERO — INVESTIGATE' END AS status
FROM accounts a
WHERE a.account_type = 'INTERNAL_ACCOUNT'
   OR a.account_number LIKE '%SUSP%'
ORDER BY a.balance DESC;
```

### 2.7 Inter-Branch Clearing Imbalance Check

**Purpose:** Verify clearing accounts net to zero. Non-zero indicates incomplete inter-branch settlement.

```sql
-- AUDIT-07: Clearing account balance check
-- Expected: balance = 0 for all clearing accounts
SELECT a.id, a.account_number, a.account_name, a.balance, a.branch_code,
       CASE WHEN a.balance = 0 THEN 'BALANCED' ELSE 'IMBALANCED — INVESTIGATE' END AS status
FROM accounts a
WHERE a.account_type = 'CLEARING_ACCOUNT'
   OR a.account_number LIKE '%CLR%'
ORDER BY ABS(a.balance) DESC;
```

### 2.8 Same-Maker-Checker Detection (Segregation of Duty Violation)

**Purpose:** Find vouchers where maker_id == checker_id AND the authorization was NOT system-auto. This indicates a segregation-of-duty bypass.

```sql
-- AUDIT-08: Same maker-checker vouchers (excluding system auto-auth)
-- Expected: 0 rows (all matches should have SYSTEM_AUTO in authorization_remarks)
SELECT v.id AS voucher_id, v.voucher_number,
       v.maker_id, v.checker_id,
       m.username AS maker_username,
       c.username AS checker_username,
       v.authorization_remarks,
       v.transaction_amount, v.dr_cr, v.posting_date,
       CASE
           WHEN v.authorization_remarks LIKE '%SYSTEM_AUTO%' THEN 'STP — ACCEPTABLE'
           ELSE 'VIOLATION — MANUAL SAME-USER APPROVAL'
       END AS audit_result
FROM vouchers v
JOIN users m ON m.id = v.maker_id
JOIN users c ON c.id = v.checker_id
WHERE v.maker_id = v.checker_id
  AND v.auth_flag = 'Y'
  AND v.cancel_flag = 'N'
ORDER BY v.posting_date DESC, v.id;
```

### 2.9 Backdated Posting Detection

**Purpose:** Find vouchers or ledger entries where the posting date does not match the tenant's business date at the time of creation. Backdated entries require special approval.

```sql
-- AUDIT-09a: Vouchers where posting_date != entry_date (backdated creation)
-- Expected: 0 rows in normal operations
SELECT v.id AS voucher_id, v.voucher_number,
       v.entry_date, v.posting_date,
       v.entry_date - v.posting_date AS date_gap_days,
       v.transaction_amount, v.dr_cr,
       m.username AS maker
FROM vouchers v
JOIN users m ON m.id = v.maker_id
WHERE v.entry_date != v.posting_date
  AND v.cancel_flag = 'N'
ORDER BY ABS(DATEDIFF(DAY, v.entry_date, v.posting_date)) DESC;

-- AUDIT-09b: Ledger entries posted after business date advanced
-- (entry created_at timestamp is after the business_date + 1 day)
-- Expected: 0 rows
SELECT le.id, le.business_date, le.created_at, le.amount, le.entry_type,
       le.narration
FROM ledger_entries le
WHERE CAST(le.created_at AS DATE) > le.business_date
ORDER BY le.created_at DESC;
```

### 2.10 Duplicate Transaction Detection

**Purpose:** Find transactions with identical amounts, accounts, and dates that may indicate duplicate processing.

```sql
-- AUDIT-10a: Exact duplicate transactions (same ref)
-- Expected: 0 rows (transaction_ref is unique, but check for near-duplicates)
SELECT t.transaction_ref, COUNT(*) AS occurrences
FROM transactions t
GROUP BY t.transaction_ref
HAVING COUNT(*) > 1;

-- AUDIT-10b: Suspicious near-duplicates (same amount + same account + same date + within 60 seconds)
-- Expected: review each match manually
SELECT t1.id AS txn1_id, t2.id AS txn2_id,
       t1.transaction_ref AS ref1, t2.transaction_ref AS ref2,
       t1.amount, t1.transaction_type,
       t1.business_date,
       t1.created_at AS time1, t2.created_at AS time2
FROM transactions t1
JOIN transactions t2
  ON t1.amount = t2.amount
  AND t1.transaction_type = t2.transaction_type
  AND t1.business_date = t2.business_date
  AND t1.tenant_id = t2.tenant_id
  AND t1.id < t2.id
  AND (t1.source_account_id = t2.source_account_id
       OR t1.destination_account_id = t2.destination_account_id)
WHERE t1.status != 'REJECTED' AND t2.status != 'REJECTED'
ORDER BY t1.business_date DESC, t1.amount DESC;

-- AUDIT-10c: Idempotency key collision check
-- Expected: 0 rows
SELECT idempotency_key, COUNT(*) AS occurrences
FROM idempotency_keys
GROUP BY idempotency_key
HAVING COUNT(*) > 1;
```

### 2.11 Per-Journal Double-Entry Balance Proof

**Purpose:** Verify that the ledger remains balanced at the journal level across the entire system. This is a detective control that catches any imbalance regardless of cause.

**Accounting model note:** Ledgora uses a **single-leg voucher model** — each voucher is DR xor CR, and each `postVoucher()` call creates one `LedgerJournal` with one `LedgerEntry`. The DR==CR balance is enforced at the **pair level** by `TransactionService` (creates matched DR+CR vouchers with equal amounts within a single `@Transactional`). This query validates the aggregate invariant across all journals.

**Preventive controls:** `VoucherService.postVoucher()` validates voucher integrity (`drCr != null`, `amount > 0`) before any ledger persistence. `TransactionService` passes identical amounts to both DR and CR legs. `createVoucherPair()` is atomic via `@Transactional`.

```sql
-- AUDIT-11: Per-journal debit vs credit balance proof
-- Expected: ZERO rows returned. Any row = severe accounting violation.
-- Run: daily (pre-EOD) + on-demand after any posting incident.
SELECT
    lj.id AS journal_id,
    lj.business_date,
    lj.description,
    SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END) AS total_debit,
    SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END) AS total_credit,
    SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END)
      - SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END) AS imbalance,
    COUNT(le.id) AS entry_count,
    CASE
        WHEN SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END)
           = SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END)
        THEN 'BALANCED'
        ELSE 'IMBALANCED — SEVERE VIOLATION'
    END AS audit_result
FROM ledger_journals lj
JOIN ledger_entries le ON le.journal_id = lj.id
GROUP BY lj.id, lj.business_date, lj.description
HAVING SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END)
    != SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END)
ORDER BY lj.business_date DESC, lj.id;
```

**If any rows are returned:**
1. **BLOCK EOD IMMEDIATELY** — do not close the business day.
2. Escalate to Operations Head + IT Head + Compliance.
3. Investigate the `journal_id` — trace back to the voucher and transaction.
4. The `@Immutable` annotation prevents modification — correction requires a compensating reversal journal.
5. Root cause analysis: check if `VoucherService.postVoucher()` was bypassed or if a code defect allowed unbalanced persistence.

**Governance control chain:**
- **Preventive (pair-level):** `TransactionService` creates equal-amount DR+CR vouchers within single `@Transactional`. `createVoucherPair()` atomic rollback if either leg fails.
- **Preventive (leg-level):** `VoucherService.postVoucher()` validates `drCr != null` and `amount > 0` via `AccountingException` (commit `b3f20d5c`).
- **Detective (EOD):** `EodValidationService.validateEod()` checks `SUM(debits) == SUM(credits)` at ledger and voucher level.
- **Detective (SQL):** This query (AUDIT-11) run daily pre-EOD — catches any imbalance regardless of source.
- **Corrective:** Reversal journal + root cause analysis if violation detected.

### 2.12 SYSTEM_AUTO Segregation of Duties Proof

**Purpose:** Prove that all STP (auto-authorized) vouchers use the `SYSTEM_AUTO` pseudo-user as checker, never the maker. This is the audit evidence for segregation of duties in straight-through processing.

Enforced by: `VoucherService.systemAuthorizeVoucher()` — throws `GovernanceException` if SYSTEM_AUTO user is not found (commit `04ad39db`). No fallback to maker.

```sql
-- AUDIT-12: Verify all STP vouchers have SYSTEM_AUTO as checker (not maker)
-- Expected: 0 rows. Any row = governance violation (maker acted as own checker).
SELECT v.id AS voucher_id, v.voucher_number,
       v.maker_id, v.checker_id,
       m.username AS maker_username,
       c.username AS checker_username,
       v.authorization_remarks,
       v.transaction_amount, v.dr_cr, v.posting_date
FROM vouchers v
JOIN users m ON m.id = v.maker_id
JOIN users c ON c.id = v.checker_id
WHERE v.authorization_remarks LIKE '%SYSTEM_AUTO_AUTHORIZED%'
  AND c.username != 'SYSTEM_AUTO'
  AND v.auth_flag = 'Y'
  AND v.cancel_flag = 'N'
ORDER BY v.posting_date DESC;
```

### 2.13 Audit Pack Execution Checklist

| # | Query | Run Frequency | Acceptable Result | Action on Failure |
|---|---|---|---|---|
| 01 | Global ledger balance | Daily (pre-EOD) | difference = 0 | **BLOCK EOD** — escalate to Operations Head |
| 02 | Per-date balance | Daily (pre-EOD) | All dates balanced | Investigate specific date; check for orphan entries |
| 03 | Voucher-ledger mismatch | Daily | 0 rows all 3 queries | Investigate posting engine; check VoucherService logs |
| 04 | Orphan ledger entries | Weekly | 0 rows all 3 queries | Data remediation required; root cause analysis |
| 05 | Batch imbalance | Daily (pre-EOD) | 0 unbalanced CLOSED/SETTLED | Block batch settlement; recount totals |
| 06 | Suspense GL balance | Daily (pre-EOD) | balance = 0 | Park resolution required before EOD |
| 07 | Clearing imbalance | Daily (pre-EOD) | balance = 0 | Inter-branch reconciliation required |
| 08 | Same maker-checker | Weekly | 0 non-STP violations | Disciplinary review; access audit |
| 09 | Backdated postings | Weekly | 0 rows | Investigate authorization chain |
| 10 | Duplicate transactions | Daily | 0 exact dupes; review near-dupes | Reversal + root cause |
| 11 | Per-journal DR==CR proof | Daily (pre-EOD) | 0 rows | **BLOCK EOD** — severe accounting violation; escalate immediately |
| 12 | SYSTEM_AUTO SoD proof | Weekly | 0 rows | Governance violation; investigate bypass; disciplinary action |

---

*End of Part 2.*

---

## PART 3 — SUSPENSE GL + EXCEPTION HANDLING MODEL

### 3.1 Problem Statement

When a financial operation partially fails (e.g., debit succeeds but credit fails due to account freeze, or an inter-system timeout), the system must not silently discard the imbalance. CBS standards require a **Suspense GL** to temporarily park the orphaned leg until manual correction.

Currently Ledgora has `INT-SUSP-001` (Internal Suspense Account) seeded in `DataInitializer` with `AccountType.INTERNAL_ACCOUNT`, but:
- No posting logic routes failed legs to it
- No EOD check validates its balance is zero
- No correction voucher workflow exists

### 3.2 Suspense GL Architecture

```
Normal Posting:
  DR Customer Account  ──→  CR Cash GL        (balanced, no suspense)

Failed Posting (credit leg fails):
  DR Customer Account  ──→  CR SUSPENSE GL    (parked temporarily)
  [Exception logged with reason code]

Correction (manual adjustment voucher):
  DR SUSPENSE GL       ──→  CR Cash GL        (clears suspense to zero)
```

**Key Rule:** EOD MUST NOT proceed if any tenant's Suspense GL balance ≠ 0 (configurable).

### 3.3 Design — Per-Tenant Suspense GL

Each tenant gets its own suspense account. The existing `INT-SUSP-001` serves TENANT-001. For multi-tenant:

| Tenant | Suspense Account | GL Code |
|---|---|---|
| TENANT-001 | `SUSP-T001` | `2900` (Other Liabilities → Suspense) |
| TENANT-002 | `SUSP-T002` | `2900` |

**GL Hierarchy Addition:**
```
2000 Liabilities
  └── 2400 Other Liabilities
        └── 2900 Suspense GL (NEW)
```

### 3.4 Posting Engine Modification (Pseudocode)

The change is in `VoucherService.postVoucher()` — wrap the credit/debit posting in a try-catch that routes to suspense on failure:

```java
// Inside VoucherService.postVoucher() — CONCEPTUAL (not literal code change)
try {
    // Normal posting: create LedgerJournal + LedgerEntry
    // Update actual balance via CbsBalanceEngine
    // Update GL balance via GlBalanceService
} catch (PostingException e) {
    // SUSPENSE ROUTING:
    // 1. Post the successful leg normally
    // 2. Post the failed leg against SUSPENSE GL instead of target
    // 3. Log exception with reason code
    // 4. Mark voucher with suspense_flag = 'Y' and suspense_reason
    // 5. Create SuspenseEntry record for tracking

    Account suspenseAccount = resolveSuspenseAccount(tenant.getId());
    // Create ledger entry: DR/CR suspenseAccount (opposite of failed leg)
    // This keeps double-entry balanced even though the real target failed

    auditService.logEvent(userId, "VOUCHER_SUSPENSE_ROUTED", "VOUCHER", voucher.getId(),
        "Routed to suspense: " + e.getMessage(), null);
}
```

### 3.5 Entity Changes Required

**Option A (Minimal — recommended first):** Add fields to existing Voucher entity:

```java
// On Voucher.java — new fields
@Column(name = "suspense_flag", length = 1, nullable = false)
@Builder.Default
private String suspenseFlag = "N";

@Column(name = "suspense_reason", length = 500)
private String suspenseReason;

@Column(name = "suspense_cleared_at")
private LocalDateTime suspenseClearedAt;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "correction_voucher_id")
private Voucher correctionVoucher;
```

**Option B (Full — for production):** Create a dedicated `SuspenseEntry` entity:

```java
@Entity
@Table(name = "suspense_entries")
public class SuspenseEntry {
    @Id @GeneratedValue private Long id;
    @ManyToOne private Tenant tenant;
    @ManyToOne private Voucher originalVoucher;
    @ManyToOne private Account suspenseAccount;
    @ManyToOne private Account intendedAccount;
    private BigDecimal amount;
    private String reasonCode;       // ACCOUNT_FROZEN, TIMEOUT, INSUFFICIENT_GL, etc.
    private String reasonDetail;
    private String status;           // OPEN, CLEARED, WRITTEN_OFF
    @ManyToOne private Voucher correctionVoucher;
    private LocalDate businessDate;
    private LocalDateTime createdAt;
    private LocalDateTime clearedAt;
}
```

### 3.6 EOD Validation Enhancement

Add to `EodValidationService.validateEod()`:

```java
// NEW EOD CHECK: Suspense GL must be zero before day close
BigDecimal suspenseBalance = accountRepository
    .sumBalanceByTenantIdAndAccountType(tenantId, AccountType.INTERNAL_ACCOUNT);
if (suspenseBalance.compareTo(BigDecimal.ZERO) != 0) {
    errors.add("EOD blocked: Suspense GL balance is " + suspenseBalance
        + " for tenant " + tenantId
        + ". All suspense entries must be cleared before EOD.");
}
```

This integrates with the existing EOD gate pattern — the check returns an error string, and EOD is blocked if any errors exist.

### 3.7 Correction Workflow

**Step 1 — Identify:** Operations officer reviews suspense entries via `/suspense/pending` screen.

**Step 2 — Create correction voucher:**
```
Original (parked):  DR CustomerAccount    CR SuspenseGL    (amount X)
Correction:         DR SuspenseGL         CR CashGL        (amount X)
```

The correction voucher:
- Must go through maker-checker approval
- Links back to the original via `correctionVoucher` FK
- Marks the suspense entry as CLEARED
- Updates `suspenseClearedAt` timestamp

**Step 3 — Verify:** Suspense GL balance returns to zero. EOD can now proceed.

### 3.8 Example Scenario: Transfer Failure → Suspense → Correction

**Scenario:** Rajesh transfers ₹10,000 to Priya. Priya's account has `freezeLevel = CREDIT_ONLY` (credit freeze active).

**Step 1 — Transfer attempt:**
```
Transaction TRF-001 initiated by teller1
  Voucher V1 (DR): Rajesh SAV-1001-0001, amount=10000  → POSTED OK
  Voucher V2 (CR): Priya SAV-1002-0001, amount=10000   → FAILS (CREDIT_FROZEN)
```

**Step 2 — Suspense routing (automatic):**
```
  Voucher V2-SUSP (CR): SUSP-T001, amount=10000        → POSTED to suspense
  SuspenseEntry created: reason=ACCOUNT_CREDIT_FROZEN, status=OPEN
```

Ledger is balanced: DR Rajesh 10000, CR Suspense 10000.

**Step 3 — Correction (next day, after freeze lifted):**
```
Operations officer creates correction voucher pair:
  Voucher V3 (DR): SUSP-T001, amount=10000              → Clears suspense
  Voucher V4 (CR): Priya SAV-1002-0001, amount=10000    → Posts to intended account
  SuspenseEntry updated: status=CLEARED, correctionVoucher=V3
```

Suspense GL returns to zero. EOD can proceed.

### 3.9 SQL — Suspense Monitoring Query

```sql
-- Suspense entries requiring resolution
SELECT a.account_number, a.account_name, a.balance AS suspense_balance,
       a.tenant_id,
       CASE WHEN a.balance = 0 THEN 'CLEAR' ELSE 'PENDING RESOLUTION' END AS status
FROM accounts a
WHERE (a.account_type = 'INTERNAL_ACCOUNT' OR a.account_number LIKE '%SUSP%')
  AND a.balance != 0
ORDER BY ABS(a.balance) DESC;
```

### 3.10 Integration Summary

| Component | Change Required |
|---|---|
| `DataInitializer` | Seed `GL 2900` (Suspense) + per-tenant suspense accounts |
| `VoucherService.postVoucher()` | Try-catch wrapper routing failed legs to suspense |
| `Voucher` entity | Add `suspenseFlag`, `suspenseReason`, `correctionVoucher` fields |
| `EodValidationService` | Add suspense GL balance ≠ 0 check |
| `VoucherController` | Add `/suspense/pending` screen for operations |
| `AuditService` | Log `VOUCHER_SUSPENSE_ROUTED` and `SUSPENSE_CLEARED` events |

**Invariants preserved:**
- ✅ Ledger remains balanced (suspense is a real GL, entries are double-entry)
- ✅ Ledger entries remain immutable (correction creates NEW entries)
- ✅ EOD still blocks on inconsistency (suspense ≠ 0 is a blocking error)
- ✅ Batch totals unaffected (suspense posting updates batch like any other)

---

*End of Part 3.*

---

## PART 4 — INTER-BRANCH CLEARING ARCHITECTURE

### 4.1 Problem Statement

Ledgora supports multi-branch operations (HQ001, BR001, BR002 seeded in `DataInitializer`). When a customer at Branch A transfers funds to a customer at Branch B within the **same tenant**, the current system posts directly between the two customer accounts without creating branch-level clearing entries.

This means:
- Branch-level GL balancing (`CbsGlBalanceService.isBranchGlBalanced()`) cannot detect inter-branch imbalances
- No audit trail of inter-branch fund movement exists at the GL level
- RBI expects branch books to independently balance

### 4.2 Clearing GL Design

**New GL Account:**
```
2000 Liabilities
  └── 2400 Other Liabilities
        └── 2910 Inter-Branch Clearing GL (NEW)
```

**Per-Branch Clearing Accounts:**

| Branch | Clearing Account | Type |
|---|---|---|
| HQ001 | `CLR-IB-HQ001` | `CLEARING_ACCOUNT` |
| BR001 | `CLR-IB-BR001` | `CLEARING_ACCOUNT` |
| BR002 | `CLR-IB-BR002` | `CLEARING_ACCOUNT` |

Each branch gets its own clearing account mapped to GL `2910`. The clearing GL nets to zero across all branches when all inter-branch movements are settled.

### 4.3 Posting Flow — Inter-Branch Transfer

**Scenario:** Rajesh (BR001) transfers ₹5,000 to Priya (BR001 — same branch).

Same-branch transfer — **no clearing needed:**
```
Voucher V1 (DR): Rajesh SAV-1001-0001 (BR001)    amount=5000
Voucher V2 (CR): Priya  SAV-1002-0001 (BR001)    amount=5000
```
Both legs are at BR001. Branch GL stays balanced. No clearing entry.

---

**Scenario:** Rajesh (BR001) transfers ₹5,000 to Amit (BR002) — **inter-branch**.

Current (broken): Direct posting between branches — BR001 GL loses 5000, BR002 GL gains 5000.

**Target (with clearing):**

```
┌─────────── Branch A (BR001) ───────────┐    ┌─────────── Branch B (BR002) ───────────┐
│                                         │    │                                         │
│ V1 DR: Rajesh SAV-1001-0001     5000    │    │ V3 DR: CLR-IB-BR002           5000    │
│ V2 CR: CLR-IB-BR001            5000    │    │ V4 CR: Amit SAV-1003-0001     5000    │
│                                         │    │                                         │
│ Branch GL: DR 5000, CR 5000 = BALANCED  │    │ Branch GL: DR 5000, CR 5000 = BALANCED │
└─────────────────────────────────────────┘    └─────────────────────────────────────────┘

Cross-branch clearing net:
  CLR-IB-BR001 balance = +5000 (CR)
  CLR-IB-BR002 balance = -5000 (DR)
  Net clearing = 0  ✅
```

**4 vouchers created instead of 2:**

| Voucher | DR/CR | Account | Branch | Amount | Purpose |
|---|---|---|---|---|---|
| V1 | DR | Rajesh SAV-1001-0001 | BR001 | 5000 | Debit sender |
| V2 | CR | CLR-IB-BR001 | BR001 | 5000 | Credit Branch A clearing |
| V3 | DR | CLR-IB-BR002 | BR002 | 5000 | Debit Branch B clearing |
| V4 | CR | Amit SAV-1003-0001 | BR002 | 5000 | Credit receiver |

### 4.4 Ledger Entry Examples

All 4 entries are immutable, created via `VoucherService.postVoucher()`:

```
LedgerEntry 1: journal=J1, account=SAV-1001-0001, type=DEBIT,  amount=5000, gl=2110, branch=BR001
LedgerEntry 2: journal=J1, account=CLR-IB-BR001,  type=CREDIT, amount=5000, gl=2910, branch=BR001
LedgerEntry 3: journal=J2, account=CLR-IB-BR002,  type=DEBIT,  amount=5000, gl=2910, branch=BR002
LedgerEntry 4: journal=J2, account=SAV-1003-0001, type=CREDIT, amount=5000, gl=2110, branch=BR002
```

**Verification:**
- Per-branch: BR001 debit=5000, credit=5000 ✅; BR002 debit=5000, credit=5000 ✅
- Overall: total debit=10000, total credit=10000 ✅
- Clearing GL 2910 net: DR 5000 + CR 5000 = 0 ✅

### 4.5 Implementation — TransactionService Modification

In `TransactionService.postTransferLedger()`, detect inter-branch and route through clearing:

```java
// PSEUDOCODE — inside postTransferLedger()
Branch sourceBranch = resolveBranch(sourceAccount, poster);
Branch destBranch = resolveBranch(destAccount, poster);

if (sourceBranch.getId().equals(destBranch.getId())) {
    // SAME BRANCH — direct posting (existing behavior)
    postVoucher(transaction, tenant, poster, sourceAccount, VoucherDrCr.DR, ...);
    postVoucher(transaction, tenant, poster, destAccount,   VoucherDrCr.CR, ...);
} else {
    // INTER-BRANCH — route through clearing accounts
    Account sourceClearingAcct = resolveBranchClearingAccount(tenant.getId(), sourceBranch);
    Account destClearingAcct   = resolveBranchClearingAccount(tenant.getId(), destBranch);

    // Branch A entries (source branch stays balanced)
    postVoucher(transaction, tenant, poster, sourceAccount,      VoucherDrCr.DR, ...);
    postVoucher(transaction, tenant, poster, sourceClearingAcct,  VoucherDrCr.CR, ...);

    // Branch B entries (dest branch stays balanced)
    postVoucher(transaction, tenant, poster, destClearingAcct,    VoucherDrCr.DR, ...);
    postVoucher(transaction, tenant, poster, destAccount,         VoucherDrCr.CR, ...);
}
```

### 4.6 EOD Validation — Clearing GL Check

Add to `EodValidationService.validateEod()`:

```java
// NEW EOD CHECK: Inter-branch clearing GL must net to zero
BigDecimal clearingBalance = accountRepository
    .sumBalanceByTenantIdAndAccountType(tenantId, AccountType.CLEARING_ACCOUNT);
if (clearingBalance.compareTo(BigDecimal.ZERO) != 0) {
    errors.add("EOD blocked: Inter-branch clearing GL net balance is "
        + clearingBalance + " for tenant " + tenantId
        + ". All inter-branch transfers must be fully cleared.");
}
```

### 4.7 SQL — Clearing Reconciliation Query

```sql
-- CLEARING-01: Per-branch clearing account balances (should net to zero across branches)
SELECT a.account_number, a.account_name, a.branch_code, a.balance,
       CASE WHEN a.balance = 0 THEN 'SETTLED' ELSE 'PENDING' END AS status
FROM accounts a
WHERE a.account_type = 'CLEARING_ACCOUNT'
  AND a.account_number LIKE 'CLR-IB-%'
ORDER BY a.branch_code;

-- CLEARING-02: Net clearing balance per tenant (MUST be zero)
SELECT a.tenant_id, SUM(a.balance) AS net_clearing,
       CASE WHEN SUM(a.balance) = 0 THEN 'BALANCED' ELSE 'IMBALANCED' END AS status
FROM accounts a
WHERE a.account_type = 'CLEARING_ACCOUNT'
  AND a.account_number LIKE 'CLR-IB-%'
GROUP BY a.tenant_id;

-- CLEARING-03: Clearing entries for a specific business date
SELECT le.business_date, le.entry_type, le.amount, le.gl_account_code,
       a.account_number, a.branch_code, le.narration
FROM ledger_entries le
JOIN accounts a ON a.id = le.account_id
WHERE a.account_type = 'CLEARING_ACCOUNT'
  AND le.business_date = CURRENT_DATE
ORDER BY le.id;
```

### 4.8 Failure Scenario — Incomplete Clearing

**Scenario:** Inter-branch transfer — Branch A leg posts, Branch B leg fails (e.g., Amit's account frozen).

**Without suspense integration:** Transaction rolls back entirely (safe but user-unfriendly).

**With suspense integration (Part 3 + Part 4 combined):**
```
V1 DR: Rajesh SAV-1001-0001 (BR001)     5000  → POSTED
V2 CR: CLR-IB-BR001 (BR001)             5000  → POSTED
V3 DR: CLR-IB-BR002 (BR002)             5000  → POSTED
V4 CR: Amit SAV-1003-0001 (BR002)       5000  → FAILS (FROZEN)
V4-SUSP CR: SUSP-T001 (BR002)           5000  → POSTED to suspense
```

State:
- Clearing GL: CLR-IB-BR001=+5000, CLR-IB-BR002=-5000 → net=0 ✅
- Suspense GL: +5000 → **blocks EOD** until corrected
- Rajesh debited, Amit not credited — suspense holds the difference

Correction (after freeze lifted): DR SUSP-T001, CR Amit SAV-1003-0001 → clears suspense.

### 4.9 DataInitializer Seeding

Add to `initCustomersAndAccounts()`:

```java
// Per-branch inter-branch clearing accounts
createAccount("CLR-IB-HQ001", "Inter-Branch Clearing - HQ",
    AccountType.CLEARING_ACCOUNT, LedgerAccountType.CLEARING_ACCOUNT,
    BigDecimal.ZERO, "INR", hqBranch, null, null, "2910", null);

createAccount("CLR-IB-BR001", "Inter-Branch Clearing - Downtown",
    AccountType.CLEARING_ACCOUNT, LedgerAccountType.CLEARING_ACCOUNT,
    BigDecimal.ZERO, "INR", branch1, null, null, "2910", null);

createAccount("CLR-IB-BR002", "Inter-Branch Clearing - Uptown",
    AccountType.CLEARING_ACCOUNT, LedgerAccountType.CLEARING_ACCOUNT,
    BigDecimal.ZERO, "INR", branch2, null, null, "2910", null);
```

### 4.10 Integration Summary

| Component | Change Required |
|---|---|
| `DataInitializer` | Seed GL `2910` + per-branch `CLR-IB-*` clearing accounts |
| `TransactionService.postTransferLedger()` | Detect inter-branch; route through 4-voucher clearing flow |
| `EodValidationService` | Add clearing GL net ≠ 0 check |
| `AccountRepository` | Add `sumBalanceByTenantIdAndAccountType()` query |
| Audit SQL Pack | CLEARING-01/02/03 queries added |

**Invariants preserved:**
- ✅ Each branch independently balances (DR == CR within branch)
- ✅ Clearing GL nets to zero across all branches
- ✅ Ledger entries remain immutable (4 entries instead of 2, all append-only)
- ✅ EOD blocks if clearing is non-zero
- ✅ Integrates with suspense (Part 3) for partial failure scenarios
- ✅ Batch totals updated correctly (4 vouchers = 4 batch total updates)

---

*End of Part 4. Part 5 follows.*

---

## PART 5 — FRAUD CONTROL & RISK MATRIX (STRICT RBI INSPECTION VIEW)

This section is written as an **internal RBI inspection team** review. It assumes the system will undergo regulatory audit and that **control failures will be treated as audit observations**.

**Legend**
- **Likelihood:** L/M/H (probability of occurrence)
- **Impact:** L/M/H (financial + reputational + regulatory impact)
- **Risk rating:** derived (guidance only) — RBI inspection focuses on evidence and enforceability.

### 5.1 Fraud & Abuse Risk Matrix (10 items)

| Risk ID | Risk / Abuse Scenario | Likelihood | Impact | Existing Controls (Evidence in Code) | Gaps / Audit Observations (Strict) | Mandatory Remediation / Recommendation |
|---|---|---:|---:|---|---|---|
| FR-01 | **Brute-force / credential stuffing** against privileged roles (ADMIN, CHECKER) | H | H | Account lockout after 5 failures (`src/main/java/com/ledgora/security/AuthenticationFailureListener.java:30-46`, `src/main/java/com/ledgora/security/CustomUserDetailsService.java`) | Lockout is permanent until admin unlock (policy-dependent). No IP throttling, CAPTCHA, or adaptive MFA. | Add rate limiting (per-IP + per-username), CAPTCHA after N attempts, and MFA for privileged roles. Produce evidence logs (SIEM). |
| FR-02 | **Session hijack / replay** (shared terminals, teller machines) | M | H | Session fixation protection + single session per user (`src/main/java/com/ledgora/config/SecurityConfig.java:136-142`); session timeout configured (`src/main/resources/application.properties:33-34`, `src/main/resources/application-prod.properties:22-23`) | No device binding. No forced re-auth for high-risk actions (authorize/cancel/EOD). | Add step-up authentication for: voucher authorize/post/cancel, EOD run, tenant switch. Consider short idle timeout for teller roles. |
| FR-03 | **CSRF / forced transaction submission** | M | H | CSRF enabled with cookie token repository (`src/main/java/com/ledgora/config/SecurityConfig.java:90-94`) | H2 console is excluded from CSRF and must never be enabled in prod. No explicit CSRF validation evidence for custom AJAX calls (if any). | Ensure prod profile disables H2. Verify all state-changing endpoints are POST with CSRF token in JSP forms. Add security tests. |
| FR-04 | **Maker–checker bypass** (maker approves own transaction) | M | H | Enforced in transaction approval (`TransactionService.java:386-392`, `480-486`) and voucher authorization (`VoucherService.java:253-255`). **STP flow uses dedicated `SYSTEM_AUTO` pseudo-user** as checker — `GovernanceException` thrown if missing, no fallback to maker (commit `04ad39db`). | ✅ **RESOLVED.** SYSTEM_AUTO with ROLE_SYSTEM seeded in DataInitializer. AUDIT-12 SQL validates no maker==checker for STP vouchers. | ✅ Resolved |
| FR-05 | **High-value transactions executed without checker due to misconfigured policies** | M | H | Approval policy engine is fail-safe: missing policy => requires approval (`src/main/java/com/ledgora/approval/service/ApprovalPolicyService.java:76-81`) | If policies are configured incorrectly (wide ranges with autoAuthorize=true), very high-value transfers could be auto-approved. No “hard ceiling” per role/channel. | Implement hard caps: per-role/per-channel absolute limits enforced in service layer (not only policy table). Require dual authorization above threshold. |
| FR-06 | **Velocity / rapid-fire fraud** (multiple withdrawals/transfers quickly) | H | H | None observed in code; only idempotency for exact client references (`src/main/java/com/ledgora/transaction/service/TransactionService.java:834-854`) | No per-account/per-user velocity controls. RBI audit will flag absence of fraud monitoring controls as a gap. | Add velocity rules (e.g., max N txns / time-window, max cumulative amount) and alerting + auto-block. Store decisions in audit logs. |
| FR-07 | **Duplicate / replay transactions** due to client retries or channel faults | M | M/H | Idempotency checks via `clientReferenceId:channel` + IdempotencyService (`src/main/java/com/ledgora/transaction/service/TransactionService.java:834-854`) | If clientReferenceId is missing, there is no deduplication. Near-duplicate detection is only via SQL audit pack (Part 2). | Make client reference mandatory for external channels (ONLINE/MOBILE/ATM). Add server-generated idempotency keys per request. |
| FR-08 | ~~**Unauthorized account access (IDOR)**~~ — customer views another customer’s account history | M | H | Tenant isolation enforced in service methods (`src/main/java/com/ledgora/transaction/service/TransactionService.java:636-659`), controller validates account format (`src/main/java/com/ledgora/transaction/controller/TransactionController.java:175-185`) | Controller itself notes missing ownership validation for CUSTOMER users (`src/main/java/com/ledgora/transaction/controller/TransactionController.java:163-173`). This is a direct RBI observation (privacy + fraud). | Enforce account ownership for CUSTOMER role: accountNumber must map to the authenticated customer only. Add negative tests. |
| FR-09 | **Reversal misuse** (fraudulent cancellation to conceal theft) | M | H | Cancellation restricted to OPEN day + no backdated reversal (`src/main/java/com/ledgora/voucher/service/VoucherService.java:403-416`) | Cancel action currently sets reversal.maker = cancelledBy and auto-posts reversal if original posted (`src/main/java/com/ledgora/voucher/service/VoucherService.java:452-466`). This is effectively “single-person reversal” unless further gated in controller/security. | Require maker-checker for cancellations above threshold and for all reversals in production. Add separate REVERSAL approval workflow and audit evidence. |
| FR-10 | **Audit trail integrity break** — ledger entries not traceable to originating transaction | M | H | Voucher linked to transaction at creation (`TransactionService.java:779-797`). `findOrCreateTransaction()` **now checks `voucher.getTransaction()` before creating synthetic** (FR-10 fix in `VoucherService.java:645-651`). Reversal vouchers carry `original.getTransaction()` FK. | ✅ **RESOLVED.** `findOrCreateTransaction` prioritizes voucher's own transaction FK. FRAUD-08 SQL detects any remaining mismatches. | ✅ Resolved |

### 5.2 Risk Acceptance / Exception Register Template

RBI inspectors will ask: "Where is the risk acceptance record for each known gap?"

The bank's CISO / CRO must maintain a signed register for every open item. Template:

| Reg # | Risk ID | Gap Description | Compensating Control (if any) | Risk Owner | Accepted By (Name + Designation) | Acceptance Date | Review-By Date | Status |
|---|---|---|---|---|---|---|---|---|
| ~~RAR-001~~ | ~~FR-06~~ | ~~Velocity monitoring absent~~ | ~~`VelocityFraudEngine` now enforces 60-min velocity limits with account freeze + FraudAlert.~~ | Head — Operations | *(CISO signature)* | *(date)* | — | ✅ CLOSED (PR #41 — Velocity Fraud Engine) |
| ~~RAR-002~~ | ~~FR-05~~ | ~~No hard ceiling per role~~ | ~~`HardTransactionCeilingService` now enforces absolute hard ceilings from `hard_transaction_limits` table. No role bypass.~~ | Head — Compliance | *(CRO signature)* | *(date)* | — | ✅ CLOSED (PR #41 — Hard Ceiling) |
| ~~RAR-003~~ | ~~FR-04~~ | ~~STP checker = maker~~ | ~~SYSTEM_AUTO now enforced with GovernanceException~~ | Head — IT | *(CISO signature)* | *(date)* | — | ✅ CLOSED (commit `04ad39db`) |
| ~~RAR-004~~ | ~~FR-08~~ | ~~IDOR — CUSTOMER account history~~ | ~~Ownership check now enforced in `TransactionController.transactionHistory()` for CUSTOMER role~~ | Head — IT Security | *(CRO signature)* | *(date)* | — | ✅ CLOSED (commit `6bb7667f` — stopgap, name-based matching) |
| ~~RAR-005~~ | ~~FR-10~~ | ~~Reversal audit trail linkage gap~~ | ~~`findOrCreateTransaction` now checks `voucher.getTransaction()` first~~ | Head — IT | *(CTO signature)* | *(date)* | — | ✅ CLOSED (PR #40 — FR-10 fix) |

**Rules:**
- Every OPEN item must have a review-by date ≤ 90 days.
- Items not remediated by review-by date escalate to the Board's IT Sub-Committee.
- Closed items remain in the register for 3 years (RBI IS Audit evidence retention).

### 5.3 Fraud Monitoring SQL Pack (10 queries)

All queries are read-only, safe for production, and compatible with H2 (dev) and SQL Server (prod).
These are **fraud-focused** — distinct from the ledger integrity pack in Part 2.
Recommended execution: daily by Operations + weekly by Internal Audit.

#### FRAUD-01: High-value transactions above policy ceiling

```sql
-- FRAUD-01: Transactions above a configurable threshold (adjust 500000 per bank policy)
-- Expected: all rows should have checker_id IS NOT NULL (dual authorization)
SELECT t.id, t.transaction_ref, t.transaction_type, t.amount, t.currency,
       t.channel, t.status, t.business_date,
       m.username AS maker, c.username AS checker,
       CASE
           WHEN t.checker_id IS NULL AND t.status = 'COMPLETED'
           THEN 'ALERT — HIGH VALUE WITHOUT CHECKER'
           ELSE 'OK'
       END AS fraud_flag
FROM transactions t
LEFT JOIN users m ON m.id = t.maker_id
LEFT JOIN users c ON c.id = t.checker_id
WHERE t.amount >= 500000
  AND t.status != 'REJECTED'
ORDER BY t.amount DESC, t.business_date DESC;
```

#### FRAUD-02: Velocity — accounts with excessive transaction count in single day

```sql
-- FRAUD-02: Accounts with > 10 transactions on same business date
-- Expected: review each match; flag accounts with unusual spikes
SELECT a.account_number, a.account_name, t.business_date,
       COUNT(*) AS txn_count,
       SUM(t.amount) AS total_amount,
       CASE WHEN COUNT(*) > 20 THEN 'HIGH ALERT' ELSE 'REVIEW' END AS severity
FROM transactions t
JOIN accounts a ON (a.id = t.source_account_id OR a.id = t.destination_account_id)
WHERE t.status != 'REJECTED'
GROUP BY a.account_number, a.account_name, t.business_date
HAVING COUNT(*) > 10
ORDER BY txn_count DESC, t.business_date DESC;
```

#### FRAUD-03: Velocity — users with excessive transaction creation in single day

```sql
-- FRAUD-03: Users initiating > 20 transactions on same business date
-- Expected: teller volume varies; flag outliers vs. peer average
SELECT u.username, u.branch_code, t.business_date,
       COUNT(*) AS txn_count,
       SUM(t.amount) AS total_amount
FROM transactions t
JOIN users u ON u.id = t.maker_id
WHERE t.status != 'REJECTED'
GROUP BY u.username, u.branch_code, t.business_date
HAVING COUNT(*) > 20
ORDER BY txn_count DESC;
```

#### FRAUD-04: Reversal frequency by user (potential cancellation abuse)

```sql
-- FRAUD-04: Users with high reversal/cancellation counts
-- Expected: reversals should be rare; investigate patterns
SELECT u.username, u.branch_code,
       COUNT(*) AS reversal_count,
       SUM(v.transaction_amount) AS total_reversed_amount,
       MIN(v.posting_date) AS first_reversal,
       MAX(v.posting_date) AS last_reversal
FROM vouchers v
JOIN users u ON u.id = v.maker_id
WHERE v.cancel_flag = 'Y'
   OR v.narration LIKE 'REVERSAL%'
GROUP BY u.username, u.branch_code
HAVING COUNT(*) > 3
ORDER BY reversal_count DESC;
```

#### FRAUD-05: Same-day create-and-cancel pattern (round-tripping)

```sql
-- FRAUD-05: Vouchers created and cancelled on the same day by the same user
-- Expected: 0 rows outside normal correction workflows
SELECT v.id AS voucher_id, v.voucher_number,
       v.dr_cr, v.transaction_amount, v.posting_date,
       m.username AS maker,
       v.narration,
       rv.id AS reversal_voucher_id, rv.voucher_number AS reversal_number
FROM vouchers v
JOIN vouchers rv ON rv.reversal_of_voucher_id = v.id
JOIN users m ON m.id = v.maker_id
WHERE v.cancel_flag = 'Y'
  AND v.posting_date = rv.posting_date
  AND v.maker_id = rv.maker_id
ORDER BY v.posting_date DESC, v.transaction_amount DESC;
```

#### FRAUD-06: Transactions on frozen / inactive accounts (bypass detection)

```sql
-- FRAUD-06: Completed transactions against non-ACTIVE or frozen accounts
-- Expected: 0 rows (should have been blocked by service-layer validation)
SELECT t.id, t.transaction_ref, t.transaction_type, t.amount,
       t.status, t.business_date,
       a.account_number, a.status AS account_status, a.freeze_level,
       CASE
           WHEN a.status != 'ACTIVE' THEN 'INACTIVE ACCOUNT'
           WHEN a.freeze_level != 'NONE' THEN 'FROZEN ACCOUNT'
       END AS violation_type
FROM transactions t
JOIN accounts a ON (a.id = t.source_account_id OR a.id = t.destination_account_id)
WHERE t.status = 'COMPLETED'
  AND (a.status != 'ACTIVE' OR a.freeze_level != 'NONE')
ORDER BY t.business_date DESC;
```

#### FRAUD-07: Off-hours transaction detection (outside business day window)

```sql
-- FRAUD-07: Transactions created outside normal business day OPEN window
-- Expected: 0 rows for TELLER channel; system/batch channels may have entries
SELECT t.id, t.transaction_ref, t.transaction_type,
       t.amount, t.channel, t.created_at,
       t.business_date, ten.day_status,
       m.username AS maker
FROM transactions t
JOIN tenants ten ON ten.id = t.tenant_id
JOIN users m ON m.id = t.maker_id
WHERE t.status = 'COMPLETED'
  AND t.channel IN ('TELLER', 'ONLINE', 'MOBILE')
  AND ten.day_status != 'OPEN'
ORDER BY t.created_at DESC;
```

#### FRAUD-08: Voucher–transaction linkage mismatch (audit trail break)

```sql
-- FRAUD-08: Vouchers where voucher.transaction_id differs from ledger_entry.transaction_id
-- Expected: 0 rows — any mismatch is a severe audit observation (see FR-10)
SELECT v.id AS voucher_id, v.voucher_number,
       v.transaction_id AS voucher_txn_id,
       le.transaction_id AS ledger_txn_id,
       vt.transaction_ref AS voucher_txn_ref,
       lt.transaction_ref AS ledger_txn_ref,
       v.transaction_amount, v.dr_cr, v.posting_date
FROM vouchers v
JOIN ledger_entries le ON le.id = v.ledger_entry_id
LEFT JOIN transactions vt ON vt.id = v.transaction_id
LEFT JOIN transactions lt ON lt.id = le.transaction_id
WHERE v.post_flag = 'Y'
  AND v.cancel_flag = 'N'
  AND (v.transaction_id IS NULL
       OR le.transaction_id IS NULL
       OR v.transaction_id != le.transaction_id)
ORDER BY v.posting_date DESC;
```

#### FRAUD-09: Privileged role login anomalies (lockouts + rapid attempts)

```sql
-- FRAUD-09: Users with high failed login attempts or currently locked
-- Expected: review each locked account; investigate patterns
SELECT u.id, u.username, u.branch_code, u.is_locked,
       u.failed_login_attempts, u.last_login,
       u.is_active,
       CASE
           WHEN u.is_locked = TRUE THEN 'LOCKED — INVESTIGATE'
           WHEN u.failed_login_attempts >= 3 THEN 'NEAR THRESHOLD'
           ELSE 'OK'
       END AS status
FROM users u
WHERE u.failed_login_attempts > 0
   OR u.is_locked = TRUE
ORDER BY u.failed_login_attempts DESC, u.is_locked DESC;
```

#### FRAUD-10: Dormant account reactivation with immediate high-value transaction

```sql
-- FRAUD-10: Accounts with no transactions for 90+ days followed by a high-value transaction
-- Expected: review each match; potential money laundering pattern
SELECT a.account_number, a.account_name, a.status,
       MAX(t_old.business_date) AS last_activity_before,
       t_new.business_date AS reactivation_date,
       t_new.transaction_ref, t_new.amount, t_new.transaction_type,
       DATEDIFF(DAY, MAX(t_old.business_date), t_new.business_date) AS dormancy_days
FROM accounts a
JOIN transactions t_new ON (t_new.source_account_id = a.id OR t_new.destination_account_id = a.id)
LEFT JOIN transactions t_old ON (t_old.source_account_id = a.id OR t_old.destination_account_id = a.id)
                              AND t_old.id != t_new.id
                              AND t_old.business_date < t_new.business_date
WHERE t_new.status = 'COMPLETED'
  AND t_new.amount >= 100000
GROUP BY a.account_number, a.account_name, a.status,
         t_new.business_date, t_new.transaction_ref, t_new.amount, t_new.transaction_type
HAVING DATEDIFF(DAY, MAX(t_old.business_date), t_new.business_date) >= 90
ORDER BY dormancy_days DESC, t_new.amount DESC;
```

### 5.4 Fraud Monitoring SQL Execution Checklist

| # | Query | Run Frequency | Acceptable Result | Action on Alert |
|---|---|---|---|---|
| FRAUD-01 | High-value without checker | Daily | 0 rows without checker | Escalate to Compliance; block account if unauthorized |
| FRAUD-02 | Account velocity spike | Daily | < threshold per policy | Flag account for review; consider temporary freeze |
| FRAUD-03 | User velocity spike | Daily | Within peer norms | Review user activity; check for collusion |
| FRAUD-04 | Reversal frequency by user | Weekly | < 3 per user per month | Investigate maker; check for kickback schemes |
| FRAUD-05 | Same-day round-tripping | Daily | 0 rows | Immediate investigation; potential embezzlement |
| FRAUD-06 | Frozen/inactive account txn | Daily | 0 rows | Root cause analysis; service-layer bug if found |
| FRAUD-07 | Off-hours transactions | Daily | 0 for TELLER/ONLINE/MOBILE | Check for after-hours access; policy violation |
| FRAUD-08 | Voucher–txn linkage break | Daily | 0 rows | **Severe** — fix `findOrCreateTransaction`; data remediation |
| FRAUD-09 | Login anomalies | Daily | Review each locked account | Investigate credential compromise; reset + MFA |
| FRAUD-10 | Dormant reactivation | Weekly | Review all matches | KYC re-verification; STR filing if warranted |

### 5.5 Minimum Fraud Control Evidence Pack (What auditors will ask for)

1. **Maker-checker evidence:**
   - Extract of transactions/vouchers showing maker != checker for manual approvals.
   - Evidence that same-user approval is either blocked or explicitly SYSTEM_AUTO.
   - SQL: AUDIT-08 (Part 2) + FRAUD-01 (this section).

2. **Velocity + high-value enforcement evidence (currently missing in code):**
   - Policies and service-layer hard limits.
   - Alert logs for breaches.
   - SQL: FRAUD-01, FRAUD-02, FRAUD-03.

3. **Customer privacy evidence:**
   - Proof that CUSTOMER cannot access other customer accounts (server-side ownership checks).
   - Negative test results (attempted IDOR returns 403/404).

4. **Reversal governance evidence:**
   - Proof that reversals require independent authorization (recommended change).
   - SQL: FRAUD-04, FRAUD-05.

5. **Risk acceptance register:**
   - Signed RAR per §5.2 template for all open gaps.
   - Board IT Sub-Committee minutes acknowledging residual risk.

---

*End of Part 5.*
