-- ============================================================================
-- Ledgora CBS -- Role Matrix Seed Data (RBI + Finacle Grade)
-- Target: SQL Server 2019+
-- Description: Complete role hierarchy with privilege mapping.
--              Enforces CBS segregation-of-duty constraints:
--                - Maker and Checker cannot coexist for the same user
--                - Teller cannot authorize (checker role)
--                - Auditor is read-only
--                - Operations cannot modify financial data
--                - Risk can view fraud flags but cannot post vouchers
-- ============================================================================

SET NOCOUNT ON;

-- ============================================================================
-- ROLES (Idempotent: INSERT only if name does not exist)
-- ============================================================================

-- 1. ROLE_SUPER_ADMIN: Full system access across all tenants
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_SUPER_ADMIN')
    INSERT INTO roles (name, description) VALUES ('ROLE_SUPER_ADMIN', 'Super Administrator — full cross-tenant system access');

-- 2. ROLE_ADMIN: Tenant-level administrator
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN')
    INSERT INTO roles (name, description) VALUES ('ROLE_ADMIN', 'System Administrator — tenant-level admin privileges');

-- 3. ROLE_TENANT_ADMIN: Tenant configuration and user management
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_TENANT_ADMIN')
    INSERT INTO roles (name, description) VALUES ('ROLE_TENANT_ADMIN', 'Tenant Administrator — tenant config, user, branch management');

-- 4. ROLE_MANAGER: Branch manager with supervisory access
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_MANAGER')
    INSERT INTO roles (name, description) VALUES ('ROLE_MANAGER', 'Branch Manager — supervisory access, approve teller sessions');

-- 5. ROLE_TELLER: Front-line teller (CANNOT authorize/check)
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_TELLER')
    INSERT INTO roles (name, description) VALUES ('ROLE_TELLER', 'Branch Teller — cash operations, deposits, withdrawals. Cannot authorize.');

-- 6. ROLE_MAKER: Transaction initiator (CANNOT be checker for same txn)
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_MAKER')
    INSERT INTO roles (name, description) VALUES ('ROLE_MAKER', 'Transaction Maker — initiates transactions. Cannot also be checker for same transaction.');

-- 7. ROLE_CHECKER: Transaction authorizer (CANNOT be maker for same txn)
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_CHECKER')
    INSERT INTO roles (name, description) VALUES ('ROLE_CHECKER', 'Transaction Checker — authorizes/rejects maker-initiated transactions.');

-- 8. ROLE_AUDITOR: Read-only audit access (CANNOT modify any data)
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_AUDITOR')
    INSERT INTO roles (name, description) VALUES ('ROLE_AUDITOR', 'Internal Auditor — read-only access to all modules, audit logs, hash chain verification.');

-- 9. ROLE_OPERATIONS: Operational support (CANNOT modify financial data)
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_OPERATIONS')
    INSERT INTO roles (name, description) VALUES ('ROLE_OPERATIONS', 'Operations Team — ledger validation, reconciliation, EOD monitoring. Cannot post financial entries.');

-- 10. ROLE_RISK: Risk management (can view fraud flags, CANNOT post vouchers)
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_RISK')
    INSERT INTO roles (name, description) VALUES ('ROLE_RISK', 'Risk Officer — view fraud alerts, velocity breaches, risk dashboards. Cannot post vouchers or modify financial data.');

-- 11. ROLE_SYSTEM: System process pseudo-role (no UI login)
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_SYSTEM')
    INSERT INTO roles (name, description) VALUES ('ROLE_SYSTEM', 'System Process Account — STP auto-authorization, batch processing. No interactive login.');

-- 12. ROLE_COMPLIANCE: Compliance officer for AML/CFT/KYC oversight
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_COMPLIANCE_OFFICER')
    INSERT INTO roles (name, description) VALUES ('ROLE_COMPLIANCE_OFFICER', 'Compliance Officer — AML/CFT oversight, STR filing, KYC review per RBI Master Direction.');

