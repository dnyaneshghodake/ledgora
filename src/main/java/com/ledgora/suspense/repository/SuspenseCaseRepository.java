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
}
