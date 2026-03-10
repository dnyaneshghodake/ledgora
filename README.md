# Ledgora — Core Banking Platform (Spring Boot + JSP)

Ledgora is a Spring Boot monolith (WAR) with a server-rendered JSP UI, focused on core banking primitives: customers, accounts, transactions, immutable double-entry ledger posting, batching, multi-tenancy, and business-day / EOD controls.

> Note: This repository previously had multiple root docs that were removed. This README is the current consolidated, code-verified guide.

## Tech Stack

From `pom.xml` and runtime configuration:

- **Java:** 17 (`pom.xml`)
- **Spring Boot:** 3.2.3 (`pom.xml`)
- **Packaging:** `war` (`pom.xml:17`)
- **Web:** Spring MVC + JSP (Tomcat Jasper + JSTL) (`pom.xml:51-66`, `src/main/resources/application.properties:8-9`)
- **Persistence:** Spring Data JPA + Hibernate (`pom.xml:33-37`)
- **Security:** Spring Security (form login) + JWT filter (`src/main/java/com/ledgora/config/SecurityConfig.java:56-111`)
- **DB:** H2 (dev) + SQL Server driver included (runtime) (`pom.xml:68-80`)
- **Observability:** Actuator + Micrometer Prometheus (`pom.xml:114-125`, `src/main/resources/application.properties:38-43`)

## Architecture (High Level)

This is a classic layered monolith:

- **JSP UI** under `/WEB-INF/jsp` (server-rendered)
- **Controllers** (`@Controller`) receive HTTP requests and build JSP models
- **Services** implement business rules (posting, batching, EOD)
- **Repositories** (Spring Data JPA) persist entities
- **Security filter chain** handles authn/authz + CSRF
- **Tenant context** is propagated through session + ThreadLocal (`TenantContextHolder`)

Key domain invariants are enforced by design:

- **Ledger immutability:** `LedgerJournal` and `LedgerEntry` are marked `@Immutable` (no updates) (`src/main/java/com/ledgora/ledger/entity/LedgerJournal.java:21`, `src/main/java/com/ledgora/ledger/entity/LedgerEntry.java:31`).
- **Double-entry:** Each posted business event creates balanced debit/credit entries (see seeding helper `createBalancedJournal(...)` in `src/main/java/com/ledgora/config/DataInitializer.java:851-904`).
- **Account balance is a cache:** `Account.balance` is explicitly a performance cache; true balance derives from ledger entries (`src/main/java/com/ledgora/account/entity/Account.java:52-58`).

## Getting Started (Dev)

### Prerequisites

- Java 17+
- Maven 3.6+

### Run

```bash
mvn clean test
mvn spring-boot:run
```

Application starts at:

- UI: `http://localhost:8080` (`src/main/resources/application.properties:4`)

### H2 Console (Dev)

Enabled by default:

- URL: `http://localhost:8080/h2-console` (`src/main/resources/application.properties:17`)
- JDBC URL: `jdbc:h2:mem:ledgoradb` (`src/main/resources/application.properties:12`)
- Username: `sa`
- Password: *(empty)*

## Seed Data (Ready-made verification)

On startup, `DataInitializer` seeds tenants, roles, users, branches, GL hierarchy, customers, accounts, balances, sample transactions + ledger, FX rates, and idempotency keys (`src/main/java/com/ledgora/config/DataInitializer.java:49-165`).

### Tenants

Seeded tenants:

- `TENANT-001` — **Ledgora Main Bank** (`src/main/java/com/ledgora/config/DataInitializer.java:170-179`)
- `TENANT-002` — **Ledgora Partner Bank** (`src/main/java/com/ledgora/config/DataInitializer.java:181-190`)

### Default Users (login)

Passwords are stored BCrypt-hashed, but these are the seeded raw credentials used at creation time:

