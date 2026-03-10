package com.ledgora.fraud.repository;

import com.ledgora.fraud.entity.FraudAlert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findByTenantIdAndStatus(Long tenantId, String status);

    List<FraudAlert> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<FraudAlert> findByAccountIdAndStatusOrderByCreatedAtDesc(Long accountId, String status);

    @Query(
            "SELECT COUNT(fa) FROM FraudAlert fa "
                    + "WHERE fa.tenant.id = :tenantId AND fa.status = 'OPEN'")
    long countOpenByTenantId(@Param("tenantId") Long tenantId);
}
