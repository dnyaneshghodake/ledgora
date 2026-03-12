package com.ledgora.reporting.controller;

import com.ledgora.reporting.dto.DailyTransactionSummary;
import com.ledgora.reporting.dto.LiquidityReport;
import com.ledgora.reporting.dto.TrialBalanceReport;
import com.ledgora.reporting.service.ReportingService;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Financial reports controller. Routes: GET /reports — Reports landing page GET
 * /reports/trial-balance — Trial balance report GET /reports/daily-summary — Daily transaction
 * summary GET /reports/liquidity — Liquidity report
 */
@Controller
@RequestMapping("/reports")
public class ReportsController {

    private final ReportingService reportingService;
    private final TenantService tenantService;

    public ReportsController(ReportingService reportingService, TenantService tenantService) {
        this.reportingService = reportingService;
        this.tenantService = tenantService;
    }

    /** Reports landing page. */
    @GetMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR', 'BRANCH_MANAGER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String reportsHome(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        model.addAttribute("businessDate", tenantService.getCurrentBusinessDate(tenantId));
        return "report/reports";
    }

    /** Trial Balance report. */
    @GetMapping("/trial-balance")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR', 'BRANCH_MANAGER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String trialBalance(
            @RequestParam(value = "reportDate", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate reportDate,
            Model model,
            HttpSession session) {
        Long tenantId = resolveTenantId(session);
        if (reportDate == null) {
            reportDate = tenantService.getCurrentBusinessDate(tenantId);
        }
        TrialBalanceReport report = reportingService.generateTrialBalance(reportDate);
        model.addAttribute("report", report);
        model.addAttribute("reportDate", reportDate);
        return "report/trial-balance";
    }

    /** Daily Transaction Summary report. */
    @GetMapping("/daily-summary")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR', 'BRANCH_MANAGER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String dailySummary(
            @RequestParam(value = "reportDate", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate reportDate,
            Model model,
            HttpSession session) {
        Long tenantId = resolveTenantId(session);
        if (reportDate == null) {
            reportDate = tenantService.getCurrentBusinessDate(tenantId);
        }
        DailyTransactionSummary summary =
                reportingService.generateDailyTransactionSummary(reportDate);
        model.addAttribute("summary", summary);
        model.addAttribute("reportDate", reportDate);
        return "report/daily-summary";
    }

    /** Liquidity report. */
    @GetMapping("/liquidity")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String liquidityReport(Model model, HttpSession session) {
        resolveTenantId(session); // ensures tenant context is set
        LiquidityReport report = reportingService.generateLiquidityReport();
        model.addAttribute("report", report);
        return "report/liquidity";
    }

    /** Account Statement report. */
    @GetMapping("/account-statement")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR', 'TELLER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String accountStatement(
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            @RequestParam(value = "startDate", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            Model model,
            HttpSession session) {
        resolveTenantId(session);
        if (accountNumber != null && !accountNumber.isBlank()) {
            if (startDate == null) startDate = LocalDate.now().minusMonths(1);
            if (endDate == null) endDate = LocalDate.now();
            com.ledgora.reporting.dto.AccountStatementReport statement =
                    reportingService.generateAccountStatement(accountNumber, startDate, endDate);
            model.addAttribute("statement", statement);
        }
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "report/account-statement";
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
        TenantContextHolder.setTenantId(tenantId);
        return tenantId;
    }
}
