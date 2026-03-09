package com.ledgora.approval.controller;

import com.ledgora.approval.entity.ApprovalRequest;
import com.ledgora.approval.service.ApprovalService;
import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.transaction.service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Approval controller for unified maker-checker workflow.
 * Handles approval/rejection of all entity types (CUSTOMER, ACCOUNT, LIEN, CALENDAR, TRANSACTION).
 * For TRANSACTION approvals, delegates to TransactionService to trigger ledger posting.
 */
@Controller
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final TransactionService transactionService;

    public ApprovalController(ApprovalService approvalService,
                              TransactionService transactionService) {
        this.approvalService = approvalService;
        this.transactionService = transactionService;
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

    /**
     * Approve a pending request (checker step).
     * For TRANSACTION entity type: delegates to TransactionService.approveTransaction()
     * which re-validates conditions and posts vouchers/ledger/balances.
     */
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String remarks,
                          RedirectAttributes redirectAttributes) {
        try {
            ApprovalRequest request = approvalService.approve(id, remarks);
            // If this is a TRANSACTION approval, trigger the actual posting
            if ("TRANSACTION".equals(request.getEntityType()) && request.getEntityId() != null) {
                transactionService.approveTransaction(request.getEntityId(), remarks);
            }
            redirectAttributes.addFlashAttribute("message", "Request approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/approvals";
    }

    /**
     * Reject a pending request (checker step).
     * For TRANSACTION entity type: delegates to TransactionService.rejectTransaction()
     * which marks the transaction as REJECTED with no posting.
     */
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String remarks,
                         RedirectAttributes redirectAttributes) {
        try {
            ApprovalRequest request = approvalService.reject(id, remarks);
            // If this is a TRANSACTION rejection, mark the transaction as REJECTED
            if ("TRANSACTION".equals(request.getEntityType()) && request.getEntityId() != null) {
                transactionService.rejectTransaction(request.getEntityId(), remarks);
            }
            redirectAttributes.addFlashAttribute("message", "Request rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/approvals";
    }
}
