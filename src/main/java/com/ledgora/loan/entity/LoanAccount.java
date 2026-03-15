package com.ledgora.loan.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Loan account — individual loan contract linked to a customer account.
 *
 * <p>RBI IRAC Norms — Income Recognition and Asset Classification:
 *
 * <ul>
 *   <li>DPD (Days Past Due) tracked daily at EOD
 *   <li>NPA classification when dpd > product.npaDaysThreshold (default 90)
 *   <li>Interest recognition stops on NPA classification
 *   <li>Provisioning computed based on NPA classification tier
 * </ul>
 *
 * <p>STRICT CONSTRAINT: No direct mutation of outstandingPrincipal or accruedInterest. All changes
 * must flow through the voucher engine (disbursement, accrual, EMI, write-off).
 */
@Entity
@Table(
        name = "loan_accounts",
        indexes = {
            @Index(name = "idx_la_tenant", columnList = "tenant_id"),
            @Index(name = "idx_la_status", columnList = "status"),
            @Index(name = "idx_la_linked_account", columnList = "linked_account_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    /** The customer's operating account (for EMI debit / disbursement credit). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_account_id", nullable = false)
    private Account linkedAccount;

    @Column(name = "loan_account_number", length = 30, nullable = false, unique = true)
    private String loanAccountNumber;

    @Column(name = "principal_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "outstanding_principal", precision = 19, scale = 4, nullable = false)
    private BigDecimal outstandingPrincipal;

    @Column(name = "accrued_interest", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    /** Days Past Due — updated daily at EOD by LoanDpdService. */
    @Column(name = "dpd", nullable = false)
    @Builder.Default
    private Integer dpd = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private LoanStatus status = LoanStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "npa_classification", length = 20, nullable = false)
    @Builder.Default
    private NpaClassification npaClassification = NpaClassification.STANDARD;

    /** Date when loan was classified as NPA. Null if still performing. */
    @Column(name = "npa_date")
    private LocalDate npaDate;

    /** Current provision amount per RBI IRAC provisioning norms. */
    @Column(name = "provision_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal provisionAmount = BigDecimal.ZERO;

    /** Last business date on which interest was accrued — prevents double-accrual on EOD retry. */
    @Column(name = "last_accrual_date")
    private LocalDate lastAccrualDate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

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
