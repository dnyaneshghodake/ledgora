package com.ledgora.loan.repository;

import com.ledgora.loan.entity.LoanProvision;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Loan Provision Repository — daily provision snapshot access.
 *
 * <p>Append-only per business date. Used for regulatory reporting and RBI audit.
 */
@Repository
public interface LoanProvisionRepository extends JpaRepository<LoanProvision, Long> {

    /** Provision snapshot for a loan on a specific date (unique constraint). */
    Optional<LoanProvision> findByLoanAccountIdAndBusinessDate(
            Long loanAccountId, LocalDate businessDate);

    /** All provision snapshots for a loan (newest first). */
    List<LoanProvision> findByLoanAccountIdOrderByBusinessDateDesc(Long loanAccountId);

    /** All provision snapshots for a tenant on a date (for EOD reporting). */
    List<LoanProvision> findByTenantIdAndBusinessDate(Long tenantId, LocalDate businessDate);
}
