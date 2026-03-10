package com.ledgora.eod.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Crash-safe EOD state machine — tracks the progress of each EOD execution.
 *
 * <p>RBI IT Framework — Business Continuity / Operational Resilience:
 *
 * <ul>
 *   <li>Each phase commits independently — crash between phases loses no completed work
 *   <li>On application restart, incomplete EOD is detected and resumed from last phase
 *   <li>Double execution prevented via unique constraint (tenant_id, business_date)
 *   <li>Stuck detection: if same phase for > 30 minutes, raise alert
 * </ul>
 *
 * <p>Phase progression: VALIDATED → DAY_CLOSING → BATCH_CLOSED → SETTLED → DATE_ADVANCED
 */
@Entity
@Table(
        name = "eod_processes",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_eod_tenant_date",
                    columnNames = {"tenant_id", "business_date"})
        },
        indexes = {
            @Index(name = "idx_eod_tenant", columnList = "tenant_id"),
            @Index(name = "idx_eod_status", columnList = "status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EodProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    /** Current phase of the EOD process. */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 30, nullable = false)
    private EodPhase phase;

    /** RUNNING, COMPLETED, FAILED */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "RUNNING";

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    /** EOD phases in strict progression order. */
    public enum EodPhase {
        VALIDATED,
        DAY_CLOSING,
        BATCH_CLOSED,
        SETTLED,
        DATE_ADVANCED
    }
}
