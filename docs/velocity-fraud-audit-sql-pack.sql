-- ============================================================
-- LEDGORA — VELOCITY FRAUD ENGINE AUDIT SQL PACK
-- RBI Fraud Risk Management | Proactive Burst Prevention
-- Compatible with H2 (dev) and SQL Server (prod)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- VEL-AUDIT-01: Account transaction velocity (last 60 minutes)
-- Purpose: Real-time view of per-account transaction bursts.
-- Expected: Review accounts with high counts or amounts.
-- ─────────────────────────────────────────────────────────────
SELECT a.account_number,
       a.account_name,
       a.status AS account_status,
       COUNT(t.id) AS txn_count_last_hour,
       COALESCE(SUM(t.amount), 0) AS total_amount_last_hour,
       a.tenant_id
FROM transactions t
JOIN accounts a ON (a.id = t.source_account_id OR a.id = t.destination_account_id)
WHERE t.status != 'REJECTED'
  AND t.created_at >= DATEADD('HOUR', -1, CURRENT_TIMESTAMP)
GROUP BY a.account_number, a.account_name, a.status, a.tenant_id
HAVING COUNT(t.id) > 3
ORDER BY txn_count_last_hour DESC, total_amount_last_hour DESC;


-- ─────────────────────────────────────────────────────────────
-- VEL-AUDIT-02: Open fraud alerts (requires investigation)
-- Purpose: List all unresolved fraud alerts.
-- Expected: Operations team reviews and resolves each.
-- ─────────────────────────────────────────────────────────────
SELECT fa.id            AS alert_id,
       fa.alert_type,
       fa.account_number,
       fa.status,
       fa.details,
       fa.observed_count,
       fa.observed_amount,
       fa.threshold_value,
       u.username        AS triggered_by_user,
       fa.created_at
FROM fraud_alerts fa
LEFT JOIN users u ON u.id = fa.user_id
WHERE fa.status = 'OPEN'
ORDER BY fa.created_at DESC;


-- ─────────────────────────────────────────────────────────────
-- VEL-AUDIT-03: Accounts currently under fraud review
-- Purpose: List all accounts frozen by velocity engine.
-- Expected: Each should have a corresponding FraudAlert.
-- ─────────────────────────────────────────────────────────────
SELECT a.account_number,
       a.account_name,
       a.status,
       a.tenant_id,
       fa.id             AS related_alert_id,
       fa.alert_type,
       fa.details,
       fa.created_at     AS alert_time
FROM accounts a
LEFT JOIN fraud_alerts fa ON fa.account_id = a.id AND fa.status = 'OPEN'
WHERE a.status = 'UNDER_REVIEW'
ORDER BY a.tenant_id, a.account_number;


-- ─────────────────────────────────────────────────────────────
-- VEL-AUDIT-04: Velocity limit configuration check
-- Purpose: Verify all tenants have velocity limits configured.
-- Expected: At least a tenant-wide default per active tenant.
-- ─────────────────────────────────────────────────────────────
SELECT t.id             AS tenant_id,
       t.tenant_code,
       vl.account_id,
       a.account_number,
       vl.max_txn_count_per_hour,
       vl.max_total_amount_per_hour,
       vl.is_active,
       CASE
           WHEN vl.id IS NULL THEN 'NO LIMIT — CONFIGURE'
           ELSE 'ACTIVE'
       END AS config_status
FROM tenants t
LEFT JOIN velocity_limits vl ON vl.tenant_id = t.id AND vl.is_active = TRUE
LEFT JOIN accounts a ON a.id = vl.account_id
ORDER BY t.tenant_code, vl.account_id;


-- ─────────────────────────────────────────────────────────────
-- VEL-AUDIT-05: Fraud alert summary per tenant (dashboard)
-- Purpose: Aggregate fraud alert status for monitoring dashboard.
-- ─────────────────────────────────────────────────────────────
SELECT fa.tenant_id,
       COUNT(CASE WHEN fa.status = 'OPEN' THEN 1 END)           AS open_alerts,
       COUNT(CASE WHEN fa.status = 'ACKNOWLEDGED' THEN 1 END)   AS acknowledged,
       COUNT(CASE WHEN fa.status = 'RESOLVED' THEN 1 END)       AS resolved,
       COUNT(CASE WHEN fa.status = 'FALSE_POSITIVE' THEN 1 END) AS false_positives,
       COUNT(fa.id)                                               AS total_alerts,
       COUNT(DISTINCT fa.account_id)                              AS affected_accounts
FROM fraud_alerts fa
GROUP BY fa.tenant_id;


-- ─────────────────────────────────────────────────────────────
-- VEL-AUDIT-06: Velocity breach audit trail (all blocked txns)
-- Purpose: Complete log of all velocity-blocked transactions.
-- ─────────────────────────────────────────────────────────────
SELECT al.id           AS log_id,
       al.user_id,
       u.username,
       al.action,
       al.entity_type,
       al.entity_id    AS fraud_alert_id,
       al.details,
       al.created_at
FROM audit_logs al
LEFT JOIN users u ON u.id = al.user_id
WHERE al.action LIKE 'VELOCITY_BREACH_%'
ORDER BY al.created_at DESC;


-- ─────────────────────────────────────────────────────────────
-- EXECUTION CHECKLIST
-- ─────────────────────────────────────────────────────────────
-- #  Query             Run Frequency    Acceptable Result              Action
-- 01 Velocity snapshot Hourly/real-time Review high-burst accounts     Investigate spikes
-- 02 Open alerts       Hourly           Ops team reviews each          Resolve or escalate
-- 03 Under review accs Daily            Each has corresponding alert   Unfreeze after investigation
-- 04 Config check      Monthly          All tenants have defaults      Configure missing
-- 05 Summary dashboard Continuous       Low open_alerts count          Monitor trend
-- 06 Breach trail      Daily            Review patterns                Correlate with fraud cases