| Username | Password | Notes |
|---|---|---|
| `admin` | `admin123` | ADMIN, **MULTI** tenant scope (`DataInitializer.java:253-257`) |
| `superadmin` | `super123` | SUPER_ADMIN + ADMIN, MULTI scope (`DataInitializer.java:314-317`) |
| `tenantadmin` | `tenant123` | TENANT_ADMIN, MULTI scope (`DataInitializer.java:318-320`) |
| `manager` | `manager123` | MANAGER (`DataInitializer.java:259-261`) |
| `teller1` | `teller123` | TELLER (`DataInitializer.java:263-265`) |
| `teller2` | `teller123` | TELLER (`DataInitializer.java:266-268`) |
| `teller3` | `teller123` | TELLER on TENANT-002 (`DataInitializer.java:326-329`) |
| `maker1` | `maker123` | MAKER (`DataInitializer.java:288-290`) |
| `checker1` | `checker123` | CHECKER (`DataInitializer.java:291-293`) |
| `ops1` | `ops123` | OPERATIONS (`DataInitializer.java:300-302`) |
| `auditor1` | `auditor123` | AUDITOR (`DataInitializer.java:303-305`) |
| `customer1` | `cust123` | CUSTOMER (`DataInitializer.java:270-272`) |
| `customer2` | `cust123` | CUSTOMER (`DataInitializer.java:273-275`) |
| `customer3` | `cust123` | CUSTOMER (`DataInitializer.java:276-278`) |
| `customer4` | `cust123` | CUSTOMER (`DataInitializer.java:279-281`) |

### Seeded Accounts (examples)

Customer accounts (TENANT-001) include:

- Rajesh Kumar:
  - `SAV-1001-0001` (Savings)
  - `CUR-1001-0001` (Current)
  - `LN-1001-0001` (Loan)
  - `FD-1001-0001` (Fixed Deposit)
- Priya Sharma:
  - `SAV-1002-0001` (Savings)
  - `CUR-1002-0001` (Current)
  - `FD-1002-0001` (Fixed Deposit)

See `src/main/java/com/ledgora/config/DataInitializer.java:556-603`.

### Sample Transactions

Four sample transactions are created (TENANT-001) with balanced journals/entries:

- Deposit: `DEP-SEED-0001` (Rajesh Savings)
- Deposit: `DEP-SEED-0002` (Priya Savings)
- Transfer: `TRF-SEED-0001` (Rajesh → Priya)
- Withdrawal: `WDR-SEED-0001` (Rajesh Current)

See `src/main/java/com/ledgora/config/DataInitializer.java:734-833`.

## End-to-End Flow (Operational)

### 1) Login → Session → Tenant Context

- User hits `/login` (GET) and authenticates via Spring Security form login (`src/main/java/com/ledgora/config/SecurityConfig.java:76-89`).
- Multi-tenant switching is controlled via session attributes (scope + allowed tenant list) and a POST switch endpoint:
  - Legacy GET switch is disabled: `/tenant/switch/{tenantId}` (`src/main/java/com/ledgora/tenant/controller/TenantController.java:32-38`)
  - Supported switch: `POST /tenant/switch` (`TenantController.java:40-82`)

### 2) Deposit / Withdrawal / Transfer (via Voucher layer)

Target posting flow:

```
Transaction → Voucher (DR+CR) → Authorize → Post → LedgerJournal → LedgerEntry (immutable) → Batch
```

1. User submits form (POST) → `TransactionController` → `TransactionService`
2. Service validates business rules, creates `Transaction` row
3. Service creates **Voucher pair** (DR leg + CR leg) via `VoucherService.createVoucher()`:
   - Each voucher linked to transaction via `transaction_id` FK
   - `voucherNumber` generated: `<TENANT>-<BRANCH>-<YYYYMMDD>-<6-digit scroll>`
   - Shadow balance updated (not actual)
4. Vouchers are **authorized** (system-auto for STP, manual checker for approval flow)
5. Vouchers are **posted** → creates immutable `LedgerJournal` + `LedgerEntry`, updates actual balance + GL + batch totals
6. Account balance cache updated

