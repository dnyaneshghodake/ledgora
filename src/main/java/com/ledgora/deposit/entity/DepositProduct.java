package com.ledgora.deposit.entity;

import com.ledgora.deposit.enums.DepositType;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Deposit product master — Finacle-grade product definition.
 *
 * <p>RBI Master Directions on Interest Rate on Deposits:
 *
 * <ul>
 *   <li>Savings: quarterly compounding (RBI mandated)
 *   <li>Current: no interest (RBI regulation)
 *   <li>FD: quarterly/monthly compounding per product
 *   <li>RD: quarterly compounding on installments
 * </ul>
 *
 * <p>GL Mappings drive all accounting through voucher engine:
 *
 * <ul>
 *   <li>glDepositLiability: Customer deposit liability (Schedule 5)
 *   <li>glInterestExpense: Interest expense on deposits (Schedule 14)
 * </ul>
 */
@Entity
@Table(
        name = "deposit_products",
        indexes = {@Index(name = "idx_dp_tenant", columnList = "tenant_id")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositProduct {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_type", length = 20, nullable = false)
    private DepositType depositType;

    /** Annual interest rate (e.g., 4.0000 for 4% p.a.). Zero for current accounts. */
    @Column(name = "interest_rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal interestRate;

    /** DAILY, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY */
    @Column(name = "compounding_frequency", length = 20, nullable = false)
    @Builder.Default
    private String compoundingFrequency = "QUARTERLY";

    /** Minimum balance for CASA accounts. */
    @Column(name = "min_balance", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal minBalance = BigDecimal.ZERO;

    /** Penalty rate for premature closure (% of interest earned). */
    @Column(name = "premature_penalty_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal prematurePenaltyPercent = new BigDecimal("1.00");

    /** Default tenure in months (FD/RD). Null for CASA. */
    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "auto_renewal_allowed", nullable = false)
    @Builder.Default
    private Boolean autoRenewalAllowed = false;

    /** RBI CRR eligibility flag — contributes to NDTL computation. */
    @Column(name = "crr_eligible", nullable = false)
    @Builder.Default
    private Boolean crrEligible = true;

    /** RBI SLR eligibility flag. */
    @Column(name = "slr_eligible", nullable = false)
    @Builder.Default
    private Boolean slrEligible = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_deposit_liability_id")
    private GeneralLedger glDepositLiability;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_interest_expense_id")
    private GeneralLedger glInterestExpense;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
