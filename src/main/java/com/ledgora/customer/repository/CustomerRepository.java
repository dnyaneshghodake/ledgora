package com.ledgora.customer.repository;

import com.ledgora.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByNationalId(String nationalId);
    Optional<Customer> findByEmail(String email);
    List<Customer> findByKycStatus(String kycStatus);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> searchByName(@Param("name") String name);

    boolean existsByNationalId(String nationalId);
    boolean existsByEmail(String email);

    // Tenant-isolated queries
    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId")
    List<Customer> findByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND c.kycStatus = :kycStatus")
    List<Customer> findByTenantIdAndKycStatus(@Param("tenantId") Long tenantId, @Param("kycStatus") String kycStatus);

    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Customer> searchByTenantIdAndName(@Param("tenantId") Long tenantId, @Param("name") String name);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") Long tenantId);
}
