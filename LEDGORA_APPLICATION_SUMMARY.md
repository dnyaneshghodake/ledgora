# Ledgora - Core Banking Platform

## Full Application Summary: Functionality & Technology

---

## 1. Platform Overview

**Ledgora** is a monolithic, multi-tenant Core Banking System (CBS) built for regulatory-grade financial operations. It implements double-entry accounting, maker-checker approval workflows, voucher-based transaction processing, and end-of-day (EOD) settlement -- all within a single deployable Spring Boot application with a JSP-based web frontend.

The platform is designed to serve as a complete banking back-office system supporting customer onboarding, account management, financial transactions (deposits, withdrawals, transfers), general ledger maintenance, settlement processing, and regulatory reporting.

---

## 2. Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Java | 17 (LTS) |
| **Framework** | Spring Boot | 3.2.3 |
| **Web** | Spring MVC + JSP (JSTL) | Jakarta EE |
| **Security** | Spring Security | 6.x |
| **ORM** | Spring Data JPA / Hibernate | 6.x |
| **Database (Dev)** | H2 (In-Memory) | Runtime |
| **Database (Prod)** | Microsoft SQL Server | MSSQL JDBC |
| **Authentication** | JWT (JSON Web Tokens) | jjwt 0.11.5 |
| **Build Tool** | Apache Maven | 3.6+ |
| **Packaging** | WAR (deployable to Tomcat) | -- |
| **Code Generation** | Lombok | Latest |
| **JSON** | Jackson + JSR-310 DateTime | -- |
| **Observability** | Spring Boot Actuator + Micrometer Prometheus | -- |
| **Validation** | Jakarta Bean Validation (spring-boot-starter-validation) | -- |
| **View Engine** | Apache Tomcat Embed Jasper + JSTL | -- |

### Build & Run

```bash
# Build
mvn clean package

# Run (development with H2)
java -jar target/ledgora.war
# or
mvn spring-boot:run

# Access
http://localhost:8080
```

### Key Configuration

| Property | Value |
|----------|-------|
| Server Port | 8080 |
| Context Path | / |
| JSP Location | /WEB-INF/jsp/ |
| H2 Console | /h2-console (dev only) |
| Actuator Endpoints | /actuator/health, /actuator/metrics, /actuator/prometheus |
| DDL Strategy | hibernate.ddl-auto=update |

---

## 3. Architecture

### 3.1 Multi-Tenant Architecture

Ledgora operates as a **single-database, multi-tenant** application. Every major entity (Account, Customer, Transaction, Voucher, Ledger, etc.) is linked to a `Tenant` via a foreign key. Tenant context is managed through `TenantContextHolder`, a ThreadLocal-based mechanism that is set on each request based on the authenticated user's tenant assignment.

**Tenant Entity Fields:**
- `tenantCode` -- Unique identifier (e.g., "BANK001")
- `tenantName` -- Display name
- `currentBusinessDate` -- The tenant's current business date (advanced by EOD)
- `dayStatus` -- OPEN or CLOSED (controls whether transactions are allowed)

**User Scoping:**
- Each `User` belongs to a `Tenant` (via `tenant_id`)
- `TenantScope` enum (SINGLE / MULTI) controls whether a user can operate across tenants

### 3.2 Module Structure

The application is organized into **25+ domain modules**, each following a standard layered pattern:

```
com.ledgora.{module}/
    controller/     -- Spring MVC Controllers (JSP views + REST)
    dto/            -- Data Transfer Objects
    entity/         -- JPA Entities
    repository/     -- Spring Data JPA Repositories
    service/        -- Business Logic Services
```

### 3.3 Security Architecture

- **Authentication**: Form-based login with Spring Security + JWT token support
- **Authorization**: Role-based access control via `User` <-> `Role` (ManyToMany)
- **CSRF Protection**: Enabled via `CookieCsrfTokenRepository` for all endpoints (except H2 console in dev)
- **Password Encoding**: BCrypt via `PasswordEncoder` bean
- **Session Management**: Custom `AuthenticationSuccessHandler` sets tenant context on login
- **Account Lockout**: Tracks `failedLoginAttempts` and `isLocked` on User entity

---

## 4. Functional Modules

### 4.1 Customer Management

**Entities:** `Customer`, `CustomerMaster`, `CustomerTaxProfile`

