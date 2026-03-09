# Ledgora — RBI Governance & Compliance Control Layer

**Document Version:** 1.0
**System:** Ledgora CBS (Spring Boot 3.2.3)
**Commit Baseline:** d6a0a46
**Standard Applied:** RBI Master Direction on IT Governance (2023), Banking Regulation Act §10/§35A, IS Audit Guidelines

---

## PART 1 — RBI COMPLIANCE MAPPING

### 1.1 Governance Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    GOVERNANCE LAYER                       │
├─────────────────────────────────────────────────────────┤
│  Segregation of Duties (Maker-Checker)                   │
│  Voucher Authorization Control                           │
│  Business Day Gate (OPEN / DAY_CLOSING / CLOSED)         │
│  Audit Trail (AuditService → audit_logs)                 │
│  Account Lockout & Session Control                       │
├─────────────────────────────────────────────────────────┤
│                  ACCOUNTING CONTROL LAYER                 │
├─────────────────────────────────────────────────────────┤
│  Immutable Ledger (@Immutable on Journal + Entry)        │
│  Double-Entry Enforcement (debit == credit per journal)  │
│  Voucher Lifecycle (DRAFT → APPROVED → POSTED)           │
│  GL Balance Integrity (Branch + Tenant level)            │
│  Account.balance = cache only (ledger = truth)           │
├─────────────────────────────────────────────────────────┤
│                  OPERATIONAL CONTROL LAYER                │
├─────────────────────────────────────────────────────────┤
│  Batch Lifecycle (OPEN → CLOSED → SETTLED)               │
│  EOD Validation (10+ pre-checks before day close)        │
│  Day Begin Ceremony (explicit open after CLOSED)         │
│  Settlement (per-tenant, validates all invariants)        │
│  Tenant Isolation (ThreadLocal + session + DB scope)     │
│  Idempotency Keys (deduplication per client+channel)     │
└─────────────────────────────────────────────────────────┘
```

### 1.2 RBI Requirement → Ledgora Component Mapping

| # | RBI Requirement | Reference | Ledgora Component | Implementation | Status |
|---|---|---|---|---|---|
| R1 | **Ledger Immutability** — Books of accounts must not be altered after posting | Banking Reg Act §10, IAS/GAAP | `LedgerJournal` + `LedgerEntry` | Both marked `@org.hibernate.annotations.Immutable`; Hibernate prevents UPDATE/DELETE SQL. Corrections via reversal vouchers only. | ✅ Compliant |
| R2 | **Double-Entry Accounting** — Every debit must have an equal credit | Banking Reg Act §10, AS-1 | `VoucherService.postVoucher()`, `EodValidationService` | Voucher pairs (DR+CR) created per transaction. EOD validates `SUM(debits) == SUM(credits)` at ledger and voucher level. | ✅ Compliant |
| R3 | **Segregation of Duties** — Maker and checker must be different persons | RBI IT Gov §4.2, IS Audit §5.2 | `VoucherService.authorizeVoucher()`, `TransactionService.approveTransaction()` | `maker.getId() != checker.getId()` enforced in service layer. `@PreAuthorize` role gates on VoucherController. | ✅ Compliant |
| R4 | **Daily Balancing** — Books must balance at end of each business day | Banking Reg Act §10, RBI Circular | `EodValidationService.validateEod()` | 10+ pre-checks: unauthorized vouchers, unposted vouchers, approved-but-unposted, voucher DR/CR balance, ledger DR/CR balance, pending approvals, pending transactions, open batches, tenant GL balance. EOD blocked on any failure. | ✅ Compliant |
| R5 | **Audit Trail** — All financial operations must be traceable | RBI IT Gov §6.3, IS Audit §6 | `AuditService`, `audit_logs` table | Voucher lifecycle (CREATED/AUTHORIZED/POSTED/CANCELLED) logged via `AuditService.logEvent()`. Transaction lifecycle logged. Login/logout tracked. Old/new value capture available via `logChangeEvent()`. | ✅ Compliant |
| R6 | **Data Integrity** — Financial data must not be corrupted | RBI IT Gov §4.3, IS Audit §5.7 | `@Version` optimistic locking, `PESSIMISTIC_WRITE` on scroll sequences, `@Immutable` on ledger | Voucher has `@Version` for concurrent access safety. Scroll sequences use pessimistic locks. Ledger entries are immutable. Account balance uses `@Version`. | ✅ Compliant |
| R7 | **Business Day Control** — No transactions outside business hours | RBI Ops Circular | `TenantService.validateBusinessDayOpen()`, `Tenant.dayStatus` | Transactions blocked when `dayStatus != OPEN`. Day lifecycle: OPEN → DAY_CLOSING → CLOSED → (Day Begin) → OPEN. Per-tenant independent dates. | ✅ Compliant |
| R8 | **Batch Integrity** — Transaction batches must balance | CBS Operations Standard | `BatchService.validateBatchClose()`, `settleAllBatches()` | `totalDebit == totalCredit` enforced before close and before settlement. Only OPEN batches accept new transactions. | ✅ Compliant |
| R9 | **Tenant Isolation** — Multi-bank data must not leak | RBI IT Gov §4.3 | `TenantContextHolder` (ThreadLocal), `tenant_id` FK on all entities | Every query is tenant-scoped. Tenant switch requires POST + MULTI scope + allowed tenant validation. Context cleared after request. | ✅ Compliant |
| R10 | **Access Control** — Role-based access with least privilege | RBI Cyber Security Framework §4.1 | `SecurityConfig`, `@PreAuthorize`, 12 roles | CSRF enabled (cookie-based). H2 console requires ADMIN. Voucher endpoints role-gated. Session concurrency limit (1). Account lockout after 5 failures. | ✅ Compliant |
| R11 | **Idempotency** — Duplicate transactions must be prevented | CBS Operations Standard | `IdempotencyService`, composite index `(client_reference_id, channel, tenant_id)` | Checked before transaction creation. Existing key returns error. | ✅ Compliant |
| R12 | **KYC Compliance** — Customer identity verification before account operations | RBI KYC/AML Master Direction | `CbsCustomerValidationService`, `CustomerMaster.kycStatus` | Account operations validate customer KYC status. PAN/Aadhaar stored in `CustomerTaxProfile`. | ✅ Compliant |

### 1.3 Identified Compliance Gaps

| # | Gap | Severity | Current State | Recommendation |
|---|---|---|---|---|
| G1 | **Suspense GL not implemented** | HIGH | No mechanism to park unresolved postings. Failed partial transfers have no safe landing zone. | Implement per-tenant Suspense GL (see Part 3). EOD should validate Suspense GL == 0. |
| G2 | **Inter-branch clearing GL absent** | HIGH | Multi-branch transfers within same tenant lack a clearing mechanism. No mirrored entries for branch-level reconciliation. | Implement INTER_BRANCH_CLEARING GL with mirrored entries (see Part 4). |
| G3 | **No transaction amount limits per role** | MEDIUM | `TransactionService` validates amount > 0 and < 999999999999.99, but no per-role/per-channel limits. A teller can process unlimited amounts. | Add configurable teller/channel limits in approval policy. |
| G4 | **No velocity checks** | MEDIUM | No detection of rapid-fire transactions from same account/user within a time window. | Add velocity monitoring (e.g., max N transactions per account per hour). |
| G5 | **systemAuthorizeVoucher uses maker as checker** | MEDIUM | STP vouchers show `maker_id == checker_id`. Audit trail does not clearly distinguish human approval from system auto. | Seed a dedicated `SYSTEM_AUTO` user; use it as checker for STP flows. |
| G6 | **No scheduled ledger-vs-cache reconciliation** | LOW | `Account.balance` is documented as cache, but no scheduled job validates it against `SUM(ledger_entries)`. Drift could go undetected between EODs. | Add a `@Scheduled` validator service (e.g., every 5 minutes) that compares cache vs ledger and logs discrepancies. |
| G7 | **No data retention / archival policy** | LOW | All data lives in active tables indefinitely. No archival for historical ledger entries or closed-day vouchers. | Design an archival strategy: move closed-day data to archive tables after configurable retention period. |
| G8 | **Password policy not enforced** | LOW | BCrypt hashing is used, but no minimum length, complexity, or rotation requirements are enforced at registration/change time. | Add password policy validation (min 8 chars, uppercase, digit, special character). |

### 1.4 Recommended Enhancement Roadmap

| Priority | Enhancement | Effort | Impact |
|---|---|---|---|
| P0 (Immediate) | Suspense GL implementation | 2-3 days | Blocks production deployment without safe error routing |
| P0 (Immediate) | Inter-branch clearing GL | 2-3 days | Required for multi-branch operations |
| P1 (Next sprint) | Dedicated SYSTEM_AUTO user for STP | 0.5 day | Cleaner audit trail for auto-authorized vouchers |
| P1 (Next sprint) | Per-role transaction amount limits | 1-2 days | Prevents unauthorized high-value transactions |
| P1 (Next sprint) | Scheduled ledger-vs-cache validator | 1 day | Detects balance drift between EODs |
| P2 (Backlog) | Velocity monitoring | 2-3 days | Fraud detection for rapid-fire transactions |
| P2 (Backlog) | Password policy enforcement | 1 day | Regulatory hygiene |
| P3 (Long-term) | Data archival strategy | 5+ days | Storage optimization and regulatory retention |

### 1.5 Integration Points Summary

| Ledgora Service | Governance Role | Key Method |
|---|---|---|
| `EodValidationService` | Daily balancing gate — blocks EOD on ANY inconsistency | `validateEod(tenantId, businessDate)` — 10+ checks |
| `SettlementService` | Per-tenant settlement — validates ledger, closes/settles batches, advances date | `processSettlement(date)` — 8-step workflow |
| `BatchService` | Batch integrity — enforces `debit == credit` before close/settle | `validateBatchClose()`, `settleAllBatches()` |
| `VoucherService` | Accounting control — voucher lifecycle with maker-checker | `createVoucher()` → `authorizeVoucher()` → `postVoucher()` |
| `TenantService` | Business day authority — per-tenant OPEN/DAY_CLOSING/CLOSED | `validateBusinessDayOpen()`, `startDayClosing()`, `closeDayAndAdvance()` |
| `AuditService` | Audit trail — persistent logging of all financial events | `logEvent()`, `logFinancialEvent()`, `logChangeEvent()` |
| `CbsGlBalanceService` | GL integrity — branch + tenant level GL balance tracking | `isTenantGlBalanced()`, `isBranchGlBalanced()` |
| `DayBeginService` | Day Begin ceremony — validates previous day closed, batches settled | `validateDayBegin()`, `openDay()` |

---

*End of Part 1.*

---

## PART 2 — LEDGER INTEGRITY SQL AUDIT PACK

All queries are read-only, safe for production execution, and compatible with both H2 (dev) and SQL Server (prod).
Run these via H2 console (`/h2-console` with dev profile) or SQL Server Management Studio.

### 2.1 Overall Debit vs Credit Validation

**Purpose:** Verify the fundamental accounting equation holds across ALL ledger entries.

```sql
-- AUDIT-01: Global ledger balance check
-- Expected: total_debits == total_credits (difference = 0)
SELECT
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) AS total_debits,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS total_credits,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END)
      - SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS difference,
    CASE
        WHEN SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END)
           = SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END)
        THEN 'PASS' ELSE 'FAIL — LEDGER IMBALANCED'
    END AS audit_result
