package com.ledgora.common.enums;

/**
 * CBS Transaction sub-type classification per Finacle model.
 * Used alongside TransactionType for granular reporting and RBI returns.
 */
public enum TransactionSubType {
    CASH,
    CHEQUE,
    NEFT,
    RTGS,
    IMPS,
    UPI,
    INTERNAL,
    INTEREST,
    CHARGE,
    EOD_ADJUSTMENT,
    REVERSAL,
    SETTLEMENT,
    OTHER
}
