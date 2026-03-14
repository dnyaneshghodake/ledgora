package com.ledgora.voucher.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.balance.service.CbsBalanceService;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.repository.TransactionBatchRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.BatchStatus;
import com.ledgora.common.enums.EntryType;
import com.ledgora.common.enums.VoucherDrCr;
import com.ledgora.customer.service.CbsCustomerValidationService;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.gl.service.GlBalanceService;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.entity.LedgerJournal;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.ledger.repository.LedgerJournalRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.repository.TransactionRepository;
import com.ledgora.voucher.entity.ScrollSequence;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.repository.ScrollSequenceRepository;
import com.ledgora.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS Voucher Service - controls the full lifecycle of vouchers.
 *
 * <p>Lifecycle: createVoucher -> authorizeVoucher -> postVoucher -> (cancelVoucher)
 *
 * <p>Rules: - On create: update shadow balance only - On authorize: set auth_flag = Y - On post:
 * create LedgerJournal + LedgerEntry (immutable), update actual balance, update GL balance, reduce
 * shadow delta, mark post_flag = Y - On cancel: create reversal voucher, create reversal ledger
 * entry, mark cancel_flag = Y - NO DELETE allowed
 */
@Service
public class VoucherService {

    private static final Logger log = LoggerFactory.getLogger(VoucherService.class);

