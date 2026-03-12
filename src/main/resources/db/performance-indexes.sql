-- =============================================================================
-- Ledgora CBS — Production Performance Indexing Strategy
-- =============================================================================
-- Purpose: Ensure stable query performance under production transaction volume
--          for dashboards, EOD validation, reconciliation, and audit exploration.
--
-- Compatibility: H2 (dev) + SQL Server (prod). Uses CREATE INDEX IF NOT EXISTS.
-- Execution: Run manually via H2 console or integrate into startup init.
--
-- CONSTRAINT: Does NOT drop existing indexes. Does NOT modify table structures.
--             Does NOT add foreign keys. Additive only.
--
-- Existing JPA @Index annotations (already created by Hibernate ddl-auto=update):
--   InterBranchTransfer: idx_ibt_tenant_date, idx_ibt_status, idx_ibt_from_branch, idx_ibt_to_branch
--   SuspenseCase:        idx_sc_tenant_date, idx_sc_status, idx_sc_transaction
--   Voucher:             idx_voucher_composite, idx_voucher_number, idx_voucher_tenant,
--                        idx_voucher_tenant_date, idx_voucher_status_flags, idx_voucher_branch,
--                        idx_voucher_posting_date, idx_voucher_account, idx_voucher_batch,
--                        idx_voucher_transaction
--   LedgerEntry:         idx_ledger_entry_account_created, idx_ledger_entry_journal,
--                        idx_ledger_entry_tenant
--   FraudAlert:          idx_fa_tenant, idx_fa_account, idx_fa_status, idx_fa_alert_type
--   Transaction:         idx_transaction_ref, idx_txn_client_ref_channel_tenant, idx_txn_tenant
--
-- Below indexes fill GAPS not covered by existing JPA annotations.
-- =============================================================================

-- =============================================
-- A) Inter-Branch Transfers — COMPOSITE gaps
-- =============================================
-- Gap: tenant + status composite (existing idx_ibt_status is status-only, no tenant leading)
-- Used by: IBT list (Specification filter), reconciliation dashboard countByTenantIdAndStatusIn,
--          EOD unsettled check, clearing engine
CREATE INDEX IF NOT EXISTS idx_ibt_tenant_status
    ON inter_branch_transfers (tenant_id, status);

-- Gap: reference_transaction_id lookup (used by IBT detail findByReferenceTransactionIdAndTenantId)
CREATE INDEX IF NOT EXISTS idx_ibt_ref_transaction
    ON inter_branch_transfers (reference_transaction_id);

-- Gap: tenant + created_at for aging queries (findOldestUnsettledByTenantId ORDER BY created_at)
CREATE INDEX IF NOT EXISTS idx_ibt_tenant_created
    ON inter_branch_transfers (tenant_id, created_at);

-- =============================================
-- B) Suspense Cases — COMPOSITE gaps
-- =============================================
-- Gap: tenant + status composite (existing idx_sc_status is status-only)
-- Used by: suspense dashboard countByTenantIdAndStatus, countOpenByTenantId,
--          sumOpenAmountByTenantId, EOD suspense validation
CREATE INDEX IF NOT EXISTS idx_sc_tenant_status
    ON suspense_cases (tenant_id, status);

-- Gap: tenant + created_at for aging queries (findOldestOpenByTenantId ORDER BY created_at)
CREATE INDEX IF NOT EXISTS idx_sc_tenant_created
    ON suspense_cases (tenant_id, created_at);

-- =============================================
-- C) Audit Logs — ALL new (no JPA @Index exists)
-- =============================================
-- Used by: AuditExplorerController Specification queries (date range, action, username)
--          HardCeilingDashboardController (countByTenantIdAndActionAndTimestampBetween,
--          findTop20ByTenantIdAndActionOrderByTimestampDesc)
CREATE INDEX IF NOT EXISTS idx_audit_tenant_timestamp
    ON audit_logs (tenant_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_audit_tenant_action
    ON audit_logs (tenant_id, action);

CREATE INDEX IF NOT EXISTS idx_audit_tenant_action_ts
    ON audit_logs (tenant_id, action, timestamp);

CREATE INDEX IF NOT EXISTS idx_audit_tenant_username
    ON audit_logs (tenant_id, username);

-- =============================================
-- D) Accounts — tenant + type gap
-- =============================================
-- Gap: tenant + account_type composite for sumBalanceByTenantIdAndAccountType
-- Used by: clearing engine (CLEARING_ACCOUNT), suspense dashboard (SUSPENSE_ACCOUNT),
--          IBT reconciliation (CLEARING_ACCOUNT), EOD validation
CREATE INDEX IF NOT EXISTS idx_accounts_tenant_type
    ON accounts (tenant_id, account_type);

-- =============================================
-- E) Ledger Entries — voucher_id gap
-- =============================================
-- Gap: voucher_id lookup for voucher-ledger join (AUDIT-03 queries, IBT detail)
-- Existing idx_ledger_entry_journal and idx_ledger_entry_tenant don't cover this
CREATE INDEX IF NOT EXISTS idx_ledger_voucher
    ON ledger_entries (voucher_id);

-- Gap: GL account code for GL reconciliation queries (AUDIT-07, trial balance)
CREATE INDEX IF NOT EXISTS idx_ledger_gl_code
    ON ledger_entries (gl_account_code);

-- Gap: business_date + tenant for EOD per-date balance checks (AUDIT-02)
CREATE INDEX IF NOT EXISTS idx_ledger_tenant_bizdate
    ON ledger_entries (tenant_id, business_date);

-- =============================================
-- F) Audit Logs — entity + entity_id for Transaction 360° View
-- =============================================
-- Gap: entity + entity_id composite for findByEntityAndEntityIdOrderByTimestampDesc
-- Used by: Transaction 360° View audit trail section
CREATE INDEX IF NOT EXISTS idx_audit_entity_entityid
    ON audit_logs (entity, entity_id);

-- =============================================
-- G) Ledger Entries — transaction_id for 360° View
-- =============================================
-- Gap: transaction_id lookup for Transaction 360° ledger tab
-- Used by: LedgerEntryRepository.findByTransactionId
CREATE INDEX IF NOT EXISTS idx_ledger_transaction
    ON ledger_entries (transaction_id);

-- =============================================
-- H) Fraud Alerts — tenant + created_at gap
-- =============================================
-- Gap: tenant + created_at for findTop20ByTenantIdOrderByCreatedAtDesc
-- Existing idx_fa_tenant is tenant-only, no ordering support
CREATE INDEX IF NOT EXISTS idx_fa_tenant_created
    ON fraud_alerts (tenant_id, created_at);
