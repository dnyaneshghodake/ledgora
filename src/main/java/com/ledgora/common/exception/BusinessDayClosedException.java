package com.ledgora.common.exception;

/**
 * Thrown when a transaction is attempted after the business day has been closed
 * or is in DAY_CLOSING status for a tenant.
 */
public class BusinessDayClosedException extends RuntimeException {

    private final Long tenantId;
    private final String dayStatus;

    public BusinessDayClosedException(Long tenantId, String dayStatus) {
        super("Business day is not open for tenant " + tenantId + ". Current status: " + dayStatus
                + ". No transactions allowed when day status is " + dayStatus);
        this.tenantId = tenantId;
        this.dayStatus = dayStatus;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getDayStatus() {
        return dayStatus;
    }
}
