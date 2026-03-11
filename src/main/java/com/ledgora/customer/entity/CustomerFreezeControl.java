package com.ledgora.customer.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade Customer Freeze Control entity. Controls debit freeze and credit freeze at customer
 * level. Freeze propagates to all accounts owned by the customer.
 */
@Entity
@Table(
        name = "customer_freeze_control",
        indexes = {
            @Index(name = "idx_cfc_customer", columnList = "customer_master_id"),
            @Index(name = "idx_cfc_tenant", columnList = "tenant_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFreezeControl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_master_id", nullable = false)
    private CustomerMaster customerMaster;

    @Column(name = "debit_freeze", nullable = false)
    @Builder.Default
    private Boolean debitFreeze = false;

    @Column(name = "credit_freeze", nullable = false)
    @Builder.Default
    private Boolean creditFreeze = false;

    @Column(name = "debit_freeze_reason", length = 255)
    private String debitFreezeReason;

    @Column(name = "credit_freeze_reason", length = 255)
    private String creditFreezeReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freeze_applied_by")
    private User freezeAppliedBy;

    @Column(name = "freeze_applied_at")
    private LocalDateTime freezeAppliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freeze_released_by")
    private User freezeReleasedBy;

    @Column(name = "freeze_released_at")
    private LocalDateTime freezeReleasedAt;

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
