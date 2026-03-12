package com.ledgora.audit.controller;

import com.ledgora.audit.service.AuditService;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Audit hash chain verification controller.
 *
 * <p>Routes:
 *
 * <ul>
 *   <li>POST /audit/verify-chain — Trigger hash chain re-verification
 * </ul>
 *
 * <p>GET /audit/validation is handled by {@link AuditDiagnosticController}. GET /audit/explorer is
 * handled by {@link AuditExplorerController}.
 */
@Controller
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /** Trigger hash chain re-verification (POST action from dashboard). */
    @PostMapping("/verify-chain")
    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN', 'SUPER_ADMIN')")
    public String verifyChain(HttpSession session, RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        long result = auditService.verifyHashChain(tenantId);
        if (result == -1) {
            redirectAttributes.addFlashAttribute(
                    "message", "Audit hash chain is INTACT for tenant " + tenantId);
        } else {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Audit hash chain BROKEN at entry ID " + result + " for tenant " + tenantId);
        }
        return "redirect:/audit/validation";
    }

    private Long resolveTenantId(HttpSession session) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            Object sessionTenantId = session.getAttribute("tenantId");
            if (sessionTenantId instanceof Number n) tenantId = n.longValue();
            else if (sessionTenantId instanceof String s && !s.isBlank())
                tenantId = Long.valueOf(s);
        }
        if (tenantId == null) throw new IllegalStateException("Tenant context not set");
        return tenantId;
    }
}
