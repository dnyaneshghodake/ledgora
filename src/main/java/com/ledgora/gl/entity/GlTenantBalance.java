package com.ledgora.gl.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * GL Tenant Balance - tracks GL balance at tenant level. Aggregate of all branch-level GL balances
 * for a tenant.
 */
@Entity
@Table(
        name = "gl_tenant_balances",
        indexes = {
            @Index(name = "idx_gltb_composite", columnList = "tenant_id, gl_id", unique = true),
            @Index(name = "idx_gltb_tenant", columnList = "tenant_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlTenantBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_id", nullable = false)
    private GeneralLedger gl;

    @Column(name = "gl_actual_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal glActualBalance = BigDecimal.ZERO;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
