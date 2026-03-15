package com.ledgora.loan.service;

import com.ledgora.account.entity.Account;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.InstallmentStatus;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.entity.LoanSchedule;
import com.ledgora.loan.entity.RepaymentTransaction;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanScheduleRepository;
import com.ledgora.loan.repository.RepaymentTransactionRepository;
import com.ledgora.loan.validation.EmiCalculator;
import com.ledgora.loan.validation.LoanBusinessValidator;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loan EMI Payment Service — processes equated monthly installment receipts.
 *
 * <p>Finacle-grade EMI processing with principal/interest split:
 *
 * <p>Principal Component:
 *
 * <pre>
 *   DR Customer Account
 *   CR Loan Asset GL (reduces outstanding)
 * </pre>
 *
 * <p>Interest Component:
 *
 * <pre>
 *   DR Customer Account
 *   CR Interest Receivable GL (clears accrued interest)
 * </pre>
 *
 * <p>Controls (RBI + CBS):
 *
 * <ul>
 *   <li>EMI cannot exceed outstanding principal + accrued interest
 *   <li>Loan cannot be closed if accruedInterest > 0
 *   <li>Prepayment recalculates schedule (not implemented in basic model)
 *   <li>No direct update to outstandingPrincipal — all via voucher
 * </ul>
 */
@Service
public class LoanEmiPaymentService {

    private static final Logger log = LoggerFactory.getLogger(LoanEmiPaymentService.class);

    private final LoanAccountRepository loanAccountRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final RepaymentTransactionRepository repaymentTransactionRepository;
    private final VoucherService voucherService;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final GeneralLedgerRepository glRepository;
    private final TenantService tenantService;
    private final AuditService auditService;

    public LoanEmiPaymentService(
            LoanAccountRepository loanAccountRepository,
            LoanScheduleRepository loanScheduleRepository,
            RepaymentTransactionRepository repaymentTransactionRepository,
            VoucherService voucherService,
            BranchRepository branchRepository,
            UserRepository userRepository,
            GeneralLedgerRepository glRepository,
            TenantService tenantService,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.repaymentTransactionRepository = repaymentTransactionRepository;
        this.voucherService = voucherService;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.glRepository = glRepository;
        this.tenantService = tenantService;
        this.auditService = auditService;
    }

    /**
     * Process an EMI payment against a loan account.
     *
     * <p>Splits the payment into principal and interest components. Updates the loan account's
     * outstanding principal and accrued interest. Posts voucher pairs via the voucher engine. If
     * outstanding reaches zero, closes the loan.
     *
     * <p>Accounting entries (CBS-grade, voucher-driven):
     *
     * <pre>
     *   Principal: DR Customer Account, CR Loan Asset GL
     *   Interest:  DR Customer Account, CR Interest Receivable GL
     * </pre>
     *
     * @param principalComponent amount applied to principal reduction
     * @param interestComponent amount applied to interest clearance
     */
    @Transactional
    public LoanAccount processEmiPayment(
            Long loanAccountId, BigDecimal principalComponent, BigDecimal interestComponent) {

        LoanAccount loan =
                loanAccountRepository
                        .findById(loanAccountId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LOAN_NOT_FOUND",
                                                "Loan account not found: " + loanAccountId));

        // Centralized validation via LoanBusinessValidator
        Long tenantId = TenantContextHolder.getTenantId();
        LoanBusinessValidator.validateTenantOwnership(loan, tenantId);
        LoanBusinessValidator.validateEmiPayment(principalComponent, interestComponent, loan);
        LoanBusinessValidator.validateLoanOperational(loan);

        // CBS Tier-1: validate business day is OPEN before financial operations
        Long effectiveTenantId = tenantId != null ? tenantId : loan.getTenant().getId();
        tenantService.validateBusinessDayOpen(effectiveTenantId);

        // Total payment for CBS allocation (individual components are caller hints;
        // the CBS engine reallocates in Penal → Interest → Principal order)
        BigDecimal totalPayment = principalComponent.add(interestComponent);

        // ── CBS REPAYMENT ALLOCATION ORDER: Penal → Interest → Principal ──
        // Per RBI Fair Practices Code, penal charges are recovered first,
        // then accrued interest, then principal reduction.
        BigDecimal remaining = totalPayment;

        // Step 1: Apply to penal interest first
        BigDecimal penalApplied = BigDecimal.ZERO;
        if (loan.getPenalInterest().compareTo(BigDecimal.ZERO) > 0
                && remaining.compareTo(BigDecimal.ZERO) > 0) {
            penalApplied = remaining.min(loan.getPenalInterest());
            loan.setPenalInterest(loan.getPenalInterest().subtract(penalApplied));
            remaining = remaining.subtract(penalApplied);
        }

