package com.ledgora.eod.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.batch.service.BatchService;
import com.ledgora.eod.entity.EodProcess;
import com.ledgora.eod.entity.EodProcess.EodPhase;
import com.ledgora.eod.repository.EodProcessRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.tenant.service.TenantService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crash-safe EOD State Machine — each phase commits independently.
 *
 * <p>RBI IT Framework — Business Continuity / Operational Resilience:
 *
 * <ul>
 *   <li>Phase progression: VALIDATED → DAY_CLOSING → BATCH_CLOSED → SETTLED → DATE_ADVANCED
 *   <li>Each phase runs in its own transaction (REQUIRES_NEW)
 *   <li>On crash/restart: detect incomplete EOD and resume from last successful phase
 *   <li>Double execution prevented via unique constraint (tenant_id, business_date)
 *   <li>Stuck detection: RUNNING status with last_updated > 30 minutes ago
 * </ul>
 */
@Service
public class EodStateMachineService {

    private static final Logger log = LoggerFactory.getLogger(EodStateMachineService.class);

    /** Stuck threshold: 30 minutes in same phase = stuck. */
    private static final int STUCK_THRESHOLD_MINUTES = 30;

    private final EodProcessRepository eodProcessRepository;
    private final EodValidationService eodValidationService;
    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final BatchService batchService;
    private final AuditService auditService;
    private final org.springframework.context.ApplicationContext applicationContext;

    public EodStateMachineService(
            EodProcessRepository eodProcessRepository,
            EodValidationService eodValidationService,
            TenantService tenantService,
            TenantRepository tenantRepository,
            BatchService batchService,
            AuditService auditService,
            org.springframework.context.ApplicationContext applicationContext) {
        this.eodProcessRepository = eodProcessRepository;
        this.eodValidationService = eodValidationService;
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.batchService = batchService;
        this.auditService = auditService;
        this.applicationContext = applicationContext;
    }

    /**
     * Get the proxied instance of this service to ensure @Transactional(REQUIRES_NEW) is honored on
     * internal method calls. Spring proxy-based AOP only intercepts calls through the proxy.
     */
    private EodStateMachineService self() {
        return applicationContext.getBean(EodStateMachineService.class);
    }

    /**
     * Execute or resume EOD for a tenant. If an incomplete process exists for the current business
     * date, it resumes from the last successful phase. If no process exists, starts fresh.
     *
     * <p>Each phase commits independently — crash between phases is safe.
     */
    public void executeEod(Long tenantId) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        LocalDate businessDate = tenant.getCurrentBusinessDate();

        // Check for existing process (resume or prevent double execution)
        EodProcess process =
                eodProcessRepository
                        .findByTenantIdAndBusinessDate(tenantId, businessDate)
                        .orElse(null);

        if (process != null) {
            if ("COMPLETED".equals(process.getStatus())) {
                throw new RuntimeException(
                        "EOD already completed for tenant "
                                + tenantId
                                + " on "
                                + businessDate
                                + ". Double execution blocked.");
            }
            if ("FAILED".equals(process.getStatus())) {
                // Reset to resume from the failed phase
                log.info(
                        "Resuming failed EOD for tenant {} date {} from phase {}",
                        tenantId,
                        businessDate,
                        process.getPhase());
                process.setStatus("RUNNING");
                process.setFailureReason(null);
                eodProcessRepository.save(process);
            }
            // RUNNING status — resume from current phase
            log.info(
                    "Resuming EOD for tenant {} date {} from phase {}",
                    tenantId,
                    businessDate,
                    process.getPhase());
        } else {
            // Start fresh
            process = self().createNewProcess(tenant, businessDate);
        }

