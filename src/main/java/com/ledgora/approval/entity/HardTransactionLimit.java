package com.ledgora.approval.entity;

import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Absolute hard transaction ceiling per tenant + channel.
 *
 * <p>RBI Risk Appetite Framework: No role (including ADMIN) may bypass these limits. No
 * configuration override path exists. Violations throw GovernanceException and are logged to the
 * governance audit trail.
 *
 * <p>This is a safety net above the approval policy engine — approval policies may allow
 * auto-authorization up to a soft threshold, but this hard limit is an absolute ceiling that cannot
 * be exceeded regardless of approval status.
 */
@Entity
@Table(
        name = "hard_transaction_limits",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_htl_tenant_channel",
                    columnNames = {"tenant_id", "channel"})
        },
        indexes = {@Index(name = "idx_htl_tenant", columnList = "tenant_id")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HardTransactionLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * Channel this limit applies to. Null means default/fallback for the tenant (applies to all
     * channels not explicitly configured).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private TransactionChannel channel;

    /** Absolute maximum amount for a single transaction. No role may exceed this. */
    @Column(name = "absolute_max_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal absoluteMaxAmount;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
