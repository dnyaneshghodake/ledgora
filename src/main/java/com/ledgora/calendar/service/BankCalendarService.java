package com.ledgora.calendar.service;

import com.ledgora.approval.service.ApprovalService;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.calendar.entity.BankCalendar;
import com.ledgora.calendar.repository.BankCalendarRepository;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Banking Calendar Service for RBI-grade calendar discipline.
 * Enforces WORKING_DAY vs HOLIDAY gating for transactions.
 * Calendar changes require maker-checker approval.
 */
@Service
public class BankCalendarService {

    private static final Logger log = LoggerFactory.getLogger(BankCalendarService.class);
    private final BankCalendarRepository calendarRepository;
    private final TenantService tenantService;
    private final ApprovalService approvalService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public BankCalendarService(BankCalendarRepository calendarRepository,
                                TenantService tenantService,
                                ApprovalService approvalService,
                                AuditService auditService,
                                UserRepository userRepository) {
        this.calendarRepository = calendarRepository;
        this.tenantService = tenantService;
        this.approvalService = approvalService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * Check if a date is a holiday for the given tenant.
     */
    public boolean isHoliday(Long tenantId, LocalDate date) {
        Optional<BankCalendar> calendar = calendarRepository.findApprovedByTenantIdAndDate(tenantId, date);
        return calendar.isPresent() && calendar.get().isHoliday();
    }

    /**
     * Check if a date is a working day for the given tenant.
     * If no calendar entry exists, defaults to working day (weekdays) or holiday (weekends).
     */
    public boolean isWorkingDay(Long tenantId, LocalDate date) {
        Optional<BankCalendar> calendar = calendarRepository.findApprovedByTenantIdAndDate(tenantId, date);
        if (calendar.isPresent()) {
            return calendar.get().isWorkingDay();
        }
        // Default: weekdays are working days, weekends are holidays
        return date.getDayOfWeek().getValue() <= 5;
    }

    /**
     * Validate that a transaction is allowed on the given date.
     * Manual (TELLER) transactions are blocked on holidays.
     * ATM/System transactions are allowed only if configured.
     */
    public void validateTransactionAllowed(Long tenantId, LocalDate date, TransactionChannel channel) {
        if (isWorkingDay(tenantId, date)) {
            return; // Working day - all transactions allowed
        }

        // It's a holiday
        Optional<BankCalendar> calendarOpt = calendarRepository.findApprovedByTenantIdAndDate(tenantId, date);
        BankCalendar calendar = calendarOpt.orElse(null);

        if (channel == TransactionChannel.TELLER) {
            throw new RuntimeException("Manual transactions are blocked on holidays. Date: " + date);
        }

        if (channel == TransactionChannel.ATM) {
            if (calendar == null || !Boolean.TRUE.equals(calendar.getAtmAllowed())) {
                throw new RuntimeException("ATM transactions are not allowed on this holiday. Date: " + date);
            }
        }

        if (channel == TransactionChannel.BATCH) {
            if (calendar == null || !Boolean.TRUE.equals(calendar.getSystemTransactionsAllowed())) {
                throw new RuntimeException("System/batch transactions are not allowed on this holiday. Date: " + date);
            }
        }

        // ONLINE and MOBILE channels follow the same policy as ATM on holidays
        if (channel == TransactionChannel.ONLINE || channel == TransactionChannel.MOBILE) {
            if (calendar == null || !Boolean.TRUE.equals(calendar.getAtmAllowed())) {
                throw new RuntimeException("Digital banking transactions (" + channel + ") are not allowed on this holiday. Date: " + date);
            }
        }
    }

    /**
     * Create a calendar entry (maker step). Requires approval.
     */
    @Transactional
    public BankCalendar createCalendarEntry(Long tenantId, LocalDate date, String dayType,
                                             String holidayName, String holidayType,
                                             boolean atmAllowed, boolean systemTxnAllowed,
                                             String remarks) {
        // No backdated edits after EOD
        LocalDate currentBusinessDate = tenantService.getCurrentBusinessDate(tenantId);
        if (date.isBefore(currentBusinessDate)) {
            throw new RuntimeException("Cannot create calendar entry for past date: " + date
                    + ". Current business date: " + currentBusinessDate);
        }

        Tenant tenant = tenantService.getTenantById(tenantId);
        User currentUser = getCurrentUser();

        BankCalendar calendar = BankCalendar.builder()
                .tenant(tenant)
                .calendarDate(date)
                .dayType(dayType)
                .holidayName(holidayName)
                .holidayType(holidayType)
                .atmAllowed(atmAllowed)
                .systemTransactionsAllowed(systemTxnAllowed)
                .approvalStatus(MakerCheckerStatus.PENDING)
                .createdBy(currentUser)
                .remarks(remarks)
                .build();

        BankCalendar saved = calendarRepository.save(calendar);

        // Submit for maker-checker approval
        approvalService.submitForApproval("BANK_CALENDAR", saved.getId(),
                "Calendar entry: " + date + " -> " + dayType);

        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(userId, "CALENDAR_CREATE", "BANK_CALENDAR", saved.getId(),
                "Calendar entry created: " + date + " type=" + dayType, null);

        log.info("Calendar entry created (PENDING): date={}, type={}, tenant={}", date, dayType, tenantId);
        return saved;
    }

    /**
     * Approve a calendar entry (checker step).
     */
    @Transactional
    public BankCalendar approveCalendarEntry(Long calendarId) {
        BankCalendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new RuntimeException("Calendar entry not found: " + calendarId));

        if (calendar.getApprovalStatus() != MakerCheckerStatus.PENDING) {
            throw new RuntimeException("Calendar entry is not pending approval");
        }

        User currentUser = getCurrentUser();
        if (calendar.getCreatedBy() != null && currentUser != null
                && calendar.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot approve your own calendar entry (maker-checker violation)");
        }

        calendar.setApprovalStatus(MakerCheckerStatus.APPROVED);
        calendar.setApprovedBy(currentUser);
        BankCalendar saved = calendarRepository.save(calendar);

        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(userId, "CALENDAR_APPROVE", "BANK_CALENDAR", saved.getId(),
                "Calendar entry approved: " + calendar.getCalendarDate(), null);

        log.info("Calendar entry approved: date={}, type={}", calendar.getCalendarDate(), calendar.getDayType());
        return saved;
    }

