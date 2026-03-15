package com.ledgora.loan.controller;

import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanSchedule;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanScheduleRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Finacle-grade Loan Module UI Controller.
 *
 * <p>Loan balances derive from LedgerEntry-based aggregates via LoanAccount entity. All repayments
 * flow through the voucher engine. Tenant-scoped, RBAC-enforced.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>GET /loan/dashboard — portfolio summary with NPA distribution
 *   <li>GET /loan/list — paginated loan list with filters
 *   <li>GET /loan/{id} — detailed loan view with product, schedule + ledger
 *   <li>GET /loan/npa-monitor — NPA classification monitor
 * </ul>
 *
 * <p>Disbursement and repayment endpoints are in dedicated controllers: {@link
 * LoanDisbursementController}, {@link LoanRepaymentController}.
 */
@Controller
@RequestMapping("/loan")
public class LoanDashboardController {

    private static final Logger log = LoggerFactory.getLogger(LoanDashboardController.class);

    private final LoanAccountRepository loanAccountRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TenantRepository tenantRepository;

    public LoanDashboardController(
            LoanAccountRepository loanAccountRepository,
            LoanScheduleRepository loanScheduleRepository,
            LedgerEntryRepository ledgerEntryRepository,
            TenantRepository tenantRepository) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK', 'OPERATIONS')")
    public String dashboard(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            model.addAttribute("error", "Tenant not found");
            model.addAttribute("totalPortfolio", BigDecimal.ZERO);
            model.addAttribute("totalOutstanding", BigDecimal.ZERO);
            model.addAttribute("totalLoans", 0);
            model.addAttribute("npaCount", 0L);
            model.addAttribute("npaPercent", BigDecimal.ZERO);
            return "loan/loan-dashboard";
        }

        List<LoanAccount> allLoans = loanAccountRepository.findActiveAndNpaByTenantId(tenantId);

        // Portfolio KPIs
        BigDecimal totalPortfolio = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        long standardCount = 0, substandardCount = 0, doubtfulCount = 0, lossCount = 0;
        BigDecimal standardAmt = BigDecimal.ZERO, substandardAmt = BigDecimal.ZERO;
        BigDecimal doubtfulAmt = BigDecimal.ZERO, lossAmt = BigDecimal.ZERO;
        long npaCount = 0;

        for (LoanAccount loan : allLoans) {
            totalPortfolio = totalPortfolio.add(loan.getPrincipalAmount());
            totalOutstanding = totalOutstanding.add(loan.getOutstandingPrincipal());

            switch (loan.getNpaClassification()) {
                case STANDARD -> {
                    standardCount++;
                    standardAmt = standardAmt.add(loan.getOutstandingPrincipal());
                }
                case SUBSTANDARD -> {
                    substandardCount++;
                    substandardAmt = substandardAmt.add(loan.getOutstandingPrincipal());
                }
                case DOUBTFUL -> {
                    doubtfulCount++;
                    doubtfulAmt = doubtfulAmt.add(loan.getOutstandingPrincipal());
                }
                case LOSS -> {
                    lossCount++;
                    lossAmt = lossAmt.add(loan.getOutstandingPrincipal());
                }
            }
            if (loan.getStatus() == LoanStatus.NPA) npaCount++;
        }

        BigDecimal npaPercent =
                totalOutstanding.compareTo(BigDecimal.ZERO) > 0
                        ? substandardAmt
                                .add(doubtfulAmt)
                                .add(lossAmt)
                                .multiply(new BigDecimal("100"))
                                .divide(totalOutstanding, 2, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        model.addAttribute("tenantName", tenant.getTenantName());
        model.addAttribute("totalPortfolio", totalPortfolio);
        model.addAttribute("totalOutstanding", totalOutstanding);
        model.addAttribute("standardCount", standardCount);
        model.addAttribute("standardAmt", standardAmt);
        model.addAttribute("substandardCount", substandardCount);
        model.addAttribute("substandardAmt", substandardAmt);
        model.addAttribute("doubtfulCount", doubtfulCount);
        model.addAttribute("doubtfulAmt", doubtfulAmt);
        model.addAttribute("lossCount", lossCount);
        model.addAttribute("lossAmt", lossAmt);
        model.addAttribute("npaCount", npaCount);
        model.addAttribute("npaPercent", npaPercent);
        model.addAttribute("totalLoans", allLoans.size());

        return "loan/loan-dashboard";
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK', 'OPERATIONS')")
    public String list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String npaCategory,
            Model model,
            HttpSession session) {
        Long tenantId = resolveTenantId(session);

        List<LoanAccount> loans;
        if (status != null && !status.isEmpty()) {
            loans =
                    loanAccountRepository.findByTenantIdAndStatus(
                            tenantId, LoanStatus.valueOf(status));
        } else {
            loans = loanAccountRepository.findActiveAndNpaByTenantId(tenantId);
        }

        // Filter by NPA category if specified
        if (npaCategory != null && !npaCategory.isEmpty()) {
            NpaClassification filter = NpaClassification.valueOf(npaCategory);
            loans = loans.stream().filter(l -> l.getNpaClassification() == filter).toList();
        }

        model.addAttribute("loans", loans);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedNpaCategory", npaCategory);
        return "loan/loan-list";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK', 'OPERATIONS')")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        // Eager-fetch product + linked account to avoid LazyInitializationException in JSP
        LoanAccount loan = loanAccountRepository.findByIdWithProductAndAccount(id).orElse(null);
        // Tenant isolation: verify loan belongs to current tenant
        if (loan == null
                || loan.getTenant() == null
                || !loan.getTenant().getId().equals(tenantId)) {
            model.addAttribute("error", "Loan not found");
            return "loan/loan-detail";
        }
        model.addAttribute("loan", loan);

        // Finacle LACHST — load repayment schedule for detail view
        List<LoanSchedule> schedule =
                loanScheduleRepository.findByLoanAccountIdOrderByInstallmentNumberAsc(loan.getId());
        model.addAttribute("schedule", schedule);

        // ── CBS AUDIT TRAIL: Load immutable ledger entries for this loan's linked account ──
        // Per CBS/RBI/Finacle Tier-1: the auditor must be able to trace any loan balance
        // back to the immutable ledger entries (vouchers → ledger entries → trial balance).
        if (loan.getLinkedAccount() != null) {
            List<LedgerEntry> ledgerEntries =
                    ledgerEntryRepository.findByAccountId(loan.getLinkedAccount().getId());
            model.addAttribute("ledgerEntries", ledgerEntries);
        }

        return "loan/loan-detail";
    }

    @GetMapping("/npa-monitor")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK')")
    public String npaMonitor(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        List<LoanAccount> npaLoans =
                loanAccountRepository.findByTenantIdAndStatus(tenantId, LoanStatus.NPA);
        List<LoanAccount> allLoans = loanAccountRepository.findActiveAndNpaByTenantId(tenantId);

        // Include active loans with DPD > 0 (approaching NPA)
        List<LoanAccount> atRisk =
                allLoans.stream()
                        .filter(l -> l.getStatus() == LoanStatus.ACTIVE && l.getDpd() > 0)
                        .toList();

        model.addAttribute("npaLoans", npaLoans);
        model.addAttribute("atRiskLoans", atRisk);
        return "loan/loan-npa-monitor";
    }

    // ── Disbursement (GET/POST /loan/create, POST /loan/preview) moved to
    // LoanDisbursementController
    // ── Repayment (POST /loan/{id}/repay) moved to LoanRepaymentController

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
