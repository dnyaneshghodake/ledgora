package com.ledgora.teller.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.TellerStatus;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Finacle-grade Teller Master — defines a teller position at a branch with RBI-compliant cash
 * limits. Each teller is assigned to exactly one branch and one user. Limits are configurable per
 * teller to support differentiated authority levels.
 */
@Entity
@Table(
        name = "teller_masters",
        indexes = {
            @Index(name = "idx_teller_master_branch", columnList = "branch_id"),
            @Index(name = "idx_teller_master_user", columnList = "user_id"),
            @Index(name = "idx_teller_master_tenant", columnList = "tenant_id"),
            @Index(
                    name = "idx_teller_master_branch_user",
                    columnList = "branch_id, user_id",
                    unique = true)
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TellerMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private TellerStatus status = TellerStatus.ASSIGNED;

    /** Maximum single deposit transaction amount (default ₹2,00,000). */
    @Column(name = "single_txn_limit_deposit", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal singleTxnLimitDeposit = new BigDecimal("200000.0000");

    /** Maximum single withdrawal transaction amount (default ₹50,000). */
    @Column(name = "single_txn_limit_withdrawal", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal singleTxnLimitWithdrawal = new BigDecimal("50000.0000");

    /** Maximum total daily transaction volume (default ₹5,00,000). */
    @Column(name = "daily_txn_limit", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal dailyTxnLimit = new BigDecimal("500000.0000");

    /** Maximum cash a teller may hold at any time (default ₹10,00,000). */
    @Column(name = "cash_holding_limit", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal cashHoldingLimit = new BigDecimal("1000000.0000");

    @Column(name = "active_flag", nullable = false)
    @Builder.Default
    private Boolean activeFlag = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

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
