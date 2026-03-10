package com.ledgora.common.exception;

/**
 * PART 11: Custom exception for transaction not found scenarios. Maps to 404 page with a friendly
 * message via GlobalExceptionHandler.
 */
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String message) {
        super(message);
    }

    public TransactionNotFoundException(Long transactionId) {
        super("Transaction not found with ID: " + transactionId);
    }
}
