package com.ledgora.fraud.repository;

import com.ledgora.fraud.entity.VelocityLimit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VelocityLimitRepository extends JpaRepository<VelocityLimit, Long> {

    /** Find account-specific velocity limit. */
    Optional<VelocityLimit> findByTenantIdAndAccountIdAndIsActiveTrue(
            Long tenantId, Long accountId);

    /** Find tenant-wide default velocity limit (accountId = null). */
    Optional<VelocityLimit> findByTenantIdAndAccountIdIsNullAndIsActiveTrue(Long tenantId);
}
