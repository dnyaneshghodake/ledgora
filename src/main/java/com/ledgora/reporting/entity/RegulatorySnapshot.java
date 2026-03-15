package com.ledgora.reporting.entity;

import com.ledgora.reporting.enums.SnapshotStatus;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Immutable regulatory report snapshot — covers Trial Balance, CRAR, and ALM.
 *
 * <p>RBI Supervisory Reporting Framework: Each regulatory report is persisted as an immutable
 * snapshot with SHA-256 checksum for tamper detection. Once FINAL, the snapshot cannot be modified.
 *
 * <p>Snapshot types:
 *
 * <ul>
 *   <li>TRIAL_BALANCE — RBI Schedule 5/14 trial balance
 *   <li>CRAR — Basel III Capital Adequacy Ratio
 *   <li>ALM — Structural Liquidity Statement
 * </ul>
 *
 * <p>Generated during EOD after financial statement snapshots. Must survive crash recovery.
 */
@Entity
@Table(
        name = "regulatory_snapshots",
        indexes = {
            @Index(
                    name = "idx_rs_tenant_date_type",
                    columnList = "tenant_id, business_date, report_type"),
            @Index(name = "idx_rs_status", columnList = "status")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_rs_tenant_date_type",
                    columnNames = {"tenant_id", "business_date", "report_type"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulatorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    /** TRIAL_BALANCE, CRAR, ALM */
    @Column(name = "report_type", length = 20, nullable = false)
    private String reportType;

    /** Full report as JSON. */
    @Column(name = "json_payload", columnDefinition = "TEXT", nullable = false)
    private String jsonPayload;

    /** SHA-256 hash of jsonPayload for tamper detection. */
    @Column(name = "hash_checksum", length = 64, nullable = false)
    private String hashChecksum;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "generated_by", length = 100)
    private String generatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    @Builder.Default
    private SnapshotStatus status = SnapshotStatus.DRAFT;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
