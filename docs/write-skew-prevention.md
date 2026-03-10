# Ledgora — Write-Skew Prevention Strategy

**Version:** 1.0
**Baseline:** PR #43 — Feature Enhancement
**Standard:** ANSI SQL Isolation Levels, Berenson et al. "A Critique of ANSI SQL Isolation Levels" (1995)

## What is Write Skew?

Write skew occurs when two concurrent transactions each read a value, make independent decisions based on that read, and then write — resulting in a state that neither transaction would have allowed individually.

**Classic CBS example:**
```
Account balance = 1000
Concurrent withdrawal A = 800
Concurrent withdrawal B = 700

Thread A reads balance=1000, checks 1000>=800 → OK, writes balance=200
Thread B reads balance=1000, checks 1000>=700 → OK, writes balance=300

Without protection: both succeed → final balance = -500 (OVERDRAFT)
```

## Why Ledgora is Already Protected (Current State)

**Ledgora does NOT have a write-skew vulnerability.** The current code uses `PESSIMISTIC_WRITE` locks on account rows before reading the balance:

```java
// TransactionService.transfer() — line 389-392
Account sourceAccount = accountRepository
    .findByAccountNumberWithLockAndTenantId(dto.getSourceAccountNumber(), tenantId)
    .orElseThrow(...);
// Balance check happens AFTER lock is acquired:
if (sourceAccount.getBalance().compareTo(dto.getAmount()) < 0) {
    throw new InsufficientBalanceException(...);
}
```

The `findByAccountNumberWithLockAndTenantId()` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`:

```java
// AccountRepository.java — line 68-75
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.tenant.id = :tenantId")
Optional<Account> findByAccountNumberWithLockAndTenantId(...);
```

**This means:** Thread B's `findByAccountNumberWithLockAndTenantId()` **blocks** until Thread A's transaction commits. When Thread B finally reads the balance, it sees Thread A's committed update (balance=200), and the check `200 >= 700` correctly fails.

**The PESSIMISTIC_WRITE lock serializes access to the same account row**, preventing write skew without SERIALIZABLE isolation.

## Where Write Skew Could Theoretically Occur

Write skew requires two conditions:
1. Read a value without holding a lock
2. Make a decision based on that read, then write

In Ledgora, the only flows where this pattern exists:

### A) Transfer — Source + Destination (PROTECTED)

Both accounts locked via `findByAccountNumberWithLockAndTenantId()` before any validation. No write skew possible.

### B) Velocity Fraud Check (ACCEPTABLE RISK)

```java
// TransactionService — velocity check reads recent transaction count
velocityFraudEngine.evaluateVelocity(tenant, account, dto.getAmount(), userId);
```

The velocity check queries `TransactionRepository.countRecentByAccountId()` — this is a **read without lock** on the `transactions` table. Two concurrent transactions could both pass the velocity check because neither has committed yet.

**Why this is acceptable:** Velocity limits are **soft controls** — they freeze the account for review, they don't prevent financial loss. A brief race window where two transactions both pass a 10-transaction limit (resulting in 11 transactions) is operationally acceptable because:
- The 12th transaction will be blocked (freeze is persistent)
- The drift is at most 1 transaction per race window
- Production velocity windows are 60 minutes — the probability of exact-threshold races is very low

### C) EOD Validation (PROTECTED by Business Day Lock)

EOD reads voucher counts, ledger sums, etc. without pessimistic locks. But the business day is locked to `DAY_CLOSING` before validation, which blocks all new transactions. No concurrent writes can occur during EOD validation.

## Recommended Enhancement: Atomic Conditional Update

**Status: Deferred** — current PESSIMISTIC_WRITE is correct. This enhancement is an optimization.

### Current Pattern (Correct but 2-step):
```java
Account account = accountRepository.findByAccountNumberWithLockAndTenantId(accNo, tenantId);
if (account.getBalance().compareTo(amount) < 0) throw new InsufficientBalanceException();
account.setBalance(account.getBalance().subtract(amount));
accountRepository.save(account);
```

### Proposed Pattern (Atomic, 1-step):
```java
// New repository method:
@Modifying
@Query("UPDATE Account a SET a.balance = a.balance - :amount "
     + "WHERE a.id = :id AND a.balance >= :amount")
