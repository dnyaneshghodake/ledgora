package com.ledgora.account.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade snapshot of product rules captured at account-opening time. Once persisted, this row is
 * immutable — it records the exact product configuration (GL codes, version, type) that was
 * effective when the account was opened.
 *
 * <p>This decouples account behavior from live product changes: if the product's GL mapping changes
 * in a future version, existing accounts retain their original configuration.
 *
 * <p>One-to-one with Account (nullable for legacy accounts opened before the product engine).
 */
@Entity
@Table(
        name = "account_product_snapshots",
        indexes = {
            @Index(name = "idx_aps_account", columnList = "account_id"),
            @Index(name = "idx_aps_product", columnList = "product_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountProductSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_code", length = 20, nullable = false)
    private String productCode;

    @Column(name = "product_name", length = 100, nullable = false)
    private String productName;

    @Column(name = "product_type", length = 20, nullable = false)
    private String productType;

    @Column(name = "product_version_number", nullable = false)
    private Integer productVersionNumber;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    // ── Snapshotted GL codes from ProductGlMapping at time of account opening ──
    @Column(name = "dr_gl_code", length = 20, nullable = false)
    private String drGlCode;

    @Column(name = "cr_gl_code", length = 20, nullable = false)
    private String crGlCode;

    @Column(name = "clearing_gl_code", length = 20)
    private String clearingGlCode;

    @Column(name = "suspense_gl_code", length = 20, nullable = false)
    private String suspenseGlCode;

    @Column(name = "interest_accrual_gl_code", length = 20, nullable = false)
    private String interestAccrualGlCode;

    @Column(name = "snapshot_at", nullable = false, updatable = false)
    private LocalDateTime snapshotAt;

    @PrePersist
    protected void onCreate() {
        snapshotAt = LocalDateTime.now();
    }
}
