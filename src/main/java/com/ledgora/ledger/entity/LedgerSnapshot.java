package com.ledgora.ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PART 5: Ledger snapshot for performance optimization.
 * Stores point-in-time balance snapshots per account.
 * Balance queries combine: snapshot_balance + entries after snapshot.
 */
@Entity
@Table(name = "ledger_snapshots", indexes = {
    @Index(name = "idx_snapshot_account_date", columnList = "account_id, snapshot_date")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "snapshot_balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal snapshotBalance;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "last_entry_id", nullable = false)
    private Long lastEntryId;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
