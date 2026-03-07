package com.ledgora.gl.repository;

import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.common.enums.GLAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeneralLedgerRepository extends JpaRepository<GeneralLedger, Long> {
    Optional<GeneralLedger> findByGlCode(String glCode);
    Boolean existsByGlCode(String glCode);
    List<GeneralLedger> findByAccountType(GLAccountType accountType);
    List<GeneralLedger> findByParentIsNull();
    List<GeneralLedger> findByParentId(Long parentId);
    List<GeneralLedger> findByLevel(Integer level);
    List<GeneralLedger> findByIsActive(Boolean isActive);

    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.parent IS NULL AND gl.isActive = true ORDER BY gl.glCode")
    List<GeneralLedger> findRootAccounts();

    @Query("SELECT gl FROM GeneralLedger gl WHERE gl.parent.id = :parentId AND gl.isActive = true ORDER BY gl.glCode")
    List<GeneralLedger> findActiveChildren(@Param("parentId") Long parentId);
}
