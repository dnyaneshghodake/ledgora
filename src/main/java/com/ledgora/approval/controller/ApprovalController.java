package com.ledgora.approval.controller;

import com.ledgora.approval.entity.ApprovalRequest;
import com.ledgora.approval.service.ApprovalService;
import com.ledgora.common.enums.ApprovalStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * PART 3: Approval controller for maker-checker workflow.
 */
@Controller
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    public String listApprovals(@RequestParam(value = "status", required = false) String status, Model model) {
        if (status != null && !status.isEmpty()) {
            model.addAttribute("approvals", approvalService.getByEntityType("TRANSACTION", ApprovalStatus.valueOf(status)));
        } else {
            model.addAttribute("approvals", approvalService.getAllRequests());
        }
        // Populate categorized pending data for unified approval queue tabs
        model.addAttribute("pendingCount", approvalService.getPendingRequests().size());
        model.addAttribute("pendingCustomers", approvalService.getByEntityType("CUSTOMER", ApprovalStatus.PENDING));
        model.addAttribute("pendingAccounts", approvalService.getByEntityType("ACCOUNT", ApprovalStatus.PENDING));
        model.addAttribute("pendingLiens", approvalService.getByEntityType("LIEN", ApprovalStatus.PENDING));
        model.addAttribute("pendingCalendar", approvalService.getByEntityType("CALENDAR", ApprovalStatus.PENDING));
        model.addAttribute("pendingTransactions", approvalService.getByEntityType("TRANSACTION", ApprovalStatus.PENDING));
        return "approval/approvals";
    }

    @GetMapping("/pending")
    public String pendingApprovals(Model model) {
        model.addAttribute("approvals", approvalService.getPendingRequests());
        return "approval/approvals";
    }

    @GetMapping("/{id}")
    public String viewApproval(@PathVariable Long id, Model model) {
        ApprovalRequest request = approvalService.getById(id)
                .orElseThrow(() -> new RuntimeException("Approval request not found"));
        model.addAttribute("approval", request);
        return "approval/approval-view";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String remarks,
                          RedirectAttributes redirectAttributes) {
        try {
            approvalService.approve(id, remarks);
            redirectAttributes.addFlashAttribute("message", "Request approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/approvals";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String remarks,
                         RedirectAttributes redirectAttributes) {
        try {
            approvalService.reject(id, remarks);
            redirectAttributes.addFlashAttribute("message", "Request rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/approvals";
    }
}
