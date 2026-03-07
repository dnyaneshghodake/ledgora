package com.ledgora.account.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_balances")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountBalance {
    @Id
    @Column(name = "account_id")
    private Long accountId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "ledger_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "hold_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal holdAmount = BigDecimal.ZERO;

    // CBS Balance Engine fields
    @Column(name = "actual_total_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal actualTotalBalance = BigDecimal.ZERO;

    @Column(name = "actual_cleared_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal actualClearedBalance = BigDecimal.ZERO;

    @Column(name = "shadow_total_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal shadowTotalBalance = BigDecimal.ZERO;

    @Column(name = "shadow_clearing_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal shadowClearingBalance = BigDecimal.ZERO;

    @Column(name = "inward_clearing_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal inwardClearingBalance = BigDecimal.ZERO;

    @Column(name = "uncleared_effect_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal unclearedEffectBalance = BigDecimal.ZERO;

    @Column(name = "lien_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal lienBalance = BigDecimal.ZERO;

    @Column(name = "charge_hold_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal chargeHoldBalance = BigDecimal.ZERO;

    // OD (overdraft) permitted flag
    @Column(name = "od_permitted", nullable = false)
    @Builder.Default
    private Boolean odPermitted = false;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
