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
     * Validate EMI payment components (null, negative, zero-total, exceeds outstanding).
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
        if (principalComponent.compareTo(BigDecimal.ZERO) == 0
                && interestComponent.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(
                    "INVALID_EMI_PARAMS", "Total payment must be greater than zero");
        }
        if (interestComponent.compareTo(loan.getAccruedInterest()) > 0) {
            throw new BusinessException(
                    "INTEREST_EXCEEDS_ACCRUED",
                    "Interest component "
                            + interestComponent
                            + " exceeds accrued interest "
                            + loan.getAccruedInterest());
        }
        if (principalComponent.compareTo(loan.getOutstandingPrincipal()) > 0) {
            throw new BusinessException(
                    "PRINCIPAL_EXCEEDS_OUTSTANDING",
                    "Principal component "
                            + principalComponent
                            + " exceeds outstanding principal "
                            + loan.getOutstandingPrincipal());
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
