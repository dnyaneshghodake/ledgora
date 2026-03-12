package com.ledgora.approval.repository;

import com.ledgora.approval.entity.ApprovalRequest;
import com.ledgora.common.enums.ApprovalStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByStatus(ApprovalStatus status);

    long countByTenant_IdAndStatus(Long tenantId, ApprovalStatus status);

    List<ApprovalRequest> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<ApprovalRequest> findByRequestedBy_Id(Long userId);

    List<ApprovalRequest> findByApprovedBy_Id(Long userId);

    List<ApprovalRequest> findByEntityTypeAndStatus(String entityType, ApprovalStatus status);

    // ── Tenant-isolated query methods ──

    /** Tenant-isolated lookup by primary key. */
    java.util.Optional<ApprovalRequest> findByIdAndTenant_Id(Long id, Long tenantId);

    /** Tenant-isolated pending requests. */
    List<ApprovalRequest> findByTenant_IdAndStatus(Long tenantId, ApprovalStatus status);

    /** Tenant-isolated list by entity type and status. */
    List<ApprovalRequest> findByTenant_IdAndEntityTypeAndStatus(
            Long tenantId, String entityType, ApprovalStatus status);

    /** Tenant-isolated list of all requests. */
    List<ApprovalRequest> findByTenant_Id(Long tenantId);
}
