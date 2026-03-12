package com.ledgora.common.validation;

import com.ledgora.common.exception.InvalidTransactionAmountException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * RBI-grade field validation for banking operations. Validates at service level as the final
 * defense before persistence.
 */
public final class RbiFieldValidator {

    private RbiFieldValidator() {
        // Utility class
    }

    /**
     * Validate transaction amount: - Must be positive (> 0) - Zero not allowed - Negative not
     * allowed - Scale must be <= 2 decimal places
     */
    public static void validateTransactionAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransactionAmountException("Transaction amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException(
                    "Transaction amount must be positive. Received: " + amount);
        }
        if (amount.scale() > 2) {
            throw new InvalidTransactionAmountException(
                    "Transaction amount must have at most 2 decimal places. Received: " + amount);
        }
    }

    /** Validate transaction amount with configurable max limit. */
    public static void validateTransactionAmount(BigDecimal amount, BigDecimal maxLimit) {
        validateTransactionAmount(amount);
        if (maxLimit != null && amount.compareTo(maxLimit) > 0) {
            throw new InvalidTransactionAmountException(
                    "Transaction amount exceeds maximum limit of " + maxLimit);
        }
    }

    /** Validate interest rate (0-100). */
    public static void validateInterestRate(BigDecimal rate, String fieldName) {
        if (rate == null) {
            return;
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100");
        }
    }

    /** Validate overdraft amount (>= 0). */
    public static void validateOverdraft(BigDecimal amount, String fieldName) {
        if (amount == null) {
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must be >= 0");
        }
    }

    /** Validate ownership percentage (0-100). */
    public static void validateOwnershipPercentage(BigDecimal percentage) {
        if (percentage == null) {
            throw new IllegalArgumentException("Ownership percentage is required");
        }
        if (percentage.compareTo(BigDecimal.ZERO) <= 0
                || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Ownership percentage must be between 0 and 100");
        }
    }

    /** Validate Date of Birth (must be >= 18 years). */
    public static void validateDob(LocalDate dob) {
        if (dob == null) {
            return;
        }
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 18) {
            throw new IllegalArgumentException(
                    "Customer must be at least 18 years old. Current age: " + age);
        }
    }

    /** Validate PAN is mandatory for INDIVIDUAL customers. */
    public static void validatePanForIndividual(String customerType, String panNumber) {
        if ("INDIVIDUAL".equals(customerType)
                && (panNumber == null || panNumber.trim().isEmpty())) {
            throw new IllegalArgumentException("PAN number is mandatory for INDIVIDUAL customers");
        }
    }

    /** Validate GST is mandatory for CORPORATE customers. */
    public static void validateGstForCorporate(String customerType, String gstNumber) {
        if ("CORPORATE".equals(customerType) && (gstNumber == null || gstNumber.trim().isEmpty())) {
            throw new IllegalArgumentException("GST number is mandatory for CORPORATE customers");
        }
    }

    /**
     * Validate PAN format: 5 uppercase letters + 4 digits + 1 uppercase letter. Example: ABCDE1234F
     */
    public static void validatePanFormat(String pan) {
        if (pan == null || pan.trim().isEmpty()) return;
        if (!pan.matches("[A-Z]{5}[0-9]{4}[A-Z]{1}")) {
            throw new IllegalArgumentException(
                    "Invalid PAN format. Expected format: ABCDE1234F (5 letters + 4 digits + 1 letter)");
        }
    }

    /**
     * Validate Aadhaar number: exactly 12 digits. RBI KYC Master Direction: Aadhaar is a valid OVD
     * for account opening.
     */
    public static void validateAadhaarFormat(String aadhaar) {
        if (aadhaar == null || aadhaar.trim().isEmpty()) return;
        if (!aadhaar.matches("[0-9]{12}")) {
            throw new IllegalArgumentException("Invalid Aadhaar format. Must be exactly 12 digits");
        }
    }

    /**
     * Validate IFSC code format: 4 uppercase letters + 0 + 6 alphanumeric. RBI NEFT/RTGS
     * requirement.
     */
    public static void validateIfscFormat(String ifsc) {
        if (ifsc == null || ifsc.trim().isEmpty()) return;
        if (!ifsc.matches("[A-Z]{4}0[A-Z0-9]{6}")) {
            throw new IllegalArgumentException("Invalid IFSC format. Expected format: ABCD0123456");
        }
    }

    /**
     * Validate GST format: 15-character alphanumeric. Format: 2-digit state code + 10-char PAN + 1
     * entity + 1 Z + 1 checksum
     */
    public static void validateGstFormat(String gst) {
        if (gst == null || gst.trim().isEmpty()) return;
        if (!gst.matches("[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}")) {
            throw new IllegalArgumentException(
                    "Invalid GST format. Expected 15-character GSTIN (e.g. 29ABCDE1234F1Z5)");
        }
    }

    /** Validate mobile number: exactly 10 digits, starting with 6-9 (Indian mobile). */
    public static void validateMobileNumber(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) return;
        if (!mobile.matches("[6-9][0-9]{9}")) {
            throw new IllegalArgumentException(
                    "Invalid mobile number. Must be 10 digits starting with 6-9");
        }
    }

    /** Validate account number format (alphanumeric, 5-30 chars). */
    public static void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required");
        }
        if (accountNumber.length() < 5 || accountNumber.length() > 30) {
            throw new IllegalArgumentException(
                    "Account number must be between 5 and 30 characters");
        }
        if (!accountNumber.matches("[A-Za-z0-9\\-]+")) {
            throw new IllegalArgumentException(
                    "Account number must contain only alphanumeric characters and hyphens");
        }
    }
}
