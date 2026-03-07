# Ledgora Fintech Ledger Architecture

## Overview

Ledgora is a monolithic core banking platform built with Spring Boot 3.x that implements modern fintech ledger architecture patterns used by high-scale payment systems. The system focuses on ledger correctness, idempotency, event-driven internal design, auditability, and scalability.

---

## 1. Ledger Model (System of Record)

### Principles
- **Ledger entries are the ultimate source of financial truth**
- Ledger entries are **immutable** -- they must never be updated or deleted
- Every financial operation creates a **ledger journal** with 2+ **ledger entries**
- The system enforces **double-entry accounting**: `SUM(debits) = SUM(credits)`

### Data Model

```
ledger_journals
├── id (PK)
├── transaction_id (FK -> transactions)
├── description
├── business_date
└── created_at

ledger_entries
├── id (PK)
├── journal_id (FK -> ledger_journals)
├── transaction_id (FK -> transactions)
├── account_id (FK -> accounts)
├── gl_account_id (FK -> general_ledger)
├── gl_account_code
├── entry_type (DEBIT / CREDIT)
├── amount
├── balance_after
├── currency
├── business_date
├── posting_time
├── narration
└── created_at
```

### Flow
1. `TransactionService` creates a `Transaction` record
2. `LedgerService.postJournal()` validates `SUM(debits) = SUM(credits)`
3. Creates 1 `LedgerJournal` + 2+ `LedgerEntry` records
4. Publishes `LedgerPostedEvent`

---

## 2. Account Architecture

### Account Types

| Type | Purpose |
|------|---------|
| `SAVINGS` | Customer savings account |
| `CURRENT` | Customer current/checking account |
| `LOAN` | Loan account |
| `FIXED_DEPOSIT` | Fixed deposit account |
| `CUSTOMER_ACCOUNT` | Generic customer ledger account |
| `GL_ACCOUNT` | General ledger account |
| `INTERNAL_ACCOUNT` | Internal/system account |
| `CLEARING_ACCOUNT` | Clearing/suspense account |
| `SETTLEMENT_ACCOUNT` | Settlement account |

### Hierarchical Relationships

Accounts support parent-child relationships for Chart of Accounts structure:

```
Assets (GL_ACCOUNT)
├── Cash (GL_ACCOUNT)
│   └── ATM Cash (GL_ACCOUNT)
└── Loans (GL_ACCOUNT)

Liabilities (GL_ACCOUNT)
├── Customer Deposits (GL_ACCOUNT)
│   ├── Savings Accounts (GL_ACCOUNT)
│   └── Current Accounts (GL_ACCOUNT)
└── Other Liabilities (GL_ACCOUNT)
```

---

## 3. Event-Driven Architecture

### Domain Events

| Event | Publisher | Listeners |
|-------|-----------|-----------|
| `TransactionCreatedEvent` | `TransactionService` | `LedgerEventListener` |
| `LedgerPostedEvent` | `LedgerService` | `BalanceEventListener` |
| `SettlementCompletedEvent` | `SettlementService` | `BalanceEventListener` |
| `AccountCreatedEvent` | `AccountService` | `BalanceEventListener` |

### Event Flow

```
TransactionService
  → publish TransactionCreatedEvent
  → LedgerEventListener receives
  → (Ledger entries already created synchronously)
  → LedgerService publishes LedgerPostedEvent
  → BalanceEventListener updates balance cache
```

### Implementation
- Uses Spring's `ApplicationEventPublisher` for in-process event publishing
- Events extend `ApplicationEvent` for type safety
- Listeners use `@EventListener` annotation
- All events are processed within the same JVM (monolith)

---

## 4. Idempotency Strategy

### Design
Every financial operation can carry an **idempotency key** to ensure at-most-once processing.

### Data Model

```
idempotency_keys
├── id (PK)
├── idempotency_key (UNIQUE)
├── request_hash (SHA-256)
├── response_hash (SHA-256)
├── response_body
├── status (PROCESSING / COMPLETED / FAILED)
├── created_at
└── expires_at
```

### Flow
1. Client sends request with `Idempotency-Key` header
2. `IdempotencyService.checkExisting()` looks up the key
3. If found and COMPLETED: return cached response
4. If not found: `registerKey()` in `REQUIRES_NEW` transaction
5. Process the financial operation
6. `completeKey()` stores the response hash and body
7. On failure: `failKey()` marks as FAILED

### Key Features
- SHA-256 hashing for request/response comparison
- `REQUIRES_NEW` propagation ensures key registration survives parent rollback
- Automatic expiration support

---

## 5. Multi-Currency Design

### Exchange Rates

```
exchange_rates
├── id (PK)
├── currency_from (e.g., "USD")
├── currency_to (e.g., "INR")
├── rate (decimal, 8 precision)
├── effective_date
└── created_at
```