**Functionality:**
- **Customer Onboarding**: Create customer with KYC details (name, DOB, national ID, phone, email, address)
- **KYC Lifecycle**: PENDING -> VERIFIED (approval) or REJECTED
- **Maker-Checker**: Customer creation records the `createdBy` user; approval/rejection requires a different user
- **CBS-Grade CustomerMaster**: Extended customer entity with customer number, home branch, freeze level, customer type (INDIVIDUAL/CORPORATE), and full maker-checker audit trail (maker, checker, timestamps)
- **Tax Profile**: One-to-one linked `CustomerTaxProfile` for tax compliance
- **Search**: By name, KYC status, tenant-isolated queries

**UI Pages:**
- Customer list with search and filters
- Customer create/edit forms
- Customer detail view with KYC status and audit info

### 4.2 Account Management

**Entity:** `Account`

**Functionality:**
- **Account Opening**: Generate unique account number, link to tenant/branch/customer
- **Account Types**: Supports multiple types via `AccountType` enum (SAVINGS, CURRENT, etc.)
- **Account Status**: ACTIVE, FROZEN, DORMANT, CLOSED via `AccountStatus` enum
- **Freeze Controls**: Granular `FreezeLevel` enum -- NONE, DEBIT_ONLY, CREDIT_ONLY, FULL
- **Approval Workflow**: Accounts start as PENDING (`MakerCheckerStatus`); require a different user to approve before transactions are allowed
- **Balance Tracking**: Real-time balance with precision (19, 4) in configurable currency (default INR)
- **Hierarchical Accounts**: Parent-child relationship for Chart of Accounts structure
- **Ledger Account Type**: Classification via `LedgerAccountType` enum
- **GL Linkage**: Each account links to a General Ledger account code
- **Branch Association**: Accounts belong to a branch (home branch for CBS-grade isolation)
- **Optimistic Locking**: `@Version` field prevents concurrent modification

**UI Pages:**
- Account list with status/type filters
- Account create/edit forms
- Account detail view with freeze history, lien history, audit info

### 4.3 Transaction Processing

**Entity:** `Transaction`

**Functionality:**
- **Deposit**: Credit to customer account, debit to cash GL; creates two vouchers
- **Withdrawal**: Debit from customer account, credit to cash GL; insufficient balance check
- **Transfer**: Debit source, credit destination; same-account check, balance validation
- **Pessimistic Locking**: `SELECT ... FOR UPDATE` on accounts during transactions
- **Idempotency**: Composite key (client_reference_id, channel, tenant_id) prevents duplicate processing
- **Batch Processing**: Transactions grouped into `TransactionBatch` by channel/tenant/date
- **Channel-Aware**: `TransactionChannel` enum (TELLER, ATM, ONLINE, MOBILE, API, SYSTEM, BRANCH)
- **Transaction Reference**: Auto-generated unique refs (DEP-xxx, WDR-xxx, TRF-xxx)
- **Reversal Support**: Link to original transaction via `reversalOf` reference
- **Business Date Validation**: Transactions only allowed when tenant day status is OPEN
- **Holiday Calendar Enforcement**: Server-side validation via `BankCalendarService` before processing
- **Freeze Level Enforcement**: Server-side check blocks transactions on frozen accounts (FULL, DEBIT_ONLY, CREDIT_ONLY)
- **Approval Status Check**: Blocks transactions on accounts not yet approved (PENDING/REJECTED)

**UI Pages:**
- Deposit/Withdraw/Transfer forms with account lookup
- Transaction list with filters
- Transaction detail view with voucher and ledger entry links

### 4.4 Voucher System

**Entity:** `Voucher`

**Functionality:**
- **Voucher Lifecycle**: Create -> Authorize -> Post -> (optional Cancel via reversal)
- **Maker-Checker**: Every voucher has a `maker` (creator) and `checker` (authorizer); same user cannot be both
- **System Auto-Authorization**: `systemAuthorizeVoucher()` for straight-through processing (teller transactions) with `[SYSTEM_AUTO_AUTHORIZED]` audit narration
- **Composite Index**: (tenant_id, branch_id, posting_date, batch_code, scroll_no)
- **Scroll Number**: Sequential numbering within batch for audit trail
- **Debit/Credit Indicator**: `VoucherDrCr` enum (DR/CR)
- **Date Fields**: entry_date, posting_date, value_date, effective_date
- **Cancel Flag**: Soft-delete via cancel flag; cancelled vouchers cannot be authorized
- **Financial Effect Flag**: Controls whether voucher impacts financial balances
- **GL Account Link**: Each voucher linked to a General Ledger account
- **Reversal Reference**: Cancelled vouchers reference the original via `reversalOfVoucher`
- **Immutability**: No physical delete; corrections via reversal entries only

