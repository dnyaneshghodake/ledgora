package com.ledgora.loan.repository;

import com.ledgora.loan.entity.ExposureRegister;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Exposure Register Repository — aggregate exposure snapshot queries.
 */
@Repository
public interface ExposureRegisterRepository extends JpaRepository<ExposureRegister, Long> {

    /** Exposure snapshot for a borrower on a specific date. */
    Optional<ExposureRegister> findByTenantIdAndBorrowerIdAndSnapshotDate(
            Long tenantId, String borrowerId, LocalDate snapshotDate);

    /** All exposure snapshots for a tenant on a date (for regulatory reporting). */
    List<ExposureRegister> findByTenantIdAndSnapshotDate(Long tenantId, LocalDate snapshotDate);

    /** All breached exposures for a tenant on a date. */
    List<ExposureRegister> findByTenantIdAndSnapshotDateAndBreachFlagTrue(
            Long tenantId, LocalDate snapshotDate);

    /** Exposure history for a borrower (newest first). */
    List<ExposureRegister> findByTenantIdAndBorrowerIdOrderBySnapshotDateDesc(
            Long tenantId, String borrowerId);
}
