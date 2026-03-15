package com.ledgora.loan.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Loan Collateral — security pledged against a loan per RBI Prudential Norms.
 *
 * <p>RBI Master Circular on Prudential Norms — Collateral:
 *
 * <ul>
 *   <li>Collateral type determines provisioning rate (secured vs unsecured)
 *   <li>Valuation must be current (revaluation frequency per RBI norms)
 *   <li>LTV (Loan-to-Value) ratio monitored for regulatory compliance
 *   <li>Collateral release only after loan closure
 * </ul>
 *
 * <p>Collateral types per RBI: PROPERTY, GOLD, FD, SHARES, VEHICLE, MACHINERY, OTHER.
 */
@Entity
@Table(
        name = "loan_collaterals",
        indexes = {
            @Index(name = "idx_lc_tenant", columnList = "tenant_id"),
            @Index(name = "idx_lc_loan", columnList = "loan_account_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanCollateral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    /** Collateral type: PROPERTY, GOLD, FD, SHARES, VEHICLE, MACHINERY, OTHER. */
    @Column(name = "collateral_type", length = 30, nullable = false)
    private String collateralType;

    /** Description of the collateral asset. */
    @Column(name = "description", length = 500, nullable = false)
    private String description;

    /** Current valuation amount. */
    @Column(name = "valuation_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal valuationAmount;

    /** Date of last valuation. */
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;

    /** Name of the valuer / valuation agency. */
    @Column(name = "valuer_name", length = 100)
    private String valuerName;

    /** LTV ratio = outstanding / valuation (computed, stored for quick query). */
    @Column(name = "ltv_ratio", precision = 7, scale = 4)
    private BigDecimal ltvRatio;

    /** Status: PLEDGED, RELEASED, SEIZED. */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "PLEDGED";

    /** External reference (e.g., property registration number, FD number). */
    @Column(name = "external_reference", length = 100)
    private String externalReference;

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
