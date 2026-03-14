package com.ledgora.common.enums;

/**
 * Finacle-grade teller lifecycle state machine.
 *
 * <pre>
 * ASSIGNED → OPEN_REQUESTED → OPEN → SUSPENDED → CLOSING_REQUESTED → CLOSED
 *                                  └─────────────→ CLOSING_REQUESTED → CLOSED
 * </pre>
 */
public enum TellerStatus {
    /** Teller created and assigned to a branch/user but not yet opened. */
    ASSIGNED,
    /** Teller open request submitted, pending supervisor authorization. */
    OPEN_REQUESTED,
    /** Teller session is active — transactions allowed. */
    OPEN,
    /** Teller temporarily suspended (e.g., break). No transactions allowed. */
    SUSPENDED,
    /** Teller close request submitted, pending supervisor authorization. */
    CLOSING_REQUESTED,
    /** Teller session closed for the day. Immutable after closure. */
    CLOSED
}
