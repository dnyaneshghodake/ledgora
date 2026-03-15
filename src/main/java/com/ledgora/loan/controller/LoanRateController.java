package com.ledgora.loan.controller;

import com.ledgora.loan.dto.LoanRateDTO;
import com.ledgora.loan.entity.LoanRate;
import com.ledgora.loan.entity.LoanRateChangeHistory;
import com.ledgora.loan.repository.LoanRateChangeHistoryRepository;
import com.ledgora.loan.repository.LoanRateRepository;
import com.ledgora.loan.service.LoanRateService;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Loan Rate Controller — Finacle LARATE equivalent.
 *
 * <p>CBS-grade rate management per RBI Fair Practices Code:
 *
 * <ul>
 *   <li>GET /loan/rates — view all rates and change history
 *   <li>POST /loan/rates/create — create new rate for a product
 *   <li>POST /loan/rates/propagate — propagate floating rate to active loans
 * </ul>
 *
 * <p>Tenant-scoped, RBAC-enforced (ADMIN/MANAGER only).
 */
@Controller
@RequestMapping("/loan/rates")
public class LoanRateController {

    private static final Logger log = LoggerFactory.getLogger(LoanRateController.class);

    private final LoanRateService loanRateService;
    private final LoanRateRepository loanRateRepository;
    private final LoanRateChangeHistoryRepository historyRepository;

    public LoanRateController(
            LoanRateService loanRateService,
            LoanRateRepository loanRateRepository,
            LoanRateChangeHistoryRepository historyRepository) {
        this.loanRateService = loanRateService;
        this.loanRateRepository = loanRateRepository;
        this.historyRepository = historyRepository;
    }

    /** Rate dashboard — current rates + change history for the tenant. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    public String rateList(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        List<LoanRate> rates = loanRateRepository.findByTenantIdOrderByEffectiveDateDesc(tenantId);
        List<LoanRateChangeHistory> history =
                historyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        model.addAttribute("rates", rates);
        model.addAttribute("history", history);
        return "loan/loan-rates";
    }

    /** Create a new rate for a product. */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String createRate(
            @RequestParam Long productId,
            @RequestParam(required = false) String benchmarkName,
            @RequestParam(required = false) BigDecimal benchmarkRate,
            @RequestParam(required = false) BigDecimal spread,
            @RequestParam(required = false) BigDecimal effectiveRate,
            @RequestParam String effectiveDate,
            @RequestParam(required = false) String changeReason,
            @RequestParam(required = false) String remarks,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        try {
            LoanRateDTO dto =
                    LoanRateDTO.builder()
                            .productId(productId)
                            .benchmarkName(benchmarkName)
                            .benchmarkRate(benchmarkRate)
                            .spread(spread)
                            .effectiveRate(effectiveRate)
                            .effectiveDate(LocalDate.parse(effectiveDate))
                            .changeReason(changeReason)
                            .remarks(remarks)
                            .build();
            LoanRate rate = loanRateService.createRate(tenantId, dto);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Rate created: " + rate.getEffectiveRate() + "% effective " + rate.getEffectiveDate());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Rate creation failed: " + e.getMessage());
        }
        return "redirect:/loan/rates";
    }

    /** Propagate floating rate change to active loans of a product. */
    @PostMapping("/propagate")
    @PreAuthorize("hasRole('ADMIN')")
    public String propagateRate(
            @RequestParam Long productId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        try {
            int updated = loanRateService.propagateRateToActiveLoans(tenantId, productId, null);
            redirectAttributes.addFlashAttribute(
                    "message", "Floating rate propagated to " + updated + " active loans");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "Rate propagation failed: " + e.getMessage());
        }
        return "redirect:/loan/rates";
    }

    /** Rate change history for a specific product. */
    @GetMapping("/history/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    public String rateHistory(
            @PathVariable Long productId, Model model, HttpSession session) {
        List<LoanRateChangeHistory> history =
                historyRepository.findByLoanProductIdOrderByCreatedAtDesc(productId);
        List<LoanRate> rates =
                loanRateRepository.findByLoanProductIdOrderByEffectiveDateDesc(productId);
        model.addAttribute("history", history);
        model.addAttribute("rates", rates);
        model.addAttribute("productId", productId);
        return "loan/loan-rate-history";
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
