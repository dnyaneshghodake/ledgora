-- ============================================================================
-- Ledgora CBS — Production Tier-1 Seed Data (Enhanced)
-- Target: SQL Server 2019+
-- Generated: 2026-03-14
-- Description: Comprehensive CBS seed data extending prod-seed-data.sql with
--              additional transaction scenarios: interest posting, charge
--              deduction, reversal, teller sessions, approval pending, and
--              EOD run. Covers all 14 mandatory transaction types.
-- ============================================================================
-- VALIDATION INVARIANTS:
--   SUM(DR) = SUM(CR) across all ledger entries per tenant
--   Clearing GL nets to zero after settlement
--   Suspense GL nets to zero before EOD
--   Batch totals match ledger totals
--   EOD inserted as COMPLETED
-- ============================================================================
-- PREREQUISITES: Run prod-seed-data.sql first to create base data.
--   This script adds incremental data on top of the base seed.
-- ============================================================================

SET NOCOUNT ON;
BEGIN TRANSACTION;

-- ============================================================================
-- ADDITIONAL ROLES (ROLE_RISK + ROLE_COMPLIANCE_OFFICER if missing)
-- IDs must match prod-seed-data.sql: ROLE_RISK=13, ROLE_COMPLIANCE_OFFICER=14
-- ============================================================================
IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_RISK')
BEGIN
    SET IDENTITY_INSERT roles ON;
    INSERT INTO roles (id, name, description) VALUES
    (13, 'ROLE_RISK', 'Risk Officer — view fraud alerts, velocity breaches. Cannot post vouchers.'),
    (14, 'ROLE_COMPLIANCE_OFFICER', 'Compliance Officer — AML/CFT oversight, STR filing per RBI.');
    SET IDENTITY_INSERT roles OFF;
END;

