package com.ledgora.common.exception;

/**
 * Exception thrown when transaction amount validation fails. Part of RBI-grade strict field
 * validation.
 */
public class InvalidTransactionAmountException extends RuntimeException {

    public InvalidTransactionAmountException(String message) {
        super(message);
    }

    public InvalidTransactionAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}
