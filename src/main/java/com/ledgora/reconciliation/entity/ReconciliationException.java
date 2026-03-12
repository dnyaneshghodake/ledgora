package com.ledgora.reconciliation.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade Reconciliation Exception log. Records mismatches between the cached account.balance and
 * the true ledger SUM(credits) - SUM(debits). Each row is immutable — represents a point-in-time
 * observation. Auto-correction actions are recorded in the same row for full audit trail.
 */
@Entity
@Table(
        name = "reconciliation_exceptions",
        indexes = {
            @Index(name = "idx_recon_tenant_date", columnList = "tenant_id, business_date"),
            @Index(name = "idx_recon_account", columnList = "account_id"),
            @Index(name = "idx_recon_resolved", columnList = "resolved")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationException {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "account_number", length = 20, nullable = false)
    private String accountNumber;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    /** Cached balance on account.balance field. */
    @Column(name = "cached_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal cachedBalance;

    /** True balance computed from SUM of ledger entries. */
    @Column(name = "ledger_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal ledgerBalance;

    /** Absolute mismatch amount. */
    @Column(name = "mismatch_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal mismatchAmount;

    /** Whether auto-correction was applied. */
    @Column(name = "auto_corrected", nullable = false)
    @Builder.Default
    private Boolean autoCorrected = false;

    /** Whether this exception has been resolved (auto or manual). */
    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }
}