> For the full voucher lifecycle, status mapping, reversal flow, and security controls, see [`docs/E2E_FLOW.md`](docs/E2E_FLOW.md).

### 3) Batching

Transactions are grouped into `TransactionBatch` by:

- tenant
- channel → batch type mapping (`BatchService.java:199-207`)
- business date

Batch lifecycle:

- `OPEN` → `CLOSED` → `SETTLED` (`src/main/java/com/ledgora/common/enums/BatchStatus.java`)

Batch correctness:

- Totals can only be updated while `OPEN` (`BatchService.java:72-78`).
- Closing/settling requires `totalDebit == totalCredit` (`BatchService.java:137-144`, `159-173`).

### 4) EOD (End-of-Day)

EOD UI is driven by `EodController` (`src/main/java/com/ledgora/eod/controller/EodController.java`).

Routes:

- Day Begin validation + open day:
  - `GET /eod/day-begin` (`EodController.java:44-54`)
  - `POST /eod/day-begin` (`EodController.java:56-71`)
- EOD validation:
  - `GET /eod/validate` (`EodController.java:75-90`)
- EOD run:
  - `GET /eod/run` (`EodController.java:92-99`)
  - `POST /eod/run` (`EodController.java:101-116`)
- Status:
  - `GET /eod/status` (`EodController.java:118-128`)

### 5) Settlement (per-tenant)

Settlement is an operational flow that:

- moves tenant into DAY_CLOSING
- validates ledger integrity
- closes + settles batches
- advances the tenant business date

See `SettlementService.processSettlement(...)` (`src/main/java/com/ledgora/settlement/service/SettlementService.java:88-233`).

UI endpoints:

- `GET /settlements` list (`src/main/java/com/ledgora/settlement/controller/SettlementController.java:24-35`)
- `GET /settlements/process` form (`SettlementController.java:45-49`)
- `POST /settlements/process` execute (`SettlementController.java:51-65`)
- `GET /settlement/dashboard` dashboard (`src/main/java/com/ledgora/settlement/controller/SettlementDashboardController.java:39-75`)

## Endpoints (UI + system)

### Auth

- `GET /` → redirect to login (`src/main/java/com/ledgora/auth/controller/AuthController.java:25-28`)
- `GET /login` (`AuthController.java:30-42`)
- `GET /register` (`AuthController.java:44-48`)
- `POST /register` (`AuthController.java:50-64`)
- `POST /perform_login` (Spring Security login processing) (`src/main/java/com/ledgora/config/SecurityConfig.java:76-81`)
- `POST /logout` (`SecurityConfig.java:83-89`)

### Transactions

- `GET /transactions` list (`TransactionController.java:30-40`)
- `GET /transactions/{id}` view (`TransactionController.java:42-50`)
- Deposit:
  - `GET /transactions/deposit` (`TransactionController.java:52-57`)
  - `POST /transactions/deposit` (`TransactionController.java:59-87`)
- Withdrawal:
  - `GET /transactions/withdraw` (`TransactionController.java:89-94`)
  - `POST /transactions/withdraw` (`TransactionController.java:96-124`)
- Transfer:
  - `GET /transactions/transfer` (`TransactionController.java:126-131`)
  - `POST /transactions/transfer` (`TransactionController.java:133-161`)
- History:
  - `GET /transactions/history/{accountNumber}` (`TransactionController.java:163-169`)

### Tenant Switching

- `POST /tenant/switch` (`src/main/java/com/ledgora/tenant/controller/TenantController.java:40-82`)

### EOD

- `GET /eod/day-begin` / `POST /eod/day-begin`
- `GET /eod/validate`
- `GET /eod/run` / `POST /eod/run`
- `GET /eod/status`

(See `src/main/java/com/ledgora/eod/controller/EodController.java:44-128`.)

### Settlement

