package com.ledgora.product.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade GL mapping for a Product Version. Defines which General Ledger accounts are
 * used for debit, credit, clearing, suspense, and interest accrual operations when
 * transactions are posted against accounts opened under this product.
 *
 * <p>Mappings must exist before a Product can transition to ACTIVE status. This is
 * enforced by ProductValidationService.
 *
 * <p>Immutable once the parent ProductVersion is APPROVED — mirrors the version immutability
 * guarantee.
 */
@Entity
@Table(
        name = "product_gl_mappings",
        indexes = {
            @Index(name = "idx_pglm_version", columnList = "product_version_id")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pglm_version",
                    columnNames = {"product_version_id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductGlMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_version_id", nullable = false)
    private ProductVersion productVersion;

    /** GL code for debit leg of customer transactions (e.g., "1100" Cash for deposits). */
    @Column(name = "dr_gl_code", length = 20, nullable = false)
    private String drGlCode;

    /** GL code for credit leg of customer transactions (e.g., "2110" Savings Deposits). */
    @Column(name = "cr_gl_code", length = 20, nullable = false)
    private String crGlCode;

    /** GL code for inter-branch clearing (nullable — only needed for cross-branch products). */
    @Column(name = "clearing_gl_code", length = 20)
    private String clearingGlCode;

    /** GL code for suspense postings (e.g., unreconciled items, failed reversals). */
    @Column(name = "suspense_gl_code", length = 20, nullable = false)
    private String suspenseGlCode;

    /** GL code for interest accrual (e.g., "5100" Interest Expense for savings products). */
    @Column(name = "interest_accrual_gl_code", length = 20, nullable = false)
    private String interestAccrualGlCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
