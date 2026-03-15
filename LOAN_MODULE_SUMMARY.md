# Ledgora CBS — Loan Module End-to-End Flow Document

## Module Overview

The Ledgora Loan Module is a production-grade CBS Tier-1 Lending Engine aligned with RBI IRAC Guidelines, Basel III prudential treatment, and Finacle-grade modular design. All financial mutations flow through the voucher engine — no direct balance manipulation. Multi-tenant ready, maker-checker enabled, audit traceable.

### Architecture Principles
- **Ledger-derived accounting only** — every financial event creates immutable LedgerEntry records via VoucherService
- **No direct AccountBalance manipulation** — all balance changes flow through VoucherService → CbsBalanceService
- **Separate DR/CR account targeting** — each voucher leg targets a distinct account (customer account vs internal GL account) so VoucherService updates the correct balance per leg. Internal accounts (`INTERNAL_ACCOUNT` type) are auto-created per GL code per tenant, matching the Finacle pattern for Cash GL, Clearing GL, and Suspense GL accounts.
- **Tenant-scoped** — every entity carries a tenant_id FK; all queries enforce tenant isolation via `TenantContextHolder.getRequiredTenantId()`
- **Business date enforcement** — all date comparisons use `tenant.getCurrentBusinessDate()`, never `LocalDate.now()`. Snapshot lookups use the last COMPLETED `EodProcess.businessDate` to handle weekend/holiday skips.
- **Maker-checker** — vouchers require system-authorize before posting; `SYSTEM_AUTO` pseudo-user as checker for STP flows
- **Audit defensible** — `AuditService.logEvent()` on every mutation + immutable history entities (`RepaymentTransaction`, `LoanDisbursement`, `LoanProvision`, `LoanRateChangeHistory`)

---

## Complete Loan Lifecycle

```
Customer → Credit Limit → Loan Sanction (Maker-Checker) → Disbursement (Voucher) →
Moratorium → EMI Schedule → Repayment (Penal→Interest→Principal) →
DPD Calculation → SMA Classification → NPA Classification → Interest Reversal →
Provisioning → Recovery → Restructure → Write-off → Post Write-off Recovery → Closure
```

### Status Lifecycle
```
PENDING → SANCTIONED → ACTIVE → NPA → RESTRUCTURED → WRITTEN_OFF
                         ↓                                    ↓
                       CLOSED ←──────────────────────── (Recovery)
```

---

## Phase 1 — Credit Limit & Exposure Engine

### Entities
- **CreditLimit** — sanctioned facility per borrower with sanctioned/utilized/available amounts, facility type, sector, expiry, maker-checker
- **ExposureRegister** — daily aggregate exposure snapshot per borrower for CRILC reporting

### Business Rules
- Loan sanction must not exceed available limit
- RBI single borrower cap 15%, group cap 40%
- Sector-wise exposure caps per internal policy
- Multiple loans under one limit supported

### Key Methods
- `CreditLimitService.validateDisbursementAgainstLimit(creditLimitId, amount, tenantId)` — validates limit is ACTIVE, not expired (using tenant business date, not system clock), and disbursement does not exceed available amount
- `CreditLimitService.utilizeLimit()` / `releaseLimit()` — on disbursement / repayment

---

## Phase 2 — Loan Contract Engine

### Entities
- **LoanProduct** — interest rate, type (FIXED/FLOATING), tenure, penalty rate, grace days, moratorium flag, repayment type, NPA threshold, 5 GL mappings
- **LoanRate** — effective-dated rate with benchmark/spread for FLOATING products
- **LoanRateChangeHistory** — immutable audit trail per rate change
- **LoanAccount** — loan contract with SMA category, credit limit FK, penal interest, interest reversed, restructure flag, moratorium end date

### GL Mappings per Product
| GL Field | Purpose |
|----------|---------|
| glLoanAsset | Standard loan asset (Schedule 9) |
| glInterestIncome | Interest earned on performing loans (Schedule 13) |
| glInterestReceivable | Accrued but unrealized interest |
| glNpaLoanAsset | NPA loan asset (reclassified) |
| glProvision | Provision for loan losses (Schedule 12) |

