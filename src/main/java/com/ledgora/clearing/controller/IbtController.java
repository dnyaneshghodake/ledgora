package com.ledgora.clearing.controller;

import com.ledgora.clearing.entity.InterBranchTransfer;
import com.ledgora.clearing.repository.InterBranchTransferRepository;
import com.ledgora.clearing.service.InterBranchClearingService;
import com.ledgora.common.enums.InterBranchTransferStatus;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Inter-Branch Transfer (IBT) list, detail, and reconciliation controller. Routes: /ibt (list),
 * /ibt/{id} (detail), /ibt/reconciliation (reconciliation dashboard)
 */
@Controller
@RequestMapping("/ibt")
public class IbtController {

    private final InterBranchTransferRepository ibtRepository;
    private final InterBranchClearingService clearingService;
    private final TenantService tenantService;
    private final TransactionService transactionService;

    public IbtController(
            InterBranchTransferRepository ibtRepository,
            InterBranchClearingService clearingService,
            TenantService tenantService,
            TransactionService transactionService) {
        this.ibtRepository = ibtRepository;
        this.clearingService = clearingService;
        this.tenantService = tenantService;
        this.transactionService = transactionService;
    }

    /** Show IBT create form. */
    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN', 'MANAGER', 'TELLER')")
    public String createForm(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        model.addAttribute("businessDate", tenantService.getCurrentBusinessDate(tenantId));
        return "ibt/ibt-create";
    }

    /**
     * Execute IBT — delegates to TransactionService.transfer() which handles cross-branch detection
     * and routing through the IBC clearing flow automatically.
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN', 'MANAGER', 'TELLER')")
    public String createIbt(
            @org.springframework.web.bind.annotation.RequestParam String sourceAccountNumber,
            @org.springframework.web.bind.annotation.RequestParam String destinationAccountNumber,
            @org.springframework.web.bind.annotation.RequestParam java.math.BigDecimal amount,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
                    String narration,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Long tenantId = resolveTenantId(session);
            // IBT uses TransactionService.transfer() — cross-branch detection is automatic
            com.ledgora.transaction.dto.TransactionDTO dto =
                    com.ledgora.transaction.dto.TransactionDTO.builder()
                            .sourceAccountNumber(sourceAccountNumber)
                            .destinationAccountNumber(destinationAccountNumber)
                            .amount(amount)
                            .currency("INR")
                            .channel("TELLER")
                            .narration(narration)
                            .description(narration != null ? narration : "Inter-Branch Transfer")
                            .build();
            // Find the IBT record created by the transfer
            com.ledgora.transaction.entity.Transaction txn = transactionService.transfer(dto);
            // Redirect to the IBT record linked to this transaction
            java.util.Optional<InterBranchTransfer> ibt =
                    ibtRepository.findByReferenceTransactionIdAndTenantId(txn.getId(), tenantId);
            if (ibt.isPresent()) {
                redirectAttributes.addFlashAttribute(
                        "message",
                        "IBT created successfully. Transfer ref: " + txn.getTransactionRef());
                return "redirect:/ibt/" + ibt.get().getId();
            }
            redirectAttributes.addFlashAttribute(
                    "message", "Transfer submitted. Ref: " + txn.getTransactionRef());
            return "redirect:/ibt";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ibt/create";
        }
    }

    /** IBT list — paginated, filterable by status. */
    @GetMapping
    @PreAuthorize(
            "hasAnyRole('MAKER', 'CHECKER', 'ADMIN', 'MANAGER', 'TELLER', 'OPERATIONS', 'AUDITOR')")
    public String list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model,
            HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Pageable pageable = PageRequest.of(page, size);
        Page<InterBranchTransfer> ibtPage;

        if (status != null && !status.isBlank()) {
            InterBranchTransferStatus statusEnum = InterBranchTransferStatus.valueOf(status);
            ibtPage = ibtRepository.findByTenantIdAndStatus(tenantId, statusEnum, pageable);
            model.addAttribute("selectedStatus", status);
        } else {
            ibtPage = ibtRepository.findByTenantId(tenantId, pageable);
        }

        model.addAttribute("ibts", ibtPage.getContent());
        model.addAttribute("currentPage", ibtPage.getNumber());
        model.addAttribute("totalPages", ibtPage.getTotalPages());
        model.addAttribute("totalElements", ibtPage.getTotalElements());
        model.addAttribute("statuses", InterBranchTransferStatus.values());
        model.addAttribute("businessDate", tenantService.getCurrentBusinessDate(tenantId));
        return "ibt/ibt-list";
    }

    /** IBT detail view. */
    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('MAKER', 'CHECKER', 'ADMIN', 'MANAGER', 'TELLER', 'OPERATIONS', 'AUDITOR')")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        InterBranchTransfer ibt =
                ibtRepository
                        .findByIdWithGraph(id)
                        .filter(
                                t ->
                                        t.getTenant() != null
                                                && tenantId.equals(t.getTenant().getId()))
                        .orElseThrow(() -> new RuntimeException("IBT not found: " + id));
        model.addAttribute("ibt", ibt);
        return "ibt/ibt-detail";
    }

    /** IBT reconciliation dashboard — shows unsettled and failed transfers. */
    @GetMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'ADMIN', 'MANAGER', 'AUDITOR')")
    public String reconciliation(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);

        List<InterBranchTransfer> unsettled = ibtRepository.findUnsettledByTenantId(tenantId);
        List<InterBranchTransfer> pendingReceiving =
                ibtRepository.findByTenantIdAndBusinessDateAndStatus(
                        tenantId, businessDate, InterBranchTransferStatus.SENT);

        long initiatedCount =
                ibtRepository.countByTenantIdAndStatusIn(
                        tenantId, java.util.Set.of(InterBranchTransferStatus.INITIATED));
        long sentCount =
                ibtRepository.countByTenantIdAndStatusIn(
                        tenantId, java.util.Set.of(InterBranchTransferStatus.SENT));
        long receivedCount =
                ibtRepository.countByTenantIdAndStatusIn(
                        tenantId, java.util.Set.of(InterBranchTransferStatus.RECEIVED));
        long failedCount =
                ibtRepository.countByTenantIdAndStatusIn(
                        tenantId, java.util.Set.of(InterBranchTransferStatus.FAILED));

        String clearingError = clearingService.validateClearingBalance(tenantId, businessDate);

        model.addAttribute("unsettledTransfers", unsettled);
        model.addAttribute("pendingReceiving", pendingReceiving);
        model.addAttribute("initiatedCount", initiatedCount);
        model.addAttribute("sentCount", sentCount);
        model.addAttribute("receivedCount", receivedCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("clearingError", clearingError);
        model.addAttribute("clearingOk", clearingError == null);
        model.addAttribute("businessDate", businessDate);
        return "ibt/ibt-reconciliation";
    }

    /** Mark a SENT transfer as FAILED manually (operations action). */
    @PostMapping("/{id}/fail")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'ADMIN', 'MANAGER')")
    public String markFailed(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Manual failure by operations")
                    String reason,
            RedirectAttributes redirectAttributes) {
        try {
            clearingService.markFailed(id, reason);
            redirectAttributes.addFlashAttribute("message", "IBT " + id + " marked as FAILED.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ibt/reconciliation";
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
