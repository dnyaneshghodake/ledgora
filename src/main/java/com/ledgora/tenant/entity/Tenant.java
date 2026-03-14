package com.ledgora.tenant.entity;

import com.ledgora.common.enums.DayStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade Multi-tenant entity.
 *
 * <p>Lifecycle: INITIALIZING → ACTIVE (after bootstrap completes). INACTIVE = suspended. Each
 * tenant has its own business date, day status, GL chart, branch set, and user base. No cross-tenant
 * data leakage is possible — all child entities carry a tenant_id FK.
 *
 * <p>RBI alignment: regulatory_code maps to RBI-assigned entity identifier. base_currency enforces
 * INR by default per RBI guidelines. effective_from is the date from which the tenant is legally
 * operational.
 */
@Entity
@Table(
        name = "tenants",
        indexes = {@Index(name = "idx_tenant_code", columnList = "tenant_code", unique = true)})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", length = 20, nullable = false, unique = true)
    private String tenantCode;

    @Column(name = "tenant_name", length = 100, nullable = false)
    private String tenantName;

    /**
     * Lifecycle status: INITIALIZING → ACTIVE → INACTIVE.
     * INITIALIZING is set during bootstrap; no operations are allowed until ACTIVE.
     */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "current_business_date", nullable = false)
    private LocalDate currentBusinessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_status", length = 20, nullable = false)
    @Builder.Default
    private DayStatus dayStatus = DayStatus.OPEN;

    // ── CBS / RBI Regulatory fields ──

    /** ISO 3166-1 alpha-2 country code (e.g. IN for India). */
    @Column(name = "country", length = 5)
    @Builder.Default
    private String country = "IN";

    /** ISO 4217 base currency (e.g. INR). All GL balances are in this currency. */
    @Column(name = "base_currency", length = 5)
    @Builder.Default
    private String baseCurrency = "INR";

    /** IANA timezone identifier (e.g. Asia/Kolkata). */
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    /** RBI-assigned regulatory identifier (e.g. RBI/2024/BANK/001). */
    @Column(name = "regulatory_code", length = 50)
    private String regulatoryCode;

    /** Whether multi-branch operations are enabled for this tenant. */
    @Column(name = "multi_branch_enabled", nullable = false)
    @Builder.Default
    private Boolean multiBranchEnabled = false;

    /**
     * EOD processing status for the current business date.
     * NOT_STARTED → IN_PROGRESS → COMPLETED.
     */
    @Column(name = "eod_status", length = 20)
    @Builder.Default
    private String eodStatus = "NOT_STARTED";

    /** Date from which this tenant is legally operational (RBI activation date). */
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    /** Free-text remarks from the maker at time of tenant creation request. */
    @Column(name = "remarks", length = 500)
    private String remarks;

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
