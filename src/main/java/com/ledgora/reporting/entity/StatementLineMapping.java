package com.ledgora.reporting.entity;

import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.reporting.enums.StatementSection;
import com.ledgora.reporting.enums.StatementType;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Maps a General Ledger account to a specific line on a financial statement.
 *
 * <p>RBI Schedule 5 (Balance Sheet) / Schedule 14 (P&L) — each GL must map to exactly one statement
 * line. This mapping drives the Finacle-grade GL-to-statement aggregation engine.
 *
 * <p>Architecture: LedgerEntry → GeneralLedger → StatementLineMapping → Statement output. The
 * AccountBalance table is NEVER used as an accounting source of truth.
 *
 * <p>Example mappings:
 *
 * <ul>
 *   <li>GL 1100 (Cash) → BALANCE_SHEET / ASSET / "Cash and Balances with RBI"
 *   <li>GL 2110 (Savings) → BALANCE_SHEET / LIABILITY / "Demand Deposits"
 *   <li>GL 4100 (Interest Income) → PNL / INCOME / "Interest Earned"
 *   <li>GL 5100 (Interest Expense) → PNL / EXPENSE / "Interest Expended"
 * </ul>
 */
@Entity
@Table(
        name = "statement_line_mappings",
        indexes = {
            @Index(name = "idx_slm_tenant_type", columnList = "tenant_id, statement_type"),
            @Index(name = "idx_slm_gl", columnList = "gl_id")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_slm_gl_type",
                    columnNames = {"gl_id", "statement_type"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementLineMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_id", nullable = false)
    private GeneralLedger gl;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", length = 20, nullable = false)
    private StatementType statementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "section", length = 20, nullable = false)
    private StatementSection section;

    /** Sub-section for RBI schedule grouping (e.g., "Schedule 1 — Capital"). */
    @Column(name = "sub_section", length = 100)
    private String subSection;

    /** Display name on the statement (e.g., "Cash and Balances with RBI"). */
    @Column(name = "line_name", length = 200, nullable = false)
    private String lineName;

    /** Display order within the section (lower = higher on statement). */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
