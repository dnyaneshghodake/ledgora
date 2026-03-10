package com.ledgora.fraud.entity;

import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fraud alert raised when velocity or other fraud detection rules are triggered.
 *
 * <p>RBI Fraud Risk Management: Each alert is an immutable record that forms the compliance evidence
 * trail. Alerts are never deleted — only resolved via status update.
 */
@Entity
@Table(
        name = "fraud_alerts",
        indexes = {
            @Index(name = "idx_fa_tenant", columnList = "tenant_id"),
            @Index(name = "idx_fa_account", columnList = "account_id"),
            @Index(name = "idx_fa_status", columnList = "status"),
            @Index(name = "idx_fa_alert_type", columnList = "alert_type")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "account_number", length = 50, nullable = false)
    private String accountNumber;

    /** VELOCITY_COUNT, VELOCITY_AMOUNT, RAPID_REVERSAL, etc. */
    @Column(name = "alert_type", length = 50, nullable = false)
    private String alertType;

    /** OPEN, ACKNOWLEDGED, RESOLVED, FALSE_POSITIVE */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "details", length = 2000, nullable = false)
    private String details;

    /** Observed count in the window at time of alert. */
    @Column(name = "observed_count")
    private Integer observedCount;

    /** Observed amount in the window at time of alert. */
    @Column(name = "observed_amount", precision = 19, scale = 4)
    private BigDecimal observedAmount;

    /** Configured threshold that was breached. */
    @Column(name = "threshold_value", length = 100)
    private String thresholdValue;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
