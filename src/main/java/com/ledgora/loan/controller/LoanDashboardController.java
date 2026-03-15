package com.ledgora.loan.controller;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.loan.dto.LoanSchedulePreviewDTO;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanProductRepository;
import com.ledgora.loan.service.LoanDisbursementService;
import com.ledgora.loan.service.LoanEmiPaymentService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Finacle-grade Loan Module UI Controller.
 *
 * <p>Loan balances derive from LedgerEntry-based aggregates via LoanAccount entity. All
 * repayments flow through the voucher engine. Tenant-scoped, RBAC-enforced.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /loan/dashboard — portfolio summary with NPA distribution
 *   <li>GET /loan/list — paginated loan list with filters
 *   <li>GET /loan/{id} — detailed loan view with product, schedule + ledger
 *   <li>GET /loan/npa-monitor — NPA classification monitor
 *   <li>GET /loan/create — loan onboarding form (product + account selection)
 *   <li>POST /loan/create — loan disbursement (creates LoanAccount + schedule)
 *   <li>POST /loan/{id}/repay — EMI payment (maker-checker)
 * </ul>
 */
@Controller
@RequestMapping("/loan")
public class LoanDashboardController {

    private static final Logger log = LoggerFactory.getLogger(LoanDashboardController.class);

    private final LoanAccountRepository loanAccountRepository;
    private final LoanProductRepository loanProductRepository;
    private final AccountRepository accountRepository;
    private final LoanDisbursementService disbursementService;
    private final LoanEmiPaymentService emiPaymentService;
    private final TenantRepository tenantRepository;

