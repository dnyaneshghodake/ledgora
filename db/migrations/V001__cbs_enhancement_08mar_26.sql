-- ============================================================
-- Ledgora CBS — UAT Migration Script
-- PR: cbs-enhancement_08mar_26
-- Target: SQL Server (UAT / PROD)
--
-- Connection: localhost:1433  DB: ledgora  User: sa
-- Run BEFORE deploying the new WAR.
-- All statements are idempotent (IF NOT EXISTS guards).
-- ============================================================

USE ledgora;
GO

PRINT '====================================================';
PRINT 'Ledgora CBS Migration V001 — starting...';
PRINT 'Database: ' + DB_NAME();
PRINT '====================================================';
GO

-- ============================================================
-- 1. customers — new CBS / KYC / maker-checker columns
-- ============================================================
PRINT '';
PRINT '--- [1/8] Altering [customers] table ---';
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'full_name')
BEGIN
    ALTER TABLE customers ADD full_name NVARCHAR(120) NULL;
    PRINT '  + customers.full_name';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'customer_type')
BEGIN
    ALTER TABLE customers ADD customer_type NVARCHAR(20) NULL;
    PRINT '  + customers.customer_type';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'pan_number')
BEGIN
    ALTER TABLE customers ADD pan_number NVARCHAR(10) NULL;
    PRINT '  + customers.pan_number';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'aadhaar_number')
BEGIN
    ALTER TABLE customers ADD aadhaar_number NVARCHAR(12) NULL;
    PRINT '  + customers.aadhaar_number';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'gst_number')
BEGIN
    ALTER TABLE customers ADD gst_number NVARCHAR(15) NULL;
    PRINT '  + customers.gst_number';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'risk_category')
BEGIN
    ALTER TABLE customers ADD risk_category NVARCHAR(10) NULL;
    PRINT '  + customers.risk_category';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'approved_by')
BEGIN
    ALTER TABLE customers ADD approved_by BIGINT NULL;
    PRINT '  + customers.approved_by';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'maker_timestamp')
BEGIN
    ALTER TABLE customers ADD maker_timestamp DATETIME2 NULL;
    PRINT '  + customers.maker_timestamp';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'checker_timestamp')
BEGIN
    ALTER TABLE customers ADD checker_timestamp DATETIME2 NULL;
    PRINT '  + customers.checker_timestamp';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'version')
BEGIN
    ALTER TABLE customers ADD version BIGINT NULL;
    PRINT '  + customers.version';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'updated_at')
BEGIN
    ALTER TABLE customers ADD updated_at DATETIME2 NULL;
    PRINT '  + customers.updated_at';
END
GO

-- approval_status: add nullable first, backfill, then enforce NOT NULL
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('customers') AND name = 'approval_status')
BEGIN
    ALTER TABLE customers ADD approval_status NVARCHAR(20) NULL;
    PRINT '  + customers.approval_status (nullable, pending backfill)';
END
GO

-- Backfill approval_status from kyc_status
UPDATE customers
SET approval_status = CASE
    WHEN kyc_status = 'VERIFIED' THEN 'APPROVED'
    WHEN kyc_status = 'REJECTED' THEN 'REJECTED'
    ELSE 'PENDING'
END
WHERE approval_status IS NULL;
PRINT '  ~ customers.approval_status backfilled';
GO

-- Backfill other new columns with defaults
UPDATE customers SET customer_type = 'INDIVIDUAL' WHERE customer_type IS NULL;
UPDATE customers SET risk_category  = 'LOW'        WHERE risk_category  IS NULL;
UPDATE customers SET version        = 0             WHERE version        IS NULL;
UPDATE customers SET updated_at     = created_at    WHERE updated_at     IS NULL;
GO

-- Backfill full_name
UPDATE customers
SET full_name = LTRIM(RTRIM(ISNULL(first_name, '') + ' ' + ISNULL(last_name, '')))
WHERE full_name IS NULL
  AND first_name IS NOT NULL
  AND last_name  IS NOT NULL;
GO

