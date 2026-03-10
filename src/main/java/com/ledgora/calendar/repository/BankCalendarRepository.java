package com.ledgora.calendar.repository;

import com.ledgora.calendar.entity.BankCalendar;
import com.ledgora.common.enums.MakerCheckerStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BankCalendarRepository extends JpaRepository<BankCalendar, Long> {

    @Query(
            "SELECT bc FROM BankCalendar bc WHERE bc.tenant.id = :tenantId AND bc.calendarDate = :date AND bc.approvalStatus = 'APPROVED'")
    Optional<BankCalendar> findApprovedByTenantIdAndDate(
            @Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query(
            "SELECT bc FROM BankCalendar bc WHERE bc.tenant.id = :tenantId AND bc.calendarDate = :date")
    Optional<BankCalendar> findByTenantIdAndDate(
            @Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query(
            "SELECT bc FROM BankCalendar bc WHERE bc.tenant.id = :tenantId AND bc.calendarDate BETWEEN :startDate AND :endDate AND bc.approvalStatus = 'APPROVED' ORDER BY bc.calendarDate")
    List<BankCalendar> findApprovedByTenantIdAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(
            "SELECT bc FROM BankCalendar bc WHERE bc.tenant.id = :tenantId AND bc.approvalStatus = :status ORDER BY bc.calendarDate")
    List<BankCalendar> findByTenantIdAndApprovalStatus(
            @Param("tenantId") Long tenantId, @Param("status") MakerCheckerStatus status);

    @Query("SELECT bc FROM BankCalendar bc WHERE bc.tenant.id = :tenantId ORDER BY bc.calendarDate")
    List<BankCalendar> findByTenantId(@Param("tenantId") Long tenantId);

    @Query(
            "SELECT bc FROM BankCalendar bc WHERE bc.tenant.id = :tenantId AND bc.dayType = 'HOLIDAY' AND bc.approvalStatus = 'APPROVED' AND bc.calendarDate >= :fromDate ORDER BY bc.calendarDate")
    List<BankCalendar> findUpcomingHolidays(
            @Param("tenantId") Long tenantId, @Param("fromDate") LocalDate fromDate);
}
