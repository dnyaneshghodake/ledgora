# Ledgora CBS — Loan Module End-to-End Flow Document

## Module Overview

The Ledgora Loan Module is a production-grade CBS Tier-1 Lending Engine aligned with RBI IRAC Guidelines, Basel III prudential treatment, and Finacle-grade modular design. All financial mutations flow through the voucher engine — no direct balance manipulation. Multi-tenant ready, maker-checker enabled, audit traceable.

### Architecture Principles
- **Ledger-derived accounting only** — every financial event creates immutable LedgerEntry records via VoucherService
- **No direct AccountBalance manipulation** — all balance changes flow through VoucherService → CbsBalanceService
- **Tenant-scoped** — every entity carries a tenant_id FK; all queries enforce tenant isolation
- **Maker-checker** — vouchers require system-authorize before posting
- **Audit defensible** — AuditService.logEvent() on every mutation + immutable history entities

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
- `CreditLimitService.validateDisbursementAgainstLimit()` — active, not expired, available check
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
- `LoanRateService.createRate()` — creates rate, closes previous window, writes history
- `LoanRateService.propagateRateToActiveLoans()` — FLOATING rate propagation with EMI recalculation

---

## Phase 3 — EMI & Repayment Engine

### EMI Formula (Reducing Balance)
```
EMI = P × r × (1+r)^n / ((1+r)^n - 1)
```
- Centralized in `EmiCalculator.computeEmi()` with zero-rate guard
- Last installment adjustment absorbs rounding residual

### Two-Step Disbursement (Finacle LACSMNT)
1. **Preview** — POST /loan/preview computes full schedule without persisting
2. **Confirm** — POST /loan/create creates loan + schedule + voucher posting

### Repayment Allocation Order (CBS Standard)
```
Step 1: Penal Interest (recovered first)
Step 2: Accrued Interest
Step 3: Outstanding Principal
```
- FIFO installment matching — oldest pending/overdue first
- Grace period respected before penal accrual

### Schedule Restructuring
- `LoanScheduleService.regenerateSchedule()` — replaces unpaid installments only
- PAID installments immutable (historical fact)

### Moratorium
- `LoanProduct.moratoriumAllowed` + `LoanAccount.moratoriumEndDate`
- Accrual skipped during moratorium period

### Foreclosure
- `LoanEmiPaymentService.calculateForeclosureAmount()` returns:
  Outstanding Principal + Accrued Interest + Penal Interest = Total

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

### Interest Reversal on NPA
- Accrued interest reversed: `interestReversed = accruedInterest; accruedInterest = 0`
- GL: DR Interest Income, CR Interest Suspense

### NPA Suspense Accrual
- NPA loans accrue interest to suspense (not income)
- Income recognized only on realization (actual payment)

### NPA Upgrade
- When ALL overdue cleared (DPD=0): NPA → ACTIVE, suspense reversed to income

### Penal Interest
- `accruePenalInterest()` — daily, after DPD calculation, respects grace days
- Formula: outstandingPrincipal × penalRate / 365

---

## Phase 5 — Write-Off & Recovery

### Technical Write-Off
- Pre-conditions: NPA + LOSS + 100% provisioned
- GL: DR Provision for NPA, CR Loan Asset GL
- Status → WRITTEN_OFF, all amounts zeroed

### Post Write-Off Recovery
- GL: DR Cash, CR Recovery Income
- Loan status remains WRITTEN_OFF — recovery is income event

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

| # | Scenario | DR | CR |
|---|----------|----|----|
| 1 | Disbursement | Loan Asset GL | Customer Account |
| 2 | Standard Accrual | Interest Receivable | Interest Income |
| 3 | EMI Receipt | Customer Account | Interest Receivable + Loan Asset |
| 4 | Penal Accrual | Penal Receivable | Penal Income |
| 5 | Partial Payment | Customer | Penal → Interest → Principal |
| 6 | NPA Interest Reversal | Interest Income | Interest Suspense |
| 7 | NPA Suspense Accrual | Accrued Interest | Interest Suspense |
| 8 | Provision | Provision Expense | Provision for NPA |
| 9 | NPA Upgrade | Interest Suspense | Interest Income |
| 10 | Write-Off | Provision for NPA | Loan Asset GL |
| 11 | Recovery After Write-Off | Cash | Recovery Income |
| 12 | Foreclosure | Customer | Loan + Interest + Penal |
| 13 | Re-amortization | No GL | Schedule recalculation only |
| 14 | Rate Reset | No GL | Future EMI recalculated |

---

## EOD Process

```
Step 1: LoanAccrualScheduler.runForTenant()
  → ACTIVE loans: DR Interest Receivable, CR Interest Income
  → NPA loans: DR Accrued Interest, CR Interest Suspense
  → Moratorium loans: skip

Step 2: LoanNpaScheduler.runForTenant()
  → 2a: DPD + SMA/NPA classification + interest reversal + NPA upgrade
  → 2b: Penal interest accrual (with grace period)
  → 2c: Provision recalculation + immutable snapshots
```

---

## Immutable Audit Trail

| Record | Entity | Trigger |
|--------|--------|---------|
| Every payment | RepaymentTransaction | EMI/prepayment received |
| Every disbursement | LoanDisbursement | Tranche disbursed |
| Every provision | LoanProvision | Daily EOD calculation |
| Every rate change | LoanRateChangeHistory | Rate created/propagated |
| Every status change | AuditService.logEvent() | All mutations |

---

## Voucher Engine Integration

| Operation | Voucher Integrated | Lifecycle |
|-----------|-------------------|-----------|
| Disbursement | ✅ | create → system-authorize → post → LedgerEntry |
| Repayment (principal) | ✅ | create → system-authorize → post → LedgerEntry |
| Repayment (interest) | ✅ | create → system-authorize → post → LedgerEntry |
| Accrual (standard) | ✅ | create → system-authorize → post → LedgerEntry |

---

## Test Coverage (36+ cases)

| Test Class | Cases | Coverage |
|-----------|-------|----------|
| EmiCalculationTest | 10 | EMI formula, zero-rate, schedule integrity |
| NpaClassificationTest | 12+ | DPD tiers, SMA boundaries, provision rates |
| LoanBusinessValidatorTest | 14 | Null/negative/zero-total, status gates, tenant isolation |

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

| Requirement | Status |
|-------------|--------|
| DPD calculated daily from schedule | ✅ |
| SMA-0/1/2 auto-classified | ✅ |
| NPA at 90+ DPD | ✅ |
| NPA tier progression daily | ✅ |
| No income booked after NPA | ✅ |
| Interest reversed on NPA | ✅ |
| NPA suspense accrual | ✅ |
| NPA upgrade on full clearance | ✅ |
| Penal interest with grace period | ✅ |
| Provisioning per RBI norms | ✅ |
| Provision snapshots immutable | ✅ |
| Write-off LOSS + 100% provisioned | ✅ |
| Post write-off recovery as income | ✅ |
| Credit limit before disbursement | ✅ |
| Borrower/sector exposure tracking | ✅ |
| All mutations via voucher engine | ✅ |
| Immutable audit trail | ✅ |
| Multi-tenant isolation | ✅ |
| Maker-checker on vouchers | ✅ |
| Moratorium support | ✅ |
| Restructure flag tracking | ✅ |
| Foreclosure calculation | ✅ |
| Repayment order Penal→Interest→Principal | ✅ |

---

*This module is at Tier-1 CBS Lending Engine Grade — equivalent to Infosys Finacle and Temenos T24 core lending modules.*
