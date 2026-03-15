package com.ledgora.loan.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Loan Rate Change History — immutable audit trail for every rate change.
 *
 * <p>RBI Master Directions on Interest Rate on Advances:
 *
 * <ul>
 *   <li>Every rate change must be recorded with before/after values
 *   <li>Effective date of change must be captured
 *   <li>Reason for change (benchmark reset, manual override, RBI directive)
 *   <li>Impact on affected loan accounts must be tracked
 *   <li>Borrower notification requirement per RBI FPC
 * </ul>
 *
 * <p>This entity is append-only (immutable). No updates or deletes.
 * Used for RBI audit compliance and borrower dispute resolution.
 */
@Entity
@Table(
        name = "loan_rate_change_history",
        indexes = {
            @Index(name = "idx_lrch_tenant", columnList = "tenant_id"),
            @Index(name = "idx_lrch_product", columnList = "loan_product_id"),
            @Index(name = "idx_lrch_loan", columnList = "loan_account_id"),
            @Index(name = "idx_lrch_effective", columnList = "effective_date")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRateChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Product whose rate changed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    /** Specific loan account affected. Null for product-level changes before propagation. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id")
    private LoanAccount loanAccount;

    /** The LoanRate record that was created/modified. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_rate_id")
    private LoanRate loanRate;

    /** Previous interest rate (before change). */
    @Column(name = "old_rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal oldRate;

    /** New interest rate (after change). */
    @Column(name = "new_rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal newRate;

    /** Previous benchmark rate (for FLOATING products). */
    @Column(name = "old_benchmark_rate", precision = 7, scale = 4)
    private BigDecimal oldBenchmarkRate;

    /** New benchmark rate (for FLOATING products). */
    @Column(name = "new_benchmark_rate", precision = 7, scale = 4)
    private BigDecimal newBenchmarkRate;

    /** Previous EMI amount (before recalculation). Null for product-level changes. */
    @Column(name = "old_emi", precision = 19, scale = 4)
    private BigDecimal oldEmi;

    /** New EMI amount (after recalculation). Null for product-level changes. */
    @Column(name = "new_emi", precision = 19, scale = 4)
    private BigDecimal newEmi;

    /** Date from which the new rate is effective. */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /**
     * Reason for rate change:
     * BENCHMARK_RESET, MANUAL_OVERRIDE, RBI_DIRECTIVE, PRODUCT_REVISION, RATE_NEGOTIATION.
     */
    @Column(name = "change_reason", length = 50, nullable = false)
    private String changeReason;

    /** Detailed remarks for the change. */
    @Column(name = "remarks", length = 500)
    private String remarks;

    /** User who initiated the change. */
    @Column(name = "changed_by", length = 50)
    private String changedBy;

    /** Immutable — created_at only, no updates. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
