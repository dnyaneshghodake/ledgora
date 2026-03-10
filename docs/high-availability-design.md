# Ledgora — High-Availability and Failover Safety Design

**Version:** 1.0
**Baseline:** PR #43 — Feature Enhancement
**Standard:** RBI IT Framework — Business Continuity, Operational Resilience
**Prerequisites:** `docs/multi-node-cluster-strategy.md`, `docs/transaction-isolation-strategy.md`

## Design Principles

1. **No distributed transaction manager** — all safety via DB constraints + state machine
2. **Idempotent operations** — safe to retry any operation after crash
3. **Phase-based EOD** — crash between phases loses no committed work
4. **DB is source of truth** — no in-memory state that can't be reconstructed from DB

## Current Crash Safety Mechanisms

### 1) EOD Crash-Safe State Machine

The EOD process is the most crash-sensitive operation in the system. It uses a 5-phase state machine where each phase commits independently:

```
VALIDATED → DAY_CLOSING → BATCH_CLOSED → SETTLED → DATE_ADVANCED
     ↓            ↓             ↓            ↓            ↓
  COMMIT 1     COMMIT 2      COMMIT 3     COMMIT 4     COMMIT 5
```

**Crash scenario:** If the application crashes between BATCH_CLOSED and SETTLED:
- `eod_processes` table shows: `status=RUNNING, phase=SETTLED` (phase was advanced in COMMIT 3)
- On restart: `findIncompleteProcesses()` finds this record
- `executeEod()` detects RUNNING status → resumes from SETTLED phase
- Phases already committed (VALIDATED, DAY_CLOSING, BATCH_CLOSED) are NOT re-executed

**Implementation:** `EodStateMachineService` — each phase method annotated with `@Transactional(propagation = REQUIRES_NEW)`. See `docs/E2E_FLOW.md` §7.2.

**Status:** ✅ Already implemented.

### 2) Idempotency Guarantees

| Operation | Idempotency Mechanism | Status |
|---|---|---|
| Transaction creation | `UNIQUE(client_reference_id, channel, tenant_id)` + `IdempotencyService` | ✅ Exists |
| Voucher creation | `UNIQUE(voucher_number)` DB constraint | ✅ Exists |
| EOD execution | `UNIQUE(tenant_id, business_date)` on `eod_processes` | ✅ Exists |
| Transaction ref | `UNIQUE(transaction_ref)` on `transactions` | ✅ Exists |
| Scroll sequence | `PESSIMISTIC_WRITE` + increment-and-save pattern | ✅ Exists |

**Key insight:** If a transaction crashes after the DB commit but before the HTTP response, the client retries with the same `clientReferenceId`. The idempotency check finds the existing record and returns an error (not a duplicate posting).

### 3) Immutable Ledger

`LedgerEntry` and `LedgerJournal` are `@Immutable`. Hibernate generates no UPDATE/DELETE SQL. Once a ledger entry is committed to the DB, it cannot be corrupted by any subsequent crash or code defect. Corrections require new compensating entries.

**Status:** ✅ Already implemented.

### 4) Atomic Voucher Pairs

`createVoucherPair()` creates both DR and CR vouchers in a single `@Transactional`. If the CR voucher insert fails, the DR voucher is rolled back — no orphaned half-pairs.

**Status:** ✅ Already implemented.

## Recommended Enhancements

### Enhancement 1: Automatic EOD Resume on Startup

**Current:** `findIncompleteProcesses()` exists but is not called automatically on application startup. An operator must manually trigger resume via the UI.

**Proposed:** Add `@EventListener(ApplicationReadyEvent.class)` to auto-detect and resume incomplete EOD processes:

```java
@Component
public class EodStartupRecovery {
    @EventListener(ApplicationReadyEvent.class)
    public void resumeIncompleteEod() {
        List<EodProcess> incomplete = eodStateMachineService.findIncompleteProcesses();
        for (EodProcess p : incomplete) {
            log.warn("STARTUP RECOVERY: Resuming incomplete EOD for tenant {} date {}",
                     p.getTenant().getId(), p.getBusinessDate());
            try {
                eodStateMachineService.executeEod(p.getTenant().getId());
            } catch (Exception e) {
                log.error("STARTUP RECOVERY FAILED for EOD process {}: {}", p.getId(), e.getMessage());
            }
        }
    }
}
```

**Why deferred:** Requires careful handling of `TenantContextHolder` and `SecurityContext` setup during startup (no HTTP request context available). Also needs to handle the case where the startup recovery itself fails. The chaos EOD tester (`POST /stress/chaos-eod`) validates the resume logic works correctly.

### Enhancement 2: Retry Template for Posting

