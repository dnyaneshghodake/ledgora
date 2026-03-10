# Ledgora — Concurrency Model Validation Checklist

**Version:** 1.0
**Baseline:** PR #43 — Feature Enhancement
**Endpoint:** `GET /diagnostics/concurrency-audit` (ADMIN, stress profile)

## Purpose

Programmatically validate that all CBS financial invariants hold after concurrent load testing. This is the **final verification step** after running stress tests (EOD performance, lock contention, deadlock simulation, production load).

## 11 Validation Checks

| # | Check | SQL Pattern | What it detects |
|---|---|---|---|
| 1 | **Ledger balanced** | `SUM(DEBIT) == SUM(CREDIT)` across all `ledger_entries` | Double-entry violation — most critical |
| 2 | **Clearing GL zero** | `SUM(balance) WHERE account_type='CLEARING_ACCOUNT'` | Unsettled inter-branch transfers |
| 3 | **Suspense GL zero** | `SUM(balance) WHERE account_type='SUSPENSE_ACCOUNT'` | Unresolved suspense cases |
| 4 | **No negative balances** | `COUNT WHERE balance < 0 AND type IN (SAVINGS, CURRENT)` | Overdraft from concurrent withdrawals |
| 5 | **No orphan entries** | `COUNT WHERE transaction_id IS NULL` | Ledger entries without transaction — posting bug |
| 6 | **No stuck EOD** | `COUNT WHERE status='RUNNING'` | EOD crash without recovery |
| 7 | **No stale vouchers** | `COUNT WHERE auth=Y, post=N, age > 30min` | Authorized but never posted — stuck in pipeline |
| 8 | **No duplicate voucher#** | `GROUP BY voucher_number HAVING COUNT > 1` | Scroll sequence concurrency failure |
| 9 | **No partial IBT reversals** | IBT transactions with some-but-not-all vouchers cancelled | Partial reversal violating IBT atomicity |
| 10 | **All IBTs have 4 vouchers** | Non-FAILED IBTs with voucher count ≠ 4 | Incomplete IBT posting |
| 11 | **All batches balanced** | CLOSED/SETTLED batches with `total_debit != total_credit` | Batch integrity violation |

## Usage

```bash
# After any stress test run:
curl http://localhost:8080/diagnostics/concurrency-audit -H "Authorization: ..."
```

## Expected Response

```json
{
  "financialIntegrity": true,
  "ledgerBalanced": true,
  "clearingGlZero": true,
  "suspenseGlZero": true,
  "noNegativeBalances": true,
  "noOrphanEntries": true,
  "noStuckEodProcesses": true,
  "noStalePendingVouchers": true,
  "noDuplicateVoucherNumbers": true,
  "noPartialIbtReversals": true,
  "allIbtHaveFourVouchers": true,
  "allBatchesBalanced": true,
  "orphanEntryCount": 0,
  "negativeBalanceCount": 0,
  "stuckEodCount": 0,
  "stalePendingVoucherCount": 0,
  "duplicateVoucherNumberCount": 0,
  "totalChecks": 11,
  "passedChecks": 11,
  "violations": []
}
```

## Integration with Stress Tests

Recommended workflow:

1. `POST /stress/load` — generate production-style load
2. `POST /stress/lock-contention` — test concurrent access
3. `POST /stress/deadlock` — test deadlock recovery
4. `POST /stress/chaos-eod` — test EOD crash recovery
5. **`GET /diagnostics/concurrency-audit`** — verify no invariant violated

If `financialIntegrity: false`, inspect the `violations` array for specific failure codes.

## Failure Response Codes

| Code | Severity | Meaning |
|---|---|---|
| `LEDGER_IMBALANCED` | **CRITICAL** | Double-entry broken — block all operations |
| `CLEARING_GL_NON_ZERO` | HIGH | Unsettled IBTs — blocks EOD |
| `SUSPENSE_GL_NON_ZERO` | HIGH | Unresolved suspense — blocks EOD |
| `NEGATIVE_BALANCE` | HIGH | Concurrent overdraft — investigate locking |
| `ORPHAN_ENTRIES` | HIGH | Posting engine defect |
| `STUCK_EOD` | MEDIUM | EOD crash without recovery |
| `STALE_VOUCHERS` | MEDIUM | Pipeline stall |
| `DUPLICATE_VOUCHER_NUMBERS` | HIGH | Scroll sequence lock failure |
| `PARTIAL_IBT_REVERSAL` | HIGH | IBT atomicity violation |
| `IBT_VOUCHER_COUNT` | HIGH | Incomplete IBT posting |
| `UNBALANCED_BATCHES` | HIGH | Batch integrity violation |