    private final VoucherRepository voucherRepository;
    private final ScrollSequenceRepository scrollSequenceRepository;
    private final AccountRepository accountRepository;
    private final GeneralLedgerRepository glRepository;
    private final LedgerJournalRepository journalRepository;
    private final LedgerEntryRepository entryRepository;
    private final TransactionRepository transactionRepository;
    private final CbsBalanceService cbsBalanceEngine;
    private final TransactionBatchRepository transactionBatchRepository;
    private final CbsCustomerValidationService customerValidationService;
    private final GlBalanceService glBalanceService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public VoucherService(
            VoucherRepository voucherRepository,
            ScrollSequenceRepository scrollSequenceRepository,
            AccountRepository accountRepository,
            GeneralLedgerRepository glRepository,
            LedgerJournalRepository journalRepository,
            LedgerEntryRepository entryRepository,
            TransactionRepository transactionRepository,
            CbsBalanceService cbsBalanceEngine,
            TransactionBatchRepository transactionBatchRepository,
            CbsCustomerValidationService customerValidationService,
            GlBalanceService glBalanceService,
            AuditService auditService,
            UserRepository userRepository) {
        this.voucherRepository = voucherRepository;
        this.scrollSequenceRepository = scrollSequenceRepository;
        this.accountRepository = accountRepository;
        this.glRepository = glRepository;
        this.journalRepository = journalRepository;
        this.entryRepository = entryRepository;
        this.transactionRepository = transactionRepository;
        this.cbsBalanceEngine = cbsBalanceEngine;
        this.transactionBatchRepository = transactionBatchRepository;
        this.customerValidationService = customerValidationService;
        this.glBalanceService = glBalanceService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * Create a new voucher. On create, only shadow balance is updated. Backward-compatible overload
     * without Transaction FK.
     */
    @Transactional
    public Voucher createVoucher(
            Tenant tenant,
            Branch branch,
            Account account,
            GeneralLedger glAccount,
            VoucherDrCr drCr,
            BigDecimal transactionAmount,
            BigDecimal localCurrencyAmount,
            String currency,
            LocalDate postingDate,
            LocalDate valueDate,
            String batchCode,
            Integer setNo,
            User maker,
            String narration) {
        return createVoucher(
                tenant,
                branch,
                account,
                glAccount,
                drCr,
                transactionAmount,
                localCurrencyAmount,
                currency,
                postingDate,
                valueDate,
                batchCode,
                setNo,
                maker,
                narration,
                null);
    }

    /**
     * Create a new voucher linked to an originating transaction. On create, only shadow balance is
     * updated.
     *
     * <p>Voucher number format: <TENANT_CODE>-<BRANCH_CODE>-<YYYYMMDD>-<6-digit scroll>
     *
     * @param linkedTransaction optional FK to the originating Transaction
     */
    @Transactional
    public Voucher createVoucher(
            Tenant tenant,
            Branch branch,
            Account account,
            GeneralLedger glAccount,
            VoucherDrCr drCr,
            BigDecimal transactionAmount,
            BigDecimal localCurrencyAmount,
            String currency,
            LocalDate postingDate,
            LocalDate valueDate,
            String batchCode,
            Integer setNo,
            User maker,
            String narration,
            com.ledgora.transaction.entity.Transaction linkedTransaction) {

        assertTenantContext(tenant.getId());

        // ── CBS-grade voucher amount validation ──
        if (transactionAmount == null || transactionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.ledgora.common.exception.AccountingException(
                    "INVALID_VOUCHER_AMOUNT", "Voucher amount must be positive");
        }
        if (transactionAmount.scale() > 4) {
            throw new com.ledgora.common.exception.AccountingException(
                    "INVALID_VOUCHER_AMOUNT", "Voucher amount must have at most 4 decimal places");
        }
        if (drCr == null) {
            throw new com.ledgora.common.exception.AccountingException(
                    "INVALID_VOUCHER", "Voucher DR/CR direction is required");
        }
        if (maker == null) {
            throw new com.ledgora.common.exception.AccountingException(
                    "INVALID_VOUCHER", "Maker user is required for voucher creation");
        }

        // Validate voucher business_date matches tenant current business date
        if (!postingDate.equals(tenant.getCurrentBusinessDate())) {
            throw new RuntimeException(
                    "Voucher posting date "
                            + postingDate
                            + " does not match tenant business date "
                            + tenant.getCurrentBusinessDate());
        }

        // Validate customer and account
        customerValidationService.validateAccountForTransaction(
                account, tenant.getId(), branch.getId(), drCr);

        // Validate sufficient balance for debit (skip for GL/internal accounts — they are contra
        // legs)
        if (drCr == VoucherDrCr.DR
                && account.getAccountType() != com.ledgora.common.enums.AccountType.GL_ACCOUNT
                && account.getAccountType()
                        != com.ledgora.common.enums.AccountType.INTERNAL_ACCOUNT) {
            cbsBalanceEngine.validateSufficientBalance(account.getId(), transactionAmount);
        }

        // Get next scroll number (concurrency-safe via PESSIMISTIC_WRITE)
        Long scrollNo = getNextScrollNo(tenant.getId(), branch.getId(), postingDate);

        // Generate formatted voucher number
        String voucherNumber =
                generateVoucherNumber(
                        tenant.getTenantCode(), branch.getBranchCode(), postingDate, scrollNo);

        Voucher voucher =
                Voucher.builder()
                        .voucherNumber(voucherNumber)
                        .tenant(tenant)
                        .branch(branch)
                        .transaction(linkedTransaction)
                        .account(account)
                        .glAccount(glAccount)
                        .drCr(drCr)
                        .transactionAmount(transactionAmount)
                        .localCurrencyAmount(localCurrencyAmount)
                        .currency(currency != null ? currency : "INR")
                        .entryDate(LocalDate.now())
                        .postingDate(postingDate)
                        .valueDate(valueDate != null ? valueDate : postingDate)
                        .effectiveDate(postingDate)
                        .batchCode(batchCode)
                        .setNo(setNo != null ? setNo : 1)
                        .scrollNo(scrollNo)
                        .maker(maker)
                        .authFlag("N")
                        .postFlag("N")
                        .cancelFlag("N")
                        .financialEffectFlag("Y")
                        .narration(narration)
                        .build();

        voucher = voucherRepository.save(voucher);

        // Update shadow balance only on create
        cbsBalanceEngine.updateShadowBalance(account.getId(), transactionAmount, drCr);

        log.info(
                "Voucher created: voucherNo={}, id={}, account={}, dr_cr={}, amount={}, scroll={}, txn={}",
                voucherNumber,
                voucher.getId(),
                account.getAccountNumber(),
                drCr,
                transactionAmount,
                scrollNo,
                linkedTransaction != null ? linkedTransaction.getTransactionRef() : "N/A");

        // RBI-F7: Persistent audit trail for voucher creation
        auditService.logEvent(
                maker.getId(),
                "VOUCHER_CREATED",
                "VOUCHER",
                voucher.getId(),
                "Voucher "
                        + voucherNumber
                        + " created: "
                        + drCr
                        + " "
                        + currency
                        + " "
                        + transactionAmount
                        + " account="
                        + account.getAccountNumber()
                        + " maker="
                        + maker.getUsername(),
                null);

        return voucher;
    }

    /**
     * Atomically create a DR+CR voucher pair within a single transaction. If either leg fails, both
     * are rolled back — no orphaned half-pairs.
     *
     * @return array of [drVoucher, crVoucher]
     */
    @Transactional
    public Voucher[] createVoucherPair(
            Tenant tenant,
            Branch debitBranch,
            Account debitAccount,
            GeneralLedger debitGl,
            Branch creditBranch,
            Account creditAccount,
            GeneralLedger creditGl,
            BigDecimal amount,
            String currency,
            LocalDate businessDate,
            String batchCode,
            User maker,
            String drNarration,
            String crNarration) {
        Voucher drVoucher =
                createVoucher(
                        tenant,
                        debitBranch,
                        debitAccount,
                        debitGl,
                        VoucherDrCr.DR,
                        amount,
                        amount,
                        currency,
                        businessDate,
                        businessDate,
                        batchCode,
                        1,
                        maker,
                        drNarration);
        Voucher crVoucher =
                createVoucher(
                        tenant,
                        creditBranch,
                        creditAccount,
                        creditGl,
                        VoucherDrCr.CR,
                        amount,
                        amount,
                        currency,
                        businessDate,
                        businessDate,
                        batchCode,
                        1,
                        maker,
                        crNarration);
        return new Voucher[] {drVoucher, crVoucher};
    }

    /**
     * Generate formatted voucher number: <TENANT_CODE>-<BRANCH_CODE>-<YYYYMMDD>-<6-digit scroll>.
     * Example: TENANT-001-HQ001-20250130-000001
     */
    private String generateVoucherNumber(
            String tenantCode, String branchCode, LocalDate postingDate, Long scrollNo) {
        String dateStr = postingDate.toString().replace("-", "");
        return String.format("%s-%s-%s-%06d", tenantCode, branchCode, dateStr, scrollNo);
    }

    /**
     * Authorize a voucher. Sets auth_flag = Y. Checker must not be null and must differ from maker
     * (maker-checker enforcement).
     */
    @Transactional
    public Voucher authorizeVoucher(Long voucherId, User checker) {
        Long tenantId = requireTenantId();
        Voucher voucher =
                voucherRepository
                        .findByIdAndTenantId(voucherId, tenantId)
                        .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));

