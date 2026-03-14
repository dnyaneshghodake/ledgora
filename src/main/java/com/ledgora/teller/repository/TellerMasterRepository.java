package com.ledgora.teller.repository;

import com.ledgora.teller.entity.TellerMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TellerMasterRepository extends JpaRepository<TellerMaster, Long> {

    Optional<TellerMaster> findByBranchIdAndUserId(Long branchId, Long userId);

    List<TellerMaster> findByBranchId(Long branchId);

    List<TellerMaster> findByTenantId(Long tenantId);

    Optional<TellerMaster> findByUserIdAndTenantId(Long userId, Long tenantId);

    List<TellerMaster> findByBranchIdAndActiveFlagTrue(Long branchId);
}
