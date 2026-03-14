package com.ledgora.common.enums;

import java.util.Map;
import java.util.Set;

/**
 * CBS-grade Transaction Status with state machine enforcement.
 *
 * <p>Allowed transitions (Finacle model):
 * <pre>
 *   PENDING          → PENDING_APPROVAL, COMPLETED, FAILED, PARKED
 *   PENDING_APPROVAL → APPROVED, REJECTED
 *   APPROVED         → COMPLETED, FAILED
 *   COMPLETED        → REVERSED
 *   REVERSED         → (terminal)
 *   REJECTED         → (terminal)
 *   FAILED           → (terminal)
 *   PARKED           → COMPLETED, REVERSED, FAILED
 * </pre>
 *
 * <p>RBI compliance: No backward transitions allowed. Terminal states are immutable.
 * Use {@link #canTransitionTo(TransactionStatus)} before any status change.
 */
public enum TransactionStatus {
    PENDING,
    PENDING_APPROVAL,
    APPROVED,
    COMPLETED,
    FAILED,
    REVERSED,
    REJECTED,
    /** Transaction parked to Suspense GL due to partial posting failure. Requires resolution. */
    PARKED;

    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    PENDING, Set.of(PENDING_APPROVAL, COMPLETED, FAILED, PARKED),
                    PENDING_APPROVAL, Set.of(APPROVED, REJECTED),
                    APPROVED, Set.of(COMPLETED, FAILED),
                    COMPLETED, Set.of(REVERSED),
                    REVERSED, Set.of(),
                    REJECTED, Set.of(),
                    FAILED, Set.of(),
                    PARKED, Set.of(COMPLETED, REVERSED, FAILED));

    /**
     * Check if a transition from this status to the target is allowed.
     *
     * @param target the desired next status
     * @return true if the transition is valid per CBS state machine rules
     */
    public boolean canTransitionTo(TransactionStatus target) {
        Set<TransactionStatus> allowed = ALLOWED_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    /**
     * Validate and return the target status, throwing if the transition is illegal.
     *
     * @param target the desired next status
     * @return the target status if transition is valid
     * @throws IllegalStateException if the transition violates the CBS state machine
     */
    public TransactionStatus validateTransition(TransactionStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "CBS state machine violation: cannot transition from "
                            + this.name()
                            + " to "
                            + target.name()
                            + ". Allowed from "
                            + this.name()
                            + ": "
                            + ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()));
        }
        return target;
    }
}
