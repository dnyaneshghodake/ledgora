package com.ledgora.audit.repository;

import com.ledgora.audit.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByEntity(String entity);

    List<AuditLog> findByEntityAndEntityId(String entity, Long entityId);

    /** Tenant-isolated lookup by entity type and entity ID (for freeze/lien history display). */
    List<AuditLog> findByTenantIdAndEntityAndEntityId(Long tenantId, String entity, Long entityId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Paginated queries for audit log viewer
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByEntityOrderByTimestampDesc(String entity, Pageable pageable);

    // Enhanced audit trail queries (CBS compliance)
    List<AuditLog> findByTenantIdOrderByTimestampDesc(Long tenantId);

    Page<AuditLog> findByTenantIdOrderByTimestampDesc(Long tenantId, Pageable pageable);

    List<AuditLog> findByBatchId(Long batchId);

    List<AuditLog> findByEntityAndEntityIdOrderByTimestampDesc(String entity, Long entityId);

    Page<AuditLog> findByTenantIdAndEntityOrderByTimestampDesc(
            Long tenantId, String entity, Pageable pageable);

    Page<AuditLog> findByTenantIdAndActionOrderByTimestampDesc(
            Long tenantId, String action, Pageable pageable);

    // ===== Governance dashboard queries =====

    /**
     * Count audit events by tenant, action, and time range (e.g., today's hard ceiling violations).
     */
    long countByTenantIdAndActionAndTimestampBetween(
            Long tenantId, String action, LocalDateTime start, LocalDateTime end);

    /**
     * Fetch most recent audit events for a specific action (e.g., last 20 hard ceiling violations).
     */
    List<AuditLog> findTop20ByTenantIdAndActionOrderByTimestampDesc(Long tenantId, String action);

    // ===== Hash chain queries =====

    /** Count all audit events for a tenant (for dashboard badge — avoids loading full list). */
    long countByTenantId(Long tenantId);

    /** Find the most recent audit log entry for a tenant (for hash chain linking). */
    java.util.Optional<AuditLog> findTopByTenantIdOrderByIdDesc(Long tenantId);

    /** Find audit entries with broken hash chain (for integrity verification). */
    @org.springframework.data.jpa.repository.Query(
            "SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.hash IS NOT NULL ORDER BY a.id ASC")
    List<AuditLog> findHashedEntriesByTenantIdOrderByIdAsc(
            @org.springframework.data.repository.query.Param("tenantId") Long tenantId);
}
