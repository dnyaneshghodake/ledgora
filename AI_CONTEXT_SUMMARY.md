# Ledgora AI Context Summary

This document is a **prompt-ready system map** for AI assistants working on this repository.

## 1) What this app is

Ledgora is a Spring Boot monolithic Core Banking System (CBS) with JSP-based server-rendered UI. It emphasizes:
- double-entry accounting,
- immutable ledger posting,
- strict business-day/EOD controls,
- transaction batching,
- multi-tenant data isolation,
- maker-checker style approvals and operations views.

## 2) Tech and runtime profile

- Java 17, Spring Boot 3.2.3, packaged as a `war`.
- MVC + JSP (`/WEB-INF/jsp`) with Bootstrap assets.
- Spring Data JPA + Hibernate.
- H2 in-memory DB for dev; SQL Server driver present for runtime/prod.
- Spring Security with form login + JWT support classes/filters.
- Actuator + Micrometer Prometheus metrics.

## 3) High-level architecture

Request flow is roughly:
1. `TenantFilter` sets tenant context (session/header/default).
2. Security chain authenticates/authorizes (form login + JWT filter).
3. MVC controllers route to service layer.
4. Services orchestrate domain rules and write through JPA repositories.
5. Ledger/account/batch/tenant constraints are applied in service transactions.

A key runtime pattern is **ThreadLocal tenant context** via `TenantContextHolder`.

## 4) Core domain modules and responsibilities

- `auth/`: users, roles, registration/login pages, security integration.
- `account/`: customer/internal accounts + balances.
- `transaction/`: deposit/withdraw/transfer orchestration, posting, events.
- `ledger/`: journals and entries (system of record for accounting movement).
- `gl/`: chart of accounts and GL balance logic (including rollups).
- `batch/`: channel/date/tenant-based transaction batch lifecycle.
- `settlement/`: batch/settlement processing views and services.
- `tenant/`: tenant entity, tenant switching, business date/day-status state.
- `eod/`: EOD validation + close/advance day flow.
- `voucher/`: voucher lifecycle views and supporting service/repo.
- `approval/`: pending/approve/reject flows.
- `reporting/`: trial balance, liquidity, daily summary, account statement pages.
- `customer/`, `branch/`, `currency/`, `idempotency/`, `audit/`, `dashboard/`, `observability/` provide surrounding banking platform capabilities.

## 5) Non-negotiable banking invariants

When changing code, preserve these rules:
- **Double-entry integrity**: debits must equal credits for a journaled business event.
- **Ledger immutability**: ledger entries are append-only system-of-record artifacts.
- **Business-day gate**: transactional activity is blocked when tenant day is not OPEN.
- **Batch correctness**: only OPEN batches are mutable; settlement requires balanced totals.
- **Tenant isolation**: tenant context determines data boundaries; avoid cross-tenant leaks.
- **Idempotency**: client/channel/tenant deduplication is expected in transaction entry paths.

## 6) Business-day and EOD behavior (critical)

EOD logic validates voucher posting/authorization and ledger balancing signals, then transitions day state and advances business date. Transaction services explicitly check tenant business-day status before posting, so bypassing service-layer checks is unsafe.

## 7) Security and tenancy details to remember

- Security chain permits `/`, `/login`, `/register`, static resources, and H2 console.
- Most business routes require authentication; `/admin/**` requires admin role.
- Tenant is resolved from session `tenantId` or `X-Tenant-Id` header; if missing defaults to tenant `1`.
- Tenant context is cleared at request completion.

## 8) Main MVC route families

Use these as discovery anchors while implementing changes:
- `/dashboard`
- `/customers/**`
- `/accounts/**`
- `/transactions/**`
- `/settlements/**`, `/settlement/dashboard`
- `/ledger/explorer`
- `/gl/**`
- `/approvals/**`
- `/reports/**`
- `/batches`
- `/eod/**`
- `/vouchers/**`
- `/tenant/switch` (POST)
- `/tenant/switch/{tenantId}`

## 9) Seeded local environment assumptions

`DataInitializer` seeds roles, users, branches, tenants, GL structure, accounts/balances, sample financial data, and idempotency artifacts. This means local behavior often assumes pre-existing baseline data and default credentials.

## 10) AI coding guidance for this repo

- Prefer service-layer updates over controller/repository shortcuts.
- For transaction-like flows, verify tenant checks, idempotency, batch assignment, ledger posting, and audit/event side-effects stay coherent.
- Preserve enum-driven state transitions (`OPEN/CLOSED/...`) and avoid introducing ad hoc status strings.
- Keep JSP + controller model attribute contracts intact when altering UI flows.
- Validate with integration tests whenever changing accounting, batch, tenant, or EOD logic.

## 11) Prompt starter (copy/paste)

Use this when asking another AI to work safely:

> You are modifying Ledgora (Spring Boot monolith for core banking). Preserve double-entry accounting, immutable ledger behavior, tenant isolation via `TenantContextHolder`, batch lifecycle correctness, and business-day/EOD constraints. Implement changes in service layer first, keep MVC/JSP contracts stable, and update tests for transaction/settlement/tenant/EOD side effects.

