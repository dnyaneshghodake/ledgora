package com.ledgora.common.exception;

/**
 * RBI Governance violation exception.
 *
 * <p>Thrown when a mandatory governance control is not satisfied: - SYSTEM_AUTO user not configured
 * - Segregation of duties violated - Mandatory system infrastructure missing
 *
 * <p>This exception must NEVER be caught and silently ignored. It indicates a fatal configuration
 * or compliance failure.
 */
public class GovernanceException extends RuntimeException {

    private final String errorCode;

    public GovernanceException(String message) {
        super(message);
        this.errorCode = "GOVERNANCE_VIOLATION";
    }

    public GovernanceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
