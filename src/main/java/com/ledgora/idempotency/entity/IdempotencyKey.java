package com.ledgora.idempotency.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * PART 4: Idempotency key entity for financial operation deduplication.
 * Prevents duplicate transaction processing.
 * Multi-tenant aware.
 */
@Entity
@Table(name = "idempotency_keys", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_idempotency_tenant", columnList = "tenant_id")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class IdempotencyKey {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "idempotency_key", length = 255, nullable = false, unique = true)
    private String key;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "response_hash", length = 64)
    private String responseHash;

    @Column(name = "response_body", length = 4000)
    private String responseBody;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "PROCESSING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
