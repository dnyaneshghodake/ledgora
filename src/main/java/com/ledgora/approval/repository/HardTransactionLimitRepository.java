package com.ledgora.approval.repository;

import com.ledgora.approval.entity.HardTransactionLimit;
import com.ledgora.common.enums.TransactionChannel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HardTransactionLimitRepository
        extends JpaRepository<HardTransactionLimit, Long> {

    /** Find channel-specific hard limit. */
    Optional<HardTransactionLimit> findByTenantIdAndChannelAndIsActiveTrue(
            Long tenantId, TransactionChannel channel);

    /** Find default (channel=null) hard limit for tenant. */
    Optional<HardTransactionLimit> findByTenantIdAndChannelIsNullAndIsActiveTrue(Long tenantId);
}
