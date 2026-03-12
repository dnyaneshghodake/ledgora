package com.ledgora.lien.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.LienStatus;
import com.ledgora.common.enums.LienType;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Account Lien entity for lien management. Lien reduces available balance. Creation/release require
 * maker-checker.
 */
@Entity
@Table(
        name = "account_liens",
        indexes = {
            @Index(name = "idx_lien_account", columnList = "account_id"),
            @Index(name = "idx_lien_tenant", columnList = "tenant_id"),
            @Index(name = "idx_lien_status", columnList = "status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "lien_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal lienAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "lien_type", length = 30, nullable = false)
    private LienType lienType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private LienStatus status = LienStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20, nullable = false)
    @Builder.Default
    private MakerCheckerStatus approvalStatus = MakerCheckerStatus.PENDING;

    @Column(name = "lien_reference", length = 50)
    private String lienReference;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    /**
     * Optimistic locking — prevents two checkers from concurrently approving or releasing the
     * same lien. The second write throws ObjectOptimisticLockingFailureException.
     */
    @Version
    @Column(name = "version")
    private Long version;

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