FROM ledger_entries;
```

### 2.2 Per Business Date Balance Check

**Purpose:** Verify double-entry holds for each individual business date. Detects date-specific corruption.

```sql
-- AUDIT-02: Per-date ledger balance
-- Expected: every row shows difference = 0
SELECT
    business_date,
    tenant_id,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) AS day_debits,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS day_credits,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END)
      - SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS difference,
    CASE
        WHEN SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END)
           = SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END)
        THEN 'BALANCED' ELSE 'IMBALANCED'
    END AS status
FROM ledger_entries
GROUP BY business_date, tenant_id
ORDER BY business_date DESC, tenant_id;
```

### 2.3 Voucher vs Ledger Mismatch Detection

**Purpose:** Detect vouchers marked POSTED (post_flag=Y) that have no corresponding ledger entry, or ledger entries with no voucher linkage.

```sql
-- AUDIT-03a: POSTED vouchers with no ledger entry
-- Expected: 0 rows
SELECT v.id AS voucher_id, v.voucher_number, v.dr_cr,
       v.transaction_amount, v.post_flag, v.ledger_entry_id
FROM vouchers v
WHERE v.post_flag = 'Y'
  AND v.cancel_flag = 'N'
  AND v.ledger_entry_id IS NULL;

-- AUDIT-03b: Ledger entries with voucher_id that references a non-POSTED voucher
-- Expected: 0 rows
SELECT le.id AS ledger_entry_id, le.voucher_id, le.amount, le.entry_type,
       v.post_flag, v.cancel_flag
