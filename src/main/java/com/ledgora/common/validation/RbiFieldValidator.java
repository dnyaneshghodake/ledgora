package com.ledgora.common.validation;

import com.ledgora.common.exception.InvalidTransactionAmountException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * RBI-grade field validation for banking operations.
 * Validates at service level as the final defense before persistence.
 */
public final class RbiFieldValidator {

    private RbiFieldValidator() {
        // Utility class
    }

    /**
     * Validate transaction amount:
     * - Must be positive (> 0)
     * - Zero not allowed
     * - Negative not allowed
     * - Scale must be <= 2 decimal places
     */
    public static void validateTransactionAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransactionAmountException("Transaction amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("Transaction amount must be positive. Received: " + amount);
        }
        if (amount.scale() > 2) {
            throw new InvalidTransactionAmountException("Transaction amount must have at most 2 decimal places. Received: " + amount);
        }
    }

    /**
     * Validate transaction amount with configurable max limit.
     */
    public static void validateTransactionAmount(BigDecimal amount, BigDecimal maxLimit) {
        validateTransactionAmount(amount);
        if (maxLimit != null && amount.compareTo(maxLimit) > 0) {
            throw new InvalidTransactionAmountException("Transaction amount exceeds maximum limit of " + maxLimit);
        }
    }

    /**
     * Validate interest rate (0-100).
     */
    public static void validateInterestRate(BigDecimal rate, String fieldName) {
        if (rate == null) {
            return;
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100");
        }
    }

    /**
     * Validate overdraft amount (>= 0).
     */
    public static void validateOverdraft(BigDecimal amount, String fieldName) {
        if (amount == null) {
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must be >= 0");
        }
    }

    /**
     * Validate ownership percentage (0-100).
     */
    public static void validateOwnershipPercentage(BigDecimal percentage) {
        if (percentage == null) {
            throw new IllegalArgumentException("Ownership percentage is required");
        }
        if (percentage.compareTo(BigDecimal.ZERO) <= 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Ownership percentage must be between 0 and 100");
        }
    }

    /**
     * Validate Date of Birth (must be >= 18 years).
     */
    public static void validateDob(LocalDate dob) {
        if (dob == null) {
            return;
        }
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 18) {
            throw new IllegalArgumentException("Customer must be at least 18 years old. Current age: " + age);
        }
    }

    /**
     * Validate PAN is mandatory for INDIVIDUAL customers.
     */
    public static void validatePanForIndividual(String customerType, String panNumber) {
        if ("INDIVIDUAL".equals(customerType) && (panNumber == null || panNumber.trim().isEmpty())) {
            throw new IllegalArgumentException("PAN number is mandatory for INDIVIDUAL customers");
        }
    }

    /**
     * Validate GST is mandatory for CORPORATE customers.
     */
    public static void validateGstForCorporate(String customerType, String gstNumber) {
        if ("CORPORATE".equals(customerType) && (gstNumber == null || gstNumber.trim().isEmpty())) {
            throw new IllegalArgumentException("GST number is mandatory for CORPORATE customers");
        }
    }
}
