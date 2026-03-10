package com.ledgora.common.enums;

/**
 * Status lifecycle for inter-branch clearing transfers.
 *
 * INITIATED  → Transfer record created, no posting yet
 * SENT       → Branch A leg posted (DR Customer, CR IBC_OUT)
 * RECEIVED   → Branch B leg posted (DR IBC_IN, CR Customer)
 * SETTLED    → Clearing settlement completed (IBC_IN/OUT zeroed)
 * FAILED     → One or both legs failed; routed to suspense
 */
public enum InterBranchTransferStatus {
    INITIATED,
    SENT,
    RECEIVED,
    SETTLED,
    FAILED
}