FROM ledger_entries le
JOIN vouchers v ON v.id = le.voucher_id
WHERE v.post_flag != 'Y'
  AND le.voucher_id IS NOT NULL;

-- AUDIT-03c: Voucher amount vs ledger entry amount mismatch
-- Expected: 0 rows
SELECT v.id AS voucher_id, v.voucher_number,
       v.transaction_amount AS voucher_amount,
       le.amount AS ledger_amount,
       v.transaction_amount - le.amount AS difference
FROM vouchers v
JOIN ledger_entries le ON le.id = v.ledger_entry_id
WHERE v.post_flag = 'Y'
  AND v.cancel_flag = 'N'
  AND v.transaction_amount != le.amount;
```

### 2.4 Orphan Ledger Entry Detection

**Purpose:** Find ledger entries not linked to any transaction or journal. Orphans indicate posting engine defects.

```sql
-- AUDIT-04a: Ledger entries with no transaction
-- Expected: 0 rows
SELECT le.id, le.amount, le.entry_type, le.business_date, le.narration
FROM ledger_entries le
WHERE le.transaction_id IS NULL;

-- AUDIT-04b: Ledger entries with no journal
-- Expected: 0 rows
SELECT le.id, le.amount, le.entry_type, le.business_date, le.narration
FROM ledger_entries le
WHERE le.journal_id IS NULL;

