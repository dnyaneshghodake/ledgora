package com.ledgora.common.exception;

/**
 * Accounting integrity violation exception.
 *
 * <p>Thrown when a double-entry accounting invariant is violated: - totalDebit != totalCredit at
 * posting time - Unbalanced journal attempted - Ledger integrity check failed
 *
 * <p>This exception triggers a full transaction rollback. No ledger entries, no balance updates, no
 * batch total changes.
 */
public class AccountingException extends RuntimeException {

    private final String errorCode;

    public AccountingException(String message) {
        super(message);
        this.errorCode = "ACCOUNTING_VIOLATION";
    }

    public AccountingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
