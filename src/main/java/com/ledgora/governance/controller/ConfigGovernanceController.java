package com.ledgora.governance.controller;

import com.ledgora.governance.entity.ConfigChangeRequest;
import com.ledgora.governance.service.ConfigGovernanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for CBS Config Governance screens.
 *
 * <p>Routes:
 *
 * <ul>
 *   <li>GET /governance — pending config changes dashboard
 *   <li>GET /governance/{id} — view a specific change request
 *   <li>POST /governance/{id}/approve — approve a config change (checker)
 *   <li>POST /governance/{id}/reject — reject a config change (checker)
 * </ul>
 */
@Controller
@RequestMapping("/governance")
public class ConfigGovernanceController {

    private final ConfigGovernanceService governanceService;

    public ConfigGovernanceController(ConfigGovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    /** Config governance dashboard — shows all pending config change requests. */
    @GetMapping
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String dashboard(
            @RequestParam(value = "type", required = false) String configType, Model model) {
        if (configType != null && !configType.isBlank()) {
            model.addAttribute(
                    "pendingChanges", governanceService.getPendingChangesByType(configType));
            model.addAttribute("selectedType", configType);
        } else {
            model.addAttribute("pendingChanges", governanceService.getPendingChanges());
        }
        model.addAttribute("pendingCount", governanceService.countPending());
        return "governance/governance-dashboard";
    }

    /** View a specific config change request (before/after diff). */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String viewChange(@PathVariable Long id, Model model) {
        ConfigChangeRequest request =
                governanceService
                        .getById(id)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Config change request not found: " + id));
        model.addAttribute("changeRequest", request);
        return "governance/governance-view";
    }

    /** Approve a pending config change (checker step). */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String approve(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            governanceService.approve(id, remarks);
            redirectAttributes.addFlashAttribute("message", "Config change approved successfully");
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Approval conflict: this change was already actioned by another session. Please refresh.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/governance";
    }

    /** Reject a pending config change (checker step). */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String reject(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            governanceService.reject(id, remarks);
            redirectAttributes.addFlashAttribute("message", "Config change rejected");
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Rejection conflict: this change was already actioned by another session. Please refresh.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/governance";
    }
}