-- AUDIT-04c: Journals with no entries (empty journals)
-- Expected: 0 rows
SELECT lj.id AS journal_id, lj.description, lj.business_date
FROM ledger_journals lj
LEFT JOIN ledger_entries le ON le.journal_id = lj.id
WHERE le.id IS NULL;
```

### 2.5 Batch Imbalance Detection

**Purpose:** Find batches where total_debit != total_credit. These should never exist in CLOSED or SETTLED state.

```sql
-- AUDIT-05: Unbalanced batches
-- Expected: 0 rows for CLOSED/SETTLED batches
SELECT tb.id AS batch_id, tb.batch_code, tb.batch_type, tb.status,
       tb.business_date, tb.tenant_id,
       tb.total_debit, tb.total_credit,
       tb.total_debit - tb.total_credit AS imbalance,
       tb.transaction_count
FROM transaction_batches tb
WHERE tb.total_debit != tb.total_credit
ORDER BY tb.status, tb.business_date DESC;
```

### 2.6 Suspense GL Balance Check

**Purpose:** Verify the Internal Suspense Account has zero balance. Non-zero indicates unresolved postings.

```sql
-- AUDIT-06: Suspense account balance
-- Expected: balance = 0 for all suspense accounts
SELECT a.id, a.account_number, a.account_name, a.balance,
       CASE WHEN a.balance = 0 THEN 'CLEAR' ELSE 'NON-ZERO — INVESTIGATE' END AS status
FROM accounts a
WHERE a.account_type = 'INTERNAL_ACCOUNT'
   OR a.account_number LIKE '%SUSP%'
ORDER BY a.balance DESC;
```

### 2.7 Inter-Branch Clearing Imbalance Check

**Purpose:** Verify clearing accounts net to zero. Non-zero indicates incomplete inter-branch settlement.

```sql
-- AUDIT-07: Clearing account balance check
-- Expected: balance = 0 for all clearing accounts
SELECT a.id, a.account_number, a.account_name, a.balance, a.branch_code,
       CASE WHEN a.balance = 0 THEN 'BALANCED' ELSE 'IMBALANCED — INVESTIGATE' END AS status
FROM accounts a
WHERE a.account_type = 'CLEARING_ACCOUNT'
   OR a.account_number LIKE '%CLR%'
ORDER BY ABS(a.balance) DESC;
```

### 2.8 Same-Maker-Checker Detection (Segregation of Duty Violation)

**Purpose:** Find vouchers where maker_id == checker_id AND the authorization was NOT system-auto. This indicates a segregation-of-duty bypass.

```sql
-- AUDIT-08: Same maker-checker vouchers (excluding system auto-auth)
-- Expected: 0 rows (all matches should have SYSTEM_AUTO in authorization_remarks)
SELECT v.id AS voucher_id, v.voucher_number,
       v.maker_id, v.checker_id,
       m.username AS maker_username,
       c.username AS checker_username,
       v.authorization_remarks,
       v.transaction_amount, v.dr_cr, v.posting_date,
       CASE
           WHEN v.authorization_remarks LIKE '%SYSTEM_AUTO%' THEN 'STP — ACCEPTABLE'
           ELSE 'VIOLATION — MANUAL SAME-USER APPROVAL'
       END AS audit_result