**Current:** If a deadlock occurs during posting, the transaction fails and the user sees an error. No automatic retry.

**Proposed:** Wrap posting operations in Spring `RetryTemplate`:

```java
@Bean
public RetryTemplate postingRetryTemplate() {
    return RetryTemplate.builder()
        .maxAttempts(3)
        .exponentialBackoff(100, 2.0, 2000)  // 100ms, 200ms, 400ms
        .retryOn(DeadlockLoserDataAccessException.class)
        .retryOn(LockAcquisitionException.class)
        .retryOn(CannotAcquireLockException.class)
        .build();
}
```

**Usage:**
```java
retryTemplate.execute(ctx -> {
    transactionService.transfer(dto);
    return null;
});
```

**Why deferred:**
- Retry must be at the controller level (outermost `@Transactional` boundary), not inside the service
- The entire transaction must be re-executed on retry (new voucher numbers, new idempotency check)
- Requires `spring-retry` dependency (not currently in `pom.xml`)
- The deadlock simulator (`POST /stress/deadlock`) confirms that deadlocks are currently safe (no data corruption) — retry is a UX improvement, not a correctness fix

### Enhancement 3: Read Replica Routing

**Current:** All queries (dashboards, lists, audit explorer) hit the primary database.

**Proposed configuration:**

```properties
# application-prod.properties
app.readReplica.enabled=true
app.readReplica.url=jdbc:sqlserver://replica-host:1433;databaseName=ledgoradb
app.readReplica.username=readonly_user
app.readReplica.password=${REPLICA_PASSWORD}
```

**Routing rules:**
- `@Transactional(readOnly = true)` → route to replica
- All dashboard controllers already use `readOnly = true`
- Posting, EOD, settlement → always primary

**Implementation approach:** Spring `AbstractRoutingDataSource` with `determineCurrentLookupKey()` checking `TransactionSynchronizationManager.isCurrentTransactionReadOnly()`.

**Why deferred:**
- Requires multi-datasource configuration (primary + replica)
- H2 doesn't support replication
- All dashboard controllers already have `@Transactional(readOnly = true)` — the annotation is ready, only the routing infrastructure is missing

## Failure Mode Analysis

| Failure | Impact | Recovery | Status |
|---|---|---|---|
| **App crash during posting** | Transaction rolls back (Spring `@Transactional`) | No data corruption. Client retries with same `clientReferenceId`. | ✅ Safe |
| **App crash during EOD** | Phase partially committed | `findIncompleteProcesses()` → resume from last phase | ✅ Safe |
| **DB crash** | All uncommitted transactions lost | DB recovery restores to last checkpoint. Committed data intact. | ✅ Safe (DB responsibility) |
| **Network partition** (app ↔ DB) | Connection timeout → `@Transactional` rollback | Reconnect automatically (HikariCP). Retry operation. | ✅ Safe |
| **Deadlock** | One transaction aborted by DB | Aborted transaction rolls back cleanly. Other succeeds. | ✅ Safe (validated by stress/deadlock) |
| **Double EOD request** | UNIQUE constraint violation | Second request fails immediately. First continues. | ✅ Safe |
| **Concurrent same-account transfers** | `PESSIMISTIC_WRITE` serializes | Second thread waits, then proceeds with updated balance. | ✅ Safe |

## Validation

| Test | What it validates | Endpoint |
|---|---|---|
| Chaos EOD | Phase-based crash recovery works | `POST /stress/chaos-eod` |
| Deadlock simulation | Deadlock recovery is clean (no partial state) | `POST /stress/deadlock` |
| Lock contention | High concurrency doesn't corrupt data | `POST /stress/lock-contention` |
| Concurrency audit | All invariants hold after failures | `GET /diagnostics/concurrency-audit` |

## Implementation Status

| Item | Status |
|---|---|
| Phase-based EOD state machine | ✅ Implemented (`EodStateMachineService`) |
| UNIQUE constraints for idempotency | ✅ Implemented (EOD, voucher, transaction) |
| Immutable ledger | ✅ Implemented (`@Immutable`) |
| Atomic voucher pairs | ✅ Implemented (`createVoucherPair()`) |
| `@Version` optimistic locking | ✅ Implemented (Account, Voucher) |
| `PESSIMISTIC_WRITE` on financial rows | ✅ Implemented (accounts, scroll sequences) |
| Dashboard `readOnly = true` | ✅ Implemented (all governance controllers) |
| Automatic EOD resume on startup | ⬜ Deferred (needs startup context setup) |
| Retry template for deadlock recovery | ⬜ Deferred (needs `spring-retry` dependency) |
| Read replica routing | ⬜ Deferred (needs multi-datasource config) |
| HA design documented | ✅ This document |
