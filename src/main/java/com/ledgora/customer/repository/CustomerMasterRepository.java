package com.ledgora.customer.repository;

import com.ledgora.common.enums.CustomerStatus;
import com.ledgora.customer.entity.CustomerMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerMasterRepository extends JpaRepository<CustomerMaster, Long> {

    @Query(
            "SELECT cm FROM CustomerMaster cm WHERE cm.tenant.id = :tenantId AND cm.customerNumber = :customerNumber")
    Optional<CustomerMaster> findByTenantIdAndCustomerNumber(
            @Param("tenantId") Long tenantId, @Param("customerNumber") String customerNumber);

    @Query(
            "SELECT cm FROM CustomerMaster cm WHERE cm.tenant.id = :tenantId AND cm.status = :status")
    List<CustomerMaster> findByTenantIdAndStatus(
            @Param("tenantId") Long tenantId, @Param("status") CustomerStatus status);

    @Query("SELECT cm FROM CustomerMaster cm WHERE cm.tenant.id = :tenantId")
    List<CustomerMaster> findByTenantId(@Param("tenantId") Long tenantId);

    @Query(
            "SELECT cm FROM CustomerMaster cm WHERE cm.tenant.id = :tenantId AND cm.nationalId = :nationalId")
    Optional<CustomerMaster> findByTenantIdAndNationalId(
            @Param("tenantId") Long tenantId, @Param("nationalId") String nationalId);

    @Query(
            "SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END FROM CustomerMaster cm WHERE cm.tenant.id = :tenantId AND cm.customerNumber = :customerNumber")
    boolean existsByTenantIdAndCustomerNumber(
            @Param("tenantId") Long tenantId, @Param("customerNumber") String customerNumber);
}
