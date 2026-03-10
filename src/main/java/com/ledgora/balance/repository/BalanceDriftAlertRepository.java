package com.ledgora.balance.repository;

import com.ledgora.balance.entity.BalanceDriftAlert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceDriftAlertRepository extends JpaRepository<BalanceDriftAlert, Long> {

    List<BalanceDriftAlert> findByTenantIdAndStatus(Long tenantId, String status);

    List<BalanceDriftAlert> findByTenantIdOrderByDetectedAtDesc(Long tenantId);

    @Query(
            "SELECT COUNT(bda) FROM BalanceDriftAlert bda "
                    + "WHERE bda.tenant.id = :tenantId AND bda.status = 'OPEN'")
    long countOpenByTenantId(@Param("tenantId") Long tenantId);

    /** Check if an open alert already exists for this account (avoid duplicates). */
    boolean existsByAccountIdAndStatus(Long accountId, String status);
}
