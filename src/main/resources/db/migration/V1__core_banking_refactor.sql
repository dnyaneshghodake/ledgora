-- =====================================================
-- Ledgora Core Banking Platform - Schema Migration
-- Version: 1.0
-- Description: Complete schema for Parts 1-7 refactoring
-- =====================================================

-- PART 7: Branch Structure
CREATE TABLE IF NOT EXISTS branches (
    id BIGINT IDENTITY PRIMARY KEY,
    branch_code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(500),
    is_active BIT NOT NULL DEFAULT 1
);

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Users table (PART 7: added branch_code)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    branch_code VARCHAR(10),
    is_active BIT NOT NULL DEFAULT 1,
    is_locked BIT NOT NULL DEFAULT 0,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_login DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME
);

-- User-Role mapping
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- General Ledger (Chart of Accounts)
CREATE TABLE IF NOT EXISTS general_ledgers (
    id BIGINT IDENTITY PRIMARY KEY,
    gl_code VARCHAR(20) NOT NULL UNIQUE,
    gl_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    account_type VARCHAR(20) NOT NULL,
    parent_id BIGINT,
    level_num INT NOT NULL DEFAULT 0,
    is_active BIT NOT NULL DEFAULT 1,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    normal_balance VARCHAR(10),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    FOREIGN KEY (parent_id) REFERENCES general_ledgers(id)
);

-- Accounts table (PART 7: added branch_code)
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT IDENTITY PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    branch_code VARCHAR(10),
    customer_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(100),
    customer_phone VARCHAR(20),
    gl_account_code VARCHAR(20),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- PART 3: Account Balance Cache
CREATE TABLE IF NOT EXISTS account_balances (
    account_id BIGINT PRIMARY KEY,
    ledger_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    available_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    hold_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- Transactions table (PART 2: added reversal_transaction_id, PART 4: added business_date)
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT IDENTITY PRIMARY KEY,
    transaction_ref VARCHAR(30) NOT NULL UNIQUE,
    transaction_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    source_account_id BIGINT,
    destination_account_id BIGINT,
    description VARCHAR(255),
    narration VARCHAR(500),
    value_date DATETIME,
    business_date DATE,
    reversal_transaction_id BIGINT,
    performed_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    FOREIGN KEY (destination_account_id) REFERENCES accounts(id),
    FOREIGN KEY (reversal_transaction_id) REFERENCES transactions(id),
    FOREIGN KEY (performed_by) REFERENCES users(id)
);

-- PART 1: Transaction Lines (intermediate layer between Transaction and Ledger Entries)
CREATE TABLE IF NOT EXISTS transaction_lines (
    id BIGINT IDENTITY PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    line_type VARCHAR(10) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- PART 2: Ledger Entries (immutable double-entry accounting)
CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGINT IDENTITY PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    gl_account_id BIGINT,
    gl_account_code VARCHAR(20),
    entry_type VARCHAR(10) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    business_date DATE,
    posting_time DATETIME,
    narration VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (gl_account_id) REFERENCES general_ledgers(id)
);

-- PART 4: Business Date / System Date
CREATE TABLE IF NOT EXISTS system_dates (
    id BIGINT IDENTITY PRIMARY KEY,
    business_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);

-- PART 5: Settlement tables
CREATE TABLE IF NOT EXISTS settlements (
    id BIGINT IDENTITY PRIMARY KEY,
    settlement_ref VARCHAR(30) NOT NULL UNIQUE,
    business_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_count INT DEFAULT 0,
    remarks VARCHAR(500),
    processed_by BIGINT,
    start_time DATETIME,
    end_time DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (processed_by) REFERENCES users(id)
);

-- PART 5: Settlement Entries (per-account settlement detail)
CREATE TABLE IF NOT EXISTS settlement_entries (
    id BIGINT IDENTITY PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    opening_balance DECIMAL(19,4) NOT NULL,
    total_debits DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_credits DECIMAL(19,4) NOT NULL DEFAULT 0,
    closing_balance DECIMAL(19,4) NOT NULL,
    FOREIGN KEY (settlement_id) REFERENCES settlements(id),
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- PART 6: Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT IDENTITY PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(50) NOT NULL,
    entity VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    details VARCHAR(1000),
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45)
);

-- =====================================================
-- Indexes for performance
-- =====================================================
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_branch_code ON accounts(branch_code);
CREATE INDEX idx_transactions_ref ON transactions(transaction_ref);
CREATE INDEX idx_transactions_business_date ON transactions(business_date);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transaction_lines_transaction_id ON transaction_lines(transaction_id);
CREATE INDEX idx_transaction_lines_account_id ON transaction_lines(account_id);
CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_business_date ON ledger_entries(business_date);
CREATE INDEX idx_ledger_entries_gl_account_code ON ledger_entries(gl_account_code);
CREATE INDEX idx_settlements_business_date ON settlements(business_date);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_system_dates_status ON system_dates(status);
