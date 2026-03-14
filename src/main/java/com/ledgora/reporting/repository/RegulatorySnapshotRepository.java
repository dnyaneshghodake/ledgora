package com.ledgora.reporting.repository;

import com.ledgora.reporting.entity.RegulatorySnapshot;
import com.ledgora.reporting.enums.SnapshotStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegulatorySnapshotRepository
        extends JpaRepository<RegulatorySnapshot, Long> {

    Optional<RegulatorySnapshot> findByTenantIdAndBusinessDateAndReportType(
            Long tenantId, LocalDate businessDate, String reportType);

    boolean existsByTenantIdAndBusinessDateAndReportTypeAndStatus(
            Long tenantId, LocalDate businessDate, String reportType,
            SnapshotStatus status);
}
