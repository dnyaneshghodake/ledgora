package com.ledgora.suspense.repository;

import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.suspense.entity.SuspenseGlMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuspenseGlMappingRepository extends JpaRepository<SuspenseGlMapping, Long> {

    /** Find channel-specific suspense mapping. */
    Optional<SuspenseGlMapping> findByTenantIdAndChannelAndIsActiveTrue(
            Long tenantId, TransactionChannel channel);

    /** Find default (channel=null) suspense mapping for tenant. */
    Optional<SuspenseGlMapping> findByTenantIdAndChannelIsNullAndIsActiveTrue(Long tenantId);
}
