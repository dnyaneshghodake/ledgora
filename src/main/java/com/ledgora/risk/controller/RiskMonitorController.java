package com.ledgora.risk.controller;

import com.ledgora.approval.entity.HardTransactionLimit;
import com.ledgora.approval.repository.HardTransactionLimitRepository;
import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.fraud.entity.FraudAlert;
import com.ledgora.fraud.entity.VelocityLimit;
import com.ledgora.fraud.repository.FraudAlertRepository;
import com.ledgora.fraud.repository.VelocityLimitRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Risk & Fraud monitoring dashboard controller.
 * Routes:
 *   GET /risk/hard-ceiling  — Hard Transaction Ceiling monitor
 *   GET /risk/velocity      — Velocity Fraud monitor
 */
@Controller
@RequestMapping("/risk")
public class RiskMonitorController {

    private final HardTransactionLimitRepository hardLimitRepository;
    private final VelocityLimitRepository velocityLimitRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final AuditLogRepository auditLogRepository;

    public RiskMonitorController(
            HardTransactionLimitRepository hardLimitRepository,
            VelocityLimitRepository velocityLimitRepository,
            FraudAlertRepository fraudAlertRepository,
            AuditLogRepository auditLogRepository) {
        this.hardLimitRepository = hardLimitRepository;
        this.velocityLimitRepository = velocityLimitRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Hard Transaction Ceiling monitor — shows configured limits and recent violations.
     */
    @GetMapping("/hard-ceiling")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String hardCeilingMonitor(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        // All hard limits for this tenant
        List<HardTransactionLimit> limits = hardLimitRepository.findByTenant_Id(tenantId);

        // Recent hard ceiling violations from audit log
        List<AuditLog> recentViolations =
                auditLogRepository.findTop20ByTenantIdAndActionOrderByTimestampDesc(
                        tenantId, "HARD_LIMIT_EXCEEDED");

        long violationCount24h =
                auditLogRepository.countByTenantIdAndActionAndTimestampBetween(
                        tenantId,
                        "HARD_LIMIT_EXCEEDED",
                        java.time.LocalDateTime.now().minusHours(24),
                        java.time.LocalDateTime.now());

        model.addAttribute("limits", limits);
        model.addAttribute("recentViolations", recentViolations);
        model.addAttribute("violationCount24h", violationCount24h);
        return "risk/hard-ceiling-monitor";
    }

    /**
     * Velocity Fraud monitor — shows configured velocity limits and open fraud alerts.
     */
    @GetMapping("/velocity")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String velocityMonitor(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        // Configured velocity limits
        List<VelocityLimit> limits =
                velocityLimitRepository.findByTenantIdAndIsActiveTrue(tenantId);

        // Open fraud alerts
        List<FraudAlert> openAlerts =
                fraudAlertRepository.findByTenantIdAndStatus(tenantId, "OPEN");

        // Recent 20 alerts (all statuses)
        List<FraudAlert> recentAlerts =
                fraudAlertRepository.findTop20ByTenantIdOrderByCreatedAtDesc(tenantId);

        long openAlertCount = fraudAlertRepository.countOpenByTenantId(tenantId);

        model.addAttribute("velocityLimits", limits);
        model.addAttribute("openAlerts", openAlerts);
        model.addAttribute("recentAlerts", recentAlerts);
        model.addAttribute("openAlertCount", openAlertCount);
        return "risk/velocity-monitor";
    }

    private Long resolveTenantId(HttpSession session) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            Object sessionTenantId = session.getAttribute("tenantId");
            if (sessionTenantId instanceof Number n) tenantId = n.longValue();
            else if (sessionTenantId instanceof String s && !s.isBlank()) tenantId = Long.valueOf(s);
        }
        if (tenantId == null) throw new IllegalStateException("Tenant context not set");
        return tenantId;
    }
}
