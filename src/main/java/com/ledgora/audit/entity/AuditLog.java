package com.ledgora.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * PART 11: Extended audit log with full financial traceability.
 * Each transaction stores request payload, response payload, user id, ip address, and timestamp.
 */
@Entity
@Table(name = "audit_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
