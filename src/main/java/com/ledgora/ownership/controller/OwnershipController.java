package com.ledgora.ownership.controller;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.OwnershipType;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.repository.CustomerMasterRepository;
import com.ledgora.ownership.entity.AccountOwnership;
import com.ledgora.ownership.service.AccountOwnershipService;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Account Ownership management controller. Routes: /ownership (list),
 * /ownership/account/{accountId}, /ownership/customer/{customerId}
 */
@Controller
@RequestMapping("/ownership")
public class OwnershipController {

    private final AccountOwnershipService ownershipService;
    private final AccountRepository accountRepository;
    private final CustomerMasterRepository customerMasterRepository;

    public OwnershipController(
            AccountOwnershipService ownershipService,
            AccountRepository accountRepository,
            CustomerMasterRepository customerMasterRepository) {
        this.ownershipService = ownershipService;
        this.accountRepository = accountRepository;
        this.customerMasterRepository = customerMasterRepository;
    }

    /** Pending ownerships for current tenant. */
    @GetMapping
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER', 'AUDITOR')")
    public String listPendingOwnerships(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        List<AccountOwnership> pendingOwnerships = ownershipService.getPendingOwnerships(tenantId);
        model.addAttribute("ownerships", pendingOwnerships);
        model.addAttribute("pendingCount", pendingOwnerships.size());
        return "ownership/ownership-list";
    }

    /** Ownerships for a specific account. */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'ADMIN', 'MANAGER', 'TELLER', 'AUDITOR')")
    public String ownershipsByAccount(
            @PathVariable Long accountId, Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Account account =
                accountRepository
                        .findByIdAndTenantId(accountId, tenantId)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        List<AccountOwnership> ownerships =
                ownershipService.getOwnershipsByAccount(accountId, tenantId);
        List<CustomerMaster> customers =
                customerMasterRepository.findAll().stream()
                        .filter(c -> tenantId.equals(c.getTenant().getId()))
                        .toList();
        model.addAttribute("account", account);
        model.addAttribute("ownerships", ownerships);
        model.addAttribute("customers", customers);
        model.addAttribute("ownershipTypes", OwnershipType.values());
        return "ownership/ownership-account";
    }

    /** Ownerships for a specific customer. */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'ADMIN', 'MANAGER', 'TELLER', 'AUDITOR')")
    public String ownershipsByCustomer(
            @PathVariable Long customerId, Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        CustomerMaster customer =
                customerMasterRepository
                        .findById(customerId)
                        .filter(c -> tenantId.equals(c.getTenant().getId()))
                        .orElseThrow(
                                () -> new RuntimeException("Customer not found: " + customerId));
        List<AccountOwnership> ownerships =
                ownershipService.getOwnershipsByCustomer(customerId, tenantId);
        model.addAttribute("customer", customer);
        model.addAttribute("ownerships", ownerships);
        return "ownership/ownership-customer";
    }

    /** Create ownership link (maker step). */
    @PostMapping("/account/{accountId}/create")
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN', 'MANAGER')")
    public String createOwnership(
            @PathVariable Long accountId,
            @RequestParam Long customerMasterId,
            @RequestParam String ownershipType,
            @RequestParam BigDecimal ownershipPercentage,
            @RequestParam(defaultValue = "false") boolean isOperational,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Long tenantId = resolveTenantId(session);
            ownershipService.createOwnership(
                    tenantId,
                    accountId,
                    customerMasterId,
                    OwnershipType.valueOf(ownershipType),
                    ownershipPercentage,
                    isOperational);
            redirectAttributes.addFlashAttribute(
                    "message", "Ownership link submitted for approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ownership/account/" + accountId;
    }

    /** Approve ownership (checker step). */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String approveOwnership(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            AccountOwnership o = ownershipService.approveOwnership(id);
            redirectAttributes.addFlashAttribute("message", "Ownership approved.");
            return "redirect:/ownership/account/" + o.getAccount().getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ownership";
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
