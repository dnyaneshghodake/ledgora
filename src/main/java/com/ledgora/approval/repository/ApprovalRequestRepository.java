package com.ledgora.approval.repository;

import com.ledgora.approval.entity.ApprovalRequest;
import com.ledgora.common.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByStatus(ApprovalStatus status);
    long countByTenant_IdAndStatus(Long tenantId, ApprovalStatus status);
    List<ApprovalRequest> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<ApprovalRequest> findByRequestedBy_Id(Long userId);
    List<ApprovalRequest> findByApprovedBy_Id(Long userId);
    List<ApprovalRequest> findByEntityTypeAndStatus(String entityType, ApprovalStatus status);
}
