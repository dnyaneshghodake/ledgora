package com.ledgora.balance.entity;

import com.ledgora.tenant.entity.Tenant;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Alert raised when the account balance cache drifts from the authoritative ledger balance.
 *
 * <p>RBI IT Framework — Data Validation / Internal Audit: Account.balance is a performance cache;
 * the ledger (SUM of credits - SUM of debits) is the system of truth. Any mismatch indicates a
 * cache corruption, posting engine defect, or concurrent update anomaly.
 */
@Entity
@Table(
        name = "balance_drift_alerts",
        indexes = {
            @Index(name = "idx_bda_tenant", columnList = "tenant_id"),
            @Index(name = "idx_bda_account", columnList = "account_id"),
            @Index(name = "idx_bda_status", columnList = "status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceDriftAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "account_number", length = 50, nullable = false)
    private String accountNumber;

    /** The cached balance value (Account.balance). */
    @Column(name = "cached_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal cachedBalance;

    /** The authoritative ledger-derived balance (SUM credits - SUM debits). */
    @Column(name = "ledger_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal ledgerBalance;

    /** The drift amount (cached - ledger). */
    @Column(name = "drift_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal driftAmount;

    /** OPEN, RESOLVED, ACKNOWLEDGED */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_remarks", length = 500)
    private String resolutionRemarks;

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }
}
