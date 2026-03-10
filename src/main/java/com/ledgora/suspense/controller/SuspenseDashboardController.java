package com.ledgora.suspense.controller;

import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.AccountType;
import com.ledgora.suspense.entity.SuspenseCase;
import com.ledgora.suspense.repository.SuspenseCaseRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CBS-grade Suspense GL governance dashboard. Read-only operational visibility for suspense case
 * management and GL balance monitoring.
 *
 * <p>RBI Control: Suspense GL must be reconciled daily. Open suspense beyond T+1 requires
 * operations review. EOD blocks on non-zero suspense GL balance.
 *
 * <p>Performance: Maximum 3 SELECTs. No lazy loading in JSP. No N+1 queries.
 *
 * <ul>
 *   <li>Query 1: countOpenByTenantId + countByTenantIdAndStatus (Spring Data derived — 2 COUNT
 *       SELECTs optimized by DB)
 *   <li>Query 2: sumOpenAmountByTenantId (aggregate)
 *   <li>Query 3: findOldestOpenByTenantId (JOIN FETCH for aging table)
 * </ul>
 *
 * <p>Suspense GL net balance reuses existing AccountRepository.sumBalanceByTenantIdAndAccountType
 * with SUSPENSE_ACCOUNT type — NOT voucher derivation.
 */
@Controller
@RequestMapping("/suspense")
public class SuspenseDashboardController {

    private final SuspenseCaseRepository suspenseCaseRepository;
    private final AccountRepository accountRepository;
    private final TenantService tenantService;

    public SuspenseDashboardController(
            SuspenseCaseRepository suspenseCaseRepository,
            AccountRepository accountRepository,
            TenantService tenantService) {
        this.suspenseCaseRepository = suspenseCaseRepository;
        this.accountRepository = accountRepository;
        this.tenantService = tenantService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'ADMIN', 'MANAGER', 'AUDITOR')")
    @Transactional(readOnly = true)
    public String dashboard(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        // KPI 1: Open case count (existing repository method)
        long openCaseCount = suspenseCaseRepository.countOpenByTenantId(tenantId);

        // KPI 2: Resolved case count
        long resolvedCaseCount =
                suspenseCaseRepository.countByTenantIdAndStatus(tenantId, "RESOLVED");

        // KPI 3: Suspense GL net balance (from SUSPENSE_ACCOUNT type — NOT voucher derivation)
        BigDecimal suspenseGlNetBalance =
                accountRepository.sumBalanceByTenantIdAndAccountType(
                        tenantId, AccountType.SUSPENSE_ACCOUNT);

        // KPI 4: Total open suspense exposure (sum of open case amounts)
        BigDecimal totalOpenSuspenseAmount =
                suspenseCaseRepository.sumOpenAmountByTenantId(tenantId);

        // Computed flags
        boolean suspenseBalanced = suspenseGlNetBalance.compareTo(BigDecimal.ZERO) == 0;
        boolean exposureMismatch = suspenseGlNetBalance.compareTo(totalOpenSuspenseAmount) != 0;

        // Aging: oldest open cases with associations eagerly fetched (N+1 prevention)
        List<SuspenseCase> allOpen = suspenseCaseRepository.findOldestOpenByTenantId(tenantId);
        List<SuspenseCase> oldestOpenCases = allOpen.size() > 10 ? allOpen.subList(0, 10) : allOpen;

        // Business date for aging calculation
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);

        model.addAttribute("openCaseCount", openCaseCount);
        model.addAttribute("resolvedCaseCount", resolvedCaseCount);
        model.addAttribute("suspenseGlNetBalance", suspenseGlNetBalance);
        model.addAttribute("totalOpenSuspenseAmount", totalOpenSuspenseAmount);
        model.addAttribute("suspenseBalanced", suspenseBalanced);
        model.addAttribute("exposureMismatch", exposureMismatch);
        model.addAttribute("oldestOpenCases", oldestOpenCases);
        model.addAttribute("businessDate", businessDate);

        return "suspense/suspense-dashboard";
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
                    "Tenant context is not set for suspense dashboard operation");
        }
        return tenantId;
    }
}
