package com.ledgora.common.exception;

/**
 * Thrown when a cross-tenant data access violation is detected.
 */
public class TenantIsolationException extends RuntimeException {

    private final Long requestedTenantId;
    private final Long currentTenantId;

    public TenantIsolationException(Long requestedTenantId, Long currentTenantId) {
        super("Tenant isolation violation: attempted to access tenant " + requestedTenantId
                + " from tenant context " + currentTenantId);
        this.requestedTenantId = requestedTenantId;
        this.currentTenantId = currentTenantId;
    }

    public Long getRequestedTenantId() {
        return requestedTenantId;
    }

    public Long getCurrentTenantId() {
        return currentTenantId;
    }
}
