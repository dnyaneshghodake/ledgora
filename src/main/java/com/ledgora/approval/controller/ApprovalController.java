package com.ledgora.approval.controller;

import com.ledgora.approval.entity.ApprovalRequest;
import com.ledgora.approval.service.ApprovalService;
import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.customer.service.CustomerService;
import com.ledgora.transaction.service.TransactionService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Unified approval controller for CBS maker-checker workflow. Handles approval/rejection of all
 * entity types and delegates to the appropriate service: - TRANSACTION →
 * TransactionService.approveTransaction() / rejectTransaction() - CUSTOMER →
 * CustomerService.approveCustomer() / rejectCustomer() - ACCOUNT, LIEN, CALENDAR → handled by their
 * own services (already wired)
 */
@Controller
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final TransactionService transactionService;
    private final CustomerService customerService;

    public ApprovalController(
            ApprovalService approvalService,
            TransactionService transactionService,
            CustomerService customerService) {
        this.approvalService = approvalService;
        this.transactionService = transactionService;
        this.customerService = customerService;
    }

    @GetMapping
    public String listApprovals(Model model) {
        // CBS Standard: Approval queue shows ONLY pending records.
        // Approved/rejected records are visible via audit trail, not the work queue.
        List<ApprovalRequest> pendingRequests = approvalService.getPendingRequests();
        model.addAttribute("approvals", pendingRequests);
        model.addAttribute("pendingCount", pendingRequests.size());
        model.addAttribute(
                "pendingCustomers",
                approvalService.getByEntityType("CUSTOMER", ApprovalStatus.PENDING));
        model.addAttribute(
                "pendingAccounts",
                approvalService.getByEntityType("ACCOUNT", ApprovalStatus.PENDING));
        model.addAttribute(
                "pendingLiens", approvalService.getByEntityType("LIEN", ApprovalStatus.PENDING));
        model.addAttribute(
                "pendingCalendar",
                approvalService.getByEntityType("CALENDAR", ApprovalStatus.PENDING));
        model.addAttribute(
                "pendingTransactions",
                approvalService.getByEntityType("TRANSACTION", ApprovalStatus.PENDING));
        return "approval/approvals";
    }

    @GetMapping("/pending")
    public String pendingApprovals(Model model) {
        return listApprovals(model);
    }

    @GetMapping("/{id}")
    public String viewApproval(@PathVariable Long id, Model model) {
        ApprovalRequest request =
                approvalService
                        .getById(id)
                        .orElseThrow(() -> new RuntimeException("Approval request not found"));
        model.addAttribute("approval", request);
        return "approval/approval-view";
    }

    /**
     * Approve a pending request (checker step). Each service call has its own @Transactional
     * boundary. If entity-specific approval fails, the error is reported to the user and the
     * ApprovalRequest remains in its updated state (service-level rollback handles its own scope).
     */
    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            ApprovalRequest request = approvalService.approve(id, remarks);
            // Delegate to appropriate service based on entity type
            if (request.getEntityId() != null) {
                switch (request.getEntityType()) {
                    case "TRANSACTION" ->
                            transactionService.approveTransaction(request.getEntityId(), remarks);
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
     * Reject a pending request (checker step). Each service call has its own @Transactional
     * boundary.
     */
    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            ApprovalRequest request = approvalService.reject(id, remarks);
            // Delegate to appropriate service based on entity type
            if (request.getEntityId() != null) {
                switch (request.getEntityType()) {
                    case "TRANSACTION" ->
                            transactionService.rejectTransaction(request.getEntityId(), remarks);
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
