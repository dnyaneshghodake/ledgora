package com.ledgora.reporting.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.reporting.entity.FinancialStatementSnapshot;
import com.ledgora.reporting.enums.SnapshotStatus;
import com.ledgora.reporting.enums.StatementType;
import com.ledgora.reporting.repository.FinancialStatementSnapshotRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI-grade Financial Statement Snapshot Service.
 *
 * <p>Orchestrates Balance Sheet + P&L snapshot generation with:
 *
 * <ul>
 *   <li>SHA-256 checksum for tamper detection (RBI IT Framework)
 *   <li>DRAFT → FINAL immutable lifecycle
 *   <li>Idempotent — skips if FINAL snapshot already exists
 *   <li>Audit trail for every generation
 *   <li>Crash-recovery safe — DRAFT snapshots regenerated on resume
 * </ul>
 *
 * <p>Called by EOD after DATE_ADVANCED phase via {@link #generateDailySnapshots(Long)}.
 *
 * <p>Architecture: LedgerEntry → GeneralLedger → StatementLineMapping → Snapshot. AccountBalance
 * is NEVER used as accounting source of truth.
 */
@Service
public class FinancialStatementService {

    private static final Logger log = LoggerFactory.getLogger(FinancialStatementService.class);

    private final BalanceSheetEngine balanceSheetEngine;
    private final PnlEngine pnlEngine;
    private final FinancialStatementSnapshotRepository snapshotRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public FinancialStatementService(
            BalanceSheetEngine balanceSheetEngine,
            PnlEngine pnlEngine,
            FinancialStatementSnapshotRepository snapshotRepository,
            TenantRepository tenantRepository,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.balanceSheetEngine = balanceSheetEngine;
        this.pnlEngine = pnlEngine;
        this.snapshotRepository = snapshotRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate daily Balance Sheet + P&L snapshots. Called during EOD after DATE_ADVANCED.
     *
     * @param tenantId tenant isolation
     * @param businessDate the completed business date (passed from EodProcess, not derived)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateDailySnapshots(Long tenantId, LocalDate businessDate) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () -> new RuntimeException("Tenant not found: " + tenantId));

        generateBalanceSheetSnapshot(tenant, businessDate);
        generatePnlSnapshot(tenant, businessDate);

        log.info(
                "Daily financial snapshots generated for tenant {} date {}",
                tenantId,
                businessDate);
    }

    /** Generate and persist a Balance Sheet snapshot. Validates A=L+E before persisting. */
    @Transactional
    public FinancialStatementSnapshot generateBalanceSheetSnapshot(
            Tenant tenant, LocalDate businessDate) {
        Long tenantId = tenant.getId();

        if (snapshotRepository.existsByTenantIdAndBusinessDateAndStatementTypeAndStatus(
                tenantId, businessDate, StatementType.BALANCE_SHEET, SnapshotStatus.FINAL)) {
            log.info(
                    "Balance Sheet already FINAL for tenant {} date {} — skipping",
                    tenantId,
                    businessDate);
            return snapshotRepository
                    .findByTenantIdAndBusinessDateAndStatementTypeAndStatus(
                            tenantId, businessDate, StatementType.BALANCE_SHEET,
                            SnapshotStatus.FINAL)
                    .orElse(null);
        }

        // Throws AccountingException if A != L + E
        Map<String, Object> data = balanceSheetEngine.generate(tenantId, businessDate);
        return persistSnapshot(
                tenant, businessDate, StatementType.BALANCE_SHEET, data, "EOD_SYSTEM");
    }

    /** Generate and persist a daily P&L snapshot. */
    @Transactional
    public FinancialStatementSnapshot generatePnlSnapshot(
            Tenant tenant, LocalDate businessDate) {
        Long tenantId = tenant.getId();

        if (snapshotRepository.existsByTenantIdAndBusinessDateAndStatementTypeAndStatus(
                tenantId, businessDate, StatementType.PNL, SnapshotStatus.FINAL)) {
            log.info(
                    "P&L already FINAL for tenant {} date {} — skipping",
                    tenantId,
                    businessDate);
            return snapshotRepository
                    .findByTenantIdAndBusinessDateAndStatementTypeAndStatus(
                            tenantId, businessDate, StatementType.PNL, SnapshotStatus.FINAL)
                    .orElse(null);
        }

        Map<String, Object> data = pnlEngine.generateDaily(tenantId, businessDate);
        return persistSnapshot(tenant, businessDate, StatementType.PNL, data, "EOD_SYSTEM");
    }

    private FinancialStatementSnapshot persistSnapshot(
            Tenant tenant,
            LocalDate businessDate,
            StatementType type,
            Map<String, Object> data,
            String generatedBy) {
        try {
            String json = objectMapper.writeValueAsString(data);
            String hash = sha256(json);

            FinancialStatementSnapshot snapshot =
                    FinancialStatementSnapshot.builder()
                            .tenant(tenant)
                            .businessDate(businessDate)
                            .statementType(type)
                            .jsonPayload(json)
                            .hashChecksum(hash)
                            .generatedAt(LocalDateTime.now())
                            .generatedBy(generatedBy)
                            .status(SnapshotStatus.FINAL)
                            .build();
            snapshot = snapshotRepository.save(snapshot);

            auditService.logEvent(
                    null,
                    "STATEMENT_SNAPSHOT_GENERATED",
                    "FINANCIAL_STATEMENT",
                    snapshot.getId(),
                    type.name()
                            + " snapshot for tenant "
                            + tenant.getId()
                            + " date "
                            + businessDate
                            + " hash="
                            + hash,
                    null);

            log.info(
                    "{} snapshot FINAL: tenant={} date={} hash={}",
                    type,
                    tenant.getId(),
                    businessDate,
                    hash);

            return snapshot;
        } catch (com.ledgora.common.exception.AccountingException ae) {
            throw ae; // re-throw accounting violations without wrapping
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate " + type + " snapshot: " + e.getMessage(), e);
        }
    }

    /** SHA-256 hash for tamper detection per RBI IT Framework. */
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
