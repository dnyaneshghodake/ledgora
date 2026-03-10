package com.ledgora.lien.repository;

import com.ledgora.common.enums.LienStatus;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.lien.entity.AccountLien;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountLienRepository extends JpaRepository<AccountLien, Long> {

    @Query(
            "SELECT al FROM AccountLien al WHERE al.account.id = :accountId AND al.status = 'ACTIVE' AND al.approvalStatus = 'APPROVED'")
    List<AccountLien> findActiveApprovedByAccountId(@Param("accountId") Long accountId);

    @Query(
            "SELECT COALESCE(SUM(al.lienAmount), 0) FROM AccountLien al WHERE al.account.id = :accountId AND al.status = 'ACTIVE' AND al.approvalStatus = 'APPROVED'")
    BigDecimal sumActiveLienAmountByAccountId(@Param("accountId") Long accountId);

    @Query(
            "SELECT al FROM AccountLien al WHERE al.tenant.id = :tenantId AND al.approvalStatus = :status")
    List<AccountLien> findByTenantIdAndApprovalStatus(
            @Param("tenantId") Long tenantId, @Param("status") MakerCheckerStatus status);

    @Query("SELECT al FROM AccountLien al WHERE al.tenant.id = :tenantId AND al.status = :status")
    List<AccountLien> findByTenantIdAndStatus(
            @Param("tenantId") Long tenantId, @Param("status") LienStatus status);

    @Query(
            "SELECT al FROM AccountLien al WHERE al.status = 'ACTIVE' AND al.approvalStatus = 'APPROVED' AND al.endDate IS NOT NULL AND al.endDate < :today")
    List<AccountLien> findExpiredLiens(@Param("today") LocalDate today);

    @Query(
            "SELECT al FROM AccountLien al WHERE al.account.id = :accountId AND al.tenant.id = :tenantId")
    List<AccountLien> findByAccountIdAndTenantId(
            @Param("accountId") Long accountId, @Param("tenantId") Long tenantId);
}
