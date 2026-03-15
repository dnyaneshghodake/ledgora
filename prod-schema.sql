-- ============================================================================
-- Ledgora CBS — Production SQL Server Schema
-- Target: SQL Server 2019+
-- Generated: 2026-03-14
-- Description: Full DDL for Core Banking System with multi-tenant isolation,
--              double-entry ledger, maker-checker workflow, voucher-driven
--              posting, IBT clearing, suspense handling, and EOD lifecycle.
-- ============================================================================

-- ============================================================================
-- 1. TENANTS
-- ============================================================================
CREATE TABLE tenants (
    id                    BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_code           NVARCHAR(20)   NOT NULL,
    tenant_name           NVARCHAR(100)  NOT NULL,
    status                NVARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    current_business_date DATE           NOT NULL,
    day_status            NVARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    country               NVARCHAR(5)    DEFAULT 'IN',
    base_currency         NVARCHAR(5)    DEFAULT 'INR',
    timezone              NVARCHAR(50)   DEFAULT 'Asia/Kolkata',
    regulatory_code       NVARCHAR(50)   NULL,
    multi_branch_enabled  BIT            NOT NULL DEFAULT 0,
    eod_status            NVARCHAR(20)   DEFAULT 'NOT_STARTED',
    effective_from        DATE           NULL,
    remarks               NVARCHAR(500)  NULL,
    created_at            DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at            DATETIME2(3)   NULL,
    CONSTRAINT pk_tenants PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_tenant_code UNIQUE (tenant_code)
);
CREATE NONCLUSTERED INDEX idx_tenant_code ON tenants (tenant_code);

-- ============================================================================
-- 2. BRANCHES
-- ============================================================================
CREATE TABLE branches (
    id                BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_id         BIGINT         NOT NULL,
    branch_code       NVARCHAR(10)   NOT NULL,
    branch_name       NVARCHAR(100)  NOT NULL,
    name              NVARCHAR(100)  NULL,
    address           NVARCHAR(500)  NULL,
    city              NVARCHAR(50)   NULL,
    state             NVARCHAR(50)   NULL,
    pincode           NVARCHAR(10)   NULL,
    ifsc_code         NVARCHAR(11)   NULL,
    micr_code         NVARCHAR(9)    NULL,
    branch_type       NVARCHAR(30)   DEFAULT 'BRANCH',
    contact_phone     NVARCHAR(20)   NULL,
    contact_email     NVARCHAR(100)  NULL,
    is_active         BIT            NOT NULL DEFAULT 1,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        DATETIME2(3)   NULL,
    updated_at        DATETIME2(3)   NULL,
    CONSTRAINT pk_branches PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_branch_code UNIQUE (branch_code),
    CONSTRAINT fk_branch_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION
);
CREATE NONCLUSTERED INDEX idx_branch_tenant ON branches (tenant_id);

-- ============================================================================
-- 3. ROLES
-- ============================================================================
CREATE TABLE roles (
    id          BIGINT        IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(30)  NOT NULL,
    description NVARCHAR(100) NULL,
    CONSTRAINT pk_roles PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_role_name UNIQUE (name)
);

-- ============================================================================
-- 4. USERS
-- ============================================================================
CREATE TABLE users (
    id                    BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_id             BIGINT         NULL,
    tenant_scope          NVARCHAR(10)   DEFAULT 'SINGLE',
    username              NVARCHAR(50)   NOT NULL,
    password              NVARCHAR(255)  NOT NULL,
    full_name             NVARCHAR(100)  NOT NULL,
    email                 NVARCHAR(100)  NULL,
    phone                 NVARCHAR(20)   NULL,
    branch_code           NVARCHAR(10)   NULL,
    branch_id             BIGINT         NULL,
    is_active             BIT            NOT NULL DEFAULT 1,
    is_locked             BIT            NOT NULL DEFAULT 0,
    failed_login_attempts INT            DEFAULT 0,
    last_login            DATETIME2(3)   NULL,
    created_at            DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at            DATETIME2(3)   NULL,
    CONSTRAINT pk_users PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_username UNIQUE (username),
    CONSTRAINT uk_email UNIQUE (email),
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_user_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE NO ACTION
);

