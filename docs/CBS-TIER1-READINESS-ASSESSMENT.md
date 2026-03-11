# Ledgora CBS Tier-1 Readiness Assessment (Current State)

> Scope: Current-stage documentation of the Ledgora application as a Core Banking System (CBS), including directory structure, end-to-end flow, and a prioritized gap list to reach “CBS grade Tier-1”.
>
> Source-of-truth: code at `a3e6076` (latest PR head after all P0 + P1 fixes).

---

## 1) Executive Summary

Ledgora is a Spring Boot 3.2 (Java 17) monolithic CBS-style application with JSP UI. It already implements several Tier-1 control patterns:

- **Tenant-scoped operations** using JWT tenant claims + `TenantContextHolder` ThreadLocal.
- **Maker-checker** workflow for high-risk operations.
- **Voucher lifecycle** (create → authorize → post → cancel/reverse) with immutable ledger journal/entries.
- **EOD crash-safe state machine** with phase-by-phase commits.
- **Fraud and risk controls** (hard ceilings, velocity checks, governance exceptions).
- **Audit logging** with a tenant-specific SHA-256 hash chain for tamper evidence.

However, there are important gaps for Tier-1 production-grade CBS (regulatory, security, correctness, resiliency, and operational maturity). This document captures the current flows and provides a structured gap backlog.

---

## 2) Tech Stack & Runtime Profile

### 2.1 Backend
- Java 17
- Spring Boot 3.2.3
- Spring MVC + JSP rendering (`spring.mvc.view.prefix=/WEB-INF/jsp/`)
- Spring Data JPA (Hibernate)
- Spring Security (form login + JWT)
- Actuator + Micrometer Prometheus

### 2.2 Frontend
- JSP + JSTL
- Bootstrap CSS + Bootstrap Icons
- Vanilla JavaScript (`src/main/webapp/resources/js/app.js`)

### 2.3 Persistence
- H2 configured for development (`spring.datasource.url=jdbc:h2:mem:ledgoradb`)
- SQL Server driver included (runtime)

### 2.4 Packaging
- WAR packaging (servlet container deployment)

---

## 3) High-Level Architecture

### 3.1 Primary Domains (Module Map)

