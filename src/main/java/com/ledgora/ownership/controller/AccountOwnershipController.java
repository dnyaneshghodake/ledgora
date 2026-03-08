package com.ledgora.ownership.controller;

import com.ledgora.common.enums.OwnershipType;
import com.ledgora.ownership.entity.AccountOwnership;
import com.ledgora.ownership.service.AccountOwnershipService;
import com.ledgora.tenant.context.TenantContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for Account Ownership management.
 */
@Controller
@RequestMapping("/ownership")
public class AccountOwnershipController {

    private final AccountOwnershipService ownershipService;

    public AccountOwnershipController(AccountOwnershipService ownershipService) {
        this.ownershipService = ownershipService;
    }

    @GetMapping
    public String listPending(Model model) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<AccountOwnership> pending = ownershipService.getPendingOwnerships(tenantId);
        model.addAttribute("pendingOwnerships", pending);
        model.addAttribute("ownershipTypes", OwnershipType.values());
        return "ownership/ownership-list";
    }

    @GetMapping("/account/{accountId}")
    public String listByAccount(@PathVariable Long accountId, Model model) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<AccountOwnership> ownerships = ownershipService.getOwnershipsByAccount(accountId, tenantId);
        model.addAttribute("ownerships", ownerships);
        model.addAttribute("accountId", accountId);
        return "ownership/ownership-account";
    }

    @GetMapping("/customer/{customerMasterId}")
    public String listByCustomer(@PathVariable Long customerMasterId, Model model) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<AccountOwnership> ownerships = ownershipService.getOwnershipsByCustomer(customerMasterId, tenantId);
        model.addAttribute("ownerships", ownerships);
        model.addAttribute("customerMasterId", customerMasterId);
        return "ownership/ownership-customer";
    }

    @PostMapping("/create")
    public String createOwnership(@RequestParam("accountId") Long accountId,
                                   @RequestParam("customerMasterId") Long customerMasterId,
                                   @RequestParam("ownershipType") String ownershipType,
                                   @RequestParam("ownershipPercentage") BigDecimal ownershipPercentage,
                                   @RequestParam(value = "isOperational", defaultValue = "true") boolean isOperational,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            ownershipService.createOwnership(tenantId, accountId, customerMasterId,
                    OwnershipType.valueOf(ownershipType), ownershipPercentage, isOperational);
            redirectAttributes.addFlashAttribute("success", "Ownership submitted for approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ownership";
    }

    @PostMapping("/approve/{id}")
    public String approveOwnership(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ownershipService.approveOwnership(id);
            redirectAttributes.addFlashAttribute("success", "Ownership approved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ownership";
    }

    @PostMapping("/reject/{id}")
    public String rejectOwnership(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ownershipService.rejectOwnership(id);
            redirectAttributes.addFlashAttribute("success", "Ownership rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ownership";
    }
}
