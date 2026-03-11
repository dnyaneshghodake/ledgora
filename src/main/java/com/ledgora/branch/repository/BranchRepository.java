package com.ledgora.branch.repository;

import com.ledgora.branch.entity.Branch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByBranchCode(String branchCode);

    Boolean existsByBranchCode(String branchCode);

    @Query("SELECT b FROM Branch b WHERE b.tenant.id = :tenantId AND b.isActive = true")
    List<Branch> findActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT b FROM Branch b WHERE b.tenant.id = :tenantId")
    List<Branch> findByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT b FROM Branch b WHERE b.tenant.id = :tenantId AND b.branchCode = :branchCode")
    Optional<Branch> findByTenantIdAndBranchCode(
            @Param("tenantId") Long tenantId, @Param("branchCode") String branchCode);
}
