package com.ledgora.gl.repository;

import com.ledgora.gl.entity.GlTenantBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlTenantBalanceRepository extends JpaRepository<GlTenantBalance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gtb FROM GlTenantBalance gtb WHERE gtb.tenant.id = :tenantId AND gtb.gl.id = :glId")
    Optional<GlTenantBalance> findByTenantIdAndGlIdWithLock(
            @Param("tenantId") Long tenantId,
            @Param("glId") Long glId);

    @Query("SELECT gtb FROM GlTenantBalance gtb WHERE gtb.tenant.id = :tenantId")
    List<GlTenantBalance> findByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(gtb.glActualBalance), 0) FROM GlTenantBalance gtb WHERE gtb.tenant.id = :tenantId")
    BigDecimal sumBalanceByTenantId(@Param("tenantId") Long tenantId);
}
