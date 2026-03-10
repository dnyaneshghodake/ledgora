package com.ledgora.gl.repository;

import com.ledgora.gl.entity.GlBranchBalance;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GlBranchBalanceRepository extends JpaRepository<GlBranchBalance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT gbb FROM GlBranchBalance gbb WHERE gbb.tenant.id = :tenantId AND gbb.branch.id = :branchId AND gbb.gl.id = :glId")
    Optional<GlBranchBalance> findByTenantIdAndBranchIdAndGlIdWithLock(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("glId") Long glId);

    @Query(
            "SELECT gbb FROM GlBranchBalance gbb WHERE gbb.tenant.id = :tenantId AND gbb.branch.id = :branchId")
    List<GlBranchBalance> findByTenantIdAndBranchId(
            @Param("tenantId") Long tenantId, @Param("branchId") Long branchId);

    @Query(
            "SELECT COALESCE(SUM(gbb.glActualBalance), 0) FROM GlBranchBalance gbb WHERE gbb.tenant.id = :tenantId AND gbb.branch.id = :branchId")
    BigDecimal sumBalanceByTenantIdAndBranchId(
            @Param("tenantId") Long tenantId, @Param("branchId") Long branchId);
}