-- Enforce NOT NULL on approval_status now that all rows are populated
ALTER TABLE customers ALTER COLUMN approval_status NVARCHAR(20) NOT NULL;
PRINT '  ~ customers.approval_status set NOT NULL';
GO

-- FK: approved_by → users
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'fk_customers_approved_by'
      AND parent_object_id = OBJECT_ID('customers')
)
BEGIN
    ALTER TABLE customers
        ADD CONSTRAINT fk_customers_approved_by
        FOREIGN KEY (approved_by) REFERENCES users(id);
    PRINT '  + FK fk_customers_approved_by';
END
GO

-- Index on approval_status for checker pending-queue performance
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'idx_customer_approval_status'
      AND object_id = OBJECT_ID('customers')
)
BEGIN
    CREATE NONCLUSTERED INDEX idx_customer_approval_status
        ON customers (approval_status)
        INCLUDE (tenant_id);
    PRINT '  + INDEX idx_customer_approval_status';
END
GO

PRINT '--- [1/8] customers done ---';
GO

-- ============================================================
-- 2. transactions — maker/checker + version + updated_at
-- ============================================================
PRINT '';
PRINT '--- [2/8] Altering [transactions] table ---';
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('transactions') AND name = 'maker_id')
BEGIN
    ALTER TABLE transactions ADD maker_id BIGINT NULL;
    PRINT '  + transactions.maker_id';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('transactions') AND name = 'checker_id')
BEGIN
    ALTER TABLE transactions ADD checker_id BIGINT NULL;
    PRINT '  + transactions.checker_id';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('transactions') AND name = 'maker_timestamp')
BEGIN
    ALTER TABLE transactions ADD maker_timestamp DATETIME2 NULL;
    PRINT '  + transactions.maker_timestamp';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('transactions') AND name = 'checker_timestamp')
BEGIN
    ALTER TABLE transactions ADD checker_timestamp DATETIME2 NULL;
    PRINT '  + transactions.checker_timestamp';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('transactions') AND name = 'checker_remarks')
BEGIN
    ALTER TABLE transactions ADD checker_remarks NVARCHAR(500) NULL;
    PRINT '  + transactions.checker_remarks';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('transactions') AND name = 'version')
BEGIN
    ALTER TABLE transactions ADD version BIGINT NULL;
    UPDATE transactions SET version = 0 WHERE version IS NULL;
    PRINT '  + transactions.version';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('transactions') AND name = 'updated_at')
BEGIN
    ALTER TABLE transactions ADD updated_at DATETIME2 NULL;
    UPDATE transactions SET updated_at = created_at WHERE updated_at IS NULL;
    PRINT '  + transactions.updated_at';
END
GO

-- FKs for maker/checker
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_transactions_maker' AND parent_object_id = OBJECT_ID('transactions'))
BEGIN
    ALTER TABLE transactions ADD CONSTRAINT fk_transactions_maker FOREIGN KEY (maker_id) REFERENCES users(id);
    PRINT '  + FK fk_transactions_maker';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_transactions_checker' AND parent_object_id = OBJECT_ID('transactions'))
BEGIN
    ALTER TABLE transactions ADD CONSTRAINT fk_transactions_checker FOREIGN KEY (checker_id) REFERENCES users(id);
    PRINT '  + FK fk_transactions_checker';
END
GO

PRINT '--- [2/8] transactions done ---';
GO

-- ============================================================
-- 3. approval_requests — version column
-- ============================================================
PRINT '';
PRINT '--- [3/8] Altering [approval_requests] table ---';
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('approval_requests') AND name = 'version')
BEGIN
    ALTER TABLE approval_requests ADD version BIGINT NULL;
    UPDATE approval_requests SET version = 0 WHERE version IS NULL;
    PRINT '  + approval_requests.version';
END
GO

PRINT '--- [3/8] approval_requests done ---';
GO

-- ============================================================
-- 4. config_change_requests — version column
-- ============================================================
PRINT '';
PRINT '--- [4/8] Altering [config_change_requests] table ---';
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('config_change_requests') AND name = 'version')
BEGIN
    ALTER TABLE config_change_requests ADD version BIGINT NULL;
    UPDATE config_change_requests SET version = 0 WHERE version IS NULL;
    PRINT '  + config_change_requests.version';
