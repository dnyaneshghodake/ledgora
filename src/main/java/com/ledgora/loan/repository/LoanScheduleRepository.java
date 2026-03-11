package com.ledgora.loan.repository;

import com.ledgora.common.enums.InstallmentStatus;
import com.ledgora.loan.entity.LoanSchedule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, Long> {

    List<LoanSchedule> findByAccountIdOrderByInstallmentNumberAsc(Long accountId);

    @Query("SELECT ls FROM LoanSchedule ls WHERE ls.account.id = :accountId AND ls.status = :status ORDER BY ls.installmentNumber")
    List<LoanSchedule> findByAccountIdAndStatus(
            @Param("accountId") Long accountId, @Param("status") InstallmentStatus status);

    /** Find overdue installments across all accounts for a tenant (for DPD/NPA processing). */
    @Query("SELECT ls FROM LoanSchedule ls WHERE ls.account.tenant.id = :tenantId AND ls.status = 'DUE' AND ls.dueDate < :today")
    List<LoanSchedule> findOverdueByTenantId(
            @Param("tenantId") Long tenantId, @Param("today") LocalDate today);

    /** Count installments with DPD > threshold for NPA classification. */
    @Query("SELECT COUNT(ls) FROM LoanSchedule ls WHERE ls.account.id = :accountId AND ls.dpdDays > :threshold")
    long countByAccountIdAndDpdGreaterThan(
            @Param("accountId") Long accountId, @Param("threshold") int threshold);

    boolean existsByAccountId(Long accountId);
}
