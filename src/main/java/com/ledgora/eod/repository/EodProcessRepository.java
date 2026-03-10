package com.ledgora.eod.repository;

import com.ledgora.eod.entity.EodProcess;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EodProcessRepository extends JpaRepository<EodProcess, Long> {

    /** Find EOD process for a specific tenant and business date. */
    Optional<EodProcess> findByTenantIdAndBusinessDate(Long tenantId, LocalDate businessDate);

    /** Find all incomplete (RUNNING) EOD processes — for restart recovery. */
    List<EodProcess> findByStatus(String status);

    /** Detect stuck EOD processes: RUNNING status and last_updated older than threshold. */
    @Query(
            "SELECT e FROM EodProcess e WHERE e.status = 'RUNNING' "
                    + "AND e.lastUpdated < :stuckThreshold")
    List<EodProcess> findStuckProcesses(@Param("stuckThreshold") LocalDateTime stuckThreshold);

    /** Find the most recent EOD process for a tenant. */
    Optional<EodProcess> findTopByTenantIdOrderByBusinessDateDesc(Long tenantId);
}
