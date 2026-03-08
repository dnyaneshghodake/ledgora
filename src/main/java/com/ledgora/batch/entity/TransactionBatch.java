package com.ledgora.batch.entity;

import com.ledgora.common.enums.BatchStatus;
import com.ledgora.common.enums.BatchType;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import jakarta.persistence.Version;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transaction batch entity for grouping transactions by channel, tenant, and business date.
 */
@Entity
@Table(name = "transaction_batches", indexes = {
    @Index(name = "idx_batch_tenant_type_date", columnList = "tenant_id, batch_type, business_date"),
    @Index(name = "idx_batch_status", columnList = "status")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_type", length = 20, nullable = false)
    private BatchType batchType;

    @Column(name = "batch_code", length = 50)
    private String batchCode;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Version
    @Column(name = "version")
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private BatchStatus status = BatchStatus.OPEN;

    @Column(name = "total_debit", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(name = "total_credit", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(name = "transaction_count", nullable = false)
    @Builder.Default
    private Integer transactionCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
