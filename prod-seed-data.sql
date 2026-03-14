-- ============================================================================
-- Ledgora CBS — Production Seed Data
-- Target: SQL Server 2019+
-- Generated: 2026-03-14
-- Description: Realistic multi-tenant CBS seed data with full transaction
--              lifecycle including deposits, withdrawals, transfers, IBT,
--              suspense handling, velocity breach, hard ceiling violations,
--              batch close, and EOD run.
-- ============================================================================
-- VALIDATION INVARIANTS:
--   SUM(DR) = SUM(CR) across all ledger entries
--   Clearing GL nets to zero after settlement
--   Suspense GL nets to zero before EOD
--   Batch totals match ledger totals
-- ============================================================================

SET NOCOUNT ON;
BEGIN TRANSACTION;

-- ============================================================================
-- TENANTS
-- ============================================================================
SET IDENTITY_INSERT tenants ON;
INSERT INTO tenants (id, tenant_code, tenant_name, status, current_business_date, day_status, country, base_currency, timezone, regulatory_code, multi_branch_enabled, eod_status, effective_from, remarks, created_at, updated_at)
VALUES
(1, 'TENANT001', 'Ledgora National Bank',   'ACTIVE', '2026-03-14', 'OPEN', 'IN', 'INR', 'Asia/Kolkata', 'RBI/2026/BANK/001', 1, 'NOT_STARTED', '2025-01-01', 'Primary banking tenant', SYSUTCDATETIME(), SYSUTCDATETIME()),
(2, 'TENANT002', 'Ledgora Cooperative Bank', 'ACTIVE', '2026-03-14', 'OPEN', 'IN', 'INR', 'Asia/Kolkata', 'RBI/2026/COOP/002', 1, 'NOT_STARTED', '2025-06-01', 'Cooperative banking tenant', SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT tenants OFF;

-- ============================================================================
-- BRANCHES (2 per tenant)
-- ============================================================================
SET IDENTITY_INSERT branches ON;
INSERT INTO branches (id, tenant_id, branch_code, branch_name, name, address, city, state, pincode, ifsc_code, micr_code, branch_type, contact_phone, contact_email, is_active, version, created_at, updated_at)
VALUES
-- Tenant 1 branches
(1, 1, 'BR001', 'Mumbai Main Branch',   'Mumbai Main Branch',   '123 Nariman Point, Fort',    'Mumbai',    'Maharashtra', '400001', 'LDGR0000001', '400240001', 'HEAD_OFFICE', '+91-22-12345678', 'mumbai@ledgora.in',    1, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(2, 1, 'BR002', 'Pune Camp Branch',     'Pune Camp Branch',     '45 MG Road, Camp',           'Pune',      'Maharashtra', '411001', 'LDGR0000002', '411240001', 'BRANCH',      '+91-20-87654321', 'pune@ledgora.in',      1, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Tenant 2 branches
(3, 2, 'BR003', 'Delhi Connaught Place','Delhi Connaught Place','Block A, CP Inner Circle',   'New Delhi', 'Delhi',       '110001', 'LDGR0000003', '110240001', 'HEAD_OFFICE', '+91-11-23456789', 'delhi@ledgora-coop.in',1, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(4, 2, 'BR004', 'Noida Sector 18',     'Noida Sector 18',     'Plot 12, Sector 18',         'Noida',     'Uttar Pradesh','201301', 'LDGR0000004', '201240001', 'BRANCH',      '+91-120-9876543','noida@ledgora-coop.in',1, 0, SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT branches OFF;

-- ============================================================================
-- ROLES
-- ============================================================================
SET IDENTITY_INSERT roles ON;
INSERT INTO roles (id, name, description) VALUES
(1,  'ROLE_ADMIN',        'System Administrator'),
(2,  'ROLE_MANAGER',      'Branch Manager'),
(3,  'ROLE_TELLER',       'Branch Teller'),
(4,  'ROLE_MAKER',        'Transaction Maker'),
(5,  'ROLE_CHECKER',      'Transaction Checker/Authorizer'),
(6,  'ROLE_AUDITOR',      'Internal Auditor'),
(7,  'ROLE_SUPER_ADMIN',  'Super Administrator'),
(8,  'ROLE_TENANT_ADMIN', 'Tenant Administrator'),
(9,  'ROLE_OPERATIONS',   'Operations Team'),
(10, 'ROLE_SYSTEM',       'System Process Account');
SET IDENTITY_INSERT roles OFF;

-- ============================================================================
-- USERS (per tenant: admin_hq, manager_br1, teller_br1, maker_br2,
--         checker_br2, auditor) + SYSTEM_AUTO
-- Passwords are BCrypt-hashed value of 'Password@123'
-- ============================================================================
SET IDENTITY_INSERT users ON;
INSERT INTO users (id, tenant_id, tenant_scope, username, password, full_name, email, phone, branch_code, branch_id, is_active, is_locked, failed_login_attempts, created_at, updated_at)
VALUES
-- System user (no tenant, global)
(1,  NULL, 'GLOBAL', 'SYSTEM_AUTO',   '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'System Auto-Authorizer', 'system@ledgora.in',       NULL,            NULL, NULL, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Tenant 1 users
(2,  1, 'SINGLE', 'admin_hq_t1',    '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Rajesh Kumar',         'rajesh@ledgora.in',       '+91-9876543210', 'BR001', 1, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(3,  1, 'SINGLE', 'manager_br1_t1', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Priya Sharma',         'priya@ledgora.in',        '+91-9876543211', 'BR001', 1, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(4,  1, 'SINGLE', 'teller_br1_t1',  '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Amit Patel',           'amit@ledgora.in',         '+91-9876543212', 'BR001', 1, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(5,  1, 'SINGLE', 'maker_br2_t1',   '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Sneha Reddy',          'sneha@ledgora.in',        '+91-9876543213', 'BR002', 2, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(6,  1, 'SINGLE', 'checker_br2_t1', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Vikram Singh',         'vikram@ledgora.in',       '+91-9876543214', 'BR002', 2, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(7,  1, 'SINGLE', 'auditor_t1',     '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Deepa Nair',           'deepa@ledgora.in',        '+91-9876543215', 'BR001', 1, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Tenant 2 users
(8,  2, 'SINGLE', 'admin_hq_t2',    '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Suresh Mehta',         'suresh@ledgora-coop.in',  '+91-9876543220', 'BR003', 3, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(9,  2, 'SINGLE', 'manager_br1_t2', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Kavita Joshi',         'kavita@ledgora-coop.in',  '+91-9876543221', 'BR003', 3, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(10, 2, 'SINGLE', 'teller_br1_t2',  '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Rahul Verma',          'rahul@ledgora-coop.in',   '+91-9876543222', 'BR003', 3, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(11, 2, 'SINGLE', 'maker_br2_t2',   '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Anita Desai',          'anita@ledgora-coop.in',   '+91-9876543223', 'BR004', 4, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(12, 2, 'SINGLE', 'checker_br2_t2', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Manoj Tiwari',         'manoj@ledgora-coop.in',   '+91-9876543224', 'BR004', 4, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(13, 2, 'SINGLE', 'auditor_t2',     '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Pooja Gupta',          'pooja@ledgora-coop.in',   '+91-9876543225', 'BR003', 3, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT users OFF;

-- ============================================================================
-- USER_ROLES
-- ============================================================================
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 10),  -- SYSTEM_AUTO -> ROLE_SYSTEM
(2, 1),   -- admin_hq_t1 -> ROLE_ADMIN
(2, 8),   -- admin_hq_t1 -> ROLE_TENANT_ADMIN
(3, 2),   -- manager_br1_t1 -> ROLE_MANAGER
(3, 5),   -- manager_br1_t1 -> ROLE_CHECKER
(4, 3),   -- teller_br1_t1 -> ROLE_TELLER
(4, 4),   -- teller_br1_t1 -> ROLE_MAKER
(5, 4),   -- maker_br2_t1 -> ROLE_MAKER
(6, 5),   -- checker_br2_t1 -> ROLE_CHECKER
(7, 6),   -- auditor_t1 -> ROLE_AUDITOR
(8, 1),   -- admin_hq_t2 -> ROLE_ADMIN
(8, 8),   -- admin_hq_t2 -> ROLE_TENANT_ADMIN
(9, 2),   -- manager_br1_t2 -> ROLE_MANAGER
(9, 5),   -- manager_br1_t2 -> ROLE_CHECKER
(10, 3),  -- teller_br1_t2 -> ROLE_TELLER
(10, 4),  -- teller_br1_t2 -> ROLE_MAKER
(11, 4),  -- maker_br2_t2 -> ROLE_MAKER
(12, 5),  -- checker_br2_t2 -> ROLE_CHECKER
(13, 6);  -- auditor_t2 -> ROLE_AUDITOR

-- ============================================================================
-- GENERAL LEDGERS (GL Chart of Accounts per tenant)
-- ============================================================================
SET IDENTITY_INSERT general_ledgers ON;
INSERT INTO general_ledgers (id, tenant_id, gl_code, gl_name, description, account_type, parent_id, level_num, is_active, balance, normal_balance, created_at, updated_at) VALUES
-- Tenant 1 GL Hierarchy
(1,  1, 'GL-T1-ASSET',     'Assets',                    'Top-level asset group',              'ASSET',     NULL, 0, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(2,  1, 'GL-T1-LIABILITY',  'Liabilities',              'Top-level liability group',          'LIABILITY', NULL, 0, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(3,  1, 'GL-T1-EQUITY',     'Equity',                   'Top-level equity group',             'EQUITY',    NULL, 0, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(4,  1, 'GL-T1-REVENUE',    'Revenue',                  'Top-level revenue group',            'REVENUE',   NULL, 0, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(5,  1, 'GL-T1-EXPENSE',    'Expenses',                 'Top-level expense group',            'EXPENSE',   NULL, 0, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(6,  1, 'GL-T1-CASH',       'Cash in Hand',             'Cash GL for teller operations',      'ASSET',     1,   1, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(7,  1, 'GL-T1-SAVINGS',    'Savings Deposits',         'Savings account liability GL',       'LIABILITY', 2,   1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(8,  1, 'GL-T1-CURRENT',    'Current Deposits',         'Current account liability GL',       'LIABILITY', 2,   1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(9,  1, 'GL-T1-TD',         'Term Deposits',            'Term deposit liability GL',          'LIABILITY', 2,   1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(10, 1, 'GL-T1-CLR-OUT',    'IBC Outward Clearing',     'Inter-branch clearing outward',      'CLEARING',  1,   1, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(11, 1, 'GL-T1-CLR-IN',     'IBC Inward Clearing',      'Inter-branch clearing inward',       'CLEARING',  2,   1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(12, 1, 'GL-T1-SUSPENSE',   'Suspense Account',         'Suspense GL for parked entries',     'SUSPENSE',  2,   1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(13, 1, 'GL-T1-INT-INC',    'Interest Income',          'Interest earned on loans',           'REVENUE',   4,   1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(14, 1, 'GL-T1-INT-EXP',    'Interest Expense',         'Interest paid on deposits',          'EXPENSE',   5,   1, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(15, 1, 'GL-T1-FEE-INC',    'Fee Income',               'Service charges and fees',           'REVENUE',   4,   1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Tenant 2 GL Hierarchy
(16, 2, 'GL-T2-ASSET',      'Assets',                   'Top-level asset group',              'ASSET',     NULL, 0, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(17, 2, 'GL-T2-LIABILITY',  'Liabilities',              'Top-level liability group',          'LIABILITY', NULL, 0, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(18, 2, 'GL-T2-EQUITY',     'Equity',                   'Top-level equity group',             'EQUITY',    NULL, 0, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(19, 2, 'GL-T2-REVENUE',    'Revenue',                  'Top-level revenue group',            'REVENUE',   NULL, 0, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(20, 2, 'GL-T2-EXPENSE',    'Expenses',                 'Top-level expense group',            'EXPENSE',   NULL, 0, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(21, 2, 'GL-T2-CASH',       'Cash in Hand',             'Cash GL for teller operations',      'ASSET',     16,  1, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(22, 2, 'GL-T2-SAVINGS',    'Savings Deposits',         'Savings account liability GL',       'LIABILITY', 17,  1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(23, 2, 'GL-T2-CURRENT',    'Current Deposits',         'Current account liability GL',       'LIABILITY', 17,  1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(24, 2, 'GL-T2-TD',         'Term Deposits',            'Term deposit liability GL',          'LIABILITY', 17,  1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(25, 2, 'GL-T2-CLR-OUT',    'IBC Outward Clearing',     'Inter-branch clearing outward',      'CLEARING',  16,  1, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(26, 2, 'GL-T2-CLR-IN',     'IBC Inward Clearing',      'Inter-branch clearing inward',       'CLEARING',  17,  1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(27, 2, 'GL-T2-SUSPENSE',   'Suspense Account',         'Suspense GL for parked entries',     'SUSPENSE',  17,  1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(28, 2, 'GL-T2-INT-INC',    'Interest Income',          'Interest earned on loans',           'REVENUE',   19,  1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
(29, 2, 'GL-T2-INT-EXP',    'Interest Expense',         'Interest paid on deposits',          'EXPENSE',   20,  1, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(30, 2, 'GL-T2-FEE-INC',    'Fee Income',               'Service charges and fees',           'REVENUE',   19,  1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT general_ledgers OFF;

-- ============================================================================
-- ACCOUNTS
-- Tenant 1: 10 Savings, 5 Current, 5 Term Deposit + internal (Cash, Clearing, Suspense)
-- Tenant 2: 10 Savings, 5 Current, 5 Term Deposit + internal (Cash, Clearing, Suspense)
-- ============================================================================
SET IDENTITY_INSERT accounts ON;
INSERT INTO accounts (id, tenant_id, account_number, account_name, account_type, status, balance, currency, branch_code, branch_id, customer_name, gl_account_code, freeze_level, npa_flag, approval_status, created_at, version, updated_at) VALUES
-- ── Tenant 1: Internal / GL Accounts ──
(1,  1, 'T1-CASH-BR001',     'Cash Account BR001',            'INTERNAL_ACCOUNT', 'ACTIVE', 5000000.0000, 'INR', 'BR001', 1, NULL,                'GL-T1-CASH',    'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(2,  1, 'T1-CASH-BR002',     'Cash Account BR002',            'INTERNAL_ACCOUNT', 'ACTIVE', 3000000.0000, 'INR', 'BR002', 2, NULL,                'GL-T1-CASH',    'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(3,  1, 'T1-CLR-OUT-BR001',  'IBC Outward Clearing BR001',    'CLEARING_ACCOUNT', 'ACTIVE', 0.0000,       'INR', 'BR001', 1, NULL,                'GL-T1-CLR-OUT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(4,  1, 'T1-CLR-IN-BR001',   'IBC Inward Clearing BR001',     'CLEARING_ACCOUNT', 'ACTIVE', 0.0000,       'INR', 'BR001', 1, NULL,                'GL-T1-CLR-IN',  'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(5,  1, 'T1-CLR-OUT-BR002',  'IBC Outward Clearing BR002',    'CLEARING_ACCOUNT', 'ACTIVE', 0.0000,       'INR', 'BR002', 2, NULL,                'GL-T1-CLR-OUT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(6,  1, 'T1-CLR-IN-BR002',   'IBC Inward Clearing BR002',     'CLEARING_ACCOUNT', 'ACTIVE', 0.0000,       'INR', 'BR002', 2, NULL,                'GL-T1-CLR-IN',  'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(7,  1, 'T1-SUSPENSE',       'Suspense Account',              'SUSPENSE_ACCOUNT', 'ACTIVE', 0.0000,       'INR', 'BR001', 1, NULL,                'GL-T1-SUSPENSE','NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
-- ── Tenant 1: Savings Accounts (10) ──
(8,  1, 'T1-SAV-0001', 'Arun Joshi Savings',        'SAVINGS', 'ACTIVE',  125000.0000, 'INR', 'BR001', 1, 'Arun Joshi',        'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(9,  1, 'T1-SAV-0002', 'Meena Kapoor Savings',      'SAVINGS', 'ACTIVE',   87500.0000, 'INR', 'BR001', 1, 'Meena Kapoor',      'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(10, 1, 'T1-SAV-0003', 'Ravi Shankar Savings',      'SAVINGS', 'ACTIVE',  250000.0000, 'INR', 'BR001', 1, 'Ravi Shankar',      'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(11, 1, 'T1-SAV-0004', 'Sunita Devi Savings',       'SAVINGS', 'ACTIVE',   45000.0000, 'INR', 'BR001', 1, 'Sunita Devi',       'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(12, 1, 'T1-SAV-0005', 'Kiran Bhat Savings',        'SAVINGS', 'ACTIVE',  175000.0000, 'INR', 'BR001', 1, 'Kiran Bhat',        'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(13, 1, 'T1-SAV-0006', 'Ajay Mishra Savings',       'SAVINGS', 'ACTIVE',   62000.0000, 'INR', 'BR002', 2, 'Ajay Mishra',       'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(14, 1, 'T1-SAV-0007', 'Geeta Rao Savings',         'SAVINGS', 'ACTIVE',  310000.0000, 'INR', 'BR002', 2, 'Geeta Rao',         'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(15, 1, 'T1-SAV-0008', 'Prakash Iyer Savings',      'SAVINGS', 'ACTIVE',   98000.0000, 'INR', 'BR002', 2, 'Prakash Iyer',      'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(16, 1, 'T1-SAV-0009', 'Lakshmi Nair Savings',      'SAVINGS', 'ACTIVE',  145000.0000, 'INR', 'BR002', 2, 'Lakshmi Nair',      'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(17, 1, 'T1-SAV-0010', 'Ramesh Gupta Savings',      'SAVINGS', 'ACTIVE',  220000.0000, 'INR', 'BR002', 2, 'Ramesh Gupta',      'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
-- ── Tenant 1: Current Accounts (5) ──
(18, 1, 'T1-CUR-0001', 'ABC Enterprises Current',   'CURRENT', 'ACTIVE',  850000.0000, 'INR', 'BR001', 1, 'ABC Enterprises',   'GL-T1-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(19, 1, 'T1-CUR-0002', 'XYZ Trading Current',       'CURRENT', 'ACTIVE',  425000.0000, 'INR', 'BR001', 1, 'XYZ Trading Co',    'GL-T1-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(20, 1, 'T1-CUR-0003', 'Omega Solutions Current',   'CURRENT', 'ACTIVE', 1200000.0000, 'INR', 'BR002', 2, 'Omega Solutions',   'GL-T1-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(21, 1, 'T1-CUR-0004', 'Delta Services Current',    'CURRENT', 'ACTIVE',  675000.0000, 'INR', 'BR002', 2, 'Delta Services',    'GL-T1-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(22, 1, 'T1-CUR-0005', 'Sigma Corp Current',        'CURRENT', 'ACTIVE',  950000.0000, 'INR', 'BR001', 1, 'Sigma Corp',        'GL-T1-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
-- ── Tenant 1: Term Deposit Accounts (5) ──
(23, 1, 'T1-TD-0001', 'Arun Joshi FD',             'FIXED_DEPOSIT', 'ACTIVE',  500000.0000, 'INR', 'BR001', 1, 'Arun Joshi',   'GL-T1-TD', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(24, 1, 'T1-TD-0002', 'Meena Kapoor FD',           'FIXED_DEPOSIT', 'ACTIVE', 1000000.0000, 'INR', 'BR001', 1, 'Meena Kapoor', 'GL-T1-TD', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(25, 1, 'T1-TD-0003', 'Ravi Shankar FD',           'FIXED_DEPOSIT', 'ACTIVE',  750000.0000, 'INR', 'BR002', 2, 'Ravi Shankar', 'GL-T1-TD', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(26, 1, 'T1-TD-0004', 'ABC Enterprises FD',        'FIXED_DEPOSIT', 'ACTIVE', 2000000.0000, 'INR', 'BR001', 1, 'ABC Enterprises','GL-T1-TD','NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(27, 1, 'T1-TD-0005', 'XYZ Trading FD',            'FIXED_DEPOSIT', 'ACTIVE',  300000.0000, 'INR', 'BR002', 2, 'XYZ Trading Co','GL-T1-TD', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),

-- ── Tenant 2: Internal / GL Accounts ──
(28, 2, 'T2-CASH-BR003',     'Cash Account BR003',            'INTERNAL_ACCOUNT', 'ACTIVE', 4000000.0000, 'INR', 'BR003', 3, NULL,                'GL-T2-CASH',    'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(29, 2, 'T2-CASH-BR004',     'Cash Account BR004',            'INTERNAL_ACCOUNT', 'ACTIVE', 2500000.0000, 'INR', 'BR004', 4, NULL,                'GL-T2-CASH',    'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(30, 2, 'T2-CLR-OUT-BR003',  'IBC Outward Clearing BR003',    'CLEARING_ACCOUNT', 'ACTIVE', 0.0000,       'INR', 'BR003', 3, NULL,                'GL-T2-CLR-OUT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(31, 2, 'T2-CLR-IN-BR003',   'IBC Inward Clearing BR003',     'CLEARING_ACCOUNT', 'ACTIVE', 0.0000,       'INR', 'BR003', 3, NULL,                'GL-T2-CLR-IN',  'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(32, 2, 'T2-CLR-OUT-BR004',  'IBC Outward Clearing BR004',    'CLEARING_ACCOUNT', 'ACTIVE', 0.0000,       'INR', 'BR004', 4, NULL,                'GL-T2-CLR-OUT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(33, 2, 'T2-CLR-IN-BR004',   'IBC Inward Clearing BR004',     'CLEARING_ACCOUNT', 'ACTIVE', 0.0000,       'INR', 'BR004', 4, NULL,                'GL-T2-CLR-IN',  'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(34, 2, 'T2-SUSPENSE',       'Suspense Account',              'SUSPENSE_ACCOUNT', 'ACTIVE', 0.0000,       'INR', 'BR003', 3, NULL,                'GL-T2-SUSPENSE','NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
-- ── Tenant 2: Savings Accounts (10) ──
(35, 2, 'T2-SAV-0001', 'Mohan Lal Savings',         'SAVINGS', 'ACTIVE',  110000.0000, 'INR', 'BR003', 3, 'Mohan Lal',         'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(36, 2, 'T2-SAV-0002', 'Sita Ram Savings',          'SAVINGS', 'ACTIVE',   95000.0000, 'INR', 'BR003', 3, 'Sita Ram',          'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(37, 2, 'T2-SAV-0003', 'Gopal Krishna Savings',     'SAVINGS', 'ACTIVE',  180000.0000, 'INR', 'BR003', 3, 'Gopal Krishna',     'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(38, 2, 'T2-SAV-0004', 'Radha Devi Savings',        'SAVINGS', 'ACTIVE',   55000.0000, 'INR', 'BR003', 3, 'Radha Devi',        'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(39, 2, 'T2-SAV-0005', 'Vinod Khanna Savings',      'SAVINGS', 'ACTIVE',  200000.0000, 'INR', 'BR003', 3, 'Vinod Khanna',      'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(40, 2, 'T2-SAV-0006', 'Nisha Agarwal Savings',     'SAVINGS', 'ACTIVE',   72000.0000, 'INR', 'BR004', 4, 'Nisha Agarwal',     'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(41, 2, 'T2-SAV-0007', 'Hari Prasad Savings',       'SAVINGS', 'ACTIVE',  290000.0000, 'INR', 'BR004', 4, 'Hari Prasad',       'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(42, 2, 'T2-SAV-0008', 'Uma Shankar Savings',       'SAVINGS', 'ACTIVE',   88000.0000, 'INR', 'BR004', 4, 'Uma Shankar',       'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(43, 2, 'T2-SAV-0009', 'Kamla Nehru Savings',       'SAVINGS', 'ACTIVE',  155000.0000, 'INR', 'BR004', 4, 'Kamla Nehru',       'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(44, 2, 'T2-SAV-0010', 'Dev Anand Savings',         'SAVINGS', 'ACTIVE',  230000.0000, 'INR', 'BR004', 4, 'Dev Anand',         'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
-- ── Tenant 2: Current Accounts (5) ──
(45, 2, 'T2-CUR-0001', 'Alpha Industries Current',  'CURRENT', 'ACTIVE',  780000.0000, 'INR', 'BR003', 3, 'Alpha Industries',  'GL-T2-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(46, 2, 'T2-CUR-0002', 'Beta Traders Current',      'CURRENT', 'ACTIVE',  390000.0000, 'INR', 'BR003', 3, 'Beta Traders',      'GL-T2-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(47, 2, 'T2-CUR-0003', 'Gamma Tech Current',        'CURRENT', 'ACTIVE', 1100000.0000, 'INR', 'BR004', 4, 'Gamma Tech',        'GL-T2-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(48, 2, 'T2-CUR-0004', 'Zeta Logistics Current',    'CURRENT', 'ACTIVE',  560000.0000, 'INR', 'BR004', 4, 'Zeta Logistics',    'GL-T2-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(49, 2, 'T2-CUR-0005', 'Theta Pharma Current',      'CURRENT', 'ACTIVE',  920000.0000, 'INR', 'BR003', 3, 'Theta Pharma',      'GL-T2-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
-- ── Tenant 2: Term Deposit Accounts (5) ──
(50, 2, 'T2-TD-0001', 'Mohan Lal FD',              'FIXED_DEPOSIT', 'ACTIVE',  450000.0000, 'INR', 'BR003', 3, 'Mohan Lal',     'GL-T2-TD', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(51, 2, 'T2-TD-0002', 'Sita Ram FD',               'FIXED_DEPOSIT', 'ACTIVE',  800000.0000, 'INR', 'BR003', 3, 'Sita Ram',      'GL-T2-TD', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(52, 2, 'T2-TD-0003', 'Alpha Industries FD',       'FIXED_DEPOSIT', 'ACTIVE', 1500000.0000, 'INR', 'BR004', 4, 'Alpha Industries','GL-T2-TD','NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(53, 2, 'T2-TD-0004', 'Beta Traders FD',           'FIXED_DEPOSIT', 'ACTIVE',  250000.0000, 'INR', 'BR004', 4, 'Beta Traders',  'GL-T2-TD', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(54, 2, 'T2-TD-0005', 'Gamma Tech FD',             'FIXED_DEPOSIT', 'ACTIVE', 1800000.0000, 'INR', 'BR003', 3, 'Gamma Tech',    'GL-T2-TD', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME());
SET IDENTITY_INSERT accounts OFF;

-- ============================================================================
-- TRANSACTION BATCHES
-- ============================================================================
SET IDENTITY_INSERT transaction_batches ON;
INSERT INTO transaction_batches (id, tenant_id, batch_type, batch_code, business_date, version, status, total_debit, total_credit, transaction_count, created_at) VALUES
(1, 1, 'TELLER',  'BATCH-T1-TEL-20260314', '2026-03-14', 0, 'OPEN', 0.0000, 0.0000, 0, SYSUTCDATETIME()),
(2, 1, 'ONLINE',  'BATCH-T1-ONL-20260314', '2026-03-14', 0, 'OPEN', 0.0000, 0.0000, 0, SYSUTCDATETIME()),
(3, 2, 'TELLER',  'BATCH-T2-TEL-20260314', '2026-03-14', 0, 'OPEN', 0.0000, 0.0000, 0, SYSUTCDATETIME()),
(4, 2, 'ONLINE',  'BATCH-T2-ONL-20260314', '2026-03-14', 0, 'OPEN', 0.0000, 0.0000, 0, SYSUTCDATETIME());
SET IDENTITY_INSERT transaction_batches OFF;

-- ============================================================================
-- SCROLL SEQUENCES
-- ============================================================================
SET IDENTITY_INSERT scroll_sequences ON;
INSERT INTO scroll_sequences (id, tenant_id, branch_id, posting_date, last_scroll_no) VALUES
(1, 1, 1, '2026-03-14', 20),
(2, 1, 2, '2026-03-14', 20),
(3, 2, 3, '2026-03-14', 20),
(4, 2, 4, '2026-03-14', 20);
SET IDENTITY_INSERT scroll_sequences OFF;

-- ============================================================================
-- TRANSACTIONS + VOUCHERS + LEDGER JOURNALS + LEDGER ENTRIES
-- All transactions are for Tenant 1 for brevity; Tenant 2 mirrors the pattern.
-- Each transaction creates balanced DR/CR legs.
-- ============================================================================

-- ── TXN 1: Deposit 50,000 to T1-SAV-0001 (Teller at BR001) ──
SET IDENTITY_INSERT transactions ON;
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (1, 1, 1, 1, 'DEP-20260314-000001', 'DEPOSIT', 'COMPLETED', 50000.0000, 'INR', 'TELLER', NULL, 8, 'Cash Deposit', '2026-03-14', '2026-03-14', 4, 4, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 2: Withdrawal 20,000 from T1-SAV-0003 (Teller at BR001) ──
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (2, 1, 1, 1, 'WDR-20260314-000001', 'WITHDRAWAL', 'COMPLETED', 20000.0000, 'INR', 'TELLER', 10, NULL, 'Cash Withdrawal', '2026-03-14', '2026-03-14', 4, 4, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 3: Same-branch transfer 15,000 from T1-SAV-0001 to T1-SAV-0002 (BR001) ──
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (3, 1, 1, 1, 'TRF-20260314-000001', 'TRANSFER', 'COMPLETED', 15000.0000, 'INR', 'TELLER', 8, 9, 'Internal Transfer', '2026-03-14', '2026-03-14', 4, 4, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 4: Cross-branch IBT 25,000 from T1-SAV-0001 (BR001) to T1-SAV-0006 (BR002) ──
-- This generates 4 vouchers: DR Customer A, CR IBC_OUT_A, DR IBC_IN_B, CR Customer B
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (4, 1, 1, 1, 'TRF-20260314-000002', 'TRANSFER', 'COMPLETED', 25000.0000, 'INR', 'TELLER', 8, 13, 'Cross-Branch IBT Transfer', '2026-03-14', '2026-03-14', 4, 4, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 5: Deposit 100,000 to T1-CUR-0001 (large deposit) ──
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (5, 1, 1, 1, 'DEP-20260314-000002', 'DEPOSIT', 'COMPLETED', 100000.0000, 'INR', 'TELLER', NULL, 18, 'Business Deposit', '2026-03-14', '2026-03-14', 4, 4, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 6: Suspense parked case — deposit intended for frozen account ──
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (6, 1, 1, 1, 'DEP-20260314-000003', 'DEPOSIT', 'COMPLETED', 10000.0000, 'INR', 'TELLER', NULL, 7, 'Suspense Parked - Intended for frozen account', '2026-03-14', '2026-03-14', 4, 4, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 7: Suspense resolution — move from suspense to intended account ──
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (7, 1, 1, 1, 'TRF-20260314-000003', 'TRANSFER', 'COMPLETED', 10000.0000, 'INR', 'TELLER', 7, 11, 'Suspense Resolution - Move to intended account', '2026-03-14', '2026-03-14', 3, 3, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 8: Hard ceiling violation attempt (recorded as FAILED) ──
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (8, 1, 1, 1, 'DEP-20260314-000004', 'DEPOSIT', 'FAILED', 50000000.0000, 'INR', 'TELLER', NULL, 8, 'REJECTED: Hard ceiling breach - 5Cr deposit', '2026-03-14', '2026-03-14', 4, 4, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 9: Velocity breach simulation (recorded as FAILED) ──
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (9, 1, 1, 1, 'WDR-20260314-000002', 'WITHDRAWAL', 'FAILED', 5000.0000, 'INR', 'TELLER', 8, NULL, 'REJECTED: Velocity fraud check - rapid transactions', '2026-03-14', '2026-03-14', 4, 4, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 10: Tenant 2 Deposit 75,000 to T2-SAV-0001 ──
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (10, 2, 3, 3, 'DEP-20260314-T2-0001', 'DEPOSIT', 'COMPLETED', 75000.0000, 'INR', 'TELLER', NULL, 35, 'Cash Deposit', '2026-03-14', '2026-03-14', 10, 10, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 11: Tenant 2 Withdrawal 30,000 from T2-CUR-0001 ──
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (11, 2, 3, 3, 'WDR-20260314-T2-0001', 'WITHDRAWAL', 'COMPLETED', 30000.0000, 'INR', 'TELLER', 45, NULL, 'Cash Withdrawal', '2026-03-14', '2026-03-14', 10, 10, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── TXN 12: Tenant 2 Cross-branch IBT 40,000 from T2-SAV-0001 (BR003) to T2-SAV-0006 (BR004) ──
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (12, 2, 3, 3, 'TRF-20260314-T2-0001', 'TRANSFER', 'COMPLETED', 40000.0000, 'INR', 'TELLER', 35, 40, 'Cross-Branch IBT Transfer', '2026-03-14', '2026-03-14', 10, 10, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT transactions OFF;

-- ============================================================================
-- VOUCHERS (balanced DR/CR pairs per transaction)
-- ============================================================================
SET IDENTITY_INSERT vouchers ON;
INSERT INTO vouchers (id, voucher_number, tenant_id, branch_id, transaction_id, batch_code, set_no, scroll_no, entry_date, posting_date, value_date, dr_cr, account_id, gl_account_id, transaction_amount, local_currency_amount, currency, maker_id, checker_id, auth_flag, post_flag, cancel_flag, financial_effect_flag, total_debit, total_credit, narration, version, created_at, updated_at) VALUES
-- TXN 1: Deposit 50,000 → DR Cash, CR Customer
(1,  'TENANT001-BR001-20260314-000001', 1, 1, 1, 'BATCH-T1-TEL-20260314', 1, 1,  '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 1,  6,  50000.0000, 50000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 50000.0000, 0.0000, 'Cash deposit - cash leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(2,  'TENANT001-BR001-20260314-000002', 1, 1, 1, 'BATCH-T1-TEL-20260314', 1, 2,  '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 8,  7,  50000.0000, 50000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 50000.0000, 'Cash deposit - customer leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- TXN 2: Withdrawal 20,000 → DR Customer, CR Cash
(3,  'TENANT001-BR001-20260314-000003', 1, 1, 2, 'BATCH-T1-TEL-20260314', 1, 3,  '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 10, 7,  20000.0000, 20000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 20000.0000, 0.0000, 'Cash withdrawal - customer leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(4,  'TENANT001-BR001-20260314-000004', 1, 1, 2, 'BATCH-T1-TEL-20260314', 1, 4,  '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 1,  6,  20000.0000, 20000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 20000.0000, 'Cash withdrawal - cash leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- TXN 3: Same-branch transfer 15,000 → DR Source, CR Dest
(5,  'TENANT001-BR001-20260314-000005', 1, 1, 3, 'BATCH-T1-TEL-20260314', 1, 5,  '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 8,  7,  15000.0000, 15000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 15000.0000, 0.0000, 'Transfer to T1-SAV-0002', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(6,  'TENANT001-BR001-20260314-000006', 1, 1, 3, 'BATCH-T1-TEL-20260314', 1, 6,  '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 9,  7,  15000.0000, 15000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 15000.0000, 'Transfer from T1-SAV-0001', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- TXN 4: Cross-branch IBT 25,000 (4-voucher model)
-- Branch A (BR001): DR Customer A (SAV-0001), CR IBC_OUT_BR001
(7,  'TENANT001-BR001-20260314-000007', 1, 1, 4, 'BATCH-T1-TEL-20260314', 1, 7,  '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 8,  7,  25000.0000, 25000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 25000.0000, 0.0000, 'IBC Transfer DR: T1-SAV-0001', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(8,  'TENANT001-BR001-20260314-000008', 1, 1, 4, 'BATCH-T1-TEL-20260314', 1, 8,  '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 3, 10,  25000.0000, 25000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 25000.0000, 'IBC OUT CR: BR001', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Branch B (BR002): DR IBC_IN_BR002, CR Customer B (SAV-0006)
(9,  'TENANT001-BR002-20260314-000001', 1, 2, 4, 'BATCH-T1-TEL-20260314', 1, 9,  '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 6, 11,  25000.0000, 25000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 25000.0000, 0.0000, 'IBC IN DR: BR002', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(10, 'TENANT001-BR002-20260314-000002', 1, 2, 4, 'BATCH-T1-TEL-20260314', 1, 10, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 13, 7,  25000.0000, 25000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 25000.0000, 'IBC Transfer CR: T1-SAV-0006', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- TXN 5: Deposit 100,000 → DR Cash, CR Customer
(11, 'TENANT001-BR001-20260314-000009', 1, 1, 5, 'BATCH-T1-TEL-20260314', 1, 11, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 1,  6, 100000.0000, 100000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 100000.0000, 0.0000, 'Business deposit - cash leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(12, 'TENANT001-BR001-20260314-000010', 1, 1, 5, 'BATCH-T1-TEL-20260314', 1, 12, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 18, 8, 100000.0000, 100000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 100000.0000, 'Business deposit - customer leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- TXN 6: Suspense park 10,000 → DR Cash, CR Suspense
(13, 'TENANT001-BR001-20260314-000011', 1, 1, 6, 'BATCH-T1-TEL-20260314', 1, 13, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 1,  6,  10000.0000, 10000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 10000.0000, 0.0000, 'Suspense park - cash leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(14, 'TENANT001-BR001-20260314-000012', 1, 1, 6, 'BATCH-T1-TEL-20260314', 1, 14, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 7, 12,  10000.0000, 10000.0000, 'INR', 4, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 10000.0000, 'Suspense park - suspense leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- TXN 7: Suspense resolution 10,000 → DR Suspense, CR Customer
(15, 'TENANT001-BR001-20260314-000013', 1, 1, 7, 'BATCH-T1-TEL-20260314', 1, 15, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 7, 12,  10000.0000, 10000.0000, 'INR', 3, 1, 'Y', 'Y', 'N', 'Y', 10000.0000, 0.0000, 'Suspense resolution - DR suspense', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(16, 'TENANT001-BR001-20260314-000014', 1, 1, 7, 'BATCH-T1-TEL-20260314', 1, 16, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 11, 7,  10000.0000, 10000.0000, 'INR', 3, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 10000.0000, 'Suspense resolution - CR customer', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Tenant 2: TXN 10 Deposit 75,000
(17, 'TENANT002-BR003-20260314-000001', 2, 3, 10, 'BATCH-T2-TEL-20260314', 1, 1, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 28, 21, 75000.0000, 75000.0000, 'INR', 10, 1, 'Y', 'Y', 'N', 'Y', 75000.0000, 0.0000, 'Cash deposit - cash leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(18, 'TENANT002-BR003-20260314-000002', 2, 3, 10, 'BATCH-T2-TEL-20260314', 1, 2, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 35, 22, 75000.0000, 75000.0000, 'INR', 10, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 75000.0000, 'Cash deposit - customer leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Tenant 2: TXN 11 Withdrawal 30,000
(19, 'TENANT002-BR003-20260314-000003', 2, 3, 11, 'BATCH-T2-TEL-20260314', 1, 3, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 45, 23, 30000.0000, 30000.0000, 'INR', 10, 1, 'Y', 'Y', 'N', 'Y', 30000.0000, 0.0000, 'Cash withdrawal - customer leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(20, 'TENANT002-BR003-20260314-000004', 2, 3, 11, 'BATCH-T2-TEL-20260314', 1, 4, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 28, 21, 30000.0000, 30000.0000, 'INR', 10, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 30000.0000, 'Cash withdrawal - cash leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Tenant 2: TXN 12 Cross-branch IBT 40,000 (4-voucher model)
(21, 'TENANT002-BR003-20260314-000005', 2, 3, 12, 'BATCH-T2-TEL-20260314', 1, 5, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 35, 22, 40000.0000, 40000.0000, 'INR', 10, 1, 'Y', 'Y', 'N', 'Y', 40000.0000, 0.0000, 'IBC Transfer DR: T2-SAV-0001', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(22, 'TENANT002-BR003-20260314-000006', 2, 3, 12, 'BATCH-T2-TEL-20260314', 1, 6, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 30, 25, 40000.0000, 40000.0000, 'INR', 10, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 40000.0000, 'IBC OUT CR: BR003', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(23, 'TENANT002-BR004-20260314-000001', 2, 4, 12, 'BATCH-T2-TEL-20260314', 1, 7, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 33, 26, 40000.0000, 40000.0000, 'INR', 10, 1, 'Y', 'Y', 'N', 'Y', 40000.0000, 0.0000, 'IBC IN DR: BR004', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(24, 'TENANT002-BR004-20260314-000002', 2, 4, 12, 'BATCH-T2-TEL-20260314', 1, 8, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 40, 22, 40000.0000, 40000.0000, 'INR', 10, 1, 'Y', 'Y', 'N', 'Y', 0.0000, 40000.0000, 'IBC Transfer CR: T2-SAV-0006', 0, SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT vouchers OFF;

-- ============================================================================
-- LEDGER JOURNALS
-- ============================================================================
SET IDENTITY_INSERT ledger_journals ON;
INSERT INTO ledger_journals (id, tenant_id, transaction_id, description, business_date, created_at) VALUES
(1,  1, 1, 'Deposit 50,000 to T1-SAV-0001',                               '2026-03-14', SYSUTCDATETIME()),
(2,  1, 2, 'Withdrawal 20,000 from T1-SAV-0003',                          '2026-03-14', SYSUTCDATETIME()),
(3,  1, 3, 'Transfer 15,000 T1-SAV-0001 to T1-SAV-0002',                  '2026-03-14', SYSUTCDATETIME()),
(4,  1, 4, 'IBT 25,000 T1-SAV-0001 (BR001) to T1-SAV-0006 (BR002)',      '2026-03-14', SYSUTCDATETIME()),
(5,  1, 5, 'Deposit 100,000 to T1-CUR-0001',                              '2026-03-14', SYSUTCDATETIME()),
(6,  1, 6, 'Suspense park 10,000 (intended for frozen account)',           '2026-03-14', SYSUTCDATETIME()),
(7,  1, 7, 'Suspense resolution 10,000 to T1-SAV-0004',                   '2026-03-14', SYSUTCDATETIME()),
(8,  2, 10, 'Deposit 75,000 to T2-SAV-0001',                              '2026-03-14', SYSUTCDATETIME()),
(9,  2, 11, 'Withdrawal 30,000 from T2-CUR-0001',                         '2026-03-14', SYSUTCDATETIME()),
(10, 2, 12, 'IBT 40,000 T2-SAV-0001 (BR003) to T2-SAV-0006 (BR004)',     '2026-03-14', SYSUTCDATETIME());
SET IDENTITY_INSERT ledger_journals OFF;

-- ============================================================================
-- LEDGER ENTRIES (Immutable — each journal has balanced DR/CR)
-- ============================================================================
SET IDENTITY_INSERT ledger_entries ON;
INSERT INTO ledger_entries (id, tenant_id, journal_id, transaction_id, account_id, gl_account_id, gl_account_code, entry_type, amount, balance_after, currency, business_date, posting_time, narration, voucher_id, created_at) VALUES
-- Journal 1: Deposit 50,000 (DR cash, CR customer) → balanced
(1,  1, 1, 1, 1,  6,  'GL-T1-CASH',    'DEBIT',  50000.0000, 5050000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Cash deposit - cash leg',     1, SYSUTCDATETIME()),
(2,  1, 1, 1, 8,  7,  'GL-T1-SAVINGS', 'CREDIT', 50000.0000,  175000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Cash deposit - customer leg',  2, SYSUTCDATETIME()),
-- Journal 2: Withdrawal 20,000 (DR customer, CR cash) → balanced
(3,  1, 2, 2, 10, 7,  'GL-T1-SAVINGS', 'DEBIT',  20000.0000,  230000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Cash withdrawal - customer leg', 3, SYSUTCDATETIME()),
(4,  1, 2, 2, 1,  6,  'GL-T1-CASH',    'CREDIT', 20000.0000, 5030000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Cash withdrawal - cash leg',    4, SYSUTCDATETIME()),
-- Journal 3: Transfer 15,000 (DR source, CR dest) → balanced
(5,  1, 3, 3, 8,  7,  'GL-T1-SAVINGS', 'DEBIT',  15000.0000,  160000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Transfer to T1-SAV-0002',     5, SYSUTCDATETIME()),
(6,  1, 3, 3, 9,  7,  'GL-T1-SAVINGS', 'CREDIT', 15000.0000,  102500.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Transfer from T1-SAV-0001',   6, SYSUTCDATETIME()),
-- Journal 4: IBT 25,000 (4 entries) → balanced
(7,  1, 4, 4, 8,  7,  'GL-T1-SAVINGS', 'DEBIT',  25000.0000,  135000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'IBC Transfer DR: T1-SAV-0001', 7, SYSUTCDATETIME()),
(8,  1, 4, 4, 3, 10,  'GL-T1-CLR-OUT', 'CREDIT', 25000.0000,   25000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'IBC OUT CR: BR001',            8, SYSUTCDATETIME()),
(9,  1, 4, 4, 6, 11,  'GL-T1-CLR-IN',  'DEBIT',  25000.0000,   25000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'IBC IN DR: BR002',             9, SYSUTCDATETIME()),
(10, 1, 4, 4, 13, 7,  'GL-T1-SAVINGS', 'CREDIT', 25000.0000,   87000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'IBC Transfer CR: T1-SAV-0006',10, SYSUTCDATETIME()),
-- Journal 5: Deposit 100,000 (DR cash, CR customer) → balanced
(11, 1, 5, 5, 1,  6,  'GL-T1-CASH',    'DEBIT', 100000.0000, 5130000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Business deposit - cash leg',   11, SYSUTCDATETIME()),
(12, 1, 5, 5, 18, 8,  'GL-T1-CURRENT', 'CREDIT',100000.0000,  950000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Business deposit - customer leg',12, SYSUTCDATETIME()),
-- Journal 6: Suspense park 10,000 (DR cash, CR suspense) → balanced
(13, 1, 6, 6, 1,  6,  'GL-T1-CASH',    'DEBIT',  10000.0000, 5140000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Suspense park - cash leg',    13, SYSUTCDATETIME()),
(14, 1, 6, 6, 7, 12,  'GL-T1-SUSPENSE','CREDIT', 10000.0000,   10000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Suspense park - suspense leg', 14, SYSUTCDATETIME()),
-- Journal 7: Suspense resolution 10,000 (DR suspense, CR customer) → balanced
(15, 1, 7, 7, 7, 12,  'GL-T1-SUSPENSE','DEBIT',  10000.0000,       0.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Suspense resolution - DR suspense', 15, SYSUTCDATETIME()),
(16, 1, 7, 7, 11, 7,  'GL-T1-SAVINGS', 'CREDIT', 10000.0000,   55000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Suspense resolution - CR customer', 16, SYSUTCDATETIME()),
-- Journal 8: T2 Deposit 75,000
(17, 2, 8, 10, 28, 21, 'GL-T2-CASH',    'DEBIT',  75000.0000, 4075000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Cash deposit - cash leg',     17, SYSUTCDATETIME()),
(18, 2, 8, 10, 35, 22, 'GL-T2-SAVINGS', 'CREDIT', 75000.0000,  185000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Cash deposit - customer leg',  18, SYSUTCDATETIME()),
-- Journal 9: T2 Withdrawal 30,000
(19, 2, 9, 11, 45, 23, 'GL-T2-CURRENT', 'DEBIT',  30000.0000,  750000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Cash withdrawal - customer leg', 19, SYSUTCDATETIME()),
(20, 2, 9, 11, 28, 21, 'GL-T2-CASH',    'CREDIT', 30000.0000, 4045000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Cash withdrawal - cash leg',    20, SYSUTCDATETIME()),
-- Journal 10: T2 IBT 40,000 (4 entries)
(21, 2, 10, 12, 35, 22, 'GL-T2-SAVINGS', 'DEBIT',  40000.0000,  145000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'IBC Transfer DR: T2-SAV-0001', 21, SYSUTCDATETIME()),
(22, 2, 10, 12, 30, 25, 'GL-T2-CLR-OUT', 'CREDIT', 40000.0000,   40000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'IBC OUT CR: BR003',            22, SYSUTCDATETIME()),
(23, 2, 10, 12, 33, 26, 'GL-T2-CLR-IN',  'DEBIT',  40000.0000,   40000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'IBC IN DR: BR004',             23, SYSUTCDATETIME()),
(24, 2, 10, 12, 40, 22, 'GL-T2-SAVINGS', 'CREDIT', 40000.0000,  112000.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'IBC Transfer CR: T2-SAV-0006', 24, SYSUTCDATETIME());
SET IDENTITY_INSERT ledger_entries OFF;

-- ============================================================================
-- INTER-BRANCH TRANSFERS
-- ============================================================================
SET IDENTITY_INSERT inter_branch_transfers ON;
INSERT INTO inter_branch_transfers (id, tenant_id, from_branch_id, to_branch_id, amount, currency, status, reference_transaction_id, business_date, narration, created_by_id, version, created_at, updated_at) VALUES
(1, 1, 1, 2, 25000.0000, 'INR', 'RECEIVED', 4, '2026-03-14', 'IBC: T1-SAV-0001 to T1-SAV-0006', 4, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(2, 2, 3, 4, 40000.0000, 'INR', 'RECEIVED', 12, '2026-03-14', 'IBC: T2-SAV-0001 to T2-SAV-0006', 10, 0, SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT inter_branch_transfers OFF;

-- ============================================================================
-- SUSPENSE CASES
-- ============================================================================
SET IDENTITY_INSERT suspense_cases ON;
INSERT INTO suspense_cases (id, tenant_id, original_transaction_id, posted_voucher_id, suspense_voucher_id, intended_account_id, suspense_account_id, amount, currency, reason_code, reason_detail, status, business_date, resolution_voucher_id, resolved_by_id, resolution_checker_id, resolution_remarks, resolved_at, created_at, updated_at) VALUES
(1, 1, 6, 13, 14, 11, 7, 10000.0000, 'INR', 'ACCOUNT_FROZEN', 'Intended account T1-SAV-0004 was temporarily frozen at time of deposit', 'RESOLVED', '2026-03-14', 16, 3, 6, 'Account unfrozen, funds moved to intended account', SYSUTCDATETIME(), SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT suspense_cases OFF;

-- ============================================================================
-- FRAUD ALERTS
-- ============================================================================
SET IDENTITY_INSERT fraud_alerts ON;
INSERT INTO fraud_alerts (id, tenant_id, account_id, account_number, alert_type, status, details, observed_count, observed_amount, threshold_value, user_id, created_at) VALUES
(1, 1, 8, 'T1-SAV-0001', 'VELOCITY_COUNT', 'OPEN', 'Account T1-SAV-0001 exceeded transaction count threshold: 6 transactions in 30 minutes (limit: 5)', 6, 225000.0000, '5 per 30min', 4, SYSUTCDATETIME()),
(2, 1, 8, 'T1-SAV-0001', 'HARD_CEILING',   'RESOLVED', 'Deposit of 50,000,000 blocked - exceeds hard ceiling of 10,000,000 per transaction', NULL, 50000000.0000, '10000000 per txn', 4, SYSUTCDATETIME());
SET IDENTITY_INSERT fraud_alerts OFF;

-- ============================================================================
-- AUDIT LOGS (sample entries for key transactions)
-- ============================================================================
SET IDENTITY_INSERT audit_logs ON;
INSERT INTO audit_logs (id, user_id, action, entity, entity_id, details, timestamp, tenant_id, hash, previous_hash) VALUES
(1,  4, 'DEPOSIT',                 'TRANSACTION', 1,  'DEP-20260314-000001: Deposit 50,000 to T1-SAV-0001',                        SYSUTCDATETIME(), 1, 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', NULL),
(2,  4, 'WITHDRAWAL',              'TRANSACTION', 2,  'WDR-20260314-000001: Withdrawal 20,000 from T1-SAV-0003',                    SYSUTCDATETIME(), 1, 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3', 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2'),
(3,  4, 'TRANSFER',                'TRANSACTION', 3,  'TRF-20260314-000001: Transfer 15,000 from T1-SAV-0001 to T1-SAV-0002',       SYSUTCDATETIME(), 1, 'c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4', 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3'),
(4,  4, 'TRANSFER',                'TRANSACTION', 4,  'TRF-20260314-000002: IBT 25,000 from T1-SAV-0001 (BR001) to T1-SAV-0006 (BR002)', SYSUTCDATETIME(), 1, 'd4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5', 'c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4'),
(5,  4, 'DEPOSIT',                 'TRANSACTION', 5,  'DEP-20260314-000002: Deposit 100,000 to T1-CUR-0001',                        SYSUTCDATETIME(), 1, 'e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6', 'd4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5'),
(6,  4, 'SUSPENSE_PARKED',         'TRANSACTION', 6,  'DEP-20260314-000003: 10,000 parked to suspense - intended account frozen',    SYSUTCDATETIME(), 1, 'f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1', 'e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6'),
(7,  3, 'SUSPENSE_RESOLVED',       'SUSPENSE_CASE', 1, 'Suspense case #1 resolved: 10,000 moved from suspense to T1-SAV-0004',     SYSUTCDATETIME(), 1, 'a1a2a3a4a5a6a1a2a3a4a5a6a1a2a3a4a5a6a1a2a3a4a5a6a1a2a3a4a5a6a1a2', 'f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1'),
(8,  4, 'HARD_CEILING_VIOLATION',  'TRANSACTION', 8,  'DEP-20260314-000004: 50,000,000 deposit blocked by hard ceiling',            SYSUTCDATETIME(), 1, 'b1b2b3b4b5b6b1b2b3b4b5b6b1b2b3b4b5b6b1b2b3b4b5b6b1b2b3b4b5b6b1b2', 'a1a2a3a4a5a6a1a2a3a4a5a6a1a2a3a4a5a6a1a2a3a4a5a6a1a2a3a4a5a6a1a2'),
(9,  4, 'VELOCITY_BREACH',         'TRANSACTION', 9,  'WDR-20260314-000002: Withdrawal blocked by velocity fraud check',            SYSUTCDATETIME(), 1, 'c1c2c3c4c5c6c1c2c3c4c5c6c1c2c3c4c5c6c1c2c3c4c5c6c1c2c3c4c5c6c1c2', 'b1b2b3b4b5b6b1b2b3b4b5b6b1b2b3b4b5b6b1b2b3b4b5b6b1b2b3b4b5b6b1b2'),
(10, 10,'DEPOSIT',                 'TRANSACTION', 10, 'DEP-20260314-T2-0001: Deposit 75,000 to T2-SAV-0001',                        SYSUTCDATETIME(), 2, 'd1d2d3d4d5d6d1d2d3d4d5d6d1d2d3d4d5d6d1d2d3d4d5d6d1d2d3d4d5d6d1d2', NULL),
(11, 10,'WITHDRAWAL',              'TRANSACTION', 11, 'WDR-20260314-T2-0001: Withdrawal 30,000 from T2-CUR-0001',                   SYSUTCDATETIME(), 2, 'e1e2e3e4e5e6e1e2e3e4e5e6e1e2e3e4e5e6e1e2e3e4e5e6e1e2e3e4e5e6e1e2', 'd1d2d3d4d5d6d1d2d3d4d5d6d1d2d3d4d5d6d1d2d3d4d5d6d1d2d3d4d5d6d1d2'),
(12, 10,'TRANSFER',                'TRANSACTION', 12, 'TRF-20260314-T2-0001: IBT 40,000 from T2-SAV-0001 (BR003) to T2-SAV-0006 (BR004)', SYSUTCDATETIME(), 2, 'f1f2f3f4f5f6f1f2f3f4f5f6f1f2f3f4f5f6f1f2f3f4f5f6f1f2f3f4f5f6f1f2', 'e1e2e3e4e5e6e1e2e3e4e5e6e1e2e3e4e5e6e1e2e3e4e5e6e1e2e3e4e5e6e1e2');
SET IDENTITY_INSERT audit_logs OFF;

-- ============================================================================
-- VALIDATION QUERIES (run after insert to verify invariants)
-- ============================================================================
-- Verify SUM(DR) = SUM(CR) for Tenant 1
-- Expected: Both should equal 295,000.0000
-- (50000 + 20000 + 15000 + 25000*2 + 100000 + 10000 + 10000 = 255000 debits for T1)
-- SELECT
--     SUM(CASE WHEN entry_type = 'DEBIT'  THEN amount ELSE 0 END) AS total_debits,
--     SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS total_credits
-- FROM ledger_entries WHERE tenant_id = 1;

-- Verify Suspense GL nets to zero (parked then resolved)
-- SELECT SUM(CASE WHEN entry_type='DEBIT' THEN amount ELSE 0 END)
--      - SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE 0 END) AS suspense_net
-- FROM ledger_entries WHERE gl_account_code = 'GL-T1-SUSPENSE';
-- Expected: 0.0000

-- Verify Clearing GL nets to zero per tenant (before settlement)
-- Note: Before settlement, clearing accounts show IBT in transit.
-- After EOD settlement, IBC_OUT CR and IBC_IN DR cancel out.

COMMIT TRANSACTION;
SET NOCOUNT OFF;

-- ============================================================================
-- END OF SEED DATA
-- ============================================================================
