package com.ledgora.loan.repository;

import com.ledgora.loan.entity.RepaymentTransaction;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repayment Transaction Repository — immutable payment record access.
 *
 * <p>Append-only. Used for statement generation, interest certificates, and RBI audit.
 */
@Repository
public interface RepaymentTransactionRepository extends JpaRepository<RepaymentTransaction, Long> {

    /** All payments for a loan (newest first). */
    List<RepaymentTransaction> findByLoanAccountIdOrderByPaymentDateDesc(Long loanAccountId);

    /** Payments for a loan within a date range (for statement generation). */
    @Query(
            "SELECT rt FROM RepaymentTransaction rt "
                    + "WHERE rt.loanAccount.id = :loanAccountId "
                    + "AND rt.paymentDate BETWEEN :fromDate AND :toDate "
                    + "ORDER BY rt.paymentDate ASC")
    List<RepaymentTransaction> findByLoanAccountIdAndDateRange(
            @Param("loanAccountId") Long loanAccountId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /** All payments for a tenant on a specific date (for EOD reconciliation). */
    List<RepaymentTransaction> findByTenantIdAndPaymentDate(Long tenantId, LocalDate paymentDate);
}