        if ("Y".equals(voucher.getCancelFlag())) {
            throw new RuntimeException("Cannot authorize a cancelled voucher: " + voucherId);
        }
        if ("Y".equals(voucher.getAuthFlag())) {
            throw new RuntimeException("Voucher already authorized: " + voucherId);
        }
        if (checker == null) {
            throw new RuntimeException(
                    "Checker must not be null for voucher authorization: " + voucherId);
        }
        if (voucher.getMaker() != null && voucher.getMaker().getId().equals(checker.getId())) {
            throw new RuntimeException(
                    "Maker and checker cannot be the same user for voucher: " + voucherId);
        }

        voucher.setAuthFlag("Y");
        voucher.setChecker(checker);
        voucher = voucherRepository.save(voucher);

        log.info("Voucher authorized: id={}, checker={}", voucherId, checker.getUsername());

        // RBI-F7: Audit trail for authorization
        auditService.logEvent(
                checker.getId(),
                "VOUCHER_AUTHORIZED",
                "VOUCHER",
                voucher.getId(),
                "Voucher "
                        + voucher.getVoucherNumber()
                        + " authorized by checker="
                        + checker.getUsername()
                        + " (maker="
                        + (voucher.getMaker() != null ? voucher.getMaker().getUsername() : "N/A")
                        + ")",
                null);

