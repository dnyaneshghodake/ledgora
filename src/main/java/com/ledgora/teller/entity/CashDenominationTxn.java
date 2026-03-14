package com.ledgora.teller.entity;

import com.ledgora.transaction.entity.Transaction;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Cash Denomination Transaction — records denomination breakdown for a cash transaction. Immutable
 * after posting. Rule: SUM(denominationValue × count) MUST equal transaction amount.
 */
@Entity
@Table(
        name = "cash_denomination_txns",
        indexes = {
            @Index(name = "idx_denom_txn_transaction", columnList = "transaction_id"),
            @Index(name = "idx_denom_txn_session", columnList = "session_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashDenominationTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TellerSession session;

    /** Face value of the denomination (e.g., 2000, 500). */
    @Column(name = "denomination_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal denominationValue;

    /** Number of notes/coins of this denomination. */
    @Column(name = "count", nullable = false)
    private Integer count;

    /** Computed: denominationValue × count. Stored for query performance. */
    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        totalAmount = denominationValue.multiply(new BigDecimal(count));
    }
}
