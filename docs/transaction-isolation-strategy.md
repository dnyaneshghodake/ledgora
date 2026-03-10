# Ledgora — Transaction Isolation Level Strategy

**Version:** 1.0
**Baseline:** PR #43 — Feature Enhancement
**Standard:** RBI IT Governance, CBS Accounting Standard, ANSI SQL Isolation Levels

## Overview

This document defines the transaction isolation level strategy for Ledgora CBS. The goal is to balance **financial correctness** with **concurrency throughput** while avoiding unnecessary lock contention and deadlock risk.

**Key principle:** Use the **minimum isolation level that preserves correctness** for each workload. Do NOT globally set SERIALIZABLE.

## Global Default: READ_COMMITTED

```properties
# application.properties (already the JDBC default for H2 and SQL Server)
spring.jpa.properties.hibernate.connection.isolation=2
```

**JDBC isolation level constants:** 1=READ_UNCOMMITTED, 2=READ_COMMITTED, 4=REPEATABLE_READ, 8=SERIALIZABLE

**Rationale for READ_COMMITTED as default:**
- Prevents dirty reads (reading uncommitted data from other transactions)
- Allows high concurrency (no shared read locks held between statements)
- Suitable for the majority of CBS workloads (posting, queries, dashboards)
- H2 (dev) and SQL Server (prod) both support it natively

## Workload-Specific Isolation Strategy

### A) Transaction Posting (Deposits, Withdrawals, Transfers, IBT)

| Setting | Value |
|---|---|
| **Isolation** | `READ_COMMITTED` (default) |
| **Annotation** | `@Transactional` (no explicit isolation) |
| **Location** | `TransactionService.deposit()`, `withdraw()`, `transfer()` |

**Correctness guarantees (without escalating isolation):**
- `PESSIMISTIC_WRITE` lock on source/destination accounts via `findByAccountNumberWithLockAndTenantId()` — prevents concurrent balance modification
- `PESSIMISTIC_WRITE` lock on `scroll_sequences` via `findByTenantIdAndBranchIdAndPostingDateWithLock()` — ensures strictly monotonic voucher numbers
- `@Transactional` boundary ensures all-or-nothing: if any voucher post fails, entire transaction rolls back (no partial ledger)
- `@Version` optimistic locking on `Voucher` entity prevents concurrent authorize/post race conditions

**Why NOT REPEATABLE_READ:** The pessimistic locks already prevent the specific concurrent access patterns that REPEATABLE_READ protects against. Escalating isolation would add lock overhead without additional safety.

**Risk if downgraded to READ_UNCOMMITTED:** Dirty reads on account balances → incorrect insufficient-balance checks → overdrafts. **BLOCKING.**

### B) Scroll Sequence Generation

| Setting | Value |
|---|---|
| **Isolation** | `READ_COMMITTED` (default) |
| **Lock** | `PESSIMISTIC_WRITE` (explicit) |
| **Location** | `VoucherService.getNextScrollNo()` |

