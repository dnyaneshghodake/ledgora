package com.ledgora.gl.repository;

import com.ledgora.common.enums.GLAccountType;
import com.ledgora.gl.entity.GeneralLedger;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneralLedgerRepository extends JpaRepository<GeneralLedger, Long> {
    Optional<GeneralLedger> findByGlCode(String glCode);

    Boolean existsByGlCode(String glCode);

    List<GeneralLedger> findByAccountType(GLAccountType accountType);

    List<GeneralLedger> findByParentIsNull();

    List<GeneralLedger> findByParentId(Long parentId);

    List<GeneralLedger> findByLevel(Integer level);

    List<GeneralLedger> findByIsActive(Boolean isActive);

    /** Tenant-isolated lookup by primary key. Use instead of findById() for CBS operations. */
    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.id = :id AND gl.tenant.id = :tenantId")
    Optional<GeneralLedger> findByIdAndTenantId(
            @Param("id") Long id, @Param("tenantId") Long tenantId);

    /** Tenant-scoped GL list — returns GLs owned by the tenant or shared (null tenant). */
    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.tenant.id = :tenantId OR gl.tenant IS NULL")
    List<GeneralLedger> findByTenantIdOrShared(@Param("tenantId") Long tenantId);

    @Query(
            "SELECT gl FROM GeneralLedger gl WHERE gl.parent IS NULL AND gl.isActive = true ORDER BY gl.glCode")
    List<GeneralLedger> findRootAccounts();

    @Query(
            "SELECT gl FROM GeneralLedger gl WHERE gl.parent.id = :parentId AND gl.isActive = true ORDER BY gl.glCode")
    List<GeneralLedger> findActiveChildren(@Param("parentId") Long parentId);
}
