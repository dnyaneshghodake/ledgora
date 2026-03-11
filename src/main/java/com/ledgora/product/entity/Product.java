package com.ledgora.product.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.common.enums.ProductStatus;
import com.ledgora.common.enums.ProductType;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade Product entity. A Product is a template (e.g., "Regular Savings", "Premium Current")
 * under which Accounts are opened. Maker-checker enforced on creation. Unique productCode per
 * tenant. No hard delete — products are RETIRED, never removed.
 */
@Entity
@Table(
        name = "products",
        indexes = {
            @Index(name = "idx_product_tenant", columnList = "tenant_id"),
            @Index(name = "idx_product_type", columnList = "product_type"),
            @Index(name = "idx_product_status", columnList = "status")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_product_tenant_code",
                    columnNames = {"tenant_id", "product_code"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "product_code", length = 20, nullable = false)
    private String productCode;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", length = 20, nullable = false)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20, nullable = false)
    @Builder.Default
    private MakerCheckerStatus approvalStatus = MakerCheckerStatus.PENDING;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

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
