package com.ledgora.common.enums;

public enum TransactionStatus {
    PENDING,
    PENDING_APPROVAL,
    APPROVED,
    COMPLETED,
    FAILED,
    REVERSED,
    REJECTED,
    /** Transaction parked to Suspense GL due to partial posting failure. Requires resolution. */
    PARKED
}
