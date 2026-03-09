package com.ledgora.approval.controller;

import com.ledgora.approval.entity.ApprovalRequest;
import com.ledgora.approval.service.ApprovalService;
import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.customer.service.CustomerService;
import com.ledgora.transaction.service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Unified approval controller for CBS maker-checker workflow.
 * Handles approval/rejection of all entity types and delegates to the appropriate service:
 *   - TRANSACTION → TransactionService.approveTransaction() / rejectTransaction()
 *   - CUSTOMER → CustomerService.approveCustomer() / rejectCustomer()
 *   - ACCOUNT, LIEN, CALENDAR → handled by their own services (already wired)
 */
@Controller
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final TransactionService transactionService;
    private final CustomerService customerService;

    public ApprovalController(ApprovalService approvalService,
                              TransactionService transactionService,
                              CustomerService customerService) {
        this.approvalService = approvalService;
        this.transactionService = transactionService;
        this.customerService = customerService;
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
     * @Transactional ensures atomicity: if entity-specific approval fails,
     * the ApprovalRequest status change is also rolled back.
     */
    @PostMapping("/{id}/approve")
    @Transactional
    public String approve(@PathVariable Long id, @RequestParam(required = false) String remarks,
                          RedirectAttributes redirectAttributes) {
        try {
            ApprovalRequest request = approvalService.approve(id, remarks);
            // Delegate to appropriate service based on entity type
            if (request.getEntityId() != null) {
                switch (request.getEntityType()) {
                    case "TRANSACTION" -> transactionService.approveTransaction(request.getEntityId(), remarks);
                    case "CUSTOMER" -> customerService.approveCustomer(request.getEntityId());
                }
            }
            redirectAttributes.addFlashAttribute("message", "Request approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/approvals";
    }

    /**
     * Reject a pending request (checker step).
     * @Transactional ensures atomicity: if entity-specific rejection fails,
     * the ApprovalRequest status change is also rolled back.
     */
    @PostMapping("/{id}/reject")
    @Transactional
    public String reject(@PathVariable Long id, @RequestParam(required = false) String remarks,
                         RedirectAttributes redirectAttributes) {
        try {
            ApprovalRequest request = approvalService.reject(id, remarks);
            // Delegate to appropriate service based on entity type
            if (request.getEntityId() != null) {
                switch (request.getEntityType()) {
                    case "TRANSACTION" -> transactionService.rejectTransaction(request.getEntityId(), remarks);
                    case "CUSTOMER" -> customerService.rejectCustomer(request.getEntityId());
                }
            }
            redirectAttributes.addFlashAttribute("message", "Request rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/approvals";
    }
}
