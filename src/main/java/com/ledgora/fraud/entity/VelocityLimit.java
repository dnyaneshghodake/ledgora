package com.ledgora.fraud.entity;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configurable velocity limits per tenant (optionally per account).
 *
 * <p>RBI Fraud Risk Management: Detect rapid-fire transaction bursts that may indicate fraud,
 * account takeover, or money laundering. Limits are enforced proactively before posting.
 *
 * <p>If account_id is null, the limit applies as a tenant-wide default for all accounts.
 */
@Entity
@Table(
        name = "velocity_limits",
        indexes = {
            @Index(name = "idx_vl_tenant", columnList = "tenant_id"),
            @Index(name = "idx_vl_account", columnList = "account_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VelocityLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Specific account this limit applies to. Null = tenant-wide default. */
    @Column(name = "account_id")
    private Long accountId;

    /** Maximum number of transactions allowed in the window. */
    @Column(name = "max_txn_count_per_hour", nullable = false)
    private Integer maxTxnCountPerHour;

    /** Maximum cumulative amount allowed in the window. */
    @Column(name = "max_total_amount_per_hour", precision = 19, scale = 4, nullable = false)
    private BigDecimal maxTotalAmountPerHour;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
