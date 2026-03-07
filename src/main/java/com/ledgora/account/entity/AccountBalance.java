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
