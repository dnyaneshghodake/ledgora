package com.ledgora.loan.repository;

import com.ledgora.loan.entity.LoanDisbursement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Loan Disbursement Repository — multi-tranche disbursement tracking.
 *
 * <p>Append-only. Used for disbursement history and audit trail.
 */
@Repository
public interface LoanDisbursementRepository extends JpaRepository<LoanDisbursement, Long> {

    /** All disbursement tranches for a loan (ordered by tranche number). */
    List<LoanDisbursement> findByLoanAccountIdOrderByTrancheNumberAsc(Long loanAccountId);

    /** Count of tranches for a loan (for next tranche number). */
    long countByLoanAccountId(Long loanAccountId);
}