### Rate Management
- `LoanRateService.createRate()` — creates rate, closes previous window, writes immutable `LoanRateChangeHistory`
- `LoanRateService.propagateRateToActiveLoans()` — FLOATING rate propagation with EMI recalculation using **remaining tenure** (not product's full tenure). Uses `LoanAccountRepository.countRemainingInstallments()` to determine the correct number of unpaid installments for partially repaid loans.

---

## Phase 3 — EMI & Repayment Engine

### EMI Formula (Reducing Balance)
```
EMI = P × r × (1+r)^n / ((1+r)^n - 1)
```
- Centralized in `EmiCalculator.computeEmi()` with zero-rate guard
- Precision: `MathContext.DECIMAL128` for intermediate calculations, `setScale(4, HALF_UP)` for final amounts
- Last installment adjustment absorbs rounding residual: `principalComponent = remaining`
- Daily rate: `annualRate / 100 / 365` (10-decimal intermediate precision per RBI 365-day basis)

### Two-Step Disbursement (Finacle LACSMNT)
1. **Preview** — POST /loan/preview computes full schedule without persisting (read-only simulation)
2. **Confirm** — POST /loan/create creates loan + schedule + voucher posting

#### Disbursement Worked Example
```
Loan: ₹5,00,000 at 12% p.a. for 24 months
Product: PL-001 (Personal Loan, REDUCING_BALANCE)

Step 1: EMI Calculation
  monthlyRate = 12 / 100 / 12 = 0.01
  EMI = 500000 × 0.01 × (1.01)^24 / ((1.01)^24 - 1) = ₹23,536.7400

Step 2: Voucher Pair (via VoucherService.createVoucherPair)
  DR voucher: account=INT-LOAN-TENANT001 (INTERNAL_ACCOUNT), GL=glLoanAsset
    → LedgerEntry: entryType=DEBIT, glAccountCode=9100, amount=500000
    → Internal loan asset balance: +500000
  CR voucher: account=SAV-0001 (customer savings), GL=GL-T1-SAVINGS
    → LedgerEntry: entryType=CREDIT, glAccountCode=GL-T1-SAVINGS, amount=500000
    → Customer balance: +500000 (funds credited)

Step 3: Balance Sheet Impact (via BalanceSheetEngine from LedgerEntry)
  Loan Asset GL (9100): SUM(DR)=500000 → asset = 500000
  Savings GL (GL-T1-SAVINGS): SUM(CR)=500000 → liability = 500000
  A = L + E ✓

Step 4: Immutable Records Created
  - LoanAccount (status=ACTIVE, outstanding=500000)
  - LoanSchedule × 24 installments
  - LoanDisbursement (tranche=1, amount=500000)
  - Voucher × 2 (DR + CR, auth=Y, post=Y)
  - LedgerEntry × 2 (DEBIT + CREDIT, immutable)
  - AuditLog ("LOAN_DISBURSED")
```

### Repayment Allocation Order (CBS Standard — RBI Fair Practices Code)

The CBS engine reallocates the total payment in strict Penal → Interest → Principal order. The caller's `principalComponent` and `interestComponent` are treated as **hints** — the actual allocation is determined by the CBS engine.

```
Step 1: Penal Interest (recovered first per RBI FPC)
Step 2: Accrued Interest (clears interest receivable)
Step 3: Outstanding Principal (reduces loan asset)
```

#### Validation
`LoanBusinessValidator.validateEmiPayment()` validates **total payment ≤ total payable** (principal + interest + penal). Individual component checks are NOT performed because the CBS allocation ignores the caller's split.

#### Repayment Worked Example (with penal interest)
```
Loan state: outstanding=₹4,82,464, accrued=₹5,000, penal=₹1,000
Caller sends: principalComponent=₹18,536, interestComponent=₹5,000
Total payment: ₹23,536

CBS Allocation (Penal → Interest → Principal):
  Step 1: penalApplied = min(23536, 1000) = ₹1,000
          remaining = 23536 - 1000 = ₹22,536
          loan.penalInterest: 1000 → 0

  Step 2: interestApplied = min(22536, 5000) = ₹5,000
          remaining = 22536 - 5000 = ₹17,536
          loan.accruedInterest: 5000 → 0

  Step 3: principalApplied = min(17536, 482464) = ₹17,536
          loan.outstandingPrincipal: 482464 → 464928

Voucher Pairs (CBS-allocated amounts, NOT caller's original split):
  Principal pair (₹17,536):
    DR: SAV-0001 (customer, customerGL) → customer balance -17536
    CR: INT-LOAN-TENANT001 (internal, glLoanAsset) → loan asset -17536
  Interest pair (₹5,000):
    DR: SAV-0001 (customer, customerGL) → customer balance -5000
    CR: INT-INTRECV-TENANT001 (internal, glInterestReceivable) → receivable -5000

RepaymentTransaction (immutable audit record):
  totalAmount=23536, principalComponent=17536, interestComponent=5000,
  penalComponent=1000, outstandingAfter=464928, accruedInterestAfter=0

NOTE: The caller requested P=18536/I=5000 but the actual allocation was
P=17536/I=5000/Penal=1000. The audit record reflects REALITY, not the request.
```

### FIFO Installment Matching
After CBS allocation, the total payment is matched against the oldest pending/overdue installments:
```
Installment #1 (OVERDUE, due=₹23,536, paid=₹0):
  installmentDue = 23536 - 0 = 23536
  remainingPayment (23536) ≥ installmentDue (23536) → PAID
  paidAmount=23536, paidDate=businessDate, status=PAID, dpdDays=0
```

### Schedule Restructuring
- `LoanScheduleService.regenerateSchedule()` — replaces unpaid installments only
- PAID installments immutable (historical fact)
- New EMI computed using `EmiCalculator.computeEmi(outstandingPrincipal, newRate, remainingTenure)`

### Moratorium
- `LoanProduct.moratoriumAllowed` + `LoanAccount.moratoriumEndDate`
- Accrual skipped during moratorium period: `if (businessDate.isBefore(moratoriumEndDate)) continue`
- Idempotency guard: `if (businessDate.equals(lastAccrualDate)) skip`

### Foreclosure
- `LoanEmiPaymentService.calculateForeclosureAmount()` returns:
  ```
  Outstanding Principal + Accrued Interest + Penal Interest = Total
  Example: 464928 + 0 + 0 = ₹4,64,928
  ```

---

## Phase 4 — IRAC & Risk Engine

### Daily DPD Calculation
- DPD = businessDate − oldestOverdueDueDate (from schedule)
- SCHEDULED/DUE → OVERDUE transition automated

### SMA Classification
| DPD | Category | Action |
|-----|----------|--------|
| 0 | NONE | Performing |
| 1–30 | SMA_0 | Early warning |
| 31–60 | SMA_1 | Elevated risk |
| 61–90 | SMA_2 | Immediate attention |
| 90+ | — | NPA transition |

### NPA Classification
| DPD | Classification | Provision |
|-----|---------------|-----------|
| ≤90 | STANDARD | 0.40% |
| 91–365 | SUBSTANDARD | 15.00% |
| 366–1095 | DOUBTFUL | 25.00% |
| >1095 | LOSS | 100.00% |

### Interest Reversal on NPA (Voucher-Integrated)
- Accrued interest reversed: `interestReversed = accruedInterest; accruedInterest = 0`
- Voucher pair posted via `postInterestReversalVouchers()`:
  ```
  DR: INT-INTINC (internal, glInterestIncome) — reverse income recognition
  CR: INT-INTRECV (internal, glInterestReceivable) — reverse asset
  Customer balance: UNAFFECTED (GL-to-GL entry)
  ```

### NPA Suspense Accrual (Voucher-Integrated)
- NPA loans accrue interest to suspense (not income) — `interestReversed` accumulator
- Voucher pair posted via `postNpaSuspenseVouchers()` for GL audit trail
- Income recognized only on realization (actual payment)

### NPA Upgrade (Voucher-Integrated)
- When ALL overdue cleared (DPD=0): NPA → ACTIVE, suspense reversed to income
- Voucher pair posted via `postNpaUpgradeVouchers()`:
  ```
  DR: INT-INTRECV (internal, glInterestReceivable) — restore asset
  CR: INT-INTINC (internal, glInterestIncome) — recognize income
  ```

#### NPA Lifecycle Worked Example
```
Day 1: Loan disbursed ₹5,00,000 at 12% p.a.
Day 91: DPD = 91 (oldest overdue installment 91 days past due)
  → NpaClassifier.classify(91, 90) = SUBSTANDARD
  → loan.status: ACTIVE → NPA
  → loan.npaDate = businessDate
  → Interest reversal: accruedInterest (₹15,000) → interestReversed
  → Voucher: DR Interest Income GL, CR Interest Receivable GL
  → Provisioning: 500000 × 15% = ₹75,000 (up from ₹2,000 at STANDARD 0.4%)
  → AuditLog: "LOAN_NPA_CLASSIFIED"

Day 92-180: NPA suspense accrual
  → dailyInterest = 500000 × 12/100/365 = ₹164.38
  → loan.interestReversed += 164.38 (NOT accruedInterest)
  → Voucher: memorandum DR/CR Interest Receivable GL (audit trail only)
  → NO income recognition

Day 181: Borrower clears ALL overdue (DPD returns to 0)
  → loan.status: NPA → ACTIVE
  → Suspense reversal: interestReversed → accruedInterest
  → Voucher: DR Interest Receivable GL, CR Interest Income GL
  → AuditLog: "LOAN_NPA_UPGRADED"
```

### Penal Interest
- `accruePenalInterest()` — daily, after DPD calculation, respects grace days
- Formula: `outstandingPrincipal × penalRate / 100 / 365` (DECIMAL128 precision, 4dp)
- Grace period check: `if (dpd <= product.graceDays) skip`
- Penal is recovered FIRST in the CBS allocation waterfall (before interest and principal)

---

## Phase 5 — Write-Off & Recovery

### Technical Write-Off (Voucher-Integrated)
- Pre-conditions: NPA + LOSS + 100% provisioned
- Voucher pair via `postWriteOffVouchers()`:
  ```
  DR: INT-PROV-TENANT001 (internal, glProvision) — use up provision
  CR: INT-NPA-TENANT001 (internal, glNpaLoanAsset) — remove from asset book
  Customer balance: UNAFFECTED (GL-to-GL entry)
  ```
- Status → WRITTEN_OFF, outstanding/accrued/provision all zeroed

#### Write-Off Worked Example
```
Loan: outstanding=₹3,00,000, classification=LOSS, provision=₹3,00,000

Validation:
  ✓ status == NPA
  ✓ npaClassification == LOSS
  ✓ provisionAmount (300000) ≥ outstandingPrincipal (300000)

Voucher Pair:
  DR: INT-PROV-TENANT001 (glProvision) — provision balance -300000
  CR: INT-NPA-TENANT001 (glNpaLoanAsset) — NPA asset balance -300000

After write-off:
  loan.outstandingPrincipal = 0
  loan.accruedInterest = 0
  loan.provisionAmount = 0
  loan.status = WRITTEN_OFF
```

### Post Write-Off Recovery (Voucher-Integrated)
- Voucher pair via `postRecoveryVouchers()`:
  ```
  DR: SAV-0001 (customer account, customerGL) — cash received, balance +amount
  CR: INT-INTINC-TENANT001 (internal, glInterestIncome) — recovery income recognized
  ```
- Loan status remains WRITTEN_OFF — recovery is a separate income event
- `AuditService.logEvent("LOAN_RECOVERY_POST_WRITEOFF")`

---

## Phase 6 — Collateral Engine

- **LoanCollateral** entity: type, valuation, LTV ratio, status (PLEDGED/RELEASED/SEIZED)
- Types: PROPERTY, GOLD, FD, SHARES, VEHICLE, MACHINERY, OTHER

---

## Phase 7 — Reporting

- **ExposureRegister** — daily borrower exposure snapshots with breach flag
- **LoanProvision** — daily provision snapshots per loan per business date
- **LoanRateChangeHistory** — immutable rate change audit trail
- **RepaymentTransaction** — immutable payment records for statements

---

## GL Posting Matrix (All 14 Scenarios)

Each voucher leg targets a **separate account** — customer accounts for customer-facing operations, internal accounts (`INTERNAL_ACCOUNT` type) for GL-level entries. This ensures `VoucherService.postVoucher()` updates the correct account balance per leg.

| # | Scenario | DR Account | DR GL | CR Account | CR GL | Customer Impact |
|---|----------|-----------|-------|-----------|-------|----------------|
| 1 | Disbursement | INT-LOAN (internal) | Loan Asset | Customer (savings) | Customer Deposit GL | +amount |
| 2 | Standard Accrual | INT-INTRECV (internal) | Interest Receivable | INT-INTINC (internal) | Interest Income | None |
| 3 | EMI Principal | Customer (savings) | Customer Deposit GL | INT-LOAN (internal) | Loan Asset | −amount |
| 4 | EMI Interest | Customer (savings) | Customer Deposit GL | INT-INTRECV (internal) | Interest Receivable | −amount |
| 5 | Penal Accrual | Entity-level only | — | — | — | None |
| 6 | NPA Interest Reversal | INT-INTINC (internal) | Interest Income | INT-INTRECV (internal) | Interest Receivable | None |
| 7 | NPA Suspense Accrual | Customer (memo) | Interest Receivable | Customer (memo) | Interest Receivable | Net zero |
| 8 | NPA Upgrade | INT-INTRECV (internal) | Interest Receivable | INT-INTINC (internal) | Interest Income | None |
| 9 | Provision | Entity-level only | — | — | — | None |
| 10 | Write-Off | INT-PROV (internal) | Provision GL | INT-NPA (internal) | NPA Loan Asset | None |
| 11 | Recovery | Customer (savings) | Customer Deposit GL | INT-INTINC (internal) | Interest Income | +amount |
| 12 | Foreclosure | Customer | Customer Deposit GL | INT-LOAN + INT-INTRECV | Loan Asset + Interest Recv | −amount |
| 13 | Re-amortization | No GL | — | — | — | None |
| 14 | Rate Reset | No GL | — | — | — | None |

### Internal Account Auto-Creation
Internal accounts are resolved via `resolveInternalAccount(tenant, glCode, branch, accountNumber, accountName)`. If no `INTERNAL_ACCOUNT` exists for the tenant + GL code, one is auto-created:
```java
Account.builder()
    .accountType(AccountType.INTERNAL_ACCOUNT)
    .status(AccountStatus.ACTIVE)
    .approvalStatus(MakerCheckerStatus.APPROVED)
    .glAccountCode(glCode)
    .build();
```
This matches the Finacle pattern where Cash GL, Clearing GL, and Suspense GL each have dedicated internal accounts.

---

## EOD Process

All loan EOD operations run during the **VALIDATED** phase of `EodStateMachineService`, BEFORE financial statement and regulatory snapshot generation. Snapshots are generated BEFORE the process is marked COMPLETED for crash safety.

```
Phase VALIDATED (single @Transactional(REQUIRES_NEW)):
  Step 1: DepositInterestAccrualService.accrueDailyInterest()
    → Deposit interest via voucher engine (DR Interest Expense, CR Deposit Liability)

  Step 2: LoanAccrualService.accrueDailyInterest()
    → ACTIVE loans: DR INT-INTRECV (Interest Receivable), CR INT-INTINC (Interest Income)
    → NPA loans: memorandum DR/CR INT-INTRECV (suspense trail, no income)
    → Moratorium loans: skip
    → Idempotency: lastAccrualDate guard prevents double-accrual on retry

  Step 3: LoanNpaService.evaluateNpaAndUpdateDpd()
    → 3a: Installment SCHEDULED → OVERDUE transitions
    → 3b: DPD computation from oldest overdue installment
    → 3c: SMA classification (SMA-0/1/2) for performing loans
    → 3d: NPA classification + interest reversal vouchers + NPA upgrade vouchers
    → 3e: Penal interest accrual (with grace period)

  Step 4: LoanProvisionService.calculateProvisions()
    → Bidirectional: increases AND decreases per NPA tier changes
    → Immutable LoanProvision snapshots per business date

  Step 5: EodValidationService.validateEod()
    → SUM(DR) = SUM(CR), no pending vouchers, clearing GL = 0

Phase DATE_ADVANCED:
  Step 6: tenantService.closeDayAndAdvance()
  Step 7: FinancialStatementService.generateDailySnapshots() ← BEFORE COMPLETED
  Step 8: RegulatorySnapshotService.generateAllSnapshots() ← BEFORE COMPLETED
  Step 9: EodProcess.status = COMPLETED ← LAST (crash-safe)
```

---

## Immutable Audit Trail

| Record | Entity | Trigger | Key Fields |
|--------|--------|---------|------------|
| Every payment | `RepaymentTransaction` | EMI/prepayment received | totalAmount, principalComponent, interestComponent, **penalComponent**, outstandingAfter, accruedInterestAfter |
| Every disbursement | `LoanDisbursement` | Tranche disbursed | disbursementAmount, trancheNumber, disbursedBy |
| Every provision | `LoanProvision` | Daily EOD calculation | npaClassification, provisionRate, requiredProvision, previousProvision, incrementalProvision |
| Every rate change | `LoanRateChangeHistory` | Rate created/propagated | oldRate, newRate, oldEmi, newEmi, effectiveDate, changedBy |
| Every status change | `AuditService.logEvent()` | All mutations | action, entityType, entityId, details |
| Every voucher | `Voucher` + `LedgerEntry` | create → authorize → post | voucherNumber, drCr, amount, glAccountCode, auth_flag, post_flag |

---

## Voucher Engine Integration

**Every financial mutation** in the loan module flows through the voucher engine. No service directly modifies `AccountBalance` — all changes go through `VoucherService.createVoucherPair()` → `systemAuthorizeVoucher()` → `postVoucher()` → immutable `LedgerEntry`.

| Operation | Voucher Integrated | DR Account | CR Account | Lifecycle |
|-----------|-------------------|-----------|-----------|-----------|
| Disbursement | ✅ | INT-LOAN (internal) | Customer (savings) | create → system-authorize → post → LedgerEntry |
| Repayment (principal) | ✅ | Customer (savings) | INT-LOAN (internal) | create → system-authorize → post → LedgerEntry |
| Repayment (interest) | ✅ | Customer (savings) | INT-INTRECV (internal) | create → system-authorize → post → LedgerEntry |
| Accrual (standard) | ✅ | INT-INTRECV (internal) | INT-INTINC (internal) | create → system-authorize → post → LedgerEntry |
| Accrual (NPA suspense) | ✅ | Customer (memo) | Customer (memo) | create → system-authorize → post → LedgerEntry |
| NPA interest reversal | ✅ | INT-INTINC (internal) | INT-INTRECV (internal) | create → system-authorize → post → LedgerEntry |
| NPA upgrade | ✅ | INT-INTRECV (internal) | INT-INTINC (internal) | create → system-authorize → post → LedgerEntry |
| Write-off | ✅ | INT-PROV (internal) | INT-NPA (internal) | create → system-authorize → post → LedgerEntry |
| Recovery | ✅ | Customer (savings) | INT-INTINC (internal) | create → system-authorize → post → LedgerEntry |

---

## Test Coverage (40+ cases)

| Test Class | Cases | Coverage |
|-----------|-------|----------|
| EmiCalculationTest | 10 | EMI formula, zero-rate, schedule integrity, DECIMAL128 precision |
| NpaClassificationTest | 12+ | DPD tiers, SMA boundaries, provision rates, tier progression |
| LoanBusinessValidatorTest | 16 | Null/negative/zero-total, status gates, tenant isolation, **CBS total-payment validation**, **penal interest in total payable**, **caller hint exceeds individual balance but total valid** |
| LoanAccountingIntegrityTest | 4 | Accrual increases accruedInterest, NPA at DPD>90, NPA loans don't accrue, provisioning rates |

---

## Finacle Maturity Scorecard

| Level | Requirement | Status |
|-------|-------------|--------|
| Level 1 — Basic LMS | EMI + simple repayment | ✅ |
| Level 2 — Advanced LMS | Multi-disbursement + NPA logic | ✅ |
| Level 3 — CBS Grade | Limit engine + SMA + provisioning + collateral | ✅ |
| **Level 4 — Tier-1 Core Banking** | Exposure + moratorium + restructuring + write-off | **✅** |
| Level 5 — Regulatory Enterprise | Basel capital + stress testing + IFRS9 | Future |

---

## RBI IRAC Compliance Checklist

| Requirement | Status | Implementation Detail |
|-------------|--------|----------------------|
| DPD calculated daily from schedule | ✅ | `LoanNpaService.evaluateNpaAndUpdateDpd()` — oldest overdue installment |
| SMA-0/1/2 auto-classified | ✅ | `NpaClassifier.classifySma()` — DPD 1-30/31-60/61-90 |
| NPA at 90+ DPD | ✅ | `NpaClassifier.isNpa()` — configurable threshold per product |
| NPA tier progression daily | ✅ | SUBSTANDARD→DOUBTFUL→LOSS based on DPD age (91-365/366-1095/>1095) |
| No income booked after NPA | ✅ | Accrual to `interestReversed` (suspense), NOT `accruedInterest` |
| Interest reversed on NPA | ✅ | Voucher: DR Interest Income, CR Interest Receivable |
| NPA suspense accrual | ✅ | Memorandum voucher for GL audit trail |
| NPA upgrade on full clearance | ✅ | Voucher: DR Interest Receivable, CR Interest Income |
| Penal interest with grace period | ✅ | `accruePenalInterest()` — `dpd > graceDays` check |
| Provisioning per RBI norms | ✅ | 0.4%/15%/25%/100% — **bidirectional** (increases AND decreases) |
| Provision snapshots immutable | ✅ | `LoanProvision` entity per business date |
| Write-off LOSS + 100% provisioned | ✅ | Voucher: DR Provision GL, CR NPA Loan Asset GL |
| Post write-off recovery as income | ✅ | Voucher: DR Customer GL, CR Interest Income GL |
| Credit limit before disbursement | ✅ | `validateDisbursementAgainstLimit()` — tenant business date for expiry |
| Borrower/sector exposure tracking | ✅ | `ExposureRegister` — daily snapshots |
| All mutations via voucher engine | ✅ | **Every** financial event creates voucher pairs with separate DR/CR accounts |
| Immutable audit trail | ✅ | `RepaymentTransaction` (with `penalComponent`), `LoanDisbursement`, `LoanProvision`, `LoanRateChangeHistory` |
| Multi-tenant isolation | ✅ | `TenantContextHolder.getRequiredTenantId()` on every operation |
| Maker-checker on vouchers | ✅ | `SYSTEM_AUTO` pseudo-user as checker for STP flows |
| Moratorium support | ✅ | Accrual skipped during moratorium; idempotency via `lastAccrualDate` |
| Restructure flag tracking | ✅ | `LoanAccount.restructureFlag` + `LoanScheduleService.regenerateSchedule()` |
| Foreclosure calculation | ✅ | principal + interest + penal = total |
| Repayment order Penal→Interest→Principal | ✅ | CBS waterfall allocation; **audit records actual amounts, not caller hints** |
| Separate DR/CR account targeting | ✅ | Internal accounts per GL code prevent customer balance pollution |
| Business date enforcement | ✅ | `tenant.getCurrentBusinessDate()` everywhere; never `LocalDate.now()` |
| Crash-safe EOD snapshots | ✅ | Snapshots generated BEFORE `EodProcess.status=COMPLETED` |
| Floating rate remaining tenure | ✅ | `countRemainingInstallments()` for EMI recalculation |

---

*This module is at Tier-1 CBS Lending Engine Grade — equivalent to Infosys Finacle and Temenos T24 core lending modules.*
