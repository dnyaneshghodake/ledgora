package com.ledgora.ledger.entity;

import com.ledgora.tenant.entity.Tenant;
import com.ledgora.transaction.entity.Transaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable ledger journal - groups ledger entries for a single transaction posting.
 * Once created, journals and their entries must never be modified.
 * Corrections are made via reversal transactions only.
 */
@Entity
@org.hibernate.annotations.Immutable
@Table(name = "ledger_journals", indexes = {
    @Index(name = "idx_ledger_journal_tenant", columnList = "tenant_id")
})
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerJournal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "journal", fetch = FetchType.LAZY)
    @Builder.Default
    private List<LedgerEntry> entries = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
