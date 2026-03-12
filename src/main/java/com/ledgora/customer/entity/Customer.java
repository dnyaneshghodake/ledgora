package com.ledgora.customer.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade Customer entity. Multi-tenant aware with full maker-checker lifecycle.
 *
 * <p>Lifecycle: Maker creates (PENDING) → Checker approves (VERIFIED) or rejects (REJECTED).
 * Modifications also require maker-checker dual control per RBI IT Framework.
 *
 * <p>Fields aligned with RBI KYC Master Direction:
 *
 * <ul>
 *   <li>customerType: INDIVIDUAL or CORPORATE
 *   <li>panNumber: mandatory for INDIVIDUAL
 *   <li>aadhaarNumber: optional OVD for INDIVIDUAL
 *   <li>gstNumber: mandatory for CORPORATE
 *   <li>approvalStatus: PENDING → APPROVED / REJECTED
 *   <li>maker / checker: dual-control audit trail
 * </ul>
 */
@Entity
@Table(
        name = "customers",
        indexes = {
            @Index(name = "idx_customer_national_id", columnList = "national_id", unique = true),
            @Index(name = "idx_customer_email", columnList = "email"),
            @Index(name = "idx_customer_tenant", columnList = "tenant_id"),
            @Index(name = "idx_customer_approval_status", columnList = "approval_status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    /** Full name — derived from first + last on persist, stored for search performance. */
    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "national_id", length = 50, unique = true)
    private String nationalId;

    @Column(name = "kyc_status", length = 20, nullable = false)
    @Builder.Default
    private String kycStatus = "PENDING";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "freeze_level", length = 20)
    @Builder.Default
    private String freezeLevel = "NONE";

    @Column(name = "freeze_reason", length = 255)
    private String freezeReason;

    // ── RBI KYC Master Direction fields ──

    /** INDIVIDUAL or CORPORATE. Drives mandatory field validation. */
    @Column(name = "customer_type", length = 20)
    @Builder.Default
    private String customerType = "INDIVIDUAL";

    /** PAN number — mandatory for INDIVIDUAL per RBI KYC norms. */
    @Column(name = "pan_number", length = 10)
    private String panNumber;

    /** Aadhaar number — valid OVD for account opening. Stored masked in production. */
    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;

    /** GST number — mandatory for CORPORATE per RBI KYC norms. */
    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    /** Risk category: LOW, MEDIUM, HIGH per RBI KYC risk-based approach. */
    @Column(name = "risk_category", length = 10)
    @Builder.Default
    private String riskCategory = "LOW";

    /** Annual income (INR) — used for risk derivation. */
    @Column(name = "annual_income", precision = 18, scale = 2)
    private java.math.BigDecimal annualIncome;

    /** Occupation category — used for risk derivation (e.g., SALARIED, BUSINESS, POLITICIAN). */
    @Column(name = "occupation", length = 50)
    private String occupation;

    /** Politically Exposed Person flag — immediately elevates risk to HIGH. */
    @Column(name = "is_pep", nullable = false)
    @Builder.Default
    private Boolean isPep = false;

    // ── Maker-Checker fields ──

    /**
     * Approval status — PENDING until checker approves or rejects.
     * Transactions are blocked while status is PENDING.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20, nullable = false)
    @Builder.Default
    private MakerCheckerStatus approvalStatus = MakerCheckerStatus.PENDING;

    /** The user who created this customer record (maker). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** The user who approved or rejected this customer record (checker). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "maker_timestamp")
    private LocalDateTime makerTimestamp;

    @Column(name = "checker_timestamp")
    private LocalDateTime checkerTimestamp;

    /**
     * Optimistic locking — prevents concurrent approval of the same customer from two sessions.
     */
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
        if (fullName == null && firstName != null && lastName != null) {
            fullName = firstName + " " + lastName;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (firstName != null && lastName != null) {
            fullName = firstName + " " + lastName;
        }
    }
}
