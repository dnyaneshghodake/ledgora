package com.ledgora.tenant.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade Tenant Operation Lock. Prevents concurrent execution of critical
 * batch operations (EOD, Settlement, Reconciliation) for the same tenant.
 *
 * <p>Usage: Before starting EOD or Settlement, acquire the lock row using
 * SELECT ... FOR UPDATE (pessimistic write lock). If another process already
 * holds the lock, the acquisition blocks until the first process commits.
 *
 * <p>One row per tenant — created on first use, never deleted.
 */
@Entity
@Table(
        name = "tenant_operation_locks",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_tol_tenant", columnNames = {"tenant_id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantOperationLock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /** The operation currently holding the lock (EOD, SETTLEMENT, RECONCILIATION, etc.). */
    @Column(name = "locked_by_operation", length = 50)
    private String lockedByOperation;

    /** Username or system process that acquired the lock. */
    @Column(name = "locked_by_user", length = 100)
    private String lockedByUser;

    /** When the lock was acquired. Null if not currently locked. */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    /** When the lock was released. Updated on successful completion. */
    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
