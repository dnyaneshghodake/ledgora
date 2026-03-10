package com.ledgora.suspense.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration table mapping tenant + channel to a suspense account.
 *
 * <p>CBS Standard: Each tenant must have a dedicated Suspense GL account. Failed partial postings
 * are routed to the suspense account to preserve double-entry integrity. Channel-specific suspense
 * accounts allow segregated tracking.
 */
@Entity
@Table(
        name = "suspense_gl_mappings",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_suspense_gl_tenant_channel",
                    columnNames = {"tenant_id", "channel"})
        },
        indexes = {@Index(name = "idx_sgl_tenant", columnList = "tenant_id")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspenseGlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Channel this mapping applies to. Null means default/fallback for the tenant. */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private TransactionChannel channel;

    /** The suspense account number (e.g., "SUSP-T001"). */
    @Column(name = "suspense_account_number", length = 50, nullable = false)
    private String suspenseAccountNumber;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