- **tenant/**: tenant model, business date control, tenant operation locks
- **security/** + **config/**: auth, JWT, filter chain, login success handler
- **account/**: accounts, balances, product snapshots
- **customer/**: customer lifecycle, maker-checker
- **transaction/**: deposit/withdraw/transfer, approval decisions, posting orchestration
- **voucher/**: voucher lifecycle + scroll sequences
- **ledger/**: immutable ledger journal/entries
- **gl/**: chart of accounts + GL balance propagation
- **batch/**: batch lifecycle (open → closed → settled)
- **approval/**: approval requests + approval policy + hard ceilings
- **fraud/**: velocity fraud engine + alerts
- **clearing/**: inter-branch transfer / clearing
- **interest/**: daily interest accrual calculation logs
- **loan/**: loan schedules, DPD updates, NPA classification
- **reconciliation/**: scheduled reconciliation and EOD validation
- **audit/**: audit trail and hash chain
- **eod/**: EOD validation and crash-safe state machine

---

## 4) Directory Structure (Curated)

> This is a curated structure focusing on CBS-critical areas.

### 4.1 Backend (`src/main/java/com/ledgora/`)

- `LedgoraApplication.java` (boot entry, scheduling enabled)

**Security & Auth**
- `config/SecurityConfig.java`
- `security/JwtAuthenticationFilter.java`
- `security/JwtTokenProvider.java`
- `security/CustomAuthenticationSuccessHandler.java`

**Tenant**
- `tenant/context/TenantContextHolder.java`
- `tenant/service/TenantService.java`
- `tenant/service/TenantOperationLockService.java`
- `tenant/controller/TenantController.java`

**Transactions / Accounting**
- `transaction/service/TransactionService.java`
- `voucher/service/VoucherService.java`
- `balance/service/CbsBalanceEngine.java`
- `ledger/entity/*` and `ledger/repository/*`
- `gl/service/GlBalanceService.java`
- `batch/service/BatchService.java`

**EOD**
- `eod/controller/EodController.java`
- `eod/service/EodValidationService.java`
- `eod/service/EodStateMachineService.java`

**Audit / Governance**
- `audit/service/AuditService.java`
- `approval/service/ApprovalService.java`
- `approval/service/ApprovalPolicyService.java`
- `approval/service/HardTransactionCeilingService.java`

### 4.2 Frontend (`src/main/webapp/WEB-INF/jsp/`)

**Layout**
- `layout/header.jsp`
- `layout/sidebar.jsp`

**EOD UI**
- `eod/day-begin.jsp`
- `eod/eod-validate.jsp`
- `eod/eod-run.jsp`
- `eod/eod-status.jsp`

### 4.3 Static (`src/main/webapp/resources/`)
- `js/app.js` (session timer + UI behavior)

---

## 5) End-to-End Flows (Current State)

### 5.1 Authentication → Tenant Context Setup

**Flow**
1. User logs in (form login).
2. `CustomAuthenticationSuccessHandler` stores in session:
   - `username`, role flags, `tenantId`, `tenantName`, `tenantScope`, `branch*`, `businessDate*`
   - Generates JWT with tenant claims and stores `jwt_token`.
3. On every request, `JwtAuthenticationFilter`:
   - extracts JWT from Authorization header or session
   - validates token
   - sets Spring Security `Authentication`
   - sets `TenantContextHolder.setTenantId(tenantId)`
   - ensures `TenantContextHolder.clear()` in finally block

**Key files**
- `src/main/java/com/ledgora/security/CustomAuthenticationSuccessHandler.java`
- `src/main/java/com/ledgora/security/JwtAuthenticationFilter.java`
- `src/main/java/com/ledgora/tenant/context/TenantContextHolder.java`


### 5.2 Tenant Switching (MULTI scope)

**Flow**
1. Header renders tenant switch dropdown when `sessionScope.tenantScope == 'MULTI'` (`layout/header.jsp`).
2. User posts to `/tenant/switch`.
3. Controller validates:
   - session tenantScope is MULTI
   - target tenantId exists in `availableTenants`
4. Updates session `tenantId`, `tenantName`, `tenantCode`, `businessDate`, `businessDateStatus`.
5. Updates `TenantContextHolder.setTenantId()` for immediate thread context.

**Key file**
- `src/main/java/com/ledgora/tenant/controller/TenantController.java`


### 5.3 Customer Lifecycle (Maker-Checker)

**Create (Maker)**
1. `CustomerService.createCustomer()` persists customer with KYC status = PENDING.
2. Creates `ApprovalRequest` for entity type CUSTOMER.
3. Writes audit log entry.

**Approve/Reject (Checker)**
1. Enforces maker != checker.
2. Updates KYC status.
3. Writes audit log.

**Key file**
- `src/main/java/com/ledgora/customer/service/CustomerService.java`


### 5.4 Account Opening (Legacy vs Product Engine)

**Legacy path**
- If `productId` is null, account type derived from DTO enum and GL from dto.

**Product path**
1. Load product and validate:
   - belongs to tenant
   - status ACTIVE
2. Load effective product version.
3. Load GL mapping for the version.
4. Derive account type and GL codes.
5. Save Account.
6. Save immutable `AccountProductSnapshot`.

**Key file**
- `src/main/java/com/ledgora/account/service/AccountService.java`


### 5.5 Transaction Flow (Deposit / Withdraw / Transfer)

**Shared pre-checks**
1. Validate amount (`RbiFieldValidator.validateTransactionAmount`).
2. Validate business day OPEN (`TenantService.validateBusinessDayOpen`).
3. Enforce **Hard Ceiling** (absolute limit).
4. Idempotency check (`clientReferenceId + channel`).
5. Lock accounts pessimistically.
6. Validate account ACTIVE and maker-checker approval status.
7. Freeze-level enforcement.
8. Velocity fraud check (may freeze account to UNDER_REVIEW and block).
9. Holiday calendar enforcement.
10. Determine batch (tenant+channel+businessDate) and approval policy decision.

**If auto-authorized**
- Post immediately by creating vouchers, authorizing (SYSTEM_AUTO), posting vouchers → ledger entries → balances updated.

**If requires approval**
- Persist transaction in PENDING_APPROVAL.
- Create `ApprovalRequest`.
- On approval, re-validate and post.

**Key file**
- `src/main/java/com/ledgora/transaction/service/TransactionService.java`


### 5.6 Voucher Lifecycle (Create → Authorize → Post → Cancel)

- Create: generates voucher number w/ scroll sequence; updates shadow balance.
- Authorize: enforces maker != checker (or uses SYSTEM_AUTO).
- Post: validates tenant day status OPEN, posting date matches tenant business date, voucher integrity; creates `LedgerJournal` and `LedgerEntry`.
- Cancel: blocks backdated cancellation; creates reversal voucher if already posted.

**Key file**
- `src/main/java/com/ledgora/voucher/service/VoucherService.java`


### 5.7 Ledger & GL Posting

- Ledger journal and entry are created per voucher posting.
- GL balances updated using sign conventions (asset/expense: debit increases; liability/revenue/equity: credit increases), with recursive propagation to parent GL.

**Key files**
- `src/main/java/com/ledgora/ledger/entity/*`
- `src/main/java/com/ledgora/gl/service/GlBalanceService.java`


### 5.8 EOD (End of Day) Flow — Crash-Safe

**Validation** (`EodValidationService.validateEod`)
- all vouchers authorized
- no approved-but-unposted vouchers
- ledger DR == CR
- posted voucher totals DR == CR
- no pending approvals
- no pending approval transactions
- inter-branch transfers settled/failed
- clearing GL net zero
- suspense within tolerance
- tenant GL balanced

**Execution** (`EodStateMachineService.executeEod`)
Phases (each `REQUIRES_NEW`):
1. VALIDATED
2. DAY_CLOSING (locks day, revalidates)
3. BATCH_CLOSED
4. SETTLED
5. DATE_ADVANCED (advances tenant business date and marks EOD COMPLETED)

**Key files**
- `src/main/java/com/ledgora/eod/service/EodValidationService.java`
- `src/main/java/com/ledgora/eod/service/EodStateMachineService.java`
- `src/main/java/com/ledgora/eod/controller/EodController.java`

---

## 6) Current “Tier-1” Strengths (What’s Already CBS-Grade)

1. **Tenant context discipline**: JWT → ThreadLocal → cleared per request.
2. **Maker-checker controls** across multiple domains.
3. **Voucher + ledger immutability** (posting creates immutable ledger entries).
4. **Crash-safe EOD orchestration** with phase persistence and resume behavior.
5. **Risk engines**: hard ceilings + velocity fraud blocks.
6. **Operational guardrails**: posting blocked when business day is not OPEN.
7. **Audit logging** with hash chain and verification.
8. **Inter-branch clearing flow** with IBC accounts.

---

## 7) Tier-1 Gaps & Backlog (Prioritized)

> This section is intentionally action-oriented. Each gap is phrased as a CBS Tier-1 requirement and the current deficiency.

### P0 — Correctness / Isolation / Data Integrity

1. ~~**Tenant isolation not uniformly enforced in all read paths**~~ ✅ FIXED
   - All CBS-critical services now use `findByIdAndTenantId()` repository methods.
   - 7 new tenant-scoped repository methods added; 18 files updated across 3 commits.

2. ~~**Dual business-date systems (global SystemDate vs per-tenant business date)**~~ ✅ FIXED
   - `BusinessDateService` marked `@Deprecated(forRemoval=true)`. Dead dependency removed from `TransactionService`.
   - All CBS operations use `TenantService.getCurrentBusinessDate(tenantId)` as the single authority.

3. ~~**Reconciliation scheduler hard-coded tenant**~~ ✅ FIXED
   - `BalanceReconciliationService.scheduledReconciliation()` now iterates all active tenants.

4. ~~**Audit hash chain not applied to all audit entry types**~~ ✅ FIXED
   - `logFinancialEvent()` and `logChangeEvent()` now compute SHA-256 hash chain, consistent with `logEvent()`.

### P1 — Security / Compliance

5. **Session timer is cosmetic and not authoritative**
   - The JS countdown in `app.js` is UX-only; session timeout enforcement is server-side.
   - Tier-1 expectation: consistent timeout warnings based on server-side expiry metadata; avoid false confidence.

6. **JWT secret handling**
   - Startup validation exists (good), but deployment hardening needs formalization: rotation, secret management integration, audit.

### P1 — Operational Resilience / Observability

7. ~~**EOD locking & concurrency**~~ ✅ FIXED
   - `EodStateMachineService.executeEod()` now acquires `TenantOperationLockService` lock before any phase runs
     and releases in a `finally` block. Concurrent EOD/Settlement/Reconciliation is blocked per tenant.

8. **Idempotency**
   - Present for transactions, but must be enforced consistently for all externally-triggered financial operations.

### P2 — Product Engine / Parameters / Config Governance

9. **Config governance**
   - Tier-1 expectation: maker-checker on product configs, GL mappings, limits, fraud rules, etc., with versioning, effective dating, and audit.

### P2 — Data Model / Accounting Controls

10. **Formal double-entry invariants at database level**
   - Tier-1 expectation: database constraints and ledger/journal invariants that prevent partial posting and support audit/reconciliation at scale.

---

## 8) Suggested Next Steps (Roadmap)

### Phase A: Hardening (P0) — ✅ COMPLETE
- ~~Enforce tenant scoping everywhere~~ ✅ Done (26 edits, 18 files, 7 new repository methods)
- ~~Consolidate business date architecture (tenant-first)~~ ✅ Done (BusinessDateService deprecated, dead dep removed)
- ~~Update scheduled jobs to iterate all tenants~~ ✅ Done
- ~~Apply audit hash chain to all regulatory audit events~~ ✅ Done

### Phase B: Tier-1 Controls — IN PROGRESS
- ~~EOD mutual exclusion via TenantOperationLockService~~ ✅ Done
- Expand EOD phases: interest accrual posting, NPA provisioning voucher posting, settlement postings
- Strengthen concurrency controls: outbox/event consistency

### Phase C: Operations
- Add structured observability dashboards
- Add operational runbooks: EOD recovery, stuck phase handling, clearing failures

---

## 9) Appendix: Key Entry Points

- Boot entry: `src/main/java/com/ledgora/LedgoraApplication.java`
- Security config: `src/main/java/com/ledgora/config/SecurityConfig.java`
- Login success handler: `src/main/java/com/ledgora/security/CustomAuthenticationSuccessHandler.java`
- Tenant switching: `src/main/java/com/ledgora/tenant/controller/TenantController.java`
- Transaction processing: `src/main/java/com/ledgora/transaction/service/TransactionService.java`
- Voucher engine: `src/main/java/com/ledgora/voucher/service/VoucherService.java`
- EOD validation: `src/main/java/com/ledgora/eod/service/EodValidationService.java`
- EOD state machine: `src/main/java/com/ledgora/eod/service/EodStateMachineService.java`
- UI layout: `src/main/webapp/WEB-INF/jsp/layout/header.jsp`, `src/main/webapp/WEB-INF/jsp/layout/sidebar.jsp`
