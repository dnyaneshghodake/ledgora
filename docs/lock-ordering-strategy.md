# Ledgora — Deterministic Lock Ordering Strategy

**Version:** 1.0
**Baseline:** PR #43 — Feature Enhancement
**Standard:** Deadlock prevention via Coffman condition elimination (circular wait)

## Problem Statement

Database deadlocks occur when two or more transactions hold locks and each waits for a lock held by the other. In Ledgora CBS, this happens specifically during concurrent transfers:

```
Thread A: transfer(Account X → Account Y)
  Step 1: PESSIMISTIC_WRITE lock on Account X  ← acquired
  Step 2: PESSIMISTIC_WRITE lock on Account Y  ← WAIT (held by Thread B)

Thread B: transfer(Account Y → Account X)
  Step 1: PESSIMISTIC_WRITE lock on Account Y  ← acquired
  Step 2: PESSIMISTIC_WRITE lock on Account X  ← WAIT (held by Thread A)

Result: DEADLOCK — circular wait condition satisfied
```

## Solution: Deterministic Lock Ordering

**Rule:** When a transaction must lock multiple accounts, always acquire locks in **ascending order of `account_id`** (the database primary key). This eliminates the circular wait condition.

```
Thread A: transfer(Account X(id=5) → Account Y(id=12))
  Step 1: Lock Account 5 (lower ID first)
  Step 2: Lock Account 12

Thread B: transfer(Account Y(id=12) → Account X(id=5))
  Step 1: Lock Account 5 (lower ID first)  ← WAIT (held by Thread A)
  ... Thread A completes and releases both locks ...
  Step 2: Lock Account 12

Result: No deadlock — both threads lock in same order
```

## Lock Acquisition Hierarchy

All lock-acquiring operations must follow this global ordering:

```
Level 1: Scroll Sequence (PESSIMISTIC_WRITE)
  ↓ must be acquired before
Level 2: Account Rows (PESSIMISTIC_WRITE, sorted by account_id ASC)
  ↓ must be acquired before
Level 3: Batch Row (implicit via batchService.updateBatchTotals())
  ↓ must be acquired before
Level 4: Ledger Journal + Entry (INSERT — no lock conflict, append-only)
```

**Within Level 2:** When multiple accounts are locked in one transaction, sort by `account.getId()` ascending and lock in that order.

## Current Lock Acquisition Points

### A) TransactionService.transfer() — Lines 389-408

**Current behavior (deadlock-prone):**
```java
Account sourceAccount = accountRepository.findByAccountNumberWithLockAndTenantId(source, tenantId);
Account destAccount = accountRepository.findByAccountNumberWithLockAndTenantId(dest, tenantId);
```

Lock order is determined by parameter order (source first, dest second). If two concurrent transfers have opposite source/dest, deadlock occurs.

**Recommended fix (deferred — requires 2-phase fetch):**
```java
// Phase 1: Fetch WITHOUT lock to get IDs
Account srcNoLock = accountRepository.findByAccountNumberAndTenantId(source, tenantId).orElseThrow();
Account dstNoLock = accountRepository.findByAccountNumberAndTenantId(dest, tenantId).orElseThrow();

// Phase 2: Lock in ascending ID order
Long firstId = Math.min(srcNoLock.getId(), dstNoLock.getId());
Long secondId = Math.max(srcNoLock.getId(), dstNoLock.getId());
Account first = accountRepository.findByIdWithLock(firstId).orElseThrow();
Account second = accountRepository.findByIdWithLock(secondId).orElseThrow();

// Map back to source/dest
Account sourceAccount = first.getId().equals(srcNoLock.getId()) ? first : second;
Account destAccount = first.getId().equals(srcNoLock.getId()) ? second : first;
```

**Why deferred:** This changes the fetch pattern from 1 locked fetch per account to 1 unlocked fetch + 1 locked fetch per account (4 queries instead of 2). Needs performance validation via `POST /stress/load` before deploying.

### B) TransactionService.approveTransaction() — Lines 610-625

Same pattern for transfer approval: source and dest locked in parameter order. Same fix applies.

### C) IBT 4-Voucher Flow — postTransferLedger() Lines 913-964

IBT involves 4 accounts: source customer, IBC_OUT, IBC_IN, dest customer. The clearing accounts are resolved by branch code (not user input), so their IDs are deterministic per branch. Risk is lower but the ordering principle still applies.

**Current lock exposure:**
```
Lock 1: sourceAccount (user-determined)
Lock 2: ibcOutAccount (branch-determined)
Lock 3: ibcInAccount (branch-determined)
Lock 4: destAccount (user-determined)
```

Accounts at positions 2 and 3 are IBC system accounts — they have fixed IDs per branch. Deadlock risk is between positions 1/4 (user accounts) and is covered by the same ascending-ID strategy.

### D) VoucherService.createVoucher() → getNextScrollNo()

Scroll sequences are locked via `PESSIMISTIC_WRITE` on `(tenant_id, branch_id, posting_date)`. This is a **single-row lock per branch per date** — no multi-row ordering needed. The lock is acquired before any account locks (Level 1 in the hierarchy).

### E) VoucherService.cancelVoucher()

Reversal involves the original voucher's account. Only one account is locked per reversal voucher, so no ordering issue.

## Deadlock Risk Assessment

| Flow | Accounts Locked | Ordering Risk | Status |
|---|---|---|---|
| Deposit | 1 (destination) | None (single lock) | ✅ Safe |
| Withdrawal | 1 (source) | None (single lock) | ✅ Safe |
| Transfer | 2 (source + destination) | **HIGH** — opposite ordering possible | ⚠️ Fix deferred |
| IBT | 4 (source + IBC_OUT + IBC_IN + dest) | **MEDIUM** — IBC accounts are deterministic | ⚠️ Fix deferred |
| Transfer approval | 2 (source + destination) | **HIGH** — same as transfer | ⚠️ Fix deferred |
| Reversal | 1 (original account) | None (single lock) | ✅ Safe |
| Scroll sequence | 1 (sequence row) | None (single lock per branch) | ✅ Safe |

## Mitigation (Current)

The deadlock simulator (`POST /stress/deadlock`) validates that:
1. When a deadlock occurs, the DB engine aborts one transaction (deadlock victim)
2. Spring's `@Transactional` rolls back the victim cleanly — no partial state
3. Ledger remains balanced after deadlock (vouchers come in pairs, rollback is atomic)
4. The other transaction completes successfully

**This means deadlocks are currently SAFE (no data corruption) but cause transaction failures that require retry.** The deterministic ordering would eliminate deadlocks entirely, removing the need for retry.

## Implementation Status

| Item | Status |
|---|---|
| Lock ordering strategy documented | ✅ This document |
| Deadlock simulator validates recovery | ✅ `POST /stress/deadlock` |
| Lock contention simulator profiles production risk | ✅ `POST /stress/lock-contention` |
| TransactionService.transfer() — ascending ID lock order | ⬜ Deferred (validate performance impact first) |
| TransactionService.approveTransaction() — same | ⬜ Deferred |
| IBT 4-account ordering | ⬜ Deferred |

## Validation Approach

Before applying lock ordering changes:

1. Run `POST /stress/deadlock` — baseline current deadlock count per round
2. Apply lock ordering change to `TransactionService.transfer()`
3. Re-run `POST /stress/deadlock` — verify deadlock count drops to 0
4. Run `POST /stress/load` — verify no TPS regression from 2-phase fetch
5. Run `POST /stress/lock-contention` — verify no new contention patterns
