-- ============================================================================
-- Ledgora CBS Tier-1: Database-Level Double-Entry & Integrity Constraints
-- Target: SQL Server / Oracle (ANSI SQL where possible)
-- ============================================================================
-- This migration adds production-grade constraints that enforce CBS accounting
-- invariants at the database level, independent of application code.
--
-- Constraints are additive — they do not modify existing columns or data.
-- Run AFTER initial schema creation (Hibernate ddl-auto=update creates tables).
-- ============================================================================

-- ────────────────────────────────────────────────────────────────────────────
-- 1. LEDGER ENTRIES: Immutability + positive amount + valid entry type
-- ────────────────────────────────────────────────────────────────────────────

-- 1a. Amount must be strictly positive (no zero or negative ledger entries)
ALTER TABLE ledger_entries ADD CONSTRAINT chk_ledger_entry_amount_positive
    CHECK (amount > 0);

-- 1b. Entry type must be DEBIT or CREDIT (defense against corrupt data)
ALTER TABLE ledger_entries ADD CONSTRAINT chk_ledger_entry_type_valid
    CHECK (entry_type IN ('DEBIT', 'CREDIT'));

-- 1c. Tenant must be set (no orphaned entries)
ALTER TABLE ledger_entries ADD CONSTRAINT chk_ledger_entry_tenant_not_null
    CHECK (tenant_id IS NOT NULL);

-- 1d. Prevent UPDATE on ledger_entries (SQL Server trigger / Oracle trigger)
-- NOTE: Hibernate @Immutable prevents UPDATEs at ORM level. This trigger is
-- defense-in-depth at DB level for direct SQL access or ETL tools.

-- SQL Server version:
-- CREATE TRIGGER trg_ledger_entries_no_update ON ledger_entries
-- INSTEAD OF UPDATE AS
-- BEGIN
--     RAISERROR('CBS VIOLATION: ledger_entries rows are immutable. Use reversal entries.', 16, 1);
--     ROLLBACK TRANSACTION;
-- END;

-- Oracle version:
-- CREATE OR REPLACE TRIGGER trg_ledger_entries_no_update
-- BEFORE UPDATE ON ledger_entries
-- FOR EACH ROW
-- BEGIN
--     RAISE_APPLICATION_ERROR(-20001, 'CBS VIOLATION: ledger_entries rows are immutable. Use reversal entries.');
-- END;

-- 1e. Prevent DELETE on ledger_entries
-- SQL Server version:
-- CREATE TRIGGER trg_ledger_entries_no_delete ON ledger_entries
-- INSTEAD OF DELETE AS
-- BEGIN
--     RAISERROR('CBS VIOLATION: ledger_entries rows cannot be deleted. Use reversal entries.', 16, 1);
--     ROLLBACK TRANSACTION;
-- END;

-- Oracle version:
-- CREATE OR REPLACE TRIGGER trg_ledger_entries_no_delete
-- BEFORE DELETE ON ledger_entries
-- FOR EACH ROW
-- BEGIN
--     RAISE_APPLICATION_ERROR(-20002, 'CBS VIOLATION: ledger_entries rows cannot be deleted. Use reversal entries.');
-- END;


-- ────────────────────────────────────────────────────────────────────────────
-- 2. VOUCHERS: Amount positive + DR/CR valid + flag values
-- ────────────────────────────────────────────────────────────────────────────

-- 2a. Transaction amount must be strictly positive
ALTER TABLE vouchers ADD CONSTRAINT chk_voucher_amount_positive
    CHECK (transaction_amount > 0);

-- 2b. DR/CR must be valid
ALTER TABLE vouchers ADD CONSTRAINT chk_voucher_drcr_valid
    CHECK (dr_cr IN ('DR', 'CR'));

-- 2c. Flag values must be Y or N
ALTER TABLE vouchers ADD CONSTRAINT chk_voucher_auth_flag
    CHECK (auth_flag IN ('Y', 'N'));

ALTER TABLE vouchers ADD CONSTRAINT chk_voucher_post_flag
    CHECK (post_flag IN ('Y', 'N'));

ALTER TABLE vouchers ADD CONSTRAINT chk_voucher_cancel_flag
    CHECK (cancel_flag IN ('Y', 'N'));

-- 2d. Posted voucher must be authorized first (post_flag=Y implies auth_flag=Y)
ALTER TABLE vouchers ADD CONSTRAINT chk_voucher_post_requires_auth
    CHECK (post_flag = 'N' OR auth_flag = 'Y');

