# Ledgora — Hot Row Mitigation Strategy for Account Balance Updates

**Version:** 1.0
**Baseline:** PR #43 — Feature Enhancement
**Prerequisite:** `docs/lock-ordering-strategy.md`, `docs/transaction-isolation-strategy.md`

## Problem Statement

In a high-volume CBS, certain account rows become "hot" — updated by many concurrent transactions. The `accounts.balance` field is the primary hot spot because every deposit, withdrawal, and transfer updates it.

**Current update pattern (read-modify-write via JPA):**
```java
// TransactionService.postTransferLedger()
BigDecimal sourceNewBalance = sourceAccount.getBalance().subtract(dto.getAmount());
sourceAccount.setBalance(sourceNewBalance);
accountRepository.save(sourceAccount);
```

This pattern:
1. Reads the current balance from the entity (already locked via `PESSIMISTIC_WRITE`)
2. Computes new balance in Java
3. Writes back via JPA `UPDATE accounts SET balance=?, version=? WHERE id=? AND version=?`

**Hot row symptoms:**
- Lock wait time > 500ms on account rows
- `@Version` optimistic lock failures (`OptimisticLockException`)
- Throughput plateau under concurrent load to same account
- Detectable via `POST /stress/lock-contention` (slow transaction count > 0)

## Key Architectural Insight

**`Account.balance` is explicitly a PERFORMANCE CACHE** (see `Account.java:57-62`):

> *"PERFORMANCE CACHE ONLY — not the source of truth. True balance = SUM(ledger credits) - SUM(ledger debits) per account."*

This means:
- The balance field exists purely for read performance (avoid SUM query on every account view)
- Financial decisions (insufficient balance check) read it under `PESSIMISTIC_WRITE` lock — safe
- If the cache drifts, `AccountBalanceReconciliationService` (runs every 15 min) detects and alerts
- The balance field does NOT need the same transactional guarantees as the immutable ledger

## Current Protections (Already Implemented)

| Protection | Location | Status |
|---|---|---|
| `@Version` optimistic locking | `Account.java:152-154` | ✅ Already present |
| `PESSIMISTIC_WRITE` on account rows | `AccountRepository.findByAccountNumberWithLockAndTenantId()` | ✅ Already present |
| `PESSIMISTIC_WRITE` on scroll sequences | `ScrollSequenceRepository.findByTenantIdAndBranchIdAndPostingDateWithLock()` | ✅ Already present |
| Scheduled cache reconciliation | `AccountBalanceReconciliationService` (every 15 min) | ✅ Already present |
| Balance drift detection | `BalanceDriftAlert` + `ledgora.balance.drift` metric | ✅ Already present |
| Immutable ledger as source of truth | `LedgerEntry` with `@Immutable` | ✅ Already present |

## Recommended Enhancements (Deferred)

### Enhancement 1: Atomic SQL Increment

**Replace** read-modify-write with single atomic SQL:

```java
// New repository method
@Modifying
@Query("UPDATE Account a SET a.balance = a.balance + :delta WHERE a.id = :id")
int incrementBalance(@Param("id") Long id, @Param("delta") BigDecimal delta);
```

**Usage:**
```java
// Instead of:
BigDecimal newBalance = account.getBalance().add(dto.getAmount());
account.setBalance(newBalance);
accountRepository.save(account);

// Use:
accountRepository.incrementBalance(account.getId(), dto.getAmount());
// For withdrawals: incrementBalance(id, amount.negate())
```

**Benefits:**
- Eliminates read-modify-write race window
- Works with `PESSIMISTIC_WRITE` lock already held
- Single SQL round-trip instead of SELECT + UPDATE
- No optimistic lock failure possible (no version check needed for increment)

**Why deferred:** The `@Modifying` query bypasses JPA entity state management. After calling `incrementBalance()`, the in-memory `Account` entity still holds the old balance. Any subsequent code reading `account.getBalance()` in the same transaction would see stale data. The posting flow would need to be restructured to use the repository method and refresh the entity, which is a service-level change.

### Enhancement 2: Late Balance Update

**Move balance cache update to the END of the posting flow**, after all validations and ledger writes:

```
Current order:
1. Lock account (PESSIMISTIC_WRITE)
2. Validate (balance check, freeze, velocity, ceiling)
3. Create vouchers
4. Authorize vouchers
5. Post vouchers (create ledger entries)
6. Update balance cache        ← lock held during steps 2-6
7. Release lock

Optimized order:
1. Lock account (PESSIMISTIC_WRITE)
2. Validate (balance check, freeze)
3. Release lock                ← early release
4. Velocity check (no lock needed — reads are approximate)
5. Create + authorize + post vouchers
6. Re-lock account             ← minimal lock duration
7. Update balance cache
8. Release lock
```

**Why deferred:** This requires splitting the `@Transactional` boundary or changing the lock acquisition timing. The current single-transaction design (lock → validate → post → update → release) is simple and correct. The optimization trades simplicity for throughput and needs stress test validation.

### Enhancement 3: Hot Row Monitoring

**Log slow balance updates:**

```java
long updateStart = System.currentTimeMillis();
accountRepository.save(account);
long updateTime = System.currentTimeMillis() - updateStart;
if (updateTime > 500) {
    log.warn("HOT_ROW_CONTENTION: account={} updateTime={}ms",
             account.getAccountNumber(), updateTime);
}
```

**Status:** Can be added to `TransactionService` posting methods without business logic change. Deferred pending the broader hot row optimization.

## Lock Duration Analysis

Current lock duration for a transfer (worst case: cross-branch IBT):

```
PESSIMISTIC_WRITE acquired (source + dest accounts)
  ├── Validate active/frozen/balance          ~1ms
  ├── Velocity fraud check                    ~5ms (DB query)
  ├── Create 4 vouchers                       ~20ms (4 INSERTs + scroll lock)
  ├── System-authorize 4 vouchers             ~10ms (4 UPDATEs)
  ├── Post 4 vouchers (ledger entries)        ~40ms (4 journal INSERTs + 4 entry INSERTs)
  ├── Update GL balances                      ~10ms
  ├── Update account balances                 ~5ms (2 UPDATEs)
  └── Update batch totals                     ~5ms
PESSIMISTIC_WRITE released
Total lock hold time: ~96ms per IBT transfer
```

For simple deposits: ~30ms lock hold time. For same-branch transfers: ~50ms.

**At 100 TPS to the same account, the theoretical max throughput is ~10 TPS** (1000ms / 96ms ≈ 10). For most accounts, this is not a bottleneck (accounts rarely receive 10 concurrent transactions). For high-volume system accounts (cash GL, clearing GL), the lock contention simulator should be used to profile.

## Validation Approach

1. Run `POST /stress/load` — measure P95 latency and identify hot accounts
2. Run `POST /stress/lock-contention` — count slow transactions (>2s) and lock waits
3. If hot row contention is confirmed in production metrics, apply Enhancement 1 (atomic increment)
4. Re-run stress tests to verify improvement

## Implementation Status

| Item | Status |
|---|---|
| `@Version` optimistic locking on Account | ✅ Already present |
| `PESSIMISTIC_WRITE` on account rows | ✅ Already present |
| Scheduled balance reconciliation | ✅ Already present (`AccountBalanceReconciliationService`) |
| Atomic SQL increment (`incrementBalance`) | ⬜ Deferred (service restructuring needed) |
| Late balance update (lock duration reduction) | ⬜ Deferred (transaction boundary change) |
| Hot row monitoring (>500ms logging) | ⬜ Deferred (minimal — can add anytime) |
| Hot row strategy documented | ✅ This document |
