-- ============================================================
-- LEDGORA — HARD TRANSACTION CEILING AUDIT SQL PACK
-- RBI Risk Appetite Framework | Non-bypassable Limits
-- Compatible with H2 (dev) and SQL Server (prod)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- CEIL-AUDIT-01: All hard limit violations (governance audit log)
-- Purpose: Detect all transactions blocked by hard ceiling.
-- Expected: Review each — these are legitimate blocks.
-- ─────────────────────────────────────────────────────────────
SELECT al.id           AS log_id,
       al.user_id,
       u.username,
       al.action        AS event_type,
       al.entity_type,
       al.details,
       al.created_at
FROM audit_logs al
LEFT JOIN users u ON u.id = al.user_id
WHERE al.action = 'HARD_LIMIT_EXCEEDED'
ORDER BY al.created_at DESC;


-- ─────────────────────────────────────────────────────────────
-- CEIL-AUDIT-02: Current hard limit configuration per tenant
-- Purpose: Verify all tenants have hard limits configured.
-- Expected: Every production tenant should have at least a
--           default (channel=null) limit.
-- ─────────────────────────────────────────────────────────────
SELECT t.id            AS tenant_id,
       t.tenant_code,
       t.tenant_name,
       htl.channel,
       htl.absolute_max_amount,
       htl.is_active,
       CASE
           WHEN htl.id IS NULL THEN 'NO LIMIT — CONFIGURE FOR PRODUCTION'
           WHEN htl.is_active = FALSE THEN 'INACTIVE — NOT ENFORCED'
           ELSE 'ACTIVE'
       END AS config_status
FROM tenants t
LEFT JOIN hard_transaction_limits htl ON htl.tenant_id = t.id
ORDER BY t.tenant_code, htl.channel;


-- ─────────────────────────────────────────────────────────────
-- CEIL-AUDIT-03: Transactions that exceeded hard limits
--                (detect any that slipped through — should be 0)
-- Purpose: Find completed transactions above the configured
--          hard ceiling. If any exist, the enforcement was
--          bypassed — severe governance violation.
-- Expected: 0 rows.
-- ─────────────────────────────────────────────────────────────
SELECT t.id             AS transaction_id,
       t.transaction_ref,
       t.transaction_type,
       t.amount,
       t.channel,
       t.status,
       t.business_date,
       htl.absolute_max_amount AS configured_ceiling,
       t.amount - htl.absolute_max_amount AS overage,
       m.username        AS maker,
       'SEVERE — CEILING BYPASSED' AS audit_result
FROM transactions t
JOIN hard_transaction_limits htl
  ON htl.tenant_id = t.tenant_id
  AND htl.is_active = TRUE
  AND (htl.channel = t.channel OR htl.channel IS NULL)
LEFT JOIN users m ON m.id = t.maker_id
WHERE t.status IN ('COMPLETED', 'PENDING_APPROVAL')
  AND t.amount > htl.absolute_max_amount
ORDER BY t.amount DESC, t.business_date DESC;


-- ─────────────────────────────────────────────────────────────
-- CEIL-AUDIT-04: Hard limit violation frequency by user
-- Purpose: Detect users repeatedly attempting to exceed limits.
--          May indicate social engineering or testing for bypass.
-- Expected: Review patterns; investigate repeat offenders.
-- ─────────────────────────────────────────────────────────────
SELECT al.user_id,
       u.username,
       u.branch_code,
       COUNT(al.id) AS violation_count,
       MIN(al.created_at) AS first_violation,
       MAX(al.created_at) AS last_violation,
       CASE
           WHEN COUNT(al.id) > 5 THEN 'HIGH ALERT — INVESTIGATE'
           WHEN COUNT(al.id) > 2 THEN 'REVIEW'
           ELSE 'NORMAL'
       END AS severity
FROM audit_logs al
LEFT JOIN users u ON u.id = al.user_id
WHERE al.action = 'HARD_LIMIT_EXCEEDED'
GROUP BY al.user_id, u.username, u.branch_code
HAVING COUNT(al.id) > 1
ORDER BY violation_count DESC;


-- ─────────────────────────────────────────────────────────────
-- EXECUTION CHECKLIST
-- ─────────────────────────────────────────────────────────────
-- #  Query             Run Frequency    Acceptable Result            Action
-- 01 Violation log     Daily            Review all blocked attempts  Investigate patterns
-- 02 Config check      Monthly          All tenants configured       Add missing limits
-- 03 Bypass detection  Daily            0 rows                       SEVERE — immediate investigation
-- 04 User frequency    Weekly           No repeat offenders          Flag for compliance review
