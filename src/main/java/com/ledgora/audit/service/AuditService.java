package com.ledgora.audit.service;

import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .details(details)
                .timestamp(LocalDateTime.now())
                .ipAddress(ipAddress)
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

    public List<AuditLog> getByEntity(String entity) {
        return auditLogRepository.findByEntity(entity);
    }

    public List<AuditLog> getByUser(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }
}
