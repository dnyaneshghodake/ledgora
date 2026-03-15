package com.ledgora.loan.controller;

import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.service.LoanEmiPaymentService;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Loan Repayment Controller — CBS-grade EMI payment processing.
 *
 * <p>Separated from LoanDashboardController per Finacle module structure:
 *
 * <ul>
 *   <li>POST /loan/{id}/repay — process EMI payment (principal + interest split)
 * </ul>
 *
 * <p>All payments flow through the voucher engine via {@link LoanEmiPaymentService}. Tenant-scoped,
 * RBAC-enforced.
 */
@Controller
@RequestMapping("/loan")
public class LoanRepaymentController {

    private static final Logger log = LoggerFactory.getLogger(LoanRepaymentController.class);

    private final LoanAccountRepository loanAccountRepository;
    private final LoanEmiPaymentService emiPaymentService;

    public LoanRepaymentController(
            LoanAccountRepository loanAccountRepository, LoanEmiPaymentService emiPaymentService) {
        this.loanAccountRepository = loanAccountRepository;
        this.emiPaymentService = emiPaymentService;
    }

    /**
     * Process EMI payment — CBS Tier-1 maker-initiated.
     *
     * <p>Voucher posting: DR Customer Account, CR Loan Asset GL (principal) + CR Interest
     * Receivable GL (interest).
     */
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
            redirectAttributes.addFlashAttribute("error", "Loan not found or access denied");
            return "redirect:/loan/list";
        }
        try {
            emiPaymentService.processEmiPayment(id, principalAmount, interestAmount);
            redirectAttributes.addFlashAttribute("message", "EMI payment processed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Payment failed: " + e.getMessage());
        }
        return "redirect:/loan/" + id;
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
