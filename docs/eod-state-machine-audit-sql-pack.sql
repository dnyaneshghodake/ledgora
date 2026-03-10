-- ============================================================
-- LEDGORA — EOD STATE MACHINE AUDIT SQL PACK
-- RBI Operational Resilience | Crash-safe EOD
-- Compatible with H2 (dev) and SQL Server (prod)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- EOD-SM-01: Current EOD process status per tenant
-- Purpose: Dashboard view of all EOD executions.
-- ─────────────────────────────────────────────────────────────
SELECT ep.id             AS process_id,
       t.tenant_code,
       ep.business_date,
       ep.phase,
       ep.status,
       ep.started_at,
       ep.completed_at,
       ep.last_updated,
       ep.failure_reason
FROM eod_processes ep
JOIN tenants t ON t.id = ep.tenant_id
ORDER BY ep.business_date DESC, t.tenant_code;


-- ─────────────────────────────────────────────────────────────
-- EOD-SM-02: Stuck EOD detection (> 30 minutes in same phase)
-- Purpose: Detect EOD processes that may have crashed mid-phase.
-- Expected: 0 rows during normal operations.
-- Action:   Investigate and resume or reset.
-- ─────────────────────────────────────────────────────────────
SELECT ep.id             AS process_id,
       t.tenant_code,
       ep.business_date,
       ep.phase           AS stuck_phase,
       ep.status,
       ep.last_updated,
       TIMESTAMPDIFF(MINUTE, ep.last_updated, CURRENT_TIMESTAMP) AS minutes_stuck,
       CASE
           WHEN TIMESTAMPDIFF(MINUTE, ep.last_updated, CURRENT_TIMESTAMP) > 60
           THEN 'CRITICAL — OVER 1 HOUR'
           WHEN TIMESTAMPDIFF(MINUTE, ep.last_updated, CURRENT_TIMESTAMP) > 30
           THEN 'WARNING — OVER 30 MIN'
           ELSE 'OK'
       END AS severity
FROM eod_processes ep
JOIN tenants t ON t.id = ep.tenant_id
WHERE ep.status = 'RUNNING'
  AND ep.last_updated < DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)
ORDER BY ep.last_updated ASC;


-- ─────────────────────────────────────────────────────────────
-- EOD-SM-03: Failed EOD processes (requires investigation)
-- Purpose: List all EOD runs that failed and may need retry.
-- Expected: Resolve each before next business day.
-- ─────────────────────────────────────────────────────────────
SELECT ep.id             AS process_id,
       t.tenant_code,
       ep.business_date,
       ep.phase           AS failed_at_phase,
       ep.failure_reason,
       ep.started_at,
       ep.last_updated
FROM eod_processes ep
JOIN tenants t ON t.id = ep.tenant_id
WHERE ep.status = 'FAILED'
ORDER BY ep.business_date DESC;


-- ─────────────────────────────────────────────────────────────
-- EOD-SM-04: Incomplete EOD processes (RUNNING — for restart recovery)
-- Purpose: On application restart, find all EOD processes
--          that were interrupted and need resumption.
-- Expected: 0 rows after clean shutdown.
-- ─────────────────────────────────────────────────────────────
SELECT ep.id             AS process_id,
       t.tenant_code,
       ep.business_date,
       ep.phase           AS resume_from_phase,
       ep.started_at,
       ep.last_updated
FROM eod_processes ep
JOIN tenants t ON t.id = ep.tenant_id
WHERE ep.status = 'RUNNING'
ORDER BY ep.started_at ASC;


-- ─────────────────────────────────────────────────────────────
-- EOD-SM-05: EOD execution history (audit trail)
-- Purpose: Complete log of EOD lifecycle events.
-- ─────────────────────────────────────────────────────────────
SELECT al.id           AS log_id,
       al.action,
       al.entity_id    AS eod_process_id,
       al.details,
       al.created_at
FROM audit_logs al
WHERE al.entity_type = 'EOD_PROCESS'
  AND al.action IN ('EOD_STARTED', 'EOD_COMPLETED', 'EOD_FAILED')
ORDER BY al.created_at DESC;


-- ─────────────────────────────────────────────────────────────
-- EOD-SM-06: Double execution detection
-- Purpose: Verify no tenant has multiple EOD runs for same date.
-- Expected: 0 rows (unique constraint should prevent this).
-- ─────────────────────────────────────────────────────────────
SELECT ep.tenant_id,
       ep.business_date,
       COUNT(ep.id) AS process_count,
       'DOUBLE EXECUTION — INVESTIGATE' AS audit_result
FROM eod_processes ep
GROUP BY ep.tenant_id, ep.business_date
HAVING COUNT(ep.id) > 1;


-- ─────────────────────────────────────────────────────────────
-- EXECUTION CHECKLIST
-- ─────────────────────────────────────────────────────────────
-- #  Query              Run Frequency     Acceptable Result            Action
-- 01 Current status     After each EOD    All COMPLETED                Review any non-COMPLETED
-- 02 Stuck detection    Every 15 min      0 rows                       Investigate + resume
-- 03 Failed processes   Daily             0 rows                       Retry or escalate
-- 04 Incomplete (restart) On app startup  0 rows after clean shutdown  Resume incomplete
-- 05 Audit trail        Weekly            Complete lifecycle logged     Verify no gaps
-- 06 Double execution   Daily             0 rows                       DB constraint violation
