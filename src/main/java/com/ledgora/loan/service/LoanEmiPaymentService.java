package com.ledgora.loan.service;

import com.ledgora.account.entity.Account;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.InstallmentStatus;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.entity.LoanSchedule;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanScheduleRepository;
import com.ledgora.loan.validation.LoanBusinessValidator;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.service.VoucherService;
import java.math.BigDecimal;
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
    private final VoucherService voucherService;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final AuditService auditService;

    public LoanEmiPaymentService(
            LoanAccountRepository loanAccountRepository,
            LoanScheduleRepository loanScheduleRepository,
            VoucherService voucherService,
            BranchRepository branchRepository,
            UserRepository userRepository,
            TenantService tenantService,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.voucherService = voucherService;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.auditService = auditService;
    }

    /**
     * Process an EMI payment against a loan account.
     *
     * <p>Splits the payment into principal and interest components. Updates the loan account's
     * outstanding principal and accrued interest. Posts voucher pairs via the voucher engine.
     * If outstanding reaches zero, closes the loan.
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

        // Validate: EMI cannot exceed outstanding
        BigDecimal totalPayment = principalComponent.add(interestComponent);
        BigDecimal maxPayable = loan.getOutstandingPrincipal().add(loan.getAccruedInterest());
        if (totalPayment.compareTo(maxPayable) > 0) {
            throw new BusinessException(
                    "EMI_EXCEEDS_OUTSTANDING",
                    "Payment "
                            + totalPayment
                            + " exceeds outstanding "
                            + maxPayable
                            + " for loan "
                            + loan.getLoanAccountNumber());
        }

        // Apply interest component
        BigDecimal newAccrued = loan.getAccruedInterest().subtract(interestComponent);
        loan.setAccruedInterest(newAccrued);

        // Apply principal component
        BigDecimal newOutstanding = loan.getOutstandingPrincipal().subtract(principalComponent);
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

        // ── VOUCHER ENGINE: Post repayment vouchers ──
        postRepaymentVouchers(loan, principalComponent, interestComponent, paymentDate);

        // Auto-close if fully repaid
        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0
                && newAccrued.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
            log.info("Loan {} fully repaid — status set to CLOSED", loan.getLoanAccountNumber());
        } else if (newOutstanding.compareTo(BigDecimal.ZERO) == 0
                && newAccrued.compareTo(BigDecimal.ZERO) > 0) {
            // Cannot close: accrued interest remains
            log.warn(
                    "Loan {} principal cleared but accrued interest {} remains",
                    loan.getLoanAccountNumber(),
                    newAccrued);
        }

        loan = loanAccountRepository.save(loan);

        auditService.logEvent(
                null,
                "LOAN_EMI_RECEIVED",
                "LOAN_ACCOUNT",
                loan.getId(),
                "EMI received for "
                        + loan.getLoanAccountNumber()
                        + ": principal="
                        + principalComponent
                        + " interest="
                        + interestComponent
                        + " remaining_principal="
                        + newOutstanding
                        + " remaining_interest="
                        + newAccrued,
                null);

        return loan;
    }

    /**
     * Post repayment voucher pairs via the voucher engine.
     *
     * <p>CBS-grade double-entry per component:
     *
     * <pre>
     *   Principal: DR Customer Account, CR Loan Asset GL
     *   Interest:  DR Customer Account, CR Interest Receivable GL
     * </pre>
     */
    private void postRepaymentVouchers(
            LoanAccount loan, BigDecimal principalComponent,
            BigDecimal interestComponent, LocalDate businessDate) {
        try {
            Account customerAccount = loan.getLinkedAccount();
            LoanProduct product = loan.getLoanProduct();
            Tenant tenant = loan.getTenant();

            Branch branch = customerAccount.getBranch() != null
                    ? customerAccount.getBranch()
                    : branchRepository.findByTenantId(tenant.getId()).stream()
                            .findFirst()
                            .orElseThrow(() -> new BusinessException("NO_BRANCH",
                                    "No branch configured for tenant " + tenant.getTenantCode()));

            User systemUser = userRepository.findByUsername("SYSTEM_AUTO")
                    .orElseThrow(() -> new BusinessException(
                            "SYSTEM_USER_MISSING",
                            "SYSTEM_AUTO user not configured. Repayment voucher posting blocked."));

            // Principal component voucher pair: DR Customer, CR Loan Asset GL
            if (principalComponent.compareTo(BigDecimal.ZERO) > 0) {
                String batchCode = "LOAN-REPAY-P-" + loan.getLoanAccountNumber();
                Voucher[] pair = voucherService.createVoucherPair(
                        tenant,
                        branch, customerAccount, product.getGlLoanAsset(),
                        branch, customerAccount, product.getGlLoanAsset(),
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

            // Interest component voucher pair: DR Customer, CR Interest Receivable GL
            if (interestComponent.compareTo(BigDecimal.ZERO) > 0) {
                String batchCode = "LOAN-REPAY-I-" + loan.getLoanAccountNumber();
                Voucher[] pair = voucherService.createVoucherPair(
                        tenant,
                        branch, customerAccount, product.getGlInterestReceivable(),
                        branch, customerAccount, product.getGlInterestReceivable(),
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

            log.info("Repayment vouchers posted for loan {}: principal={} interest={}",
                    loan.getLoanAccountNumber(), principalComponent, interestComponent);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Repayment voucher posting failed for loan {}: {}",
                    loan.getLoanAccountNumber(), e.getMessage(), e);
            throw new BusinessException(
                    "VOUCHER_POSTING_FAILED",
                    "Repayment voucher posting failed for loan "
                            + loan.getLoanAccountNumber() + ": " + e.getMessage());
        }
    }
}
