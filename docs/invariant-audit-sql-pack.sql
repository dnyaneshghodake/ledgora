-- =============================================================================
-- Ledgora CBS — Financial Invariant Audit SQL Pack
-- =============================================================================
-- Purpose: Formal verification of CBS financial invariants.
-- Run after stress tests or as part of daily pre-EOD checks.
-- All queries are read-only and safe for production.
-- Compatible with H2 (dev) and SQL Server (prod).
-- =============================================================================

-- =============================================
-- INV-01: Global Ledger Balance Proof
-- =============================================
-- Invariant: SUM(all debits) == SUM(all credits) across entire ledger
-- Violation: CRITICAL — double-entry broken
SELECT
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) AS total_debits,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS total_credits,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END)
      - SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS difference,
    CASE
        WHEN SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END)
           = SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END)
        THEN 'INVARIANT HOLDS' ELSE 'INVARIANT VIOLATED'
    END AS result
FROM ledger_entries;

-- =============================================
-- INV-02: Account Balance vs Ledger Reconciliation
-- =============================================
-- Invariant: Account.balance == SUM(credits) - SUM(debits) per account
-- Violation: Balance cache drift — not critical (cache only) but requires reconciliation
SELECT
    a.id AS account_id,
    a.account_number,
    a.balance AS cached_balance,
    COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END), 0)
      - COALESCE(SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END), 0) AS ledger_balance,
    a.balance - (
        COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END), 0)
      - COALESCE(SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END), 0)
    ) AS drift
FROM accounts a
LEFT JOIN ledger_entries le ON le.account_id = a.id
WHERE a.account_type IN ('SAVINGS', 'CURRENT', 'CUSTOMER_ACCOUNT')
GROUP BY a.id, a.account_number, a.balance
HAVING a.balance != (
    COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END), 0)
  - COALESCE(SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END), 0)
)
ORDER BY ABS(a.balance - (
    COALESCE(SUM(CASE WHEN le.entry_type = 'CREDIT' THEN le.amount ELSE 0 END), 0)
  - COALESCE(SUM(CASE WHEN le.entry_type = 'DEBIT' THEN le.amount ELSE 0 END), 0)
)) DESC;

-- =============================================
-- INV-03: IBT 4-Voucher Integrity
-- =============================================
-- Invariant: Every non-FAILED IBT must have exactly 4 vouchers
-- Violation: Incomplete IBT posting
SELECT
    ibt.id AS ibt_id,
    ibt.status,
    ibt.reference_transaction_id,
    COUNT(v.id) AS voucher_count,
    CASE WHEN COUNT(v.id) = 4 THEN 'OK' ELSE 'VIOLATED' END AS result
FROM inter_branch_transfers ibt
LEFT JOIN vouchers v ON v.transaction_id = ibt.reference_transaction_id
WHERE ibt.status NOT IN ('FAILED')
GROUP BY ibt.id, ibt.status, ibt.reference_transaction_id
HAVING COUNT(v.id) != 4;

-- =============================================
-- INV-04: Clearing GL Net Zero
-- =============================================
-- Invariant: SUM(balance) of all CLEARING_ACCOUNT = 0
SELECT
    SUM(balance) AS clearing_net,
    CASE WHEN SUM(balance) = 0 THEN 'INVARIANT HOLDS' ELSE 'INVARIANT VIOLATED' END AS result
FROM accounts
WHERE account_type = 'CLEARING_ACCOUNT';

-- =============================================
-- INV-05: Suspense GL Zero
-- =============================================
-- Invariant: SUM(balance) of all SUSPENSE_ACCOUNT = 0
SELECT
    SUM(balance) AS suspense_net,
    CASE WHEN SUM(balance) = 0 THEN 'INVARIANT HOLDS' ELSE 'INVARIANT VIOLATED' END AS result
FROM accounts
WHERE account_type = 'SUSPENSE_ACCOUNT';

-- =============================================
-- INV-06: Duplicate Voucher Number Detection
-- =============================================
-- Invariant: voucher_number is globally unique
SELECT voucher_number, COUNT(*) AS occurrences
FROM vouchers
GROUP BY voucher_number
HAVING COUNT(*) > 1;

-- =============================================
-- INV-07: Partial IBT Reversal Detection
-- =============================================
-- Invariant: IBT transactions must have all vouchers cancelled or none
SELECT
    v.transaction_id,
    COUNT(*) AS total_vouchers,
    SUM(CASE WHEN v.cancel_flag = 'Y' THEN 1 ELSE 0 END) AS cancelled_count,
    'PARTIAL REVERSAL' AS violation
FROM vouchers v
JOIN inter_branch_transfers ibt ON ibt.reference_transaction_id = v.transaction_id
GROUP BY v.transaction_id
HAVING SUM(CASE WHEN v.cancel_flag = 'Y' THEN 1 ELSE 0 END) > 0
   AND SUM(CASE WHEN v.cancel_flag = 'Y' THEN 1 ELSE 0 END) < COUNT(*);

-- =============================================
-- INV-08: Multiple Reversal Detection
-- =============================================
-- Invariant: A voucher can only be reversed once
SELECT
    rv.reversal_of_voucher_id AS original_voucher_id,
    COUNT(*) AS reversal_count,
    'MULTIPLE REVERSAL' AS violation
FROM vouchers rv
WHERE rv.reversal_of_voucher_id IS NOT NULL
GROUP BY rv.reversal_of_voucher_id
HAVING COUNT(*) > 1;

-- =============================================
-- INV-09: Batch Balance Integrity
-- =============================================
-- Invariant: CLOSED/SETTLED batches must have total_debit == total_credit
SELECT
    id AS batch_id,
    batch_code,
    status,
    total_debit,
    total_credit,
    total_debit - total_credit AS imbalance
FROM transaction_batches
WHERE status IN ('CLOSED', 'SETTLED')
  AND total_debit != total_credit;

-- =============================================
-- INV-10: Orphan Ledger Entry Detection
-- =============================================
-- Invariant: Every ledger entry must be linked to a transaction
SELECT COUNT(*) AS orphan_count
FROM ledger_entries
WHERE transaction_id IS NULL;
