package com.ledgora.audit.repository;

import com.ledgora.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByEntity(String entity);
    List<AuditLog> findByEntityAndEntityId(String entity, Long entityId);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Paginated queries for audit log viewer
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<AuditLog> findByEntityOrderByTimestampDesc(String entity, Pageable pageable);
}