-- 2e. Maker must be set
ALTER TABLE vouchers ADD CONSTRAINT chk_voucher_maker_not_null
    CHECK (maker_id IS NOT NULL);

-- 2f. Authorized voucher must have a checker
ALTER TABLE vouchers ADD CONSTRAINT chk_voucher_auth_has_checker
    CHECK (auth_flag = 'N' OR checker_id IS NOT NULL);

-- 2g. Maker and checker must differ (maker-checker enforcement at DB level)
ALTER TABLE vouchers ADD CONSTRAINT chk_voucher_maker_ne_checker
    CHECK (checker_id IS NULL OR maker_id <> checker_id);


-- ────────────────────────────────────────────────────────────────────────────
-- 3. TRANSACTION BATCHES: Balanced batches at close time
-- ────────────────────────────────────────────────────────────────────────────

-- 3a. Settled batch must be balanced (total_debit == total_credit)
-- NOTE: This constraint allows OPEN batches to be temporarily unbalanced
-- (vouchers are added incrementally). Only enforced when status = SETTLED.
ALTER TABLE transaction_batches ADD CONSTRAINT chk_batch_settled_balanced
    CHECK (status <> 'SETTLED' OR total_debit = total_credit);

-- 3b. Totals must be non-negative
ALTER TABLE transaction_batches ADD CONSTRAINT chk_batch_debit_nonneg
    CHECK (total_debit >= 0);

ALTER TABLE transaction_batches ADD CONSTRAINT chk_batch_credit_nonneg
    CHECK (total_credit >= 0);


-- ────────────────────────────────────────────────────────────────────────────
-- 4. ACCOUNTS: Balance cache non-null + valid status
-- ────────────────────────────────────────────────────────────────────────────

-- 4a. Account status must be a known value
ALTER TABLE accounts ADD CONSTRAINT chk_account_status_valid
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DORMANT', 'CLOSED', 'FROZEN',
                      'UNDER_REVIEW', 'PENDING_CLOSURE'));

-- 4b. Freeze level must be a known value
ALTER TABLE accounts ADD CONSTRAINT chk_account_freeze_valid
    CHECK (freeze_level IN ('NONE', 'DEBIT_ONLY', 'CREDIT_ONLY', 'FULL'));

-- 4c. Currency must be 3-char ISO code
ALTER TABLE accounts ADD CONSTRAINT chk_account_currency_len
    CHECK (LEN(currency) = 3);


-- ────────────────────────────────────────────────────────────────────────────
-- 5. TENANTS: Day status lifecycle
-- ────────────────────────────────────────────────────────────────────────────

-- 5a. Day status must be a known value
ALTER TABLE tenants ADD CONSTRAINT chk_tenant_day_status_valid
    CHECK (day_status IN ('OPEN', 'DAY_CLOSING', 'CLOSED'));


-- ────────────────────────────────────────────────────────────────────────────
-- 6. LOAN SCHEDULES: Non-negative amounts + valid status
-- ────────────────────────────────────────────────────────────────────────────

ALTER TABLE loan_schedules ADD CONSTRAINT chk_loan_principal_nonneg
    CHECK (principal_component >= 0);

ALTER TABLE loan_schedules ADD CONSTRAINT chk_loan_interest_nonneg
    CHECK (interest_component >= 0);

ALTER TABLE loan_schedules ADD CONSTRAINT chk_loan_emi_nonneg
    CHECK (emi_amount >= 0);

ALTER TABLE loan_schedules ADD CONSTRAINT chk_loan_dpd_nonneg
    CHECK (dpd_days >= 0);


-- ────────────────────────────────────────────────────────────────────────────
-- 7. AUDIT LOGS: Tenant-specific hash chain integrity index
-- ────────────────────────────────────────────────────────────────────────────

-- Ensures hash chain queries are fast (used by AuditService.verifyHashChain)
CREATE INDEX idx_audit_log_tenant_hash_chain
    ON audit_logs (tenant_id, id ASC)
    WHERE hash IS NOT NULL;


-- ────────────────────────────────────────────────────────────────────────────
-- 8. EOD PROCESS: One active EOD per tenant per business date
-- ────────────────────────────────────────────────────────────────────────────

-- Already enforced by unique constraint (tenant_id, business_date) on eod_processes.
-- This is a safety-net filtered index for RUNNING processes only.
CREATE UNIQUE INDEX idx_eod_one_running_per_tenant
    ON eod_processes (tenant_id)
    WHERE status = 'RUNNING';