    /**
     * Reject a calendar entry.
     */
    @Transactional
    public BankCalendar rejectCalendarEntry(Long calendarId, String reason) {
        BankCalendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new RuntimeException("Calendar entry not found: " + calendarId));

        if (calendar.getApprovalStatus() != MakerCheckerStatus.PENDING) {
            throw new RuntimeException("Calendar entry is not pending approval");
        }

        calendar.setApprovalStatus(MakerCheckerStatus.REJECTED);
        calendar.setRemarks(reason);
        BankCalendar saved = calendarRepository.save(calendar);

        User currentUser = getCurrentUser();
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(userId, "CALENDAR_REJECT", "BANK_CALENDAR", saved.getId(),
                "Calendar entry rejected: " + reason, null);

        log.info("Calendar entry rejected: date={}", calendar.getCalendarDate());
        return saved;
    }

    public List<BankCalendar> getCalendarForTenant(Long tenantId) {
        return calendarRepository.findByTenantId(tenantId);
    }

    public List<BankCalendar> getPendingCalendarEntries(Long tenantId) {
        return calendarRepository.findByTenantIdAndApprovalStatus(tenantId, MakerCheckerStatus.PENDING);
    }

    public List<BankCalendar> getUpcomingHolidays(Long tenantId) {
        return calendarRepository.findUpcomingHolidays(tenantId, LocalDate.now());
    }

    public Optional<BankCalendar> getCalendarEntry(Long tenantId, LocalDate date) {
        return calendarRepository.findApprovedByTenantIdAndDate(tenantId, date);
    }

    private User getCurrentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
