package com.ledgora.transaction.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.EntryType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction line - BUSINESS INTENT layer (not accounting).
 *
 * CBS Layer Separation:
 *   Transaction      = Business event ("what happened")
 *   TransactionLine  = Business intent detail ("debit/credit intent per account")
 *   Voucher          = Accounting instruction (approved before posting)
 *   LedgerEntry      = Immutable financial truth (system-generated from voucher)
 *
 * TransactionLines are created when the transaction is initiated (maker step).
 * They do NOT affect balances. Only LedgerEntries (created via VoucherService) affect balances.
 */
@Entity
@Table(name = "transaction_lines")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", length = 10, nullable = false)
    private EntryType lineType;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
