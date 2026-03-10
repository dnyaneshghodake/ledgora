# Ledgora — Multi-Node Cluster Locking Strategy

**Version:** 1.0
**Baseline:** PR #43 — Feature Enhancement
**Standard:** RBI IT Framework — Business Continuity / Operational Resilience

## Objective

Ensure CBS financial correctness when multiple application nodes (horizontal scaling) operate against the same database. All coordination must be DB-backed — no in-memory locks, no local caches for financial state.

## Architecture Principle

```
┌─────────┐  ┌─────────┐  ┌─────────┐
│ Node A  │  │ Node B  │  │ Node C  │
│ (App)   │  │ (App)   │  │ (App)   │
└────┬────┘  └────┬────┘  └────┬────┘
     │            │            │
     └────────────┼────────────┘
                  │
         ┌────────┴────────┐
         │   Shared DB     │
         │  (SQL Server)   │
         │                 │
         │  All locks are  │
         │  DB-backed:     │
         │  - PESSIMISTIC  │
         │  - UNIQUE       │
         │  - @Version     │
         └─────────────────┘
```

**Rule:** The database is the ONLY coordination point. No node holds any lock or state that other nodes cannot see.

## Current Cluster-Safe Mechanisms (Already Implemented)

### 1) Singleton EOD Execution

**Mechanism:** `UNIQUE(tenant_id, business_date)` constraint on `eod_processes` table.

```java
// EodProcess.java — lines 44-48
@UniqueConstraint(
    name = "uk_eod_tenant_date",
    columnNames = {"tenant_id", "business_date"})
```

**How it works in multi-node:**
- Node A attempts `INSERT INTO eod_processes (tenant_id, business_date, ...)` → succeeds
- Node B attempts same INSERT → `DataIntegrityViolationException` (UNIQUE violation) → caught, EOD blocked
- Node A's EOD proceeds; Node B sees "EOD already in progress"

**Status:** ✅ Already implemented. Works identically on single-node and multi-node.

### 2) Account Balance Locking

**Mechanism:** `PESSIMISTIC_WRITE` on account rows via `SELECT ... FOR UPDATE`.

```java
// AccountRepository.java — line 68-75
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.tenant.id = :tenantId")
Optional<Account> findByAccountNumberWithLockAndTenantId(...);
```

**How it works in multi-node:**
- Node A locks Account X → DB row-level lock acquired
- Node B attempts to lock Account X → blocks at DB level until Node A's transaction commits
- Both nodes see the same committed state — no stale reads

**Status:** ✅ Already implemented. `SELECT FOR UPDATE` is a DB-level lock visible to all nodes.

### 3) Scroll Sequence Generation

**Mechanism:** `PESSIMISTIC_WRITE` on `scroll_sequences` table.

```java
// ScrollSequenceRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM ScrollSequence s WHERE s.tenantId = :tenantId AND s.branchId = :branchId AND s.postingDate = :postingDate")
Optional<ScrollSequence> findByTenantIdAndBranchIdAndPostingDateWithLock(...);
```

**How it works in multi-node:**
- Node A locks scroll sequence row → increments to 000042
- Node B blocks → when released, reads 000042, increments to 000043
- Monotonicity guaranteed across all nodes

**Status:** ✅ Already implemented. DB-level row lock.

### 4) Optimistic Locking (Conflict Detection)

**Mechanism:** `@Version` on `Account` and `Voucher` entities.

```java
// Account.java — line 152-154
@Version
@Column(name = "version")
private Long version;
```

**How it works in multi-node:**
- Node A reads Account (version=5), modifies, saves → UPDATE ... WHERE version=5 → succeeds, version→6
- Node B reads Account (version=5), modifies, saves → UPDATE ... WHERE version=5 → 0 rows updated → `OptimisticLockException`
- Node B must retry or fail

**Status:** ✅ Already implemented on Account and Voucher.

### 5) Idempotency (Duplicate Prevention)

**Mechanism:** `UNIQUE INDEX (client_reference_id, channel, tenant_id)` on `transactions`.

```java
// Transaction.java — line 26-27
@Index(name = "idx_txn_client_ref_channel_tenant",
       columnList = "client_reference_id, channel, tenant_id")
```

**How it works in multi-node:**
- Node A inserts transaction with ref=ABC → succeeds
- Node B attempts same ref=ABC → constraint violation → caught as `DUPLICATE_TRANSACTION`

**Status:** ✅ Already implemented. DB-level unique index.

### 6) Immutable Ledger

**Mechanism:** `@Immutable` on `LedgerJournal` and `LedgerEntry`.

Hibernate generates no UPDATE or DELETE SQL for these entities. This is enforced at the ORM level but the immutability is the same regardless of which node created the entry — once committed, no node can modify it.

**Status:** ✅ Already implemented.

## What Does NOT Need Cluster Coordination

| Component | Why No Coordination Needed |
|---|---|
| `TenantContextHolder` (ThreadLocal) | Per-request context, not shared state |
| `SecurityContextHolder` | Per-request authentication, not shared |
| Hibernate L1 cache | Per-transaction, cleared on commit |
| Session attributes | Per-HTTP-session, sticky to node via LB |

## In-Memory Synchronization Audit

**Result: NONE FOUND.** The codebase contains zero `synchronized` blocks, zero `ReentrantLock` instances, and zero `java.util.concurrent.locks.Lock` fields in financial service classes. All coordination is DB-backed.

## Advisory Lock Strategy (PostgreSQL Production)

For production PostgreSQL deployments, an optional advisory lock can provide an additional coordination layer for long-running operations like EOD:

```java
// Conceptual — not implemented (deferred to production deployment)
public boolean acquireTenantLock(Long tenantId) {
    return jdbcTemplate.queryForObject(
        "SELECT pg_try_advisory_lock(?)", Boolean.class, tenantId);
}

public void releaseTenantLock(Long tenantId) {
    jdbcTemplate.execute("SELECT pg_advisory_unlock(" + tenantId + ")");
}
```

**Why deferred:**
- H2 (dev) does not support `pg_try_advisory_lock`
- The UNIQUE constraint on `eod_processes` already prevents double EOD
- Advisory locks are a PostgreSQL-specific optimization, not a correctness requirement

## Cluster Safety Verification

The stress test harness validates cluster safety:

| Test | What it validates | Endpoint |
|---|---|---|
| Lock contention | DB-level locks work under concurrent access | `POST /stress/lock-contention` |
| Deadlock simulation | DB detects and resolves deadlocks; `@Transactional` rollback is clean | `POST /stress/deadlock` |
| Chaos EOD | EOD state machine resume works (simulates node crash mid-EOD) | `POST /stress/chaos-eod` |
| Concurrency audit | No invariant violated after concurrent load | `GET /diagnostics/concurrency-audit` |

## Implementation Status

| Item | Status |
|---|---|
| UNIQUE(tenant_id, business_date) on eod_processes | ✅ Already exists |
| PESSIMISTIC_WRITE on accounts | ✅ Already exists |
| PESSIMISTIC_WRITE on scroll_sequences | ✅ Already exists |
| @Version on Account + Voucher | ✅ Already exists |
| UNIQUE on voucher_number | ✅ Already exists |
| UNIQUE on transaction_ref | ✅ Already exists |
| No synchronized blocks in financial services | ✅ Verified (zero found) |
| No in-memory caches for financial state | ✅ Account.balance is DB-persisted cache (not in-memory) |
| DB advisory locks (PostgreSQL) | ⬜ Deferred (production optimization) |
| Cluster strategy documented | ✅ This document |
