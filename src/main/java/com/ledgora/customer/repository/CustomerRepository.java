package com.ledgora.customer.repository;

import com.ledgora.customer.entity.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByNationalId(String nationalId);

    Optional<Customer> findByEmail(String email);

    List<Customer> findByKycStatus(String kycStatus);

    @Query(
            "SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> searchByName(@Param("name") String name);

    boolean existsByNationalId(String nationalId);

    boolean existsByEmail(String email);

    // Tenant-isolated queries

    /** Tenant-isolated lookup by primary key. Use instead of findById() for CBS operations. */
    @Query("SELECT c FROM Customer c WHERE c.customerId = :id AND c.tenant.id = :tenantId")
    Optional<Customer> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId")
    List<Customer> findByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND c.kycStatus = :kycStatus")
    List<Customer> findByTenantIdAndKycStatus(
            @Param("tenantId") Long tenantId, @Param("kycStatus") String kycStatus);

    @Query(
            "SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Customer> searchByTenantIdAndName(
            @Param("tenantId") Long tenantId, @Param("name") String name);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") Long tenantId);

    /** Paginated customer list by tenant. */
    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId")
    org.springframework.data.domain.Page<Customer> findByTenantId(
            @Param("tenantId") Long tenantId, org.springframework.data.domain.Pageable pageable);

    /**
     * Find customers by approval status (for pending-approval queue and checker dashboard).
     * Tenant-isolated.
     */
    @Query(
            "SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND c.approvalStatus = :approvalStatus")
    List<Customer> findByTenantIdAndApprovalStatus(
            @Param("tenantId") Long tenantId,
            @Param("approvalStatus") com.ledgora.common.enums.MakerCheckerStatus approvalStatus);

    /** Count customers pending approval for dashboard badge. Tenant-isolated. */
    @Query(
            "SELECT COUNT(c) FROM Customer c WHERE c.tenant.id = :tenantId AND c.approvalStatus = :approvalStatus")
    long countByTenantIdAndApprovalStatus(
            @Param("tenantId") Long tenantId,
            @Param("approvalStatus") com.ledgora.common.enums.MakerCheckerStatus approvalStatus);
}
