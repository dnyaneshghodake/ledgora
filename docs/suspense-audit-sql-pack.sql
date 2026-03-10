-- ============================================================
-- LEDGORA — SUSPENSE GL AUDIT SQL PACK
-- CBS Exception Accounting | RBI Inspection Ready
-- Compatible with H2 (dev) and SQL Server (prod)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- SUSP-AUDIT-01: Suspense account balances (must be zero at EOD)
-- Purpose: Direct check of all suspense-type account balances.
-- Expected: balance = 0 for all rows at EOD time.
-- ─────────────────────────────────────────────────────────────
SELECT a.account_number,
       a.account_name,
       a.balance,
       a.tenant_id,
       a.account_type,
       CASE
           WHEN a.balance = 0 THEN 'CLEAR'
           ELSE 'NON-ZERO — RESOLVE BEFORE EOD'
       END AS status
FROM accounts a
WHERE a.account_type = 'SUSPENSE_ACCOUNT'
   OR a.account_type = 'INTERNAL_ACCOUNT'
   OR a.account_number LIKE '%SUSP%'
ORDER BY ABS(a.balance) DESC;


-- ─────────────────────────────────────────────────────────────
-- SUSP-AUDIT-02: Open suspense cases requiring resolution
-- Purpose: List all unresolved parked transactions.
-- Expected: 0 rows at EOD time.
-- ─────────────────────────────────────────────────────────────
SELECT sc.id               AS case_id,
       sc.status,
       sc.amount,
       sc.currency,
       sc.reason_code,
       sc.reason_detail,
       sc.business_date,
       t.transaction_ref,
       ia.account_number    AS intended_account,
       sa.account_number    AS suspense_account,
       sc.created_at
FROM suspense_cases sc
JOIN transactions t ON t.id = sc.original_transaction_id
JOIN accounts ia ON ia.id = sc.intended_account_id
JOIN accounts sa ON sa.id = sc.suspense_account_id
WHERE sc.status = 'OPEN'
ORDER BY sc.business_date DESC, sc.amount DESC;


-- ─────────────────────────────────────────────────────────────
-- SUSP-AUDIT-03: Suspense case resolution audit trail
-- Purpose: Verify all resolved/reversed cases have proper
--          maker-checker and audit evidence.
-- Expected: All resolved cases have resolver != checker.
-- ─────────────────────────────────────────────────────────────
SELECT sc.id               AS case_id,
       sc.status,
       sc.amount,
       sc.reason_code,
       sc.business_date,
       t.transaction_ref,
       rb.username          AS resolved_by,
       rc.username          AS resolution_checker,
       sc.resolution_remarks,
       sc.resolved_at,
       CASE
           WHEN sc.status IN ('RESOLVED', 'REVERSED')
                AND sc.resolved_by_id = sc.resolution_checker_id
           THEN 'MAKER-CHECKER VIOLATION'
           WHEN sc.status IN ('RESOLVED', 'REVERSED')
                AND sc.resolved_by_id IS NULL
           THEN 'MISSING RESOLVER'
           ELSE 'OK'
       END AS governance_check
FROM suspense_cases sc
JOIN transactions t ON t.id = sc.original_transaction_id
LEFT JOIN users rb ON rb.id = sc.resolved_by_id
LEFT JOIN users rc ON rc.id = sc.resolution_checker_id
WHERE sc.status IN ('RESOLVED', 'REVERSED')
ORDER BY sc.resolved_at DESC;


-- ─────────────────────────────────────────────────────────────
-- SUSP-AUDIT-04: Suspense summary by tenant (EOD dashboard)
-- Purpose: Aggregate suspense status per tenant.
-- Expected: open_count = 0 and open_amount = 0 at EOD.
-- ─────────────────────────────────────────────────────────────
SELECT sc.tenant_id,
       COUNT(CASE WHEN sc.status = 'OPEN' THEN 1 END)     AS open_count,
       COALESCE(SUM(CASE WHEN sc.status = 'OPEN' THEN sc.amount END), 0) AS open_amount,
       COUNT(CASE WHEN sc.status = 'RESOLVED' THEN 1 END) AS resolved_count,
       COUNT(CASE WHEN sc.status = 'REVERSED' THEN 1 END) AS reversed_count,
       COUNT(sc.id)                                         AS total_cases
FROM suspense_cases sc
GROUP BY sc.tenant_id;


-- ─────────────────────────────────────────────────────────────
-- SUSP-AUDIT-05: Suspense GL mapping configuration check
-- Purpose: Verify all tenants have suspense accounts configured.
-- ─────────────────────────────────────────────────────────────
SELECT t.id           AS tenant_id,
       t.tenant_code,
       t.tenant_name,
       sgm.channel,
       sgm.suspense_account_number,
       a.balance       AS current_balance,
       CASE
           WHEN sgm.id IS NULL THEN 'NO MAPPING — CONFIGURE'
           WHEN a.id IS NULL   THEN 'MAPPING EXISTS BUT ACCOUNT MISSING'
           ELSE 'OK'
       END AS config_status
FROM tenants t
LEFT JOIN suspense_gl_mappings sgm ON sgm.tenant_id = t.id AND sgm.is_active = TRUE
LEFT JOIN accounts a ON a.account_number = sgm.suspense_account_number AND a.tenant_id = t.id
ORDER BY t.tenant_code, sgm.channel;


-- ─────────────────────────────────────────────────────────────
-- SUSP-AUDIT-06: PARKED transactions (pending suspense resolution)
-- Purpose: Find all transactions in PARKED status.
-- Expected: 0 rows at EOD time.
-- ─────────────────────────────────────────────────────────────
SELECT t.id              AS transaction_id,
       t.transaction_ref,
       t.transaction_type,
       t.amount,
       t.currency,
       t.status,
       t.business_date,
       m.username         AS maker,
       t.created_at
FROM transactions t
LEFT JOIN users m ON m.id = t.maker_id
WHERE t.status = 'PARKED'
ORDER BY t.business_date DESC, t.amount DESC;


-- ─────────────────────────────────────────────────────────────
-- EXECUTION CHECKLIST
-- ─────────────────────────────────────────────────────────────
-- #  Query          Run Frequency    Acceptable Result             Action
-- 01 Account balance Daily (pre-EOD) balance = 0 for all           BLOCK EOD if non-zero
-- 02 Open cases     Daily (pre-EOD)  0 rows                        Resolve/reverse before EOD
-- 03 Resolution log Weekly           No maker-checker violations   Governance investigation
-- 04 Summary        Daily            open_count = 0                Dashboard metric
-- 05 Config check   Monthly          All tenants mapped            Configure missing
-- 06 PARKED txns    Daily (pre-EOD)  0 rows                        Resolve parked transactions
