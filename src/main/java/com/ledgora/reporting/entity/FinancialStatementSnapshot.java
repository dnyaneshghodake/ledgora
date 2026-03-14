package com.ledgora.reporting.entity;

import com.ledgora.reporting.enums.SnapshotStatus;
import com.ledgora.reporting.enums.StatementType;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Immutable point-in-time financial statement snapshot.
 *
 * <p>RBI Master Directions on Financial Statements — each snapshot captures the complete Balance
 * Sheet or P&L as a JSON payload with a SHA-256 hash checksum for tamper detection.
 *
 * <p>Lifecycle: DRAFT → FINAL. Once FINAL, the snapshot is immutable — any restatement requires a
 * new snapshot with audit justification.
 *
 * <p>Generated during EOD after DATE_ADVANCED phase. Must survive crash recovery — if EOD crashes
 * after statement generation but before commit, the DRAFT snapshot is regenerated on resume.
 *
 * <p>The jsonPayload contains the full statement structure:
 *
 * <pre>
 * {
 *   "statementDate": "2026-03-14",
 *   "sections": [
 *     { "section": "ASSET", "lines": [
 *       { "lineName": "Cash and Balances", "glCode": "1100", "amount": 5140000.00 },
 *       ...
 *     ], "sectionTotal": 7140000.00 },
 *     ...
 *   ],
 *   "totalAssets": 7140000.00,
 *   "totalLiabilities": 5140000.00,
 *   "totalEquity": 2000000.00,
 *   "balanceCheck": true
 * }
 * </pre>
 */
@Entity
@Table(
        name = "financial_statement_snapshots",
        indexes = {
            @Index(
                    name = "idx_fss_tenant_date_type",
                    columnList = "tenant_id, business_date, statement_type"),
            @Index(name = "idx_fss_status", columnList = "status")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_fss_tenant_date_type_status",
                    columnNames = {"tenant_id", "business_date", "statement_type", "status"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatementSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", length = 20, nullable = false)
    private StatementType statementType;

    /** Full statement as JSON (see class Javadoc for structure). */
    @Column(name = "json_payload", columnDefinition = "TEXT", nullable = false)
    private String jsonPayload;

    /**
     * SHA-256 hash of jsonPayload for tamper detection. Computed at generation time. Any
     * modification to jsonPayload will produce a different hash — RBI audit trail integrity.
     */
    @Column(name = "hash_checksum", length = 64, nullable = false)
    private String hashChecksum;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    /** Username or system identity that triggered generation. */
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
