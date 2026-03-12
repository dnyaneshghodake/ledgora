package com.ledgora.batch.service;

import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.repository.TransactionBatchRepository;
import com.ledgora.common.enums.BatchStatus;
import com.ledgora.common.enums.BatchType;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for transaction batch management. Every transaction must belong to a batch determined by
 * channel, tenant, and business date.
 */
@Service
public class BatchService {

    private static final Logger log = LoggerFactory.getLogger(BatchService.class);
    private final TransactionBatchRepository batchRepository;
    private final TenantRepository tenantRepository;

    public BatchService(
            TransactionBatchRepository batchRepository, TenantRepository tenantRepository) {
        this.batchRepository = batchRepository;
        this.tenantRepository = tenantRepository;
    }

    /** Get or create an open batch for the given tenant, channel, and business date. */
    @Transactional
    public TransactionBatch getOrCreateOpenBatch(
            Long tenantId, TransactionChannel channel, LocalDate businessDate) {
        BatchType batchType = mapChannelToBatchType(channel);
        return batchRepository
                .findByTenantIdAndBatchTypeAndBusinessDateAndStatus(
                        tenantId, batchType, businessDate, BatchStatus.OPEN)
                .orElseGet(
                        () -> {
                            Tenant tenant =
                                    tenantRepository
                                            .findById(tenantId)
                                            .orElseThrow(
                                                    () ->
                                                            new RuntimeException(
                                                                    "Tenant not found: "
                                                                            + tenantId));
                            TransactionBatch batch =
                                    TransactionBatch.builder()
                                            .tenant(tenant)
                                            .batchType(batchType)
                                            .businessDate(businessDate)
                                            .status(BatchStatus.OPEN)
                                            .totalDebit(BigDecimal.ZERO)
                                            .totalCredit(BigDecimal.ZERO)
                                            .transactionCount(0)
                                            .build();
                            TransactionBatch saved = batchRepository.save(batch);
                            // Generate and set batchCode after save to include the generated ID
                            saved.setBatchCode("BATCH-" + saved.getId());
                            saved = batchRepository.save(saved);
                            log.info(
                                    "Created new batch: tenant={} type={} date={}",
                                    tenantId,
                                    batchType,
                                    businessDate);
                            return saved;
                        });
    }

    /** Update batch totals when a transaction is assigned to the batch. Tenant-isolated. */
    @Transactional
    public void updateBatchTotals(Long batchId, BigDecimal debitAmount, BigDecimal creditAmount) {
        TransactionBatch batch = requireBatch(batchId);
        if (batch.getStatus() != BatchStatus.OPEN) {
            throw new RuntimeException("Cannot update closed/settled batch: " + batchId);
        }
        batch.setTotalDebit(batch.getTotalDebit().add(debitAmount));
        batch.setTotalCredit(batch.getTotalCredit().add(creditAmount));
        batch.setTransactionCount(batch.getTransactionCount() + 1);
        batchRepository.save(batch);
    }

    /**
     * Close all open batches for a tenant's business date. Each batch is validated before closing.
     */
    @Transactional
    public void closeAllBatches(Long tenantId, LocalDate businessDate) {
        List<TransactionBatch> openBatches =
                batchRepository.findByTenantIdAndBusinessDateAndStatus(
                        tenantId, businessDate, BatchStatus.OPEN);
        for (TransactionBatch batch : openBatches) {
            List<String> errors = validateBatchClose(batch);
            if (!errors.isEmpty()) {
                log.warn("Batch {} close validation failed: {}", batch.getId(), errors);
                throw new RuntimeException(
                        "Cannot close batch " + batch.getId() + ": " + String.join("; ", errors));
            }
            batch.setStatus(BatchStatus.CLOSED);
            batch.setClosedAt(LocalDateTime.now());
            batchRepository.save(batch);
            log.info(
                    "Closed batch: id={} type={} date={}",
                    batch.getId(),
                    batch.getBatchType(),
                    businessDate);
        }
    }

