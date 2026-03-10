package com.ledgora.tenant.repository;

import com.ledgora.common.enums.DayStatus;
import com.ledgora.tenant.entity.Tenant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantCode(String tenantCode);

    boolean existsByTenantCode(String tenantCode);

    List<Tenant> findByStatus(String status);

    List<Tenant> findByDayStatus(DayStatus dayStatus);
}
