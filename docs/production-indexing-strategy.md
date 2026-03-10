# Ledgora — Production Performance Indexing Strategy

**Version:** 1.0
**Baseline:** PR #43 — Feature Enhancement
**SQL File:** `src/main/resources/db/performance-indexes.sql`

## Overview

This document describes the database indexing strategy for Ledgora CBS production deployment. All indexes are **additive-only** — no existing indexes are dropped or modified.

The project uses `spring.jpa.hibernate.ddl-auto=update` which auto-creates indexes defined via JPA `@Index` annotations on entities. The performance indexes in this strategy fill **gaps** not covered by those annotations, targeting specific query patterns from dashboards, EOD validation, reconciliation, and audit exploration.

## Execution

The SQL file is located at `src/main/resources/db/performance-indexes.sql`.

**Dev (H2):** Run via H2 console (`/h2-console`) after application startup.

**Prod (SQL Server):** Execute as part of deployment runbook before enabling traffic. All statements use `CREATE INDEX IF NOT EXISTS` for idempotent re-execution.

**Future:** When Flyway is adopted, convert to a versioned migration (e.g., `V43_1__production_performance_indexes.sql`).

## Index Inventory

### A) Inter-Branch Transfers (`inter_branch_transfers`)

| Index | Columns | Query Pattern | Dashboard/Feature |
|---|---|---|---|
| `idx_ibt_tenant_status` | `(tenant_id, status)` | `countByTenantIdAndStatusIn`, JPA Specification filter | IBT list, reconciliation KPIs, clearing engine, EOD |
| `idx_ibt_ref_transaction` | `(reference_transaction_id)` | `findByReferenceTransactionIdAndTenantId` | IBT detail (Transaction ID redirect) |
| `idx_ibt_tenant_created` | `(tenant_id, created_at)` | `findOldestUnsettledByTenantId ORDER BY created_at` | Reconciliation aging table, clearing engine |

**Already exists (JPA @Index):** `idx_ibt_tenant_date` (tenant+business_date), `idx_ibt_status` (status-only), `idx_ibt_from_branch`, `idx_ibt_to_branch`.

**Gap filled:** The existing `idx_ibt_status` has no tenant leading column — multi-tenant queries like `countByTenantIdAndStatusIn` would scan all tenants. `idx_ibt_tenant_status` fixes this.

**Risk of removal:** Without `idx_ibt_tenant_status`, the IBT reconciliation dashboard and EOD unsettled check degrade to full table scan filtered in memory. Blocking for production.

---

### B) Suspense Cases (`suspense_cases`)

| Index | Columns | Query Pattern | Dashboard/Feature |
|---|---|---|---|
| `idx_sc_tenant_status` | `(tenant_id, status)` | `countByTenantIdAndStatus`, `countOpenByTenantId`, `sumOpenAmountByTenantId` | Suspense dashboard KPIs, EOD suspense validation |
| `idx_sc_tenant_created` | `(tenant_id, created_at)` | `findOldestOpenByTenantId ORDER BY created_at` | Suspense dashboard aging table |

**Already exists (JPA @Index):** `idx_sc_tenant_date` (tenant+business_date), `idx_sc_status` (status-only), `idx_sc_transaction`.

**Gap filled:** Same pattern as IBT — `idx_sc_status` lacks tenant leading column. Dashboard KPI queries filter by tenant first.

---

### C) Audit Logs (`audit_logs`)

| Index | Columns | Query Pattern | Dashboard/Feature |
|---|---|---|---|
| `idx_audit_tenant_timestamp` | `(tenant_id, timestamp)` | Specification date range filter | Audit Explorer (date range queries) |
| `idx_audit_tenant_action` | `(tenant_id, action)` | `findByTenantIdAndActionOrderByTimestampDesc` | Hard ceiling dashboard, audit explorer |
| `idx_audit_tenant_action_ts` | `(tenant_id, action, timestamp)` | `countByTenantIdAndActionAndTimestampBetween` | Hard ceiling "today's violations" count |
| `idx_audit_tenant_username` | `(tenant_id, username)` | Specification username filter | Audit Explorer (username search) |

**Already exists:** None. The `audit_logs` entity has no `@Index` annotations.

**Gap filled:** All audit explorer queries were previously unindexed. This is the highest-impact indexing for the audit domain.

