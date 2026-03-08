package com.ledgora.tenant.entity;

import com.ledgora.common.enums.DayStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Multi-tenant entity. Each tenant has its own business date and day status.
 */
@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenant_code", columnList = "tenant_code", unique = true)
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", length = 20, nullable = false, unique = true)
    private String tenantCode;

    @Column(name = "tenant_name", length = 100, nullable = false)
    private String tenantName;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "current_business_date", nullable = false)
    private LocalDate currentBusinessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_status", length = 20, nullable = false)
    @Builder.Default
    private DayStatus dayStatus = DayStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
