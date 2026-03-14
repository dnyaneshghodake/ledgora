package com.ledgora.reporting.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.reporting.dto.AlmReport;
import com.ledgora.reporting.dto.CrarReport;
import com.ledgora.reporting.dto.TrialBalanceReport;
import com.ledgora.reporting.entity.RegulatorySnapshot;
import com.ledgora.reporting.enums.SnapshotStatus;
import com.ledgora.reporting.repository.RegulatorySnapshotRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI Supervisory Reporting — Regulatory Snapshot Orchestrator.
 *
 * <p>Generates and persists immutable snapshots for:
 *
 * <ol>
 *   <li>Trial Balance (RBI Schedule 5/14 validation)
 *   <li>CRAR (Basel III Capital Adequacy)
 *   <li>ALM (Structural Liquidity Statement)
 * </ol>
 *
 * <p>Each snapshot is serialized to JSON, hashed with SHA-256, and stored as FINAL. Idempotent —
 * skips if FINAL snapshot already exists for the date. All generations logged to AuditLog.
 *
 * <p>Called during EOD after financial statement snapshot generation.
 */
@Service
public class RegulatorySnapshotService {

    private static final Logger log = LoggerFactory.getLogger(RegulatorySnapshotService.class);

    public static final String TYPE_TRIAL_BALANCE = "TRIAL_BALANCE";
    public static final String TYPE_CRAR = "CRAR";
    public static final String TYPE_ALM = "ALM";

    private final TrialBalanceEngine trialBalanceEngine;
    private final CrarEngine crarEngine;
    private final AlmEngine almEngine;
    private final RegulatorySnapshotRepository snapshotRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public RegulatorySnapshotService(
            TrialBalanceEngine trialBalanceEngine,
            CrarEngine crarEngine,
            AlmEngine almEngine,
            RegulatorySnapshotRepository snapshotRepository,
            TenantRepository tenantRepository,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.trialBalanceEngine = trialBalanceEngine;
        this.crarEngine = crarEngine;
        this.almEngine = almEngine;
        this.snapshotRepository = snapshotRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate all regulatory snapshots for a tenant. Called during EOD after financial statements.
     *
     * <p>Order: Trial Balance → CRAR → ALM (CRAR depends on TB being valid).
     */
    @Transactional
    public void generateAllSnapshots(Long tenantId) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () -> new RuntimeException("Tenant not found: " + tenantId));
        LocalDate businessDate = tenant.getCurrentBusinessDate().minusDays(1);

        generateTrialBalanceSnapshot(tenant, businessDate);
        generateCrarSnapshot(tenant, businessDate);
        generateAlmSnapshot(tenant, businessDate);

        log.info(
                "All regulatory snapshots generated for tenant {} date {}",
                tenantId,
                businessDate);
    }

    @Transactional
    public RegulatorySnapshot generateTrialBalanceSnapshot(
            Tenant tenant, LocalDate businessDate) {
        if (alreadyFinal(tenant.getId(), businessDate, TYPE_TRIAL_BALANCE)) {
            return null;
        }
        TrialBalanceReport report =
                trialBalanceEngine.generate(tenant.getId(), businessDate);
        return persist(tenant, businessDate, TYPE_TRIAL_BALANCE, report);
    }

    @Transactional
    public RegulatorySnapshot generateCrarSnapshot(
            Tenant tenant, LocalDate businessDate) {
        if (alreadyFinal(tenant.getId(), businessDate, TYPE_CRAR)) {
            return null;
        }
        CrarReport report = crarEngine.compute(tenant.getId(), businessDate);
        return persist(tenant, businessDate, TYPE_CRAR, report);
    }

    @Transactional
    public RegulatorySnapshot generateAlmSnapshot(
            Tenant tenant, LocalDate businessDate) {
        if (alreadyFinal(tenant.getId(), businessDate, TYPE_ALM)) {
            return null;
        }
        AlmReport report = almEngine.generate(tenant.getId(), businessDate);
        return persist(tenant, businessDate, TYPE_ALM, report);
    }

    private boolean alreadyFinal(Long tenantId, LocalDate date, String type) {
        boolean exists =
                snapshotRepository.existsByTenantIdAndBusinessDateAndReportTypeAndStatus(
                        tenantId, date, type, SnapshotStatus.FINAL);
        if (exists) {
            log.info("{} snapshot already FINAL for tenant {} date {} — skipping", type,
                    tenantId, date);
        }
        return exists;
    }

    private RegulatorySnapshot persist(
            Tenant tenant, LocalDate businessDate, String type, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            String hash = sha256(json);

            RegulatorySnapshot snapshot =
                    RegulatorySnapshot.builder()
                            .tenant(tenant)
                            .businessDate(businessDate)
                            .reportType(type)
                            .jsonPayload(json)
                            .hashChecksum(hash)
                            .generatedAt(LocalDateTime.now())
                            .generatedBy("EOD_SYSTEM")
                            .status(SnapshotStatus.FINAL)
                            .build();
            snapshot = snapshotRepository.save(snapshot);

            auditService.logEvent(
                    null,
                    "REGULATORY_SNAPSHOT_GENERATED",
                    "REGULATORY_SNAPSHOT",
                    snapshot.getId(),
                    type + " snapshot for tenant " + tenant.getId()
                            + " date " + businessDate + " hash=" + hash,
                    null);

            log.info("{} snapshot FINAL: tenant={} date={} hash={}",
                    type, tenant.getId(), businessDate, hash);
            return snapshot;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate " + type + " snapshot: " + e.getMessage(), e);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 computation failed", e);
        }
    }
}