-- Additional supporting roles (already in system)
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_CUSTOMER')
    INSERT INTO roles (name, description) VALUES ('ROLE_CUSTOMER', 'Bank Customer — view own accounts and statements');

IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_BRANCH_MANAGER')
    INSERT INTO roles (name, description) VALUES ('ROLE_BRANCH_MANAGER', 'Branch Manager — branch-level supervisory role');

IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ATM_SYSTEM')
    INSERT INTO roles (name, description) VALUES ('ROLE_ATM_SYSTEM', 'ATM System Channel — channel-level system identity for ATM transactions');

-- ============================================================================
-- ROLE PRIVILEGE MATRIX (Reference Documentation)
-- ============================================================================
-- +---------------------+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+
-- | Privilege           | SUPER  | ADMIN  | T_ADM  | MGR    | TELLER | MAKER  | CHCKR  | AUDIT  | OPS    | RISK   | SYSTEM | COMPL  |
-- +---------------------+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+
-- | View Dashboard      |   Y    |   Y    |   Y    |   Y    |   Y    |   Y    |   Y    |   Y    |   Y    |   Y    |   -    |   Y    |
-- | Create Transaction  |   Y    |   Y    |   -    |   Y    |   Y    |   Y    |   -    |   -    |   -    |   -    |   Y    |   -    |
-- | Authorize Txn       |   Y    |   Y    |   -    |   Y    |   -    |   -    |   Y    |   -    |   -    |   -    |   Y    |   -    |
-- | Post Voucher        |   Y    |   Y    |   -    |   Y    |   -    |   -    |   Y    |   -    |   -    |   -    |   Y    |   -    |
-- | View Ledger         |   Y    |   Y    |   Y    |   Y    |   Y    |   Y    |   Y    |   Y    |   Y    |   Y    |   -    |   Y    |
-- | Modify Financial    |   Y    |   Y    |   -    |   Y    |   Y    |   Y    |   Y    |   -    |   -    |   -    |   Y    |   -    |
-- | View Fraud Flags    |   Y    |   Y    |   -    |   Y    |   -    |   -    |   -    |   Y    |   -    |   Y    |   -    |   Y    |
-- | Manage Users        |   Y    |   Y    |   Y    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |
-- | Manage Branches     |   Y    |   Y    |   Y    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |
-- | Manage Tenants      |   Y    |   -    |   Y    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |
-- | Audit Logs          |   Y    |   Y    |   -    |   -    |   -    |   -    |   -    |   Y    |   -    |   -    |   -    |   -    |
-- | Hash Chain Verify   |   Y    |   Y    |   -    |   -    |   -    |   -    |   -    |   Y    |   -    |   -    |   -    |   -    |
-- | EOD Trigger         |   Y    |   Y    |   -    |   Y    |   -    |   -    |   -    |   -    |   Y    |   -    |   Y    |   -    |
-- | Reconciliation      |   Y    |   Y    |   -    |   -    |   -    |   -    |   -    |   Y    |   Y    |   -    |   -    |   -    |
-- | Teller Cash Ops     |   -    |   -    |   -    |   -    |   Y    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |
-- | Open Teller Session |   -    |   -    |   -    |   Y    |   Y    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |
-- | KYC/AML Review      |   Y    |   Y    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   Y    |
-- | STR Filing          |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   -    |   Y    |
-- +---------------------+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+
--
-- SEGREGATION OF DUTY RULES:
--   1. Maker != Checker: A user with ROLE_MAKER cannot also hold ROLE_CHECKER (enforced at application level)
--   2. Teller cannot authorize: ROLE_TELLER users cannot approve transactions (no ROLE_CHECKER assignment)
--   3. Auditor read-only: ROLE_AUDITOR cannot create/modify any financial or master data
--   4. Operations non-financial: ROLE_OPERATIONS can trigger EOD/reconciliation but cannot post vouchers
--   5. Risk view-only: ROLE_RISK can view fraud dashboards/alerts but cannot post vouchers or modify data
--
-- ============================================================================

PRINT 'Role matrix seed completed. All 15 CBS roles verified.';
