package com.ledgora.customer.repository;

import com.ledgora.customer.entity.CustomerFreezeControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerFreezeControlRepository extends JpaRepository<CustomerFreezeControl, Long> {

    @Query("SELECT cfc FROM CustomerFreezeControl cfc WHERE cfc.customerMaster.id = :customerMasterId AND cfc.tenant.id = :tenantId")
    Optional<CustomerFreezeControl> findByCustomerMasterIdAndTenantId(@Param("customerMasterId") Long customerMasterId,
                                                                       @Param("tenantId") Long tenantId);
}