-- ============================================================================
-- 5. USER_ROLES (join table)
-- ============================================================================
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY CLUSTERED (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- ============================================================================
-- 6. GENERAL LEDGERS (GL Accounts / Chart of Accounts)
-- ============================================================================
CREATE TABLE general_ledgers (
    id             BIGINT          IDENTITY(1,1) NOT NULL,
    tenant_id      BIGINT          NULL,
    gl_code        NVARCHAR(20)    NOT NULL,
    gl_name        NVARCHAR(100)   NOT NULL,
    description    NVARCHAR(255)   NULL,
    account_type   NVARCHAR(20)    NOT NULL,
    parent_id      BIGINT          NULL,
    level_num      INT             NOT NULL DEFAULT 0,
    is_active      BIT             NOT NULL DEFAULT 1,
    balance        DECIMAL(19,4)   NOT NULL DEFAULT 0.0000,
    normal_balance NVARCHAR(10)    NULL,
    created_at     DATETIME2(3)    NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at     DATETIME2(3)    NULL,
    CONSTRAINT pk_general_ledgers PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_gl_code UNIQUE (gl_code),
    CONSTRAINT fk_gl_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_gl_parent FOREIGN KEY (parent_id) REFERENCES general_ledgers(id) ON DELETE NO ACTION,
    CONSTRAINT chk_gl_account_type CHECK (account_type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE'))
);
CREATE NONCLUSTERED INDEX idx_gl_tenant ON general_ledgers (tenant_id);
CREATE NONCLUSTERED INDEX idx_gl_parent ON general_ledgers (parent_id);
CREATE NONCLUSTERED INDEX idx_gl_account_type ON general_ledgers (account_type);

-- ============================================================================
-- 7. ACCOUNTS (Customer / Internal Accounts)
-- ============================================================================
CREATE TABLE accounts (
    id                      BIGINT          IDENTITY(1,1) NOT NULL,
    tenant_id               BIGINT          NULL,
    account_number          NVARCHAR(20)    NOT NULL,
    account_name            NVARCHAR(100)   NOT NULL,
    account_type            NVARCHAR(20)    NOT NULL,
    status                  NVARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    balance                 DECIMAL(19,4)   NOT NULL DEFAULT 0.0000,
    currency                NVARCHAR(3)     NOT NULL DEFAULT 'INR',
    branch_code             NVARCHAR(10)    NULL,
    branch_id               BIGINT          NULL,
    customer_name           NVARCHAR(100)   NULL,
    customer_email          NVARCHAR(100)   NULL,
    customer_phone          NVARCHAR(20)    NULL,
    customer_id             BIGINT          NULL,
    customer_master_id      BIGINT          NULL,
    customer_number         NVARCHAR(30)    NULL,
    home_branch_id          BIGINT          NULL,
    product_id              BIGINT          NULL,
    gl_account_code         NVARCHAR(20)    NULL,
    interest_rate           DECIMAL(5,2)    NULL,
    overdraft_limit         DECIMAL(19,4)   NULL,
    last_modified_by        BIGINT          NULL,
    ledger_account_type     NVARCHAR(30)    NULL,
    parent_account_id       BIGINT          NULL,
    freeze_level            NVARCHAR(20)    NOT NULL DEFAULT 'NONE',
    freeze_reason           NVARCHAR(255)   NULL,
    npa_flag                BIT             NOT NULL DEFAULT 0,
    npa_date                DATE            NULL,
    npa_provisioning_amount DECIMAL(19,4)   NULL,
    approval_status         NVARCHAR(20)    NOT NULL DEFAULT 'APPROVED',
    approved_by             BIGINT          NULL,
    created_by              BIGINT          NULL,
    created_at              DATETIME2(3)    NOT NULL DEFAULT SYSUTCDATETIME(),
    version                 BIGINT          NOT NULL DEFAULT 0,
    updated_at              DATETIME2(3)    NULL,
    CONSTRAINT pk_accounts PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_account_number UNIQUE (account_number),
    CONSTRAINT fk_account_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_account_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE NO ACTION,
    CONSTRAINT fk_account_home_branch FOREIGN KEY (home_branch_id) REFERENCES branches(id) ON DELETE NO ACTION,
    CONSTRAINT fk_account_parent FOREIGN KEY (parent_account_id) REFERENCES accounts(id) ON DELETE NO ACTION,
    CONSTRAINT fk_account_approved_by FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_account_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_account_last_modified FOREIGN KEY (last_modified_by) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT chk_account_type CHECK (account_type IN ('SAVINGS','CURRENT','LOAN','FIXED_DEPOSIT','CUSTOMER_ACCOUNT','GL_ACCOUNT','INTERNAL_ACCOUNT','CLEARING_ACCOUNT','SETTLEMENT_ACCOUNT','SUSPENSE_ACCOUNT')),
    CONSTRAINT chk_freeze_level CHECK (freeze_level IN ('NONE','DEBIT_ONLY','CREDIT_ONLY','FULL')),
    CONSTRAINT chk_approval_status CHECK (approval_status IN ('PENDING','APPROVED','REJECTED'))
);
CREATE NONCLUSTERED INDEX idx_account_number ON accounts (account_number);
CREATE NONCLUSTERED INDEX idx_account_tenant ON accounts (tenant_id);
CREATE NONCLUSTERED INDEX idx_account_branch ON accounts (branch_id);
CREATE NONCLUSTERED INDEX idx_account_gl_code ON accounts (gl_account_code);

-- ============================================================================
-- 8. ACCOUNT BALANCES (CBS Balance Engine)
-- ============================================================================
CREATE TABLE account_balances (
    account_id               BIGINT        NOT NULL,
    ledger_balance           DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    available_balance        DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    hold_amount              DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    actual_total_balance     DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    actual_cleared_balance   DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    shadow_total_balance     DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    shadow_clearing_balance  DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    inward_clearing_balance  DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    uncleared_effect_balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    lien_balance             DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    charge_hold_balance      DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    od_permitted             BIT           NOT NULL DEFAULT 0,
    version                  BIGINT        NOT NULL DEFAULT 0,
    last_updated             DATETIME2(3)  NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_account_balances PRIMARY KEY CLUSTERED (account_id),
    CONSTRAINT fk_acbal_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE NO ACTION
);

-- ============================================================================
-- 9. TRANSACTION BATCHES
-- ============================================================================
CREATE TABLE transaction_batches (
    id                BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_id         BIGINT         NOT NULL,
    batch_type        NVARCHAR(20)   NOT NULL,
    batch_code        NVARCHAR(50)   NULL,
    business_date     DATE           NOT NULL,
    version           BIGINT         NOT NULL DEFAULT 0,
    status            NVARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    total_debit       DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    total_credit      DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    transaction_count INT            NOT NULL DEFAULT 0,
    created_at        DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    closed_at         DATETIME2(3)   NULL,
    CONSTRAINT pk_transaction_batches PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_batch_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT chk_batch_status CHECK (status IN ('OPEN','CLOSED','SETTLED'))
);
CREATE NONCLUSTERED INDEX idx_batch_tenant_type_date ON transaction_batches (tenant_id, batch_type, business_date);
CREATE NONCLUSTERED INDEX idx_batch_status ON transaction_batches (status);
CREATE NONCLUSTERED INDEX idx_batch_code ON transaction_batches (batch_code);

-- ============================================================================
-- 10. TRANSACTIONS
-- ============================================================================
CREATE TABLE transactions (
    id                     BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_id              BIGINT         NULL,
    batch_id               BIGINT         NULL,
    branch_id              BIGINT         NULL,
    transaction_ref        NVARCHAR(30)   NOT NULL,
    transaction_type       NVARCHAR(20)   NOT NULL,
    status                 NVARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    transaction_sub_type   NVARCHAR(20)   NULL,
    risk_score             INT            NULL,
    amount                 DECIMAL(19,4)  NOT NULL,
    currency               NVARCHAR(3)    NOT NULL DEFAULT 'INR',
    channel                NVARCHAR(20)   NULL,
    client_reference_id    NVARCHAR(100)  NULL,
    source_account_id      BIGINT         NULL,
    destination_account_id BIGINT         NULL,
    description            NVARCHAR(255)  NULL,
    narration              NVARCHAR(500)  NULL,
    value_date             DATE           NULL,
    business_date          DATE           NULL,
    is_reversal            BIT            NOT NULL DEFAULT 0,
    reversal_transaction_id BIGINT        NULL,
    performed_by           BIGINT         NULL,
    maker_id               BIGINT         NULL,
    checker_id             BIGINT         NULL,
    maker_timestamp        DATETIME2(3)   NULL,
    checker_timestamp      DATETIME2(3)   NULL,
    checker_remarks        NVARCHAR(500)  NULL,
    version                BIGINT         NOT NULL DEFAULT 0,
    created_at             DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at             DATETIME2(3)   NULL,
    CONSTRAINT pk_transactions PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_transaction_ref UNIQUE (transaction_ref),
    CONSTRAINT fk_txn_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_txn_batch FOREIGN KEY (batch_id) REFERENCES transaction_batches(id) ON DELETE NO ACTION,
    CONSTRAINT fk_txn_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE NO ACTION,
    CONSTRAINT fk_txn_source FOREIGN KEY (source_account_id) REFERENCES accounts(id) ON DELETE NO ACTION,
    CONSTRAINT fk_txn_dest FOREIGN KEY (destination_account_id) REFERENCES accounts(id) ON DELETE NO ACTION,
    CONSTRAINT fk_txn_reversal FOREIGN KEY (reversal_transaction_id) REFERENCES transactions(id) ON DELETE NO ACTION,
    CONSTRAINT fk_txn_performer FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_txn_maker FOREIGN KEY (maker_id) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_txn_checker FOREIGN KEY (checker_id) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT chk_txn_type CHECK (transaction_type IN ('DEPOSIT','WITHDRAWAL','TRANSFER','REVERSAL','SETTLEMENT')),
    CONSTRAINT chk_txn_status CHECK (status IN ('PENDING','PENDING_APPROVAL','APPROVED','COMPLETED','FAILED','REVERSED','REJECTED','PARKED')),
    CONSTRAINT chk_txn_amount CHECK (amount >= 0)
);
CREATE NONCLUSTERED INDEX idx_transaction_ref ON transactions (transaction_ref);
CREATE NONCLUSTERED INDEX idx_txn_client_ref_channel_tenant ON transactions (client_reference_id, channel, tenant_id);
CREATE NONCLUSTERED INDEX idx_txn_tenant ON transactions (tenant_id);
CREATE NONCLUSTERED INDEX idx_txn_status ON transactions (status);
CREATE NONCLUSTERED INDEX idx_txn_business_date ON transactions (business_date);
CREATE NONCLUSTERED INDEX idx_txn_tenant_status ON transactions (tenant_id, status);
CREATE NONCLUSTERED INDEX idx_txn_source_account ON transactions (source_account_id);
CREATE NONCLUSTERED INDEX idx_txn_dest_account ON transactions (destination_account_id);

-- ============================================================================
-- 11. TRANSACTION LINES (Business Intent Layer)
-- ============================================================================
CREATE TABLE transaction_lines (
    id             BIGINT         IDENTITY(1,1) NOT NULL,
    transaction_id BIGINT         NOT NULL,
    account_id     BIGINT         NOT NULL,
    line_type      NVARCHAR(10)   NOT NULL,
    amount         DECIMAL(19,4)  NOT NULL,
    currency       NVARCHAR(3)    NOT NULL DEFAULT 'INR',
    description    NVARCHAR(255)  NULL,
    created_at     DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_transaction_lines PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_tl_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE NO ACTION,
    CONSTRAINT fk_tl_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE NO ACTION,
    CONSTRAINT chk_tl_type CHECK (line_type IN ('DEBIT','CREDIT')),
    CONSTRAINT chk_tl_amount CHECK (amount >= 0)
);
CREATE NONCLUSTERED INDEX idx_tl_transaction ON transaction_lines (transaction_id);

-- ============================================================================
-- 12. SCROLL SEQUENCES (Voucher Number Generation)
-- ============================================================================
CREATE TABLE scroll_sequences (
    id             BIGINT   IDENTITY(1,1) NOT NULL,
    tenant_id      BIGINT   NOT NULL,
    branch_id      BIGINT   NOT NULL,
    posting_date   DATE     NOT NULL,
    last_scroll_no BIGINT   NOT NULL DEFAULT 0,
    CONSTRAINT pk_scroll_sequences PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_scroll_seq UNIQUE (tenant_id, branch_id, posting_date)
);

-- ============================================================================
-- 13. VOUCHERS
-- ============================================================================
CREATE TABLE vouchers (
    id                     BIGINT          IDENTITY(1,1) NOT NULL,
    voucher_number         NVARCHAR(60)    NULL,
    tenant_id              BIGINT          NOT NULL,
    branch_id              BIGINT          NOT NULL,
    transaction_id         BIGINT          NULL,
    batch_id               BIGINT          NULL,
    batch_code             NVARCHAR(30)    NOT NULL,
    set_no                 INT             NOT NULL DEFAULT 1,
    scroll_no              BIGINT          NOT NULL,
    entry_date             DATE            NOT NULL,
    posting_date           DATE            NOT NULL,
    value_date             DATE            NOT NULL,
    effective_date         DATE            NULL,
    dr_cr                  NVARCHAR(5)     NOT NULL,
    account_id             BIGINT          NOT NULL,
    gl_account_id          BIGINT          NULL,
    transaction_amount     DECIMAL(19,4)   NOT NULL,
    local_currency_amount  DECIMAL(19,4)   NOT NULL,
    currency               NVARCHAR(3)     NOT NULL DEFAULT 'INR',
    maker_id               BIGINT          NOT NULL,
    checker_id             BIGINT          NULL,
    auth_flag              NVARCHAR(1)     NOT NULL DEFAULT 'N',
    post_flag              NVARCHAR(1)     NOT NULL DEFAULT 'N',
    cancel_flag            NVARCHAR(1)     NOT NULL DEFAULT 'N',
    financial_effect_flag  NVARCHAR(1)     NOT NULL DEFAULT 'Y',
    ledger_entry_id        BIGINT          NULL,
    narration              NVARCHAR(500)   NULL,
    reversal_of_voucher_id BIGINT          NULL,
    total_debit            DECIMAL(19,4)   NOT NULL DEFAULT 0.0000,
    total_credit           DECIMAL(19,4)   NOT NULL DEFAULT 0.0000,
    authorization_remarks  NVARCHAR(500)   NULL,
    version                BIGINT          NOT NULL DEFAULT 0,
    created_at             DATETIME2(3)    NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at             DATETIME2(3)    NULL,
    CONSTRAINT pk_vouchers PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_voucher_number UNIQUE (voucher_number),
    CONSTRAINT fk_vch_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_vch_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE NO ACTION,
    CONSTRAINT fk_vch_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE NO ACTION,
    CONSTRAINT fk_vch_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE NO ACTION,
    CONSTRAINT fk_vch_gl FOREIGN KEY (gl_account_id) REFERENCES general_ledgers(id) ON DELETE NO ACTION,
    CONSTRAINT fk_vch_maker FOREIGN KEY (maker_id) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_vch_checker FOREIGN KEY (checker_id) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_vch_reversal FOREIGN KEY (reversal_of_voucher_id) REFERENCES vouchers(id) ON DELETE NO ACTION,
    CONSTRAINT chk_vch_drcr CHECK (dr_cr IN ('DR','CR')),
    CONSTRAINT chk_vch_amount CHECK (transaction_amount >= 0),
    CONSTRAINT chk_vch_auth CHECK (auth_flag IN ('Y','N')),
    CONSTRAINT chk_vch_post CHECK (post_flag IN ('Y','N')),
    CONSTRAINT chk_vch_cancel CHECK (cancel_flag IN ('Y','N'))
);
CREATE NONCLUSTERED INDEX idx_voucher_composite ON vouchers (tenant_id, branch_id, posting_date, batch_code, scroll_no);
CREATE NONCLUSTERED INDEX idx_voucher_tenant ON vouchers (tenant_id);
CREATE NONCLUSTERED INDEX idx_voucher_tenant_date ON vouchers (tenant_id, posting_date);
CREATE NONCLUSTERED INDEX idx_voucher_status_flags ON vouchers (auth_flag, post_flag, cancel_flag);
CREATE NONCLUSTERED INDEX idx_voucher_branch ON vouchers (branch_id);
CREATE NONCLUSTERED INDEX idx_voucher_posting_date ON vouchers (posting_date);
CREATE NONCLUSTERED INDEX idx_voucher_account ON vouchers (account_id);
CREATE NONCLUSTERED INDEX idx_voucher_batch ON vouchers (batch_id);
CREATE NONCLUSTERED INDEX idx_voucher_transaction ON vouchers (transaction_id);
-- EOD aggregation: quickly sum posted DR/CR per tenant per date
CREATE NONCLUSTERED INDEX idx_voucher_posted_agg ON vouchers (tenant_id, posting_date, post_flag) INCLUDE (total_debit, total_credit);

-- ============================================================================
-- 14. LEDGER JOURNALS (Immutable)
-- ============================================================================
CREATE TABLE ledger_journals (
    id             BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_id      BIGINT         NULL,
    transaction_id BIGINT         NOT NULL,
    description    NVARCHAR(500)  NULL,
    business_date  DATE           NOT NULL,
    created_at     DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_ledger_journals PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_lj_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_lj_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE NO ACTION
);
CREATE NONCLUSTERED INDEX idx_ledger_journal_tenant ON ledger_journals (tenant_id);
CREATE NONCLUSTERED INDEX idx_ledger_journal_txn ON ledger_journals (transaction_id);
CREATE NONCLUSTERED INDEX idx_ledger_journal_date ON ledger_journals (business_date);

-- ============================================================================
-- 15. LEDGER ENTRIES (Immutable - Source of Financial Truth)
-- ============================================================================
CREATE TABLE ledger_entries (
    id                   BIGINT          IDENTITY(1,1) NOT NULL,
    tenant_id            BIGINT          NULL,
    journal_id           BIGINT          NULL,
    transaction_id       BIGINT          NOT NULL,
    account_id           BIGINT          NOT NULL,
    gl_account_id        BIGINT          NULL,
    gl_account_code      NVARCHAR(20)    NULL,
    entry_type           NVARCHAR(10)    NOT NULL,
    amount               DECIMAL(19,4)   NOT NULL,
    balance_after        DECIMAL(19,4)   NOT NULL,
    currency             NVARCHAR(3)     NOT NULL DEFAULT 'INR',
    business_date        DATE            NULL,
    posting_time         DATETIME2(3)    NULL,
    narration            NVARCHAR(255)   NULL,
    voucher_id           BIGINT          NULL,
    reversal_of_entry_id BIGINT          NULL,
    created_at           DATETIME2(3)    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_ledger_entries PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_le_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_le_journal FOREIGN KEY (journal_id) REFERENCES ledger_journals(id) ON DELETE NO ACTION,
    CONSTRAINT fk_le_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE NO ACTION,
    CONSTRAINT fk_le_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE NO ACTION,
    CONSTRAINT fk_le_gl FOREIGN KEY (gl_account_id) REFERENCES general_ledgers(id) ON DELETE NO ACTION,
    CONSTRAINT chk_le_entry_type CHECK (entry_type IN ('DEBIT','CREDIT')),
    CONSTRAINT chk_le_amount CHECK (amount > 0)
);
-- Primary query path: account statement (account + date range)
CREATE NONCLUSTERED INDEX idx_ledger_entry_account_created ON ledger_entries (account_id, created_at);
CREATE NONCLUSTERED INDEX idx_ledger_entry_journal ON ledger_entries (journal_id);
CREATE NONCLUSTERED INDEX idx_ledger_entry_tenant ON ledger_entries (tenant_id);
CREATE NONCLUSTERED INDEX idx_ledger_entry_txn ON ledger_entries (transaction_id);
CREATE NONCLUSTERED INDEX idx_ledger_entry_voucher ON ledger_entries (voucher_id);
CREATE NONCLUSTERED INDEX idx_ledger_entry_business_date ON ledger_entries (business_date);
-- GL aggregation: sum debits/credits per GL account per date
CREATE NONCLUSTERED INDEX idx_le_gl_agg ON ledger_entries (gl_account_id, business_date, entry_type) INCLUDE (amount);
-- EOD validation: sum debits/credits per tenant per date
CREATE NONCLUSTERED INDEX idx_le_tenant_date_type ON ledger_entries (tenant_id, business_date, entry_type) INCLUDE (amount);

-- ============================================================================
-- 16. INTER-BRANCH TRANSFERS (IBT Clearing)
-- ============================================================================
CREATE TABLE inter_branch_transfers (
    id                       BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_id                BIGINT         NOT NULL,
    from_branch_id           BIGINT         NOT NULL,
    to_branch_id             BIGINT         NOT NULL,
    amount                   DECIMAL(19,4)  NOT NULL,
    currency                 NVARCHAR(3)    NOT NULL DEFAULT 'INR',
    status                   NVARCHAR(20)   NOT NULL DEFAULT 'INITIATED',
    reference_transaction_id BIGINT         NULL,
    business_date            DATE           NOT NULL,
    settlement_date          DATE           NULL,
    narration                NVARCHAR(500)  NULL,
    created_by_id            BIGINT         NULL,
    approved_by_id           BIGINT         NULL,
    failure_reason           NVARCHAR(500)  NULL,
    version                  BIGINT         NOT NULL DEFAULT 0,
    created_at               DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at               DATETIME2(3)   NULL,
    CONSTRAINT pk_inter_branch_transfers PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_ibt_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ibt_from FOREIGN KEY (from_branch_id) REFERENCES branches(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ibt_to FOREIGN KEY (to_branch_id) REFERENCES branches(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ibt_txn FOREIGN KEY (reference_transaction_id) REFERENCES transactions(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ibt_created FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ibt_approved FOREIGN KEY (approved_by_id) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT chk_ibt_status CHECK (status IN ('INITIATED','SENT','RECEIVED','SETTLED','FAILED')),
    CONSTRAINT chk_ibt_amount CHECK (amount > 0)
);
CREATE NONCLUSTERED INDEX idx_ibt_tenant_date ON inter_branch_transfers (tenant_id, business_date);
CREATE NONCLUSTERED INDEX idx_ibt_status ON inter_branch_transfers (status);
CREATE NONCLUSTERED INDEX idx_ibt_from_branch ON inter_branch_transfers (from_branch_id);
CREATE NONCLUSTERED INDEX idx_ibt_to_branch ON inter_branch_transfers (to_branch_id);

-- ============================================================================
-- 17. SUSPENSE CASES
-- ============================================================================
CREATE TABLE suspense_cases (
    id                      BIGINT          IDENTITY(1,1) NOT NULL,
    tenant_id               BIGINT          NOT NULL,
    original_transaction_id BIGINT          NOT NULL,
    posted_voucher_id       BIGINT          NULL,
    suspense_voucher_id     BIGINT          NULL,
    intended_account_id     BIGINT          NOT NULL,
    suspense_account_id     BIGINT          NOT NULL,
    amount                  DECIMAL(19,4)   NOT NULL,
    currency                NVARCHAR(3)     NOT NULL DEFAULT 'INR',
    reason_code             NVARCHAR(50)    NOT NULL,
    reason_detail           NVARCHAR(1000)  NULL,
    status                  NVARCHAR(20)    NOT NULL DEFAULT 'OPEN',
    business_date           DATE            NOT NULL,
    resolution_voucher_id   BIGINT          NULL,
    resolved_by_id          BIGINT          NULL,
    resolution_checker_id   BIGINT          NULL,
    resolution_remarks      NVARCHAR(500)   NULL,
    resolved_at             DATETIME2(3)    NULL,
    created_at              DATETIME2(3)    NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at              DATETIME2(3)    NULL,
    CONSTRAINT pk_suspense_cases PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_sc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_sc_txn FOREIGN KEY (original_transaction_id) REFERENCES transactions(id) ON DELETE NO ACTION,
    CONSTRAINT fk_sc_posted_vch FOREIGN KEY (posted_voucher_id) REFERENCES vouchers(id) ON DELETE NO ACTION,
    CONSTRAINT fk_sc_suspense_vch FOREIGN KEY (suspense_voucher_id) REFERENCES vouchers(id) ON DELETE NO ACTION,
    CONSTRAINT fk_sc_intended FOREIGN KEY (intended_account_id) REFERENCES accounts(id) ON DELETE NO ACTION,
    CONSTRAINT fk_sc_suspense_acc FOREIGN KEY (suspense_account_id) REFERENCES accounts(id) ON DELETE NO ACTION,
    CONSTRAINT fk_sc_resolution_vch FOREIGN KEY (resolution_voucher_id) REFERENCES vouchers(id) ON DELETE NO ACTION,
    CONSTRAINT fk_sc_resolved_by FOREIGN KEY (resolved_by_id) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_sc_checker FOREIGN KEY (resolution_checker_id) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT chk_sc_status CHECK (status IN ('OPEN','RESOLVED','REVERSED'))
);
CREATE NONCLUSTERED INDEX idx_sc_tenant_date ON suspense_cases (tenant_id, business_date);
CREATE NONCLUSTERED INDEX idx_sc_status ON suspense_cases (status);
CREATE NONCLUSTERED INDEX idx_sc_transaction ON suspense_cases (original_transaction_id);

-- ============================================================================
-- 18. TELLER MASTERS
-- ============================================================================
CREATE TABLE teller_masters (
    id                          BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_id                   BIGINT         NOT NULL,
    branch_id                   BIGINT         NOT NULL,
    user_id                     BIGINT         NOT NULL,
    status                      NVARCHAR(20)   NOT NULL DEFAULT 'ASSIGNED',
    single_txn_limit_deposit    DECIMAL(19,4)  NOT NULL DEFAULT 200000.0000,
    single_txn_limit_withdrawal DECIMAL(19,4)  NOT NULL DEFAULT 50000.0000,
    daily_txn_limit             DECIMAL(19,4)  NOT NULL DEFAULT 500000.0000,
    cash_holding_limit          DECIMAL(19,4)  NOT NULL DEFAULT 1000000.0000,
    active_flag                 BIT            NOT NULL DEFAULT 1,
    created_at                  DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at                  DATETIME2(3)   NULL,
    version                     BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_teller_masters PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_teller_branch_user UNIQUE (branch_id, user_id),
    CONSTRAINT fk_tm_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_tm_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE NO ACTION,
    CONSTRAINT fk_tm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);
CREATE NONCLUSTERED INDEX idx_teller_master_branch ON teller_masters (branch_id);
CREATE NONCLUSTERED INDEX idx_teller_master_user ON teller_masters (user_id);
CREATE NONCLUSTERED INDEX idx_teller_master_tenant ON teller_masters (tenant_id);

-- ============================================================================
-- 19. TELLER SESSIONS
-- ============================================================================
CREATE TABLE teller_sessions (
    id               BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_id        BIGINT         NOT NULL,
    teller_id        BIGINT         NOT NULL,
    branch_id        BIGINT         NOT NULL,
    business_date    DATE           NOT NULL,
    opening_balance  DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    current_balance  DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    total_credit_today DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    total_debit_today  DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    state            NVARCHAR(20)   NOT NULL DEFAULT 'OPEN_REQUESTED',
    opened_by        BIGINT         NULL,
    authorized_by    BIGINT         NULL,
    closed_by        BIGINT         NULL,
    opened_at        DATETIME2(3)   NULL,
    closed_at        DATETIME2(3)   NULL,
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at       DATETIME2(3)   NULL,
    CONSTRAINT pk_teller_sessions PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_teller_session UNIQUE (teller_id, business_date),
    CONSTRAINT fk_ts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ts_teller FOREIGN KEY (teller_id) REFERENCES teller_masters(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ts_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ts_opened_by FOREIGN KEY (opened_by) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ts_authorized_by FOREIGN KEY (authorized_by) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ts_closed_by FOREIGN KEY (closed_by) REFERENCES users(id) ON DELETE NO ACTION
);
CREATE NONCLUSTERED INDEX idx_teller_session_teller ON teller_sessions (teller_id);
CREATE NONCLUSTERED INDEX idx_teller_session_branch ON teller_sessions (branch_id);
CREATE NONCLUSTERED INDEX idx_teller_session_tenant ON teller_sessions (tenant_id);
CREATE NONCLUSTERED INDEX idx_teller_session_date ON teller_sessions (business_date);

-- ============================================================================
-- 20. TELLER SESSION DENOMINATIONS
-- ============================================================================
CREATE TABLE teller_session_denominations (
    id                 BIGINT         IDENTITY(1,1) NOT NULL,
    session_id         BIGINT         NOT NULL,
    event_type         NVARCHAR(10)   NOT NULL,
    denomination_value DECIMAL(19,4)  NOT NULL,
    count              INT            NOT NULL,
    total_amount       DECIMAL(19,4)  NOT NULL,
    created_at         DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_teller_session_denom PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_tsd_session FOREIGN KEY (session_id) REFERENCES teller_sessions(id) ON DELETE NO ACTION,
    CONSTRAINT chk_tsd_event CHECK (event_type IN ('OPENING','CLOSING'))
);
CREATE NONCLUSTERED INDEX idx_tsd_session ON teller_session_denominations (session_id);
CREATE NONCLUSTERED INDEX idx_tsd_session_type ON teller_session_denominations (session_id, event_type);

-- ============================================================================
-- 21. AUDIT LOGS
-- ============================================================================
CREATE TABLE audit_logs (
    id               BIGINT          IDENTITY(1,1) NOT NULL,
    user_id          BIGINT          NULL,
    action           NVARCHAR(50)    NOT NULL,
    entity           NVARCHAR(50)    NOT NULL,
    entity_id        BIGINT          NULL,
    details          NVARCHAR(1000)  NULL,
    timestamp        DATETIME2(3)    NOT NULL DEFAULT SYSUTCDATETIME(),
    ip_address       NVARCHAR(45)    NULL,
    user_agent       NVARCHAR(500)   NULL,
    request_payload  NVARCHAR(4000)  NULL,
    response_payload NVARCHAR(4000)  NULL,
    username         NVARCHAR(100)   NULL,
    http_method      NVARCHAR(10)    NULL,
    request_uri      NVARCHAR(500)   NULL,
    old_value        NVARCHAR(4000)  NULL,
    new_value        NVARCHAR(4000)  NULL,
    batch_id         BIGINT          NULL,
    tenant_id        BIGINT          NULL,
    hash             NVARCHAR(64)    NULL,
    previous_hash    NVARCHAR(64)    NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY CLUSTERED (id)
);
CREATE NONCLUSTERED INDEX idx_audit_action ON audit_logs (action);
CREATE NONCLUSTERED INDEX idx_audit_entity ON audit_logs (entity, entity_id);
CREATE NONCLUSTERED INDEX idx_audit_user ON audit_logs (user_id);
CREATE NONCLUSTERED INDEX idx_audit_timestamp ON audit_logs (timestamp);
CREATE NONCLUSTERED INDEX idx_audit_tenant ON audit_logs (tenant_id);
CREATE NONCLUSTERED INDEX idx_audit_tenant_time ON audit_logs (tenant_id, timestamp);
-- Hash chain verification: find previous entry for a tenant
CREATE NONCLUSTERED INDEX idx_audit_hash_chain ON audit_logs (tenant_id, id) INCLUDE (hash);

-- ============================================================================
-- 22. APPROVAL REQUESTS (Maker-Checker Workflow)
-- ============================================================================
CREATE TABLE approval_requests (
    id           BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_id    BIGINT         NULL,
    entity_type  NVARCHAR(50)   NOT NULL,
    entity_id    BIGINT         NULL,
    request_data NVARCHAR(4000) NULL,
    requested_by BIGINT         NULL,
    approved_by  BIGINT         NULL,
    status       NVARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    remarks      NVARCHAR(500)  NULL,
    version      BIGINT         NOT NULL DEFAULT 0,
    created_at   DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    approved_at  DATETIME2(3)   NULL,
    CONSTRAINT pk_approval_requests PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_ar_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ar_requester FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT fk_ar_approver FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT chk_ar_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
);
CREATE NONCLUSTERED INDEX idx_approval_entity ON approval_requests (entity_type, entity_id);
CREATE NONCLUSTERED INDEX idx_approval_status ON approval_requests (status);
CREATE NONCLUSTERED INDEX idx_approval_tenant ON approval_requests (tenant_id);
CREATE NONCLUSTERED INDEX idx_approval_tenant_status ON approval_requests (tenant_id, status);

-- ============================================================================
-- 23. FRAUD ALERTS
-- ============================================================================
CREATE TABLE fraud_alerts (
    id              BIGINT          IDENTITY(1,1) NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    account_id      BIGINT          NOT NULL,
    account_number  NVARCHAR(50)    NOT NULL,
    alert_type      NVARCHAR(50)    NOT NULL,
    status          NVARCHAR(20)    NOT NULL DEFAULT 'OPEN',
    details         NVARCHAR(2000)  NOT NULL,
    observed_count  INT             NULL,
    observed_amount DECIMAL(19,4)   NULL,
    threshold_value NVARCHAR(100)   NULL,
    user_id         BIGINT          NULL,
    created_at      DATETIME2(3)    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_fraud_alerts PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_fa_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT chk_fa_status CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED','FALSE_POSITIVE'))
);
CREATE NONCLUSTERED INDEX idx_fa_tenant ON fraud_alerts (tenant_id);
CREATE NONCLUSTERED INDEX idx_fa_account ON fraud_alerts (account_id);
CREATE NONCLUSTERED INDEX idx_fa_status ON fraud_alerts (status);
CREATE NONCLUSTERED INDEX idx_fa_alert_type ON fraud_alerts (alert_type);

-- ============================================================================
-- 24. EOD PROCESSES (Crash-safe State Machine)
-- ============================================================================
CREATE TABLE eod_processes (
    id             BIGINT         IDENTITY(1,1) NOT NULL,
    tenant_id      BIGINT         NOT NULL,
    business_date  DATE           NOT NULL,
    phase          NVARCHAR(30)   NOT NULL,
    status         NVARCHAR(20)   NOT NULL DEFAULT 'RUNNING',
    failure_reason NVARCHAR(2000) NULL,
    started_at     DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    completed_at   DATETIME2(3)   NULL,
    last_updated   DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    version        BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_eod_processes PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_eod_tenant_date UNIQUE (tenant_id, business_date),
    CONSTRAINT fk_eod_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION,
    CONSTRAINT chk_eod_phase CHECK (phase IN ('VALIDATED','DAY_CLOSING','BATCH_CLOSED','SETTLED','DATE_ADVANCED')),
    CONSTRAINT chk_eod_status CHECK (status IN ('RUNNING','COMPLETED','FAILED'))
);
CREATE NONCLUSTERED INDEX idx_eod_tenant ON eod_processes (tenant_id);
CREATE NONCLUSTERED INDEX idx_eod_status ON eod_processes (status);

-- ============================================================================
-- 25. SETTLEMENTS
-- ============================================================================
CREATE TABLE settlements (
    id                BIGINT         IDENTITY(1,1) NOT NULL,
    settlement_ref    NVARCHAR(30)   NOT NULL,
    business_date     DATE           NOT NULL,
    status            NVARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    transaction_count INT            DEFAULT 0,
    remarks           NVARCHAR(500)  NULL,
    processed_by      BIGINT         NULL,
    start_time        DATETIME2(3)   NULL,
    end_time          DATETIME2(3)   NULL,
    created_at        DATETIME2(3)   NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_settlements PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_settlement_ref UNIQUE (settlement_ref),
    CONSTRAINT fk_stl_processor FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE NO ACTION,
    CONSTRAINT chk_stl_status CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','FAILED'))
);

-- ============================================================================
-- 26. SETTLEMENT ENTRIES
-- ============================================================================
CREATE TABLE settlement_entries (
    id              BIGINT         IDENTITY(1,1) NOT NULL,
    settlement_id   BIGINT         NOT NULL,
    account_id      BIGINT         NOT NULL,
    opening_balance DECIMAL(19,4)  NOT NULL,
    total_debits    DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    total_credits   DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    closing_balance DECIMAL(19,4)  NOT NULL,
    CONSTRAINT pk_settlement_entries PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_se_settlement FOREIGN KEY (settlement_id) REFERENCES settlements(id) ON DELETE NO ACTION,
    CONSTRAINT fk_se_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE NO ACTION
);
CREATE NONCLUSTERED INDEX idx_se_settlement ON settlement_entries (settlement_id);

-- ============================================================================
-- 27. IDEMPOTENCY KEYS
-- ============================================================================
CREATE TABLE idempotency_keys (
    id              BIGINT          IDENTITY(1,1) NOT NULL,
    tenant_id       BIGINT          NULL,
    idempotency_key NVARCHAR(255)   NOT NULL,
    request_hash    NVARCHAR(64)    NULL,
    response_hash   NVARCHAR(64)    NULL,
    response_body   NVARCHAR(4000)  NULL,
    status          NVARCHAR(20)    NOT NULL DEFAULT 'PROCESSING',
    created_at      DATETIME2(3)    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_idempotency_keys PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_idk_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE NO ACTION
);
CREATE NONCLUSTERED INDEX idx_idempotency_tenant ON idempotency_keys (tenant_id);

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================
