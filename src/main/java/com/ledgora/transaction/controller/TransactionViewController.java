package com.ledgora.transaction.controller;

import com.ledgora.transaction.dto.view.TransactionViewDTO;
import com.ledgora.transaction.service.TransactionViewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CBS-Grade Transaction 360° View Controller.
 *
 * <p>Provides the unified transaction detail screen used by: 1. Maker — immediately after
 * transaction creation 2. Checker — when opening transaction for authorization 3. Inquiry list —
 * drill-down from transaction list 4. Voucher drill-down — from voucher detail 5. IBT detail
 * redirect — from inter-branch transfer screen
 *
 * <p>No business logic in controller. All aggregation delegated to TransactionViewService.
 */
@Controller
@RequestMapping("/transactions")
public class TransactionViewController {

    private final TransactionViewService transactionViewService;

    public TransactionViewController(TransactionViewService transactionViewService) {
        this.transactionViewService = transactionViewService;
    }

    /**
     * Transaction 360° View — read-only detail screen.
     *
     * <p>Accessible by all CBS roles: MAKER, CHECKER, TELLER, ADMIN, MANAGER, OPERATIONS, AUDITOR.
     */
    @GetMapping("/{id}/view")
    @PreAuthorize(
            "hasAnyRole('MAKER', 'CHECKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String viewTransaction360(@PathVariable Long id, Model model) {
        TransactionViewDTO dto = transactionViewService.buildTransactionView(id, false);
        model.addAttribute("txn", dto);
        model.addAttribute("isAuthorizeView", false);
        model.addAttribute("isAuditor", isCurrentUserAuditor());
        return "transaction/transaction-360";
    }

    /**
     * Transaction 360° Authorization View — same screen with authorization panel.
     *
     * <p>Only CHECKER, ADMIN, MANAGER can access. AUDITOR sees read-only (no approve button). Maker
     * != Checker enforcement is handled by TransactionService.approveTransaction().
     */
    @GetMapping("/{id}/authorize")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER', 'AUDITOR')")
    public String authorizeTransaction360(@PathVariable Long id, Model model) {
        TransactionViewDTO dto = transactionViewService.buildTransactionView(id, true);
        model.addAttribute("txn", dto);
        model.addAttribute("isAuthorizeView", true);
        model.addAttribute("isAuditor", isCurrentUserAuditor());
        return "transaction/transaction-360";
    }

    /** Check if the current user has AUDITOR role (read-only, no approve button). */
    private boolean isCurrentUserAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        boolean hasAuditor =
                auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_AUDITOR"));
        boolean hasChecker =
                auth.getAuthorities().stream()
                        .anyMatch(
                                a ->
                                        a.getAuthority().equals("ROLE_CHECKER")
                                                || a.getAuthority().equals("ROLE_ADMIN")
                                                || a.getAuthority().equals("ROLE_MANAGER"));
        // If user has AUDITOR but NOT CHECKER/ADMIN/MANAGER, they are read-only
        return hasAuditor && !hasChecker;
    }
}
