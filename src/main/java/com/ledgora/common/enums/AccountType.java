package com.ledgora.common.enums;

/**
 * PART 2: Extended account types with fintech-style ledger accounts.
 * Original types (SAVINGS, CURRENT, LOAN, FIXED_DEPOSIT) retained for backward compatibility.
 * New types added for ledger architecture.
 */
public enum AccountType {
    // Original account types (backward compatible)
    SAVINGS,
    CURRENT,
    LOAN,
    FIXED_DEPOSIT,
    // Fintech-style ledger account types (PART 2)
    CUSTOMER_ACCOUNT,
    GL_ACCOUNT,
    INTERNAL_ACCOUNT,
    CLEARING_ACCOUNT,
    SETTLEMENT_ACCOUNT
}
