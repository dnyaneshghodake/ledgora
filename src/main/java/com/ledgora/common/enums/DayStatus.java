package com.ledgora.common.enums;

/**
 * Day status for tenant-level business date control.
 *
 * CBS Day lifecycle:
 *   CLOSED → (Day Begin pre-checks pass) → OPEN → (EOD) → DAY_CLOSING → CLOSED
 *
 * Day Begin requires:
 *   - Previous day is CLOSED
 *   - No pending batches from previous day
 *   - Calendar loaded for current date
 *   - Explicit DBO "Open Day" action
 */
public enum DayStatus {
    CLOSED, OPEN, DAY_CLOSING
}