### Currency Conversion
- `CurrencyConversionService` handles all FX operations
- Lookups are by currency pair + effective date (most recent rate)
- Inverse rates are calculated automatically if direct rate unavailable
- All accounts, transactions, and ledger entries carry a `currency` field

### Cross-Currency Transactions
When a transaction involves different currencies:
1. Source amount debited in source currency
2. FX conversion applied using current exchange rate
3. Destination amount credited in destination currency
4. FX gain/loss captured in dedicated GL entries

---

## 6. Balance Engine

### Real-Time Balance Calculation

```
Available Balance = Snapshot Balance + Ledger Entries (after snapshot) - Holds
```

### Components

1. **Ledger Snapshots** (`ledger_snapshots` table)
   - Point-in-time balance per account
   - Contains `snapshot_balance`, `snapshot_date`, `last_entry_id`
   - Created periodically for performance optimization

2. **Balance Engine** (`BalanceEngine` service)
   - `getLedgerBalance()`: snapshot + delta entries
   - `getHoldAmount()`: sum of active holds
   - `getAvailableBalance()`: ledger balance minus holds
   - `refreshBalanceCache()`: updates `AccountBalance` entity

3. **Account Balance Cache** (`account_balances` table)
   - Cached `ledger_balance`, `available_balance`, `hold_amount`
   - Updated on every transaction and settlement

---

## 7. Payment Instruction Engine

### State Machine

```
INITIATED → AUTHORIZED → SETTLED → (complete)
    ↓            ↓
  FAILED       FAILED
```

### Data Model

```
payment_instructions
├── id (PK)
├── source_account (FK)
├── destination_account (FK)
├── amount
├── currency
├── status (INITIATED / AUTHORIZED / SETTLED / FAILED)
├── idempotency_key
├── transaction_id (FK, set when settled)
├── failure_reason
├── created_at
└── updated_at
```

### Flow
1. `createPayment()` → status = INITIATED
2. `authorizePayment()` → status = AUTHORIZED (with validation)
3. `settlePayment()` → creates transaction via `TransactionService.transfer()` → status = SETTLED

---

## 8. Settlement Process

### 7-Step Settlement Flow

| Step | Action | Details |
|------|--------|---------|
| 1 | **Stop transaction intake** | Set business date status to `DAY_CLOSING` |
| 2 | **Flush pending events** | Complete all pending transactions |
| 3 | **Validate ledger integrity** | Verify `SUM(debits) = SUM(credits)` |
| 4 | **Generate trial balance** | Run `ReportingService.generateTrialBalance()` |
| 5 | **Post accruals and fees** | Interest accruals, maintenance fees (extensible) |
| 6 | **Generate settlement reports** | Create `SettlementEntry` per account |
| 7 | **Advance business date** | Move to next business date |

### Invariant Validation
- Ledger debits must equal credits for the settlement date
- Trial balance must be balanced
- All pending transactions must be resolved before advancement

---

## 9. Financial Reporting

### Available Reports

| Report | Description | Source |
|--------|-------------|--------|
| **Trial Balance** | Debit/credit totals by GL account | `ledger_entries` grouped by GL |
| **General Ledger** | Transaction history for a GL account | `ledger_entries` by GL code |
| **Account Statement** | Transaction history for a customer account | `ledger_entries` by account |
| **Daily Transaction Summary** | Aggregated daily activity | `transactions` + `ledger_entries` |
| **Liquidity Report** | Asset/liability/liquidity ratios | `ledger_entries` by GL type |

All reports read directly from `ledger_entries` (system of record).

---

## 10. Audit Trail

### Fields Captured

| Field | Description |
|-------|-------------|
| `user_id` | ID of the user performing the action |
| `username` | Username for readability |
| `action` | Action type (DEPOSIT, WITHDRAWAL, etc.) |
| `entity` | Entity type (TRANSACTION, ACCOUNT, etc.) |
| `entity_id` | ID of the affected entity |
| `details` | Human-readable description |
| `ip_address` | Client IP address |
| `request_payload` | Full request body (up to 4000 chars) |
| `response_payload` | Full response body (up to 4000 chars) |
| `http_method` | HTTP method (GET, POST, etc.) |
| `request_uri` | Request URI path |
| `timestamp` | When the action occurred |

### Transaction Safety
- Audit logs use `@Transactional(propagation = Propagation.REQUIRES_NEW)`
- This ensures audit records persist even if the parent transaction rolls back

---

## 11. Observability

### Metrics (via Micrometer + Prometheus)

| Metric | Type | Description |
|--------|------|-------------|
| `ledgora.transactions.total` | Counter | Total transactions processed |
| `ledgora.transactions.deposits` | Counter | Deposit count |
| `ledgora.transactions.withdrawals` | Counter | Withdrawal count |
| `ledgora.transactions.transfers` | Counter | Transfer count |
| `ledgora.ledger.posting.duration` | Timer | Ledger posting latency |
| `ledgora.settlement.duration` | Timer | Settlement processing time |
| `ledgora.ledger.entries.total` | Counter | Total ledger entries |
| `ledgora.settlements.total` | Counter | Total settlements |
| `ledgora.settlements.failures` | Counter | Failed settlements |

