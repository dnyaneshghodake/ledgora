package com.ledgora.loan.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Repayment Transaction — immutable record of each EMI/prepayment received.
 *
 * <p>Per RBI and CBS audit requirements:
 *
 * <ul>
 *   <li>Every payment against a loan must be individually recorded
 *   <li>Principal and interest components tracked separately
 *   <li>Linked to voucher pair for GL reconciliation
 *   <li>Business date (not system date) used for all date references
 *   <li>Immutable — no updates after creation
 * </ul>
 *
 * <p>Used for: statement generation, interest certificate, tax computation, borrower dispute
 * resolution, RBI audit.
 */
@Entity
@Table(
        name = "repayment_transactions",
        indexes = {
            @Index(name = "idx_rt_tenant", columnList = "tenant_id"),
            @Index(name = "idx_rt_loan", columnList = "loan_account_id"),
            @Index(name = "idx_rt_date", columnList = "payment_date")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    /** Total payment amount (principal + interest). */
    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;

    /** Principal component of the payment. */
    @Column(name = "principal_component", precision = 19, scale = 4, nullable = false)
    private BigDecimal principalComponent;

    /** Interest component of the payment. */
    @Column(name = "interest_component", precision = 19, scale = 4, nullable = false)
    private BigDecimal interestComponent;

    /** Outstanding principal after this payment. */
    @Column(name = "outstanding_after", precision = 19, scale = 4, nullable = false)
    private BigDecimal outstandingAfter;

    /** Accrued interest remaining after this payment. */
    @Column(name = "accrued_interest_after", precision = 19, scale = 4, nullable = false)
    private BigDecimal accruedInterestAfter;

    /** Business date of payment (tenant business date, not system clock). */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /** Payment type: EMI, PREPAYMENT, FORECLOSURE, PENALTY. */
    @Column(name = "payment_type", length = 20, nullable = false)
    @Builder.Default
    private String paymentType = "EMI";

    /** Voucher reference for principal component (for GL reconciliation). */
    @Column(name = "principal_voucher_ref", length = 60)
    private String principalVoucherRef;

    /** Voucher reference for interest component (for GL reconciliation). */
    @Column(name = "interest_voucher_ref", length = 60)
    private String interestVoucherRef;

    /** User who initiated the payment. */
    @Column(name = "initiated_by", length = 50)
    private String initiatedBy;

    /** Remarks / narration. */
    @Column(name = "remarks", length = 500)
    private String remarks;

    /** Immutable — created_at only, no updates. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
