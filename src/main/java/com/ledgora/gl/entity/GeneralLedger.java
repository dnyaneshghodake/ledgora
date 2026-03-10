package com.ledgora.gl.entity;

import com.ledgora.common.enums.GLAccountType;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "general_ledgers",
        indexes = {@Index(name = "idx_gl_tenant", columnList = "tenant_id")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "gl_code", length = 20, nullable = false, unique = true)
    private String glCode;

    @Column(name = "gl_name", length = 100, nullable = false)
    private String glName;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20, nullable = false)
    private GLAccountType accountType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private GeneralLedger parent;

    @Column(name = "level_num", nullable = false)
    @Builder.Default
    private Integer level = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "normal_balance", length = 10)
    private String normalBalance;

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
