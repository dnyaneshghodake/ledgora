package com.ledgora.ledger.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.EntryType;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.transaction.entity.Transaction;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable ledger entry - the ultimate source of financial truth.
 *
 * <p>CBS Golden Rules enforced: - NEVER updated (Hibernate @Immutable prevents any UPDATE SQL) -
 * NEVER deleted (corrections via reversal entries only) - Each entry references a LedgerJournal for
 * grouped double-entry posting - Each entry references the originating Voucher for audit trail -
 * Multi-tenant aware
 *
 * <p>Balance derivation: SUM(credits) - SUM(debits) per account = true balance. The account.balance
 * field is a PERFORMANCE CACHE only, validated by LedgerValidatorService.
 */
@Entity
@org.hibernate.annotations.Immutable
@Table(
        name = "ledger_entries",
        indexes = {
            @Index(
                    name = "idx_ledger_entry_account_created",
                    columnList = "account_id, created_at"),
            @Index(name = "idx_ledger_entry_journal", columnList = "journal_id"),
            @Index(name = "idx_ledger_entry_tenant", columnList = "tenant_id")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private LedgerJournal journal;

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

    /** Reference to the voucher that generated this entry (audit trail: Voucher → LedgerEntry). */
    @Column(name = "voucher_id")
    private Long voucherId;

    /** If this entry is a reversal, references the original entry being reversed. */
    @Column(name = "reversal_of_entry_id")
    private Long reversalOfEntryId;

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
