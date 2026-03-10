-- ============================================================
-- LEDGORA — IBT (Inter-Branch Transfer) AUDIT SQL PACK
-- RBI Inspection Ready | CBS-Grade Accounting Discipline
-- Compatible with H2 (dev) and SQL Server (prod)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- IBT-AUDIT-01: Cross-branch transactions (all IBT activity)
-- Purpose: List all transactions where source and destination
--          are at different branches.
-- Expected: All should have corresponding IBC transfer records.
-- ─────────────────────────────────────────────────────────────
SELECT t.id AS transaction_id,
       t.transaction_ref,
       t.transaction_type,
       t.amount,
       t.currency,
       t.status,
       t.business_date,
       sa.account_number AS source_account,
       sa.branch_code    AS source_branch,
       da.account_number AS dest_account,
       da.branch_code    AS dest_branch,
       m.username         AS maker
FROM transactions t
JOIN accounts sa ON sa.id = t.source_account_id
JOIN accounts da ON da.id = t.destination_account_id
LEFT JOIN users m ON m.id = t.maker_id
WHERE sa.branch_code != da.branch_code
  AND t.status != 'REJECTED'
ORDER BY t.business_date DESC, t.amount DESC;


-- ─────────────────────────────────────────────────────────────
-- IBT-AUDIT-02: Clearing GL net balance per tenant (MUST be 0)
-- Purpose: CBS Standard — clearing accounts must net to zero.
-- Expected: net_clearing = 0 for every tenant.
-- Action:   If non-zero, EOD must be blocked.
-- ─────────────────────────────────────────────────────────────
SELECT a.tenant_id,
       SUM(a.balance)    AS net_clearing,
       COUNT(a.id)       AS clearing_account_count,
       CASE
           WHEN SUM(a.balance) = 0 THEN 'BALANCED'
           ELSE 'IMBALANCED — BLOCK EOD'
       END AS status
FROM accounts a
WHERE (a.account_number LIKE 'IBC-OUT-%'
    OR a.account_number LIKE 'IBC-IN-%')
GROUP BY a.tenant_id;


-- ─────────────────────────────────────────────────────────────
-- IBT-AUDIT-03: Per-branch clearing account balances
-- Purpose: Verify each branch's clearing accounts individually.
-- Expected: Each IBC-OUT + IBC-IN pair should net to zero
--           across all branches.
-- ─────────────────────────────────────────────────────────────
SELECT a.account_number,
       a.account_name,
       a.branch_code,
       a.balance,
       a.tenant_id,
       CASE
           WHEN a.balance = 0 THEN 'SETTLED'
           ELSE 'PENDING'
       END AS status
FROM accounts a
WHERE a.account_number LIKE 'IBC-OUT-%'
   OR a.account_number LIKE 'IBC-IN-%'
ORDER BY a.tenant_id, a.branch_code, a.account_number;


-- ─────────────────────────────────────────────────────────────
-- IBT-AUDIT-04: IBT voucher count asymmetry detection
-- Purpose: Each IBT transaction must have exactly 4 vouchers
--          (2 per branch). Any mismatch = posting engine defect.
-- Expected: 0 rows returned.
-- ─────────────────────────────────────────────────────────────
SELECT v.transaction_id,
       COUNT(v.id)         AS voucher_count,
       t.transaction_ref,
       t.amount,
       t.business_date,
       CASE
           WHEN COUNT(v.id) = 4 THEN 'OK'
           WHEN COUNT(v.id) < 4 THEN 'INCOMPLETE — MISSING VOUCHERS'
           ELSE 'EXCESS — EXTRA VOUCHERS'
       END AS audit_result
FROM vouchers v
JOIN transactions t ON t.id = v.transaction_id
WHERE t.transaction_type = 'TRANSFER'
  AND EXISTS (
      SELECT 1 FROM inter_branch_transfers ibt
      WHERE ibt.reference_transaction_id = t.id
  )
GROUP BY v.transaction_id, t.transaction_ref, t.amount, t.business_date
HAVING COUNT(v.id) != 4
ORDER BY t.business_date DESC;


-- ─────────────────────────────────────────────────────────────
-- IBT-AUDIT-05: Unsettled IBC transfers (blocks EOD)
-- Purpose: All IBC transfers must be SETTLED or FAILED before EOD.
-- Expected: 0 rows for current business date at EOD time.
-- ─────────────────────────────────────────────────────────────
SELECT ibt.id             AS transfer_id,
       ibt.status,
       ibt.amount,
       ibt.currency,
       ibt.business_date,
       fb.branch_code     AS from_branch,
       tb.branch_code     AS to_branch,
       ibt.narration,
       ibt.failure_reason,
       cb.username         AS created_by,
       ibt.created_at
