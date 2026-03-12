package com.ledgora.approval.service;

import com.ledgora.approval.entity.ApprovalRequest;
import com.ledgora.approval.repository.ApprovalRequestRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maker-Checker approval service. Manages the lifecycle of approval requests for high-value
 * operations. All approval requests are tenant-scoped for multi-tenant isolation.
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);
    private final ApprovalRequestRepository approvalRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TenantRepository tenantRepository;

    public ApprovalService(
            ApprovalRequestRepository approvalRepository,
            UserRepository userRepository,
            AuditService auditService,
            TenantRepository tenantRepository) {
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.tenantRepository = tenantRepository;
    }

    /** Submit a new approval request (maker step). */
    @Transactional
    public ApprovalRequest submitForApproval(String entityType, Long entityId, String requestData) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException(
                    "Cannot submit approval request: requester identity could not be resolved");
        }

        // Resolve tenant for multi-tenant approval isolation
        Tenant tenant = null;
        try {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            tenant = tenantRepository.findById(tenantId).orElse(null);
        } catch (IllegalStateException e) {
            log.warn("No tenant context set for approval request: {} {}", entityType, entityId);
        }

        ApprovalRequest request =
                ApprovalRequest.builder()
                        .entityType(entityType)
                        .entityId(entityId)
                        .requestData(requestData)
                        .requestedBy(currentUser)
                        .tenant(tenant)
                        .status(ApprovalStatus.PENDING)
                        .build();

        ApprovalRequest saved = approvalRepository.save(request);
        auditService.logEvent(
                currentUser.getId(),
                "APPROVAL_REQUESTED",
                entityType,
                entityId,
                "Approval requested for " + entityType + " ID: " + entityId,
                null);

        log.info(
                "Approval request submitted: {} {} by user {}",
                entityType,
                entityId,
                currentUser.getUsername());
        return saved;
    }

    /** Approve a pending request (checker step). Tenant-isolated. */
    @Transactional
    public ApprovalRequest approve(Long requestId, String remarks) {
        ApprovalRequest request = findByIdWithTenantIsolation(requestId);

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException(
                    "Approval request is not pending. Current status: " + request.getStatus());
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException(
                    "Cannot approve request: approver identity could not be resolved");
        }
        if (request.getRequestedBy() != null
                && request.getRequestedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot approve your own request (maker-checker violation)");
        }

        request.setStatus(ApprovalStatus.APPROVED);
        request.setApprovedBy(currentUser);
        request.setApprovedAt(LocalDateTime.now());
        request.setRemarks(remarks);

        ApprovalRequest saved = approvalRepository.save(request);
        auditService.logEvent(
                currentUser.getId(),
                "APPROVAL_APPROVED",
                request.getEntityType(),
                request.getEntityId(),
                "Approved: " + remarks,
                null);

        log.info(
                "Approval request {} approved by {}",
                requestId,
                currentUser.getUsername());
        return saved;
    }

    /** Reject a pending request. Tenant-isolated. */
    @Transactional
    public ApprovalRequest reject(Long requestId, String remarks) {
        ApprovalRequest request = findByIdWithTenantIsolation(requestId);

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException(
                    "Approval request is not pending. Current status: " + request.getStatus());
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException(
                    "Cannot reject request: reviewer identity could not be resolved");
        }
        if (request.getRequestedBy() != null
                && request.getRequestedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot reject your own request (maker-checker violation)");
        }
        request.setStatus(ApprovalStatus.REJECTED);
        request.setApprovedBy(currentUser);
        request.setApprovedAt(LocalDateTime.now());
        request.setRemarks(remarks);

        ApprovalRequest saved = approvalRepository.save(request);
        auditService.logEvent(
                currentUser.getId(),
                "APPROVAL_REJECTED",
                request.getEntityType(),
                request.getEntityId(),
                "Rejected: " + remarks,
                null);

        log.info(
                "Approval request {} rejected by {}",
                requestId,
                currentUser.getUsername());
        return saved;
    }

    public List<ApprovalRequest> getPendingRequests() {
        Long tenantId = requireTenantId();
        return approvalRepository.findByTenant_IdAndStatus(tenantId, ApprovalStatus.PENDING);
    }

    public List<ApprovalRequest> getByEntityType(String entityType, ApprovalStatus status) {
        Long tenantId = requireTenantId();
        return approvalRepository.findByTenant_IdAndEntityTypeAndStatus(
                tenantId, entityType, status);
    }

    public Optional<ApprovalRequest> getById(Long id) {
        Long tenantId = requireTenantId();
        return approvalRepository.findByIdAndTenant_Id(id, tenantId);
    }

    public List<ApprovalRequest> getAllRequests() {
        Long tenantId = requireTenantId();
        return approvalRepository.findByTenant_Id(tenantId);
    }

    /**
     * Tenant-isolated lookup by ID. Throws if not found or belongs to a different tenant. Used by
     * approve() and reject() to prevent cross-tenant approval manipulation.
     */
    private ApprovalRequest findByIdWithTenantIsolation(Long requestId) {
        Long tenantId = requireTenantId();
        return approvalRepository
                .findByIdAndTenant_Id(requestId, tenantId)
                .orElseThrow(
                        () -> new RuntimeException("Approval request not found: " + requestId));
    }

    private Long requireTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private User getCurrentUser() {
        try {
            org.springframework.security.core.Authentication auth =
                    SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return null;
            return userRepository.findByUsername(auth.getName()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
