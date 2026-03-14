package com.ledgora.reporting.repository;

import com.ledgora.reporting.entity.StatementLineMapping;
import com.ledgora.reporting.enums.StatementType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StatementLineMappingRepository
        extends JpaRepository<StatementLineMapping, Long> {

    List<StatementLineMapping> findByTenantIdAndStatementTypeOrderByDisplayOrder(
            Long tenantId, StatementType statementType);

    @Query(
            "SELECT m FROM StatementLineMapping m "
                    + "JOIN FETCH m.gl "
                    + "WHERE m.tenant.id = :tenantId AND m.statementType = :type "
                    + "ORDER BY m.section, m.displayOrder")
    List<StatementLineMapping> findWithGlByTenantAndType(
            @Param("tenantId") Long tenantId, @Param("type") StatementType type);
}
