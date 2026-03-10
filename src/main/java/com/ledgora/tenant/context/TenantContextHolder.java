package com.ledgora.tenant.context;

/**
 * ThreadLocal-based tenant context holder for multi-tenant isolation. Set during request processing
 * (via filter or interceptor).
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
        // utility class
    }

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static Long getRequiredTenantId() {
        Long tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException(
                    "No tenant context set. Ensure TenantContextHolder is populated before accessing tenant data.");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
