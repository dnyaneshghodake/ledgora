package com.ledgora.transaction.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.auth.entity.User;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.enums.TransactionStatus;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Transaction entity - multi-tenant aware with batch support. Composite index on
 * (client_reference_id, channel, tenant_id) for tenant-aware idempotency.
 */
@Entity
@Table(
        name = "transactions",
        indexes = {
            @Index(name = "idx_transaction_ref", columnList = "transaction_ref"),
            @Index(
                    name = "idx_txn_client_ref_channel_tenant",
                    columnList = "client_reference_id, channel, tenant_id"),
            @Index(name = "idx_txn_tenant", columnList = "tenant_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private TransactionBatch batch;

    /** CBS: Transaction must carry its own branch for branch-level reporting and EOD. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "transaction_ref", length = 30, nullable = false, unique = true)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20, nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    /** CBS sub-type for granular classification (e.g., CASH, NEFT, RTGS, UPI). */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_sub_type", length = 20)
    private com.ledgora.common.enums.TransactionSubType transactionSubType;

    /** Risk score assigned by VelocityFraudEngine. 0 = low risk, 100 = high risk. */
    @Column(name = "risk_score")
    private Integer riskScore;

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

    /** CBS value date — the date on which the transaction takes financial effect. Always a date. */
    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "business_date")
    private LocalDate businessDate;

    /** Quick filter flag: true if this transaction is a reversal of another. */
    @Column(name = "is_reversal", nullable = false)
    @Builder.Default
    private Boolean isReversal = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_transaction_id")
    private Transaction reversalOf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    // Maker-checker fields for transaction approval workflow
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maker_id")
    private User maker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checker_id")
    private User checker;

    @Column(name = "maker_timestamp")
    private LocalDateTime makerTimestamp;

    @Column(name = "checker_timestamp")
    private LocalDateTime checkerTimestamp;

    @Column(name = "checker_remarks", length = 500)
    private String checkerRemarks;

    /**
     * Optimistic locking version — prevents concurrent approval of the same transaction from
     * multiple sessions. If two checkers attempt to approve simultaneously, the second write
     * throws OptimisticLockException, preventing double-posting.
     */
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
        if (valueDate == null) {
            valueDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
