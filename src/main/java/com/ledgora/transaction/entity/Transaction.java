package com.ledgora.transaction.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.enums.TransactionStatus;
import com.ledgora.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PART 13: Performance optimization - index on transaction_ref for fast lookups.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_ref", columnList = "transaction_ref"),
    @Index(name = "idx_txn_client_ref_channel", columnList = "client_reference_id, channel")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", length = 30, nullable = false, unique = true)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20, nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    // PART 2: Transaction channel for channel-aware processing
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private TransactionChannel channel;

    // PART 2: Client reference ID for idempotency
    @Column(name = "client_reference_id", length = 100)
    private String clientReferenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "narration", length = 500)
    private String narration;

    @Column(name = "value_date")
    private LocalDateTime valueDate;

    @Column(name = "business_date")
    private LocalDate businessDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_transaction_id")
    private Transaction reversalOf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (valueDate == null) {
            valueDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
