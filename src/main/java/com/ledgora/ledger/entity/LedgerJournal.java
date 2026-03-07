package com.ledgora.ledger.entity;

import com.ledgora.transaction.entity.Transaction;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ledger_journals")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerJournal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LedgerEntry> entries = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
