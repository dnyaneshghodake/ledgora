package com.ledgora.deposit.repository;

import com.ledgora.deposit.entity.DepositAccount;
import com.ledgora.deposit.enums.DepositAccountStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositAccountRepository extends JpaRepository<DepositAccount, Long> {

    Optional<DepositAccount> findByDepositAccountNumber(String number);

    List<DepositAccount> findByTenantIdAndStatus(Long tenantId, DepositAccountStatus status);

    @Query("SELECT da FROM DepositAccount da WHERE da.tenant.id = :tenantId AND da.status = 'ACTIVE'")
    List<DepositAccount> findActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT da FROM DepositAccount da WHERE da.tenant.id = :tenantId "
            + "AND da.status = 'ACTIVE' AND da.maturityDate IS NOT NULL "
            + "AND da.maturityDate <= :date")
    List<DepositAccount> findMaturingByDate(
            @Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query("SELECT da FROM DepositAccount da WHERE da.tenant.id = :tenantId "
            + "AND da.status IN ('ACTIVE', 'MATURED')")
    List<DepositAccount> findActiveAndMaturedByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(da.principalAmount), 0) FROM DepositAccount da "
            + "WHERE da.tenant.id = :tenantId AND da.status = 'ACTIVE' "
            + "AND da.depositProduct.depositType IN ('SAVINGS', 'CURRENT')")
    java.math.BigDecimal sumCasaBalanceByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(da.principalAmount), 0) FROM DepositAccount da "
            + "WHERE da.tenant.id = :tenantId AND da.status = 'ACTIVE' "
            + "AND da.depositProduct.depositType = 'FIXED_DEPOSIT'")
    java.math.BigDecimal sumFdBalanceByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(da.principalAmount), 0) FROM DepositAccount da "
            + "WHERE da.tenant.id = :tenantId AND da.status = 'ACTIVE' "
            + "AND da.depositProduct.depositType = 'RECURRING_DEPOSIT'")
    java.math.BigDecimal sumRdBalanceByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(da.interestAccrued), 0) FROM DepositAccount da "
            + "WHERE da.tenant.id = :tenantId AND da.status = 'ACTIVE'")
    java.math.BigDecimal sumInterestPayableByTenantId(@Param("tenantId") Long tenantId);
}