FROM vouchers v
JOIN users m ON m.id = v.maker_id
JOIN users c ON c.id = v.checker_id
WHERE v.maker_id = v.checker_id
  AND v.auth_flag = 'Y'
  AND v.cancel_flag = 'N'
ORDER BY v.posting_date DESC, v.id;
```

### 2.9 Backdated Posting Detection

**Purpose:** Find vouchers or ledger entries where the posting date does not match the tenant's business date at the time of creation. Backdated entries require special approval.

```sql
-- AUDIT-09a: Vouchers where posting_date != entry_date (backdated creation)
-- Expected: 0 rows in normal operations
SELECT v.id AS voucher_id, v.voucher_number,
       v.entry_date, v.posting_date,
       v.entry_date - v.posting_date AS date_gap_days,
       v.transaction_amount, v.dr_cr,
       m.username AS maker
FROM vouchers v
JOIN users m ON m.id = v.maker_id
WHERE v.entry_date != v.posting_date
  AND v.cancel_flag = 'N'
ORDER BY ABS(DATEDIFF(DAY, v.entry_date, v.posting_date)) DESC;

-- AUDIT-09b: Ledger entries posted after business date advanced
-- (entry created_at timestamp is after the business_date + 1 day)
-- Expected: 0 rows
SELECT le.id, le.business_date, le.created_at, le.amount, le.entry_type,
       le.narration
FROM ledger_entries le
WHERE CAST(le.created_at AS DATE) > le.business_date
ORDER BY le.created_at DESC;
```

### 2.10 Duplicate Transaction Detection

**Purpose:** Find transactions with identical amounts, accounts, and dates that may indicate duplicate processing.

```sql
-- AUDIT-10a: Exact duplicate transactions (same ref)
-- Expected: 0 rows (transaction_ref is unique, but check for near-duplicates)
SELECT t.transaction_ref, COUNT(*) AS occurrences
FROM transactions t
GROUP BY t.transaction_ref
HAVING COUNT(*) > 1;

-- AUDIT-10b: Suspicious near-duplicates (same amount + same account + same date + within 60 seconds)
-- Expected: review each match manually
SELECT t1.id AS txn1_id, t2.id AS txn2_id,
       t1.transaction_ref AS ref1, t2.transaction_ref AS ref2,
       t1.amount, t1.transaction_type,
       t1.business_date,
       t1.created_at AS time1, t2.created_at AS time2
FROM transactions t1
JOIN transactions t2
  ON t1.amount = t2.amount
  AND t1.transaction_type = t2.transaction_type
  AND t1.business_date = t2.business_date
  AND t1.tenant_id = t2.tenant_id
  AND t1.id < t2.id
  AND (t1.source_account_id = t2.source_account_id
       OR t1.destination_account_id = t2.destination_account_id)
WHERE t1.status != 'REJECTED' AND t2.status != 'REJECTED'
ORDER BY t1.business_date DESC, t1.amount DESC;

-- AUDIT-10c: Idempotency key collision check
-- Expected: 0 rows
SELECT idempotency_key, COUNT(*) AS occurrences
FROM idempotency_keys
GROUP BY idempotency_key
HAVING COUNT(*) > 1;
```

### 2.11 Audit Pack Execution Checklist

| # | Query | Run Frequency | Acceptable Result | Action on Failure |
|---|---|---|---|---|
| 01 | Global ledger balance | Daily (pre-EOD) | difference = 0 | **BLOCK EOD** — escalate to Operations Head |
| 02 | Per-date balance | Daily (pre-EOD) | All dates balanced | Investigate specific date; check for orphan entries |
| 03 | Voucher-ledger mismatch | Daily | 0 rows all 3 queries | Investigate posting engine; check VoucherService logs |
| 04 | Orphan ledger entries | Weekly | 0 rows all 3 queries | Data remediation required; root cause analysis |
| 05 | Batch imbalance | Daily (pre-EOD) | 0 unbalanced CLOSED/SETTLED | Block batch settlement; recount totals |
| 06 | Suspense GL balance | Daily (pre-EOD) | balance = 0 | Park resolution required before EOD |
| 07 | Clearing imbalance | Daily (pre-EOD) | balance = 0 | Inter-branch reconciliation required |
| 08 | Same maker-checker | Weekly | 0 non-STP violations | Disciplinary review; access audit |
| 09 | Backdated postings | Weekly | 0 rows | Investigate authorization chain |
| 10 | Duplicate transactions | Daily | 0 exact dupes; review near-dupes | Reversal + root cause |

---

*End of Part 2.*

---

## PART 3 — SUSPENSE GL + EXCEPTION HANDLING MODEL

### 3.1 Problem Statement

When a financial operation partially fails (e.g., debit succeeds but credit fails due to account freeze, or an inter-system timeout), the system must not silently discard the imbalance. CBS standards require a **Suspense GL** to temporarily park the orphaned leg until manual correction.

Currently Ledgora has `INT-SUSP-001` (Internal Suspense Account) seeded in `DataInitializer` with `AccountType.INTERNAL_ACCOUNT`, but:
- No posting logic routes failed legs to it
- No EOD check validates its balance is zero
- No correction voucher workflow exists

### 3.2 Suspense GL Architecture

```
Normal Posting:
  DR Customer Account  ──→  CR Cash GL        (balanced, no suspense)

