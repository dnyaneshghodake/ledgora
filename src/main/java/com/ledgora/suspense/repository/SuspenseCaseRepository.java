package com.ledgora.suspense.repository;

import com.ledgora.suspense.entity.SuspenseCase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SuspenseCaseRepository extends JpaRepository<SuspenseCase, Long> {

    List<SuspenseCase> findByTenantIdAndStatus(Long tenantId, String status);

    List<SuspenseCase> findByTenantIdAndBusinessDateAndStatus(
            Long tenantId, LocalDate businessDate, String status);

    @Query(
            "SELECT COUNT(sc) FROM SuspenseCase sc "
                    + "WHERE sc.tenant.id = :tenantId AND sc.status = 'OPEN'")
    long countOpenByTenantId(@Param("tenantId") Long tenantId);

    @Query(
            "SELECT COALESCE(SUM(sc.amount), 0) FROM SuspenseCase sc "
                    + "WHERE sc.tenant.id = :tenantId AND sc.status = 'OPEN'")
    java.math.BigDecimal sumOpenAmountByTenantId(@Param("tenantId") Long tenantId);

    // ===== Suspense dashboard queries =====

    /** Count cases by tenant and status (generic — works for OPEN, RESOLVED, REVERSED). */
    long countByTenantIdAndStatus(Long tenantId, String status);

    /**
     * Fetch the oldest OPEN suspense cases with all associations eagerly loaded (N+1 prevention).
     * Used by the dashboard aging table. Limits handled in Java (Spring Data Top10).
     */
    @Query(
            "SELECT DISTINCT sc FROM SuspenseCase sc "
                    + "LEFT JOIN FETCH sc.originalTransaction "
                    + "LEFT JOIN FETCH sc.intendedAccount "
                    + "LEFT JOIN FETCH sc.suspenseAccount "
                    + "WHERE sc.tenant.id = :tenantId AND sc.status = 'OPEN' "
                    + "ORDER BY sc.createdAt ASC")
    List<SuspenseCase> findOldestOpenByTenantId(@Param("tenantId") Long tenantId);
}
