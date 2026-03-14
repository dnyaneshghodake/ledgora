# Ledgora CBS — End-to-End Flow Documentation

This document details step-by-step flows for all major CBS operations, including service layer calls, entity transitions, database impact, ledger movements, and state transitions.

---

## Table of Contents

1. [Customer Deposit](#1-customer-deposit)
2. [Cross-Branch IBT](#2-cross-branch-ibt)
3. [Suspense Creation and Resolution](#3-suspense-creation-and-resolution)
4. [Teller Day Lifecycle](#4-teller-day-lifecycle)
5. [EOD Lifecycle](#5-eod-lifecycle)
6. [Reversal Scenario](#6-reversal-scenario)
7. [Fraud Detection Scenario](#7-fraud-detection-scenario)

---

## 1. Customer Deposit

### Flow Summary
A teller deposits cash into a customer's savings account.

### Step-by-Step

```
Step 1: API Request
  POST /api/transactions/deposit
  Body: { destinationAccountNumber: "SAV-1001-0001", amount: 25000, channel: "TELLER" }
```

**Step 2: TransactionService.deposit(dto)**
- Validates business day is OPEN via `TenantService.validateBusinessDayOpen()`
- Resolves destination account via `AccountRepository.findByAccountNumber()`
- Checks idempotency via `IdempotencyService.checkAndMark(clientReferenceId, channel)`
- Runs fraud velocity check via `FraudDetectionService.checkVelocity()`

**Step 3: Voucher Creation**
- `VoucherService.createVoucherPair()` creates:
  - **DR Voucher**: Debit Cash GL (1100) — cash comes into the bank
  - **CR Voucher**: Credit Customer Account GL — liability to depositor increases
- Both vouchers: auth_flag=N, post_flag=N

**Step 4: System Auto-Authorization**
- `VoucherService.systemAuthorizeVoucher()` uses SYSTEM_AUTO as checker
- auth_flag → Y for both vouchers

**Step 5: Voucher Posting**
- `VoucherService.postVoucher()` for each voucher:
  - Creates `LedgerJournal` linked to the transaction
  - Creates `LedgerEntry` (DR) and `LedgerEntry` (CR)
  - Updates actual balance via `CbsBalanceService`
  - Updates GL balance via `GlBalanceService`
  - Sets post_flag → Y

**Step 6: Balance Impact**

```
  +-------------------+--------+--------+
  |     Account       | Debit  | Credit |
  +-------------------+--------+--------+
  | Cash GL (1100)    | 25,000 |        |
  | Savings GL (2110) |        | 25,000 |
  +-------------------+--------+--------+
  | Total             | 25,000 | 25,000 |  <-- DR = CR
  +-------------------+--------+--------+

  Customer Account Balance: +25,000
  Cash GL Balance: +25,000 (asset increases)
```

### Database Impact
| Table | Action | Key Fields |
|-------|--------|------------|
| `transactions` | INSERT | transactionRef, type=DEPOSIT, status=COMPLETED |
| `transaction_batches` | UPDATE | totalDebit+25000, totalCredit+25000, count+1 |
| `vouchers` | INSERT x2 | DR voucher + CR voucher, auth=Y, post=Y |
| `ledger_journals` | INSERT | linked to transaction |
| `ledger_entries` | INSERT x2 | DR(25000) + CR(25000), balanced |
| `account_balances` | UPDATE | ledgerBalance+25000, actualTotalBalance+25000 |
| `audit_logs` | INSERT x3 | VOUCHER_CREATED, VOUCHER_AUTHORIZED, VOUCHER_POSTED |
| `idempotency_keys` | INSERT | clientReferenceId+channel |

---

## 2. Cross-Branch IBT

### Flow Summary
Transfer 10,000 from a customer at BR_PUNE to a customer at BR_NAGPUR using the 4-voucher clearing model.

### Step-by-Step

```
Step 1: API Request
  POST /api/transactions/transfer
  Body: { sourceAccountNumber: "SAV-BR001-0001",
          destinationAccountNumber: "SAV-BR002-0001",
          amount: 10000, channel: "TELLER" }
```

**Step 2: TransactionService.transfer(dto)**
- Detects cross-branch transfer (source.branch != destination.branch)
- Delegates to `IbtService.initiateIbt()`

**Step 3: 4-Voucher Creation**

```
  Source Branch (BR_PUNE):
    V1: DR Customer Account SAV-BR001-0001  (debit sender)       10,000
    V2: CR IBC_OUT GL                        (credit clearing)    10,000

  Destination Branch (BR_NAGPUR):
    V3: DR IBC_IN GL                         (debit clearing)     10,000
    V4: CR Customer Account SAV-BR002-0001   (credit receiver)    10,000
```

**Step 4: Authorization & Posting**
- All 4 vouchers authorized (SYSTEM_AUTO or maker-checker)
- All 4 vouchers posted → ledger entries created

**Step 5: Settlement**
- `InterBranchClearingService.settle()` marks IBT as SETTLED
- Clearing GL accounts (IBC_OUT + IBC_IN) net to zero

### Ledger Movement

```
  +---------------------------+--------+--------+
  |        Account            | Debit  | Credit |
  +---------------------------+--------+--------+
  | SAV-BR001-0001 (Sender)   | 10,000 |        |
  | IBC_OUT GL (Source)        |        | 10,000 |
  | IBC_IN GL (Dest)           | 10,000 |        |
  | SAV-BR002-0001 (Receiver)  |        | 10,000 |
  +---------------------------+--------+--------+
  | Total                      | 20,000 | 20,000 |  <-- DR = CR
  +---------------------------+--------+--------+

  Net Clearing: IBC_OUT(-10,000) + IBC_IN(+10,000) = 0  <-- Nets to zero
```

### State Transitions
```
  InterBranchTransfer: INITIATED → IN_PROGRESS → SETTLED
  Source Account Balance: -10,000
  Destination Account Balance: +10,000
  Clearing GL: Net = 0 (invariant maintained)
```

---

## 3. Suspense Creation and Resolution

### Flow Summary
A transfer fails at the credit leg (destination account frozen). The failed leg is parked to Suspense GL, then resolved later.

### Step 3a: Suspense Creation

```
  Normal attempt:
    DR Source Account    10,000  (succeeds)
    CR Dest Account      10,000  (FAILS — account frozen)

  Recovery:
    CR Suspense GL       10,000  (parking the failed credit)
```

**Service Calls:**
1. `TransactionService.transfer()` attempts credit leg
2. Credit fails with `AccountFrozenException`
3. `SuspenseService.parkToSuspense()` creates:
   - CR Voucher to Suspense GL (replacing failed credit)
   - `SuspenseCase` entity with status=OPEN

### Step 3b: Suspense Resolution

```
  Resolution (retry success):
    DR Suspense GL       10,000  (unpark)
    CR Dest Account      10,000  (original credit now succeeds)
```

**Service Calls:**
1. `SuspenseResolutionService.resolveCase()` triggered by ops team
2. Validates destination account is now active
3. Creates resolution voucher pair (DR Suspense, CR Destination)
4. Updates SuspenseCase: status → RESOLVED, resolvedBy, resolvedAt

### State Transitions
```
  SuspenseCase:  OPEN ──→ RESOLVED
                   |
                   └──→ REVERSED (if reversal instead of retry)

  Suspense GL Balance:
    After parking:    +10,000
    After resolution: 0 (nets to zero — required for EOD)
```

### Database Impact
| Table | Action | Key Fields |
|-------|--------|------------|
| `suspense_cases` | INSERT | status=OPEN, reasonCode=ACCOUNT_FROZEN |
| `suspense_cases` | UPDATE | status=RESOLVED, resolvedBy, resolvedAt |
| `vouchers` | INSERT x2 | Parking voucher + Resolution voucher pair |
| `ledger_entries` | INSERT x4 | Original DR, Suspense CR, Resolution DR, Final CR |

---

## 4. Teller Day Lifecycle

### Flow Summary
Complete teller session lifecycle: opening → transactions → closing with denomination reconciliation.

### Step 4a: Session Opening

**Service Calls:**
1. `TellerSessionService.openSession()` validates:
   - TellerMaster exists and is ASSIGNED
   - No existing OPEN session for this teller
   - Business day is OPEN
2. Creates `TellerSession` with status=OPEN
3. Records opening denomination counts

```
  Opening Denomination:
    ₹2000 x 10 = ₹20,000
    ₹500  x 40 = ₹20,000
    ₹100  x 100 = ₹10,000
    Total Opening Cash: ₹50,000
```

### Step 4b: Transactions During Session

```
  Deposit:  Cash In  +25,000 → Teller cash increases
  Withdrawal: Cash Out -10,000 → Teller cash decreases
  Net Cash Movement: +15,000
```

### Step 4c: Session Closing

**Service Calls:**
1. `TellerSessionService.closeSession()` captures closing denominations
2. Validates: closingCash = openingCash + cashIn - cashOut
3. Records closing denomination counts
4. Updates session status → CLOSED

```
  Closing Denomination:
    ₹2000 x 15 = ₹30,000
    ₹500  x 50 = ₹25,000
    ₹100  x 100 = ₹10,000
    Total Closing Cash: ₹65,000

  Reconciliation:
    Opening:     ₹50,000
    + Cash In:   +₹25,000
    - Cash Out:  -₹10,000
    = Expected:  ₹65,000
    = Actual:    ₹65,000  ✓ Balanced
```

### State Transitions
```
  TellerSession: OPEN → CLOSED
  TellerMaster: ASSIGNED → ACTIVE (on open) → ASSIGNED (on close)
```

---

## 5. EOD Lifecycle

### Flow Summary
End-of-day processing: validate → close day → settle batches → advance date.

### Step-by-Step

**Step 1: Trigger EOD**
```
  POST /api/eod/run?tenantId=1
```

**Step 2: EodValidationService.validateEod()**
Checks all pre-conditions:
```
  [1] Unauthorized vouchers:        0  ✓
  [2] Approved-unposted vouchers:   0  ✓
  [3] Ledger DR = CR:               ✓
  [4] Voucher totals balanced:      ✓
  [5] Pending approvals:            0  ✓
  [6] Pending transactions:         0  ✓
  [7] IBC clearing settled:         ✓
  [8] Clearing GL net = 0:          ✓
  [9] Suspense GL net = 0:          ✓
  [10] Tenant GL balanced:          ✓
```

**Step 3: EodStateMachineService.executeEod()**

```
  Phase 1: VALIDATED
    → All validations passed
    → EodProcess created with status=RUNNING, phase=VALIDATED
    → COMMIT

  Phase 2: DAY_CLOSING
    → Tenant dayStatus → DAY_CLOSING
    → No new transactions allowed
    → COMMIT

  Phase 3: BATCH_CLOSED
    → All open batches closed
    → Batch totals finalized
    → COMMIT

  Phase 4: SETTLED
    → Inter-branch clearing settled
    → GL balances reconciled
    → COMMIT

  Phase 5: DATE_ADVANCED
    → Business date incremented by 1
    → Tenant dayStatus → OPEN
    → EodProcess status → COMPLETED
    → COMMIT
```

### Crash Recovery
```
  If crash between Phase 2 and Phase 3:
    → On restart, detect incomplete EodProcess (status=RUNNING)
    → Resume from Phase 3 (BATCH_CLOSED)
    → No work is lost — each phase committed independently
```

### State Transitions
```
  Tenant.dayStatus:  OPEN → DAY_CLOSING → OPEN (after advance)
  Tenant.businessDate: 2025-03-15 → 2025-03-16 (weekday skip logic)
  EodProcess.phase: VALIDATED → DAY_CLOSING → BATCH_CLOSED → SETTLED → DATE_ADVANCED
  EodProcess.status: RUNNING → COMPLETED
  Batch.status: OPEN → CLOSED → SETTLED
```

---

## 6. Reversal Scenario

### Flow Summary
Reverse a completed deposit transaction. The original ledger entries remain immutable; new contra entries are created.

### Step-by-Step

**Step 1: Request Reversal**
```
  POST /api/vouchers/{voucherId}/cancel
```

**Step 2: VoucherService.cancelVoucher()**
1. Validates original voucher is posted (post_flag=Y)
2. Validates original voucher is not already cancelled
3. Creates **reversal voucher** with opposite DR/CR direction
4. Creates **reversal ledger entries** (contra entries)
5. Updates account balance (reverses the original impact)
6. Marks original voucher: cancel_flag → Y

### Ledger Movement

```
  Original Deposit:
    +-------------------+--------+--------+
    | Cash GL (1100)    | 25,000 |        |
    | Savings GL (2110) |        | 25,000 |
    +-------------------+--------+--------+

  Reversal (new entries — original NOT modified):
    +-------------------+--------+--------+
    | Cash GL (1100)    |        | 25,000 |  ← opposite direction
    | Savings GL (2110) | 25,000 |        |  ← opposite direction
    +-------------------+--------+--------+

  Net Effect: Zero (original + reversal cancel out)
  Ledger entries: 4 total (2 original + 2 reversal) — all immutable
```

### Key Principle: Ledger Immutability
```
  WRONG: UPDATE ledger_entries SET amount = 0 WHERE id = 123  ← NEVER
  RIGHT: INSERT ledger_entries (contra entry)                  ← ALWAYS
```

### State Transitions
```
  Original Voucher: cancel_flag N → Y
  Reversal Voucher: New entry with auth_flag=Y, post_flag=Y
  Customer Account Balance: +25,000 → 0 (net effect)
  Audit Trail: 2 additional entries (VOUCHER_CANCELLED, REVERSAL_CREATED)
```

---

## 7. Fraud Detection Scenario

### Flow Summary
A customer makes 6 transactions within 30 minutes, triggering a velocity count fraud alert.

### Step-by-Step

**Step 1: Transactions 1-5 (normal)**
```
  09:00  Deposit  ₹5,000   → Velocity count: 1
  09:05  Deposit  ₹8,000   → Velocity count: 2
  09:10  Transfer ₹3,000   → Velocity count: 3
  09:20  Deposit  ₹12,000  → Velocity count: 4
  09:25  Deposit  ₹7,000   → Velocity count: 5 (at threshold)
```

**Step 2: Transaction 6 (triggers alert)**
```
  09:28  Deposit  ₹15,000  → Velocity count: 6 (EXCEEDS threshold of 5)
```

**Step 3: FraudDetectionService.checkVelocity()**
1. Counts transactions for account in last 30 minutes
2. Count (6) > threshold (5) → trigger alert
3. Creates `FraudAlert` entity:
   - alertType: VELOCITY_COUNT
   - status: OPEN
   - observedCount: 6
   - thresholdValue: "5 per 30min"
   - details: "Account SAV-1001-0001 exceeded velocity count..."

**Step 4: Transaction Decision**
```
  Soft alert (velocity count): Transaction PROCEEDS but alert is flagged
  Hard block (hard ceiling):   Transaction REJECTED and alert created
```

**Step 5: Alert Resolution**
```
  Risk Officer reviews (ROLE_RISK):
    → Views fraud dashboard
    → Investigates transaction pattern
    → Marks alert as RESOLVED or FALSE_POSITIVE

  FraudAlert.status: OPEN → ACKNOWLEDGED → RESOLVED
```

### Hard Ceiling Scenario
```
  Customer attempts: Deposit ₹50,000,000 (50M)
  Hard ceiling limit: ₹10,000,000 (10M)

  Result:
    → Transaction BLOCKED (not processed)
    → FraudAlert created: alertType=HARD_CEILING, status=OPEN
    → Customer informed: "Transaction exceeds maximum allowed amount"
```

### Database Impact
| Table | Action | Key Fields |
|-------|--------|------------|
| `fraud_alerts` | INSERT | alertType, status=OPEN, observedCount, thresholdValue |
| `audit_logs` | INSERT | FRAUD_ALERT_CREATED |
| `transactions` | INSERT (or BLOCKED) | Depends on soft/hard alert |

### State Transitions
```
  FraudAlert: (created) → OPEN → ACKNOWLEDGED → RESOLVED / FALSE_POSITIVE
  Transaction: COMPLETED (soft) or BLOCKED (hard ceiling)
```

---

## Appendix: Common Ledger Patterns

### Deposit
```
  DR Cash GL        CR Customer Deposit GL
```

### Withdrawal
```
  DR Customer Deposit GL    CR Cash GL
```

### Same-Branch Transfer
```
  DR Source Account GL    CR Destination Account GL
```

### Cross-Branch Transfer (IBT)
```
  Source Branch:  DR Source Account    CR IBC_OUT
  Dest Branch:   DR IBC_IN            CR Dest Account
  Settlement:    IBC_OUT + IBC_IN = 0
```

### Interest Posting
```
  DR Interest Expense GL (5100)    CR Customer Account GL
```

### Charge Deduction
```
  DR Customer Account GL    CR Fee Income GL (4200)
```

### Reversal
```
  Opposite of original: swap DR/CR directions
  Original entries remain — new contra entries created
```

---

*Ledgora CBS — Complete transaction lifecycle documentation for RBI-compliant core banking operations.*
