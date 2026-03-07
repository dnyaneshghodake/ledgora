package com.ledgora.approval.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * PART 3: Maker-Checker approval request entity.
 * High-value transactions require manager approval before execution.
 */
@Entity
@Table(name = "approval_requests", indexes = {
    @Index(name = "idx_approval_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_approval_status", columnList = "status")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "request_data", length = 4000)
    private String requestData;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