    public LoanDashboardController(
            LoanAccountRepository loanAccountRepository,
            LoanProductRepository loanProductRepository,
            AccountRepository accountRepository,
            LoanDisbursementService disbursementService,
            LoanEmiPaymentService emiPaymentService,
            TenantRepository tenantRepository) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanProductRepository = loanProductRepository;
        this.accountRepository = accountRepository;
        this.disbursementService = disbursementService;
        this.emiPaymentService = emiPaymentService;
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
                case STANDARD -> { standardCount++; standardAmt = standardAmt.add(loan.getOutstandingPrincipal()); }
                case SUBSTANDARD -> { substandardCount++; substandardAmt = substandardAmt.add(loan.getOutstandingPrincipal()); }
                case DOUBTFUL -> { doubtfulCount++; doubtfulAmt = doubtfulAmt.add(loan.getOutstandingPrincipal()); }
                case LOSS -> { lossCount++; lossAmt = lossAmt.add(loan.getOutstandingPrincipal()); }
            }
            if (loan.getStatus() == LoanStatus.NPA) npaCount++;
        }

        BigDecimal npaPercent = totalOutstanding.compareTo(BigDecimal.ZERO) > 0
                ? substandardAmt.add(doubtfulAmt).add(lossAmt)
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
            Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        List<LoanAccount> loans;
        if (status != null && !status.isEmpty()) {
            loans = loanAccountRepository.findByTenantIdAndStatus(
                    tenantId, LoanStatus.valueOf(status));
        } else {
            loans = loanAccountRepository.findActiveAndNpaByTenantId(tenantId);
        }

        // Filter by NPA category if specified
        if (npaCategory != null && !npaCategory.isEmpty()) {
            NpaClassification filter = NpaClassification.valueOf(npaCategory);
            loans = loans.stream()
                    .filter(l -> l.getNpaClassification() == filter)
                    .toList();
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
        return "loan/loan-detail";
    }

    @GetMapping("/npa-monitor")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK')")
    public String npaMonitor(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        List<LoanAccount> npaLoans =
                loanAccountRepository.findByTenantIdAndStatus(tenantId, LoanStatus.NPA);
        List<LoanAccount> allLoans =
                loanAccountRepository.findActiveAndNpaByTenantId(tenantId);

        // Include active loans with DPD > 0 (approaching NPA)
        List<LoanAccount> atRisk = allLoans.stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE && l.getDpd() > 0)
                .toList();

        model.addAttribute("npaLoans", npaLoans);
        model.addAttribute("atRiskLoans", atRisk);
        return "loan/loan-npa-monitor";
    }

    /** Loan onboarding form — select product, linked account, enter principal. */
    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String createForm(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        List<LoanProduct> products =
                loanProductRepository.findByTenantIdAndIsActiveTrue(tenantId);
        List<Account> accounts =
                accountRepository.findByTenantIdAndStatus(
                        tenantId, com.ledgora.common.enums.AccountStatus.ACTIVE);
        model.addAttribute("products", products);
        model.addAttribute("accounts", accounts);
        return "loan/loan-create";
    }

    /**
     * Schedule preview — computes EMI amortization without persisting (Finacle LACSMNT preview).
     * The maker reviews the full repayment schedule before confirming disbursement.
     */
    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String previewSchedule(
            @RequestParam Long productId,
            @RequestParam Long linkedAccountId,
            @RequestParam BigDecimal principalAmount,
            Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        try {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            LoanProduct product = loanProductRepository.findById(productId)
                    .filter(p -> p.getTenant().getId().equals(tenantId))
                    .orElseThrow(() -> new RuntimeException("Loan product not found for this tenant"));
            Account linkedAccount = accountRepository.findByIdAndTenantId(linkedAccountId, tenantId)
                    .orElseThrow(() -> new RuntimeException("Linked account not found for this tenant"));

            java.time.LocalDate businessDate = tenant.getCurrentBusinessDate();
            LoanSchedulePreviewDTO preview =
                    disbursementService.previewSchedule(product, principalAmount, businessDate);

            // Pass preview + original form values back for the confirm step
            model.addAttribute("preview", preview);
            model.addAttribute("selectedProductId", productId);
            model.addAttribute("selectedAccountId", linkedAccountId);
            model.addAttribute("selectedAccountNumber", linkedAccount.getAccountNumber());
            model.addAttribute("selectedAccountName",
                    linkedAccount.getCustomerName() != null
                            ? linkedAccount.getCustomerName()
                            : linkedAccount.getAccountName());
            model.addAttribute("principalAmount", principalAmount);
            model.addAttribute("businessDate", businessDate);

            // Also pass products/accounts for the form (in case user wants to go back)
            model.addAttribute("products",
                    loanProductRepository.findByTenantIdAndIsActiveTrue(tenantId));
            model.addAttribute("accounts",
                    accountRepository.findByTenantIdAndStatus(
                            tenantId, com.ledgora.common.enums.AccountStatus.ACTIVE));

            return "loan/loan-create";
        } catch (Exception e) {
            model.addAttribute("error", "Preview failed: " + e.getMessage());
            model.addAttribute("products",
                    loanProductRepository.findByTenantIdAndIsActiveTrue(tenantId));
            model.addAttribute("accounts",
                    accountRepository.findByTenantIdAndStatus(
                            tenantId, com.ledgora.common.enums.AccountStatus.ACTIVE));
            return "loan/loan-create";
        }
    }

    /** Loan disbursement — creates LoanAccount + schedule. CBS Tier-1: maker-initiated. */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String disburse(
            @RequestParam Long productId,
            @RequestParam Long linkedAccountId,
            @RequestParam BigDecimal principalAmount,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        try {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));

            // CBS Tier-1: tenant-scoped product lookup — prevent cross-tenant reference
            LoanProduct product = loanProductRepository.findById(productId)
                    .filter(p -> p.getTenant().getId().equals(tenantId))
                    .orElseThrow(() -> new RuntimeException("Loan product not found for this tenant"));

            // CBS Tier-1: tenant-scoped account lookup — prevent cross-tenant reference
            Account linkedAccount = accountRepository.findByIdAndTenantId(linkedAccountId, tenantId)
                    .orElseThrow(() -> new RuntimeException("Linked account not found for this tenant"));

            // CBS-grade loan number: LN-<tenantCode>-<YYYYMMDD>-<sequence>
            String datePart = tenant.getCurrentBusinessDate().toString().replace("-", "");
            long seq = loanAccountRepository.findActiveAndNpaByTenantId(tenantId).size() + 1;
            String loanNumber = "LN-" + tenant.getTenantCode() + "-" + datePart + "-"
                    + String.format("%04d", seq);

            LoanAccount loan = disbursementService.disburse(
                    tenant, product, linkedAccount, loanNumber, principalAmount);

            redirectAttributes.addFlashAttribute("message",
                    "Loan " + loan.getLoanAccountNumber() + " disbursed successfully. Principal: "
                            + principalAmount);
            return "redirect:/loan/" + loan.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Disbursement failed: " + e.getMessage());
            return "redirect:/loan/create";
        }
    }

    @PostMapping("/{id}/repay")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String repay(
            @PathVariable Long id,
            @RequestParam BigDecimal principalAmount,
            @RequestParam BigDecimal interestAmount,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        // Tenant isolation: verify loan belongs to current tenant before payment
        LoanAccount loan = loanAccountRepository.findById(id).orElse(null);
        if (loan == null
                || loan.getTenant() == null
                || !loan.getTenant().getId().equals(tenantId)) {
            redirectAttributes.addFlashAttribute("error",
                    "Loan not found or access denied");
            return "redirect:/loan/list";
        }
        try {
            emiPaymentService.processEmiPayment(id, principalAmount, interestAmount);
            redirectAttributes.addFlashAttribute("message",
                    "EMI payment processed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Payment failed: " + e.getMessage());
        }
        return "redirect:/loan/" + id;
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
