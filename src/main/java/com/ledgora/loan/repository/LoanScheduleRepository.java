package com.ledgora.loan.repository;

import com.ledgora.common.enums.InstallmentStatus;
import com.ledgora.loan.entity.LoanSchedule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Loan Schedule Repository — Finacle LACHST (Loan Account History) data access.
 *
 * <p>Provides installment-level queries for:
 *
 * <ul>
 *   <li>Detail view — full schedule ordered by installment number
 *   <li>DPD calculation — oldest overdue installment for NPA classification
 *   <li>EMI payment — oldest pending/overdue installment for payment matching
 * </ul>
 */
@Repository
public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, Long> {

    List<LoanSchedule> findByAccountIdOrderByInstallmentNumberAsc(Long accountId);

    @Query(
            "SELECT ls FROM LoanSchedule ls WHERE ls.account.id = :accountId AND ls.status = :status ORDER BY ls.installmentNumber")
    List<LoanSchedule> findByAccountIdAndStatus(
            @Param("accountId") Long accountId, @Param("status") InstallmentStatus status);

    /** Find overdue installments across all accounts for a tenant (for DPD/NPA processing). */
    @Query(
            "SELECT ls FROM LoanSchedule ls WHERE ls.account.tenant.id = :tenantId AND ls.status = 'DUE' AND ls.dueDate < :today")
    List<LoanSchedule> findOverdueByTenantId(
            @Param("tenantId") Long tenantId, @Param("today") LocalDate today);

    /** Count installments with DPD > threshold for NPA classification. */
    @Query(
            "SELECT COUNT(ls) FROM LoanSchedule ls WHERE ls.account.id = :accountId AND ls.dpdDays > :threshold")
    long countByAccountIdAndDpdGreaterThan(
            @Param("accountId") Long accountId, @Param("threshold") int threshold);

    boolean existsByAccountId(Long accountId);

    // ── Loan-account-scoped queries (GAP 4 — Finacle LACHST per-loan schedule) ──

    /** Full schedule for a loan — ordered by installment number (detail view). */
    List<LoanSchedule> findByLoanAccountIdOrderByInstallmentNumberAsc(Long loanAccountId);

    /** Overdue installments for a loan — oldest first for DPD computation per RBI IRAC. */
    @Query(
            "SELECT ls FROM LoanSchedule ls "
                    + "WHERE ls.loanAccount.id = :loanAccountId "
                    + "AND ls.status = 'OVERDUE' "
                    + "ORDER BY ls.dueDate ASC")
    List<LoanSchedule> findOverdueByLoanAccountIdOrderByDueDateAsc(
            @Param("loanAccountId") Long loanAccountId);

    /** Oldest pending/overdue installment — used for EMI payment matching (FIFO). */
    @Query(
            "SELECT ls FROM LoanSchedule ls "
                    + "WHERE ls.loanAccount.id = :loanAccountId "
                    + "AND ls.status IN ('SCHEDULED', 'DUE', 'OVERDUE', 'PARTIALLY_PAID') "
                    + "ORDER BY ls.installmentNumber ASC")
    List<LoanSchedule> findPendingByLoanAccountIdOrderByInstallmentAsc(
            @Param("loanAccountId") Long loanAccountId);
}
