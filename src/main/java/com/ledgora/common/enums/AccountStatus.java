package com.ledgora.common.enums;

public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    FROZEN,
    CLOSED,
    /** Account placed under fraud review due to velocity breach. Transactions blocked. */
    UNDER_REVIEW
}
