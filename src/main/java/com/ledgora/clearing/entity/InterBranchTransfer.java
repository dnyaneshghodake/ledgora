package com.ledgora.clearing.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.InterBranchTransferStatus;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.transaction.entity.Transaction;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Inter-Branch Clearing Transfer record.
 *
 * RBI Requirement: Each branch must independently balance its books.
 * When funds move between branches, the system must:
 *   1. Post Branch A leg: DR Customer A, CR IBC_OUT_A (Branch A balanced)
 *   2. Post Branch B leg: DR IBC_IN_B, CR Customer B (Branch B balanced)
 *   3. Settlement: DR IBC_IN, CR IBC_OUT (clearing zeroed)
 *
 * This entity tracks the lifecycle of each inter-branch movement.
 */
@Entity
@Table(name = "inter_branch_transfers", indexes = {
    @Index(name = "idx_ibt_tenant_date", columnList = "tenant_id, business_date"),
    @Index(name = "idx_ibt_status", columnList = "status"),
    @Index(name = "idx_ibt_from_branch", columnList = "from_branch_id"),
    @Index(name = "idx_ibt_to_branch", columnList = "to_branch_id")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InterBranchTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_branch_id", nullable = false)
    private Branch fromBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_branch_id", nullable = false)
    private Branch toBranch;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private InterBranchTransferStatus status = InterBranchTransferStatus.INITIATED;

    /** The originating transaction that triggered this inter-branch transfer. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_transaction_id")
    private Transaction referenceTransaction;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    /** Narration for audit trail. */
    @Column(name = "narration", length = 500)
    private String narration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

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
