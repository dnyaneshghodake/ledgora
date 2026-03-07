# Ledgora - Core Banking Platform

Ledgora is an enterprise-grade Core Banking System (CBS) built with Spring Boot, designed for RBI-grade financial control with multi-tenant architecture, double-entry accounting, batch-based transaction processing, and strict end-of-day (EOD) enforcement.

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2.3, Java 17 |
| Security | Spring Security + JWT (JJWT 0.11.5) |
| Database | H2 (Dev) / SQL Server (Prod) |
| ORM | Spring Data JPA / Hibernate |
| Frontend | JSP + JSTL + Bootstrap Icons |
| Build | Maven |
| Observability | Spring Boot Actuator + Micrometer Prometheus |

## Architecture Overview

```
+-----------------------------------------------------------+
|                    LEDGORA CBS                              |
+-----------------------------------------------------------+
|  JSP Frontend (Header, Sidebar, Dashboard, Batch UI)       |
+-----------------------------------------------------------+
|  REST Controllers + MVC Controllers                         |
|  (Auth, Customer, Account, Transaction, Settlement,         |
|   Ledger, GL, Batch, Tenant, Reports, Approval)            |
+-----------------------------------------------------------+
|  Service Layer                                              |
|  TransactionService | SettlementService | LedgerService     |
|  GlBalanceService   | BatchService      | TenantService     |
+-----------------------------------------------------------+
|  Security Layer                                             |
|  JWT Auth Filter | Tenant Context Filter | TenantContext    |
+-----------------------------------------------------------+
|  JPA Repositories + H2/SQL Server                           |
+-----------------------------------------------------------+
```

## Key Features

### 1. GL Balance Correction (PART 1)

- **GlBalanceService** updates GL account balances following accounting sign conventions
- **Accounting Rules:**
  - Debit increases Asset & Expense accounts
  - Credit increases Liability, Income (Revenue), and Equity accounts
- **Parent GL Rollup:** Balance changes propagate recursively from child to parent GL accounts
- **Chart of Accounts:** Full GL hierarchy with 5 root accounts (Assets, Liabilities, Equity, Revenue, Expenses) and multiple sub-levels

### 2. Transaction Batching (PART 2)

- **TransactionBatch entity** groups transactions by channel, tenant, and business date
- **Batch Types:** ATM, ONLINE, BRANCH_CASH, INTERNAL, BATCH
- **Batch Lifecycle:** OPEN -> CLOSED -> SETTLED
- **Batch Rules:**
  - Every transaction must belong to a batch
  - Batch determined by channel + tenant + business_date
  - Batch totals (debit/credit) tracked per transaction
  - Settlement validates total_debit == total_credit before marking SETTLED
  - Closed/settled batches cannot be modified
- **BatchService** methods:
  - `getOrCreateOpenBatch(tenantId, channel, businessDate)`
  - `updateBatchTotals(batchId, debitAmount, creditAmount)`
  - `closeAllBatches(tenantId, businessDate)`
  - `settleAllBatches(tenantId, businessDate)` (validates balance)
- **Batch Dashboard UI** showing open/closed/settled batches with summary cards

### 3. Multi-Tenant Architecture (PART 3)

- **Tenant entity** with independent business date and day status per tenant
- **tenant_id added to:** customers, accounts, transactions, ledger_entries, ledger_journals, batches, approvals, idempotency_keys, general_ledgers
- **Tenant Context Propagation:**
  - `TenantContextHolder` (ThreadLocal) for request-scoped tenant isolation
  - `TenantFilter` extracts tenant from JWT and sets context
  - JWT enhanced with `tenant_id` and `tenant_scope` (SINGLE/MULTI) claims
- **Security Rules:**
  - SINGLE tenant user: only access their tenant data
  - MULTI tenant admin: can switch tenant context via UI dropdown
- **Composite Index:** `(client_reference_id, channel, tenant_id)` for tenant-aware idempotency
- **Data Seeding:** Two default tenants (TENANT-001: Ledgora Main Bank, TENANT-002: Ledgora Partner Bank)

### 4. Strict End-of-Day Control (PART 4)

- **Per-tenant business date** with independent day status
- **Day Status State Machine:** OPEN -> DAY_CLOSING -> CLOSED -> OPEN (next day)
- **Transaction Blocking:**
  - `BusinessDayClosedException` thrown when day_status != OPEN
  - No transactions allowed during DAY_CLOSING or CLOSED states
- **Settlement Flow (per tenant):**
  1. Set tenant day_status = DAY_CLOSING (blocks new transactions)
  2. Close all open batches for the tenant
  3. Validate all batches balance (total_debit == total_credit)
  4. Settle all batches
  5. Validate ledger integrity
  6. Advance tenant business_date to next day
  7. Set day_status = OPEN

### 5. UI Enhancements (PART 5)