- `GET /settlement/dashboard` (`src/main/java/com/ledgora/settlement/controller/SettlementDashboardController.java:39-75`)
- `GET /settlements` (`src/main/java/com/ledgora/settlement/controller/SettlementController.java:24-35`)
- `GET /settlements/process` (`SettlementController.java:45-49`)
- `POST /settlements/process` (`SettlementController.java:51-65`)

### Admin (ADMIN only)

All under `/admin/**` and requires ADMIN (`src/main/java/com/ledgora/config/AdminController.java:33-36`, `src/main/java/com/ledgora/config/SecurityConfig.java:69`).

Examples:

- `GET /admin/tenants` (`AdminController.java:61-66`)
- `POST /admin/tenants/create` (`AdminController.java:68-79`)
- `GET /admin/branches` (`AdminController.java:83-88`)
- `POST /admin/branches/create` (`AdminController.java:90-111`)
- `GET /admin/users` (`AdminController.java:115-122`)
- `POST /admin/users/{id}/edit|toggle|delete` (`AdminController.java:137-205`)
- `GET /admin/audit` (`AdminController.java:265-282`)

### Vouchers (role-gated via @PreAuthorize)

- `GET /vouchers` — inquiry (MAKER, CHECKER, AUDITOR, ADMIN, MANAGER, TELLER, OPERATIONS)
- `GET /vouchers/{id}` — detail view (same roles)
- `GET /vouchers/create` — form (MAKER, ADMIN, MANAGER, TELLER)
- `POST /vouchers/create` — create DR+CR pair (same roles)
- `GET /vouchers/pending` — pending authorization list (CHECKER, ADMIN, MANAGER)
- `POST /vouchers/{id}/authorize` — authorize (CHECKER, ADMIN, MANAGER)
- `POST /vouchers/{id}/reject` — reject/cancel (CHECKER, ADMIN, MANAGER)
- `GET /vouchers/posted` — posted list (all operational roles)
- `GET /vouchers/cancelled` — cancelled list (all operational roles)

### Inter-Branch Transfer (IBT)

From `IbtController` (`src/main/java/com/ledgora/ibt/controller/IbtController.java`):

- `GET /ibt` — paginated list with status/date/branch filters (MAKER, CHECKER, OPERATIONS, ADMIN, MANAGER, AUDITOR)
- `GET /ibt/create` — IBT initiation form (MAKER, ADMIN, MANAGER, TELLER)
- `POST /ibt/create` — validate cross-branch + delegate to `TransactionService.transfer()` (same roles)
- `GET /ibt/{id}` — detail view: IBT header, branch-grouped voucher breakdown, ledger entries, clearing GL (MAKER, CHECKER, OPERATIONS, ADMIN, MANAGER, AUDITOR)
- `GET /ibt/reconciliation` — CBS reconciliation dashboard: KPIs, clearing GL net, aging table (OPERATIONS, ADMIN, MANAGER, AUDITOR)

### Governance Dashboards

- `GET /suspense/dashboard` — Suspense GL governance: open cases, GL net balance, aging table (OPERATIONS, ADMIN, MANAGER, AUDITOR)
- `GET /clearing/engine` — Clearing settlement readiness: unsettled IBTs, clearing GL net, settlement gate (OPERATIONS, ADMIN, MANAGER)
- `GET /risk/hard-ceiling` — Hard transaction ceiling violations: today's count, last 20 violations (OPERATIONS, ADMIN, MANAGER, AUDITOR)
- `GET /risk/velocity` — Velocity fraud monitoring: open alerts, frozen accounts, pressure level (OPERATIONS, ADMIN, MANAGER, AUDITOR)
- `GET /audit/explorer` — Enterprise audit log: searchable/filterable with date range, action, username, entity (ADMIN, AUDITOR)

### Stress Testing (stress profile only)

