package com.ledgora.approval.service;

import com.ledgora.approval.entity.ApprovalRequest;
import com.ledgora.approval.repository.ApprovalRequestRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.ApprovalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PART 3: Maker-Checker approval service.
 * Manages the lifecycle of approval requests for high-value operations.
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);
    private final ApprovalRequestRepository approvalRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ApprovalService(ApprovalRequestRepository approvalRepository,
                           UserRepository userRepository,
                           AuditService auditService) {
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Submit a new approval request (maker step).
     */
    @Transactional
    public ApprovalRequest submitForApproval(String entityType, Long entityId, String requestData) {
        User currentUser = getCurrentUser();

        ApprovalRequest request = ApprovalRequest.builder()
                .entityType(entityType)
                .entityId(entityId)
                .requestData(requestData)
                .requestedBy(currentUser)
                .status(ApprovalStatus.PENDING)
                .build();

        ApprovalRequest saved = approvalRepository.save(request);
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(userId, "APPROVAL_REQUESTED", entityType, entityId,
                "Approval requested for " + entityType + " ID: " + entityId, null);

        log.info("Approval request submitted: {} {} by user {}", entityType, entityId,
                currentUser != null ? currentUser.getUsername() : "system");
        return saved;
    }

    /**
     * Approve a pending request (checker step).
     */
    @Transactional
    public ApprovalRequest approve(Long requestId, String remarks) {
        ApprovalRequest request = approvalRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Approval request not found: " + requestId));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException("Approval request is not pending. Current status: " + request.getStatus());
        }

        User currentUser = getCurrentUser();
        if (request.getRequestedBy() != null && currentUser != null
                && request.getRequestedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot approve your own request (maker-checker violation)");
        }

        request.setStatus(ApprovalStatus.APPROVED);
        request.setApprovedBy(currentUser);
        request.setApprovedAt(LocalDateTime.now());
        request.setRemarks(remarks);

        ApprovalRequest saved = approvalRepository.save(request);
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(userId, "APPROVAL_APPROVED", request.getEntityType(), request.getEntityId(),
                "Approved: " + remarks, null);

        log.info("Approval request {} approved by {}", requestId,
                currentUser != null ? currentUser.getUsername() : "system");
        return saved;
    }

    /**
     * Reject a pending request.
     */
    @Transactional
    public ApprovalRequest reject(Long requestId, String remarks) {
        ApprovalRequest request = approvalRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Approval request not found: " + requestId));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException("Approval request is not pending. Current status: " + request.getStatus());
        }

        User currentUser = getCurrentUser();
        request.setStatus(ApprovalStatus.REJECTED);
        request.setApprovedBy(currentUser);
        request.setApprovedAt(LocalDateTime.now());
        request.setRemarks(remarks);

        ApprovalRequest saved = approvalRepository.save(request);
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(userId, "APPROVAL_REJECTED", request.getEntityType(), request.getEntityId(),
                "Rejected: " + remarks, null);

        log.info("Approval request {} rejected by {}", requestId,
                currentUser != null ? currentUser.getUsername() : "system");
        return saved;
    }

    public List<ApprovalRequest> getPendingRequests() {
        return approvalRepository.findByStatus(ApprovalStatus.PENDING);
    }

    public List<ApprovalRequest> getByEntityType(String entityType, ApprovalStatus status) {
        return approvalRepository.findByEntityTypeAndStatus(entityType, status);
    }

    public Optional<ApprovalRequest> getById(Long id) {
        return approvalRepository.findById(id);
    }

    public List<ApprovalRequest> getAllRequests() {
        return approvalRepository.findAll();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
