package com.ledgora.teller.entity;

import com.ledgora.branch.entity.Branch;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Vault Master — one vault per branch. Tracks vault cash position with dual-custody enforcement.
 * Holding limit default ₹2 Crore per RBI branch cash guidelines.
 */
@Entity
@Table(
        name = "vault_masters",
        indexes = {
            @Index(name = "idx_vault_master_branch", columnList = "branch_id", unique = true),
            @Index(name = "idx_vault_master_tenant", columnList = "tenant_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "current_balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    /** Maximum vault holding (default ₹2,00,00,000 = 2 Crore). */
    @Column(name = "holding_limit", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal holdingLimit = new BigDecimal("20000000.0000");

    /** RBI dual-custody requirement: two authorized users must approve vault access. */
    @Column(name = "dual_custody_flag", nullable = false)
    @Builder.Default
    private Boolean dualCustodyFlag = true;

    @Column(name = "last_audit_at")
    private LocalDateTime lastAuditAt;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