Failed Posting (credit leg fails):
  DR Customer Account  ──→  CR SUSPENSE GL    (parked temporarily)
  [Exception logged with reason code]

Correction (manual adjustment voucher):
  DR SUSPENSE GL       ──→  CR Cash GL        (clears suspense to zero)
```

**Key Rule:** EOD MUST NOT proceed if any tenant's Suspense GL balance ≠ 0 (configurable).

### 3.3 Design — Per-Tenant Suspense GL

Each tenant gets its own suspense account. The existing `INT-SUSP-001` serves TENANT-001. For multi-tenant:

| Tenant | Suspense Account | GL Code |
|---|---|---|
| TENANT-001 | `SUSP-T001` | `2900` (Other Liabilities → Suspense) |
| TENANT-002 | `SUSP-T002` | `2900` |

**GL Hierarchy Addition:**
```
2000 Liabilities
  └── 2400 Other Liabilities
        └── 2900 Suspense GL (NEW)
```

### 3.4 Posting Engine Modification (Pseudocode)

The change is in `VoucherService.postVoucher()` — wrap the credit/debit posting in a try-catch that routes to suspense on failure:

```java
// Inside VoucherService.postVoucher() — CONCEPTUAL (not literal code change)
try {
    // Normal posting: create LedgerJournal + LedgerEntry
    // Update actual balance via CbsBalanceEngine
    // Update GL balance via GlBalanceService
} catch (PostingException e) {
    // SUSPENSE ROUTING:
    // 1. Post the successful leg normally
    // 2. Post the failed leg against SUSPENSE GL instead of target
    // 3. Log exception with reason code
    // 4. Mark voucher with suspense_flag = 'Y' and suspense_reason
    // 5. Create SuspenseEntry record for tracking

    Account suspenseAccount = resolveSuspenseAccount(tenant.getId());
    // Create ledger entry: DR/CR suspenseAccount (opposite of failed leg)
    // This keeps double-entry balanced even though the real target failed

    auditService.logEvent(userId, "VOUCHER_SUSPENSE_ROUTED", "VOUCHER", voucher.getId(),
        "Routed to suspense: " + e.getMessage(), null);
}
```

### 3.5 Entity Changes Required

**Option A (Minimal — recommended first):** Add fields to existing Voucher entity:

```java
// On Voucher.java — new fields
@Column(name = "suspense_flag", length = 1, nullable = false)
@Builder.Default
private String suspenseFlag = "N";

@Column(name = "suspense_reason", length = 500)
private String suspenseReason;

@Column(name = "suspense_cleared_at")
private LocalDateTime suspenseClearedAt;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "correction_voucher_id")
private Voucher correctionVoucher;
```

**Option B (Full — for production):** Create a dedicated `SuspenseEntry` entity:

```java
@Entity
@Table(name = "suspense_entries")
public class SuspenseEntry {
    @Id @GeneratedValue private Long id;
    @ManyToOne private Tenant tenant;
    @ManyToOne private Voucher originalVoucher;
    @ManyToOne private Account suspenseAccount;
    @ManyToOne private Account intendedAccount;
    private BigDecimal amount;
    private String reasonCode;       // ACCOUNT_FROZEN, TIMEOUT, INSUFFICIENT_GL, etc.
    private String reasonDetail;
    private String status;           // OPEN, CLEARED, WRITTEN_OFF
    @ManyToOne private Voucher correctionVoucher;
    private LocalDate businessDate;
    private LocalDateTime createdAt;
    private LocalDateTime clearedAt;
}
```

### 3.6 EOD Validation Enhancement

Add to `EodValidationService.validateEod()`:

```java
// NEW EOD CHECK: Suspense GL must be zero before day close
BigDecimal suspenseBalance = accountRepository
    .sumBalanceByTenantIdAndAccountType(tenantId, AccountType.INTERNAL_ACCOUNT);
