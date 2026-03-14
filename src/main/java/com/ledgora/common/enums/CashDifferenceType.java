package com.ledgora.common.enums;

/** Type of cash difference detected during teller closure reconciliation. */
public enum CashDifferenceType {
    /** Physical cash is less than system balance. DR: Cash Short GL, CR: Branch Cash GL. */
    SHORT,
    /** Physical cash is more than system balance. DR: Branch Cash GL, CR: Cash Excess GL. */
    EXCESS
}
