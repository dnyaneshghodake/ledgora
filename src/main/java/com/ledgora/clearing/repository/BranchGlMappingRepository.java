package com.ledgora.clearing.repository;

import com.ledgora.clearing.entity.BranchGlMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchGlMappingRepository extends JpaRepository<BranchGlMapping, Long> {

    Optional<BranchGlMapping> findByTenantIdAndBranchIdAndIsActiveTrue(
            @Param("tenantId") Long tenantId, @Param("branchId") Long branchId);

    List<BranchGlMapping> findByTenantIdAndIsActiveTrue(@Param("tenantId") Long tenantId);

    boolean existsByTenantIdAndBranchIdAndIsActiveTrue(Long tenantId, Long branchId);
}
