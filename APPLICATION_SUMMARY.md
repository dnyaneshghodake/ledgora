# Ledgora CBS — Application Summary

## Executive Summary

Ledgora is a **production-grade Core Banking System (CBS)** built on Spring Boot 3.2, designed for Indian banking operations with full RBI regulatory compliance. The system implements enterprise-grade double-entry bookkeeping, multi-tenant isolation, maker-checker governance, inter-branch clearing, real-time fraud detection, and crash-safe end-of-day processing.

Ledgora targets Tier-1 CBS capabilities comparable to Finacle (Infosys) and FinnOne (Nucleus Software), providing a modern Java-based alternative with clean architecture, comprehensive test coverage, and production-ready seed data.

---

## CBS Capabilities

| Capability | Status | Description |
|-----------|--------|-------------|
| Double-Entry Bookkeeping | Implemented | SUM(DR)=SUM(CR) enforced at journal and EOD level |
| Voucher-Driven Posting | Implemented | Create → Authorize → Post → Cancel lifecycle |
| Multi-Tenant Isolation | Implemented | Complete data isolation per tenant via TenantContextHolder |
| Maker-Checker Workflow | Implemented | Segregation of duties enforced at voucher authorization |
| Inter-Branch Transfer | Implemented | 4-voucher model with clearing GL settlement |
| Suspense Management | Implemented | Automated parking and resolution with EOD enforcement |
| Fraud Detection | Implemented | Velocity count, amount threshold, hard ceiling alerts |
| EOD Processing | Implemented | Crash-safe state machine with phase-by-phase recovery |
| Teller Operations | Implemented | Cash denomination tracking, session management, limits |
| GL Hierarchy | Implemented | 5-level chart of accounts (Asset/Liability/Equity/Revenue/Expense) |
| Customer Master | Implemented | KYC, tax profiles, 360-degree view |
| Batch Processing | Implemented | Transaction batching with settlement lifecycle |
| Idempotency | Implemented | Duplicate transaction prevention via clientReferenceId+channel |
| Audit Trail | Implemented | Immutable audit log with SHA-256 hash chain |
| RBAC | Implemented | 15 CBS roles with RBI-compliant segregation constraints |
| Loan Module | Implemented | Disbursement, EMI, NPA classification (90-day DPD), IRAC provisioning, write-off |
| Deposit Module | Implemented | CASA + FD + RD with daily interest accrual, premature closure, CRR/SLR flags |
| Financial Statements | Implemented | Balance Sheet (Schedule 5) + P&L (Schedule 14) from immutable ledger entries |
| Regulatory Reporting | Implemented | Trial Balance, CRAR (Basel III, 9% min), ALM (8-bucket structural liquidity) |
| Regulatory Dashboard | Implemented | Finacle-grade UI with snapshot-only rendering, RBAC, SHA-256 checksums |

---

## Compliance Mapping (RBI)

| RBI Directive | Ledgora Implementation |
|--------------|----------------------|
| **RBI Master Direction on KYC** | CustomerMaster entity with KYC status tracking, Compliance Officer role (ROLE_COMPLIANCE_OFFICER) for AML/CFT oversight |
| **RBI IT Framework** | Crash-safe EOD state machine, immutable audit trail, business continuity via phase-by-phase commits |
| **RBI Fraud Risk Management** | FraudAlert entity (immutable), velocity monitoring, hard ceiling enforcement, ROLE_RISK for risk dashboard access |
| **RBI Basel III / ICAAP** | CRAR Engine (9% minimum), Tier 1/Tier 2 capital computation, RWA standardised approach, PCA warning |
| **RBI IRAC Norms** | Loan NPA classification at 90-day DPD, provisioning (0.4%/15%/25%/100%), income recognition stop for NPA |
| **RBI ALM Guidelines** | 8-bucket structural liquidity statement, gap ratio analysis, -15% risk threshold |
| **RBI Master Directions on Deposits** | CASA quarterly compounding, FD/RD daily accrual, premature closure penalty, CRR/SLR eligibility |
| **RBI Schedule 5 (Balance Sheet)** | GL-driven balance sheet with A=L+E validation, SHA-256 snapshot checksums |
| **RBI Schedule 14 (P&L)** | Period-based P&L (daily/monthly/FY April-March), revenue/expense from ledger entries only |
| **RBI Cyber Security Framework** | JWT-based authentication, role-based access control, tenant isolation, no credential exposure |
| **RBI Guidelines on Outsourcing** | Multi-tenant architecture allows independent tenant operations with isolated data |
| **RBI Master Direction on Digital Payment Security** | Idempotency enforcement, transaction reference tracking, channel-based duplicate prevention |

