package com.ledgora.teller.entity;

import com.ledgora.common.enums.CashDifferenceType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Cash Difference Log — records short/excess detected during teller closure reconciliation.
 * Immutable after creation. Supervisor must resolve before session can close.
 *
 * <p>Accounting: SHORT → DR: Cash Short GL (5200), CR: Branch Cash GL (1100) EXCESS → DR: Branch
 * Cash GL (1100), CR: Cash Excess GL (4300)
 */
@Entity
@Table(
        name = "cash_difference_logs",
        indexes = {
            @Index(name = "idx_cash_diff_session", columnList = "session_id"),
            @Index(name = "idx_cash_diff_resolved", columnList = "resolved_flag")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashDifferenceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TellerSession session;

    /** Amount declared by teller during physical cash count. */
    @Column(name = "declared_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal declaredAmount;

    /** System-computed balance from TellerSession.currentBalance. */
    @Column(name = "system_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal systemAmount;

    /** Absolute difference: |declared - system|. */
    @Column(name = "difference", precision = 19, scale = 4, nullable = false)
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private CashDifferenceType type;

    /** Supervisor must set to true before session can transition to CLOSED. */
    @Column(name = "resolved_flag", nullable = false)
    @Builder.Default
    private Boolean resolvedFlag = false;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolution_remarks", length = 500)
    private String resolutionRemarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
