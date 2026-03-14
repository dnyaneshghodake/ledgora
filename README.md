# Ledgora Core Banking System (CBS)

A production-grade, multi-tenant Core Banking System built with Spring Boot, implementing RBI-compliant double-entry bookkeeping, maker-checker workflows, inter-branch clearing, fraud detection, and crash-safe EOD processing.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Module Breakdown](#module-breakdown)
4. [Accounting Engine](#accounting-engine)
5. [Inter-Branch Transfer (IBT)](#inter-branch-transfer-ibt)
6. [Suspense Lifecycle](#suspense-lifecycle)
7. [Fraud Detection](#fraud-detection)
8. [EOD Lifecycle](#eod-lifecycle)
9. [Security Model](#security-model)
10. [How to Run](#how-to-run)
11. [How to Load Seed Data](#how-to-load-seed-data)
12. [How to Execute Test Suite](#how-to-execute-test-suite)
13. [Folder Structure](#folder-structure)
14. [Technology Stack](#technology-stack)
15. [Roadmap](#roadmap)

---

## 1. Overview

Ledgora is a **Tier-1 Core Banking System** designed for Indian banking operations with full RBI compliance. It supports:

- **Multi-tenancy** with complete tenant isolation (data, GL, business date)
- **Double-entry bookkeeping** with ledger immutability (SUM(DR) = SUM(CR) always)
- **Maker-Checker authorization** for all financial transactions
- **Inter-Branch Transfer (IBT)** using the 4-voucher clearing model
- **Suspense account management** with automated resolution
- **Fraud detection** with velocity and amount threshold monitoring
- **Crash-safe EOD** state machine with phase-by-phase recovery
- **Teller operations** with denomination tracking and cash limits

## 2. Architecture

```
                    +------------------+
                    |   REST API Layer |
                    |  (Controllers)   |
                    +--------+---------+
                             |
                    +--------v---------+
                    |  Service Layer   |
                    |  (Business Logic)|
                    +--------+---------+
                             |
              +--------------+---------------+
              |              |               |
     +--------v-----+ +-----v------+ +------v-------+
     | Voucher Engine| | Ledger     | | Balance      |
     | (Auth+Post)   | | (Immutable)| | (Shadow+Real)|
     +--------------+ +------------+ +--------------+
              |              |               |
              +--------------+---------------+
                             |
                    +--------v---------+
                    |   JPA / Hibernate|
                    |   Repositories   |
                    +--------+---------+
                             |
                    +--------v---------+
                    | SQL Server 2019+ |
                    | (H2 for tests)   |
                    +------------------+
```

**Key Architectural Decisions:**
- **Voucher-driven posting**: Every financial movement goes through a Voucher (create -> authorize -> post)
- **Ledger immutability**: No UPDATE/DELETE on ledger entries; reversals create new contra entries
- **Tenant isolation**: Every entity is scoped to a tenant; TenantContextHolder enforces isolation
- **Shadow vs Actual balance**: Shadow balance updates on voucher creation; actual balance on posting

## 3. Module Breakdown

| Module | Package | Description |
|--------|---------|-------------|
| **Auth** | `com.ledgora.auth` | Users, Roles, JWT authentication, RBAC |
| **Tenant** | `com.ledgora.tenant` | Multi-tenant management, business date, day status |
| **Branch** | `com.ledgora.branch` | Branch hierarchy, branch-level operations |
| **Account** | `com.ledgora.account` | Customer accounts, GL accounts, balance tracking |
| **GL** | `com.ledgora.gl` | General Ledger hierarchy, GL balance service |
| **Transaction** | `com.ledgora.transaction` | Deposit, Withdrawal, Transfer orchestration |
| **Voucher** | `com.ledgora.voucher` | Voucher lifecycle (create/auth/post/cancel) |
| **Ledger** | `com.ledgora.ledger` | Immutable ledger journals and entries |
| **Batch** | `com.ledgora.batch` | Transaction batch management and settlement |
| **Clearing** | `com.ledgora.clearing` | Inter-branch clearing and IBT processing |
| **Suspense** | `com.ledgora.suspense` | Suspense case tracking and resolution |
| **Fraud** | `com.ledgora.fraud` | Fraud alerts, velocity monitoring |
| **EOD** | `com.ledgora.eod` | End-of-day validation and state machine |
| **Teller** | `com.ledgora.teller` | Teller master, sessions, denomination tracking |
| **Customer** | `com.ledgora.customer` | Customer master, KYC, tax profiles |
| **Approval** | `com.ledgora.approval` | Maker-checker approval workflow |
| **Audit** | `com.ledgora.audit` | Immutable audit trail |
| **Idempotency** | `com.ledgora.idempotency` | Duplicate transaction prevention |
| **Balance** | `com.ledgora.balance` | CBS balance engine (shadow + actual) |

## 4. Accounting Engine

Ledgora implements a **full double-entry bookkeeping engine** compliant with RBI accounting standards:

### Transaction Chain
```
Transaction -> Batch -> Voucher(DR) + Voucher(CR) -> LedgerJournal -> LedgerEntry(DR) + LedgerEntry(CR)
```

### Key Invariants
- **SUM(DR) = SUM(CR)** for every journal (enforced at service and DB level)
- **Voucher lifecycle**: Created (auth_flag=N) -> Authorized (auth_flag=Y) -> Posted (post_flag=Y)
- **Balance model**: Shadow balance updates on voucher creation; actual balance on posting
- **GL impact**: Every posting updates the corresponding GL account balance
- **Immutability**: Ledger entries are never modified; reversals create new contra entries

### GL Account Types
- **Asset** (Normal: DEBIT) - Cash, Loans, Fixed Assets
- **Liability** (Normal: CREDIT) - Customer Deposits, Borrowings
- **Equity** (Normal: CREDIT) - Share Capital, Retained Earnings
- **Revenue** (Normal: CREDIT) - Interest Income, Fee Income
- **Expense** (Normal: DEBIT) - Interest Expense, Operating Expenses

## 5. Inter-Branch Transfer (IBT)

Ledgora implements the **4-voucher IBT model** used by Finacle and other Tier-1 CBS platforms:

```
Source Branch:
  V1: DR Customer Account (debit sender)
  V2: CR IBC_OUT GL (credit clearing outward)

Destination Branch:
  V3: DR IBC_IN GL (debit clearing inward)
  V4: CR Customer Account (credit receiver)

Post-Settlement: IBC_OUT + IBC_IN = 0 (clearing GL nets to zero)
```

**Settlement lifecycle**: INITIATED -> IN_PROGRESS -> SETTLED (or FAILED)

## 6. Suspense Lifecycle

When a credit leg fails after a debit succeeds (e.g., target account frozen), the system parks the failed leg to a **Suspense GL** to maintain double-entry balance:

```
Normal Flow:
  DR Source -> CR Destination

Failure (credit leg):
  DR Source -> CR Suspense GL (parking)

Resolution:
  DR Suspense GL -> CR Destination (retry success)
  OR
  CR Source -> DR Suspense GL (full reversal)

Status: OPEN -> RESOLVED (or REVERSED)
```

**EOD Constraint**: Suspense GL must net to zero before EOD can proceed.

## 7. Fraud Detection

Ledgora monitors transactions in real-time against configurable thresholds:

| Alert Type | Description | Example |
|------------|-------------|---------|
| `VELOCITY_COUNT` | Too many transactions in time window | 6 txns in 30 min (limit: 5) |
| `VELOCITY_AMOUNT` | Cumulative amount exceeds threshold | 2M in 1 day (limit: 1M) |
| `HARD_CEILING` | Single transaction exceeds maximum | 50M deposit (limit: 10M) |
| `RAPID_REVERSAL` | Suspicious reversal pattern | Multiple reversals in short window |

Alerts are **immutable records** (never deleted) and follow the lifecycle: `OPEN -> ACKNOWLEDGED -> RESOLVED / FALSE_POSITIVE`.

## 8. EOD Lifecycle

The End-of-Day process uses a **crash-safe state machine** with independent phase commits:

```
VALIDATED -> DAY_CLOSING -> BATCH_CLOSED -> SETTLED -> DATE_ADVANCED
```

### EOD Validation Checks (must ALL pass before proceeding):
1. All vouchers authorized (auth_flag = Y)
2. No approved-but-unposted vouchers
3. SUM(DR) = SUM(CR) for the business date
4. Voucher totals balanced
5. No pending approval requests
6. No pending transactions
7. Inter-branch clearing settled
8. Clearing GL net = 0
9. Suspense GL net = 0
10. Tenant GL balanced

### Recovery
On crash/restart, the incomplete EOD is detected and **resumed from the last successful phase**. Double execution is prevented via unique constraint on `(tenant_id, business_date)`.

## 9. Security Model

### Role-Based Access Control (RBAC)

| Role | Description | Key Constraints |
|------|-------------|-----------------|
| `ROLE_SUPER_ADMIN` | Platform-level administration | Full access |
| `ROLE_ADMIN` | Tenant administration | Tenant-scoped |
| `ROLE_TENANT_ADMIN` | Tenant configuration | Cannot post vouchers |
| `ROLE_MANAGER` | Branch management | Approval authority |
| `ROLE_TELLER` | Cash transactions | Cannot authorize own vouchers |
| `ROLE_MAKER` | Transaction creation | Cannot be checker for own txns |
| `ROLE_CHECKER` | Transaction authorization | Cannot be maker for same txn |
| `ROLE_AUDITOR` | Read-only audit access | No write operations |
| `ROLE_OPERATIONS` | Operational tasks | Cannot modify financial data |
| `ROLE_RISK` | Risk monitoring | View fraud flags, no voucher posting |
| `ROLE_SYSTEM` | STP auto-checker | Cannot login via UI |
| `ROLE_COMPLIANCE_OFFICER` | AML/CFT oversight | STR reporting |

### Segregation of Duties
- **Maker != Checker**: Enforced at voucher authorization
- **Teller cannot authorize**: Must go through checker
- **Auditor read-only**: No write permissions
- **Risk**: View-only for fraud dashboards

## 10. How to Run

### Prerequisites
- Java 17+
- Maven 3.8+
- SQL Server 2019+ (for production) or H2 (for development/testing)

### Development Mode (H2)
```bash
# Clone the repository
git clone https://github.com/dnyaneshghodake/ledgora.git
cd ledgora

# Build and run with default profile (H2 in-memory DB)
mvn spring-boot:run
```

The application will start at `http://localhost:8080` with H2 console at `http://localhost:8080/h2-console`.

### Production Mode (SQL Server)
```bash
# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=ledgora
export SPRING_DATASOURCE_USERNAME=sa
export SPRING_DATASOURCE_PASSWORD=your_password
export LEDGORA_SEEDER_ENABLED=false

# Build and run
mvn clean package -DskipTests
java -jar target/ledgora-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 11. How to Load Seed Data

### Automatic Seeding (Development)
The `DataInitializer` runs on startup when `ledgora.seeder.enabled=true` (default). It seeds:
- Tenants (TENANT-001, TENANT-002)
- Roles (all 15 CBS roles)
- Branches (HQ001, BR001, BR002)
- Users (admin, managers, tellers, maker, checker, risk, compliance, auditor)
- GL Chart of Accounts (full hierarchy)
- Customer accounts with balances
- Sample transactions with full ledger chains
- Exchange rates, idempotency keys, teller master data

### Manual SQL Seeding (Production)
```bash
# Load role matrix
sqlcmd -S localhost -d ledgora -i role-matrix-seed.sql

# Load full production seed data
sqlcmd -S localhost -d ledgora -i prod-tier1-seed-data.sql
```

### Disable Seeding
Set `ledgora.seeder.enabled=false` in `application.properties` or via environment variable `LEDGORA_SEEDER_ENABLED=false`.

## 12. How to Execute Test Suite

```bash
# Run all tests
mvn test

# Run integration tests only
mvn test -Dtest="com.ledgora.integration.*"

# Run specific test class
mvn test -Dtest="com.ledgora.integration.DoubleEntryEnforcementTest"

# Run with verbose output
mvn test -Dtest="com.ledgora.integration.*" -Dsurefire.useFile=false

# Run full build with tests
mvn verify
```

### Test Categories
- **Integration tests** (`com.ledgora.integration.*`): Double-entry, maker-checker, IBT, suspense, EOD, idempotency, fraud, RBAC, business date, ledger immutability
- **Banking invariant tests** (`LedgoraBankingInvariantTest`): Comprehensive banking rule validation
- **Voucher lifecycle tests** (`LedgoraVoucherLifecycleTest`): Voucher create/auth/post/cancel
- **CBS balance tests** (`LedgoraCbsBalanceTest`): Shadow vs actual balance
- **EOD validation tests** (`LedgoraEodValidationTest`): EOD state machine and validation
- **Tenant isolation tests** (`TenantIsolationTest`): Multi-tenant data isolation

## 13. Folder Structure

```
ledgora/
+-- src/
|   +-- main/
|   |   +-- java/com/ledgora/
|   |   |   +-- account/          # Account entities, repos, services
|   |   |   +-- approval/         # Maker-checker approval workflow
|   |   |   +-- audit/            # Immutable audit trail
|   |   |   +-- auth/             # Authentication, JWT, users, roles
|   |   |   +-- balance/          # CBS balance engine
|   |   |   +-- batch/            # Transaction batch management
|   |   |   +-- branch/           # Branch hierarchy
|   |   |   +-- clearing/         # Inter-branch clearing (IBT)
|   |   |   +-- common/           # Shared enums, exceptions, DTOs
|   |   |   +-- config/           # Spring config, DataInitializer, seeders
|   |   |   +-- customer/         # Customer master, KYC, tax
|   |   |   +-- eod/              # End-of-day processing
|   |   |   +-- fraud/            # Fraud detection and alerts
|   |   |   +-- gl/               # General Ledger hierarchy
|   |   |   +-- idempotency/      # Duplicate prevention
|   |   |   +-- ledger/           # Immutable ledger journals/entries
|   |   |   +-- suspense/         # Suspense case management
|   |   |   +-- teller/           # Teller operations
|   |   |   +-- tenant/           # Multi-tenant management
|   |   |   +-- transaction/      # Transaction orchestration
|   |   |   +-- voucher/          # Voucher lifecycle
|   |   +-- resources/
|   |       +-- application.properties
|   +-- test/
|       +-- java/com/ledgora/
|       |   +-- integration/      # Module-level integration tests
|       |   +-- ...               # Other test classes
|       +-- resources/
|           +-- application-test.properties
+-- role-matrix-seed.sql          # CBS role matrix SQL
+-- prod-tier1-seed-data.sql      # Production seed data SQL
+-- prod-seed-data.sql            # Base seed data SQL
+-- README.md
+-- APPLICATION_SUMMARY.md
+-- END_TO_END_FLOW.md
+-- pom.xml
```

## 14. Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.2.3 |
| ORM | Hibernate / JPA | 6.4.x |
| Database (Prod) | Microsoft SQL Server | 2019+ |
| Database (Test) | H2 | In-memory |
| Security | Spring Security + JWT | 6.2.x |
| Build | Maven | 3.8+ |
| Code Quality | Spotless (Google Java Format) | Integrated |
| Testing | JUnit 5 + Spring Boot Test | 5.10.x |
| Logging | SLF4J + Logback | Included |

## 15. Roadmap

### Completed
- Multi-tenant CBS with full double-entry bookkeeping
- Voucher-driven posting (create/authorize/post/cancel)
- Inter-branch transfer (4-voucher model)
- Suspense account lifecycle
- Fraud detection (velocity + amount thresholds)
- Crash-safe EOD state machine
- Teller operations with denomination tracking
- Role matrix with RBI-compliant segregation of duties
- Comprehensive integration test suite
- Production seed data

### Planned
- [ ] Real-time transaction monitoring dashboard
- [ ] Automated interest calculation batch
- [ ] Loan origination and servicing module
- [ ] NEFT/RTGS/IMPS integration adapters
- [ ] Mobile banking API layer
- [ ] KYC document management
- [ ] Regulatory reporting (RBI returns)
- [ ] Multi-currency support with FX engine
- [ ] High-availability deployment with clustering
- [ ] Performance benchmarking and optimization

---

*Ledgora CBS - RBI-compliant, production-grade core banking for Indian financial institutions.*
