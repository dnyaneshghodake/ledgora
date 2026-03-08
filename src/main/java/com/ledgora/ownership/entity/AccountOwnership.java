package com.ledgora.ownership.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.common.enums.OwnershipType;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AccountOwnership entity for Customer-Account ownership model.
 * Replaces direct customerId link with proper ownership tracking.
 * Supports PRIMARY, JOINT, GUARANTOR, NOMINEE ownership types.
 */
@Entity
@Table(name = "account_ownership", indexes = {
    @Index(name = "idx_ao_account", columnList = "account_id"),
    @Index(name = "idx_ao_customer", columnList = "customer_master_id"),
    @Index(name = "idx_ao_tenant", columnList = "tenant_id")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountOwnership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_master_id", nullable = false)
    private CustomerMaster customerMaster;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_type", length = 20, nullable = false)
    private OwnershipType ownershipType;

    @Column(name = "ownership_percentage", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal ownershipPercentage = new BigDecimal("100.00");

    @Column(name = "is_operational", nullable = false)
    @Builder.Default
    private Boolean isOperational = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20, nullable = false)
    @Builder.Default
    private MakerCheckerStatus approvalStatus = MakerCheckerStatus.PENDING;

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