        return voucher;
    }

    /**
     * RBI-F4/F9: System auto-authorize a voucher for straight-through processing (e.g. teller
     * deposits).
     *
     * <p>Uses the dedicated SYSTEM_AUTO pseudo-user as checker to ensure maker != checker in the
     * audit trail. SYSTEM_AUTO MUST be seeded in DataInitializer.
     *
     * <p>GOVERNANCE: If SYSTEM_AUTO is not found, posting is BLOCKED with GovernanceException. No
     * fallback to maker. No silent degradation. Fail fast.
     */
    @Transactional
    public Voucher systemAuthorizeVoucher(Long voucherId, User maker) {
        Long tenantId = requireTenantId();
        Voucher voucher =
                voucherRepository
                        .findByIdAndTenantId(voucherId, tenantId)
                        .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));

        if ("Y".equals(voucher.getCancelFlag())) {
            throw new RuntimeException("Cannot authorize a cancelled voucher: " + voucherId);
        }
        if ("Y".equals(voucher.getAuthFlag())) {
            throw new RuntimeException("Voucher already authorized: " + voucherId);
        }

        // RBI-F4: Resolve SYSTEM_AUTO — NO FALLBACK. Fail fast if not configured.
        User checker =
                userRepository
                        .findByUsername("SYSTEM_AUTO")
                        .orElseThrow(
                                () ->
                                        new com.ledgora.common.exception.GovernanceException(
                                                "SYSTEM_AUTO_MISSING",
                                                "SYSTEM_AUTO user not configured. Posting blocked. "
                                                        + "Seed SYSTEM_AUTO with ROLE_SYSTEM in DataInitializer."));

        voucher.setAuthFlag("Y");
        voucher.setChecker(checker);
        // RBI-F9: Authorization metadata goes to dedicated field, narration stays immutable
        voucher.setAuthorizationRemarks(
                "SYSTEM_AUTO_AUTHORIZED by "
                        + checker.getUsername()
                        + " for maker="
                        + (maker != null ? maker.getUsername() : "N/A")
                        + " at "
                        + LocalDateTime.now());
        voucher = voucherRepository.save(voucher);

        log.info(
                "Voucher system-auto-authorized: id={}, checker=SYSTEM_AUTO, maker={}",
                voucherId,
                maker != null ? maker.getUsername() : "N/A");

        return voucher;
    }

    /** Post a voucher. Creates LedgerJournal + LedgerEntry, updates actual balance and GL. */
    @Transactional
    public Voucher postVoucher(Long voucherId) {
        return postVoucher(voucherId, null);
    }

    /**
     * Post a voucher and optionally link ledger entries to an existing transaction.
     *
     * <p>ACCOUNTING MODEL: Each Voucher is a SINGLE accounting leg (DR or CR, never both). DR==CR
     * balance is enforced at the PAIR level: - TransactionService creates matched DR+CR vouchers
     * with equal amounts - createVoucherPair() wraps both in a single @Transactional - EOD
     * validates SUM(debits) == SUM(credits) at ledger level
     *
     * <p>VOUCHER INTEGRITY (RBI-CRITICAL): Before any ledger entry is persisted, this method
     * validates: - drCr is not null (direction must be explicit) - amount > 0 (zero/negative
     * amounts are corrupt) If either fails, AccountingException is thrown and @Transactional rolls
     * back.
     *
     * <p>TENANT SAFETY (RBI-F1): - Voucher fetched with tenant isolation (findByIdAndTenantId) -
     * Voucher businessDate validated against tenant.currentBusinessDate - Tenant dayStatus
     * validated as OPEN
     */
    @Transactional
    public Voucher postVoucher(Long voucherId, Transaction linkedTransaction) {
        Long tenantId = requireTenantId();

        // ── TENANT ISOLATION: fetch with tenant filter first ──
        voucherRepository
                .findByIdAndTenantId(voucherId, tenantId)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));
        Voucher voucher =
                voucherRepository
                        .findByIdWithLock(voucherId)
                        .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));

        // ── TENANT SAFETY: validate business date and day status ──
        Tenant tenant = voucher.getTenant();
        if (tenant.getDayStatus() != com.ledgora.common.enums.DayStatus.OPEN) {
            throw new RuntimeException(
                    "Cannot post voucher "
                            + voucherId
                            + ": tenant business day is "
                            + tenant.getDayStatus()
                            + ". Posting only allowed when day status is OPEN.");
        }
        if (!voucher.getPostingDate().equals(tenant.getCurrentBusinessDate())) {
            throw new RuntimeException(
                    "Cannot post voucher "
                            + voucherId
                            + ": posting date "
                            + voucher.getPostingDate()
                            + " does not match tenant business date "
                            + tenant.getCurrentBusinessDate());
        }

        // ── VOUCHER STATE GUARDS ──
        if ("Y".equals(voucher.getCancelFlag())) {
            throw new RuntimeException("Cannot post a cancelled voucher: " + voucherId);
        }
        if (!"Y".equals(voucher.getAuthFlag())) {
            throw new RuntimeException("Voucher must be authorized before posting: " + voucherId);
        }
        if ("Y".equals(voucher.getPostFlag())) {
            throw new RuntimeException("Voucher already posted: " + voucherId);
        }

        ensureBatchIsOpen(voucher);

        Account account = voucher.getAccount();
        BigDecimal amount = voucher.getTransactionAmount();
        VoucherDrCr drCr = voucher.getDrCr();

        // ── VOUCHER INTEGRITY ASSERTION (RBI-CRITICAL) ──
        // Each voucher is a single accounting leg (DR xor CR). The DR==CR balance
        // is enforced at the PAIR level: TransactionService always creates matched
        // DR+CR vouchers with equal amounts, and createVoucherPair() is atomic.
        //
        // Here we validate the leg itself is well-formed:
        //   - drCr must not be null
        //   - amount must be positive (> 0)
        // If either is violated, the voucher is corrupt and must not post.
        if (drCr == null) {
            throw new com.ledgora.common.exception.AccountingException(
                    "INVALID_VOUCHER",
                    "Voucher " + voucherId + " has null DR/CR direction — cannot post.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.ledgora.common.exception.AccountingException(
                    "INVALID_VOUCHER_AMOUNT",
                    "Voucher "
                            + voucherId
                            + " has zero or negative amount ("
                            + amount
                            + ") — cannot post.");
        }

        // ── All validations passed. Now persist immutable ledger entries. ──

        // Find or create a transaction reference for this voucher
        Transaction transaction = findOrCreateTransaction(voucher, linkedTransaction);

        // Create LedgerJournal
        LedgerJournal journal =
                LedgerJournal.builder()
                        .transaction(transaction)
                        .tenant(tenant)
                        .description(
                                "Voucher posting: "
                                        + voucher.getId()
                                        + " - "
                                        + voucher.getNarration())
                        .businessDate(voucher.getPostingDate())
                        .build();
        journal = journalRepository.save(journal);

        // Determine entry type and GL posting direction
        EntryType accountEntryType = drCr == VoucherDrCr.DR ? EntryType.DEBIT : EntryType.CREDIT;
        EntryType glEntryType = drCr == VoucherDrCr.DR ? EntryType.CREDIT : EntryType.DEBIT;

        // Create immutable LedgerEntry for account side
        // voucherId must be set at build time because LedgerEntry is @Immutable (no updates)
        BigDecimal balanceAfter = calculateBalanceAfter(account.getId(), amount, drCr);
        LedgerEntry accountEntry =
                LedgerEntry.builder()
                        .journal(journal)
                        .transaction(transaction)
                        .tenant(tenant)
                        .account(account)
                        .glAccount(voucher.getGlAccount())
                        .glAccountCode(
                                voucher.getGlAccount() != null
                                        ? voucher.getGlAccount().getGlCode()
                                        : null)
                        .entryType(accountEntryType)
                        .amount(amount)
                        .balanceAfter(balanceAfter)
                        .currency(voucher.getCurrency())
                        .businessDate(voucher.getPostingDate())
                        .postingTime(LocalDateTime.now())
                        .narration(voucher.getNarration())
                        .voucherId(voucher.getId())
                        .build();
        accountEntry = entryRepository.save(accountEntry);

        // Update actual balance (also reduces shadow delta)
        cbsBalanceEngine.updateActualBalance(account.getId(), amount, drCr);

        // Update GL balance (Phase 4: GL Update Enforcement)
        if (voucher.getGlAccount() != null) {
            BigDecimal glDebit = glEntryType == EntryType.DEBIT ? amount : BigDecimal.ZERO;
            BigDecimal glCredit = glEntryType == EntryType.CREDIT ? amount : BigDecimal.ZERO;
            glBalanceService.updateGlBalance(voucher.getGlAccount(), glDebit, glCredit);
        }

        // ── FIX-10: Journal balance validation (double-entry integrity assertion) ──
        // After persisting the ledger entry, validate that this journal's cumulative
        // entries are balanced (sum of debits == sum of credits). This is a post-persist
        // safety net — the pair-level balance is enforced by TransactionService, but this
        // catches any corruption at the journal level.
        java.math.BigDecimal journalDebits =
                entryRepository.sumDebitsByTransactionId(transaction.getId());
        java.math.BigDecimal journalCredits =
                entryRepository.sumCreditsByTransactionId(transaction.getId());
        if (journalDebits != null
                && journalCredits != null
                && journalDebits.compareTo(journalCredits) != 0) {
            log.warn(
                    "JOURNAL IMBALANCE DETECTED for txn {}: debits={} credits={}. "
                            + "This will be caught by EOD validation.",
                    transaction.getTransactionRef(),
                    journalDebits,
                    journalCredits);
        }

        // Mark voucher as posted
        voucher.setPostFlag("Y");
        voucher.setLedgerEntry(accountEntry);
        voucher = voucherRepository.save(voucher);

        log.info(
                "Voucher posted: id={}, journal={}, ledger_entry={}, account={}, amount={}",
                voucherId,
                journal.getId(),
                accountEntry.getId(),
                account.getAccountNumber(),
                amount);

        // RBI-F7: Audit trail for posting
        Long checkerId = voucher.getChecker() != null ? voucher.getChecker().getId() : null;
        auditService.logEvent(
                checkerId,
                "VOUCHER_POSTED",
                "VOUCHER",
                voucher.getId(),
                "Voucher "
                        + voucher.getVoucherNumber()
                        + " posted: journal="
                        + journal.getId()
                        + " ledgerEntry="
                        + accountEntry.getId()
                        + " "
                        + drCr
                        + " "
                        + amount
                        + " account="
                        + account.getAccountNumber(),
                null);

        return voucher;
    }

    /**
     * Cancel a voucher. Creates a reversal voucher and reversal ledger entry. Marks original
     * cancel_flag = Y.
     *
     * <p>RBI-F6: Validates that tenant business day is OPEN and that the voucher's posting date
     * matches the current business date (no backdated reversals without a separate back-value
     * workflow).
     */
    @Transactional
    public Voucher cancelVoucher(Long voucherId, User cancelledBy, String reason) {
        Long tenantId = requireTenantId();
        Voucher original =
                voucherRepository
                        .findByIdAndTenantId(voucherId, tenantId)
                        .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));

        if ("Y".equals(original.getCancelFlag())) {
            throw new RuntimeException("Voucher already cancelled: " + voucherId);
        }

        // RBI-F6: Block cancellation if business day is not OPEN
        com.ledgora.tenant.entity.Tenant tenant = original.getTenant();
        if (tenant.getDayStatus() != com.ledgora.common.enums.DayStatus.OPEN) {
            throw new RuntimeException(
                    "Cannot cancel voucher "
                            + voucherId
                            + ": tenant business day is "
                            + tenant.getDayStatus()
                            + ". Cancellations only allowed when day status is OPEN.");
        }
        // RBI-F6: Block backdated reversal — voucher posting date must equal current business date
        if (!original.getPostingDate().equals(tenant.getCurrentBusinessDate())) {
            throw new RuntimeException(
                    "Cannot cancel voucher "
                            + voucherId
                            + ": posting date "
                            + original.getPostingDate()
                            + " does not match current business date "
                            + tenant.getCurrentBusinessDate()
                            + ". Backdated reversals require a separate back-value workflow.");
        }

        // Determine reversal direction
        VoucherDrCr reversalDrCr =
                original.getDrCr() == VoucherDrCr.DR ? VoucherDrCr.CR : VoucherDrCr.DR;

        Voucher reversal;

        if ("Y".equals(original.getPostFlag())) {
            // Original was posted — create a full reversal voucher and auto-post it
            Long scrollNo =
                    getNextScrollNo(
                            original.getTenant().getId(),
                            original.getBranch().getId(),
                            original.getPostingDate());

            String reversalVoucherNumber =
                    generateVoucherNumber(
                            original.getTenant().getTenantCode(),
                            original.getBranch().getBranchCode(),
                            original.getPostingDate(),
                            scrollNo);

            reversal =
                    Voucher.builder()
                            .voucherNumber(reversalVoucherNumber)
                            .tenant(original.getTenant())
                            .branch(original.getBranch())
                            .transaction(original.getTransaction())
                            .account(original.getAccount())
                            .glAccount(original.getGlAccount())
                            .drCr(reversalDrCr)
                            .transactionAmount(original.getTransactionAmount())
                            .localCurrencyAmount(original.getLocalCurrencyAmount())
                            .currency(original.getCurrency())
                            .entryDate(LocalDate.now())
                            .postingDate(original.getPostingDate())
                            .valueDate(original.getValueDate())
                            .effectiveDate(original.getEffectiveDate())
                            .batchCode(original.getBatchCode())
                            .setNo(original.getSetNo())
                            .scrollNo(scrollNo)
                            .maker(cancelledBy)
                            .authFlag("Y")
                            .postFlag("N")
                            .cancelFlag("N")
                            .financialEffectFlag("Y")
                            .narration(
                                    "REVERSAL of voucher "
                                            + voucherId
                                            + " ("
                                            + original.getVoucherNumber()
                                            + "): "
                                            + reason)
                            .reversalOfVoucher(original)
                            .build();
            reversal = voucherRepository.save(reversal);

            // Auto-post the reversal voucher (creates reversal ledger entries)
            reversal = postVoucher(reversal.getId());
        } else {
            // Original was NOT posted — no ledger entries to reverse.
            // Just reverse the shadow balance effect; no reversal voucher needed.
            cbsBalanceEngine.updateShadowBalance(
                    original.getAccount().getId(), original.getTransactionAmount(), reversalDrCr);
            // Use the original as the "reversal" return value since no separate voucher is created
            reversal = original;
        }

        // Mark original as cancelled
        original.setCancelFlag("Y");
        voucherRepository.save(original);

        log.info(
                "Voucher cancelled: original={}, reversal={}, reason={}",
                voucherId,
                reversal.getId(),
                reason);

        // RBI-F7: Audit trail for cancellation/reversal
        Long cancelUserId = cancelledBy != null ? cancelledBy.getId() : null;
        auditService.logEvent(
                cancelUserId,
                "VOUCHER_CANCELLED",
                "VOUCHER",
                voucherId,
                "Voucher "
                        + original.getVoucherNumber()
                        + " cancelled. Reversal voucher="
                        + reversal.getVoucherNumber()
                        + " reason="
                        + reason
                        + " cancelledBy="
                        + (cancelledBy != null ? cancelledBy.getUsername() : "N/A"),
                null);

        return reversal;
    }

    private Long requireTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private void assertTenantContext(Long tenantId) {
        Long requiredTenantId = requireTenantId();
        if (!requiredTenantId.equals(tenantId)) {
            throw new RuntimeException(
                    "Tenant mismatch for voucher operation. Expected tenant "
                            + requiredTenantId
                            + " but got "
                            + tenantId);
        }
    }

    /** Get voucher by ID with tenant isolation. */
    public Voucher getVoucher(Long voucherId, Long tenantId) {
        return voucherRepository
                .findByIdAndTenantId(voucherId, tenantId)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));
    }

    /** Get all vouchers for a tenant/branch/posting date. */
    public List<Voucher> getVouchers(Long tenantId, Long branchId, LocalDate postingDate) {
        return voucherRepository.findByTenantIdAndBranchIdAndPostingDate(
                tenantId, branchId, postingDate);
    }

    /** Get next scroll number for (tenant, branch, posting_date). */
    @Transactional
    public Long getNextScrollNo(Long tenantId, Long branchId, LocalDate postingDate) {
        ScrollSequence seq =
                scrollSequenceRepository
                        .findByTenantIdAndBranchIdAndPostingDateWithLock(
                                tenantId, branchId, postingDate)
                        .orElseGet(
                                () -> {
                                    ScrollSequence newSeq =
                                            ScrollSequence.builder()
                                                    .tenantId(tenantId)
                                                    .branchId(branchId)
                                                    .postingDate(postingDate)
                                                    .lastScrollNo(0L)
                                                    .build();
                                    return scrollSequenceRepository.save(newSeq);
                                });

        seq.setLastScrollNo(seq.getLastScrollNo() + 1);
        scrollSequenceRepository.save(seq);
        return seq.getLastScrollNo();
    }

    private BigDecimal calculateBalanceAfter(Long accountId, BigDecimal amount, VoucherDrCr drCr) {
        BigDecimal currentBalance =
                cbsBalanceEngine.getCbsBalance(accountId).getActualTotalBalance();
        if (drCr == VoucherDrCr.CR) {
            return currentBalance.add(amount);
        } else {
            return currentBalance.subtract(amount);
        }
    }

    private Transaction findOrCreateTransaction(Voucher voucher, Transaction linkedTransaction) {
        if (linkedTransaction != null) {
            return linkedTransaction;
        }
        // FR-10 fix: Use the voucher's own transaction FK before creating a synthetic one.
        // This is critical for reversal auto-posting — cancelVoucher sets
        // reversal.transaction = original.transaction, but calls postVoucher(id)
        // without passing the transaction explicitly.
        if (voucher.getTransaction() != null) {
            return voucher.getTransaction();
        }
        // Last resort: create a minimal synthetic transaction for standalone voucher posting
        String txRef = "VCH-" + voucher.getId() + "-" + System.currentTimeMillis();
        Transaction transaction =
                Transaction.builder()
                        .tenant(voucher.getTenant())
                        .transactionRef(txRef)
                        .transactionType(com.ledgora.common.enums.TransactionType.TRANSFER)
                        .status(com.ledgora.common.enums.TransactionStatus.COMPLETED)
                        .amount(voucher.getTransactionAmount())
                        .currency(voucher.getCurrency())
                        .sourceAccount(
                                voucher.getDrCr() == VoucherDrCr.DR ? voucher.getAccount() : null)
                        .destinationAccount(
                                voucher.getDrCr() == VoucherDrCr.CR ? voucher.getAccount() : null)
                        .description("Voucher " + voucher.getId())
                        .narration(voucher.getNarration())
                        .businessDate(voucher.getPostingDate())
                        .build();
        return transactionRepository.save(transaction);
    }

    private void ensureBatchIsOpen(Voucher voucher) {
        String batchCode = voucher.getBatchCode();
        if (batchCode == null || batchCode.isBlank()) {
            throw new RuntimeException(
                    "Voucher posting requires a valid open batch code. Found: " + batchCode);
        }

        // Try lookup by batchCode column first; fall back to BATCH-<id> pattern for backward compat
        TransactionBatch batch =
                transactionBatchRepository
                        .findByBatchCode(batchCode)
                        .orElseGet(
                                () -> {
                                    // Legacy pattern: batchCode string is "BATCH-<id>" but the DB
                                    // column may be null
                                    if (batchCode.startsWith("BATCH-")) {
                                        try {
                                            Long batchId =
                                                    Long.parseLong(
                                                            batchCode.substring("BATCH-".length()));
                                            return transactionBatchRepository
                                                    .findById(batchId)
                                                    .orElse(null);
                                        } catch (NumberFormatException ex) {
                                            return null;
                                        }
                                    }
                                    return null;
                                });
        if (batch == null) {
            throw new RuntimeException("Batch not found for voucher posting: " + batchCode);
        }

        if (batch.getTenant() == null
                || voucher.getTenant() == null
                || !batch.getTenant().getId().equals(voucher.getTenant().getId())) {
            throw new RuntimeException("Batch " + batchCode + " does not belong to voucher tenant");
        }
        if (!voucher.getPostingDate().equals(batch.getBusinessDate())) {
            throw new RuntimeException(
                    "Batch " + batchCode + " business date does not match voucher posting date");
        }
        if (batch.getStatus() != BatchStatus.OPEN) {
            throw new RuntimeException(
                    "Batch " + batchCode + " must be OPEN before voucher posting");
        }
    }
}
