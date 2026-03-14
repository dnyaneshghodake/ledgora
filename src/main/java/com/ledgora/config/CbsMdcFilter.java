package com.ledgora.config;

import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CBS MDC (Mapped Diagnostic Context) Filter.
 *
 * <p>Populates SLF4J MDC with tenant, user, and request context so every log line automatically
 * includes:
 *
 * <ul>
 *   <li>{@code tenantId} — current tenant ID from TenantContextHolder
 *   <li>{@code userId} — authenticated username from SecurityContext
 *   <li>{@code requestId} — unique per-request correlation ID
 * </ul>
 *
 * <p>This filter runs early in the chain (after security) so all downstream service/repository
 * logging inherits the MDC context. MDC is cleaned up in the finally block to prevent thread-pool
 * leakage.
 *
 * <p>RBI IT Framework: Every financial log entry must be traceable to a tenant, user, and request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CbsMdcFilter extends OncePerRequestFilter {

    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Tenant context
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                MDC.put(MDC_TENANT_ID, tenantId.toString());
            }

            // User identity
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                MDC.put(MDC_USER_ID, auth.getName());
            }

            // Per-request correlation ID (use existing header or generate)
            String requestId = request.getHeader("X-Request-ID");
            if (requestId == null || requestId.isBlank()) {
                requestId =
                        Long.toHexString(System.nanoTime())
                                + "-"
                                + Long.toHexString(Thread.currentThread().getId());
            }
            MDC.put(MDC_REQUEST_ID, requestId);

            filterChain.doFilter(request, response);
        } finally {
            // Prevent MDC leakage across pooled threads
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip static resources to avoid MDC overhead on CSS/JS/images
        String path = request.getRequestURI();
        return path.startsWith("/resources/")
                || path.startsWith("/static/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.endsWith(".ico");
    }
}
