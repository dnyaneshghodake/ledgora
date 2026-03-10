# Ledgora — Formal Consistency Invariant Verification

**Version:** 1.0
**Baseline:** PR #43 — Feature Enhancement
**SQL Pack:** `docs/invariant-audit-sql-pack.sql`
**Programmatic:** `GET /diagnostics/concurrency-audit` (stress profile)

## Financial Invariants

Ledgora CBS enforces 10 formal financial invariants. Each is verifiable programmatically (via `ConcurrencyAuditService`) and manually (via SQL audit pack).

### Invariant Definitions

| ID | Invariant | Formal Expression | Enforcement Layer |
|---|---|---|---|
| INV-01 | **Ledger Balanced** | `∀t: Σ(debit_entries) = Σ(credit_entries)` | `@Transactional` atomic voucher pairs + `@Immutable` ledger |
| INV-02 | **Balance = Ledger** | `∀a: a.balance = Σ(credits_a) - Σ(debits_a)` | `TransactionService` updates cache after posting; `AccountBalanceReconciliationService` validates every 15 min |
| INV-03 | **IBT = 4 Vouchers** | `∀ibt ∉ FAILED: |vouchers(ibt)| = 4` | `IbtService.validateIbtVoucherCount()` |
| INV-04 | **Clearing Net Zero** | `Σ(balance: CLEARING_ACCOUNT) = 0` | `IbtService.validateClearingGlNetZero()` + EOD gate |
| INV-05 | **Suspense Zero** | `Σ(balance: SUSPENSE_ACCOUNT) = 0` | `SuspenseResolutionService.validateSuspenseAccountBalance()` + EOD gate |
| INV-06 | **Unique Voucher#** | `∀v1,v2: v1.number = v2.number → v1 = v2` | `UNIQUE(voucher_number)` DB constraint + `PESSIMISTIC_WRITE` on scroll sequences |
| INV-07 | **IBT All-or-Nothing** | `∀ibt: cancelled(ibt) = 0 ∨ cancelled(ibt) = |vouchers(ibt)|` | `IbtService.validateFullReversalRequired()` |
| INV-08 | **Single Reversal** | `∀v: |reversals(v)| ≤ 1` | `VoucherService.cancelVoucher()` checks `cancelFlag = 'N'` before reversal |
| INV-09 | **Batch Balanced** | `∀b ∈ {CLOSED,SETTLED}: b.debit = b.credit` | `BatchService.validateBatchClose()` |
| INV-10 | **No Orphans** | `∀le: le.transaction ≠ null` | `VoucherService.postVoucher()` always links to transaction |

### Preventive vs. Detective Controls

| Invariant | Preventive Control (prevents violation) | Detective Control (detects after fact) |
|---|---|---|
| INV-01 | `createVoucherPair()` atomic + `postVoucher()` integrity check | `EodValidationService.validateEod()` + AUDIT-11 SQL |
| INV-02 | `TransactionService` balance update after posting | `AccountBalanceReconciliationService` (15-min scheduled) |
| INV-03 | `IbtService.validateIbtVoucherCount()` inline | INV-03 SQL query |
| INV-04 | `IbtService.validateClearingGlNetZero()` at EOD | Clearing engine dashboard (`GET /clearing/engine`) |
| INV-05 | `SuspenseResolutionService.validateSuspenseForEod()` | Suspense dashboard (`GET /suspense/dashboard`) |
| INV-06 | `UNIQUE` constraint + pessimistic scroll lock | INV-06 SQL query |
| INV-07 | `IbtService.validateFullReversalRequired()` | INV-07 SQL query |
| INV-08 | `cancelVoucher()` guard: `if (cancelFlag == 'Y') throw` | INV-08 SQL query |
| INV-09 | `BatchService.validateBatchClose()` | INV-09 SQL query |
| INV-10 | `postVoucher()` always receives transaction parameter | INV-10 SQL query + `countOrphanEntries()` |

## Verification Methods

### Method 1: Programmatic (POST-stress)

```bash
curl http://localhost:8080/diagnostics/concurrency-audit
```

Returns `financialIntegrity: true` if all 11 checks pass. Maps to invariants:

| ConcurrencyAudit Check | Invariant |
|---|---|
| `ledgerBalanced` | INV-01 |
| `clearingGlZero` | INV-04 |
| `suspenseGlZero` | INV-05 |
| `noNegativeBalances` | INV-02 (partial — detects overdraft) |
| `noOrphanEntries` | INV-10 |
| `noDuplicateVoucherNumbers` | INV-06 |
| `noPartialIbtReversals` | INV-07 |
| `allIbtHaveFourVouchers` | INV-03 |
| `allBatchesBalanced` | INV-09 |

### Method 2: SQL Audit Pack (manual)

Run `docs/invariant-audit-sql-pack.sql` via H2 console or SQL Server Management Studio. Each query returns zero rows if the invariant holds.

### Method 3: EOD Validation (operational)

`EodValidationService.validateEod()` checks INV-01, INV-04, INV-05 before allowing day close. If any fails, EOD is blocked.

## Proof Approach

For each invariant, the proof follows the pattern:

1. **Induction base:** Seed data satisfies invariant (verified by DataInitializer + integration tests)
2. **Induction step:** Every state-changing operation preserves invariant (verified by `@Transactional` atomicity + service-layer guards)
3. **Detective verification:** SQL queries confirm invariant holds after operations (verified by audit pack + concurrency audit)

**INV-01 proof sketch (Ledger Balanced):**
- Base: `createBalancedJournal()` in DataInitializer creates matched DR+CR entries
- Step: `TransactionService.deposit/withdraw/transfer()` creates matched DR+CR voucher pairs via `createVoucherPair()` in single `@Transactional`. If either leg fails, both roll back.
- Detection: `EodValidationService.validateEod()` checks `SUM(debits) == SUM(credits)`. AUDIT-11 SQL provides per-journal proof. `ConcurrencyAuditService` check #1 validates globally.

**INV-06 proof sketch (Unique Voucher#):**
- Base: No vouchers exist at startup
- Step: `VoucherService.getNextScrollNo()` uses `PESSIMISTIC_WRITE` lock on `scroll_sequences(tenant_id, branch_id, posting_date)`. Only one thread can increment the scroll at a time. `UNIQUE(voucher_number)` DB constraint catches any race that escapes the lock.
- Detection: INV-06 SQL query + ConcurrencyAudit check #8.

## Execution Checklist

| When | Run | Expected |
|---|---|---|
| After stress test | `GET /diagnostics/concurrency-audit` | `financialIntegrity: true` |
| Pre-EOD (daily) | `docs/invariant-audit-sql-pack.sql` INV-01 through INV-05 | 0 violations |
| Weekly audit | Full SQL pack (INV-01 through INV-10) | 0 violations |
| After code deployment | Integration test suite + `GET /diagnostics/concurrency-audit` | All pass |
