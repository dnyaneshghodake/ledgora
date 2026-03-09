package com.ledgora.batch.controller;

import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.service.BatchService;
import com.ledgora.common.enums.BatchStatus;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for Batch Management UI.
 * Provides batch dashboard, manual close, and settle operations.
 */
@Controller
@RequestMapping("/batches")
public class BatchController {

    private final BatchService batchService;
    private final TenantService tenantService;

    public BatchController(BatchService batchService, TenantService tenantService) {
        this.batchService = batchService;
        this.tenantService = tenantService;
    }

    @GetMapping
    public String batchDashboard(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        List<TransactionBatch> openBatches = batchService.getBatchesByTenantAndStatus(tenantId, BatchStatus.OPEN);
        List<TransactionBatch> closedBatches = batchService.getBatchesByTenantAndStatus(tenantId, BatchStatus.CLOSED);
        List<TransactionBatch> settledBatches = batchService.getBatchesByTenantAndStatus(tenantId, BatchStatus.SETTLED);
        List<TransactionBatch> allBatches = batchService.getAllBatchesByTenant(tenantId);

        model.addAttribute("openBatches", openBatches);
        model.addAttribute("closedBatches", closedBatches);
        model.addAttribute("settledBatches", settledBatches);
        model.addAttribute("allBatches", allBatches);
        model.addAttribute("openCount", openBatches.size());
        model.addAttribute("closedCount", closedBatches.size());
        model.addAttribute("settledCount", settledBatches.size());

        return "batch/batch-dashboard";
    }

    /**
     * Close a single open batch manually.
     * Validates balance (debit == credit) before closing.
     */
    @PostMapping("/{id}/close")
    public String closeBatch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            TransactionBatch closed = batchService.closeBatch(id);
            redirectAttributes.addFlashAttribute("message",
                    "Batch " + closed.getBatchCode() + " closed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Close failed: " + e.getMessage());
        }
        return "redirect:/batches";
    }

    /**
     * Close all open batches for the current tenant's business date.
     */
    @PostMapping("/close-all")
    public String closeAllBatches(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Long tenantId = resolveTenantId(session);
            LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);
            batchService.closeAllBatches(tenantId, businessDate);
            redirectAttributes.addFlashAttribute("message",
                    "All open batches closed for business date " + businessDate);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Close all failed: " + e.getMessage());
        }
        return "redirect:/batches";
    }

    /**
     * Settle all closed batches for the current tenant's business date.
     * Validates debit == credit per batch before settling.
     */
    @PostMapping("/settle-all")
    public String settleAllBatches(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Long tenantId = resolveTenantId(session);
            LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);
            batchService.settleAllBatches(tenantId, businessDate);
            redirectAttributes.addFlashAttribute("message",
                    "All closed batches settled for business date " + businessDate);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Settlement failed: " + e.getMessage());
        }
        return "redirect:/batches";
    }

    /**
     * Resolve tenant ID from TenantContextHolder or session.
     */
    private Long resolveTenantId(HttpSession session) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            Object sessionTenantId = session.getAttribute("tenantId");
            if (sessionTenantId instanceof Number n) {
                tenantId = n.longValue();
            } else if (sessionTenantId instanceof String s && !s.isBlank()) {
                tenantId = Long.valueOf(s);
            }
        }
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set for batch operation");
        }
        return tenantId;
    }
}