        // Step 2: Apply to accrued interest
        BigDecimal interestApplied = remaining.min(loan.getAccruedInterest());
        BigDecimal newAccrued = loan.getAccruedInterest().subtract(interestApplied);
        loan.setAccruedInterest(newAccrued);
        remaining = remaining.subtract(interestApplied);

        // Step 3: Apply remainder to principal
        BigDecimal principalApplied = remaining.min(loan.getOutstandingPrincipal());
        BigDecimal newOutstanding = loan.getOutstandingPrincipal().subtract(principalApplied);
        loan.setOutstandingPrincipal(newOutstanding);

        // Finacle LACHST: match payment to oldest pending/overdue installment (FIFO)
        LocalDate paymentDate = tenantService.getCurrentBusinessDate(effectiveTenantId);
        BigDecimal remainingPayment = totalPayment;
        List<LoanSchedule> pendingInstallments =
                loanScheduleRepository.findPendingByLoanAccountIdOrderByInstallmentAsc(
                        loan.getId());

        for (LoanSchedule inst : pendingInstallments) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal installmentDue = inst.getEmiAmount().subtract(inst.getPaidAmount());
            if (installmentDue.compareTo(BigDecimal.ZERO) <= 0) continue;

            if (remainingPayment.compareTo(installmentDue) >= 0) {
                // Full installment payment
                inst.setPaidAmount(inst.getEmiAmount());
                inst.setPaidDate(paymentDate);
                inst.setStatus(InstallmentStatus.PAID);
                inst.setDpdDays(0);
                remainingPayment = remainingPayment.subtract(installmentDue);
            } else {
                // Partial payment
                inst.setPaidAmount(inst.getPaidAmount().add(remainingPayment));
                inst.setStatus(InstallmentStatus.PARTIALLY_PAID);
                remainingPayment = BigDecimal.ZERO;
            }
            loanScheduleRepository.save(inst);
        }

        // Recompute DPD from remaining overdue installments after payment
        List<LoanSchedule> overdueAfterPayment =
                loanScheduleRepository.findOverdueByLoanAccountIdOrderByDueDateAsc(loan.getId());
        if (overdueAfterPayment.isEmpty()) {
            loan.setDpd(0);
        } else {
            long dpdDays =
                    java.time.temporal.ChronoUnit.DAYS.between(
                            overdueAfterPayment.get(0).getDueDate(), paymentDate);
            loan.setDpd(dpdDays > 0 ? (int) dpdDays : 0);
        }

        // ── VOUCHER ENGINE: Post repayment vouchers with CBS-allocated amounts ──
        // CRITICAL: Use the actual allocated amounts (principalApplied, interestApplied)
        // after Penal→Interest→Principal reallocation, NOT the original caller parameters.
        // This ensures GL postings match the actual balance mutations.
        postRepaymentVouchers(loan, principalApplied, interestApplied, paymentDate);

        // Auto-close if fully repaid (principal + interest + penal all zero)
        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0
                && newAccrued.compareTo(BigDecimal.ZERO) == 0
                && loan.getPenalInterest().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
            log.info("Loan {} fully repaid — status set to CLOSED", loan.getLoanAccountNumber());
        } else if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            // Cannot close: accrued interest or penal remains
            log.warn(
                    "Loan {} principal cleared but interest={} penal={} remains",
                    loan.getLoanAccountNumber(),
                    newAccrued,
                    loan.getPenalInterest());
        }

        loan = loanAccountRepository.save(loan);

        // ── CBS AUDIT: Record immutable RepaymentTransaction with actual allocated amounts ──
        // CRITICAL: Use CBS-allocated amounts for the audit record, not the original caller
        // parameters. When penal interest exists, the actual principal/interest split differs
        // from the caller's requested split. The audit record must reflect reality.
        repaymentTransactionRepository.save(
                RepaymentTransaction.builder()
                        .tenant(loan.getTenant())
                        .loanAccount(loan)
                        .totalAmount(totalPayment)
                        .principalComponent(principalApplied)
                        .interestComponent(interestApplied)
                        .penalComponent(penalApplied)
                        .outstandingAfter(newOutstanding)
                        .accruedInterestAfter(newAccrued)
                        .paymentDate(paymentDate)
                        .paymentType("EMI")
                        .initiatedBy(TenantContextHolder.getUsername())
                        .remarks(
                                "EMI payment: P="
                                        + principalApplied
                                        + " I="
                                        + interestApplied
                                        + " Penal="
                                        + penalApplied
                                        + " (total="
                                        + totalPayment
                                        + ")")
                        .build());

        auditService.logEvent(
                null,
                "LOAN_EMI_RECEIVED",
                "LOAN_ACCOUNT",
                loan.getId(),
                "EMI received for "
                        + loan.getLoanAccountNumber()
                        + ": principalApplied="
                        + principalApplied
                        + " interestApplied="
                        + interestApplied
                        + " penalApplied="
                        + penalApplied
                        + " remaining_principal="
                        + newOutstanding
                        + " remaining_interest="
                        + newAccrued,
                null);

        return loan;
    }

    /**
     * Calculate penal interest for overdue loans. Called during EOD after DPD calculation.
     *
     * <p>Penal interest = outstandingPrincipal × penalRate / 365 (daily) GL: DR Penal Interest
     * Receivable, CR Penal Interest Income
     *
     * <p>RBI Fair Practices Code: Penal charges are applied only after the grace period expires.
     * Daily penal accrual uses the product's penalty rate on the outstanding principal.
     *
     * <p>Idempotency: Uses loan.lastAccrualDate to prevent double-accrual on EOD retry. The penal
     * accrual shares the same idempotency guard as the standard interest accrual since both run in
     * the same EOD phase.
     *
     * @return number of loans with penal interest accrued
     */
    @Transactional
    public int accruePenalInterest(Long tenantId) {
        Tenant tenant =
                tenantService
                        .getTenantById(tenantId);
        LocalDate businessDate = tenant.getCurrentBusinessDate();

        var loans = loanAccountRepository.findActiveAndNpaByTenantId(tenantId);
        int penalized = 0;

        for (LoanAccount loan : loans) {
            if (loan.getDpd() <= 0) continue;
            LoanProduct product = loan.getLoanProduct();

            // Grace period: no penal within grace days
            if (loan.getDpd() <= product.getGraceDays()) continue;

            BigDecimal penalRate = product.getPenaltyRate();
            if (penalRate == null || penalRate.compareTo(BigDecimal.ZERO) <= 0) continue;

            // CBS: Penal amount = outstandingPrincipal × (penalRate / 100 / 365)
            BigDecimal dailyPenal = EmiCalculator.dailyRate(penalRate);
            BigDecimal penalAmount =
                    loan.getOutstandingPrincipal()
                            .multiply(dailyPenal, MathContext.DECIMAL128)
                            .setScale(4, RoundingMode.HALF_UP);

            if (penalAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

            loan.setPenalInterest(loan.getPenalInterest().add(penalAmount));
            loanAccountRepository.save(loan);
            penalized++;

            log.debug(
                    "Penal interest accrued: loan={} dpd={} daily_penal={} total_penal={} "
                            + "rate={}% principal={}",
                    loan.getLoanAccountNumber(),
                    loan.getDpd(),
                    penalAmount,
                    loan.getPenalInterest(),
                    penalRate,
                    loan.getOutstandingPrincipal());
        }

        if (penalized > 0) {
            log.info(
                    "Penal interest accrued for {} loans (tenant {}) date {}",
                    penalized,
                    tenantId,
                    businessDate);
        }
        return penalized;
    }

    /**
     * Calculate foreclosure amount for a loan.
     *
     * <p>Foreclosure = Outstanding Principal + Accrued Interest + Penal Interest Prepayment charges
     * may be added per product policy.
     *
     * @return foreclosure amount breakdown
     */
    public java.util.Map<String, BigDecimal> calculateForeclosureAmount(Long loanAccountId) {
        LoanAccount loan =
                loanAccountRepository
                        .findById(loanAccountId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LOAN_NOT_FOUND",
                                                "Loan account not found: " + loanAccountId));

        BigDecimal principal = loan.getOutstandingPrincipal();
        BigDecimal interest = loan.getAccruedInterest();
        BigDecimal penal = loan.getPenalInterest();
        BigDecimal total = principal.add(interest).add(penal);

        java.util.Map<String, BigDecimal> breakdown = new java.util.LinkedHashMap<>();
        breakdown.put("outstandingPrincipal", principal);
        breakdown.put("accruedInterest", interest);
        breakdown.put("penalInterest", penal);
        breakdown.put("totalForeclosureAmount", total);

        log.info(
                "Foreclosure calculated: loan={} principal={} interest={} penal={} total={}",
                loan.getLoanAccountNumber(),
                principal,
                interest,
                penal,
                total);

        return breakdown;
    }

    /**
     * Post repayment voucher pairs via the voucher engine.
     *
     * <p>CBS-grade double-entry per component:
     *
     * <pre>
     *   Principal: DR Customer Account GL, CR Loan Asset GL
     *   Interest:  DR Customer Account GL, CR Interest Receivable GL
     * </pre>
     *
     * <p>CRITICAL: The DR leg uses the customer's deposit GL (resolved from account.glAccountCode)
     * and the CR leg uses the product's specific GL. Using the same GL for both legs would net to
     * zero GL impact.
     */
    private void postRepaymentVouchers(
            LoanAccount loan,
            BigDecimal principalComponent,
            BigDecimal interestComponent,
            LocalDate businessDate) {
        try {
            Account customerAccount = loan.getLinkedAccount();
            LoanProduct product = loan.getLoanProduct();
            Tenant tenant = loan.getTenant();

            Branch branch =
                    customerAccount.getBranch() != null
                            ? customerAccount.getBranch()
                            : branchRepository.findByTenantId(tenant.getId()).stream()
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new BusinessException(
                                                            "NO_BRANCH",
                                                            "No branch configured for tenant "
                                                                    + tenant.getTenantCode()));

            User systemUser =
                    userRepository
                            .findByUsername("SYSTEM_AUTO")
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    "SYSTEM_USER_MISSING",
                                                    "SYSTEM_AUTO user not configured. Repayment voucher posting blocked."));

            // Resolve customer account's deposit GL for the DR leg
            GeneralLedger customerGl = resolveCustomerAccountGl(customerAccount);

            // Principal component voucher pair: DR Customer GL, CR Loan Asset GL
            if (principalComponent.compareTo(BigDecimal.ZERO) > 0) {
                String batchCode = "LOAN-REPAY-P-" + loan.getLoanAccountNumber();
                Voucher[] pair =
                        voucherService.createVoucherPair(
                                tenant,
                                branch,
                                customerAccount,
                                customerGl, // DR leg — Customer's deposit GL
                                branch,
                                customerAccount,
                                product.getGlLoanAsset(), // CR leg — Loan Asset GL
                                principalComponent,
                                loan.getCurrency(),
                                businessDate,
                                batchCode,
                                systemUser,
                                "Loan repayment principal DR: " + loan.getLoanAccountNumber(),
                                "Loan repayment principal CR: " + loan.getLoanAccountNumber());
                voucherService.systemAuthorizeVoucher(pair[0].getId(), systemUser);
                voucherService.systemAuthorizeVoucher(pair[1].getId(), systemUser);
                voucherService.postVoucher(pair[0].getId());
                voucherService.postVoucher(pair[1].getId());
            }

            // Interest component voucher pair: DR Customer GL, CR Interest Receivable GL
            if (interestComponent.compareTo(BigDecimal.ZERO) > 0) {
                String batchCode = "LOAN-REPAY-I-" + loan.getLoanAccountNumber();
                Voucher[] pair =
                        voucherService.createVoucherPair(
                                tenant,
                                branch,
                                customerAccount,
                                customerGl, // DR leg — Customer's deposit GL
                                branch,
                                customerAccount,
                                product.getGlInterestReceivable(), // CR leg — Interest Receivable
                                interestComponent,
                                loan.getCurrency(),
                                businessDate,
                                batchCode,
                                systemUser,
                                "Loan repayment interest DR: " + loan.getLoanAccountNumber(),
                                "Loan repayment interest CR: " + loan.getLoanAccountNumber());
                voucherService.systemAuthorizeVoucher(pair[0].getId(), systemUser);
                voucherService.systemAuthorizeVoucher(pair[1].getId(), systemUser);
                voucherService.postVoucher(pair[0].getId());
                voucherService.postVoucher(pair[1].getId());
            }

            log.info(
                    "Repayment vouchers posted for loan {}: principal={} interest={}",
                    loan.getLoanAccountNumber(),
                    principalComponent,
                    interestComponent);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Repayment voucher posting failed for loan {}: {}",
                    loan.getLoanAccountNumber(),
                    e.getMessage(),
                    e);
            throw new BusinessException(
                    "VOUCHER_POSTING_FAILED",
                    "Repayment voucher posting failed for loan "
                            + loan.getLoanAccountNumber()
                            + ": "
                            + e.getMessage());
        }
    }

    /**
     * Resolve the GL account for a customer account from its glAccountCode.
     *
     * @throws BusinessException if GL code is missing or GL account not found
     */
    private GeneralLedger resolveCustomerAccountGl(Account account) {
        String glCode = account.getGlAccountCode();
        if (glCode == null || glCode.isBlank()) {
            throw new BusinessException(
                    "GL_MAPPING_MISSING",
                    "Customer account "
                            + account.getAccountNumber()
                            + " has no GL account code. CBS requires valid GL mapping.");
        }
        return glRepository
                .findByGlCode(glCode)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        "GL_ACCOUNT_NOT_FOUND",
                                        "GL account "
                                                + glCode
                                                + " not found for account "
                                                + account.getAccountNumber()));
    }
}
