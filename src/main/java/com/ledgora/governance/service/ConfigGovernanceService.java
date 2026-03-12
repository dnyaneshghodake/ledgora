package com.ledgora.governance.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.governance.entity.ConfigChangeRequest;
import com.ledgora.governance.repository.ConfigChangeRequestRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS Config Governance Service — enforces maker-checker dual control on all configuration
 * parameter changes.
 *
 * <p>Usage pattern:
 *
 * <pre>
 *   // Maker submits a change
 *   configGovernanceService.submitChange("APPROVAL_POLICY", "ApprovalPolicy", policyId,
 *       "Increase teller limit to 500000", oldJson, newJson, "maxAmount", effectiveDate);
 *
 *   // Checker approves
 *   configGovernanceService.approve(changeRequestId, "Reviewed and approved");
 *   // Then the calling service applies the newValue to the target entity
 * </pre>
 *
 * <p>RBI IT Framework — Change Management:
 *
 * <ul>
 *   <li>All config changes are persisted as immutable change request records
 *   <li>Maker != checker enforced
 *   <li>Before/after snapshot for regulatory audit
 *   <li>Effective dating for scheduled changes
 * </ul>
 */
@Service
public class ConfigGovernanceService {

    private static final Logger log = LoggerFactory.getLogger(ConfigGovernanceService.class);

    private final ConfigChangeRequestRepository changeRequestRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ConfigGovernanceService(
            ConfigChangeRequestRepository changeRequestRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.changeRequestRepository = changeRequestRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Submit a configuration change for maker-checker approval.
     *
     * @param configType category (PRODUCT, APPROVAL_POLICY, HARD_LIMIT, etc.)
     * @param entityType target entity class name
     * @param entityId target entity ID (null for new records)
     * @param description human-readable change description
     * @param oldValue JSON snapshot before change (null for creates)
     * @param newValue JSON snapshot of proposed new state
     * @param fieldName specific field changed (null for bulk)
     * @param effectiveDate when the change should take effect (null = immediate)
     * @return the created change request in PENDING status
     */
    @Transactional
    public ConfigChangeRequest submitChange(
            String configType,
            String entityType,
            Long entityId,
            String description,
            String oldValue,
            String newValue,
            String fieldName,
            LocalDate effectiveDate) {

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        User maker = getCurrentUser();

        ConfigChangeRequest request =
                ConfigChangeRequest.builder()
                        .tenant(tenant)
                        .configType(configType)
                        .targetEntityType(entityType)
                        .targetEntityId(entityId)
                        .changeDescription(description)
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .fieldName(fieldName)
                        .effectiveDate(effectiveDate)
                        .requestedBy(maker)
                        .status(ApprovalStatus.PENDING)
                        .build();

        ConfigChangeRequest saved = changeRequestRepository.save(request);

        Long userId = maker != null ? maker.getId() : null;
        auditService.logEvent(
                userId,
                "CONFIG_CHANGE_REQUESTED",
                entityType,
                entityId,
                "Config change requested: " + configType + " — " + description,
                null);

        log.info(
                "Config change request submitted: id={} type={} entity={}/{} by {}",
                saved.getId(),
                configType,
                entityType,
                entityId,
                maker != null ? maker.getUsername() : "system");
        return saved;
    }

    /** Approve a pending config change (checker step). Enforces maker != checker. */
    @Transactional
    public ConfigChangeRequest approve(Long requestId, String remarks) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ConfigChangeRequest request =
                changeRequestRepository
                        .findByIdAndTenant_Id(requestId, tenantId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Config change request not found: " + requestId));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException(
                    "Change request is not pending. Current status: " + request.getStatus());
        }

        User checker = getCurrentUser();
        if (checker == null) {
            throw new RuntimeException(
                    "Cannot approve config change: approver identity could not be resolved");
        }
        if (request.getRequestedBy() != null
                && request.getRequestedBy().getId().equals(checker.getId())) {
            throw new RuntimeException(
                    "Cannot approve your own config change (maker-checker violation)");
        }

        request.setStatus(ApprovalStatus.APPROVED);
        request.setApprovedBy(checker);
        request.setApprovedAt(LocalDateTime.now());
        request.setRemarks(remarks);

        ConfigChangeRequest saved = changeRequestRepository.save(request);

        Long userId = checker != null ? checker.getId() : null;
        auditService.logEvent(
                userId,
                "CONFIG_CHANGE_APPROVED",
                request.getTargetEntityType(),
                request.getTargetEntityId(),
                "Config change approved: "
                        + request.getConfigType()
                        + " — "
                        + request.getChangeDescription()
                        + (remarks != null ? " | Remarks: " + remarks : ""),
                null);

        log.info(
                "Config change approved: id={} type={} by {}",
                requestId,
                request.getConfigType(),
                checker != null ? checker.getUsername() : "system");
        return saved;
    }

    /** Reject a pending config change. */
    @Transactional
    public ConfigChangeRequest reject(Long requestId, String remarks) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ConfigChangeRequest request =
                changeRequestRepository
                        .findByIdAndTenant_Id(requestId, tenantId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Config change request not found: " + requestId));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException(
                    "Change request is not pending. Current status: " + request.getStatus());
        }

        User checker = getCurrentUser();
        if (checker == null) {
            throw new RuntimeException(
                    "Cannot reject config change: reviewer identity could not be resolved");
        }
        request.setStatus(ApprovalStatus.REJECTED);
        request.setApprovedBy(checker);
        request.setApprovedAt(LocalDateTime.now());
        request.setRemarks(remarks);

        ConfigChangeRequest saved = changeRequestRepository.save(request);

        Long userId = checker != null ? checker.getId() : null;
        auditService.logEvent(
                userId,
                "CONFIG_CHANGE_REJECTED",
                request.getTargetEntityType(),
                request.getTargetEntityId(),
                "Config change rejected: "
                        + request.getConfigType()
                        + " — "
                        + request.getChangeDescription()
                        + " | Reason: "
                        + remarks,
                null);

        log.info(
                "Config change rejected: id={} type={} by {}",
                requestId,
                request.getConfigType(),
                checker != null ? checker.getUsername() : "system");
        return saved;
    }

    /** Get all pending config changes for the current tenant. */
    public List<ConfigChangeRequest> getPendingChanges() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return changeRequestRepository.findByTenant_IdAndStatus(tenantId, ApprovalStatus.PENDING);
    }

    /** Get pending changes by config type. */
    public List<ConfigChangeRequest> getPendingChangesByType(String configType) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return changeRequestRepository.findByTenant_IdAndConfigTypeAndStatus(
                tenantId, configType, ApprovalStatus.PENDING);
    }

    /** Get change history for a specific entity (audit trail). */
    public List<ConfigChangeRequest> getChangeHistory(String entityType, Long entityId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return changeRequestRepository.findByTenantAndEntity(tenantId, entityType, entityId);
    }

    /** Get a specific change request by ID (tenant-isolated). */
    public Optional<ConfigChangeRequest> getById(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return changeRequestRepository.findByIdAndTenant_Id(id, tenantId);
    }

    /** Count pending config changes (for governance dashboard badge). */
    public long countPending() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return changeRequestRepository.countByTenant_IdAndStatus(tenantId, ApprovalStatus.PENDING);
    }

    private User getCurrentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
