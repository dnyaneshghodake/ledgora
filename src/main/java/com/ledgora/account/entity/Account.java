package com.ledgora.account.entity;

import com.ledgora.auth.entity.User;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.FreezeLevel;
import com.ledgora.common.enums.LedgerAccountType;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.customer.entity.Customer;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PART 2: Account entity with fintech-style ledger account support.
 * Supports hierarchical relationships for Chart of Accounts structure.
 * Multi-tenant aware.
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_number", columnList = "account_number"),
    @Index(name = "idx_account_tenant", columnList = "tenant_id")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "account_number", length = 20, nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "account_name", length = 100, nullable = false)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20, nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "balance", precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "branch_code", length = 10)
    private String branchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    // PART 1: Link to Customer entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // CBS: Link to CustomerMaster for CBS-grade customer management
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_master_id")
    private CustomerMaster customerMaster;

    // CBS: Customer number for quick reference
    @Column(name = "customer_number", length = 30)
    private String customerNumber;

    // CBS: Home branch for branch-level isolation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_branch_id")
    private Branch homeBranch;

    @Column(name = "gl_account_code", length = 20)
    private String glAccountCode;

    // PART 3: Fields referenced in JSP forms (account-create, account-edit, account-view)
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private java.math.BigDecimal interestRate;

    @Column(name = "overdraft_limit", precision = 19, scale = 4)
    private java.math.BigDecimal overdraftLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by")
    private User lastModifiedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "ledger_account_type", length = 30)
    private LedgerAccountType ledgerAccountType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private Account parentAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "freeze_level", length = 20, nullable = false)
    @Builder.Default
    private FreezeLevel freezeLevel = FreezeLevel.NONE;

    @Column(name = "freeze_reason", length = 255)
    private String freezeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20, nullable = false)
    @Builder.Default
    private MakerCheckerStatus approvalStatus = MakerCheckerStatus.APPROVED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;

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
