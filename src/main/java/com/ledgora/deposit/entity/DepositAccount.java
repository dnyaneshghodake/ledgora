package com.ledgora.deposit.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.deposit.enums.DepositAccountStatus;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Deposit account — individual CASA/FD/RD contract linked to a core account.
 *
 * <p>RBI Deposit Regulations:
 *
 * <ul>
 *   <li>CASA: demand deposit — no maturity, interest accrued daily, posted quarterly
 *   <li>FD: time deposit — fixed maturity, interest accrued daily, posted at maturity/quarterly
 *   <li>RD: recurring deposit — installment-based, interest on cumulative balance
 *   <li>DICGC coverage: up to ₹5,00,000 per depositor per bank
 * </ul>
 *
 * <p>STRICT: No direct mutation of interestAccrued. All changes via voucher engine at EOD.
 */
@Entity
@Table(
        name = "deposit_accounts",
        indexes = {
            @Index(name = "idx_da_tenant", columnList = "tenant_id"),
            @Index(name = "idx_da_status", columnList = "status"),
            @Index(name = "idx_da_maturity", columnList = "maturity_date"),
            @Index(name = "idx_da_linked", columnList = "linked_account_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_product_id", nullable = false)
    private DepositProduct depositProduct;

    /** The customer's operating account (for interest credit / maturity payout). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_account_id", nullable = false)
    private Account linkedAccount;

    @Column(name = "deposit_account_number", length = 30, nullable = false, unique = true)
    private String depositAccountNumber;

    /** Original principal (FD/RD) or current balance snapshot (CASA). */
    @Column(name = "principal_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal principalAmount;

    /** Cumulative accrued interest — updated daily at EOD. */
    @Column(name = "interest_accrued", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal interestAccrued = BigDecimal.ZERO;

    /** Maturity date (FD/RD). Null for CASA. */
    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    /** Tenure in months (FD/RD). Null for CASA. */
    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private DepositAccountStatus status = DepositAccountStatus.ACTIVE;

    @Column(name = "auto_renewal_flag", nullable = false)
    @Builder.Default
    private Boolean autoRenewalFlag = false;

    /** Date of last interest posting (for monthly/quarterly posting cycle). */
    @Column(name = "last_interest_posting_date")
    private LocalDate lastInterestPostingDate;

    @Column(name = "opening_date", nullable = false)
    private LocalDate openingDate;

    @Column(name = "closure_date")
    private LocalDate closureDate;

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
