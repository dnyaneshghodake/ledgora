package com.ledgora.loan.controller;

import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.service.LoanNpaService;
import com.ledgora.loan.service.LoanProvisionService;
import com.ledgora.loan.service.LoanWriteOffService;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Loan NPA Controller — RBI IRAC compliant NPA management.
 *
 * <p>CBS-grade NPA operations per RBI Prudential Norms:
 *
 * <ul>
 *   <li>GET /loan/npa — NPA monitor dashboard (NPA loans + at-risk loans)
 *   <li>POST /loan/npa/evaluate — trigger DPD/NPA evaluation (admin-only)
 *   <li>POST /loan/npa/provision — trigger provision recalculation (admin-only)
 *   <li>POST /loan/npa/{id}/writeoff — write off a LOSS-classified loan (admin-only)
 * </ul>
 *
 * <p>Separated from LoanDashboardController per Finacle module structure.
 * Tenant-scoped, RBAC-enforced.
 */
@Controller
@RequestMapping("/loan/npa")
public class LoanNpaController {

    private static final Logger log = LoggerFactory.getLogger(LoanNpaController.class);

    private final LoanAccountRepository loanAccountRepository;
    private final LoanNpaService loanNpaService;
    private final LoanProvisionService loanProvisionService;
    private final LoanWriteOffService loanWriteOffService;

    public LoanNpaController(
            LoanAccountRepository loanAccountRepository,
            LoanNpaService loanNpaService,
            LoanProvisionService loanProvisionService,
            LoanWriteOffService loanWriteOffService) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanNpaService = loanNpaService;
        this.loanProvisionService = loanProvisionService;
        this.loanWriteOffService = loanWriteOffService;
    }

    /** NPA monitor — NPA loans, at-risk loans, classification distribution. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK')")
    public String npaMonitor(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        List<LoanAccount> npaLoans =
                loanAccountRepository.findByTenantIdAndStatus(tenantId, LoanStatus.NPA);
        List<LoanAccount> allLoans =
                loanAccountRepository.findActiveAndNpaByTenantId(tenantId);

        // At-risk: active loans with DPD > 0 (approaching NPA threshold)
        List<LoanAccount> atRisk =
                allLoans.stream()
                        .filter(l -> l.getStatus() == LoanStatus.ACTIVE && l.getDpd() > 0)
                        .toList();

        // Classification distribution
        long substandardCount = npaLoans.stream()
                .filter(l -> l.getNpaClassification() == NpaClassification.SUBSTANDARD).count();
        long doubtfulCount = npaLoans.stream()
                .filter(l -> l.getNpaClassification() == NpaClassification.DOUBTFUL).count();
        long lossCount = npaLoans.stream()
                .filter(l -> l.getNpaClassification() == NpaClassification.LOSS).count();

        BigDecimal totalNpaOutstanding = npaLoans.stream()
                .map(LoanAccount::getOutstandingPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProvision = npaLoans.stream()
                .map(LoanAccount::getProvisionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("npaLoans", npaLoans);
        model.addAttribute("atRiskLoans", atRisk);
        model.addAttribute("substandardCount", substandardCount);
        model.addAttribute("doubtfulCount", doubtfulCount);
        model.addAttribute("lossCount", lossCount);
        model.addAttribute("totalNpaOutstanding", totalNpaOutstanding);
        model.addAttribute("totalProvision", totalProvision);
        return "loan/loan-npa-monitor";
    }

    /** Trigger DPD/NPA evaluation — admin-only, typically runs during EOD. */
    @PostMapping("/evaluate")
    @PreAuthorize("hasRole('ADMIN')")
    public String evaluateNpa(HttpSession session, RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        try {
            int newNpaCount = loanNpaService.evaluateNpaAndUpdateDpd(tenantId);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "NPA evaluation complete. New NPA classifications: " + newNpaCount);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "NPA evaluation failed: " + e.getMessage());
        }
        return "redirect:/loan/npa";
    }

    /** Trigger provision recalculation — admin-only, typically runs during EOD. */
    @PostMapping("/provision")
    @PreAuthorize("hasRole('ADMIN')")
    public String recalculateProvisions(
            HttpSession session, RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        try {
            BigDecimal incremental = loanProvisionService.calculateProvisions(tenantId);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Provision recalculation complete. Incremental provision: " + incremental);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "Provision recalculation failed: " + e.getMessage());
        }
        return "redirect:/loan/npa";
    }

    /** Write off a LOSS-classified loan — admin-only per RBI Prudential Norms. */
    @PostMapping("/{id}/writeoff")
    @PreAuthorize("hasRole('ADMIN')")
    public String writeOff(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        // Tenant isolation
        LoanAccount loan = loanAccountRepository.findById(id).orElse(null);
        if (loan == null
                || loan.getTenant() == null
                || !loan.getTenant().getId().equals(tenantId)) {
            redirectAttributes.addFlashAttribute("error", "Loan not found or access denied");
            return "redirect:/loan/npa";
        }
        try {
            loanWriteOffService.writeOff(id);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Loan " + loan.getLoanAccountNumber() + " written off successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "Write-off failed: " + e.getMessage());
        }
        return "redirect:/loan/npa";
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