END
GO

PRINT '--- [4/8] config_change_requests done ---';
GO

-- ============================================================
-- 5. account_liens — version + updated_at
-- ============================================================
PRINT '';
PRINT '--- [5/8] Altering [account_liens] table ---';
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('account_liens') AND name = 'version')
BEGIN
    ALTER TABLE account_liens ADD version BIGINT NULL;
    UPDATE account_liens SET version = 0 WHERE version IS NULL;
    PRINT '  + account_liens.version';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('account_liens') AND name = 'updated_at')
BEGIN
    ALTER TABLE account_liens ADD updated_at DATETIME2 NULL;
    UPDATE account_liens SET updated_at = created_at WHERE updated_at IS NULL;
    PRINT '  + account_liens.updated_at';
END
GO

PRINT '--- [5/8] account_liens done ---';
GO

-- ============================================================
-- 6. account_ownership — version + updated_at
-- ============================================================
PRINT '';
PRINT '--- [6/8] Altering [account_ownership] table ---';
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('account_ownership') AND name = 'version')
BEGIN
    ALTER TABLE account_ownership ADD version BIGINT NULL;
    UPDATE account_ownership SET version = 0 WHERE version IS NULL;
    PRINT '  + account_ownership.version';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('account_ownership') AND name = 'updated_at')
BEGIN
    ALTER TABLE account_ownership ADD updated_at DATETIME2 NULL;
    UPDATE account_ownership SET updated_at = created_at WHERE updated_at IS NULL;
    PRINT '  + account_ownership.updated_at';
END
GO

PRINT '--- [6/8] account_ownership done ---';
GO

-- ============================================================
-- 7. inter_branch_transfers — version + updated_at
-- ============================================================
PRINT '';
PRINT '--- [7/8] Altering [inter_branch_transfers] table ---';
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('inter_branch_transfers') AND name = 'version')
BEGIN
    ALTER TABLE inter_branch_transfers ADD version BIGINT NULL;
    UPDATE inter_branch_transfers SET version = 0 WHERE version IS NULL;
    PRINT '  + inter_branch_transfers.version';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('inter_branch_transfers') AND name = 'updated_at')
BEGIN
    ALTER TABLE inter_branch_transfers ADD updated_at DATETIME2 NULL;
    UPDATE inter_branch_transfers SET updated_at = created_at WHERE updated_at IS NULL;
    PRINT '  + inter_branch_transfers.updated_at';
END
GO

PRINT '--- [7/8] inter_branch_transfers done ---';
GO

-- ============================================================
-- 8. accounts — backfill approval_status + version
--    (columns already exist from prior release)
-- ============================================================
PRINT '';
PRINT '--- [8/8] Backfilling [accounts] table ---';
GO

-- Existing ACTIVE accounts are pre-approved (created before maker-checker was enforced)
UPDATE accounts
SET approval_status = 'APPROVED'
WHERE approval_status IS NULL OR approval_status = '';
PRINT '  ~ accounts.approval_status backfilled to APPROVED for existing rows';
GO

UPDATE accounts SET version = 0 WHERE version IS NULL;
PRINT '  ~ accounts.version backfilled to 0';
GO

-- Backfill accounts.status for any that may be NULL
UPDATE accounts SET status = 'ACTIVE' WHERE status IS NULL;
GO

PRINT '--- [8/8] accounts done ---';
GO

-- ============================================================
-- 9. Audit log — countByTenantId index (performance)
-- ============================================================
PRINT '';
PRINT '--- [9/9] Adding audit_logs index ---';
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'idx_audit_tenant_timestamp'
      AND object_id = OBJECT_ID('audit_logs')
)
BEGIN
    CREATE NONCLUSTERED INDEX idx_audit_tenant_timestamp
        ON audit_logs (tenant_id, timestamp DESC);
    PRINT '  + INDEX idx_audit_tenant_timestamp';
END
GO

PRINT '';
PRINT '====================================================';
PRINT 'Ledgora CBS Migration V001 — COMPLETE';
PRINT '====================================================';
GO