**UI Pages:**
- Voucher create form
- Pending vouchers (awaiting authorization)
- Posted vouchers
- Cancelled vouchers
- Voucher inquiry with filters

### 4.5 Ledger & Double-Entry Accounting

**Entities:** `LedgerJournal`, `LedgerEntry`

**Functionality:**
- **Double-Entry Invariant**: Every transaction creates matching debit and credit entries; SUM(debits) = SUM(credits) enforced before posting
- **Immutable Entries**: `LedgerEntry` is append-only with no update/delete capability; corrections via reversal entries
- **Journal Grouping**: Entries grouped under `LedgerJournal` linked to a `Transaction`
- **Balance Tracking**: Each entry records `balanceAfter` for running balance
- **GL Integration**: Entries linked to General Ledger accounts for aggregate reporting
- **Scheduled Validation**: `LedgerValidatorService` runs every 5 minutes:
  - Checks debit/credit balance per transaction
  - Validates account balance consistency
  - Detects orphan entries (entries without transaction linkage)
  - Verifies immutability
- **On-Demand Full Validation**: Available via controller endpoint

**UI Pages:**
- Ledger explorer with account/date filters
- Ledger validation status dashboard

### 4.6 General Ledger (Chart of Accounts)

**Entity:** `GeneralLedger`

**Functionality:**
- **GL Account Types**: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE via `GLAccountType` enum
- **Hierarchical Structure**: Parent-child GL accounts with level tracking
- **Balance Tracking**: Running balance per GL account (precision 19, 4)
- **Normal Balance**: Configurable debit/credit normal balance per account type
- **Active/Inactive**: Soft deactivation of GL accounts
- **Tenant-Aware**: Each GL account belongs to a tenant

**UI Pages:**
- GL hierarchy view (tree structure)
- GL create/edit forms
- GL account detail view

### 4.7 End-of-Day (EOD) Processing

**Service:** `EodValidationService`

**Functionality:**
- **Pre-EOD Validation**:
  - All vouchers must be authorized
  - All vouchers must be posted
  - SUM(debits) = SUM(credits) across all entries for the day
  - No pending approval requests
  - Tenant GL balanced
- **Day Close**: Transitions tenant `dayStatus` from OPEN -> DAY_CLOSING -> OPEN
- **Date Advance**: Increments `currentBusinessDate` to next business date
- **Blocks on Failures**: EOD will not proceed if any validation fails

**UI Pages:**
- EOD validation dashboard
- EOD run trigger
- EOD status view

### 4.8 Settlement Processing

**Entities:** `Settlement`, `SettlementEntry`

**Functionality:**
- **Settlement Reference**: Unique reference per settlement run
- **Status Lifecycle**: PENDING -> PROCESSING -> COMPLETED / FAILED via `SettlementStatus` enum
- **Transaction Aggregation**: Counts and groups transactions for settlement
- **Timing**: Records start_time and end_time for performance tracking
- **User Attribution**: Records `processedBy` user
- **Metrics**: Settlement duration tracked via Micrometer timer

**UI Pages:**
- Settlement dashboard with summary
- Settlement process trigger
- Settlement list and detail views

### 4.9 Bank Calendar (Holiday Management)

**Entity:** `BankCalendar`

**Functionality:**
- **Day Classification**: Each date per tenant classified as WORKING_DAY or HOLIDAY
- **Holiday Types**: Named holidays with holiday type categorization
- **Channel Controls**: Configurable `atmAllowed` and `systemTransactionsAllowed` per holiday
- **Maker-Checker**: Calendar entries require approval (PENDING -> APPROVED/REJECTED)
- **No Backdating**: Cannot edit calendar entries after EOD for that date
- **Transaction Enforcement**: `BankCalendarService.validateTransactionAllowed()` blocks transactions on holidays (server-side)

