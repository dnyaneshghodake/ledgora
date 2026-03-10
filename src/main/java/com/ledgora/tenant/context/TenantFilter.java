package com.ledgora.tenant.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter to set the tenant context from the session or request header. Runs before security filters
 * to ensure tenant context is available.
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Try from session first (JSP/form-based auth)
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object tenantId = session.getAttribute("tenantId");
                if (tenantId != null) {
                    TenantContextHolder.setTenantId(Long.valueOf(tenantId.toString()));
                }
            }

            // Override from header if present (API/JWT-based auth)
            String headerTenantId = request.getHeader("X-Tenant-Id");
            if (headerTenantId != null && !headerTenantId.isEmpty()) {
                TenantContextHolder.setTenantId(Long.valueOf(headerTenantId));
            }

            // Default to tenant 1 if not set
            if (TenantContextHolder.getTenantId() == null) {
                TenantContextHolder.setTenantId(1L);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
