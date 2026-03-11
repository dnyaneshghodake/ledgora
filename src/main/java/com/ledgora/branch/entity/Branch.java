package com.ledgora.branch.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade Branch (SOL) entity. Each branch belongs to a tenant. branchCode is the SOL ID
 * (unique per tenant). branchName is the display name used across all Finacle-style screens.
 */
@Entity
@Table(
        name = "branches",
        indexes = {
            @Index(name = "idx_branch_code", columnList = "branch_code", unique = true),
            @Index(name = "idx_branch_tenant", columnList = "tenant_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "branch_code", length = 10, nullable = false, unique = true)
    private String branchCode;

    @Column(name = "branch_name", length = 100, nullable = false)
    private String branchName;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Backward compat: sync name ↔ branchName
        if (branchName == null && name != null) branchName = name;
        if (name == null && branchName != null) name = branchName;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (branchName == null && name != null) branchName = name;
        if (name == null && branchName != null) name = branchName;
    }
}