        // Execute phases from current position
        try {
            executeFromPhase(process, tenantId, businessDate);
        } catch (Exception e) {
            self().markFailed(process, e.getMessage());
            throw e;
        }
    }

    /**
     * Detect and list all incomplete EOD processes across all tenants. Called on application
     * startup for recovery.
     */
    public List<EodProcess> findIncompleteProcesses() {
        return eodProcessRepository.findByStatus("RUNNING");
    }

    /** Detect EOD processes stuck in the same phase for longer than the threshold. */
    public List<EodProcess> findStuckProcesses() {
        LocalDateTime stuckThreshold = LocalDateTime.now().minusMinutes(STUCK_THRESHOLD_MINUTES);
        return eodProcessRepository.findStuckProcesses(stuckThreshold);
    }

    // ── Phase execution with independent commits ──

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EodProcess createNewProcess(Tenant tenant, LocalDate businessDate) {
        EodProcess process =
                EodProcess.builder()
                        .tenant(tenant)
                        .businessDate(businessDate)
                        .phase(EodPhase.VALIDATED)
                        .status("RUNNING")
                        .build();
        process = eodProcessRepository.save(process);

        auditService.logEvent(
                null,
                "EOD_STARTED",
                "EOD_PROCESS",
                process.getId(),
                "EOD started for tenant " + tenant.getId() + " business date " + businessDate,
                null);

        log.info(
                "EOD process created: id={} tenant={} date={}",
                process.getId(),
                tenant.getId(),
                businessDate);
        return process;
    }

    private void executeFromPhase(EodProcess process, Long tenantId, LocalDate businessDate) {
        EodPhase currentPhase = process.getPhase();
        EodStateMachineService proxy = self();

        // Phase 1: VALIDATED — run pre-checks
        if (currentPhase.ordinal() <= EodPhase.VALIDATED.ordinal()) {
            proxy.runPhaseValidated(process, tenantId, businessDate);
        }

        // Phase 2: DAY_CLOSING — lock the business day
        if (currentPhase.ordinal() <= EodPhase.DAY_CLOSING.ordinal()) {
            proxy.runPhaseDayClosing(process, tenantId, businessDate);
        }

        // Phase 3: BATCH_CLOSED — close all batches
        if (currentPhase.ordinal() <= EodPhase.BATCH_CLOSED.ordinal()) {
            proxy.runPhaseBatchClosed(process, tenantId, businessDate);
        }

        // Phase 4: SETTLED — settle all batches
        if (currentPhase.ordinal() <= EodPhase.SETTLED.ordinal()) {
            proxy.runPhaseSettled(process, tenantId, businessDate);
        }

        // Phase 5: DATE_ADVANCED — advance business date
        if (currentPhase.ordinal() <= EodPhase.DATE_ADVANCED.ordinal()) {
            proxy.runPhaseDateAdvanced(process, tenantId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runPhaseValidated(EodProcess process, Long tenantId, LocalDate businessDate) {
        log.info("EOD Phase VALIDATED: tenant={} date={}", tenantId, businessDate);

        List<String> errors = eodValidationService.validateEod(tenantId, businessDate);
        if (!errors.isEmpty()) {
            throw new RuntimeException("EOD validation failed: " + String.join("; ", errors));
        }

        updatePhase(process.getId(), EodPhase.DAY_CLOSING);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runPhaseDayClosing(EodProcess process, Long tenantId, LocalDate businessDate) {
        log.info("EOD Phase DAY_CLOSING: tenant={} date={}", tenantId, businessDate);

        tenantService.startDayClosing(tenantId);

        // Re-validate after locking to close TOCTOU gap
        List<String> postLockErrors = eodValidationService.validateEod(tenantId, businessDate);
        if (!postLockErrors.isEmpty()) {
            throw new RuntimeException(
                    "EOD validation failed after day-closing lock: "
                            + String.join("; ", postLockErrors));
        }

        updatePhase(process.getId(), EodPhase.BATCH_CLOSED);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runPhaseBatchClosed(EodProcess process, Long tenantId, LocalDate businessDate) {
        log.info("EOD Phase BATCH_CLOSED: tenant={} date={}", tenantId, businessDate);

        batchService.closeAllBatches(tenantId, businessDate);

        updatePhase(process.getId(), EodPhase.SETTLED);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runPhaseSettled(EodProcess process, Long tenantId, LocalDate businessDate) {
        log.info("EOD Phase SETTLED: tenant={} date={}", tenantId, businessDate);

        batchService.settleAllBatches(tenantId, businessDate);

        updatePhase(process.getId(), EodPhase.DATE_ADVANCED);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runPhaseDateAdvanced(EodProcess process, Long tenantId) {
        log.info("EOD Phase DATE_ADVANCED: tenant={}", tenantId);

        tenantService.closeDayAndAdvance(tenantId);

        // Mark completed
        process =
                eodProcessRepository
                        .findById(process.getId())
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "EodProcess not found: " + process.getId()));
        process.setPhase(EodPhase.DATE_ADVANCED);
        process.setStatus("COMPLETED");
        process.setCompletedAt(LocalDateTime.now());
        eodProcessRepository.save(process);

        auditService.logEvent(
                null,
                "EOD_COMPLETED",
                "EOD_PROCESS",
                process.getId(),
                "EOD completed for tenant "
                        + tenantId
                        + " business date "
                        + process.getBusinessDate(),
                null);

        log.info(
                "EOD COMPLETED: id={} tenant={} date={}",
                process.getId(),
                tenantId,
                process.getBusinessDate());
    }

    /**
     * Update the phase of an EodProcess within the caller's transaction. This is called from within
     * REQUIRES_NEW phase methods, so it participates in their transaction — no proxy needed.
     */
    private void updatePhase(Long processId, EodPhase nextPhase) {
        EodProcess process =
                eodProcessRepository
                        .findById(processId)
                        .orElseThrow(
                                () -> new RuntimeException("EodProcess not found: " + processId));
        process.setPhase(nextPhase);
        eodProcessRepository.save(process);
        log.info("EOD phase advanced to {} for process {}", nextPhase, processId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(EodProcess process, String reason) {
        process =
                eodProcessRepository
                        .findById(process.getId())
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "EodProcess not found: " + process.getId()));
        process.setStatus("FAILED");
        process.setFailureReason(
                reason != null && reason.length() > 2000 ? reason.substring(0, 2000) : reason);
        eodProcessRepository.save(process);

        auditService.logEvent(
                null,
                "EOD_FAILED",
                "EOD_PROCESS",
                process.getId(),
                "EOD failed at phase "
                        + process.getPhase()
                        + ": "
                        + (reason != null && reason.length() > 500
                                ? reason.substring(0, 500)
                                : reason),
                null);

        log.error(
                "EOD FAILED: id={} phase={} reason={}",
                process.getId(),
                process.getPhase(),
                reason);
    }
}
