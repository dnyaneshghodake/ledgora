package com.ledgora.approval.repository;

import com.ledgora.approval.entity.ApprovalRequest;
import com.ledgora.common.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByStatus(ApprovalStatus status);
    long countByTenantIdAndStatus(Long tenantId, ApprovalStatus status);
    List<ApprovalRequest> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<ApprovalRequest> findByRequestedById(Long userId);
    List<ApprovalRequest> findByApprovedById(Long userId);
    List<ApprovalRequest> findByEntityTypeAndStatus(String entityType, ApprovalStatus status);
}
