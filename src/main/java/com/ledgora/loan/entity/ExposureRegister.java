package com.ledgora.loan.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Exposure Register — aggregate exposure tracking per RBI Large Exposure Framework.
 *
 * <p>RBI Master Circular on Exposure Norms + Basel III LEF:
 *
 * <ul>
 *   <li>Tracks total exposure per borrower, borrower group, and sector
 *   <li>Single borrower cap: 15% of Tier-1 capital (20% for infrastructure)
 *   <li>Group borrower cap: 40% of Tier-1 capital (50% for infrastructure)
 *   <li>Sector-wise caps per bank's internal policy
 *   <li>Snapshot updated daily during EOD for regulatory reporting
 *   <li>CRILC reporting for exposures ≥ ₹5 crore
 * </ul>
 *
 * <p>Exposure types: FUND_BASED (loans, OD), NON_FUND_BASED (LC, BG), DERIVATIVE.
 */
@Entity
@Table(
        name = "exposure_register",
        indexes = {
            @Index(name = "idx_er_tenant", columnList = "tenant_id"),
            @Index(name = "idx_er_borrower", columnList = "borrower_id"),
            @Index(name = "idx_er_sector", columnList = "sector"),
            @Index(name = "idx_er_date", columnList = "snapshot_date")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_er_tenant_borrower_date",
                    columnNames = {"tenant_id", "borrower_id", "snapshot_date"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExposureRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Borrower identifier. */
    @Column(name = "borrower_id", length = 30, nullable = false)
    private String borrowerId;

    /** Borrower name. */
    @Column(name = "borrower_name", length = 100)
    private String borrowerName;

    /** Borrower group identifier. */
    @Column(name = "borrower_group_id", length = 30)
    private String borrowerGroupId;

    /** Sector classification. */
    @Column(name = "sector", length = 50)
    private String sector;

    /** Fund-based exposure (loans, OD, CC). */
    @Column(name = "fund_based_exposure", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal fundBasedExposure = BigDecimal.ZERO;

    /** Non-fund-based exposure (LC, BG). */
    @Column(name = "non_fund_based_exposure", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal nonFundBasedExposure = BigDecimal.ZERO;

    /** Total exposure = fund + non-fund. */
    @Column(name = "total_exposure", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalExposure = BigDecimal.ZERO;

    /** Total sanctioned limit for this borrower. */
    @Column(name = "total_sanctioned", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalSanctioned = BigDecimal.ZERO;

    /** Number of active credit facilities. */
    @Column(name = "facility_count", nullable = false)
    @Builder.Default
    private Integer facilityCount = 0;

    /** Whether this exposure breaches any cap (flagged for review). */
    @Column(name = "breach_flag", nullable = false)
    @Builder.Default
    private Boolean breachFlag = false;

    /** Breach details (e.g., "Single borrower cap exceeded: 18% vs 15% limit"). */
    @Column(name = "breach_details", length = 500)
    private String breachDetails;

    /** Snapshot business date (one record per borrower per date). */
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    /** Immutable. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (totalExposure == null || totalExposure.compareTo(BigDecimal.ZERO) == 0) {
            totalExposure = fundBasedExposure.add(nonFundBasedExposure);
        }
    }
}