---

## Finacle-Level Features

| Finacle Feature | Ledgora Equivalent |
|----------------|-------------------|
| **TRXNMAST** (Transaction Master) | `Transaction` entity with full lifecycle tracking |
| **VOUCHER** (Voucher Register) | `Voucher` entity with auth_flag/post_flag/cancel_flag |
| **GLBALANCE** (GL Balance) | `GeneralLedger` + `CbsGlBalanceService` |
| **ACCTBALANCE** (Account Balance) | `AccountBalance` with shadow/actual/available/ledger splits |
| **IBC Clearing** | `InterBranchClearingService` + `IbtService` with 4-voucher model |
| **SUSPENSE** (Suspense Register) | `SuspenseCase` entity with OPEN/RESOLVED/REVERSED lifecycle |
| **EOD Batch** | `EodStateMachineService` with crash-safe phase progression |
| **TELLER** (Teller Module) | `TellerMaster` + `TellerSession` + `CashDenominationMaster` |
| **SCROLL SEQUENCE** | `ScrollSequence` entity with PESSIMISTIC_WRITE locking |
| **MAKER-CHECKER** | Enforced at `VoucherService.authorizeVoucher()` — maker != checker |
| **CUSTOMER 360** | `CustomerMaster` with linked accounts, fraud alerts, and risk summary |
| **LOAN MODULE** | `LoanProduct` + `LoanAccount` with disbursement, EMI, NPA, provisioning, write-off |
| **DEPOSIT MODULE** | `DepositProduct` + `DepositAccount` — CASA/FD/RD with interest accrual engine |
| **BALANCE SHEET** | `BalanceSheetEngine` — GL-derived, A=L+E validated, SHA-256 snapshot |
| **P&L STATEMENT** | `PnlEngine` — daily/monthly/FY (April-March), revenue/expense from ledger |
| **TRIAL BALANCE** | `TrialBalanceEngine` — date-aware, DR=CR validation, contra balance handling |
| **CRAR ENGINE** | `CrarEngine` — Basel III Tier1/Tier2, RWA standardised approach, 9% minimum |
| **ALM MODULE** | `AlmEngine` — 8-bucket structural liquidity, gap ratio, risk threshold |
| **REGULATORY DASHBOARD** | Snapshot-only rendering, RBAC, admin regeneration, audit checksums |

---

## Data Model Summary

### Core Entities (30+)

```
Tenant (1) ──< Branch (N) ──< Account (N) ──< AccountBalance (1)
   |                |              |
   |                |              +──< Transaction (N) ──< TransactionLine (N)
   |                |              |
   |                |              +──< Voucher (N) ──< LedgerEntry (N)
   |                |
   |                +──< TellerMaster (N) ──< TellerSession (N)
   |                |
   |                +──< VaultMaster (1)
   |
   +──< GeneralLedger (N) [Hierarchy: parent-child]
   |
   +──< CustomerMaster (N) ──< CustomerTaxProfile (1)
   |
   +──< EodProcess (N) [per business date]
   |
   +──< FraudAlert (N)
   |
   +──< SuspenseCase (N)
   |
   +──< TransactionBatch (N)
   |
   +──< ApprovalRequest (N)
```

### Key Relationships
- **Tenant** is the root isolation boundary; every entity references a tenant
- **Account** links to Branch (home branch), CustomerMaster, and GL code
- **Transaction** links to source/destination accounts, batch, maker, and checker
- **Voucher** links to Transaction, Account, GL, Branch, Maker, and Checker
- **LedgerEntry** is immutable and links to Journal, Transaction, Account, and GL
- **EodProcess** is unique per (tenant, business_date) with optimistic locking