**UI Pages:**
- Calendar list view
- Calendar entry create form

### 4.10 Lien Management

**Entity:** `AccountLien`

**Functionality:**
- **Lien Types**: Via `LienType` enum (e.g., COURT_ORDER, GUARANTEE, etc.)
- **Balance Impact**: Lien amount reduces available balance for withdrawals
- **Lien Lifecycle**: ACTIVE -> RELEASED/EXPIRED via `LienStatus` enum
- **Maker-Checker**: Lien creation/release requires approval
- **Date Range**: Start date and optional end date for automatic expiry
- **Reference Tracking**: External lien reference for audit linkage

**UI Pages:**
- Lien list view
- Account-specific lien view

### 4.11 Account Ownership

**Entity:** `AccountOwnership`

**Functionality:**
- **Ownership Types**: PRIMARY, JOINT, GUARANTOR, NOMINEE via `OwnershipType` enum
- **Percentage Tracking**: Ownership percentage (default 100%) with precision (5, 2)
- **Operational Flag**: Whether the owner has operational rights on the account
- **Maker-Checker**: Ownership changes require approval
- **Customer-Account Linkage**: Links `CustomerMaster` to `Account` with proper audit trail

**UI Pages:**
- Ownership list view
- Account-specific and customer-specific ownership views

### 4.12 Approval (Maker-Checker) Framework

**Entity:** `ApprovalRequest`

**Functionality:**
- **Generic Approval Engine**: Supports any entity type via `entityType` + `entityId`
- **Request Data Storage**: Stores approval request payload (up to 4000 chars)
- **Status Lifecycle**: PENDING -> APPROVED / REJECTED via `ApprovalStatus` enum
- **Self-Approval Prevention**: System blocks same user from creating and approving the same request
- **Remarks**: Approval/rejection reason tracking
- **Tenant-Aware**: Approval requests scoped to tenant

**UI Pages:**
- Approval queue (pending approvals)
- Approval detail view with approve/reject actions

### 4.13 Payment Engine

**Entity:** `PaymentInstruction`

**Functionality:**
- **Payment Lifecycle**: INITIATED -> PROCESSING -> COMPLETED / FAILED / REVERSED via `PaymentStatus` enum
- **Idempotency**: Unique `idempotencyKey` prevents duplicate payment processing
- **Transaction Linkage**: Each completed payment links to a `Transaction`
- **Source/Destination Accounts**: Explicit account references for payment routing
- **Multi-Currency**: Configurable currency per payment instruction

### 4.14 Branch Management

**Entity:** `Branch`

**Functionality:**
- **Branch Registry**: Code, name, address, active/inactive status
- **Account Association**: Accounts link to branches for operational isolation
- **User Assignment**: Users assigned to branches for access control

**UI Pages:**
- Branch list (admin view)

### 4.15 Currency & Exchange Rates

**Entity:** `ExchangeRate`

**Functionality:**
- **Multi-Currency Support**: Currency pair rate management
- **Date-Effective Rates**: Exchange rates effective from a specific date
- **Precision**: Rate precision (19, 8) for accuracy
- **Default Currency**: INR (Indian Rupee) throughout the application

### 4.16 Idempotency Service

**Entity:** `IdempotencyKey`

**Functionality:**
- **Duplicate Prevention**: Stores processed idempotency keys with results
- **Tenant + Channel Scoping**: Keys scoped to (client_reference_id, channel, tenant_id)
- **Automatic Checking**: Transaction service checks for existing keys before processing

### 4.17 Audit Trail

**Entity:** `AuditLog`

**Functionality:**
- **Comprehensive Logging**: Records all significant operations (creates, updates, approvals, rejections)
- **User Attribution**: Each audit entry linked to the performing user
- **Entity Tracking**: Records entity type and entity ID for traceability
- **Event Description**: Detailed narration of what occurred
- **Timestamp**: Precise datetime of each event

**UI Pages:**
- Audit log viewer (admin view)
- Audit validation dashboard

### 4.18 Dashboard

**Service:** `DashboardService`

**Functionality:**
- **Summary Statistics**: Total accounts, transactions, balances
- **Today's Activity**: Transaction count and volume for current business date
- **Status Distribution**: Account status breakdown (ACTIVE, FROZEN, etc.)

