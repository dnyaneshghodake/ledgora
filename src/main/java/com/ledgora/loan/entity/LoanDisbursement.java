package com.ledgora.loan.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Loan Disbursement — tracks individual disbursement tranches for a loan account.
 *
 * <p>CBS multi-disbursement support per RBI Master Directions on Lending:
 *
 * <ul>
 *   <li>A sanctioned loan may be disbursed in multiple tranches
 *   <li>Each tranche has its own disbursement date and amount
 *   <li>Total disbursed cannot exceed sanctioned amount
 *   <li>Each disbursement generates a voucher pair (DR Loan Asset GL, CR Customer Account)
 *   <li>Interest accrual starts from each tranche's disbursement date
 * </ul>
 *
 * <p>Immutable after creation — disbursements cannot be modified, only reversed.
 */
@Entity
@Table(
        name = "loan_disbursements",
        indexes = {
            @Index(name = "idx_ld_tenant", columnList = "tenant_id"),
            @Index(name = "idx_ld_loan", columnList = "loan_account_id"),
            @Index(name = "idx_ld_date", columnList = "disbursement_date")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    /** Tranche number (1-based, sequential per loan). */
    @Column(name = "tranche_number", nullable = false)
    private Integer trancheNumber;

    /** Amount disbursed in this tranche. */
    @Column(name = "disbursement_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal disbursementAmount;

    /** Business date of disbursement. */
    @Column(name = "disbursement_date", nullable = false)
    private LocalDate disbursementDate;

    /** Status: DISBURSED, REVERSED. */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "DISBURSED";

    /** Voucher reference for the DR leg (for GL reconciliation). */
    @Column(name = "voucher_ref", length = 60)
    private String voucherRef;

    /** User who initiated the disbursement. */
    @Column(name = "disbursed_by", length = 50)
    private String disbursedBy;

    /** Remarks / narration. */
    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
