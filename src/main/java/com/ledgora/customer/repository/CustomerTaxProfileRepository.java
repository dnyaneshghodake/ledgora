package com.ledgora.customer.repository;

import com.ledgora.customer.entity.CustomerTaxProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerTaxProfileRepository extends JpaRepository<CustomerTaxProfile, Long> {

    @Query(
            "SELECT ctp FROM CustomerTaxProfile ctp WHERE ctp.customerMaster.id = :customerMasterId AND ctp.tenant.id = :tenantId")
    Optional<CustomerTaxProfile> findByCustomerMasterIdAndTenantId(
            @Param("customerMasterId") Long customerMasterId, @Param("tenantId") Long tenantId);

    @Query(
            "SELECT ctp FROM CustomerTaxProfile ctp WHERE ctp.panNumber = :panNumber AND ctp.tenant.id = :tenantId")
    Optional<CustomerTaxProfile> findByPanNumberAndTenantId(
            @Param("panNumber") String panNumber, @Param("tenantId") Long tenantId);
}
