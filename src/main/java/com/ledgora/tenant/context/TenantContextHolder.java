package com.ledgora.tenant.context;

/**
 * ThreadLocal-based tenant context holder for multi-tenant isolation. Set during request processing
 * (via filter or interceptor).
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<>();

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

    /** Set the current username (typically from Spring Security authentication). */
    public static void setUsername(String username) {
        CURRENT_USERNAME.set(username);
    }

    /**
     * Get the current username. Falls back to Spring Security context if ThreadLocal is not set.
     *
     * @return username or "SYSTEM" if no authentication context is available
     */
    public static String getUsername() {
        String username = CURRENT_USERNAME.get();
        if (username != null) {
            return username;
        }
        // Fallback to Spring Security context
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext()
                            .getAuthentication();
            if (auth != null && auth.getName() != null) {
                return auth.getName();
            }
        } catch (Exception e) {
            // Security context not available (e.g., EOD batch processing)
        }
        return "SYSTEM";
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_USERNAME.remove();
    }
}
