package com.ledgora.loan.repository;

import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.enums.LoanStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {

    Optional<LoanAccount> findByLoanAccountNumber(String loanAccountNumber);

    List<LoanAccount> findByTenantIdAndStatus(Long tenantId, LoanStatus status);

    /** All active (performing) loans for a tenant — used by EOD accrual/DPD. */
    @Query(
            "SELECT la FROM LoanAccount la "
                    + "WHERE la.tenant.id = :tenantId AND la.status = 'ACTIVE'")
    List<LoanAccount> findActiveByTenantId(@Param("tenantId") Long tenantId);

    /** All active + NPA loans for provisioning calculation. */
    @Query(
            "SELECT la FROM LoanAccount la "
                    + "WHERE la.tenant.id = :tenantId AND la.status IN ('ACTIVE', 'NPA')")
    List<LoanAccount> findActiveAndNpaByTenantId(@Param("tenantId") Long tenantId);

    List<LoanAccount> findByLinkedAccountId(Long linkedAccountId);

    /**
     * Loan detail with eager-fetched product and linked account — used by UI detail view to avoid
     * LazyInitializationException when open-in-view=false.
     */
    @Query(
            "SELECT la FROM LoanAccount la "
                    + "JOIN FETCH la.loanProduct "
                    + "JOIN FETCH la.linkedAccount "
                    + "WHERE la.id = :id")
    Optional<LoanAccount> findByIdWithProductAndAccount(@Param("id") Long id);
}
