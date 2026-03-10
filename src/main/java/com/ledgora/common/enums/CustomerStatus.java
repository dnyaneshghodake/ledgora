package com.ledgora.common.enums;

/**
 * CBS-grade customer status for customer master. Lifecycle: PENDING_APPROVAL → ACTIVE (on checker
 * approve) or REJECTED Active customers can become INACTIVE (dormant) or CLOSED.
 */
public enum CustomerStatus {
    PENDING_APPROVAL,
    ACTIVE,
    INACTIVE,
    CLOSED,
    REJECTED
}