**Risk of removal:** Audit Explorer degrades to full table scan on every page load. Hard ceiling dashboard "today's count" becomes O(n) over all audit records.

---

### D) Accounts (`accounts`)

| Index | Columns | Query Pattern | Dashboard/Feature |
|---|---|---|---|
| `idx_accounts_tenant_type` | `(tenant_id, account_type)` | `sumBalanceByTenantIdAndAccountType` | Clearing engine, suspense dashboard, IBT reconciliation, EOD |

**Already exists:** None for this composite.

**Gap filled:** The `sumBalanceByTenantIdAndAccountType` query runs on every clearing/suspense dashboard load and during EOD validation. Without this index, it scans all accounts for the tenant.

---

### E) Ledger Entries (`ledger_entries`)

| Index | Columns | Query Pattern | Dashboard/Feature |
|---|---|---|---|
| `idx_ledger_voucher` | `(voucher_id)` | AUDIT-03 voucher-ledger join, IBT detail | Voucher detail, audit SQL pack |
| `idx_ledger_gl_code` | `(gl_account_code)` | GL reconciliation (AUDIT-07), trial balance | Reports, EOD GL balance check |
| `idx_ledger_tenant_bizdate` | `(tenant_id, business_date)` | `sumDebitsByBusinessDateAndTenantId`, AUDIT-02 | EOD per-date balance, settlement |

**Already exists (JPA @Index):** `idx_ledger_entry_account_created` (account+created_at), `idx_ledger_entry_journal` (journal_id), `idx_ledger_entry_tenant` (tenant-only).

**Gap filled:** `voucher_id` and `gl_account_code` lookups were completely unindexed. The tenant+business_date composite is critical for EOD per-date balance checks (the existing `idx_ledger_entry_tenant` is tenant-only).

---

### F) Fraud Alerts (`fraud_alerts`)

| Index | Columns | Query Pattern | Dashboard/Feature |
|---|---|---|---|
| `idx_fa_tenant_created` | `(tenant_id, created_at)` | `findTop20ByTenantIdOrderByCreatedAtDesc` | Velocity fraud dashboard recent alerts |

**Already exists (JPA @Index):** `idx_fa_tenant` (tenant-only), `idx_fa_account`, `idx_fa_status`, `idx_fa_alert_type`.

**Gap filled:** The "last 20 alerts" query orders by `created_at DESC` but the existing tenant index has no ordering support.

---

## Index Count per Table

| Table | Existing (JPA) | New (this strategy) | Total | Limit (4) |
|---|---|---|---|---|
| `inter_branch_transfers` | 4 | 3 | 7 | **Justified:** 6 distinct query patterns across 4 dashboards + EOD |
| `suspense_cases` | 3 | 2 | 5 | **Justified:** dashboard + EOD critical path |
| `audit_logs` | 0 | 4 | 4 | ✅ At limit |
| `accounts` | 0 | 1 | 1 | ✅ Well under |
| `ledger_entries` | 3 | 3 | 6 | **Justified:** immutable table, insert-once/read-many pattern |
| `fraud_alerts` | 4 | 1 | 5 | **Justified:** dashboard query + existing 4 are single-column |

**Note on write impact:** `ledger_entries` and `audit_logs` are append-only (never updated). `inter_branch_transfers` and `suspense_cases` have low write frequency (IBT lifecycle transitions). Index maintenance overhead is negligible for these tables.

## Validation Checklist

After applying indexes:

1. Enable Hibernate statistics: `spring.jpa.properties.hibernate.generate_statistics=true`
2. Verify no full table scans for:
   - IBT reconciliation dashboard (`GET /ibt/reconciliation`)
   - Suspense dashboard (`GET /suspense/dashboard`)
   - Clearing engine (`GET /clearing/engine`)
   - Hard ceiling monitor (`GET /risk/hard-ceiling`)
   - Velocity fraud dashboard (`GET /risk/velocity`)
   - Audit explorer (`GET /audit/explorer`)
   - EOD validation (`GET /eod/validate`)
3. Verify execution plans use indexes (H2: `EXPLAIN SELECT ...`)
4. Ensure insert performance not degraded for high-throughput tables (`vouchers`, `ledger_entries`, `transactions`)
5. Monitor `prepareStatementCount` in Hibernate statistics — dashboard pages should stay ≤3 SELECTs
