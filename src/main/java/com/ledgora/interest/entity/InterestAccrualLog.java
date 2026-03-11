package com.ledgora.interest.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Idempotency log for daily interest accrual. One row per (tenant, account, business_date).
 * Prevents double-accrual if EOD is retried. Immutable once created.
 */
@Entity
@Table(
        name = "interest_accrual_log",
        indexes = {
            @Index(name = "idx_ial_tenant_date", columnList = "tenant_id, business_date"),
            @Index(name = "idx_ial_account", columnList = "account_id")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_ial_tenant_account_date",
                    columnNames = {"tenant_id", "account_id", "business_date"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestAccrualLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "account_number", length = 20, nullable = false)
    private String accountNumber;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "balance_used", precision = 19, scale = 4, nullable = false)
    private BigDecimal balanceUsed;

    @Column(name = "annual_rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal annualRate;

    @Column(name = "accrued_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal accruedAmount;

    @Column(name = "dr_gl_code", length = 20, nullable = false)
    private String drGlCode;

    @Column(name = "cr_gl_code", length = 20, nullable = false)
    private String crGlCode;

    @Column(name = "voucher_number", length = 60)
    private String voucherNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
