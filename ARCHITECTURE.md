# Ledgora Core Banking Platform - Architecture Documentation

## Overview

Ledgora is a monolithic Spring Boot core banking application implementing real banking architecture patterns while maintaining simplicity through a single deployable unit.

## Package Structure (Part 8)

```
com.ledgora/
├── account/          # Account management module
│   ├── controller/   # AccountController (MVC)
│   ├── service/      # AccountService
│   ├── repository/   # AccountRepository, AccountBalanceRepository
│   ├── entity/       # Account, AccountBalance
│   └── dto/          # AccountDTO
├── auth/             # Authentication & authorization
│   ├── controller/   # AuthController (login, register)
│   ├── service/      # AuthService
│   ├── repository/   # UserRepository, RoleRepository
│   ├── entity/       # User, Role
│   └── dto/          # LoginRequest, RegisterRequest
├── transaction/      # Transaction processing
│   ├── controller/   # TransactionController
│   ├── service/      # TransactionService
│   ├── repository/   # TransactionRepository, TransactionLineRepository
│   ├── entity/       # Transaction, TransactionLine
│   └── dto/          # TransactionDTO
├── ledger/           # Double-entry ledger
│   ├── entity/       # LedgerEntry (immutable)
│   └── repository/   # LedgerEntryRepository
├── gl/               # General Ledger / Chart of Accounts
│   ├── controller/   # GeneralLedgerController
│   ├── service/      # GeneralLedgerService
│   ├── repository/   # GeneralLedgerRepository
│   ├── entity/       # GeneralLedger
│   └── dto/          # GeneralLedgerDTO
├── settlement/       # End-of-day settlement
│   ├── controller/   # SettlementController
│   ├── service/      # SettlementService (7-step EOD flow)
│   ├── repository/   # SettlementRepository, SettlementEntryRepository
│   └── entity/       # Settlement, SettlementEntry
├── audit/            # Audit logging
│   ├── service/      # AuditService
│   ├── repository/   # AuditLogRepository
│   └── entity/       # AuditLog
├── branch/           # Branch management
│   ├── service/      # BranchService
│   ├── repository/   # BranchRepository
│   └── entity/       # Branch
├── dashboard/        # Dashboard
│   ├── controller/   # DashboardController
│   ├── service/      # DashboardService
│   └── dto/          # DashboardDTO
├── common/           # Shared utilities
│   ├── entity/       # SystemDate
│   ├── repository/   # SystemDateRepository
│   ├── service/      # BusinessDateService
│   └── enums/        # AccountStatus, AccountType, EntryType, etc.
├── config/           # Configuration
│   ├── SecurityConfig.java
│   ├── DataInitializer.java
│   └── WebMvcConfig.java
└── security/         # Security components
    ├── CustomUserDetailsService.java
    ├── JwtTokenProvider.java
    ├── JwtAuthenticationFilter.java
    └── CustomAuthenticationSuccessHandler.java
```

## Transaction Architecture (Part 1)

Transaction processing follows a three-layer flow:

```
Transaction (business event)
    → Transaction Lines (debit/credit breakdown)
        → Ledger Entries (immutable double-entry accounting)
```

Every transaction creates at minimum two transaction lines (one debit, one credit), maintaining the fundamental accounting invariant:

```
SUM(debit amounts) = SUM(credit amounts)
```

## Ledger Schema (Part 2)

Ledger entries are **immutable** - they cannot be updated or deleted. Corrections are handled through reversal transactions linked via `reversal_transaction_id`.

Each ledger entry includes:
- `transaction_id` - link to source transaction
- `account_id` - the customer account affected
- `gl_account_id` / `gl_account_code` - the GL account for accounting
- `entry_type` - DEBIT or CREDIT
- `amount` - the monetary amount
- `balance_after` - running balance after this entry
- `business_date` - accounting date (not system timestamp)
- `posting_time` - actual posting timestamp

## Account Balance Cache (Part 3)

The `account_balances` table provides a denormalized view of account balances:

- `ledger_balance` - derived from ledger entries
- `available_balance` - ledger_balance minus holds
- `hold_amount` - funds under hold

Balance updates occur in the **same database transaction** as ledger entry creation, ensuring consistency.

## Business Date (Part 4)

The `system_dates` table tracks the current banking business date, independent of system clock:

- Status: `OPEN` (accepting transactions), `DAY_CLOSING` (EOD in progress), `CLOSED`
- All transactions use the business date for accounting
- Business date advances only through the settlement process

## End-of-Day Settlement (Part 5)

The EOD process follows a strict 7-step flow:

1. **Stop transactions** - Set business date to `DAY_CLOSING`
2. **Post pending transactions** - Complete any pending transactions
3. **Validate ledger integrity** - Verify `SUM(debits) = SUM(credits)`
4. **Recalculate balances** - Recompute all account balances from ledger
5. **Post interest/fees** - Placeholder for interest accrual
6. **Create settlement records** - Per-account settlement entries with opening/closing balances
7. **Advance business date** - Close current date, open next business date

## Audit Logging (Part 6)

All significant operations are audit-logged:
- User login
- Account creation
- Transactions (deposit, withdrawal, transfer)
- Settlement execution

Audit logs use `REQUIRES_NEW` propagation to ensure they persist even if the parent transaction rolls back.

## Branch Structure (Part 7)

Branches are initialized at startup (HQ001, BR001, BR002). Both accounts and users reference a branch code for organizational hierarchy.

## Transaction Safety (Part 9)

All financial operations use `@Transactional` with the following processing order:

1. Validate accounts (status, balance)
2. Create transaction record
3. Create transaction lines (debit + credit)
4. Create ledger entries
5. Update account balances (main + cache)
6. Commit (automatic via Spring)

If any step fails, the entire transaction rolls back.

## UI Compatibility (Part 10)

All existing JSP pages continue to work:
- `/login` - Login page
- `/dashboard` - Dashboard with summary statistics
- `/accounts` - Account list, create, view, edit
- `/transactions` - Transaction list, deposit, withdrawal, transfer, history
- `/gl` - General ledger hierarchy
- `/settlements` - Settlement list, process
- `/admin/users` - User management

## Database

- **Development**: H2 in-memory database
- **Production**: SQL Server compatible schema
- **Migration**: SQL files in `src/main/resources/db/migration/`

## Technology Stack

- Java 17
- Spring Boot 3.2.3
- Spring Security with JWT
- Spring Data JPA (Hibernate)
- JSP + JSTL frontend
- Lombok
- H2 Database (dev) / SQL Server (prod)