FROM inter_branch_transfers ibt
JOIN branches fb ON fb.id = ibt.from_branch_id
JOIN branches tb ON tb.id = ibt.to_branch_id
LEFT JOIN users cb ON cb.id = ibt.created_by_id
WHERE ibt.status NOT IN ('SETTLED', 'FAILED')
ORDER BY ibt.business_date DESC, ibt.amount DESC;


-- ─────────────────────────────────────────────────────────────
-- IBT-AUDIT-06: IBT partial reversal detection (asymmetric)
-- Purpose: Detect IBT transactions where some vouchers are
--          cancelled but others are not. Partial reversal of
--          an IBT is strictly prohibited.
-- Expected: 0 rows.
-- ─────────────────────────────────────────────────────────────
SELECT v.transaction_id,
       t.transaction_ref,
       COUNT(v.id)                                           AS total_vouchers,
       SUM(CASE WHEN v.cancel_flag = 'Y' THEN 1 ELSE 0 END) AS cancelled_count,
       SUM(CASE WHEN v.cancel_flag = 'N' THEN 1 ELSE 0 END) AS active_count,
       t.business_date,
       'PARTIAL REVERSAL — GOVERNANCE VIOLATION' AS audit_result
FROM vouchers v
JOIN transactions t ON t.id = v.transaction_id
WHERE EXISTS (
    SELECT 1 FROM inter_branch_transfers ibt
    WHERE ibt.reference_transaction_id = t.id
)
GROUP BY v.transaction_id, t.transaction_ref, t.business_date
HAVING SUM(CASE WHEN v.cancel_flag = 'Y' THEN 1 ELSE 0 END) > 0
   AND SUM(CASE WHEN v.cancel_flag = 'N' THEN 1 ELSE 0 END) > 0
ORDER BY t.business_date DESC;


-- ─────────────────────────────────────────────────────────────
-- IBT-AUDIT-07: Branch GL mapping configuration check
-- Purpose: Verify all active branches have clearing GL mappings.
-- Expected: Every active branch should have a mapping.
-- ─────────────────────────────────────────────────────────────
SELECT b.branch_code,
       b.name          AS branch_name,
       b.is_active,
       bgl.clearing_gl_code,
       bgl.ibc_out_account_number,
       bgl.ibc_in_account_number,
       CASE
           WHEN bgl.id IS NULL AND b.is_active = TRUE
           THEN 'MISSING MAPPING — CONFIGURE'
           ELSE 'OK'
       END AS config_status
FROM branches b
LEFT JOIN branch_gl_mappings bgl ON bgl.branch_id = b.id AND bgl.is_active = TRUE
ORDER BY b.branch_code;


-- ─────────────────────────────────────────────────────────────
-- IBT-AUDIT-08: IBT velocity report — transfers per branch/day
-- Purpose: Detect unusual IBT volume for fraud monitoring.
-- Expected: Review branches with high transfer counts.
-- ─────────────────────────────────────────────────────────────
SELECT fb.branch_code     AS from_branch,
       ibt.business_date,
       COUNT(ibt.id)       AS transfer_count,
       SUM(ibt.amount)     AS total_amount,
       CASE
           WHEN COUNT(ibt.id) > 20 THEN 'HIGH ALERT'
           WHEN COUNT(ibt.id) > 10 THEN 'REVIEW'
           ELSE 'NORMAL'
       END AS severity
FROM inter_branch_transfers ibt
JOIN branches fb ON fb.id = ibt.from_branch_id
WHERE ibt.status != 'FAILED'
GROUP BY fb.branch_code, ibt.business_date
HAVING COUNT(ibt.id) > 5
ORDER BY transfer_count DESC, ibt.business_date DESC;


-- ─────────────────────────────────────────────────────────────
-- EXECUTION CHECKLIST
-- ─────────────────────────────────────────────────────────────
-- #  Query          Run Frequency    Acceptable Result           Action
-- 01 Cross-branch   Daily            All have IBC records        Investigate orphans
-- 02 Clearing net   Daily (pre-EOD)  net_clearing = 0            BLOCK EOD
-- 03 Per-branch     Daily (pre-EOD)  Review each balance         Reconcile non-zero
-- 04 Voucher count  Daily            0 rows                      Fix posting engine
-- 05 Unsettled IBC  Daily (pre-EOD)  0 rows for current date     Settle or fail before EOD
-- 06 Partial rev    Weekly           0 rows                      Governance violation
-- 07 Config check   Monthly          All branches mapped         Configure missing
-- 08 Velocity       Daily            Review high-volume branches  Fraud investigation
