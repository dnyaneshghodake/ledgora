package com.ledgora.common.validation;

import com.ledgora.common.exception.ScriptInjectionException;

import java.util.regex.Pattern;

/**
 * Input sanitization utility for script injection protection.
 * Rejects dangerous characters and patterns in user input.
 */
public final class InputSanitizer {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "(?i)(<script|</script|javascript:|on\\w+\\s*=|<iframe|<object|<embed|<form|<img\\s+[^>]*onerror)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("[<>]");

    private InputSanitizer() {
        // Utility class
    }

    /**
     * Sanitize a string input by rejecting script injection patterns.
     * @param input the input string
     * @param fieldName the field name for error messages
     * @return the sanitized input (trimmed)
     * @throws ScriptInjectionException if dangerous patterns are detected
     */
    public static String sanitize(String input, String fieldName) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();

        if (SCRIPT_PATTERN.matcher(trimmed).find()) {
            throw new ScriptInjectionException("Script injection detected in field: " + fieldName);
        }

        if (HTML_TAG_PATTERN.matcher(trimmed).find()) {
            throw new ScriptInjectionException("HTML tags not allowed in field: " + fieldName);
        }

        return trimmed;
    }

    /**
     * Sanitize without throwing - returns cleaned string.
     */
    public static String sanitizeStrip(String input) {
        if (input == null) {
            return null;
        }
        return input.trim()
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;");
    }

    /**
     * Validate that a string contains only allowed characters for names.
     * Allows alphabets, spaces, and dots only.
     */
    public static void validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        sanitize(name, fieldName);
        if (!name.trim().matches("^[a-zA-Z .]+$")) {
            throw new IllegalArgumentException(fieldName + " must contain only alphabets, spaces, and dots");
        }
    }

    /**
     * Validate mobile number (10 digits).
     */
    public static void validateMobile(String mobile, String fieldName) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return; // Optional field
        }
        sanitize(mobile, fieldName);
        if (!mobile.trim().matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException(fieldName + " must be exactly 10 digits");
        }
    }

    /**
     * Validate email format.
     */
    public static void validateEmail(String email, String fieldName) {
        if (email == null || email.trim().isEmpty()) {
            return; // Optional field
        }
        sanitize(email, fieldName);
        if (!email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException(fieldName + " must be a valid email address");
        }
    }

    /**
     * Validate PAN number format (ABCDE1234F).
     */
    public static void validatePAN(String pan, String fieldName) {
        if (pan == null || pan.trim().isEmpty()) {
            return; // Optional unless required by context
        }
        sanitize(pan, fieldName);
        if (!pan.trim().matches("^[A-Z]{5}[0-9]{4}[A-Z]$")) {
            throw new IllegalArgumentException(fieldName + " must be in format ABCDE1234F");
        }
    }

    /**
     * Validate Aadhaar number (12 digits numeric).
     */
    public static void validateAadhaar(String aadhaar, String fieldName) {
        if (aadhaar == null || aadhaar.trim().isEmpty()) {
            return;
        }
        sanitize(aadhaar, fieldName);
        if (!aadhaar.trim().matches("^[0-9]{12}$")) {
            throw new IllegalArgumentException(fieldName + " must be exactly 12 digits");
        }
    }

    /**
     * Validate GST number format.
     */
    public static void validateGST(String gst, String fieldName) {
        if (gst == null || gst.trim().isEmpty()) {
            return;
        }
        sanitize(gst, fieldName);
        if (!gst.trim().matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z][Z][0-9A-Z]$")) {
            throw new IllegalArgumentException(fieldName + " must be a valid GST number");
        }
    }

    /**
     * Mask Aadhaar number for display (show only last 4 digits).
     */
    public static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) {
            return "XXXX-XXXX-XXXX";
        }
        return "XXXX-XXXX-" + aadhaar.substring(aadhaar.length() - 4);
    }
}
