package com.ledgora.loan.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Credit Limit — sanctioned credit facility for a borrower per RBI exposure norms.
 *
 * <p>CBS Credit Limit Engine rules:
 *
 * <ul>
 *   <li>A borrower may have one or more credit limits (term loan, OD, CC, etc.)
 *   <li>Each limit has a sanctioned amount, utilized amount, and available amount
 *   <li>Loan disbursement must not exceed available limit
 *   <li>Aggregate exposure per borrower must respect RBI single/group borrower caps
 *   <li>Sector-wise exposure caps enforced at tenant level
 *   <li>Limit validity period (sanction date → expiry date)
 * </ul>
 *
 * <p>RBI Master Circular on Exposure Norms:
 *
 * <ul>
 *   <li>Single borrower: 15% of capital funds (20% for infrastructure)
 *   <li>Group borrower: 40% of capital funds (50% for infrastructure)
 *   <li>Sector caps as per bank's internal policy
 * </ul>
 */
@Entity
@Table(
        name = "credit_limits",
        indexes = {
            @Index(name = "idx_cl_tenant", columnList = "tenant_id"),
            @Index(name = "idx_cl_borrower", columnList = "borrower_id"),
            @Index(name = "idx_cl_status", columnList = "status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Unique limit reference number (e.g., CL-TENANT001-20260101-0001). */
    @Column(name = "limit_reference", length = 40, nullable = false, unique = true)
    private String limitReference;

    /** Borrower identifier (customer ID or customer number). */
    @Column(name = "borrower_id", length = 30, nullable = false)
    private String borrowerId;

    /** Borrower name (denormalized for quick display). */
    @Column(name = "borrower_name", length = 100)
    private String borrowerName;

    /** Borrower group identifier (for group exposure tracking). */
    @Column(name = "borrower_group_id", length = 30)
    private String borrowerGroupId;

    /** Facility type: TERM_LOAN, OVERDRAFT, CASH_CREDIT, WORKING_CAPITAL, LC, BG. */
    @Column(name = "facility_type", length = 30, nullable = false)
    private String facilityType;

    /** Sector classification for exposure caps (e.g., AGRICULTURE, INFRASTRUCTURE, MSME). */
    @Column(name = "sector", length = 50)
    private String sector;

    /** Total sanctioned limit amount. */
    @Column(name = "sanctioned_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal sanctionedAmount;

    /** Currently utilized amount (sum of all active disbursements under this limit). */
    @Column(name = "utilized_amount", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal utilizedAmount = BigDecimal.ZERO;

    /** Available amount = sanctioned - utilized. Computed, stored for quick query. */
    @Column(name = "available_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal availableAmount;

    /** Date of limit sanction. */
    @Column(name = "sanction_date", nullable = false)
    private LocalDate sanctionDate;

    /** Limit expiry date. Null means perpetual (until explicit closure). */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /** Status: ACTIVE, EXPIRED, CLOSED, SUSPENDED. */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    /** Currency (defaults to INR per RBI). */
    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    /** Linked loan product (optional — limit may span multiple products). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id")
    private LoanProduct loanProduct;

    /** Sanctioned by (maker). */
    @Column(name = "sanctioned_by", length = 50)
    private String sanctionedBy;

    /** Approved by (checker — maker-checker enforcement). */
    @Column(name = "approved_by", length = 50)
    private String approvedBy;

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
        if (availableAmount == null) {
            availableAmount = sanctionedAmount;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
