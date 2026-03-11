package com.ledgora.loan.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.InstallmentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade Loan Schedule (amortization) entity. One row per installment.
 * Generated at loan disbursement using flat/reducing balance EMI calculation.
 *
 * <p>DPD (Days Past Due) is updated nightly by EOD. NPA classification
 * triggers at DPD > 90 per RBI guidelines.
 *
 * <p>Unique constraint on (account_id, installment_number) prevents duplicate
 * schedule rows.
 */
@Entity
@Table(
        name = "loan_schedules",
        indexes = {
            @Index(name = "idx_ls_account", columnList = "account_id"),
            @Index(name = "idx_ls_due_date", columnList = "due_date"),
            @Index(name = "idx_ls_status", columnList = "status")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_ls_account_installment",
                    columnNames = {"account_id", "installment_number"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_component", precision = 19, scale = 4, nullable = false)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", precision = 19, scale = 4, nullable = false)
    private BigDecimal interestComponent;

    /** Total EMI = principal + interest. */
    @Column(name = "emi_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal emiAmount;

    /** Outstanding principal after this installment is paid. */
    @Column(name = "outstanding_principal", precision = 19, scale = 4, nullable = false)
    private BigDecimal outstandingPrincipal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private InstallmentStatus status = InstallmentStatus.SCHEDULED;

    /** Days Past Due — updated by EOD nightly run. 0 for current installments. */
    @Column(name = "dpd_days", nullable = false)
    @Builder.Default
    private Integer dpdDays = 0;

    /** Amount actually paid against this installment (for partial payments). */
    @Column(name = "paid_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_date")
    private LocalDate paidDate;

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
