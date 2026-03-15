package com.ledgora.loan.entity;

import com.ledgora.loan.enums.InterestType;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Loan Rate — Finacle LARATE equivalent. Defines interest rates with effective date tracking.
 *
 * <p>RBI Fair Practices Code for Lenders:
 *
 * <ul>
 *   <li>FIXED rates are locked at disbursement — no mid-term changes
 *   <li>FLOATING rates are linked to an external benchmark (MCLR/EBLR/repo)
 *   <li>Spread over benchmark is product-specific
 *   <li>Rate changes must be communicated to borrowers per RBI circular
 *   <li>Effective date determines when the new rate applies
 * </ul>
 *
 * <p>Rate hierarchy: Product base rate + spread = effective rate.
 * For FLOATING products, benchmark changes trigger rate recalculation.
 */
@Entity
@Table(
        name = "loan_rates",
        indexes = {
            @Index(name = "idx_lr_tenant", columnList = "tenant_id"),
            @Index(name = "idx_lr_product", columnList = "loan_product_id"),
            @Index(name = "idx_lr_effective_date", columnList = "effective_date")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_lr_product_effective",
                    columnNames = {"loan_product_id", "effective_date"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    /** Benchmark rate name — e.g. "REPO", "MCLR_1Y", "EBLR". Null for FIXED products. */
    @Column(name = "benchmark_name", length = 30)
    private String benchmarkName;

    /** Benchmark rate value — the external reference rate (e.g. 6.50 for repo rate). */
    @Column(name = "benchmark_rate", precision = 7, scale = 4)
    private BigDecimal benchmarkRate;

    /** Spread over benchmark — product-specific margin (e.g. 2.50%). */
    @Column(name = "spread", precision = 7, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal spread = BigDecimal.ZERO;

    /** Effective annual interest rate = benchmark + spread (or standalone for FIXED). */
    @Column(name = "effective_rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal effectiveRate;

    /** Interest type — FIXED or FLOATING. Inherited from product at creation. */
    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", length = 10, nullable = false)
    @Builder.Default
    private InterestType interestType = InterestType.FIXED;

    /** Date from which this rate is effective. */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /** Date until which this rate is effective. Null means currently active (open-ended). */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Whether this is the currently active rate for the product. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "remarks", length = 500)
    private String remarks;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
