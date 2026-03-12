package com.ledgora.governance.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade Config Change Request — tracks all configuration parameter changes with maker-checker
 * governance. Used for:
 *
 * <ul>
 *   <li>Product configuration changes (interest rates, GL mappings)
 *   <li>Approval policy threshold changes
 *   <li>Hard transaction ceiling changes
 *   <li>Velocity fraud limit changes
 *   <li>GL hierarchy modifications
 *   <li>Branch configuration changes
 * </ul>
 *
 * <p>Each row captures a before/after snapshot of the configuration parameter value. The change is
 * not applied until a checker approves it. On approval, the calling service reads the {@code
 * newValue} and applies it to the target entity.
 *
 * <p>RBI IT Framework — Change Management:
 *
 * <ul>
 *   <li>All parameter changes require dual control (maker != checker)
 *   <li>Full before/after audit trail for regulatory inspection
 *   <li>Effective dating support for scheduled parameter changes
 *   <li>No direct config mutation — always via change request
 * </ul>
 */
@Entity
@Table(
        name = "config_change_requests",
        indexes = {
            @Index(name = "idx_ccr_tenant", columnList = "tenant_id"),
            @Index(name = "idx_ccr_status", columnList = "status"),
            @Index(name = "idx_ccr_config_type", columnList = "config_type"),
            @Index(name = "idx_ccr_entity", columnList = "target_entity_type, target_entity_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /**
     * Configuration type — categorizes the change for routing and dashboard display. Values:
     * PRODUCT, PRODUCT_VERSION, GL_MAPPING, APPROVAL_POLICY, HARD_LIMIT, VELOCITY_LIMIT,
     * GL_ACCOUNT, BRANCH, CALENDAR, INTEREST_RATE, FX_RATE.
     */
    @Column(name = "config_type", length = 30, nullable = false)
    private String configType;

    /** Target entity type (e.g., "Product", "ApprovalPolicy", "HardTransactionLimit"). */
    @Column(name = "target_entity_type", length = 50, nullable = false)
    private String targetEntityType;

    /** Target entity ID (the record being changed). Null for new record creation. */
    @Column(name = "target_entity_id")
    private Long targetEntityId;

    /** Human-readable description of the change. */
    @Column(name = "change_description", length = 500, nullable = false)
    private String changeDescription;

    /** JSON snapshot of the entity BEFORE the change. Null for new record creation. */
    @Column(name = "old_value", length = 10000)
    private String oldValue;

    /** JSON snapshot of the entity AFTER the change (the proposed new state). */
    @Column(name = "new_value", length = 10000, nullable = false)
    private String newValue;

    /** Specific field being changed (e.g., "interestRate", "maxAmount"). Null for bulk changes. */
    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /** Effective date — when the change should take effect. Null = immediate on approval. */
    @Column(name = "effective_date")
    private java.time.LocalDate effectiveDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "remarks", length = 500)
    private String remarks;

    /**
     * Optimistic locking version — prevents concurrent approval of the same config change from
     * multiple sessions. The second concurrent approve/reject throws OptimisticLockException.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }
}
