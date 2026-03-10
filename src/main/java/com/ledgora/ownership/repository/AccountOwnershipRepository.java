package com.ledgora.ownership.repository;

import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.common.enums.OwnershipType;
import com.ledgora.ownership.entity.AccountOwnership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountOwnershipRepository extends JpaRepository<AccountOwnership, Long> {

    @Query(
            "SELECT ao FROM AccountOwnership ao WHERE ao.account.id = :accountId AND ao.tenant.id = :tenantId AND ao.approvalStatus = 'APPROVED'")
    List<AccountOwnership> findApprovedByAccountIdAndTenantId(
            @Param("accountId") Long accountId, @Param("tenantId") Long tenantId);

    @Query(
            "SELECT ao FROM AccountOwnership ao WHERE ao.customerMaster.id = :customerMasterId AND ao.tenant.id = :tenantId AND ao.approvalStatus = 'APPROVED'")
    List<AccountOwnership> findApprovedByCustomerMasterIdAndTenantId(
            @Param("customerMasterId") Long customerMasterId, @Param("tenantId") Long tenantId);

    @Query(
            "SELECT ao FROM AccountOwnership ao WHERE ao.account.id = :accountId AND ao.ownershipType = :type AND ao.approvalStatus = 'APPROVED'")
    List<AccountOwnership> findApprovedByAccountIdAndOwnershipType(
            @Param("accountId") Long accountId, @Param("type") OwnershipType type);

    @Query(
            "SELECT ao FROM AccountOwnership ao WHERE ao.tenant.id = :tenantId AND ao.approvalStatus = :status")
    List<AccountOwnership> findByTenantIdAndApprovalStatus(
            @Param("tenantId") Long tenantId, @Param("status") MakerCheckerStatus status);

    @Query(
            "SELECT ao FROM AccountOwnership ao WHERE ao.account.id = :accountId AND ao.customerMaster.id = :customerMasterId AND ao.approvalStatus = 'APPROVED'")
    Optional<AccountOwnership> findApprovedByAccountIdAndCustomerMasterId(
            @Param("accountId") Long accountId, @Param("customerMasterId") Long customerMasterId);

    @Query(
            "SELECT COALESCE(SUM(ao.ownershipPercentage), 0) FROM AccountOwnership ao WHERE ao.account.id = :accountId AND ao.approvalStatus = 'APPROVED'")
    java.math.BigDecimal sumApprovedOwnershipPercentageByAccountId(
            @Param("accountId") Long accountId);
}
