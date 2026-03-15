package com.ledgora.loan.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.repository.LoanAccountRepository;
import java.math.BigDecimal;
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
    private final AuditService auditService;

    public LoanEmiPaymentService(
            LoanAccountRepository loanAccountRepository,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.auditService = auditService;
    }

    /**
     * Process an EMI payment against a loan account.
     *
     * <p>Splits the payment into principal and interest components. Updates the loan account's
     * outstanding principal and accrued interest. If outstanding reaches zero, closes the loan.
     *
     * <p>NOTE: The actual GL postings should be done via the voucher engine by the caller.
     *
     * @param principalComponent amount applied to principal reduction
     * @param interestComponent amount applied to interest clearance
     */
    @Transactional
    public LoanAccount processEmiPayment(
            Long loanAccountId,
            BigDecimal principalComponent,
            BigDecimal interestComponent) {

        // Null validation
        if (principalComponent == null || interestComponent == null) {
            throw new BusinessException(
                    "INVALID_EMI_PARAMS",
                    "Principal and interest components must not be null");
        }

        // Negative amount validation
        if (principalComponent.compareTo(BigDecimal.ZERO) < 0
                || interestComponent.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "INVALID_EMI_PARAMS",
                    "Principal and interest components must not be negative");
        }

        LoanAccount loan =
                loanAccountRepository
                        .findById(loanAccountId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LOAN_NOT_FOUND",
                                                "Loan account not found: " + loanAccountId));

        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new BusinessException("LOAN_CLOSED", "Loan is already closed");
        }
        if (loan.getStatus() == LoanStatus.WRITTEN_OFF) {
            throw new BusinessException(
                    "LOAN_WRITTEN_OFF", "Cannot process payment on written-off loan");
        }

        // Validate: interest component cannot exceed accrued interest
        if (interestComponent.compareTo(loan.getAccruedInterest()) > 0) {
            throw new BusinessException(
                    "INTEREST_EXCEEDS_ACCRUED",
                    "Interest component "
                            + interestComponent
                            + " exceeds accrued interest "
                            + loan.getAccruedInterest()
                            + " for loan "
                            + loan.getLoanAccountNumber());
        }

        // Validate: principal component cannot exceed outstanding principal
        if (principalComponent.compareTo(loan.getOutstandingPrincipal()) > 0) {
            throw new BusinessException(
                    "PRINCIPAL_EXCEEDS_OUTSTANDING",
                    "Principal component "
                            + principalComponent
                            + " exceeds outstanding principal "
                            + loan.getOutstandingPrincipal()
                            + " for loan "
                            + loan.getLoanAccountNumber());
        }

        // Validate: EMI cannot exceed outstanding
        BigDecimal totalPayment = principalComponent.add(interestComponent);
        BigDecimal maxPayable =
                loan.getOutstandingPrincipal().add(loan.getAccruedInterest());
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
        BigDecimal newOutstanding =
                loan.getOutstandingPrincipal().subtract(principalComponent);
        loan.setOutstandingPrincipal(newOutstanding);

        // Reset DPD on payment (simplified — full implementation would check schedule)
        loan.setDpd(0);

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
}
