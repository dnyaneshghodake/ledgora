package com.ledgora.customer.controller;

import com.ledgora.customer.dto.view.Customer360DTO;
import com.ledgora.customer.service.Customer360Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Customer 360° View Controller.
 *
 * <p>Provides a unified customer detail screen covering: Overview, Accounts, Transactions, IBT
 * Exposure, Suspense Exposure, Risk & Governance, and Audit Trail.
 *
 * <p>No business logic in controller — all aggregation delegated to Customer360Service. Tenant
 * isolation enforced via TenantContextHolder at the service/repository layer.
 */
@Controller
@RequestMapping("/customers")
public class Customer360Controller {

    private final Customer360Service customer360Service;

    public Customer360Controller(Customer360Service customer360Service) {
        this.customer360Service = customer360Service;
    }

    /**
     * Customer 360° View — read-only unified detail screen.
     *
     * <p>Accessible by all CBS roles: MAKER, CHECKER, ADMIN, MANAGER, OPERATIONS, AUDITOR. Auditor
     * role restricts to read-only (no action buttons displayed in JSP).
     *
     * @param id customer primary key
     * @param page transaction page number (0-based, default 0)
     * @param size transaction page size (default 20)
     */
    @GetMapping("/{id}/360")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String viewCustomer360(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        Customer360DTO dto = customer360Service.buildCustomer360View(id, page, size);
        model.addAttribute("c360", dto);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("isAuditor", isCurrentUserAuditor());
        return "customer/customer-360";
    }

    /** Check if the current user has AUDITOR role (read-only, no action buttons). */
    private boolean isCurrentUserAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        boolean hasAuditor =
                auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_AUDITOR"));
        boolean hasElevated =
                auth.getAuthorities().stream()
                        .anyMatch(
                                a ->
                                        a.getAuthority().equals("ROLE_CHECKER")
                                                || a.getAuthority().equals("ROLE_ADMIN")
                                                || a.getAuthority().equals("ROLE_MANAGER"));
        return hasAuditor && !hasElevated;
    }
}