if (suspenseBalance.compareTo(BigDecimal.ZERO) != 0) {
    errors.add("EOD blocked: Suspense GL balance is " + suspenseBalance
        + " for tenant " + tenantId
        + ". All suspense entries must be cleared before EOD.");
}
```

This integrates with the existing EOD gate pattern — the check returns an error string, and EOD is blocked if any errors exist.

### 3.7 Correction Workflow

**Step 1 — Identify:** Operations officer reviews suspense entries via `/suspense/pending` screen.

**Step 2 — Create correction voucher:**
```
Original (parked):  DR CustomerAccount    CR SuspenseGL    (amount X)
Correction:         DR SuspenseGL         CR CashGL        (amount X)
```

The correction voucher:
- Must go through maker-checker approval
- Links back to the original via `correctionVoucher` FK
- Marks the suspense entry as CLEARED
- Updates `suspenseClearedAt` timestamp

**Step 3 — Verify:** Suspense GL balance returns to zero. EOD can now proceed.

### 3.8 Example Scenario: Transfer Failure → Suspense → Correction

**Scenario:** Rajesh transfers ₹10,000 to Priya. Priya's account has `freezeLevel = CREDIT_ONLY` (credit freeze active).

**Step 1 — Transfer attempt:**
```
Transaction TRF-001 initiated by teller1
  Voucher V1 (DR): Rajesh SAV-1001-0001, amount=10000  → POSTED OK
  Voucher V2 (CR): Priya SAV-1002-0001, amount=10000   → FAILS (CREDIT_FROZEN)
```

**Step 2 — Suspense routing (automatic):**
```
  Voucher V2-SUSP (CR): SUSP-T001, amount=10000        → POSTED to suspense
  SuspenseEntry created: reason=ACCOUNT_CREDIT_FROZEN, status=OPEN
```

Ledger is balanced: DR Rajesh 10000, CR Suspense 10000.

**Step 3 — Correction (next day, after freeze lifted):**
```
Operations officer creates correction voucher pair:
  Voucher V3 (DR): SUSP-T001, amount=10000              → Clears suspense
  Voucher V4 (CR): Priya SAV-1002-0001, amount=10000    → Posts to intended account
  SuspenseEntry updated: status=CLEARED, correctionVoucher=V3
```

Suspense GL returns to zero. EOD can proceed.

### 3.9 SQL — Suspense Monitoring Query

```sql
-- Suspense entries requiring resolution
SELECT a.account_number, a.account_name, a.balance AS suspense_balance,
       a.tenant_id,
       CASE WHEN a.balance = 0 THEN 'CLEAR' ELSE 'PENDING RESOLUTION' END AS status
FROM accounts a
WHERE (a.account_type = 'INTERNAL_ACCOUNT' OR a.account_number LIKE '%SUSP%')
  AND a.balance != 0
ORDER BY ABS(a.balance) DESC;
```

### 3.10 Integration Summary

| Component | Change Required |
|---|---|
| `DataInitializer` | Seed `GL 2900` (Suspense) + per-tenant suspense accounts |
| `VoucherService.postVoucher()` | Try-catch wrapper routing failed legs to suspense |
| `Voucher` entity | Add `suspenseFlag`, `suspenseReason`, `correctionVoucher` fields |
| `EodValidationService` | Add suspense GL balance ≠ 0 check |
| `VoucherController` | Add `/suspense/pending` screen for operations |
| `AuditService` | Log `VOUCHER_SUSPENSE_ROUTED` and `SUSPENSE_CLEARED` events |

**Invariants preserved:**
- ✅ Ledger remains balanced (suspense is a real GL, entries are double-entry)
- ✅ Ledger entries remain immutable (correction creates NEW entries)
- ✅ EOD still blocks on inconsistency (suspense ≠ 0 is a blocking error)
- ✅ Batch totals unaffected (suspense posting updates batch like any other)

---

*End of Part 3. Parts 4-5 follow.*
