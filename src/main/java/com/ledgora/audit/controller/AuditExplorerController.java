package com.ledgora.audit.controller;

import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Enterprise Audit Log Explorer — searchable, filterable audit event viewer for CBS governance.
 *
 * <p>Read-only. Does NOT modify any audit records. All AuditLog fields are scalar (no lazy
 * associations), so there is zero N+1 risk.
 *
 * <p>Performance: Single paginated SELECT via JPA Specification. Composable filters combined at
 * runtime — no combinatorial explosion of repository methods.
 *
 * <p>Filters: dateFrom, dateTo, action, username, entityType, entityId. All optional; unset filters
 * are ignored. Tenant isolation always enforced.
 */
@Controller
@RequestMapping("/audit")
public class AuditExplorerController {

    private final AuditLogRepository auditLogRepository;

    public AuditExplorerController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/explorer")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @Transactional(readOnly = true)
    public String explorer(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate dateTo,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model,
            HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        // Build composable specification — single SELECT with all filters
        Specification<AuditLog> spec =
                (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);

        if (dateFrom != null) {
            final LocalDateTime start = dateFrom.atStartOfDay();
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.greaterThanOrEqualTo(root.get("timestamp"), start));
        }
        if (dateTo != null) {
            final LocalDateTime end = dateTo.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), end));
        }
        if (action != null && !action.isBlank()) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.like(
                                            cb.upper(root.get("action")),
                                            "%" + action.trim().toUpperCase() + "%"));
        }
        if (username != null && !username.isBlank()) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.like(
                                            cb.upper(root.get("username")),
                                            "%" + username.trim().toUpperCase() + "%"));
        }
        if (entityType != null && !entityType.isBlank()) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.like(
                                            cb.upper(root.get("entity")),
                                            "%" + entityType.trim().toUpperCase() + "%"));
        }
        if (entityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityId"), entityId));
        }

        Page<AuditLog> auditPage = auditLogRepository.findAll(spec, pageable);

        // Model attributes
        model.addAttribute("auditPage", auditPage);
        model.addAttribute("auditLogs", auditPage.getContent());
        model.addAttribute("currentPage", auditPage.getNumber());
        model.addAttribute("totalPages", auditPage.getTotalPages());
        model.addAttribute("totalElements", auditPage.getTotalElements());
        model.addAttribute("pageSize", size);

        // Filter state for form re-population
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);
        model.addAttribute("filterAction", action);
        model.addAttribute("filterUsername", username);
        model.addAttribute("filterEntityType", entityType);
        model.addAttribute("filterEntityId", entityId);

        return "audit/audit-explorer";
    }

    /** Resolve tenant ID from TenantContextHolder or session. */
    private Long resolveTenantId(HttpSession session) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            Object sessionTenantId = session.getAttribute("tenantId");
            if (sessionTenantId instanceof Number n) {
                tenantId = n.longValue();
            } else if (sessionTenantId instanceof String s && !s.isBlank()) {
                tenantId = Long.valueOf(s);
            }
        }
        if (tenantId == null) {
            throw new IllegalStateException(
                    "Tenant context is not set for audit explorer operation");
        }
        return tenantId;
    }
}
