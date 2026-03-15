package com.ledgora.loan.controller;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.loan.dto.LoanSchedulePreviewDTO;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanProductRepository;
import com.ledgora.loan.service.LoanDisbursementService;
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
 * Loan Disbursement Controller — CBS-grade loan onboarding and disbursement.
 *
 * <p>Separated from LoanDashboardController per Finacle module structure:
 *
 * <ul>
 *   <li>GET /loan/create — loan onboarding form (product + account selection)
 *   <li>POST /loan/preview — schedule preview (Finacle LACSMNT — non-persisting simulation)
 *   <li>POST /loan/create — loan disbursement (creates LoanAccount + schedule + voucher posting)
 * </ul>
 *
 * <p>Two-step onboarding flow per Finacle LACSMNT: Input → Preview (simulation) → Edit → Re-preview
 * (optional) → Confirm → Create + Voucher.
 *
 * <p>All disbursements flow through the voucher engine via {@link LoanDisbursementService}.
 * Tenant-scoped, RBAC-enforced.
 */
@Controller
@RequestMapping("/loan")
public class LoanDisbursementController {

    private static final Logger log = LoggerFactory.getLogger(LoanDisbursementController.class);

    private final LoanAccountRepository loanAccountRepository;
    private final LoanProductRepository loanProductRepository;
    private final AccountRepository accountRepository;
    private final LoanDisbursementService disbursementService;
    private final TenantRepository tenantRepository;

    public LoanDisbursementController(
            LoanAccountRepository loanAccountRepository,
            LoanProductRepository loanProductRepository,
            AccountRepository accountRepository,
            LoanDisbursementService disbursementService,
            TenantRepository tenantRepository) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanProductRepository = loanProductRepository;
        this.accountRepository = accountRepository;
        this.disbursementService = disbursementService;
        this.tenantRepository = tenantRepository;
    }

    /** Loan onboarding form — select product, linked account, enter principal. */
    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String createForm(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        List<LoanProduct> products = loanProductRepository.findByTenantIdAndIsActiveTrue(tenantId);
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
            Model model,
            HttpSession session) {
        Long tenantId = resolveTenantId(session);
        try {
            Tenant tenant =
                    tenantRepository
                            .findById(tenantId)
                            .orElseThrow(() -> new RuntimeException("Tenant not found"));
            LoanProduct product =
                    loanProductRepository
                            .findById(productId)
                            .filter(p -> p.getTenant().getId().equals(tenantId))
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Loan product not found for this tenant"));
            Account linkedAccount =
                    accountRepository
                            .findByIdAndTenantId(linkedAccountId, tenantId)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Linked account not found for this tenant"));

            java.time.LocalDate businessDate = tenant.getCurrentBusinessDate();
            LoanSchedulePreviewDTO preview =
                    disbursementService.previewSchedule(product, principalAmount, businessDate);

            // Pass preview + original form values back for the confirm step
            model.addAttribute("preview", preview);
            model.addAttribute("selectedProductId", productId);
            model.addAttribute("selectedAccountId", linkedAccountId);
            model.addAttribute("selectedAccountNumber", linkedAccount.getAccountNumber());
            model.addAttribute(
                    "selectedAccountName",
                    linkedAccount.getCustomerName() != null
                            ? linkedAccount.getCustomerName()
                            : linkedAccount.getAccountName());
            model.addAttribute("principalAmount", principalAmount);
            model.addAttribute("businessDate", businessDate);

            // Also pass products/accounts for the form (in case user wants to go back)
            model.addAttribute(
                    "products", loanProductRepository.findByTenantIdAndIsActiveTrue(tenantId));
            model.addAttribute(
                    "accounts",
                    accountRepository.findByTenantIdAndStatus(
                            tenantId, com.ledgora.common.enums.AccountStatus.ACTIVE));

            return "loan/loan-create";
        } catch (Exception e) {
            model.addAttribute("error", "Preview failed: " + e.getMessage());
            model.addAttribute(
                    "products", loanProductRepository.findByTenantIdAndIsActiveTrue(tenantId));
            model.addAttribute(
                    "accounts",
                    accountRepository.findByTenantIdAndStatus(
                            tenantId, com.ledgora.common.enums.AccountStatus.ACTIVE));
            return "loan/loan-create";
        }
    }

    /** Loan disbursement — creates LoanAccount + schedule + voucher posting. CBS Tier-1. */
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
            Tenant tenant =
                    tenantRepository
                            .findById(tenantId)
                            .orElseThrow(() -> new RuntimeException("Tenant not found"));

            // CBS Tier-1: tenant-scoped product lookup — prevent cross-tenant reference
            LoanProduct product =
                    loanProductRepository
                            .findById(productId)
                            .filter(p -> p.getTenant().getId().equals(tenantId))
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Loan product not found for this tenant"));

            // CBS Tier-1: tenant-scoped account lookup — prevent cross-tenant reference
            Account linkedAccount =
                    accountRepository
                            .findByIdAndTenantId(linkedAccountId, tenantId)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Linked account not found for this tenant"));

            // CBS-grade loan number: LN-<tenantCode>-<YYYYMMDD>-<sequence>
            String datePart = tenant.getCurrentBusinessDate().toString().replace("-", "");
            long seq = loanAccountRepository.countByTenantId(tenantId) + 1;
            String loanNumber =
                    "LN-"
                            + tenant.getTenantCode()
                            + "-"
                            + datePart
                            + "-"
                            + String.format("%04d", seq);

            LoanAccount loan =
                    disbursementService.disburse(
                            tenant, product, linkedAccount, loanNumber, principalAmount);

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Loan "
                            + loan.getLoanAccountNumber()
                            + " disbursed successfully. Principal: "
                            + principalAmount);
            return "redirect:/loan/" + loan.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Disbursement failed: " + e.getMessage());
            return "redirect:/loan/create";
        }
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
