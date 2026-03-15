package com.ledgora.loan.repository;

import com.ledgora.loan.entity.LoanRateChangeHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Loan Rate Change History Repository — immutable audit trail access.
 *
 * <p>Append-only. Used for RBI audit compliance and borrower dispute resolution.
 */
@Repository
public interface LoanRateChangeHistoryRepository
        extends JpaRepository<LoanRateChangeHistory, Long> {

    /** All rate changes for a product (newest first). */
    List<LoanRateChangeHistory> findByLoanProductIdOrderByCreatedAtDesc(Long loanProductId);

    /** All rate changes for a specific loan account (newest first). */
    List<LoanRateChangeHistory> findByLoanAccountIdOrderByCreatedAtDesc(Long loanAccountId);

    /** All rate changes for a tenant (newest first). */
    List<LoanRateChangeHistory> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