- **Batch Dashboard** (`/batches`): Shows open/closed/settled batches with summary cards, transaction counts, and totals per channel
- **Tenant Switch Dropdown:** Header dropdown for MULTI-tenant users to switch active tenant context
- **Header Enhancement:** Displays current Tenant Name | Business Date | Day Status
- **Sidebar:** Added Batch Management section with link to Batch Dashboard

## Banking Safety Rules

| Rule | Implementation |
|------|---------------|
| Ledger Immutability | Entries never updated/deleted; append-only system of record |
| Double-Entry Accounting | SUM(debits) = SUM(credits) enforced per journal |
| GL Balance Integrity | Derived from ledger entries with parent rollup |
| No Post-Close Transactions | BusinessDayClosedException when day_status != OPEN |
| Batch Balance Validation | total_debit == total_credit required before settlement |
| Tenant Data Isolation | tenant_id on all entities; context-based filtering |
| Idempotency | Composite key (client_reference_id, channel, tenant_id) prevents duplicates |
| Pessimistic Locking | Account-level locks for concurrent transaction safety |

## Project Structure

```
src/main/java/com/ledgora/
  account/          - Account entities, repositories, services, controllers
  approval/         - Maker-checker approval workflow
  auth/             - Authentication (User, Role, JWT)
  batch/            - Transaction batch management (NEW)
  branch/           - Branch management
  common/           - Shared enums, exceptions, DTOs, events
  config/           - Security config, DataInitializer, WebMvc config
  currency/         - Exchange rates
  customer/         - Customer management
  dashboard/        - Dashboard controller
  gl/               - General Ledger (Chart of Accounts, GL Balance Service)
  idempotency/      - Idempotency key management
  ledger/           - Ledger journals and entries (immutable)
  report/           - Financial reports (Trial Balance)
  settlement/       - Settlement processing (per-tenant EOD)
  tenant/           - Multi-tenant support (NEW)
  transaction/      - Transaction processing

src/main/webapp/WEB-INF/jsp/
  layout/           - header.jsp, sidebar.jsp, footer.jsp
  batch/            - batch-dashboard.jsp (NEW)
  dashboard/        - Main dashboard
  ...               - Other JSP views

src/test/java/com/ledgora/
  LedgoraEnhancementIntegrationTest.java - 25 integration tests covering all 5 parts
```

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+

### Build & Run

```bash
# Build
mvn clean compile

# Run tests
mvn test

# Full verification (compile + test + package)
mvn verify

# Run application
mvn spring-boot:run
```

The application starts on `http://localhost:8080` with H2 in-memory database.

### Default Users

| Username | Password | Role | Description |
|----------|----------|------|-------------|
| admin | admin123 | ADMIN | Full system access |
| manager | manager123 | MANAGER | Branch management + approvals |
| teller1 | teller123 | TELLER | Transaction processing |
| teller2 | teller123 | TELLER | Transaction processing |
| customer1 | cust123 | CUSTOMER | Rajesh Kumar |
| customer2 | cust123 | CUSTOMER | Priya Sharma |

### Default Tenants

| Code | Name | Business Date | Status |
|------|------|---------------|--------|
| TENANT-001 | Ledgora Main Bank | Current Date | OPEN |
| TENANT-002 | Ledgora Partner Bank | Current Date | OPEN |

### H2 Console
Access at `http://localhost:8080/h2-console` with:
- JDBC URL: `jdbc:h2:mem:ledgoradb`
- Username: `sa`
- Password: (empty)

## Integration Tests

25 comprehensive integration tests covering:

- **GL Balance Tests (5):** Asset/Liability/Expense/Revenue balance updates, parent GL rollup propagation
- **Batch Tests (6):** Batch creation, reuse, totals update, close/settle lifecycle, unbalanced settlement rejection, closed batch modification prevention
- **Multi-Tenant Tests (5):** Tenant creation, batch isolation, context holder, duplicate rejection, independent business dates
- **EOD Control Tests (5):** BusinessDayClosedException, day status transitions, invalid state transitions, validation pass
- **Settlement Tests (4):** Per-tenant settlement isolation, batch close isolation, all-batches-closed check, multi-channel batches

Run tests:
```bash
mvn test -Dtest=LedgoraEnhancementIntegrationTest
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/login` | JWT authentication |
| GET | `/dashboard` | Main dashboard |
| GET/POST | `/customers/**` | Customer CRUD |
| GET/POST | `/accounts/**` | Account management |
| POST | `/transactions/deposit` | Deposit |
| POST | `/transactions/withdraw` | Withdrawal |
| POST | `/transactions/transfer` | Fund transfer |
| GET | `/batches` | Batch dashboard |
| GET | `/gl` | Chart of Accounts |
| GET | `/ledger/explorer` | Ledger explorer |
| POST | `/settlements/process` | Run settlement |
| GET | `/tenant/switch/{id}` | Switch tenant context |
| GET | `/reports/**` | Financial reports |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metrics |

## License

Proprietary - Ledgora Core Banking Platform
