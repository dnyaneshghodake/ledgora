package com.ledgora.voucher.entity;

import com.ledgora.account.entity.Account;
import com.ledgora.auth.entity.User;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.VoucherDrCr;
import com.ledgora.common.enums.VoucherStatus;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.transaction.entity.Transaction;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CBS Voucher Entity - controls the full lifecycle of a financial posting.
 * Vouchers flow: Create -> Authorize -> Post -> (optional Cancel via reversal).
 * NO DELETE allowed. Immutability enforced via cancel/reversal pattern.
 *
 * Composite index: (tenant_id, branch_id, posting_date, batch_code, scroll_no)
 */
@Entity
@Table(name = "vouchers", indexes = {
    @Index(name = "idx_voucher_composite", columnList = "tenant_id, branch_id, posting_date, batch_code, scroll_no"),
    @Index(name = "idx_voucher_number", columnList = "voucher_number", unique = true),
    @Index(name = "idx_voucher_tenant", columnList = "tenant_id"),
    @Index(name = "idx_voucher_tenant_date", columnList = "tenant_id, posting_date"),
    @Index(name = "idx_voucher_status_flags", columnList = "auth_flag, post_flag, cancel_flag"),
    @Index(name = "idx_voucher_branch", columnList = "branch_id"),
    @Index(name = "idx_voucher_posting_date", columnList = "posting_date"),
    @Index(name = "idx_voucher_account", columnList = "account_id"),
    @Index(name = "idx_voucher_batch", columnList = "batch_id"),
    @Index(name = "idx_voucher_transaction", columnList = "transaction_id")
})
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Voucher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Formatted voucher number: <TENANT_CODE>-<BRANCH_CODE>-<YYYYMMDD>-<6-digit scroll>.
     * Generated on create by VoucherService. Unique, indexed.
     */
    @Column(name = "voucher_number", length = 60, unique = true)
    private String voucherNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /**
     * FK to the originating transaction. A single transaction produces multiple vouchers (DR + CR legs).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    // Batch references
    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "batch_code", length = 30, nullable = false)
    private String batchCode;

    @Column(name = "set_no", nullable = false)
    @Builder.Default
    private Integer setNo = 1;

    @Column(name = "scroll_no", nullable = false)
    private Long scrollNo;

    // Date fields
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    // Debit/Credit indicator
    @Enumerated(EnumType.STRING)
    @Column(name = "dr_cr", length = 5, nullable = false)
    private VoucherDrCr drCr;

    // Account references
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gl_account_id")
    private GeneralLedger glAccount;

    // Amount fields
    @Column(name = "transaction_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal transactionAmount;

    @Column(name = "local_currency_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal localCurrencyAmount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    // Maker-Checker
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maker_id", nullable = false)
    private User maker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checker_id")
    private User checker;

    // Flags
    @Column(name = "auth_flag", length = 1, nullable = false)
    @Builder.Default
    private String authFlag = "N";

    @Column(name = "post_flag", length = 1, nullable = false)
    @Builder.Default
    private String postFlag = "N";

    @Column(name = "cancel_flag", length = 1, nullable = false)
    @Builder.Default
    private String cancelFlag = "N";

    @Column(name = "financial_effect_flag", length = 1, nullable = false)
    @Builder.Default
    private String financialEffectFlag = "Y";

    // Link to posted ledger entry
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_entry_id")
    private LedgerEntry ledgerEntry;

    // Narration
    @Column(name = "narration", length = 500)
    private String narration;

    // Reversal reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_voucher_id")
    private Voucher reversalOfVoucher;

    /**
     * Total debit amount for this voucher leg (equals transactionAmount when drCr == DR, else ZERO).
     * Populated on create for quick batch/EOD aggregation queries.
     */
    @Column(name = "total_debit", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalDebit = BigDecimal.ZERO;

    /**
     * Total credit amount for this voucher leg (equals transactionAmount when drCr == CR, else ZERO).
     * Populated on create for quick batch/EOD aggregation queries.
     */
    @Column(name = "total_credit", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalCredit = BigDecimal.ZERO;

    /**
     * RBI-F9: Separate field for authorization remarks (do NOT mutate narration after creation).
     */
    @Column(name = "authorization_remarks", length = 500)
    private String authorizationRemarks;

    /**
     * RBI-F11: Optimistic locking — prevents concurrent authorize/post race conditions.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Derive VoucherStatus from ground-truth flags.
     * This is NOT persisted — the triple (authFlag, postFlag, cancelFlag) is the source of truth.
     */
    @Transient
    public VoucherStatus getStatus() {
        return VoucherStatus.fromFlags(authFlag, postFlag, cancelFlag);
    }

    // ──────────────────────────────────────────────────────────────────────
    // RBI-F3: Controlled setters — only fields that may change post-creation.
    // Financial fields (amount, drCr, account, postingDate, etc.) have NO setters.
    // ──────────────────────────────────────────────────────────────────────

    public void setAuthFlag(String authFlag) { this.authFlag = authFlag; }
    public void setPostFlag(String postFlag) { this.postFlag = postFlag; }
    public void setCancelFlag(String cancelFlag) { this.cancelFlag = cancelFlag; }
    public void setChecker(User checker) { this.checker = checker; }
    public void setLedgerEntry(LedgerEntry ledgerEntry) { this.ledgerEntry = ledgerEntry; }
    public void setAuthorizationRemarks(String authorizationRemarks) { this.authorizationRemarks = authorizationRemarks; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Auto-populate totalDebit/totalCredit from drCr + transactionAmount
        if (transactionAmount != null && drCr != null) {
            if (drCr == VoucherDrCr.DR) {
                totalDebit = transactionAmount;
                totalCredit = BigDecimal.ZERO;
            } else {
                totalDebit = BigDecimal.ZERO;
                totalCredit = transactionAmount;
            }
        }
    }

    /**
     * RBI-F3: Guard against mutation of POSTED vouchers.
     * After posting, only cancelFlag may transition (to 'Y' via reversal).
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
