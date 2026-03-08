package com.ledgora.common.exception;

/**
 * Thrown when an account has insufficient balance for a transaction.
 */
public class InsufficientBalanceException extends BusinessException {

    private final String accountNumber;

    public InsufficientBalanceException(String accountNumber) {
        super("INSUFFICIENT_BALANCE", "Insufficient balance in account: " + accountNumber);
        this.accountNumber = accountNumber;
    }

    public InsufficientBalanceException(String accountNumber, String message) {
        super("INSUFFICIENT_BALANCE", message);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
