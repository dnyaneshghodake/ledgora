package com.ledgora.config;

import com.ledgora.approval.service.ApprovalService;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * CBS Interceptor: refreshes pending approval count in the HTTP session on every page request.
 *
 * <p>Finacle/CBS standard: the header bell icon and sidebar badge must always reflect the real-time
 * count of actionable (PENDING) records. Storing the count in the session at login and never
 * refreshing it causes stale badges after approve/reject actions.
 *
 * <p>This interceptor runs after Spring Security and TenantFilter have populated the security
 * context and tenant context, so {@code ApprovalService.getPendingRequests()} can resolve the
 * tenant correctly.
 */
@Component
public class PendingApprovalsInterceptor implements HandlerInterceptor {

    private final ApprovalService approvalService;

    public PendingApprovalsInterceptor(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            return true; // Not logged in — skip
        }

        // Skip static resources and AJAX endpoints
        String uri = request.getRequestURI();
        if (uri.startsWith("/resources/")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || uri.endsWith(".ico")
                || uri.contains("/api/")) {
            return true;
        }

        // Refresh pending count only if tenant context is available
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            try {
                int count = approvalService.getPendingRequests().size();
                session.setAttribute("pendingApprovals", count);
            } catch (Exception e) {
                // Non-critical — badge just shows stale or no count
            }
        }

        return true;
    }
}
