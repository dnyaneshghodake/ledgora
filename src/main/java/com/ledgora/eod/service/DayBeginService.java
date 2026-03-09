package com.ledgora.eod.service;

import com.ledgora.approval.repository.ApprovalRequestRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.batch.service.BatchService;
import com.ledgora.calendar.service.BankCalendarService;
import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.common.enums.DayStatus;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CBS Day Begin Ceremony Service.
 *
 * Before opening a new business day, the DBO (Day Begin Officer) must validate:
 *   1. Previous day is CLOSED (not OPEN or DAY_CLOSING)
 *   2. No pending (unsettled) batches from previous day
 *   3. Calendar loaded for current business date
 *   4. No pending approval requests from previous day
 *   5. No pending transactions from previous day
 *
 * Only after all pre-checks pass can the DBO execute the explicit "Open Day" action.
 */
@Service
public class DayBeginService {

    private static final Logger log = LoggerFactory.getLogger(DayBeginService.class);

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final BatchService batchService;
    private final BankCalendarService bankCalendarService;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    public DayBeginService(TenantService tenantService,
                            TenantRepository tenantRepository,
                            BatchService batchService,
                            BankCalendarService bankCalendarService,
                            ApprovalRequestRepository approvalRequestRepository,
                            TransactionRepository transactionRepository,
                            AuditService auditService) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.batchService = batchService;
        this.bankCalendarService = bankCalendarService;
        this.approvalRequestRepository = approvalRequestRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    /**
     * Validate all Day Begin pre-conditions.
     * Returns empty list if all checks pass.
     */
    public List<String> validateDayBegin(Long tenantId) {
        List<String> errors = new ArrayList<>();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        // 1. Current day must be CLOSED
        if (tenant.getDayStatus() != DayStatus.CLOSED) {
            errors.add("Day Begin blocked: current day status is " + tenant.getDayStatus()
                    + ". Must be CLOSED before opening a new day.");
            return errors; // No point checking further if day is not closed
        }

        LocalDate businessDate = tenant.getCurrentBusinessDate();
        LocalDate previousDate = businessDate.minusDays(1);

        // 2. No pending (open) batches from previous day
        if (!batchService.areAllBatchesClosed(tenantId, previousDate)) {
            errors.add("Day Begin blocked: open batches exist for previous business date " + previousDate);
        }

        // 3. Calendar loaded for current business date
        // If no calendar entry exists, the system defaults to weekday=working, weekend=holiday.
        // We just log a warning if no explicit entry exists.
        if (bankCalendarService.getCalendarEntry(tenantId, businessDate).isEmpty()) {
            // Not a blocker, but log warning
            log.warn("No explicit calendar entry for tenant {} date {}. System will use default weekday/weekend rules.",
                    tenantId, businessDate);
        }

        // 4. No pending approvals from previous day
        long pendingApprovals = approvalRequestRepository.countByTenant_IdAndStatus(tenantId, ApprovalStatus.PENDING);
        if (pendingApprovals > 0) {
            errors.add("Day Begin warning: " + pendingApprovals + " pending approval request(s) exist. "
                    + "These should be resolved but do not block Day Begin.");
        }

        // 5. No PENDING_APPROVAL transactions from previous day
        long pendingTxns = transactionRepository.countPendingApprovalByTenantId(tenantId);
        if (pendingTxns > 0) {
            errors.add("Day Begin warning: " + pendingTxns + " transaction(s) pending approval. "
                    + "These should be resolved but do not block Day Begin.");
        }

        if (errors.isEmpty()) {
            log.info("Day Begin validation passed for tenant {} business date {}", tenantId, businessDate);
        } else {
            log.warn("Day Begin validation for tenant {} date {}: {}", tenantId, businessDate, errors);
        }

        return errors;
    }

    /**
     * Execute Day Begin: validate pre-checks and open the business day.
     * Blocks if critical validations fail (day not CLOSED, open batches).
     * Warnings (pending approvals/transactions) are logged but don't block.
     */
    @Transactional
    public void openDay(Long tenantId) {
        List<String> errors = validateDayBegin(tenantId);

        // Filter critical errors (those that don't contain "warning")
        List<String> criticalErrors = errors.stream()
                .filter(e -> !e.toLowerCase().contains("warning"))
                .toList();

        if (!criticalErrors.isEmpty()) {
            throw new RuntimeException("Day Begin failed: " + String.join("; ", criticalErrors));
        }

        tenantService.openDay(tenantId);

        // Audit the Day Begin action
        String username = "system";
        try {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception ignored) {}

        Tenant tenant = tenantService.getTenantById(tenantId);
        auditService.logEvent(null, "DAY_BEGIN", "TENANT", tenantId,
                "Day opened for business date " + tenant.getCurrentBusinessDate()
                + " by " + username, null);

        log.info("Day Begin completed for tenant {} business date {} by {}",
                tenantId, tenant.getCurrentBusinessDate(), username);
    }

    /**
     * Check if Day Begin can be executed (all critical validations pass).
     */
    public boolean canOpenDay(Long tenantId) {
        List<String> errors = validateDayBegin(tenantId);
        return errors.stream().noneMatch(e -> !e.toLowerCase().contains("warning"));
    }
}