- `POST /stress/eod` — EOD performance stress test: generate bulk load + run EOD + return Hibernate statistics (ADMIN only)
- `POST /stress/lock-contention` — concurrent posting + parallel EOD lock contention simulation (ADMIN only)
- `POST /stress/deadlock` — cross-account deadlock provocation + recovery verification (ADMIN only)
- `POST /stress/load` — production-style load generator with rate limiting + percentile latency (ADMIN only)
- `POST /stress/chaos-eod` — EOD state machine crash/resume simulation + integrity verification (ADMIN only)
- `GET /diagnostics/query-plans` — EXPLAIN analysis for 6 critical queries with risk classification (ADMIN only)

### Reports

- `GET /reports` (`src/main/java/com/ledgora/reporting/controller/ReportingController.java:25-29`)
- `GET /reports/trial-balance?date=YYYY-MM-DD` (`ReportingController.java:31-38`)
- `GET /reports/account-statement?accountNumber=...&startDate=...&endDate=...` (`ReportingController.java:40-52`)
- `GET /reports/daily-summary?date=...` (`ReportingController.java:54-61`)
- `GET /reports/liquidity` (`ReportingController.java:63-68`)

### System

- H2 console: `GET /h2-console` (`src/main/resources/application.properties:17`)
- Actuator:
  - `GET /actuator/health`
  - `GET /actuator/info`
  - `GET /actuator/metrics`
  - `GET /actuator/prometheus`

Enabled via `management.endpoints.web.exposure.include` (`src/main/resources/application.properties:39`).

## Database Tables (Entity → Table)

This app uses Hibernate schema generation (`spring.jpa.hibernate.ddl-auto=update`, `src/main/resources/application.properties:20`).

Important tables (non-exhaustive but core):

- `tenants` (`src/main/java/com/ledgora/tenant/entity/Tenant.java:13-15`)
- `users`, `roles`, `user_roles` (`src/main/java/com/ledgora/auth/entity/User.java`, `src/main/java/com/ledgora/auth/entity/Role.java`)
- `branches` (`src/main/java/com/ledgora/branch/entity/Branch.java`)
- `customers`, `customer_master`, `customer_tax_profile` (`src/main/java/com/ledgora/customer/entity/*`)
- `accounts`, `account_balances` (`src/main/java/com/ledgora/account/entity/*`)
- `general_ledgers` (`src/main/java/com/ledgora/gl/entity/GeneralLedger.java`)
- `transactions` (`src/main/java/com/ledgora/transaction/entity/Transaction.java`)
- `transaction_batches` (`src/main/java/com/ledgora/batch/entity/TransactionBatch.java`)
- `ledger_journals`, `ledger_entries` (`src/main/java/com/ledgora/ledger/entity/*`)
- `settlements`, `settlement_entries` (`src/main/java/com/ledgora/settlement/entity/*`)
- `vouchers` (`src/main/java/com/ledgora/voucher/entity/Voucher.java`) — CBS voucher lifecycle (create→authorize→post→cancel)
- `scroll_sequences` (`src/main/java/com/ledgora/voucher/entity/ScrollSequence.java`) — concurrency-safe scroll number per tenant/branch/date
- `inter_branch_transfers` (`src/main/java/com/ledgora/clearing/entity/InterBranchTransfer.java`) — IBT lifecycle tracking (INITIATED→SENT→RECEIVED→SETTLED/FAILED)
- `branch_gl_mappings` (`src/main/java/com/ledgora/clearing/entity/BranchGlMapping.java`) — per-branch clearing GL configuration
- `suspense_cases` (`src/main/java/com/ledgora/suspense/entity/SuspenseCase.java`) — suspense GL case lifecycle (OPEN→RESOLVED/REVERSED)
- `suspense_gl_mappings` (`src/main/java/com/ledgora/suspense/entity/SuspenseGlMapping.java`) — tenant+channel→suspense account routing
- `fraud_alerts` (`src/main/java/com/ledgora/fraud/entity/FraudAlert.java`) — velocity breach alerts with account freeze tracking
- `velocity_limits` (`src/main/java/com/ledgora/fraud/entity/VelocityLimit.java`) — per-account/tenant velocity thresholds
- `hard_transaction_limits` (`src/main/java/com/ledgora/approval/entity/HardTransactionLimit.java`) — absolute transaction ceilings per tenant+channel
- `audit_logs` (`src/main/java/com/ledgora/audit/entity/AuditLog.java`) — immutable governance + financial event trail
- `eod_processes` (`src/main/java/com/ledgora/eod/entity/EodProcess.java`) — crash-safe EOD state machine tracking
- `exchange_rates` (`src/main/java/com/ledgora/currency/entity/ExchangeRate.java`)
- `idempotency_keys` (`src/main/java/com/ledgora/idempotency/entity/IdempotencyKey.java`)
- `system_dates` (`src/main/java/com/ledgora/common/entity/SystemDate.java`)

