package com.ledgora.risk.controller;

import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.fraud.entity.FraudAlert;
import com.ledgora.fraud.repository.FraudAlertRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Velocity Fraud Risk dashboard. Read-only governance visibility into velocity-based fraud
 * detection events and account freezes.
 *
 * <p>RBI Fraud Risk Management: Velocity breaches auto-freeze accounts to UNDER_REVIEW pending
 * investigation. This dashboard provides operational monitoring without modifying
 * VelocityFraudService or freeze logic.
 *
 * <p>Performance: Max 3 SELECTs. No N+1. No lazy loading in JSP.
 *
 * <ul>
 *   <li>Query 1: countOpenByTenantId (open fraud alerts)
 *   <li>Query 2: countByTenantIdAndStatus (UNDER_REVIEW accounts)
 *   <li>Query 3: findTop20ByTenantIdOrderByCreatedAtDesc (recent alerts)
 * </ul>
 */
@Controller
@RequestMapping("/risk")
public class VelocityFraudDashboardController {

    private final FraudAlertRepository fraudAlertRepository;
    private final AccountRepository accountRepository;

    public VelocityFraudDashboardController(
            FraudAlertRepository fraudAlertRepository, AccountRepository accountRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/velocity")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'ADMIN', 'MANAGER', 'AUDITOR')")
    @Transactional(readOnly = true)
    public String velocity(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        // Query 1: Open fraud alerts
        long openFraudAlerts = fraudAlertRepository.countOpenByTenantId(tenantId);

        // Query 2: Accounts frozen to UNDER_REVIEW by velocity engine
        long frozenAccountsCount =
                accountRepository.countByTenantIdAndStatus(tenantId, AccountStatus.UNDER_REVIEW);

        // Query 3: Recent alerts (all statuses, most recent first)
        List<FraudAlert> recentAlerts =
                fraudAlertRepository.findTop20ByTenantIdOrderByCreatedAtDesc(tenantId);

        // Computed: fraud pressure level
        String fraudPressureLevel;
        if (openFraudAlerts == 0) {
            fraudPressureLevel = "LOW";
        } else if (openFraudAlerts <= 5) {
            fraudPressureLevel = "MEDIUM";
        } else {
            fraudPressureLevel = "HIGH";
        }

        model.addAttribute("openFraudAlerts", openFraudAlerts);
        model.addAttribute("frozenAccountsCount", frozenAccountsCount);
        model.addAttribute("recentAlerts", recentAlerts);
        model.addAttribute("fraudPressureLevel", fraudPressureLevel);

        return "risk/risk-velocity";
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
                    "Tenant context is not set for velocity fraud dashboard operation");
        }
        return tenantId;
    }
}
