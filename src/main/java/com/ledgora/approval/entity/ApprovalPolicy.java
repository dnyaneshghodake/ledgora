package com.ledgora.approval.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CBS Approval Policy Table.
 * Configurable rules that determine whether a transaction is auto-authorized
 * or requires maker-checker approval.
 *
 * Decision flow:
 *   1. Match by tenant + transaction_type + channel (most specific)
 *   2. If amount within [min_amount, max_amount] and auto_authorize_flag = true -> auto-post
 *   3. Otherwise -> PENDING_APPROVAL (requires checker)
 *
 * Special rules (always require approval regardless of policy):
 *   - Reversals
 *   - Backdated entries
 *   - Config/master data changes
 *
 * System-only transactions (interest accrual, EOD, charges) bypass this entirely
 * via BATCH/SYSTEM channel with auto_authorize_flag = true.
 */
@Entity
@Table(name = "approval_policies", indexes = {
    @Index(name = "idx_ap_tenant_type_channel", columnList = "tenant_id, transaction_type, channel")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Transaction type: DEPOSIT, WITHDRAWAL, TRANSFER, REVERSAL, INTEREST_ACCRUAL, CHARGE, etc. */
    @Column(name = "transaction_type", length = 30, nullable = false)
    private String transactionType;

    /** Channel: TELLER, ATM, ONLINE, MOBILE, BATCH, or * for all channels */
    @Column(name = "channel", length = 20, nullable = false)
    @Builder.Default
    private String channel = "*";

    /** Minimum amount (inclusive) for this policy to apply. Null = no lower bound. */
    @Column(name = "min_amount", precision = 19, scale = 4)
    private BigDecimal minAmount;

    /** Maximum amount (inclusive) for auto-authorization. Above this -> requires approval. */
    @Column(name = "max_amount", precision = 19, scale = 4)
    private BigDecimal maxAmount;

    /** If true, transactions matching this policy are auto-authorized (posted immediately). */
    @Column(name = "auto_authorize_flag", nullable = false)
    @Builder.Default
    private Boolean autoAuthorizeFlag = false;

    /** If true, checker approval is required for transactions matching this policy. */
    @Column(name = "approval_required_flag", nullable = false)
    @Builder.Default
    private Boolean approvalRequiredFlag = true;

    /** Comma-separated role names allowed to approve (e.g. "ROLE_CHECKER,ROLE_MANAGER"). Null = any checker. */
    @Column(name = "role_allowed", length = 255)
    private String roleAllowed;

    /** Description of this policy rule for audit/admin UI. */
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