## Quick Verification (H2 SQL)

Open `/h2-console` and run the following.

### Check tenants

```sql
select id, tenant_code, tenant_name, status, current_business_date, day_status
from tenants
order by id;
```

### Check seeded users

```sql
select u.id, u.username, u.tenant_scope, t.tenant_code
from users u
left join tenants t on t.id = u.tenant_id
order by u.username;
```

### Check key accounts

```sql
select id, account_number, account_name, account_type, currency, balance
from accounts
where account_number in ('SAV-1001-0001','SAV-1002-0001','CUR-1001-0001')
order by account_number;
```

### Check sample transactions

```sql
select id, transaction_ref, transaction_type, status, amount, currency, business_date
from transactions
order by id;
```

### Ledger integrity (overall)

> Ledger entry sign handling depends on `entry_type`.

```sql
select
  sum(case when entry_type = 'DEBIT' then amount else 0 end)  as total_debits,
  sum(case when entry_type = 'CREDIT' then amount else 0 end) as total_credits
from ledger_entries;
```

### See ledger entries for a transaction

```sql
select le.id, t.transaction_ref, le.entry_type, le.amount, le.gl_account_code, le.business_date
from ledger_entries le
join transactions t on t.id = le.transaction_id
where t.transaction_ref = 'DEP-SEED-0001'
order by le.id;
```

### Check vouchers and their lifecycle

```sql
select id, voucher_number, dr_cr, transaction_amount, total_debit, total_credit,
       auth_flag, post_flag, cancel_flag, transaction_id, batch_code
from vouchers
order by id desc;
```

### Voucher debit/credit balance for today

```sql
select sum(total_debit) as total_dr, sum(total_credit) as total_cr,
       case when sum(total_debit) = sum(total_credit) then 'BALANCED' else 'UNBALANCED' end as status
from vouchers
where post_flag = 'Y' and cancel_flag = 'N'
  and posting_date = CURRENT_DATE;
```

### Check IBT records

```sql
select id, status, amount, currency, business_date,
       from_branch_id, to_branch_id, reference_transaction_id
from inter_branch_transfers
order by id desc;
```

### Check clearing GL net balance (must be zero before EOD)

```sql
select a.account_number, a.account_name, a.balance,
       case when a.balance = 0 then 'ZERO' else 'NON-ZERO' end as status
from accounts a
where a.account_type = 'CLEARING_ACCOUNT'
   or a.account_number like 'IBC-%'
order by a.account_number;
```

## Security Notes

- CSRF is enabled using cookie-based tokens; H2 console is excluded (`src/main/java/com/ledgora/config/SecurityConfig.java:59-63`).
- `/admin/**` is role-gated by both security config and `@PreAuthorize` (`SecurityConfig.java:69`, `AdminController.java:35-36`).
- Tenant switching is **POST-only** and validates MULTI scope + allowed tenants list (`src/main/java/com/ledgora/tenant/controller/TenantController.java:40-66`).
- Voucher endpoints are role-gated via `@PreAuthorize`: MAKER can create, CHECKER can authorize/reject, AUDITOR is read-only. Maker-checker enforcement prevents self-approval (`VoucherService.authorizeVoucher`).

---

## License

Proprietary.
