package com.ledgora.common.enums;

/**
 * Freeze level for customer and account governance. NONE - No freeze DEBIT_ONLY - Only debit
 * transactions blocked CREDIT_ONLY - Only credit transactions blocked FULL - All transactions
 * blocked
 */
public enum FreezeLevel {
    NONE,
    DEBIT_ONLY,
    CREDIT_ONLY,
    FULL
}
