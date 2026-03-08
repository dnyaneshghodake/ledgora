package com.ledgora.calendar.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Banking Calendar entity for RBI-grade calendar discipline.
 * Each date per tenant is classified as WORKING_DAY or HOLIDAY.
 * Calendar changes require maker-checker approval.
 * No backdated edits after EOD.
 */
@Entity
@Table(name = "bank_calendar", indexes = {
    @Index(name = "idx_bc_tenant_date", columnList = "tenant_id, calendar_date", unique = true),
    @Index(name = "idx_bc_tenant", columnList = "tenant_id"),
    @Index(name = "idx_bc_date", columnList = "calendar_date")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BankCalendar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "calendar_date", nullable = false)
    private LocalDate calendarDate;

    @Column(name = "day_type", length = 20, nullable = false)
    @Builder.Default
    private String dayType = "WORKING_DAY";

    @Column(name = "holiday_name", length = 100)
    private String holidayName;

    @Column(name = "holiday_type", length = 30)
    private String holidayType;

    @Column(name = "atm_allowed", nullable = false)
    @Builder.Default
    private Boolean atmAllowed = false;

    @Column(name = "system_transactions_allowed", nullable = false)
    @Builder.Default
    private Boolean systemTransactionsAllowed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20, nullable = false)
    @Builder.Default
    private MakerCheckerStatus approvalStatus = MakerCheckerStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isHoliday() {
        return "HOLIDAY".equals(dayType);
    }

    public boolean isWorkingDay() {
        return "WORKING_DAY".equals(dayType);
    }
}
