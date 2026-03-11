package com.ledgora.audit.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * PART 11: Extended audit log with full financial traceability. Each transaction stores request
 * payload, response payload, user id, ip address, and timestamp.
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "entity", length = 50, nullable = false)
    private String entity;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // PART 9: User agent for audit trail
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // PART 11: Extended audit fields
    @Column(name = "request_payload", length = 4000)
    private String requestPayload;

    @Column(name = "response_payload", length = 4000)
    private String responsePayload;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "request_uri", length = 500)
    private String requestUri;

    // Enhanced audit trail fields (CBS compliance)

    /** Old value before the change (JSON or text snapshot). Null for create operations. */
    @Column(name = "old_value", length = 4000)
    private String oldValue;

    /** New value after the change (JSON or text snapshot). */
    @Column(name = "new_value", length = 4000)
    private String newValue;

    /** Batch ID linkage for transaction audit traceability. */
    @Column(name = "batch_id")
    private Long batchId;

    /** Tenant ID for multi-tenant audit isolation. */
    @Column(name = "tenant_id")
    private Long tenantId;

    /**
     * SHA-256 hash of (previousHash + serialized payload). Forms an immutable
     * chain per tenant — any tampering with historical entries breaks the chain.
     * Computed by AuditService before persist (not in @PrePersist because it
     * requires a DB lookup for the previous hash).
     */
    @Column(name = "hash", length = 64)
    private String hash;

    /** Hash of the immediately preceding audit log entry for the same tenant. */
    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
