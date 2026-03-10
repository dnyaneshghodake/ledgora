-- ============================================================
-- LEDGORA — BALANCE RECONCILIATION AUDIT SQL PACK
-- RBI Data Validation | Ledger-vs-Cache Integrity
-- Compatible with H2 (dev) and SQL Server (prod)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- RECON-01: Live balance reconciliation (cache vs ledger)
-- Purpose: Compare every account's cached balance with the
--          authoritative ledger-derived balance.
-- Expected: drift = 0 for all rows.
-- ─────────────────────────────────────────────────────────────
SELECT a.account_number,
       a.account_name,
       a.balance AS cached_balance,
       (COALESCE(cr.total_credits, 0) - COALESCE(dr.total_debits, 0)) AS ledger_balance,
       a.balance - (COALESCE(cr.total_credits, 0) - COALESCE(dr.total_debits, 0)) AS drift,
       a.tenant_id,
       CASE
           WHEN a.balance = (COALESCE(cr.total_credits, 0) - COALESCE(dr.total_debits, 0))
           THEN 'BALANCED'
           ELSE 'DRIFT — INVESTIGATE'
       END AS status
FROM accounts a
LEFT JOIN (
    SELECT le.account_id, SUM(le.amount) AS total_credits
    FROM ledger_entries le WHERE le.entry_type = 'CREDIT'
    GROUP BY le.account_id
) cr ON cr.account_id = a.id
LEFT JOIN (
    SELECT le.account_id, SUM(le.amount) AS total_debits
    FROM ledger_entries le WHERE le.entry_type = 'DEBIT'
    GROUP BY le.account_id
) dr ON dr.account_id = a.id
WHERE a.balance != (COALESCE(cr.total_credits, 0) - COALESCE(dr.total_debits, 0))
ORDER BY ABS(a.balance - (COALESCE(cr.total_credits, 0) - COALESCE(dr.total_debits, 0))) DESC;


-- ─────────────────────────────────────────────────────────────
-- RECON-02: Open drift alerts (requires investigation)
-- Purpose: List all unresolved balance drift alerts.
-- Expected: 0 rows between reconciliation runs.
-- ─────────────────────────────────────────────────────────────
SELECT bda.id            AS alert_id,
       bda.account_number,
       bda.cached_balance,
       bda.ledger_balance,
       bda.drift_amount,
       bda.status,
       bda.detected_at,
       bda.tenant_id
FROM balance_drift_alerts bda
WHERE bda.status = 'OPEN'
ORDER BY ABS(bda.drift_amount) DESC, bda.detected_at DESC;


-- ─────────────────────────────────────────────────────────────
-- RECON-03: Drift alert history (audit trail)
-- Purpose: Complete log of all detected drifts with resolution.
-- ─────────────────────────────────────────────────────────────
SELECT bda.id            AS alert_id,
       bda.account_number,
       bda.cached_balance,
       bda.ledger_balance,
       bda.drift_amount,
       bda.status,
       bda.detected_at,
       bda.resolved_at,
       bda.resolution_remarks,
       bda.tenant_id
FROM balance_drift_alerts bda
ORDER BY bda.detected_at DESC;


-- ─────────────────────────────────────────────────────────────
-- RECON-04: Drift summary per tenant (dashboard)
-- Purpose: Aggregate drift status for monitoring dashboard.
-- ─────────────────────────────────────────────────────────────
SELECT bda.tenant_id,
       COUNT(CASE WHEN bda.status = 'OPEN' THEN 1 END)         AS open_drifts,
       COUNT(CASE WHEN bda.status = 'RESOLVED' THEN 1 END)     AS resolved_drifts,
       COUNT(CASE WHEN bda.status = 'ACKNOWLEDGED' THEN 1 END) AS acknowledged,
       COUNT(bda.id)                                             AS total_alerts,
       COALESCE(SUM(CASE WHEN bda.status = 'OPEN' THEN ABS(bda.drift_amount) END), 0)
                                                                 AS total_open_drift
FROM balance_drift_alerts bda
GROUP BY bda.tenant_id;


-- ─────────────────────────────────────────────────────────────
-- RECON-05: Drift detection audit log entries
-- Purpose: Verify reconciliation service is running and logging.
-- ─────────────────────────────────────────────────────────────
SELECT al.id           AS log_id,
       al.action,
       al.entity_id    AS drift_alert_id,
       al.details,
       al.created_at
FROM audit_logs al
WHERE al.action = 'BALANCE_DRIFT_DETECTED'
ORDER BY al.created_at DESC;


-- ─────────────────────────────────────────────────────────────
-- EXECUTION CHECKLIST
-- ─────────────────────────────────────────────────────────────
-- #  Query          Run Frequency       Acceptable Result          Action
-- 01 Live recon     On-demand/daily     0 drifting accounts        Investigate any drift
-- 02 Open alerts    Every 15 min        0 rows                     Resolve cache or ledger
-- 03 Alert history  Weekly              Review trend               Root cause repeat drifts
-- 04 Summary        Dashboard           open_drifts = 0            Monitor continuously
-- 05 Audit log      Weekly              Entries present = running  Verify scheduler health
