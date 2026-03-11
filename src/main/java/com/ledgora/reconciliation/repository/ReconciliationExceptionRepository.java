package com.ledgora.reconciliation.repository;

import com.ledgora.reconciliation.entity.ReconciliationException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciliationExceptionRepository
        extends JpaRepository<ReconciliationException, Long> {

    @Query("SELECT re FROM ReconciliationException re WHERE re.tenantId = :tenantId AND re.businessDate = :date")
    List<ReconciliationException> findByTenantIdAndBusinessDate(
            @Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query("SELECT re FROM ReconciliationException re WHERE re.tenantId = :tenantId AND re.resolved = false")
    List<ReconciliationException> findUnresolvedByTenantId(@Param("tenantId") Long tenantId);

    long countByTenantIdAndBusinessDateAndResolvedFalse(Long tenantId, LocalDate businessDate);

    @Query("SELECT COALESCE(SUM(re.mismatchAmount), 0) FROM ReconciliationException re WHERE re.tenantId = :tenantId AND re.businessDate = :date AND re.resolved = false")
    java.math.BigDecimal sumUnresolvedMismatchByTenantIdAndDate(
            @Param("tenantId") Long tenantId, @Param("date") LocalDate date);
}
