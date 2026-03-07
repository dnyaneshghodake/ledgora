package com.ledgora.reporting.controller;

import com.ledgora.reporting.dto.*;
import com.ledgora.reporting.service.ReportingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * PART 8: Reporting controller for JSP-based report views and REST endpoints.
 */
@Controller
@RequestMapping("/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping
    public String reportsDashboard(Model model) {
        model.addAttribute("today", LocalDate.now());
        return "report/reports";
    }

    @GetMapping("/trial-balance")
    public String trialBalance(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        TrialBalanceReport report = reportingService.generateTrialBalance(reportDate);
        model.addAttribute("report", report);
        return "report/trial-balance";
    }

    @GetMapping("/account-statement")
    public String accountStatement(@RequestParam(required = false) String accountNumber,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   Model model) {
        if (accountNumber != null && !accountNumber.isEmpty()) {
            LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
            LocalDate end = endDate != null ? endDate : LocalDate.now();
            AccountStatementReport report = reportingService.generateAccountStatement(accountNumber, start, end);
            model.addAttribute("report", report);
        }
        return "report/account-statement";
    }

    @GetMapping("/daily-summary")
    public String dailySummary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        DailyTransactionSummary report = reportingService.generateDailyTransactionSummary(reportDate);
        model.addAttribute("report", report);
        return "report/daily-summary";
    }

    @GetMapping("/liquidity")
    public String liquidityReport(Model model) {
        LiquidityReport report = reportingService.generateLiquidityReport();
        model.addAttribute("report", report);
        return "report/liquidity";
    }
}
