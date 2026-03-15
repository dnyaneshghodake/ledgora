package com.ledgora.reporting.repository;

import com.ledgora.reporting.entity.FinancialStatementSnapshot;
import com.ledgora.reporting.enums.SnapshotStatus;
import com.ledgora.reporting.enums.StatementType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialStatementSnapshotRepository
        extends JpaRepository<FinancialStatementSnapshot, Long> {

    Optional<FinancialStatementSnapshot> findByTenantIdAndBusinessDateAndStatementTypeAndStatus(
            Long tenantId,
            LocalDate businessDate,
            StatementType statementType,
            SnapshotStatus status);

    Optional<FinancialStatementSnapshot> findByTenantIdAndBusinessDateAndStatementType(
            Long tenantId, LocalDate businessDate, StatementType statementType);

    boolean existsByTenantIdAndBusinessDateAndStatementTypeAndStatus(
            Long tenantId,
            LocalDate businessDate,
            StatementType statementType,
            SnapshotStatus status);
}
