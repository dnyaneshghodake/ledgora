package com.ledgora.teller.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.TellerStatus;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Teller Session — tracks a single day's cash position for a teller. Only one OPEN session per
 * teller per business date. Immutable after CLOSED state. All monetary fields use precision 19,
 * scale 4 for CBS-grade accuracy.
 *
 * <p>Rules: - Cannot transact unless state = OPEN - Cannot close if reconciliation mismatch
 * unresolved - Session is date-bound (one per teller per business day)
 */
@Entity
@Table(
        name = "teller_sessions",
        indexes = {
            @Index(name = "idx_teller_session_teller", columnList = "teller_id"),
            @Index(name = "idx_teller_session_branch", columnList = "branch_id"),
            @Index(name = "idx_teller_session_tenant", columnList = "tenant_id"),
            @Index(name = "idx_teller_session_date", columnList = "business_date"),
            @Index(
                    name = "idx_teller_session_unique",
                    columnList = "teller_id, business_date",
                    unique = true)
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TellerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teller_id", nullable = false)
    private TellerMaster teller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "opening_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "current_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "total_credit_today", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalCreditToday = BigDecimal.ZERO;

    @Column(name = "total_debit_today", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalDebitToday = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20, nullable = false)
    @Builder.Default
    private TellerStatus state = TellerStatus.OPEN_REQUESTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by")
    private User openedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorized_by")
    private User authorizedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Version
    @Column(name = "version")
    private Long version;

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
}
