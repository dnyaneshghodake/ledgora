package com.ledgora.loan.validation;

import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.entity.CreditLimit;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Limit Validator — CBS-grade credit limit pre-condition checks.
 *
 * <p>Per RBI Master Circular on Exposure Norms:
 *
 * <ul>
 *   <li>Disbursement must not exceed available limit
 *   <li>Limit must be ACTIVE and not expired
 *   <li>Aggregate exposure must respect borrower/sector caps
 * </ul>
 *
 * <p>Stateless utility — all methods throw {@link BusinessException} on violation.
 */
public final class LimitValidator {

    private LimitValidator() {
        // Utility class
    }

    /**
     * Validate that a credit limit is active and not expired.
     *
     * @throws BusinessException if limit is inactive or expired
     */
    public static void validateLimitActive(CreditLimit limit, LocalDate businessDate) {
        if (limit == null) {
            throw new BusinessException("LIMIT_NOT_FOUND", "Credit limit is null");
        }
        if (!"ACTIVE".equals(limit.getStatus())) {
            throw new BusinessException(
                    "LIMIT_NOT_ACTIVE",
                    "Credit limit " + limit.getLimitReference() + " status: " + limit.getStatus());
        }
        if (limit.getExpiryDate() != null && limit.getExpiryDate().isBefore(businessDate)) {
            throw new BusinessException(
                    "LIMIT_EXPIRED",
                    "Credit limit "
                            + limit.getLimitReference()
                            + " expired: "
                            + limit.getExpiryDate());
        }
    }

    /**
     * Validate that disbursement amount does not exceed available limit.
     *
     * @throws BusinessException if amount exceeds available
     */
    public static void validateAvailableLimit(CreditLimit limit, BigDecimal amount) {
        if (amount.compareTo(limit.getAvailableAmount()) > 0) {
            throw new BusinessException(
                    "LIMIT_EXCEEDED",
                    "Amount "
                            + amount
                            + " exceeds available limit "
                            + limit.getAvailableAmount()
                            + " on "
                            + limit.getLimitReference());
        }
    }

    /**
     * Validate borrower aggregate exposure against single borrower cap.
     *
     * @param currentExposure current total exposure for the borrower
     * @param newAmount proposed additional exposure
     * @param cap maximum allowed exposure (e.g., 15% of capital funds)
     * @throws BusinessException if cap would be breached
     */
    public static void validateBorrowerCap(
            BigDecimal currentExposure, BigDecimal newAmount, BigDecimal cap) {
        BigDecimal projectedExposure = currentExposure.add(newAmount);
        if (cap != null && projectedExposure.compareTo(cap) > 0) {
            throw new BusinessException(
                    "BORROWER_CAP_EXCEEDED",
                    "Projected exposure " + projectedExposure + " exceeds borrower cap " + cap);
        }
    }
}