**UI Pages:**
- Main dashboard with widgets and summary cards

### 4.19 Reporting

**Service:** `ReportingService`

**Functionality:**
- **Trial Balance**: Debit/credit totals across all GL accounts
- **Account Statement**: Transaction history per account with running balance
- **Daily Summary**: Day's transaction aggregation by type
- **Liquidity Report**: Available balance analysis across GL accounts

**UI Pages:**
- Report selection page
- Trial balance report
- Account statement report
- Daily summary report
- Liquidity report

### 4.20 Observability

**Components:** `LedgoraHealthIndicator`, `LedgoraMetricsConfig`

**Functionality:**
- **Health Checks**: Custom health indicator for application health
- **Metrics** (via Micrometer/Prometheus):
  - `ledgora.transactions.total` -- Total transactions processed
  - `ledgora.transactions.deposits` -- Deposit count
  - `ledgora.transactions.withdrawals` -- Withdrawal count
  - `ledgora.transactions.transfers` -- Transfer count
  - `ledgora.ledger.posting.duration` -- Ledger posting latency
  - `ledgora.settlement.duration` -- Settlement processing time
  - `ledgora.ledger.entries.total` -- Total ledger entries
  - `ledgora.settlements.total` -- Settlement count
  - `ledgora.settlements.failures` -- Failed settlement count
- **Actuator Endpoints**: health, info, metrics, prometheus exposed

### 4.21 Validation Services

**Components:** `LedgerValidatorService`, `CbsBalanceEngine`, `CbsCustomerValidationService`

**Functionality:**
- **Scheduled Ledger Validation**: Every 5 minutes, validates debit/credit balance, account consistency, orphan entries
- **Balance Engine**: Real-time available balance calculation (account balance minus active liens)
- **Customer Validation**: Customer-level freeze checks before transactions

---

## 5. Data Model Summary

### Core Entities (20+)

| Entity | Table | Key Fields |
|--------|-------|------------|
| Tenant | tenants | tenantCode, currentBusinessDate, dayStatus |
| User | users | username, password, tenant, branch, roles |
| Role | (user_roles join) | ManyToMany with User |
| Customer | customers | firstName, lastName, nationalId, kycStatus, tenant |
| CustomerMaster | customer_master | customerNumber, status, freezeLevel, makerCheckerStatus |
| CustomerTaxProfile | (linked to CustomerMaster) | Tax compliance data |
| Account | accounts | accountNumber, accountType, status, balance, freezeLevel, approvalStatus |
| Transaction | transactions | transactionRef, type, status, amount, channel, clientReferenceId |
| TransactionBatch | transaction_batches | batchType, batchCode, businessDate, status, totals |
| Voucher | vouchers | batchCode, scrollNo, drCr, maker, checker, authFlag, postFlag |
| LedgerJournal | ledger_journals | transaction, businessDate, entries |
| LedgerEntry | ledger_entries | account, glAccount, entryType, amount, balanceAfter |
| GeneralLedger | general_ledgers | glCode, glName, accountType, balance, parent |
| Settlement | settlements | settlementRef, businessDate, status, transactionCount |
| SettlementEntry | (linked to Settlement) | Individual settlement items |
| BankCalendar | bank_calendar | calendarDate, dayType, holidayName, approvalStatus |
| AccountLien | account_liens | account, lienAmount, lienType, status, approvalStatus |
| AccountOwnership | account_ownership | account, customerMaster, ownershipType, percentage |
| ApprovalRequest | approval_requests | entityType, entityId, requestedBy, approvedBy, status |
| PaymentInstruction | payment_instructions | sourceAccount, destAccount, amount, status, idempotencyKey |
| ExchangeRate | exchange_rates | currencyFrom, currencyTo, rate, effectiveDate |
| IdempotencyKey | (idempotency tracking) | clientRefId, channel, tenant |
| AuditLog | (audit_logs) | userId, eventType, entityType, entityId, description |
| Branch | branches | branchCode, name, address, isActive |

### Key Enums (25)

