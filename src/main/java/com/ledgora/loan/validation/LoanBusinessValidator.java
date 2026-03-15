package com.ledgora.loan.validation;

import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.enums.LoanStatus;
import java.math.BigDecimal;

/**
 * Loan Business Validator — CBS-grade pre-condition checks for loan operations.
 *
 * <p>Centralizes validation logic used across loan services per RBI Fair Practices Code:
 *
 * <ul>
 *   <li>Principal amount validation (positive, non-null)
 *   <li>Loan status gates (CLOSED, WRITTEN_OFF rejection)
 *   <li>EMI component validation (null, negative, exceeds outstanding)
 *   <li>Tenant isolation checks
 * </ul>
 *
 * <p>Stateless utility — all methods throw {@link BusinessException} on violation.
 */
public final class LoanBusinessValidator {

    private LoanBusinessValidator() {
        // Utility class — no instantiation
    }

    /**
     * Validate that principal amount is positive and non-null.
     *
     * @throws BusinessException if principal is null or non-positive
     */
    public static void validatePrincipal(BigDecimal principalAmount) {
        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_LOAN_AMOUNT", "Loan principal must be positive");
        }
    }

    /**
     * Validate that a loan is eligible for financial operations (not CLOSED/WRITTEN_OFF).
     *
     * @throws BusinessException if loan is in a terminal state
     */
    public static void validateLoanOperational(LoanAccount loan) {
        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new BusinessException("LOAN_CLOSED", "Loan is already closed");
        }
        if (loan.getStatus() == LoanStatus.WRITTEN_OFF) {
            throw new BusinessException(
                    "LOAN_WRITTEN_OFF", "Cannot process operation on written-off loan");
        }
    }

    /**
     * Validate EMI payment components (null, negative, zero-total, exceeds total payable).
     *
     * <p>CBS/RBI Fair Practices Code: The actual allocation follows Penal → Interest → Principal
     * order (handled by LoanEmiPaymentService). The caller's principalComponent and
     * interestComponent are treated as a TOTAL PAYMENT hint — the CBS engine reallocates
     * internally. Therefore, validation checks the TOTAL against the TOTAL PAYABLE, not individual
     * components against individual balances.
     *
     * @throws BusinessException on any validation failure
     */
    public static void validateEmiPayment(
            BigDecimal principalComponent, BigDecimal interestComponent, LoanAccount loan) {

        if (principalComponent == null || interestComponent == null) {
            throw new BusinessException(
                    "INVALID_EMI_PARAMS", "Principal and interest components must not be null");
        }
        if (principalComponent.compareTo(BigDecimal.ZERO) < 0
                || interestComponent.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "INVALID_EMI_PARAMS", "Principal and interest components must not be negative");
        }
        BigDecimal totalPayment = principalComponent.add(interestComponent);
        if (totalPayment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "INVALID_EMI_PARAMS", "Total payment must be greater than zero");
        }
        // CBS: Validate total payment against total payable (principal + interest + penal).
        // Individual component checks are NOT done because the CBS allocation engine
        // reallocates the total payment in Penal → Interest → Principal order,
        // which may differ from the caller's requested split.
        BigDecimal totalPayable =
                loan.getOutstandingPrincipal()
                        .add(loan.getAccruedInterest())
                        .add(loan.getPenalInterest());
        if (totalPayment.compareTo(totalPayable) > 0) {
            throw new BusinessException(
                    "EMI_EXCEEDS_OUTSTANDING",
                    "Total payment "
                            + totalPayment
                            + " exceeds total payable "
                            + totalPayable
                            + " (principal="
                            + loan.getOutstandingPrincipal()
                            + " interest="
                            + loan.getAccruedInterest()
                            + " penal="
                            + loan.getPenalInterest()
                            + ")");
        }
    }

    /**
     * Validate tenant isolation — loan belongs to the specified tenant.
     *
     * @throws BusinessException if loan does not belong to the tenant
     */
    public static void validateTenantOwnership(LoanAccount loan, Long tenantId) {
        if (tenantId != null
                && (loan.getTenant() == null || !loan.getTenant().getId().equals(tenantId))) {
            throw new BusinessException(
                    "LOAN_NOT_FOUND", "Loan account not found: " + loan.getId());
        }
    }
}
