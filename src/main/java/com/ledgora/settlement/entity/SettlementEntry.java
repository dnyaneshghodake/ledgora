package com.ledgora.settlement.entity;

import com.ledgora.account.entity.Account;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "settlement_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "opening_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "total_debits", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(name = "total_credits", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "closing_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal closingBalance;
}
