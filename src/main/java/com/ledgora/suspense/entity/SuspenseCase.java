package com.ledgora.suspense.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.auth.entity.User;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.voucher.entity.Voucher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracks a partial posting failure that was parked to the Suspense GL.
 *
 * <p>CBS Standard: When a credit leg fails after a debit succeeds, the system routes the failed leg
 * to Suspense GL to maintain double-entry balance. This entity tracks the lifecycle of each parked
 * entry until resolution (retry or reversal).
 *
 * <p>Lifecycle: OPEN → RESOLVED (retry succeeded) or REVERSED (debit leg reversed)
 */
@Entity
@Table(
        name = "suspense_cases",
        indexes = {
            @Index(name = "idx_sc_tenant_date", columnList = "tenant_id, business_date"),
            @Index(name = "idx_sc_status", columnList = "status"),
            @Index(name = "idx_sc_transaction", columnList = "original_transaction_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspenseCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** The original transaction that partially failed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transaction_id", nullable = false)
    private Transaction originalTransaction;

    /** The voucher that was successfully posted (debit leg). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_voucher_id")
    private Voucher postedVoucher;

    /** The voucher posted to Suspense GL (replacing the failed credit leg). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspense_voucher_id")
    private Voucher suspenseVoucher;

    /** The account that was the intended target of the failed leg. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intended_account_id", nullable = false)
    private Account intendedAccount;

    /** The suspense account where the failed leg was parked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspense_account_id", nullable = false)
    private Account suspenseAccount;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    /**
     * Reason code for the parking. E.g., ACCOUNT_FROZEN, ACCOUNT_INACTIVE, POSTING_EXCEPTION,
     * TIMEOUT.
     */
    @Column(name = "reason_code", length = 50, nullable = false)
    private String reasonCode;

    /** Detailed reason / exception message. */
    @Column(name = "reason_detail", length = 1000)
    private String reasonDetail;

    /** OPEN, RESOLVED, REVERSED */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    /** The voucher created to resolve the suspense case (retry or correction). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolution_voucher_id")
    private Voucher resolutionVoucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolution_checker_id")
    private User resolutionChecker;

    @Column(name = "resolution_remarks", length = 500)
    private String resolutionRemarks;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

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
