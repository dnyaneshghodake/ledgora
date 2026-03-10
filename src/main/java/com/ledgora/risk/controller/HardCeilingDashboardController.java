package com.ledgora.risk.controller;

import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Hard Transaction Ceiling monitoring dashboard. Read-only governance visibility into absolute
 * ceiling enforcement events.
 *
 * <p>Hard ceiling violations are logged to audit_logs with action = 'HARD_LIMIT_EXCEEDED' and
 * entity = 'GOVERNANCE' by HardTransactionCeilingService. The details field contains amount,
 * ceiling, channel, and userId.
 *
 * <p>Performance: Max 2 SELECTs. No N+1. No lazy loading.
 *
 * <ul>
 *   <li>Query 1: countByTenantIdAndActionAndTimestampBetween (today's violations)
 *   <li>Query 2: findTop20ByTenantIdAndActionOrderByTimestampDesc (recent violations)
 * </ul>
 */
@Controller
@RequestMapping("/risk")
public class HardCeilingDashboardController {

    /** Audit action constant used by HardTransactionCeilingService when logging violations. */
    private static final String HARD_LIMIT_ACTION = "HARD_LIMIT_EXCEEDED";

    private final AuditLogRepository auditLogRepository;
    private final TenantService tenantService;

    public HardCeilingDashboardController(
            AuditLogRepository auditLogRepository, TenantService tenantService) {
        this.auditLogRepository = auditLogRepository;
        this.tenantService = tenantService;
    }

    @GetMapping("/hard-ceiling")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'ADMIN', 'MANAGER', 'AUDITOR')")
    @Transactional(readOnly = true)
    public String hardCeiling(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        // Use tenant business date for "today" boundary (not system clock)
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);
        LocalDateTime todayStart = businessDate.atStartOfDay();
        LocalDateTime todayEnd = businessDate.atTime(LocalTime.MAX);

        // Query 1: Today's violation count
        long todayViolationCount =
                auditLogRepository.countByTenantIdAndActionAndTimestampBetween(
                        tenantId, HARD_LIMIT_ACTION, todayStart, todayEnd);

        // Query 2: Last 20 violations (all time, most recent first)
        List<AuditLog> last20Violations =
                auditLogRepository.findTop20ByTenantIdAndActionOrderByTimestampDesc(
                        tenantId, HARD_LIMIT_ACTION);

        model.addAttribute("todayViolationCount", todayViolationCount);
        model.addAttribute("last20Violations", last20Violations);
        model.addAttribute("businessDate", businessDate);

        return "risk/risk-hard-ceiling";
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
                    "Tenant context is not set for hard ceiling dashboard operation");
        }
        return tenantId;
    }
}
