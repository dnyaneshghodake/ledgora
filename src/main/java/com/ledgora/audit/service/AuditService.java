package com.ledgora.audit.service;

import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PART 11: Enhanced audit service with full financial traceability.
 * All audit logs use REQUIRES_NEW transactions to ensure they persist
 * even if the parent transaction rolls back.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(Long userId, String action, String entity, Long entityId, String details, String ipAddress) {
        logEvent(userId, action, entity, entityId, details, ipAddress, null);
    }

    /**
     * PART 9: Enhanced audit logging with userAgent capture.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(Long userId, String action, String entity, Long entityId, String details,
                         String ipAddress, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .details(details)
                .timestamp(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit log: {} {} {} {}", action, entity, entityId, details);
    }

    /**
     * PART 11: Extended audit logging with request/response payloads.
     * Uses REQUIRES_NEW to ensure audit trail survives transaction rollbacks.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFinancialEvent(Long userId, String action, String entity, Long entityId,
                                  String details, String ipAddress, String requestPayload,
                                  String responsePayload, String username, String httpMethod,
                                  String requestUri) {
        AuditLog auditLog = AuditLog.builder()
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
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Financial audit log: {} {} {} {} user={}", action, entity, entityId, details, username);
    }

    public void logLogin(Long userId, String username, String ipAddress) {
        logEvent(userId, "LOGIN", "USER", userId, "User login: " + username, ipAddress);
    }

    public void logAccountCreation(Long userId, Long accountId, String accountNumber) {
        logEvent(userId, "CREATE", "ACCOUNT", accountId, "Account created: " + accountNumber, null);
    }

    public void logTransaction(Long userId, Long transactionId, String transactionRef, String type) {
        logEvent(userId, type, "TRANSACTION", transactionId, "Transaction: " + transactionRef, null);
    }

    public void logSettlement(Long userId, Long settlementId, String settlementRef) {
        logEvent(userId, "SETTLEMENT", "SETTLEMENT", settlementId, "Settlement processed: " + settlementRef, null);
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
     * CBS-grade audit with old/new value tracking and batch linkage.
     * Used for master data changes (customer, account, config) and financial operations.
     *
     * @param userId    the user performing the action
     * @param action    the action type (CREATE, UPDATE, APPROVE, REJECT, etc.)
     * @param entity    the entity type (CUSTOMER, ACCOUNT, TRANSACTION, etc.)
     * @param entityId  the entity ID
     * @param details   human-readable description
     * @param ipAddress the IP address of the user
     * @param oldValue  JSON/text snapshot of entity BEFORE the change (null for creates)
     * @param newValue  JSON/text snapshot of entity AFTER the change
     * @param batchId   batch ID for transaction audit linkage (null for non-transaction events)
     * @param tenantId  tenant ID for multi-tenant audit isolation
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logChangeEvent(Long userId, String action, String entity, Long entityId,
                                String details, String ipAddress,
                                String oldValue, String newValue,
                                Long batchId, Long tenantId) {
        AuditLog auditLog = AuditLog.builder()
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
        auditLogRepository.save(auditLog);
        log.debug("Change audit: {} {} {} old={} new={} batch={}", action, entity, entityId,
                oldValue != null ? oldValue.substring(0, Math.min(50, oldValue.length())) + "..." : "null",
                newValue != null ? newValue.substring(0, Math.min(50, newValue.length())) + "..." : "null",
                batchId);
    }
}