---

## Deployment Architecture

### Development
```
Developer Machine
  +-- Spring Boot (embedded Tomcat)
  +-- H2 In-Memory Database
  +-- DataInitializer auto-seeds on startup
```

### Production
```
Application Server(s)
  +-- Spring Boot JAR (Java 17)
  +-- LEDGORA_SEEDER_ENABLED=false
  +-- JWT secret via environment variable
  |
  +-- SQL Server 2019+
  |     +-- ledgora database
  |     +-- Hibernate DDL: validate (no auto-create)
  |     +-- Seed data loaded via sqlcmd
  |
  +-- Load Balancer (optional)
  +-- Monitoring (SLF4J/Logback → ELK/Splunk)
```

### Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| `LEDGORA_SEEDER_ENABLED` | Enable/disable DataInitializer | `true` |
| `SPRING_DATASOURCE_URL` | Database connection URL | H2 in-memory |
| `SPRING_DATASOURCE_USERNAME` | Database username | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | - |
| `JWT_SECRET` | JWT signing secret | - |

---

## Operational Governance Model

### Transaction Governance
1. **Maker creates** transaction → Voucher(s) created with auth_flag=N
2. **Checker authorizes** → auth_flag=Y (maker != checker enforced)
3. **System posts** → post_flag=Y, ledger entries created (immutable)
4. **EOD validates** → SUM(DR)=SUM(CR), no pending vouchers, clearing settled

### Day Lifecycle
1. **Day opens** (DayStatus=OPEN) → Transactions allowed
2. **Day closing initiated** (DayStatus=DAY_CLOSING) → No new transactions
3. **EOD validation** → All checks must pass
4. **EOD execution** → Phase-by-phase with crash recovery
5. **Date advanced** → Business date incremented, DayStatus=OPEN

### Batch Governance
- Batches are auto-created per channel per business date
- Batch status: OPEN → CLOSED → SETTLED
- Closed batches cannot be modified
- Batch totals must match ledger totals at EOD

---

## Risk Controls

| Control | Implementation |
|---------|---------------|
| **Double-entry enforcement** | SUM(DR)=SUM(CR) validated per journal and at EOD |
| **Maker-Checker** | Voucher authorization requires different user from maker |
| **Velocity monitoring** | Transaction count/amount per time window tracked |
| **Hard ceiling** | Maximum transaction amount enforced |
| **Suspense management** | Failed legs parked to suspense GL; must resolve before EOD |
| **Clearing validation** | IBC clearing GL must net to zero at EOD |
| **Business date control** | All postings locked to tenant business date |
| **Optimistic locking** | @Version on Account, EodProcess prevents concurrent modification |
| **Tenant isolation** | TenantContextHolder prevents cross-tenant data access |
| **Idempotency** | Duplicate clientReferenceId+channel rejected |

---

## Audit Model

Ledgora maintains an **immutable audit trail** for all financial operations:

- **AuditLog** entity records: user, action, entity type, entity ID, details, timestamp
- Events audited: Voucher creation, authorization, posting, cancellation; Transaction creation; EOD execution; Fraud alerts; Suspense resolution
- Audit logs are append-only (no UPDATE/DELETE)
- Accessible via ROLE_AUDITOR (read-only)

---

## Performance Considerations

| Area | Approach |
|------|----------|
| **Scroll sequence** | PESSIMISTIC_WRITE lock prevents duplicate voucher numbers under concurrency |
| **Balance updates** | Shadow balance on create (fast), actual balance on post (authoritative) |
| **Batch processing** | Transactions grouped into batches for efficient settlement |
| **Tenant isolation** | TenantContextHolder avoids expensive per-query tenant joins |
| **Optimistic locking** | @Version fields prevent lost updates without pessimistic locks |
| **Index strategy** | Targeted indexes on tenant_id, business_date, status, account_id |
| **Lazy loading** | FetchType.LAZY on all @ManyToOne to prevent N+1 queries |
| **Pagination** | Spring Data pageable on all list endpoints |

---

*Ledgora CBS — Enterprise-grade core banking for RBI-compliant Indian financial institutions.*