The scroll sequence is protected by `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the repository query, not by isolation level escalation. This ensures strict monotonicity without table-level locks.

**Do NOT escalate to SERIALIZABLE:** Gap locks from SERIALIZABLE would block all concurrent scroll sequence reads, creating a serialization bottleneck on high-volume posting.

### C) EOD State Machine Phases

| Setting | Value |
|---|---|
| **Recommended Isolation** | `REPEATABLE_READ` |
| **Current Isolation** | `READ_COMMITTED` (default — pending production validation) |
| **Annotation** | `@Transactional(propagation = REQUIRES_NEW)` |
| **Location** | `EodStateMachineService.runPhaseValidated()`, `runPhaseDayClosing()`, `runPhaseBatchClosed()`, `runPhaseSettled()`, `runPhaseDateAdvanced()` |

**Rationale for REPEATABLE_READ on EOD:**
- EOD validation phase reads voucher counts, ledger sums, clearing GL balances, suspense balances
- If these values change between reads within the same phase (due to concurrent late transactions), the validation result is inconsistent
- REPEATABLE_READ ensures that once a row is read, it remains stable for the duration of the phase transaction
- The TOCTOU re-validation after `startDayClosing()` already mitigates the primary race condition, but REPEATABLE_READ provides defense-in-depth

**Why NOT SERIALIZABLE:** EOD phases already operate on a locked business day (dayStatus = DAY_CLOSING). Once the day is locked, no new transactions can post. SERIALIZABLE's phantom read prevention is unnecessary because the data set is frozen by the business day lock.

**H2 compatibility note:** H2's MVCC mode treats REPEATABLE_READ similarly to SNAPSHOT isolation. The annotation change should be tested under the stress profile before applying to production. The current READ_COMMITTED default is safe for dev.

**Deferred action:** Add `isolation = Isolation.REPEATABLE_READ` to EOD phase methods after H2 compatibility validation via `POST /stress/chaos-eod`.

### D) Settlement Processing

| Setting | Value |
|---|---|
| **Recommended Isolation** | `REPEATABLE_READ` |
| **Current Isolation** | `READ_COMMITTED` (default) |
| **Location** | `SettlementService.processSettlement()` |

Same rationale as EOD: settlement must not see shifting balances mid-phase. The 8-step settlement workflow reads account balances, ledger sums, and batch totals — all must be consistent within each step.

**Deferred action:** Same as EOD — validate under stress profile first.

### E) Dashboard / Monitoring Queries

| Setting | Value |
|---|---|
| **Isolation** | `READ_COMMITTED` (default) |
| **Annotation** | `@Transactional(readOnly = true)` |
| **Location** | All governance dashboards, IBT list, audit explorer |

**Rationale:** Dashboards tolerate minor data drift (e.g., a KPI card showing "5 unsettled" when the actual count just changed to "4"). The `readOnly = true` flag allows Hibernate to optimize (no dirty checking, no flush). No locking impact.

**Risk if escalated to REPEATABLE_READ:** Dashboard queries would hold shared locks for their duration, potentially blocking concurrent posting transactions. No benefit — dashboards are informational.

### F) Fraud / Velocity Checks

| Setting | Value |
|---|---|
| **Isolation** | `READ_COMMITTED` (default) |
| **Location** | `VelocityFraudEngine.evaluateVelocity()` |

**Rationale:** Velocity checks query the last 60 minutes of transaction history. A near-real-time view is sufficient — if a transaction completes 1ms after the velocity check window closes, the next check will catch it. Strict snapshot isolation would add lock overhead to the critical posting path.

**Risk if escalated to REPEATABLE_READ:** Velocity checks run inside the posting transaction. If they hold shared locks on the `transactions` table, concurrent deposits/withdrawals to the same account would serialize. This defeats the purpose of high-throughput posting.

## Isolation Level Decision Matrix

| Workload | Current | Recommended | Row Locks | Reason |
|---|---|---|---|---|
| Transaction posting | READ_COMMITTED | READ_COMMITTED | PESSIMISTIC_WRITE on accounts + scroll | Row locks sufficient |
| Scroll sequence | READ_COMMITTED | READ_COMMITTED | PESSIMISTIC_WRITE | Explicit lock, not isolation |
| EOD validation | READ_COMMITTED | **REPEATABLE_READ** | None (read-only checks) | Snapshot consistency for multi-query validation |
| EOD batch close/settle | READ_COMMITTED | **REPEATABLE_READ** | None | Consistent batch state during close |
| Settlement | READ_COMMITTED | **REPEATABLE_READ** | None | Consistent balances during reconciliation |
| Dashboards | READ_COMMITTED | READ_COMMITTED | None (readOnly) | Tolerates drift |
| Fraud/velocity | READ_COMMITTED | READ_COMMITTED | None | Near-real-time sufficient |
| Audit explorer | READ_COMMITTED | READ_COMMITTED | None (readOnly) | Historical data, no contention |

## Why SERIALIZABLE is Explicitly Avoided

| Problem | Impact | Mitigation in Ledgora |
|---|---|---|
| Gap locks on indexed ranges | Blocks INSERT near existing rows → posting throughput collapse | Row-level PESSIMISTIC_WRITE is surgical and sufficient |
| Deadlock amplification | SERIALIZABLE increases deadlock probability 3-10x under load | Lock contention simulator (`POST /stress/lock-contention`) validates current deadlock profile |
| Throughput reduction | 40-70% TPS drop under SERIALIZABLE vs READ_COMMITTED | Production load generator (`POST /stress/load`) measures actual TPS |
| Phantom read protection | Only needed when querying ranges that may be concurrently inserted | EOD operates on locked business day — no concurrent inserts possible |

**Ledgora's architecture makes SERIALIZABLE unnecessary:**
1. **Immutable ledger** — `LedgerEntry` is `@Immutable`. Once written, never changes. No phantom read risk.
2. **Business day lock** — EOD sets `dayStatus = DAY_CLOSING` before validation. No new transactions can post.
3. **Row-level pessimistic locks** — `PESSIMISTIC_WRITE` on accounts and scroll sequences prevents concurrent modification without table-level locks.
4. **Atomic voucher pairs** — `createVoucherPair()` in single `@Transactional`. No orphaned half-pairs.

## Implementation Status

| Item | Status |
|---|---|
| Global READ_COMMITTED default | ✅ Already in effect (JDBC driver default) |
| Explicit `hibernate.connection.isolation=2` in properties | ⬜ Recommended (explicit is better than implicit) |
| EOD phases → REPEATABLE_READ | ⬜ Deferred (validate under stress profile first) |
| Settlement → REPEATABLE_READ | ⬜ Deferred (same) |
| Dashboards → readOnly = true | ✅ Already applied on all dashboard controllers |
| Documentation | ✅ This document |

## Validation Approach

Before applying REPEATABLE_READ to EOD/Settlement:

1. Run `POST /stress/chaos-eod` with all 4 crash phases — verify recovery works
2. Run `POST /stress/eod` with 1000+ transactions — verify EOD completes without deadlock
3. Run `POST /stress/lock-contention` with `triggerEod: true` — verify no deadlock amplification
4. Only then add `isolation = Isolation.REPEATABLE_READ` to EOD phase methods
