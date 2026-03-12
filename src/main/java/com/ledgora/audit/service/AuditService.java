package com.ledgora.audit.service;

import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * PART 11: Enhanced audit service with full financial traceability. All audit logs use REQUIRES_NEW
 * transactions to ensure they persist even if the parent transaction rolls back.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(
            Long userId,
            String action,
            String entity,
            Long entityId,
            String details,
            String ipAddress) {
        logEvent(userId, action, entity, entityId, details, ipAddress, null);
    }

    /** PART 9: Enhanced audit logging with userAgent capture + hash chain. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(
            Long userId,
            String action,
            String entity,
            Long entityId,
            String details,
            String ipAddress,
            String userAgent) {
        // Resolve tenant for hash chain
        Long tenantId = null;
        try {
            tenantId = com.ledgora.tenant.context.TenantContextHolder.getTenantId();
        } catch (Exception ignored) {
            // No tenant context (e.g., startup seeding) — hash chain uses null tenant
        }

        AuditLog auditLog =
                AuditLog.builder()
                        .userId(userId)
                        .action(action)
                        .entity(entity)
                        .entityId(entityId)
                        .details(details)
                        .timestamp(LocalDateTime.now())
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .tenantId(tenantId)
                        .build();

        // Compute hash chain (tenant-specific)
        computeAndSetHash(auditLog, tenantId);

        auditLogRepository.save(auditLog);
        log.debug("Audit log: {} {} {} {}", action, entity, entityId, details);
    }

    /**
     * Compute SHA-256 hash chain for tamper-proof audit trail. hash = SHA256(previousHash + "|" +
     * action + "|" + entity + "|" + entityId + "|" + details + "|" + timestamp) Chain is
     * tenant-specific — each tenant has its own independent chain.
     */
    private void computeAndSetHash(AuditLog auditLog, Long tenantId) {
        try {
            // Find the previous entry's hash for this tenant
            String previousHash = "GENESIS";
            if (tenantId != null) {
                previousHash =
                        auditLogRepository
                                .findTopByTenantIdOrderByIdDesc(tenantId)
                                .map(AuditLog::getHash)
                                .orElse("GENESIS");
            }
            if (previousHash == null) {
                previousHash = "GENESIS";
            }
            auditLog.setPreviousHash(previousHash);

            // Build payload string for hashing
            String payload =
                    previousHash
                            + "|"
                            + auditLog.getAction()
                            + "|"
                            + auditLog.getEntity()
                            + "|"
                            + auditLog.getEntityId()
                            + "|"
                            + auditLog.getDetails()
                            + "|"
                            + auditLog.getTimestamp();

            // SHA-256 hash
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes =
                    digest.digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            auditLog.setHash(hex.toString());
        } catch (Exception e) {
            log.warn("Failed to compute audit hash chain: {}", e.getMessage());
            // Don't fail the audit log — hash is a governance enhancement, not critical path
        }
    }

    /**
     * Verify the integrity of the audit hash chain for a tenant. Returns the ID of the first broken
     * link, or -1 if chain is intact.
     */
    public long verifyHashChain(Long tenantId) {
        List<AuditLog> entries =
                auditLogRepository.findHashedEntriesByTenantIdOrderByIdAsc(tenantId);
        if (entries.isEmpty()) {
            return -1; // No entries = chain is trivially intact
        }

        String expectedPreviousHash = "GENESIS";
        for (AuditLog entry : entries) {
            // Verify previousHash links correctly
            if (entry.getPreviousHash() != null
                    && !entry.getPreviousHash().equals(expectedPreviousHash)) {
                log.error(
                        "AUDIT CHAIN BROKEN at entry ID {} for tenant {}. Expected previousHash={} but found={}",
                        entry.getId(),
                        tenantId,
                        expectedPreviousHash,
                        entry.getPreviousHash());
                return entry.getId();
            }

            // Recompute hash and verify
            String payload =
                    (entry.getPreviousHash() != null ? entry.getPreviousHash() : "GENESIS")
                            + "|"
                            + entry.getAction()
                            + "|"
                            + entry.getEntity()
                            + "|"
                            + entry.getEntityId()
                            + "|"
                            + entry.getDetails()
                            + "|"
                            + entry.getTimestamp();
            try {
                java.security.MessageDigest digest =
                        java.security.MessageDigest.getInstance("SHA-256");
                byte[] hashBytes =
                        digest.digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder();
                for (byte b : hashBytes) {
                    hex.append(String.format("%02x", b));
                }
                String recomputedHash = hex.toString();
                if (!recomputedHash.equals(entry.getHash())) {
                    log.error(
                            "AUDIT HASH TAMPERED at entry ID {} for tenant {}. Stored={} Recomputed={}",
                            entry.getId(),
                            tenantId,
                            entry.getHash(),
                            recomputedHash);
                    return entry.getId();
                }
            } catch (Exception e) {
                log.error(
                        "Hash verification failed for entry {}: {}", entry.getId(), e.getMessage());
                return entry.getId();
            }

            expectedPreviousHash = entry.getHash();
        }

        log.info(
                "Audit hash chain verified for tenant {}: {} entries, chain intact",
                tenantId,
                entries.size());
        return -1; // Chain intact
    }

    /**
     * PART 11: Extended audit logging with request/response payloads. Uses REQUIRES_NEW to ensure
     * audit trail survives transaction rollbacks.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFinancialEvent(
            Long userId,
            String action,
            String entity,
            Long entityId,
            String details,
            String ipAddress,
            String requestPayload,
            String responsePayload,
            String username,
            String httpMethod,
            String requestUri) {
        // Resolve tenant for hash chain
        Long tenantId = null;
        try {
            tenantId = com.ledgora.tenant.context.TenantContextHolder.getTenantId();
        } catch (Exception ignored) {
        }

        AuditLog auditLog =
                AuditLog.builder()
                        .userId(userId)
                        .action(action)
                        .entity(entity)
                        .entityId(entityId)
                        .details(details)
                        .timestamp(LocalDateTime.now())
                        .ipAddress(ipAddress)
                        .requestPayload(requestPayload)
                        .responsePayload(responsePayload)
                        .username(username)
                        .httpMethod(httpMethod)
                        .requestUri(requestUri)
                        .tenantId(tenantId)
                        .build();

        // Compute hash chain (tenant-specific)
        computeAndSetHash(auditLog, tenantId);

        auditLogRepository.save(auditLog);
        log.debug(
                "Financial audit log: {} {} {} {} user={}",
                action,
                entity,
                entityId,
                details,
                username);
    }

    public void logLogin(Long userId, String username, String ipAddress) {
        logEvent(userId, "LOGIN", "USER", userId, "User login: " + username, ipAddress);
    }

    public void logAccountCreation(Long userId, Long accountId, String accountNumber) {
        logEvent(userId, "CREATE", "ACCOUNT", accountId, "Account created: " + accountNumber, null);
    }

    public void logTransaction(
            Long userId, Long transactionId, String transactionRef, String type) {
        logEvent(
                userId, type, "TRANSACTION", transactionId, "Transaction: " + transactionRef, null);
    }

    public void logSettlement(Long userId, Long settlementId, String settlementRef) {
        logEvent(
                userId,
                "SETTLEMENT",
                "SETTLEMENT",
                settlementId,
                "Settlement processed: " + settlementRef,
                null);
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAll();
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    public List<AuditLog> getByEntity(String entity) {
        return auditLogRepository.findByEntity(entity);
    }

    public Page<AuditLog> getByEntity(String entity, Pageable pageable) {
        return auditLogRepository.findByEntityOrderByTimestampDesc(entity, pageable);
    }

    public List<AuditLog> getByUser(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    /**
     * CBS-grade audit with old/new value tracking and batch linkage. Used for master data changes
     * (customer, account, config) and financial operations.
     *
     * @param userId the user performing the action
     * @param action the action type (CREATE, UPDATE, APPROVE, REJECT, etc.)
     * @param entity the entity type (CUSTOMER, ACCOUNT, TRANSACTION, etc.)
     * @param entityId the entity ID
     * @param details human-readable description
     * @param ipAddress the IP address of the user
     * @param oldValue JSON/text snapshot of entity BEFORE the change (null for creates)
     * @param newValue JSON/text snapshot of entity AFTER the change
     * @param batchId batch ID for transaction audit linkage (null for non-transaction events)
     * @param tenantId tenant ID for multi-tenant audit isolation
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logChangeEvent(
            Long userId,
            String action,
            String entity,
            Long entityId,
            String details,
            String ipAddress,
            String oldValue,
            String newValue,
            Long batchId,
            Long tenantId) {
        AuditLog auditLog =
                AuditLog.builder()
                        .userId(userId)
                        .action(action)
                        .entity(entity)
                        .entityId(entityId)
                        .details(details)
                        .timestamp(LocalDateTime.now())
                        .ipAddress(ipAddress)
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .batchId(batchId)
                        .tenantId(tenantId)
                        .build();

        // Compute hash chain (tenant-specific)
        computeAndSetHash(auditLog, tenantId);

        auditLogRepository.save(auditLog);
        log.debug(
                "Change audit: {} {} {} old={} new={} batch={}",
                action,
                entity,
                entityId,
                oldValue != null
                        ? oldValue.substring(0, Math.min(50, oldValue.length())) + "..."
                        : "null",
                newValue != null
                        ? newValue.substring(0, Math.min(50, newValue.length())) + "..."
                        : "null",
                batchId);
    }
}
