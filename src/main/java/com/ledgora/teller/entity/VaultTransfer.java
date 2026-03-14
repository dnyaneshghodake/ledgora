package com.ledgora.teller.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.VaultTransferStatus;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Vault Transfer — tracks cash movement between teller and vault (or vault to teller). Requires
 * dual authorization per RBI branch cash handling guidelines.
 */
@Entity
@Table(
        name = "vault_transfers",
        indexes = {
            @Index(name = "idx_vault_transfer_tenant", columnList = "tenant_id"),
            @Index(name = "idx_vault_transfer_status", columnList = "status"),
            @Index(name = "idx_vault_transfer_session", columnList = "teller_session_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Source entity type: TELLER or VAULT. */
    @Column(name = "from_entity", length = 10, nullable = false)
    private String fromEntity;

    /** Destination entity type: TELLER or VAULT. */
    @Column(name = "to_entity", length = 10, nullable = false)
    private String toEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teller_session_id")
    private TellerSession tellerSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vault_id")
    private VaultMaster vault;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private VaultTransferStatus status = VaultTransferStatus.INITIATED;

    /** First custodian who initiates the transfer. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dual_auth_user1")
    private User dualAuthUser1;

    /** Second custodian who authorizes (or rejects) the transfer. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dual_auth_user2")
    private User dualAuthUser2;

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        initiatedAt = LocalDateTime.now();
    }
}