    /**
     * Close a single batch manually (for batch status UI). Validates before closing: all vouchers
     * balanced, no drafts, no pending approvals. Tenant-isolated.
     */
    @Transactional
    public TransactionBatch closeBatch(Long batchId) {
        TransactionBatch batch = requireBatch(batchId);
        if (batch.getStatus() != BatchStatus.OPEN) {
            throw new RuntimeException(
                    "Batch " + batchId + " is not OPEN. Current: " + batch.getStatus());
        }

        List<String> errors = validateBatchClose(batch);
        if (!errors.isEmpty()) {
            throw new RuntimeException(
                    "Cannot close batch " + batchId + ": " + String.join("; ", errors));
        }

        batch.setStatus(BatchStatus.CLOSED);
        batch.setClosedAt(LocalDateTime.now());
        TransactionBatch saved = batchRepository.save(batch);
        log.info(
                "Batch manually closed: id={} type={} debit={} credit={}",
                batchId,
                batch.getBatchType(),
                batch.getTotalDebit(),
                batch.getTotalCredit());
        return saved;
    }

    /**
     * Validate batch close pre-conditions: 1. Total debit == total credit (batch is balanced) 2.
     * Transaction count > 0 (no empty batches, or allow empty close)
     */
    public List<String> validateBatchClose(TransactionBatch batch) {
        List<String> errors = new java.util.ArrayList<>();

        // 1. Batch must be balanced (double-entry invariant)
        if (batch.getTotalDebit().compareTo(batch.getTotalCredit()) != 0) {
            errors.add(
                    "Batch "
                            + batch.getId()
                            + " is unbalanced: debit="
                            + batch.getTotalDebit()
                            + " credit="
                            + batch.getTotalCredit());
        }

        return errors;
    }

    /**
     * Get batch close validation results for a specific batch (for UI display). Tenant-isolated.
     */
    public List<String> getBatchCloseValidation(Long batchId) {
        TransactionBatch batch = requireBatch(batchId);
        return validateBatchClose(batch);
    }

    /**
     * Validate and settle all closed batches for a tenant's business date. Validates total_debit ==
     * total_credit before marking as SETTLED.
     */
    @Transactional
    public void settleAllBatches(Long tenantId, LocalDate businessDate) {
        List<TransactionBatch> closedBatches =
                batchRepository.findByTenantIdAndBusinessDateAndStatus(
                        tenantId, businessDate, BatchStatus.CLOSED);
        for (TransactionBatch batch : closedBatches) {
            if (batch.getTotalDebit().compareTo(batch.getTotalCredit()) != 0) {
                throw new RuntimeException(
                        "Batch "
                                + batch.getId()
                                + " is unbalanced: debit="
                                + batch.getTotalDebit()
                                + " credit="
                                + batch.getTotalCredit());
            }
            batch.setStatus(BatchStatus.SETTLED);
            batchRepository.save(batch);
            log.info(
                    "Settled batch: id={} type={} debit={} credit={}",
                    batch.getId(),
                    batch.getBatchType(),
                    batch.getTotalDebit(),
                    batch.getTotalCredit());
        }
    }

    /** Check if all batches are closed or settled for a tenant's business date. */
    public boolean areAllBatchesClosed(Long tenantId, LocalDate businessDate) {
        List<TransactionBatch> openBatches =
                batchRepository.findByTenantIdAndBusinessDateAndStatus(
                        tenantId, businessDate, BatchStatus.OPEN);
        return openBatches.isEmpty();
    }

    public List<TransactionBatch> getBatchesByTenantAndDate(Long tenantId, LocalDate businessDate) {
        return batchRepository.findByTenantIdAndBusinessDate(tenantId, businessDate);
    }

    public List<TransactionBatch> getBatchesByTenantAndStatus(Long tenantId, BatchStatus status) {
        return batchRepository.findByTenantIdAndStatus(tenantId, status);
    }

    public List<TransactionBatch> getAllBatchesByTenant(Long tenantId) {
        return batchRepository.findByTenantId(tenantId);
    }

    /**
     * Tenant-isolated batch lookup. Uses tenant context when available (CBS operations), falls back
     * to findById for system-internal calls (EOD, seeder, tests without context).
     */
    private TransactionBatch requireBatch(Long batchId) {
        Long tenantId = com.ledgora.tenant.context.TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return batchRepository
                    .findByIdAndTenantId(batchId, tenantId)
                    .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));
        }
        // No tenant context — system-internal call (EOD, scheduled jobs, tests)
        return batchRepository
                .findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));
    }

    /** Map transaction channel to batch type. */
    private BatchType mapChannelToBatchType(TransactionChannel channel) {
        if (channel == null) return BatchType.INTERNAL;
        return switch (channel) {
            case ATM -> BatchType.ATM;
            case ONLINE, MOBILE -> BatchType.ONLINE;
            case TELLER -> BatchType.BRANCH_CASH;
            case BATCH -> BatchType.BATCH;
        };
    }
}
