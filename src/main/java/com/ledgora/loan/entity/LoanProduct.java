package com.ledgora.loan.entity;

import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.loan.enums.InterestType;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Loan product definition — Finacle-grade product master.
 *
 * <p>RBI Master Directions on Interest Rate on Advances: Each product defines the interest rate,
 * compounding frequency, tenure, and penalty rate. GL mappings drive all accounting entries through
 * the voucher engine.
 *
 * <p>GL Mapping (RBI IRAC compliant):
 *
 * <ul>
 *   <li>glLoanAsset: Standard loan asset (Schedule 9 — Advances)
 *   <li>glInterestIncome: Interest earned on performing loans (Schedule 13)
 *   <li>glInterestReceivable: Accrued but unrealized interest
 *   <li>glNpaLoanAsset: NPA loan asset (reclassified from standard)
 *   <li>glProvision: Provision for loan losses (Schedule 12)
 * </ul>
 */
@Entity
@Table(
        name = "loan_products",
        indexes = {@Index(name = "idx_lp_tenant", columnList = "tenant_id")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "product_code", length = 20, nullable = false, unique = true)
    private String productCode;

    @Column(name = "product_name", length = 100, nullable = false)
    private String productName;

    @Column(name = "interest_rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", length = 10, nullable = false)
    @Builder.Default
    private InterestType interestType = InterestType.FIXED;

    /** MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY */
    @Column(name = "compounding_frequency", length = 20, nullable = false)
    @Builder.Default
    private String compoundingFrequency = "MONTHLY";

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    /** Penal interest rate for overdue installments (per annum). */
    @Column(name = "penalty_rate", precision = 7, scale = 4)
    @Builder.Default
    private BigDecimal penaltyRate = new BigDecimal("2.0000");

    /** Grace days after due date before marking overdue (CBS standard: 0–5 days). */
    @Column(name = "grace_days", nullable = false)
    @Builder.Default
    private Integer graceDays = 0;

    /** Whether moratorium (EMI holiday) is allowed for this product. */
    @Column(name = "moratorium_allowed", nullable = false)
    @Builder.Default
    private Boolean moratoriumAllowed = false;

    /** Repayment type: REDUCING_BALANCE, FLAT, BULLET. */
    @Column(name = "repayment_type", length = 20, nullable = false)
    @Builder.Default
    private String repaymentType = "REDUCING_BALANCE";

    /** Provisioning policy code (for configurable provision matrix). */
    @Column(name = "provisioning_policy_code", length = 20)
    private String provisioningPolicyCode;

    /** Days past due threshold for NPA classification (RBI default: 90). */
    @Column(name = "npa_days_threshold", nullable = false)
    @Builder.Default
    private Integer npaDaysThreshold = 90;

    // ── GL Mappings (all accounting via voucher engine) ──

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_loan_asset_id", nullable = false)
    private GeneralLedger glLoanAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_interest_income_id", nullable = false)
    private GeneralLedger glInterestIncome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_interest_receivable_id", nullable = false)
    private GeneralLedger glInterestReceivable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_npa_loan_asset_id", nullable = false)
    private GeneralLedger glNpaLoanAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_provision_id", nullable = false)
    private GeneralLedger glProvision;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
