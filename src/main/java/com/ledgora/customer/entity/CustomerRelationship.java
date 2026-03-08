package com.ledgora.customer.entity;

import com.ledgora.common.enums.RelationshipType;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * CBS-grade Customer Relationship entity.
 * Tracks relationships between customers (joint holders, nominees, guarantors, etc.).
 */
@Entity
@Table(name = "customer_relationship", indexes = {
    @Index(name = "idx_cr_primary_customer", columnList = "primary_customer_id"),
    @Index(name = "idx_cr_related_customer", columnList = "related_customer_id"),
    @Index(name = "idx_cr_tenant", columnList = "tenant_id")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerRelationship {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_customer_id", nullable = false)
    private CustomerMaster primaryCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_customer_id", nullable = false)
    private CustomerMaster relatedCustomer;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", length = 30, nullable = false)
    private RelationshipType relationshipType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "remarks", length = 255)
    private String remarks;

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
