package com.ledgora.customer.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.CustomerStatus;
import com.ledgora.common.enums.FreezeLevel;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CBS-grade Customer Master entity.
 * Unique customer_number per tenant, no hard delete allowed.
 * Maker-checker required for create/modify.
 */
@Entity
@Table(name = "customer_master", indexes = {
    @Index(name = "idx_cm_tenant_custno", columnList = "tenant_id, customer_number", unique = true),
    @Index(name = "idx_cm_tenant", columnList = "tenant_id"),
    @Index(name = "idx_cm_status", columnList = "status"),
    @Index(name = "idx_cm_national_id", columnList = "national_id")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerMaster {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "customer_number", length = 30, nullable = false)
    private String customerNumber;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "kyc_status", length = 20, nullable = false)
    @Builder.Default
    private String kycStatus = "PENDING";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_branch_id")
    private Branch homeBranch;

    @Enumerated(EnumType.STRING)
    @Column(name = "freeze_level", length = 20, nullable = false)
    @Builder.Default
    private FreezeLevel freezeLevel = FreezeLevel.NONE;

    @Column(name = "freeze_reason", length = 255)
    private String freezeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20, nullable = false)
    @Builder.Default
    private MakerCheckerStatus approvalStatus = MakerCheckerStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "customer_type", length = 20)
    @Builder.Default
    private String customerType = "INDIVIDUAL";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_profile_id")
    private CustomerTaxProfile taxProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "maker_checker_status", length = 20, nullable = false)
    @Builder.Default
    private MakerCheckerStatus makerCheckerStatus = MakerCheckerStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maker_id")
    private User maker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checker_id")
    private User checker;

    @Column(name = "maker_timestamp")
    private LocalDateTime makerTimestamp;

    @Column(name = "checker_timestamp")
    private LocalDateTime checkerTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fullName == null) {
            fullName = firstName + " " + lastName;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
