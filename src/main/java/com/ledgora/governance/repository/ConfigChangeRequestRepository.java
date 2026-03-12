package com.ledgora.governance.repository;

import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.governance.entity.ConfigChangeRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigChangeRequestRepository
        extends JpaRepository<ConfigChangeRequest, Long> {

    /** Tenant-isolated lookup by primary key. */
    Optional<ConfigChangeRequest> findByIdAndTenant_Id(Long id, Long tenantId);

    /** All pending change requests for a tenant (for governance dashboard). */
    List<ConfigChangeRequest> findByTenant_IdAndStatus(Long tenantId, ApprovalStatus status);

    /** Pending change requests by config type (e.g., show only PRODUCT changes). */
    List<ConfigChangeRequest> findByTenant_IdAndConfigTypeAndStatus(
            Long tenantId, String configType, ApprovalStatus status);

    /** All change requests for a specific entity (audit history). */
    @Query(
            "SELECT c FROM ConfigChangeRequest c WHERE c.tenant.id = :tenantId "
                    + "AND c.targetEntityType = :entityType AND c.targetEntityId = :entityId "
                    + "ORDER BY c.requestedAt DESC")
    List<ConfigChangeRequest> findByTenantAndEntity(
            @Param("tenantId") Long tenantId,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    /** Count pending changes for dashboard badge. */
    long countByTenant_IdAndStatus(Long tenantId, ApprovalStatus status);

    /** All requests for a tenant ordered by most recent. */
    List<ConfigChangeRequest> findByTenant_IdOrderByRequestedAtDesc(Long tenantId);
}
