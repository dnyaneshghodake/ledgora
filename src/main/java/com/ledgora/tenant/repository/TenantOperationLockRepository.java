package com.ledgora.tenant.repository;

import com.ledgora.tenant.entity.TenantOperationLock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantOperationLockRepository extends JpaRepository<TenantOperationLock, Long> {

    Optional<TenantOperationLock> findByTenantId(Long tenantId);

    /**
     * Acquire the lock row with PESSIMISTIC_WRITE (SELECT ... FOR UPDATE). Blocks if another
     * transaction already holds the lock on this tenant's row.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tol FROM TenantOperationLock tol WHERE tol.tenantId = :tenantId")
    Optional<TenantOperationLock> findByTenantIdWithLock(@Param("tenantId") Long tenantId);
}