-- ============================================================================
-- ADDITIONAL USERS: risk_officer, compliance_officer per tenant
-- ⚠️  SECURITY WARNING: Same shared BCrypt hash as prod-seed-data.sql.
--     ALL passwords MUST be changed before production use.
-- ============================================================================
SET IDENTITY_INSERT users ON;
-- Tenant 1: Risk Officer and Compliance Officer
INSERT INTO users (id, tenant_id, tenant_scope, username, password, full_name, email, phone, branch_code, branch_id, is_active, is_locked, failed_login_attempts, created_at, updated_at)
VALUES
(14, 1, 'SINGLE', 'risk_officer_t1',      '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Anil Kumar Risk',       'anil.risk@ledgora.in',       '+91-9876543230', 'BR001', 1, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(15, 1, 'SINGLE', 'compliance_t1',        '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Meera Compliance',      'meera.compliance@ledgora.in', '+91-9876543231', 'BR001', 1, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Additional teller for Pune branch
(16, 1, 'SINGLE', 'teller_br2_t1',        '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Neha Kulkarni',         'neha@ledgora.in',            '+91-9876543232', 'BR002', 2, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Tenant 2: Risk Officer and Compliance Officer
(17, 2, 'SINGLE', 'risk_officer_t2',      '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Sanjay Risk',           'sanjay.risk@ledgora-coop.in','+91-9876543233', 'BR003', 3, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(18, 2, 'SINGLE', 'compliance_t2',        '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqdkNmGp5GjSHjkYqpMfZ3GqKG5lK', 'Priti Compliance',      'priti.compliance@ledgora-coop.in','+91-9876543234', 'BR003', 3, 1, 0, 0, SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT users OFF;

-- User-Role assignments for new users
-- ROLE_RISK=13, ROLE_COMPLIANCE_OFFICER=14 (matches prod-seed-data.sql)
INSERT INTO user_roles (user_id, role_id) VALUES
(14, 13),  -- risk_officer_t1 -> ROLE_RISK
(15, 14),  -- compliance_t1 -> ROLE_COMPLIANCE_OFFICER
(16, 3),   -- teller_br2_t1 -> ROLE_TELLER
(16, 4),   -- teller_br2_t1 -> ROLE_MAKER
(17, 13),  -- risk_officer_t2 -> ROLE_RISK
(18, 14);  -- compliance_t2 -> ROLE_COMPLIANCE_OFFICER

-- ============================================================================
-- ADDITIONAL GL ACCOUNTS (Loans, Operational Expense per tenant)
-- ============================================================================
SET IDENTITY_INSERT general_ledgers ON;
INSERT INTO general_ledgers (id, tenant_id, gl_code, gl_name, description, account_type, parent_id, level_num, is_active, balance, normal_balance, created_at, updated_at) VALUES
-- Tenant 1: Loans and Operational Expense
(31, 1, 'GL-T1-LOANS',     'Loans Portfolio',       'Loan asset GL',               'ASSET',   1,  1, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(32, 1, 'GL-T1-OP-EXP',    'Operational Expense',   'Branch operational expenses', 'EXPENSE', 5,  1, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(33, 1, 'GL-T1-CHARGES',    'Service Charges',       'CBS service charges revenue', 'REVENUE', 4,  1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Tenant 2: Loans and Operational Expense
(34, 2, 'GL-T2-LOANS',     'Loans Portfolio',       'Loan asset GL',               'ASSET',   16, 1, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(35, 2, 'GL-T2-OP-EXP',    'Operational Expense',   'Branch operational expenses', 'EXPENSE', 20, 1, 1, 0.0000, 'DEBIT',  SYSUTCDATETIME(), SYSUTCDATETIME()),
(36, 2, 'GL-T2-CHARGES',    'Service Charges',       'CBS service charges revenue', 'REVENUE', 19, 1, 1, 0.0000, 'CREDIT', SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT general_ledgers OFF;

-- ============================================================================
-- ADDITIONAL ACCOUNTS (10 more Savings + 5 more Current to reach 20/10 total)
-- ============================================================================
SET IDENTITY_INSERT accounts ON;
INSERT INTO accounts (id, tenant_id, account_number, account_name, account_type, status, balance, currency, branch_code, branch_id, customer_name, gl_account_code, freeze_level, npa_flag, approval_status, created_at, version, updated_at) VALUES
-- Tenant 1: 10 additional Savings (IDs 55-64) to reach 20 total
(55, 1, 'T1-SAV-0011', 'Vikas Pandey Savings',      'SAVINGS', 'ACTIVE',   78000.0000, 'INR', 'BR001', 1, 'Vikas Pandey',      'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(56, 1, 'T1-SAV-0012', 'Kavita Sharma Savings',     'SAVINGS', 'ACTIVE',  132000.0000, 'INR', 'BR001', 1, 'Kavita Sharma',     'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(57, 1, 'T1-SAV-0013', 'Aditya Bansal Savings',     'SAVINGS', 'ACTIVE',   54000.0000, 'INR', 'BR002', 2, 'Aditya Bansal',     'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(58, 1, 'T1-SAV-0014', 'Nandini Rao Savings',       'SAVINGS', 'ACTIVE',  195000.0000, 'INR', 'BR002', 2, 'Nandini Rao',       'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(59, 1, 'T1-SAV-0015', 'Deepak Verma Savings',      'SAVINGS', 'ACTIVE',   23000.0000, 'INR', 'BR001', 1, 'Deepak Verma',      'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(60, 1, 'T1-SAV-0016', 'Shalini Gupta Savings',     'SAVINGS', 'ACTIVE',  168000.0000, 'INR', 'BR002', 2, 'Shalini Gupta',     'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(61, 1, 'T1-SAV-0017', 'Mohan Tiwari Savings',      'SAVINGS', 'ACTIVE',   91000.0000, 'INR', 'BR001', 1, 'Mohan Tiwari',      'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(62, 1, 'T1-SAV-0018', 'Pooja Deshmukh Savings',    'SAVINGS', 'ACTIVE',  245000.0000, 'INR', 'BR002', 2, 'Pooja Deshmukh',    'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(63, 1, 'T1-SAV-0019', 'Rahul Saxena Savings',      'SAVINGS', 'ACTIVE',   37000.0000, 'INR', 'BR001', 1, 'Rahul Saxena',      'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(64, 1, 'T1-SAV-0020', 'Anita Jain Savings',        'SAVINGS', 'ACTIVE',  112000.0000, 'INR', 'BR002', 2, 'Anita Jain',        'GL-T1-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
-- Tenant 1: 5 additional Current (IDs 65-69) to reach 10 total
(65, 1, 'T1-CUR-0006', 'Pinnacle Tech Current',     'CURRENT', 'ACTIVE', 1500000.0000, 'INR', 'BR001', 1, 'Pinnacle Tech',     'GL-T1-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(66, 1, 'T1-CUR-0007', 'Zenith Exports Current',    'CURRENT', 'ACTIVE',  720000.0000, 'INR', 'BR002', 2, 'Zenith Exports',    'GL-T1-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(67, 1, 'T1-CUR-0008', 'Nova Pharma Current',       'CURRENT', 'ACTIVE', 1850000.0000, 'INR', 'BR001', 1, 'Nova Pharma',       'GL-T1-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(68, 1, 'T1-CUR-0009', 'Orbit Finance Current',     'CURRENT', 'ACTIVE',  430000.0000, 'INR', 'BR002', 2, 'Orbit Finance',     'GL-T1-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(69, 1, 'T1-CUR-0010', 'Metro Retail Current',      'CURRENT', 'ACTIVE',  980000.0000, 'INR', 'BR001', 1, 'Metro Retail',      'GL-T1-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
-- Tenant 2: 10 additional Savings (IDs 70-79) to reach 20 total
(70, 2, 'T2-SAV-0011', 'Ashok Kumar Savings',       'SAVINGS', 'ACTIVE',   85000.0000, 'INR', 'BR003', 3, 'Ashok Kumar',       'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(71, 2, 'T2-SAV-0012', 'Rekha Mishra Savings',      'SAVINGS', 'ACTIVE',  140000.0000, 'INR', 'BR003', 3, 'Rekha Mishra',      'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(72, 2, 'T2-SAV-0013', 'Sunil Das Savings',         'SAVINGS', 'ACTIVE',   62000.0000, 'INR', 'BR004', 4, 'Sunil Das',         'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(73, 2, 'T2-SAV-0014', 'Lalita Roy Savings',        'SAVINGS', 'ACTIVE',  210000.0000, 'INR', 'BR004', 4, 'Lalita Roy',        'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(74, 2, 'T2-SAV-0015', 'Manoj Saxena Savings',      'SAVINGS', 'ACTIVE',   48000.0000, 'INR', 'BR003', 3, 'Manoj Saxena',      'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(75, 2, 'T2-SAV-0016', 'Ritu Agarwal Savings',      'SAVINGS', 'ACTIVE',  178000.0000, 'INR', 'BR004', 4, 'Ritu Agarwal',      'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(76, 2, 'T2-SAV-0017', 'Vijay Chauhan Savings',     'SAVINGS', 'ACTIVE',   96000.0000, 'INR', 'BR003', 3, 'Vijay Chauhan',     'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(77, 2, 'T2-SAV-0018', 'Sunanda Pillai Savings',    'SAVINGS', 'ACTIVE',  225000.0000, 'INR', 'BR004', 4, 'Sunanda Pillai',    'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(78, 2, 'T2-SAV-0019', 'Ramesh Bose Savings',       'SAVINGS', 'ACTIVE',   33000.0000, 'INR', 'BR003', 3, 'Ramesh Bose',       'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(79, 2, 'T2-SAV-0020', 'Padma Lakshmi Savings',     'SAVINGS', 'ACTIVE',  190000.0000, 'INR', 'BR004', 4, 'Padma Lakshmi',     'GL-T2-SAVINGS', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
-- Tenant 2: 5 additional Current (IDs 80-84) to reach 10 total
(80, 2, 'T2-CUR-0006', 'Lambda Corp Current',       'CURRENT', 'ACTIVE', 1350000.0000, 'INR', 'BR003', 3, 'Lambda Corp',       'GL-T2-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(81, 2, 'T2-CUR-0007', 'Epsilon Group Current',     'CURRENT', 'ACTIVE',  680000.0000, 'INR', 'BR004', 4, 'Epsilon Group',     'GL-T2-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(82, 2, 'T2-CUR-0008', 'Kappa Traders Current',     'CURRENT', 'ACTIVE', 1720000.0000, 'INR', 'BR003', 3, 'Kappa Traders',     'GL-T2-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(83, 2, 'T2-CUR-0009', 'Iota Services Current',     'CURRENT', 'ACTIVE',  510000.0000, 'INR', 'BR004', 4, 'Iota Services',     'GL-T2-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME()),
(84, 2, 'T2-CUR-0010', 'Mu Electronics Current',    'CURRENT', 'ACTIVE',  890000.0000, 'INR', 'BR003', 3, 'Mu Electronics',    'GL-T2-CURRENT', 'NONE', 0, 'APPROVED', SYSUTCDATETIME(), 0, SYSUTCDATETIME());
SET IDENTITY_INSERT accounts OFF;

-- ============================================================================
-- ADDITIONAL TRANSACTIONS (extending from TXN 13 onward)
-- Scenarios: Interest Posting, Charge Deduction, Reversal, Approval Pending
-- ============================================================================
SET IDENTITY_INSERT transactions ON;

-- TXN 13: Interest posting batch — Credit interest 1,250 to T1-SAV-0001
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, checker_id, maker_timestamp, checker_timestamp, version, created_at, updated_at)
VALUES (13, 1, 2, 1, 'INT-20260314-000001', 'DEPOSIT', 'COMPLETED', 1250.0000, 'INR', 'BATCH', NULL, 8, 'Quarterly interest posting - Savings', '2026-03-14', '2026-03-14', 1, 1, 3, SYSUTCDATETIME(), SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- TXN 14: Charge deduction — Debit service charge 150 from T1-CUR-0001
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, checker_id, maker_timestamp, checker_timestamp, version, created_at, updated_at)
VALUES (14, 1, 2, 1, 'CHG-20260314-000001', 'WITHDRAWAL', 'COMPLETED', 150.0000, 'INR', 'BATCH', 18, NULL, 'Quarterly account maintenance charge', '2026-03-14', '2026-03-14', 1, 1, 3, SYSUTCDATETIME(), SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- TXN 15: Reversal of TXN 1 (Deposit 50,000 to T1-SAV-0001)
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, checker_id, maker_timestamp, checker_timestamp, version, created_at, updated_at)
VALUES (15, 1, 1, 1, 'REV-20260314-000001', 'REVERSAL', 'COMPLETED', 50000.0000, 'INR', 'TELLER', 8, NULL, 'Reversal of DEP-20260314-000001 — Incorrect account', '2026-03-14', '2026-03-14', 3, 3, 6, SYSUTCDATETIME(), SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- TXN 16: Approval pending transaction (maker submitted, checker has not acted)
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, maker_timestamp, version, created_at, updated_at)
VALUES (16, 1, 2, 2, 'TRF-20260314-000004', 'TRANSFER', 'PENDING_APPROVAL', 500000.0000, 'INR', 'ONLINE', 20, 22, 'High-value transfer requiring checker approval', '2026-03-14', '2026-03-14', 5, 5, SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- TXN 17: Tenant 2 — Interest posting batch
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, checker_id, maker_timestamp, checker_timestamp, version, created_at, updated_at)
VALUES (17, 2, 4, 3, 'INT-20260314-T2-0001', 'DEPOSIT', 'COMPLETED', 950.0000, 'INR', 'BATCH', NULL, 35, 'Quarterly interest posting - Savings', '2026-03-14', '2026-03-14', 1, 1, 9, SYSUTCDATETIME(), SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

-- TXN 18: Tenant 2 — Charge deduction
INSERT INTO transactions (id, tenant_id, batch_id, branch_id, transaction_ref, transaction_type, status, amount, currency, channel, source_account_id, destination_account_id, description, business_date, value_date, performed_by, maker_id, checker_id, maker_timestamp, checker_timestamp, version, created_at, updated_at)
VALUES (18, 2, 4, 3, 'CHG-20260314-T2-0001', 'WITHDRAWAL', 'COMPLETED', 100.0000, 'INR', 'BATCH', 45, NULL, 'Account maintenance charge', '2026-03-14', '2026-03-14', 1, 1, 9, SYSUTCDATETIME(), SYSUTCDATETIME(), 0, SYSUTCDATETIME(), SYSUTCDATETIME());

SET IDENTITY_INSERT transactions OFF;

-- ============================================================================
-- ADDITIONAL VOUCHERS for new transactions
-- ============================================================================
SET IDENTITY_INSERT vouchers ON;
INSERT INTO vouchers (id, voucher_number, tenant_id, branch_id, transaction_id, batch_code, set_no, scroll_no, entry_date, posting_date, value_date, dr_cr, account_id, gl_account_id, transaction_amount, local_currency_amount, currency, maker_id, checker_id, auth_flag, post_flag, cancel_flag, financial_effect_flag, total_debit, total_credit, narration, version, created_at, updated_at) VALUES
-- TXN 13: Interest posting 1,250 → DR Interest Expense GL, CR Customer SAV-0001
(25, 'TENANT001-BR001-20260314-000015', 1, 1, 13, 'BATCH-T1-ONL-20260314', 1, 21, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 1, 14,  1250.0000, 1250.0000, 'INR', 1, 3, 'Y', 'Y', 'N', 'Y', 1250.0000, 0.0000, 'Interest posting - expense leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(26, 'TENANT001-BR001-20260314-000016', 1, 1, 13, 'BATCH-T1-ONL-20260314', 1, 22, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 8, 7,   1250.0000, 1250.0000, 'INR', 1, 3, 'Y', 'Y', 'N', 'Y', 0.0000, 1250.0000, 'Interest posting - customer credit', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- TXN 14: Charge deduction 150 → DR Customer CUR-0001, CR Fee Income GL
(27, 'TENANT001-BR001-20260314-000017', 1, 1, 14, 'BATCH-T1-ONL-20260314', 1, 23, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 18, 8,  150.0000, 150.0000, 'INR', 1, 3, 'Y', 'Y', 'N', 'Y', 150.0000, 0.0000, 'Service charge - customer debit', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(28, 'TENANT001-BR001-20260314-000018', 1, 1, 14, 'BATCH-T1-ONL-20260314', 1, 24, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 1, 33,  150.0000, 150.0000, 'INR', 1, 3, 'Y', 'Y', 'N', 'Y', 0.0000, 150.0000, 'Service charge - fee income', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- TXN 15: Reversal of TXN 1 → CR Cash (reverse the original DR), DR Customer (reverse the original CR)
(29, 'TENANT001-BR001-20260314-000019', 1, 1, 15, 'BATCH-T1-TEL-20260314', 1, 25, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 1, 6,   50000.0000, 50000.0000, 'INR', 3, 6, 'Y', 'Y', 'N', 'Y', 0.0000, 50000.0000, 'Reversal - cash leg (reverse DR)', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(30, 'TENANT001-BR001-20260314-000020', 1, 1, 15, 'BATCH-T1-TEL-20260314', 1, 26, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 8, 7,   50000.0000, 50000.0000, 'INR', 3, 6, 'Y', 'Y', 'N', 'Y', 50000.0000, 0.0000, 'Reversal - customer leg (reverse CR)', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- TXN 17: T2 Interest posting 950 → DR Interest Expense GL, CR Customer SAV-0001
(31, 'TENANT002-BR003-20260314-000007', 2, 3, 17, 'BATCH-T2-ONL-20260314', 1, 9,  '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 28, 29, 950.0000, 950.0000, 'INR', 1, 9, 'Y', 'Y', 'N', 'Y', 950.0000, 0.0000, 'Interest posting - expense leg', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(32, 'TENANT002-BR003-20260314-000008', 2, 3, 17, 'BATCH-T2-ONL-20260314', 1, 10, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 35, 22, 950.0000, 950.0000, 'INR', 1, 9, 'Y', 'Y', 'N', 'Y', 0.0000, 950.0000, 'Interest posting - customer credit', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- TXN 18: T2 Charge deduction 100 → DR Customer CUR-0001, CR Fee Income GL
(33, 'TENANT002-BR003-20260314-000009', 2, 3, 18, 'BATCH-T2-ONL-20260314', 1, 11, '2026-03-14', '2026-03-14', '2026-03-14', 'DR', 45, 23, 100.0000, 100.0000, 'INR', 1, 9, 'Y', 'Y', 'N', 'Y', 100.0000, 0.0000, 'Account charge - customer debit', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
(34, 'TENANT002-BR003-20260314-000010', 2, 3, 18, 'BATCH-T2-ONL-20260314', 1, 12, '2026-03-14', '2026-03-14', '2026-03-14', 'CR', 28, 36, 100.0000, 100.0000, 'INR', 1, 9, 'Y', 'Y', 'N', 'Y', 0.0000, 100.0000, 'Account charge - fee income', 0, SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT vouchers OFF;

-- ============================================================================
-- ADDITIONAL LEDGER JOURNALS
-- ============================================================================
SET IDENTITY_INSERT ledger_journals ON;
INSERT INTO ledger_journals (id, tenant_id, transaction_id, description, business_date, created_at) VALUES
(11, 1, 13, 'Interest posting 1,250 to T1-SAV-0001',                    '2026-03-14', SYSUTCDATETIME()),
(12, 1, 14, 'Service charge 150 from T1-CUR-0001',                      '2026-03-14', SYSUTCDATETIME()),
(13, 1, 15, 'Reversal of DEP-20260314-000001 (50,000)',                  '2026-03-14', SYSUTCDATETIME()),
(14, 2, 17, 'Interest posting 950 to T2-SAV-0001',                      '2026-03-14', SYSUTCDATETIME()),
(15, 2, 18, 'Service charge 100 from T2-CUR-0001',                      '2026-03-14', SYSUTCDATETIME());
SET IDENTITY_INSERT ledger_journals OFF;

-- ============================================================================
-- ADDITIONAL LEDGER ENTRIES (balanced per journal)
-- ============================================================================
SET IDENTITY_INSERT ledger_entries ON;
INSERT INTO ledger_entries (id, tenant_id, journal_id, transaction_id, account_id, gl_account_id, gl_account_code, entry_type, amount, balance_after, currency, business_date, posting_time, narration, voucher_id, created_at) VALUES
-- Journal 11: Interest posting 1,250 (DR cash/expense proxy, CR customer) → balanced
-- Account 1 (Cash BR001): base=5,140,000 + DR 1,250 = 5,141,250
-- Account 8 (SAV-0001): base=135,000 + CR 1,250 = 136,250
(25, 1, 11, 13, 1,  14, 'GL-T1-INT-EXP', 'DEBIT',  1250.0000, 5141250.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Interest posting - expense leg',     25, SYSUTCDATETIME()),
(26, 1, 11, 13, 8,  7,  'GL-T1-SAVINGS', 'CREDIT', 1250.0000, 136250.0000,  'INR', '2026-03-14', SYSUTCDATETIME(), 'Interest posting - customer credit',  26, SYSUTCDATETIME()),
-- Journal 12: Charge deduction 150 (DR customer, CR cash/fee proxy) → balanced
-- Account 18 (CUR-0001): base=950,000 - DR 150 = 949,850
-- Account 1 (Cash BR001): 5,141,250 - CR 150 = 5,141,100
(27, 1, 12, 14, 18, 8,  'GL-T1-CURRENT', 'DEBIT',  150.0000, 949850.0000,   'INR', '2026-03-14', SYSUTCDATETIME(), 'Service charge - customer debit',    27, SYSUTCDATETIME()),
(28, 1, 12, 14, 1,  33, 'GL-T1-CHARGES', 'CREDIT', 150.0000, 5141100.0000,  'INR', '2026-03-14', SYSUTCDATETIME(), 'Service charge - fee income',        28, SYSUTCDATETIME()),
-- Journal 13: Reversal 50,000 (CR cash, DR customer) → balanced; reverses original journal 1
-- Account 1 (Cash BR001): 5,141,100 - CR 50,000 = 5,091,100
-- Account 8 (SAV-0001): 136,250 - DR 50,000 = 86,250
(29, 1, 13, 15, 1,  6,  'GL-T1-CASH',    'CREDIT', 50000.0000, 5091100.0000, 'INR', '2026-03-14', SYSUTCDATETIME(), 'Reversal - cash leg',               29, SYSUTCDATETIME()),
(30, 1, 13, 15, 8,  7,  'GL-T1-SAVINGS', 'DEBIT',  50000.0000,  86250.0000,  'INR', '2026-03-14', SYSUTCDATETIME(), 'Reversal - customer leg',           30, SYSUTCDATETIME()),
-- Journal 14: T2 Interest posting 950 (DR cash/expense proxy, CR customer) → balanced
-- Account 28 (Cash BR003): base=4,045,000 + DR 950 = 4,045,950
-- Account 35 (SAV-0001): base=145,000 + CR 950 = 145,950
(31, 2, 14, 17, 28, 29, 'GL-T2-INT-EXP', 'DEBIT',  950.0000, 4045950.0000,  'INR', '2026-03-14', SYSUTCDATETIME(), 'Interest posting - expense leg',    31, SYSUTCDATETIME()),
(32, 2, 14, 17, 35, 22, 'GL-T2-SAVINGS', 'CREDIT', 950.0000, 145950.0000,   'INR', '2026-03-14', SYSUTCDATETIME(), 'Interest posting - customer credit', 32, SYSUTCDATETIME()),
-- Journal 15: T2 Charge deduction 100 (DR customer, CR cash/fee proxy) → balanced
-- Account 45 (CUR-0001): base=750,000 - DR 100 = 749,900
-- Account 28 (Cash BR003): 4,045,950 - CR 100 = 4,045,850
(33, 2, 15, 18, 45, 23, 'GL-T2-CURRENT', 'DEBIT',  100.0000, 749900.0000,   'INR', '2026-03-14', SYSUTCDATETIME(), 'Account charge - customer debit',   33, SYSUTCDATETIME()),
(34, 2, 15, 18, 28, 36, 'GL-T2-CHARGES', 'CREDIT', 100.0000, 4045850.0000,  'INR', '2026-03-14', SYSUTCDATETIME(), 'Account charge - fee income',       34, SYSUTCDATETIME());
SET IDENTITY_INSERT ledger_entries OFF;

-- ============================================================================
-- APPROVAL REQUEST (for TXN 16 - pending approval)
-- ============================================================================
SET IDENTITY_INSERT approval_requests ON;
INSERT INTO approval_requests (id, tenant_id, entity_type, entity_id, request_data, requested_by, status, remarks, version, created_at) VALUES
(1, 1, 'TRANSACTION', 16, '{"ref":"TRF-20260314-000004","amount":500000,"from":"T1-CUR-0003","to":"T1-CUR-0005"}', 5, 'PENDING', 'High-value transfer exceeds auto-approval threshold', 0, SYSUTCDATETIME());
SET IDENTITY_INSERT approval_requests OFF;

-- ============================================================================
-- TELLER MASTERS (register tellers for session management)
-- ============================================================================
SET IDENTITY_INSERT teller_masters ON;
INSERT INTO teller_masters (id, tenant_id, branch_id, user_id, status, single_txn_limit_deposit, single_txn_limit_withdrawal, daily_txn_limit, cash_holding_limit, active_flag, created_at, updated_at, version) VALUES
(1, 1, 1, 4,  'ASSIGNED', 200000.0000, 50000.0000, 500000.0000, 1000000.0000, 1, SYSUTCDATETIME(), SYSUTCDATETIME(), 0),
(2, 1, 2, 16, 'ASSIGNED', 200000.0000, 50000.0000, 500000.0000, 1000000.0000, 1, SYSUTCDATETIME(), SYSUTCDATETIME(), 0),
(3, 2, 3, 10, 'ASSIGNED', 200000.0000, 50000.0000, 500000.0000, 1000000.0000, 1, SYSUTCDATETIME(), SYSUTCDATETIME(), 0);
SET IDENTITY_INSERT teller_masters OFF;

-- ============================================================================
-- TELLER SESSIONS (open + close lifecycle)
-- ============================================================================
SET IDENTITY_INSERT teller_sessions ON;
INSERT INTO teller_sessions (id, tenant_id, teller_id, branch_id, business_date, opening_balance, current_balance, total_credit_today, total_debit_today, state, opened_by, authorized_by, closed_by, opened_at, closed_at, version, created_at, updated_at) VALUES
-- Teller 1 (BR001): Opened, transacted, closed
(1, 1, 1, 1, '2026-03-14', 500000.0000, 541100.0000, 161250.0000, 120150.0000, 'CLOSED', 4, 3, 4, '2026-03-14T08:00:00', '2026-03-14T17:00:00', 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Teller 2 (BR002): Opened, still active
(2, 1, 2, 2, '2026-03-14', 300000.0000, 300000.0000, 0.0000, 0.0000, 'OPEN', 16, 3, NULL, '2026-03-14T08:30:00', NULL, 0, SYSUTCDATETIME(), SYSUTCDATETIME()),
-- Teller 3 (T2, BR003): Opened, transacted, closed
(3, 2, 3, 3, '2026-03-14', 400000.0000, 444900.0000, 75950.0000, 31050.0000, 'CLOSED', 10, 9, 10, '2026-03-14T08:00:00', '2026-03-14T17:00:00', 0, SYSUTCDATETIME(), SYSUTCDATETIME());
SET IDENTITY_INSERT teller_sessions OFF;

-- ============================================================================
-- TELLER SESSION DENOMINATIONS (opening + closing)
-- ============================================================================
SET IDENTITY_INSERT teller_session_denominations ON;
INSERT INTO teller_session_denominations (id, session_id, event_type, denomination_value, count, total_amount, created_at) VALUES
-- Session 1 OPENING: 500,000 = 150x2000 + 50x1000 + 80x500 + 100x200 + 500x100 + 200x50
(1,  1, 'OPENING', 2000.0000, 150, 300000.0000, SYSUTCDATETIME()),
(2,  1, 'OPENING', 1000.0000, 50,   50000.0000, SYSUTCDATETIME()),
(3,  1, 'OPENING', 500.0000,  80,   40000.0000, SYSUTCDATETIME()),
(4,  1, 'OPENING', 200.0000,  100,  20000.0000, SYSUTCDATETIME()),
(5,  1, 'OPENING', 100.0000,  500,  50000.0000, SYSUTCDATETIME()),
(6,  1, 'OPENING', 50.0000,   800,  40000.0000, SYSUTCDATETIME()),
-- Session 1 CLOSING: 541,100 = 170x2000 + 60x1000 + 82x500 + 130x200 + 502x100 + 222x50
(7,  1, 'CLOSING', 2000.0000, 170, 340000.0000, SYSUTCDATETIME()),
(8,  1, 'CLOSING', 1000.0000, 60,   60000.0000, SYSUTCDATETIME()),
(9,  1, 'CLOSING', 500.0000,  82,   41000.0000, SYSUTCDATETIME()),
(10, 1, 'CLOSING', 200.0000,  130,  26000.0000, SYSUTCDATETIME()),
(11, 1, 'CLOSING', 100.0000,  502,  50200.0000, SYSUTCDATETIME()),
(12, 1, 'CLOSING', 50.0000,   478,  23900.0000, SYSUTCDATETIME()),
-- Session 2 OPENING (still open, no closing)
(13, 2, 'OPENING', 2000.0000, 75,  150000.0000, SYSUTCDATETIME()),
(14, 2, 'OPENING', 1000.0000, 40,   40000.0000, SYSUTCDATETIME()),
(15, 2, 'OPENING', 500.0000,  80,   40000.0000, SYSUTCDATETIME()),
(16, 2, 'OPENING', 200.0000,  150,  30000.0000, SYSUTCDATETIME()),
(17, 2, 'OPENING', 100.0000,  400,  40000.0000, SYSUTCDATETIME()),
-- Session 3 OPENING + CLOSING (T2)
(18, 3, 'OPENING', 2000.0000, 100, 200000.0000, SYSUTCDATETIME()),
(19, 3, 'OPENING', 1000.0000, 50,   50000.0000, SYSUTCDATETIME()),
(20, 3, 'OPENING', 500.0000,  100,  50000.0000, SYSUTCDATETIME()),
(21, 3, 'OPENING', 200.0000,  200,  40000.0000, SYSUTCDATETIME()),
(22, 3, 'OPENING', 100.0000,  500,  50000.0000, SYSUTCDATETIME()),
(23, 3, 'OPENING', 50.0000,   200,  10000.0000, SYSUTCDATETIME()),
-- Session 3 CLOSING: 444,900 = 110x2000 + 55x1000 + 110x500 + 220x200 + 550x100 + 318x50
(24, 3, 'CLOSING', 2000.0000, 110, 220000.0000, SYSUTCDATETIME()),
(25, 3, 'CLOSING', 1000.0000, 55,   55000.0000, SYSUTCDATETIME()),
(26, 3, 'CLOSING', 500.0000,  110,  55000.0000, SYSUTCDATETIME()),
(27, 3, 'CLOSING', 200.0000,  220,  44000.0000, SYSUTCDATETIME()),
(28, 3, 'CLOSING', 100.0000,  550,  55000.0000, SYSUTCDATETIME()),
(29, 3, 'CLOSING', 50.0000,   318,  15900.0000, SYSUTCDATETIME());
SET IDENTITY_INSERT teller_session_denominations OFF;

-- ============================================================================
-- ADDITIONAL AUDIT LOGS
-- ============================================================================
SET IDENTITY_INSERT audit_logs ON;
INSERT INTO audit_logs (id, user_id, action, entity, entity_id, details, timestamp, tenant_id, hash, previous_hash) VALUES
(13, 1,  'INTEREST_POSTING',  'TRANSACTION', 13, 'INT-20260314-000001: Interest 1,250 posted to T1-SAV-0001',    SYSUTCDATETIME(), 1, 'a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3', 'c1c2c3c4c5c6c1c2c3c4c5c6c1c2c3c4c5c6c1c2c3c4c5c6c1c2c3c4c5c6c1c2'),
(14, 1,  'CHARGE_DEDUCTION',  'TRANSACTION', 14, 'CHG-20260314-000001: Service charge 150 from T1-CUR-0001',      SYSUTCDATETIME(), 1, 'b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4', 'a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3'),
(15, 3,  'REVERSAL',          'TRANSACTION', 15, 'REV-20260314-000001: Reversal of 50,000 deposit to T1-SAV-0001', SYSUTCDATETIME(), 1, 'c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5', 'b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4'),
(16, 5,  'APPROVAL_REQUESTED','TRANSACTION', 16, 'TRF-20260314-000004: High-value transfer pending checker approval', SYSUTCDATETIME(), 1, 'd5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6', 'c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5'),
(17, 4,  'TELLER_SESSION_OPEN',  'TELLER_SESSION', 1, 'Teller session opened at BR001 with 500,000 opening balance', SYSUTCDATETIME(), 1, 'e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7', 'd5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6'),
(18, 4,  'TELLER_SESSION_CLOSE', 'TELLER_SESSION', 1, 'Teller session closed at BR001 with 541,100 closing balance', SYSUTCDATETIME(), 1, 'f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2', 'e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7a2b3c4d5e6f7'),
(19, 1,  'INTEREST_POSTING',  'TRANSACTION', 17, 'INT-20260314-T2-0001: Interest 950 posted to T2-SAV-0001',      SYSUTCDATETIME(), 2, 'a3b4c5d6e7f8a3b4c5d6e7f8a3b4c5d6e7f8a3b4c5d6e7f8a3b4c5d6e7f8a3b4', 'f1f2f3f4f5f6f1f2f3f4f5f6f1f2f3f4f5f6f1f2f3f4f5f6f1f2f3f4f5f6f1f2'),
(20, 1,  'CHARGE_DEDUCTION',  'TRANSACTION', 18, 'CHG-20260314-T2-0001: Service charge 100 from T2-CUR-0001',     SYSUTCDATETIME(), 2, 'b4c5d6e7f8a3b4c5d6e7f8a3b4c5d6e7f8a3b4c5d6e7f8a3b4c5d6e7f8a3b4c5', 'a3b4c5d6e7f8a3b4c5d6e7f8a3b4c5d6e7f8a3b4c5d6e7f8a3b4c5d6e7f8a3b4');
SET IDENTITY_INSERT audit_logs OFF;

-- ============================================================================
-- ADDITIONAL IDEMPOTENCY KEYS
-- ============================================================================
SET IDENTITY_INSERT idempotency_keys ON;
INSERT INTO idempotency_keys (id, tenant_id, idempotency_key, request_hash, status, created_at) VALUES
(1, 1, 'IDEM-DEP-20260314-000001', 'sha256:abc123def456', 'COMPLETED', SYSUTCDATETIME()),
(2, 1, 'IDEM-WDR-20260314-000001', 'sha256:def456ghi789', 'COMPLETED', SYSUTCDATETIME()),
(3, 1, 'IDEM-TRF-20260314-000001', 'sha256:ghi789jkl012', 'COMPLETED', SYSUTCDATETIME()),
(4, 1, 'IDEM-TRF-20260314-000002', 'sha256:jkl012mno345', 'COMPLETED', SYSUTCDATETIME()),
(5, 1, 'IDEM-INT-20260314-000001', 'sha256:mno345pqr678', 'COMPLETED', SYSUTCDATETIME()),
(6, 1, 'IDEM-CHG-20260314-000001', 'sha256:pqr678stu901', 'COMPLETED', SYSUTCDATETIME()),
(7, 1, 'IDEM-REV-20260314-000001', 'sha256:stu901vwx234', 'COMPLETED', SYSUTCDATETIME()),
(8, 1, 'IDEM-TRF-20260314-000004', 'sha256:vwx234yza567', 'PROCESSING', SYSUTCDATETIME()),
(9, 2, 'IDEM-DEP-20260314-T2-0001','sha256:yza567bcd890', 'COMPLETED', SYSUTCDATETIME()),
(10,2, 'IDEM-WDR-20260314-T2-0001','sha256:bcd890efg123', 'COMPLETED', SYSUTCDATETIME()),
(11,2, 'IDEM-TRF-20260314-T2-0001','sha256:efg123hij456', 'COMPLETED', SYSUTCDATETIME()),
(12,2, 'IDEM-INT-20260314-T2-0001','sha256:hij456klm789', 'COMPLETED', SYSUTCDATETIME()),
(13,2, 'IDEM-CHG-20260314-T2-0001','sha256:klm789nop012', 'COMPLETED', SYSUTCDATETIME());
SET IDENTITY_INSERT idempotency_keys OFF;

-- ============================================================================
-- SETTLEMENT (IBT settlement — clearing GLs net to zero)
-- ============================================================================
SET IDENTITY_INSERT settlements ON;
INSERT INTO settlements (id, settlement_ref, business_date, status, transaction_count, remarks, processed_by, start_time, end_time, created_at) VALUES
(1, 'STL-20260314-001', '2026-03-14', 'COMPLETED', 2, 'IBT settlement - clearing accounts reconciled', 1, '2026-03-14T16:00:00', '2026-03-14T16:05:00', SYSUTCDATETIME());
SET IDENTITY_INSERT settlements OFF;

-- ============================================================================
-- EOD PROCESS (Completed for the business date)
-- ============================================================================
SET IDENTITY_INSERT eod_processes ON;
INSERT INTO eod_processes (id, tenant_id, business_date, phase, status, started_at, completed_at, last_updated, version) VALUES
(1, 1, '2026-03-14', 'DATE_ADVANCED', 'COMPLETED', '2026-03-14T17:30:00', '2026-03-14T17:35:00', '2026-03-14T17:35:00', 0),
(2, 2, '2026-03-14', 'DATE_ADVANCED', 'COMPLETED', '2026-03-14T17:30:00', '2026-03-14T17:35:00', '2026-03-14T17:35:00', 0);
SET IDENTITY_INSERT eod_processes OFF;

-- ============================================================================
-- ADDITIONAL FRAUD ALERTS (extending from base seed)
-- ============================================================================
SET IDENTITY_INSERT fraud_alerts ON;
INSERT INTO fraud_alerts (id, tenant_id, account_id, account_number, alert_type, status, details, observed_count, observed_amount, threshold_value, user_id, created_at) VALUES
(3, 1, 65, 'T1-CUR-0006', 'VELOCITY_AMOUNT', 'ACKNOWLEDGED', 'Account T1-CUR-0006 exceeded cumulative amount threshold: 1,500,000 in single day (limit: 1,000,000)', NULL, 1500000.0000, '1000000 per day', 14, SYSUTCDATETIME()),
(4, 2, 47, 'T2-CUR-0003', 'VELOCITY_COUNT',   'OPEN',         'Account T2-CUR-0003 exceeded transaction count threshold: 8 transactions in 30 minutes (limit: 5)', 8, 880000.0000, '5 per 30min', 17, SYSUTCDATETIME());
SET IDENTITY_INSERT fraud_alerts OFF;

-- ============================================================================
-- VALIDATION QUERIES (run after insert to verify invariants)
-- ============================================================================

-- Verify SUM(DR) = SUM(CR) for Tenant 1 (including new transactions)
-- T1 Debits:  50000+20000+15000+25000+25000+100000+10000+10000 (base=255000) + 1250+150+50000 (tier1=51400) = 306400
-- T1 Credits: 50000+20000+15000+25000+25000+100000+10000+10000 (base=255000) + 1250+150+50000 (tier1=51400) = 306400
-- SELECT
--     SUM(CASE WHEN entry_type = 'DEBIT'  THEN amount ELSE 0 END) AS total_debits,
--     SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS total_credits
-- FROM ledger_entries WHERE tenant_id = 1;

-- Verify Suspense GL nets to zero (parked then resolved)
-- SELECT SUM(CASE WHEN entry_type='DEBIT' THEN amount ELSE 0 END)
--      - SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE 0 END) AS suspense_net
-- FROM ledger_entries WHERE gl_account_code = 'GL-T1-SUSPENSE';
-- Expected: 0.0000

-- Verify Clearing GL nets to zero after settlement
-- SELECT SUM(CASE WHEN entry_type='DEBIT' THEN amount ELSE 0 END)
--      - SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE 0 END) AS clearing_net
-- FROM ledger_entries WHERE gl_account_code IN ('GL-T1-CLR-OUT','GL-T1-CLR-IN');
-- Expected: 0.0000

-- Verify EOD completed
-- SELECT * FROM eod_processes WHERE status = 'COMPLETED';

COMMIT TRANSACTION;
SET NOCOUNT OFF;

-- ============================================================================
-- END OF TIER-1 SEED DATA
-- ============================================================================
