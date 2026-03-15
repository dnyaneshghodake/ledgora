package com.ledgora.loan.entity;

import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Loan Provision — daily provision snapshot per RBI IRAC Prudential Norms.
 *
 * <p>Tracks incremental provisioning changes for each loan:
 *
 * <ul>
 *   <li>STANDARD: 0.40% of outstanding
 *   <li>SUBSTANDARD: 15% (25% unsecured)
 *   <li>DOUBTFUL: 25–100% depending on age
 *   <li>LOSS: 100%
 * </ul>
 *
 * <p>Immutable — one record per loan per business date. Used for: regulatory reporting, P&L impact
 * analysis, RBI audit.
 */
@Entity
@Table(
        name = "loan_provisions",
        indexes = {
            @Index(name = "idx_lprov_tenant", columnList = "tenant_id"),
            @Index(name = "idx_lprov_loan", columnList = "loan_account_id"),
            @Index(name = "idx_lprov_date", columnList = "business_date")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_lprov_loan_date",
                    columnNames = {"loan_account_id", "business_date"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProvision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    /** Business date of this provision snapshot. */
    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    /** NPA classification at time of provision. */
    @Enumerated(EnumType.STRING)
    @Column(name = "npa_classification", length = 20, nullable = false)
    private NpaClassification npaClassification;

    /** Provision rate applied (%). */
    @Column(name = "provision_rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal provisionRate;

    /** Outstanding principal at time of provision. */
    @Column(name = "outstanding_principal", precision = 19, scale = 4, nullable = false)
    private BigDecimal outstandingPrincipal;

    /** Required provision = outstanding × rate / 100. */
    @Column(name = "required_provision", precision = 19, scale = 4, nullable = false)
    private BigDecimal requiredProvision;

    /** Previous provision amount (before this calculation). */
    @Column(name = "previous_provision", precision = 19, scale = 4, nullable = false)
    private BigDecimal previousProvision;

    /** Incremental provision = required - previous (posted to P&L). */
    @Column(name = "incremental_provision", precision = 19, scale = 4, nullable = false)
    private BigDecimal incrementalProvision;

    /** Immutable — created_at only. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