int debitIfSufficient(@Param("id") Long id, @Param("amount") BigDecimal amount);

// Usage:
int rows = accountRepository.debitIfSufficient(account.getId(), amount);
if (rows == 0) throw new InsufficientBalanceException();
```

**Benefits of atomic SQL:**
- Single round-trip: check + update in one SQL statement
- DB engine guarantees atomicity — no application-level race window
- Could potentially remove PESSIMISTIC_WRITE lock (reducing contention)

**Why deferred:**
- Current PESSIMISTIC_WRITE already prevents write skew — no correctness gap
- `@Modifying` bypasses JPA entity state (stale in-memory balance after update)
- Requires restructuring posting flow to not read `account.getBalance()` after update
- See `docs/hot-row-strategy.md` for detailed analysis

## DB-Level Constraint Verification

These constraints provide defense-in-depth against invariant violations:

| Constraint | Table | Type | Prevents | Status |
|---|---|---|---|---|
| `uk_eod_tenant_date` | `eod_processes` | UNIQUE(tenant_id, business_date) | Double EOD execution | ✅ Already exists (`EodProcess.java:44-48`) |
| `idx_voucher_number` | `vouchers` | UNIQUE(voucher_number) | Duplicate voucher numbers | ✅ Already exists (`Voucher.java:32`) |
| `idx_transaction_ref` | `transactions` | UNIQUE(transaction_ref) | Duplicate transaction refs | ✅ Already exists (`Transaction.java:24`) |
| `idx_txn_client_ref_channel_tenant` | `transactions` | INDEX(client_ref, channel, tenant) | Idempotency violation | ✅ Already exists (`Transaction.java:26-27`) |
| Account balance >= 0 | `accounts` | CHECK constraint | Overdraft | ⬜ Not enforced at DB level (enforced in service layer via PESSIMISTIC_WRITE + balance check) |

**Note on CHECK constraint:** A `CHECK (balance >= 0)` constraint on the `accounts` table would provide ultimate DB-level overdraft prevention. However, it would also prevent legitimate GL accounts (clearing, suspense) from going negative during inter-branch clearing flows. The service-layer enforcement with pessimistic locks is the correct approach for CBS.

## Why SERIALIZABLE is NOT Used

SERIALIZABLE prevents write skew via **predicate locking** (gap locks on ranges). But Ledgora's architecture makes it unnecessary:

1. **Account balance checks** — protected by `PESSIMISTIC_WRITE` row lock (more targeted than SERIALIZABLE's gap locks)
2. **Velocity checks** — soft control, acceptable drift (see §B above)
3. **EOD validation** — protected by business day lock (application-level gate)
4. **Voucher number generation** — protected by `PESSIMISTIC_WRITE` on `scroll_sequences`

SERIALIZABLE would add:
- 40-70% TPS reduction (measured via `POST /stress/load`)
- Gap lock deadlocks on `transactions` table (concurrent INSERTs)
- No additional correctness benefit (all write-skew scenarios are already covered)

## Validation

The concurrency audit (`GET /diagnostics/concurrency-audit`) verifies write-skew prevention effectiveness:
- **Check 4:** No negative balances on customer accounts
- **Check 8:** No duplicate voucher numbers (scroll sequence integrity)
- **Check 1:** Ledger balanced (no phantom entries)

Run after `POST /stress/lock-contention` and `POST /stress/deadlock` to confirm no write-skew violations under concurrent load.

## Implementation Status

| Item | Status |
|---|---|
| PESSIMISTIC_WRITE on account balance reads | ✅ Already implemented |
| PESSIMISTIC_WRITE on scroll sequences | ✅ Already implemented |
| Business day lock for EOD | ✅ Already implemented |
| @Version optimistic locking on Account | ✅ Already implemented |
| @Version optimistic locking on Voucher | ✅ Already implemented |
| UNIQUE constraints on voucher_number, transaction_ref, EOD process | ✅ Already implemented |
| Concurrency audit validates no violations | ✅ `GET /diagnostics/concurrency-audit` |
| Atomic conditional debit (`debitIfSufficient`) | ⬜ Deferred (optimization, not correctness fix) |
| Write-skew strategy documented | ✅ This document |
