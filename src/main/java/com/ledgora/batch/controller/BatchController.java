package com.ledgora.batch.controller;

import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.service.BatchService;
import com.ledgora.common.enums.BatchStatus;
import com.ledgora.tenant.context.TenantContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller for the Batch Dashboard UI.
 */
@Controller
@RequestMapping("/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping
    public String batchDashboard(Model model) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = 1L; // default tenant
        }

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
}
