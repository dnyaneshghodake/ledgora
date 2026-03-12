package com.ledgora.audit.controller;

import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Audit Dashboard and Audit Log Explorer controller. Routes: GET /audit/validation — Audit
 * dashboard with hash chain verification GET /audit/explorer — Paginated audit log explorer
 */
@Controller
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditService auditService, AuditLogRepository auditLogRepository) {
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
    }

    /** Audit Dashboard — shows hash chain integrity status and recent audit events. */
    @GetMapping("/validation")
    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN', 'SUPER_ADMIN')")
    public String auditDashboard(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        // Hash chain verification
        long brokenLinkId = auditService.verifyHashChain(tenantId);
        boolean chainIntact = brokenLinkId == -1;

        // Recent audit events for this tenant
        List<AuditLog> recentEvents =
                auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId).stream()
                        .limit(50)
                        .toList();

        // Total audit event count
        long totalEvents = auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId).size();

        model.addAttribute("chainIntact", chainIntact);
        model.addAttribute("brokenLinkId", brokenLinkId);
        model.addAttribute("recentEvents", recentEvents);
        model.addAttribute("totalEvents", totalEvents);
        model.addAttribute("tenantId", tenantId);
        return "audit/audit-validation";
    }

    /** Audit Log Explorer — paginated, filterable by entity type. */
    @GetMapping("/explorer")
    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN', 'SUPER_ADMIN')")
    public String auditExplorer(
            @RequestParam(value = "entity", required = false) String entity,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            Model model,
            HttpSession session) {
        Long tenantId = resolveTenantId(session);
        PageRequest pageable = PageRequest.of(page, size);
        Page<AuditLog> auditPage;

        if (entity != null && !entity.isBlank()) {
            auditPage =
                    auditLogRepository.findByTenantIdAndEntityOrderByTimestampDesc(
                            tenantId, entity, pageable);
            model.addAttribute("filterEntity", entity);
        } else if (action != null && !action.isBlank()) {
            auditPage =
                    auditLogRepository.findByTenantIdAndActionOrderByTimestampDesc(
                            tenantId, action, pageable);
            model.addAttribute("filterAction", action);
        } else {
            auditPage = auditLogRepository.findByTenantIdOrderByTimestampDesc(tenantId, pageable);
        }

        model.addAttribute("auditLogs", auditPage.getContent());
        model.addAttribute("currentPage", auditPage.getNumber());
        model.addAttribute("totalPages", auditPage.getTotalPages());
        model.addAttribute("totalElements", auditPage.getTotalElements());
        return "audit/audit-explorer";
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
