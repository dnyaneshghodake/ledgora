package com.ledgora.ledger.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.EntryType;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.transaction.entity.Transaction;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_account_id")
    private GeneralLedger glAccount;

    @Column(name = "gl_account_code", length = 20)
    private String glAccountCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", length = 10, nullable = false)
    private EntryType entryType;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 19, scale = 4, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "business_date")
    private LocalDate businessDate;

    @Column(name = "posting_time")
    private LocalDateTime postingTime;

    @Column(name = "narration", length = 255)
    private String narration;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (postingTime == null) {
            postingTime = LocalDateTime.now();
        }
    }
}
