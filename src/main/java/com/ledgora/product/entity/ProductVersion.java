package com.ledgora.product.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.MakerCheckerStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade Product Version entity. Each Product can have multiple versions over time. Version rows
 * are immutable once approved — no updates allowed, only new versions. This enables temporal
 * product configuration (interest rate changes, fee changes) without losing audit history.
 */
@Entity
@Table(
        name = "product_versions",
        indexes = {
            @Index(name = "idx_pv_product", columnList = "product_id"),
            @Index(name = "idx_pv_status", columnList = "status")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pv_product_version",
                    columnNames = {"product_id", "version_number"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private MakerCheckerStatus status = MakerCheckerStatus.PENDING;

    @Column(name = "change_description", length = 500)
    private String changeDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