### Health Checks

Custom `LedgoraHealthIndicator` checks:
- Business date status
- Ledger integrity (debits = credits for current date)
- Total transaction count

### Endpoints
- `/actuator/health` - Health status with ledger details
- `/actuator/metrics` - All application metrics
- `/actuator/prometheus` - Prometheus-format metrics export

---

## 12. Performance Optimizations

### Database Indexes

| Table | Index | Columns |
|-------|-------|---------|
| `ledger_entries` | `idx_ledger_entry_account_created` | `account_id, created_at` |
| `ledger_entries` | `idx_ledger_entry_journal` | `journal_id` |
| `transactions` | `idx_transaction_ref` | `transaction_ref` |
| `accounts` | `idx_account_number` | `account_number` |
| `idempotency_keys` | `idx_idempotency_key` | `idempotency_key` |
| `exchange_rates` | `idx_exchange_rate_pair_date` | `currency_from, currency_to, effective_date` |
| `ledger_snapshots` | `idx_snapshot_account_date` | `account_id, snapshot_date` |

### Snapshot-Based Balance Optimization
Instead of scanning all ledger entries for balance calculation:
1. Use latest snapshot as starting point
2. Only aggregate entries after the snapshot's `last_entry_id`
3. Result: `O(delta)` instead of `O(n)` for balance queries

### Pessimistic Locking (PART 8)
- `@Lock(LockModeType.PESSIMISTIC_WRITE)` on account lookups during transactions
- Prevents race conditions on concurrent balance updates
- Applied via `findByAccountNumberWithLock()` repository methods

---

## 13. Customer Module

### Entity: Customer
- `customer_id`, `first_name`, `last_name`, `dob`, `national_id`, `kyc_status`, `phone`, `email`, `address`, `created_at`
- Links multiple accounts to one customer via `customer_id` foreign key on `Account`
- KYC status tracking: `PENDING`, `VERIFIED`, `REJECTED`

### Endpoints
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/customers` | GET | List all customers (with search/KYC filter) |
| `/customers/create` | GET/POST | Create new customer |
| `/customers/{id}` | GET | View customer details |
| `/customers/{id}/edit` | GET/POST | Edit customer |
| `/customers/{id}/kyc` | POST | Update KYC status |

---

## 14. Transaction Channels & Idempotency

### Channels
Enum: `TELLER`, `ATM`, `ONLINE`, `MOBILE`, `BATCH`

### Idempotency
- `client_reference_id` + `channel` uniqueness prevents duplicate transactions
- Checked before transaction creation via `TransactionRepository` and `IdempotencyService`
- Composite index: `idx_txn_client_ref_channel` on `(client_reference_id, channel)`

---

## 15. Maker-Checker / Approval Workflow

### Entity: ApprovalRequest
- `entity_type`, `entity_id`, `request_data`, `requested_by`, `approved_by`, `status`, `remarks`, `created_at`, `approved_at`
- Status: `PENDING`, `APPROVED`, `REJECTED`

### Rules
- High-value transactions require manager approval before execution
- Maker-checker violation prevention: user cannot approve their own request
- Audit logging on all approval actions

### Endpoints
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/approvals` | GET | List approvals (with status filter) |
| `/approvals/pending` | GET | List pending approvals |
| `/approvals/{id}` | GET | View approval details |
| `/approvals/{id}/approve` | POST | Approve request |
| `/approvals/{id}/reject` | POST | Reject request |

---

## 16. Continuous Ledger Validator

### Scheduled Validation
- Partial validation every 5 minutes (debit/credit balance check)
- Full validation during EOD settlement

### Checks
1. `SUM(debits) = SUM(credits)` per transaction
2. Account balances match ledger-derived balances
3. No orphan ledger entries
4. Ledger immutability preserved

### Admin Endpoints
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/admin/ledger/validate` | GET | Run full validation (REST) |
| `/admin/ledger/status` | GET | Get last validation status (REST) |
| `/admin/ledger/view` | GET | View validation status (JSP) |
| `/admin/ledger/view/validate` | GET | Run and view validation (JSP) |

### Status Values
- `HEALTHY` - All checks passed
- `WARNING` - Minor discrepancies detected
- `CORRUPTED` - Critical integrity violations found

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| Security | Spring Security + JWT |
| Data | Spring Data JPA |
| Frontend | JSP |
| Dev DB | H2 (in-memory) |
| Prod DB | SQL Server |
| Metrics | Micrometer + Prometheus |
| Health | Spring Boot Actuator |
| Build | Maven |
