package com.ledgora.voucher.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.repository.TransactionBatchRepository;
import com.ledgora.balance.service.CbsBalanceEngine;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.EntryType;
import com.ledgora.common.enums.BatchStatus;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CBS Voucher Service - controls the full lifecycle of vouchers.
 *
 * Lifecycle: createVoucher -> authorizeVoucher -> postVoucher -> (cancelVoucher)
 *
 * Rules:
 * - On create: update shadow balance only
 * - On authorize: set auth_flag = Y
 * - On post: create LedgerJournal + LedgerEntry (immutable), update actual balance,
 *            update GL balance, reduce shadow delta, mark post_flag = Y
 * - On cancel: create reversal voucher, create reversal ledger entry, mark cancel_flag = Y
 * - NO DELETE allowed
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
    private final CbsBalanceEngine cbsBalanceEngine;
    private final TransactionBatchRepository transactionBatchRepository;
    private final CbsCustomerValidationService customerValidationService;
    private final GlBalanceService glBalanceService;

    public VoucherService(VoucherRepository voucherRepository,
                          ScrollSequenceRepository scrollSequenceRepository,
                          AccountRepository accountRepository,
                          GeneralLedgerRepository glRepository,
                          LedgerJournalRepository journalRepository,
                          LedgerEntryRepository entryRepository,
                          TransactionRepository transactionRepository,
                          CbsBalanceEngine cbsBalanceEngine,
                          TransactionBatchRepository transactionBatchRepository,
                          CbsCustomerValidationService customerValidationService,
                          GlBalanceService glBalanceService) {
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
    }

    /**
     * Create a new voucher. On create, only shadow balance is updated.
     */
    @Transactional
    public Voucher createVoucher(Tenant tenant, Branch branch, Account account,
                                  GeneralLedger glAccount, VoucherDrCr drCr,
                                  BigDecimal transactionAmount, BigDecimal localCurrencyAmount,
                                  String currency, LocalDate postingDate, LocalDate valueDate,
                                  String batchCode, Integer setNo, User maker, String narration) {

        assertTenantContext(tenant.getId());

        // Validate customer and account
        customerValidationService.validateAccountForTransaction(
                account, tenant.getId(), branch.getId(), drCr);

        // Validate sufficient balance for debit
        if (drCr == VoucherDrCr.DR) {
            cbsBalanceEngine.validateSufficientBalance(account.getId(), transactionAmount);
        }

        // Get next scroll number
        Long scrollNo = getNextScrollNo(tenant.getId(), branch.getId(), postingDate);

        Voucher voucher = Voucher.builder()
                .tenant(tenant)
                .branch(branch)
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

        log.info("Voucher created: id={}, account={}, dr_cr={}, amount={}, scroll={}",
                voucher.getId(), account.getAccountNumber(), drCr, transactionAmount, scrollNo);

        return voucher;
    }

    /**
     * Authorize a voucher. Sets auth_flag = Y.
     * Checker must not be null and must differ from maker (maker-checker enforcement).
     */
    @Transactional
    public Voucher authorizeVoucher(Long voucherId, User checker) {
        Long tenantId = requireTenantId();
        Voucher voucher = voucherRepository.findByIdAndTenantId(voucherId, tenantId)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));

        if ("Y".equals(voucher.getCancelFlag())) {
            throw new RuntimeException("Cannot authorize a cancelled voucher: " + voucherId);
        }
        if ("Y".equals(voucher.getAuthFlag())) {
            throw new RuntimeException("Voucher already authorized: " + voucherId);
        }
        if (checker == null) {
            throw new RuntimeException("Checker must not be null for voucher authorization: " + voucherId);
        }
        if (voucher.getMaker() != null
                && voucher.getMaker().getId().equals(checker.getId())) {
            throw new RuntimeException("Maker and checker cannot be the same user for voucher: " + voucherId);
        }

        voucher.setAuthFlag("Y");
        voucher.setChecker(checker);
        voucher = voucherRepository.save(voucher);

        log.info("Voucher authorized: id={}, checker={}", voucherId, checker.getUsername());

        return voucher;
    }

    /**
     * System auto-authorize a voucher for straight-through processing (e.g. teller deposits).
     * Records the maker as the initiator and marks authorization as SYSTEM_AUTO.
     * This maintains audit trail while allowing single-user transaction flows.
     */
    @Transactional
    public Voucher systemAuthorizeVoucher(Long voucherId, User maker) {
        Long tenantId = requireTenantId();
        Voucher voucher = voucherRepository.findByIdAndTenantId(voucherId, tenantId)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));

        if ("Y".equals(voucher.getCancelFlag())) {
            throw new RuntimeException("Cannot authorize a cancelled voucher: " + voucherId);
        }
        if ("Y".equals(voucher.getAuthFlag())) {
            throw new RuntimeException("Voucher already authorized: " + voucherId);
        }

        voucher.setAuthFlag("Y");
        // Record maker as checker with SYSTEM_AUTO narration for audit trail
        voucher.setChecker(maker);
        String existingNarration = voucher.getNarration() != null ? voucher.getNarration() : "";
        voucher.setNarration(existingNarration + " [SYSTEM_AUTO_AUTHORIZED]");
        voucher = voucherRepository.save(voucher);

        log.info("Voucher system-auto-authorized: id={}, maker={}", voucherId,
                maker != null ? maker.getUsername() : "SYSTEM");

        return voucher;
    }

    /**
     * Post a voucher. Creates LedgerJournal + LedgerEntry, updates actual balance and GL.
     */
    @Transactional
    public Voucher postVoucher(Long voucherId) {
        return postVoucher(voucherId, null);
    }

    /**
     * Post a voucher and optionally link ledger entries to an existing transaction.
     */
    @Transactional
    public Voucher postVoucher(Long voucherId, Transaction linkedTransaction) {
        Voucher voucher = voucherRepository.findByIdWithLock(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));

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
        Tenant tenant = voucher.getTenant();
        BigDecimal amount = voucher.getTransactionAmount();
        VoucherDrCr drCr = voucher.getDrCr();

        // Find or create a transaction reference for this voucher
        Transaction transaction = findOrCreateTransaction(voucher, linkedTransaction);

        // Create LedgerJournal
        LedgerJournal journal = LedgerJournal.builder()
                .transaction(transaction)
                .tenant(tenant)
                .description("Voucher posting: " + voucher.getId() + " - " + voucher.getNarration())
                .businessDate(voucher.getPostingDate())
                .build();
        journal = journalRepository.save(journal);

        // Determine entry type and GL posting direction
        EntryType accountEntryType = drCr == VoucherDrCr.DR ? EntryType.DEBIT : EntryType.CREDIT;
        EntryType glEntryType = drCr == VoucherDrCr.DR ? EntryType.CREDIT : EntryType.DEBIT;

        // Create immutable LedgerEntry for account side
        BigDecimal balanceAfter = calculateBalanceAfter(account.getId(), amount, drCr);
        LedgerEntry accountEntry = LedgerEntry.builder()
                .journal(journal)
                .transaction(transaction)
                .tenant(tenant)
                .account(account)
                .glAccount(voucher.getGlAccount())
                .glAccountCode(voucher.getGlAccount() != null ? voucher.getGlAccount().getGlCode() : null)
                .entryType(accountEntryType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .currency(voucher.getCurrency())
                .businessDate(voucher.getPostingDate())
                .postingTime(LocalDateTime.now())
                .narration(voucher.getNarration())
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

        // Mark voucher as posted
        voucher.setPostFlag("Y");
        voucher.setLedgerEntry(accountEntry);
        voucher = voucherRepository.save(voucher);

        log.info("Voucher posted: id={}, journal={}, ledger_entry={}, account={}, amount={}",
                voucherId, journal.getId(), accountEntry.getId(),
                account.getAccountNumber(), amount);

        return voucher;
    }

    /**
     * Cancel a voucher. Creates a reversal voucher and reversal ledger entry.
     * Marks original cancel_flag = Y.
     */
    @Transactional
    public Voucher cancelVoucher(Long voucherId, User cancelledBy, String reason) {
        Long tenantId = requireTenantId();
        Voucher original = voucherRepository.findByIdAndTenantId(voucherId, tenantId)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));

        if ("Y".equals(original.getCancelFlag())) {
            throw new RuntimeException("Voucher already cancelled: " + voucherId);
        }

        // Determine reversal direction
        VoucherDrCr reversalDrCr = original.getDrCr() == VoucherDrCr.DR ? VoucherDrCr.CR : VoucherDrCr.DR;

        // Get next scroll number for reversal
        Long scrollNo = getNextScrollNo(
                original.getTenant().getId(),
                original.getBranch().getId(),
                original.getPostingDate());

        // Create reversal voucher
        Voucher reversal = Voucher.builder()
                .tenant(original.getTenant())
                .branch(original.getBranch())
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
                .narration("REVERSAL of voucher " + voucherId + ": " + reason)
                .reversalOfVoucher(original)
                .build();
        reversal = voucherRepository.save(reversal);

        // If original was posted, create reversal ledger entry
        if ("Y".equals(original.getPostFlag())) {
            // Auto-post the reversal voucher
            reversal = postVoucher(reversal.getId());
        } else {
            // If not posted, just reverse the shadow effect
            cbsBalanceEngine.updateShadowBalance(
                    original.getAccount().getId(),
                    original.getTransactionAmount(),
                    reversalDrCr);
        }

        // Mark original as cancelled
        original.setCancelFlag("Y");
        voucherRepository.save(original);

        log.info("Voucher cancelled: original={}, reversal={}, reason={}",
                voucherId, reversal.getId(), reason);

        return reversal;
    }

    private Long requireTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private void assertTenantContext(Long tenantId) {
        Long requiredTenantId = requireTenantId();
        if (!requiredTenantId.equals(tenantId)) {
            throw new RuntimeException("Tenant mismatch for voucher operation. Expected tenant "
                    + requiredTenantId + " but got " + tenantId);
        }
    }

    /**
     * Get voucher by ID with tenant isolation.
     */
    public Voucher getVoucher(Long voucherId, Long tenantId) {
        return voucherRepository.findByIdAndTenantId(voucherId, tenantId)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + voucherId));
    }

    /**
     * Get all vouchers for a tenant/branch/posting date.
     */
    public List<Voucher> getVouchers(Long tenantId, Long branchId, LocalDate postingDate) {
        return voucherRepository.findByTenantIdAndBranchIdAndPostingDate(tenantId, branchId, postingDate);
    }

    /**
     * Get next scroll number for (tenant, branch, posting_date).
     */
    @Transactional
    public Long getNextScrollNo(Long tenantId, Long branchId, LocalDate postingDate) {
        ScrollSequence seq = scrollSequenceRepository
                .findByTenantIdAndBranchIdAndPostingDateWithLock(tenantId, branchId, postingDate)
                .orElseGet(() -> {
                    ScrollSequence newSeq = ScrollSequence.builder()
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
        BigDecimal currentBalance = cbsBalanceEngine.getCbsBalance(accountId).getActualTotalBalance();
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
        // Look for existing transactions linked to this account on the same date
        // Create a minimal transaction record for the voucher posting
        String txRef = "VCH-" + voucher.getId() + "-" + System.currentTimeMillis();
        Transaction transaction = Transaction.builder()
                .tenant(voucher.getTenant())
                .transactionRef(txRef)
                .transactionType(com.ledgora.common.enums.TransactionType.TRANSFER)
                .status(com.ledgora.common.enums.TransactionStatus.COMPLETED)
                .amount(voucher.getTransactionAmount())
                .currency(voucher.getCurrency())
                .sourceAccount(voucher.getDrCr() == VoucherDrCr.DR ? voucher.getAccount() : null)
                .destinationAccount(voucher.getDrCr() == VoucherDrCr.CR ? voucher.getAccount() : null)
                .description("Voucher " + voucher.getId())
                .narration(voucher.getNarration())
                .businessDate(voucher.getPostingDate())
                .build();
        return transactionRepository.save(transaction);
    }

    private void ensureBatchIsOpen(Voucher voucher) {
        String batchCode = voucher.getBatchCode();
        if (batchCode == null || !batchCode.startsWith("BATCH-")) {
            throw new RuntimeException("Voucher posting requires a valid open batch code. Found: " + batchCode);
        }
        Long batchId;
        try {
            batchId = Long.parseLong(batchCode.substring("BATCH-".length()));
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Voucher posting requires batch code format BATCH-<id>. Found: " + batchCode);
        }

        TransactionBatch batch = transactionBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found for voucher posting: " + batchCode));

        if (batch.getTenant() == null || voucher.getTenant() == null
                || !batch.getTenant().getId().equals(voucher.getTenant().getId())) {
            throw new RuntimeException("Batch " + batchCode + " does not belong to voucher tenant");
        }
        if (!voucher.getPostingDate().equals(batch.getBusinessDate())) {
            throw new RuntimeException("Batch " + batchCode + " business date does not match voucher posting date");
        }
        if (batch.getStatus() != BatchStatus.OPEN) {
            throw new RuntimeException("Batch " + batchCode + " must be OPEN before voucher posting");
        }
    }
}
