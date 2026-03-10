package com.ledgora.customer.repository;

import com.ledgora.customer.entity.CustomerRelationship;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRelationshipRepository extends JpaRepository<CustomerRelationship, Long> {

    @Query(
            "SELECT cr FROM CustomerRelationship cr WHERE cr.primaryCustomer.id = :customerId AND cr.tenant.id = :tenantId AND cr.isActive = true")
    List<CustomerRelationship> findActiveByPrimaryCustomerIdAndTenantId(
            @Param("customerId") Long customerId, @Param("tenantId") Long tenantId);

    @Query(
            "SELECT cr FROM CustomerRelationship cr WHERE cr.relatedCustomer.id = :customerId AND cr.tenant.id = :tenantId AND cr.isActive = true")
    List<CustomerRelationship> findActiveByRelatedCustomerIdAndTenantId(
            @Param("customerId") Long customerId, @Param("tenantId") Long tenantId);
}