| Enum | Values |
|------|--------|
| AccountStatus | ACTIVE, FROZEN, DORMANT, CLOSED |
| AccountType | SAVINGS, CURRENT, (etc.) |
| FreezeLevel | NONE, DEBIT_ONLY, CREDIT_ONLY, FULL |
| MakerCheckerStatus | PENDING, APPROVED, REJECTED |
| DayStatus | OPEN, CLOSED, (DAY_CLOSING) |
| TransactionType | DEPOSIT, WITHDRAWAL, TRANSFER |
| TransactionStatus | PENDING, COMPLETED, FAILED, REVERSED |
| TransactionChannel | TELLER, ATM, ONLINE, MOBILE, API, SYSTEM, BRANCH |
| VoucherDrCr | DR, CR |
| BatchStatus | OPEN, CLOSED |
| BatchType | (per channel type) |
| ApprovalStatus | PENDING, APPROVED, REJECTED |
| CustomerStatus | ACTIVE, INACTIVE, SUSPENDED, CLOSED |
| SettlementStatus | PENDING, PROCESSING, COMPLETED, FAILED |
| PaymentStatus | INITIATED, PROCESSING, COMPLETED, FAILED, REVERSED |
| LienStatus | ACTIVE, RELEASED, EXPIRED |
| LienType | COURT_ORDER, GUARANTEE, (etc.) |
| OwnershipType | PRIMARY, JOINT, GUARANTOR, NOMINEE |
| GLAccountType | ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE |
| LedgerAccountType | (account classification) |
| EntryType | DEBIT, CREDIT |
| RoleName | (user roles) |
| TenantScope | SINGLE, MULTI |
| BusinessDateStatus | (business date states) |
| RelationshipType | (customer relationships) |

---

## 6. Security Controls

| Control | Implementation |
|---------|---------------|
| **Authentication** | Spring Security form login + JWT tokens |
| **Authorization** | Role-based access (User <-> Role ManyToMany) |
| **CSRF Protection** | CookieCsrfTokenRepository on all endpoints (H2 console exempted in dev) |
| **Password Storage** | BCrypt hashing |
| **Account Lockout** | Failed login attempt tracking + isLocked flag |
| **Maker-Checker** | Enforced on: Vouchers, Customers, Accounts, Calendar, Liens, Ownership |
| **Tenant Isolation** | TenantContextHolder (ThreadLocal) + tenant-filtered queries |
| **Pessimistic Locking** | SELECT FOR UPDATE on accounts during transactions |
| **Optimistic Locking** | @Version on Account entity |
| **Idempotency** | Composite key deduplication for transactions |
| **Immutability** | Ledger entries append-only; voucher cancel via reversal only |
| **Business Day Control** | DayStatus + BusinessDayInterceptor blocks POSTs when closed |
| **Holiday Enforcement** | BankCalendarService server-side validation |
| **Freeze Enforcement** | Account-level FreezeLevel checked server-side in all transaction flows |

---

## 7. UI Architecture

### View Technology
- **JSP (JavaServer Pages)** with JSTL tags
- **Layout**: Common base layout (header, sidebar, footer) with content areas
- **CSS/JS**: Static resources served from `/resources/` and `classpath:/static/`

### Page Inventory (65+ JSP pages)

| Module | Pages |
|--------|-------|
| **Layout** | base, header, sidebar, footer, pagination, lookup-modal, status-banner, audit-info |
| **Auth** | login, register |
| **Dashboard** | dashboard |
| **Customer** | list, create, edit, view |
| **Account** | list, create, edit, view |
| **Transaction** | deposit, withdraw, transfer, list, history, view |
| **Voucher** | create, pending, posted, cancelled, inquiry |
| **GL** | hierarchy, create, edit, view |
| **Ledger** | explorer |
| **EOD** | validate, run, status |
| **Settlement** | dashboard, process, list, view |
| **Calendar** | list, create |
| **Lien** | list, account-view |
| **Ownership** | list, account-view, customer-view |
| **Approval** | list, view |
| **Reporting** | reports, trial-balance, account-statement, daily-summary, liquidity |
| **Batch** | dashboard |
| **Tax** | profile-create, profiles |
| **Validation** | ledger-status |
| **Admin** | users, branches, tenants, audit |
| **Audit** | audit-validation |
| **Error** | error |

### UI Features
- Account lookup modal for transaction forms
- Status banners (holiday warnings, business day status)
- Audit info sections on entity detail pages
- Pagination support
- Flash messages for success/error feedback

---

