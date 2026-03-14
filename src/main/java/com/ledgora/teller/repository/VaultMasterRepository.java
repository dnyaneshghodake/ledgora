package com.ledgora.teller.repository;

import com.ledgora.teller.entity.VaultMaster;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VaultMasterRepository extends JpaRepository<VaultMaster, Long> {

    Optional<VaultMaster> findByBranchId(Long branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VaultMaster v WHERE v.branch.id = :branchId")
    Optional<VaultMaster> findByBranchIdWithLock(@Param("branchId") Long branchId);
}