## 8. Event-Driven Architecture

The application uses Spring's event system for decoupled processing:

| Event | Trigger | Purpose |
|-------|---------|---------|
| `AccountCreatedEvent` | Account creation | Post-creation processing |
| `TransactionCreatedEvent` | Transaction completion | Notifications, audit |
| `LedgerPostedEvent` | Ledger entry posted | Balance cache updates, metrics |
| `SettlementCompletedEvent` | Settlement completion | Reporting, notifications |

Events are handled by listeners in `com.ledgora.events.listener`.

---

## 9. Transaction Flow (End-to-End)

```
1. User submits deposit/withdraw/transfer via JSP form (POST with CSRF token)
2. TransactionController validates parameters and calls TransactionService
3. TransactionService:
   a. Validates tenant business day is OPEN
   b. Acquires pessimistic lock on account(s)
   c. Validates account is ACTIVE and APPROVED
   d. Validates FreezeLevel allows the operation (DR/CR)
   e. Validates holiday calendar allows transaction
   f. Checks idempotency (no duplicate processing)
   g. Gets/creates TransactionBatch for channel/date
   h. Creates Transaction entity
   i. Resolves branch and GL accounts
   j. Creates Voucher(s) (DR + CR) via VoucherService
   k. System-auto-authorizes vouchers (with audit trail)
   l. Posts vouchers -> creates LedgerJournal + LedgerEntry
   m. Updates account balance(s)
   n. Publishes TransactionCreatedEvent
   o. Logs audit trail
4. Response rendered to user with transaction confirmation
```

---

## 10. EOD Flow

```
1. Operator triggers EOD validation
2. EodValidationService checks:
   - All vouchers authorized (no pending)
   - All vouchers posted (no unposted)
   - SUM(debits) = SUM(credits) for the day
   - No pending approval requests
   - Tenant GL balanced
3. If all validations pass, operator triggers EOD run
4. TenantService:
   - Sets dayStatus = DAY_CLOSING
   - Closes all open batches
   - Advances currentBusinessDate to next date
   - Sets dayStatus = OPEN
5. New business day begins
```

---

## 11. Database Indexes

The application uses strategic indexing for performance:

- **Composite indexes** on vouchers (tenant, branch, posting_date, batch_code, scroll_no)
- **Tenant indexes** on all major entities for tenant-filtered queries
- **Account number indexes** for fast lookups
- **Transaction reference indexes** for quick retrieval
- **Idempotency indexes** (client_reference_id, channel, tenant_id)
- **Calendar indexes** (tenant, date) with unique constraint
- **Status indexes** on approval, lien, and batch tables

---

## 12. Deployment

### Development
- H2 in-memory database (auto-created on startup)
- `hibernate.ddl-auto=update` for automatic schema management
- H2 Console at `/h2-console`

### Production
- WAR packaging for deployment to external Tomcat or embedded Tomcat
- Microsoft SQL Server as production database
- Configure via `application.properties` or environment variables
- Actuator endpoints for monitoring:
  - `/actuator/health` -- Application health
  - `/actuator/metrics` -- Micrometer metrics
  - `/actuator/prometheus` -- Prometheus scrape endpoint

---

## 13. Regulatory Compliance Features

| Requirement | Implementation |
|------------|---------------|
| **Double-Entry Accounting** | Every transaction creates balanced debit/credit entries |
| **Audit Trail** | Comprehensive AuditLog for all operations; maker-checker timestamps |
| **Maker-Checker** | Two-user approval for customers, accounts, vouchers, calendar, liens, ownership |
| **Ledger Immutability** | No update/delete on ledger entries; corrections via reversals only |
| **Holiday Calendar** | Bank calendar with approval workflow; server-side transaction blocking |
| **Account Freeze** | Granular freeze levels (debit-only, credit-only, full) enforced server-side |
| **EOD Controls** | Mandatory validation before day close; balanced ledger requirement |
| **Tenant Isolation** | All queries filtered by tenant; ThreadLocal context management |
| **CSRF Protection** | Cookie-based CSRF tokens on all financial operations |
| **Lien Management** | Balance reduction for legal holds with approval workflow |

---

*Document generated: March 2026*
*Platform Version: 0.0.1-SNAPSHOT*
*Branch: cbs-enhancement_08mar_26*
