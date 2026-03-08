package com.ledgora.transaction.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.service.BatchService;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.EntryType;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.enums.TransactionStatus;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.common.enums.VoucherDrCr;
import com.ledgora.common.service.BusinessDateService;
import com.ledgora.events.TransactionCreatedEvent;
import com.ledgora.idempotency.service.IdempotencyService;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.entity.TransactionLine;
import com.ledgora.transaction.repository.TransactionLineRepository;
import com.ledgora.transaction.repository.TransactionRepository;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.service.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transaction service with:
 * - Ledger journal/entry creation (system of record)
 * - Event-driven architecture (publishes TransactionCreatedEvent)
 * - Pessimistic locking for account balance protection
 * - Multi-tenant support with day status enforcement
 * - Transaction batch assignment
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionLineRepository transactionLineRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final GeneralLedgerRepository glRepository;
    private final UserRepository userRepository;
    private final BusinessDateService businessDateService;
    private final AuditService auditService;
    private final VoucherService voucherService;
    private final ApplicationEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final TenantService tenantService;
    private final BatchService batchService;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionLineRepository transactionLineRepository,
                              AccountRepository accountRepository,
                              AccountBalanceRepository accountBalanceRepository,
                              LedgerEntryRepository ledgerEntryRepository,
                              GeneralLedgerRepository glRepository,
                              UserRepository userRepository,
                              BusinessDateService businessDateService,
                              AuditService auditService,
                              VoucherService voucherService,
                              ApplicationEventPublisher eventPublisher,
                              IdempotencyService idempotencyService,
                              TenantService tenantService,
                              BatchService batchService) {
        this.transactionRepository = transactionRepository;
        this.transactionLineRepository = transactionLineRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.glRepository = glRepository;
        this.userRepository = userRepository;
        this.businessDateService = businessDateService;
        this.auditService = auditService;
        this.voucherService = voucherService;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.tenantService = tenantService;
        this.batchService = batchService;
    }

    /**
     * Deposit with full transaction safety.
     * Flow: validate tenant day -> lock account -> create transaction -> assign batch -> create journal+entries -> update balances -> publish event
     */
    @Transactional
    public Transaction deposit(TransactionDTO dto) {
        // PART 4: Validate tenant business day is OPEN
        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);

        // Idempotency check
        checkIdempotency(dto);

        // Pessimistic lock on account
        Account account = accountRepository.findByAccountNumberWithLockAndTenantId(dto.getDestinationAccountNumber(), tenantId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + dto.getDestinationAccountNumber()));
        validateAccountActive(account);

        User currentUser = getCurrentUser();
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);
        String txnRef = generateTransactionRef("DEP");

        // PART 2: Get or create open batch for this channel/tenant/date
        TransactionChannel channel = parseChannel(dto.getChannel());
        TransactionBatch batch = batchService.getOrCreateOpenBatch(tenantId, channel, businessDate);

        // Create transaction record with tenant and batch
        Transaction transaction = Transaction.builder()
                .transactionRef(txnRef)
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(dto.getAmount())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .channel(channel)
                .clientReferenceId(dto.getClientReferenceId())
                .destinationAccount(account)
                .description(dto.getDescription() != null ? dto.getDescription() : "Cash Deposit")
                .narration(dto.getNarration())
                .businessDate(businessDate)
                .performedBy(currentUser)
                .tenant(tenant)
                .batch(batch)
                .build();
        transaction = transactionRepository.save(transaction);

        // Update batch totals
        batchService.updateBatchTotals(batch.getId(), dto.getAmount(), dto.getAmount());

        // Step 3: Create transaction lines (debit + credit)
        createTransactionLine(transaction, account, EntryType.DEBIT, dto.getAmount(),
                "Cash deposit - debit cash account");
        createTransactionLine(transaction, account, EntryType.CREDIT, dto.getAmount(),
                "Cash deposit - credit customer account");

        // PART 1: Create and post balanced vouchers (ledger entries are created only via VoucherService)
        BigDecimal newBalance = account.getBalance().add(dto.getAmount());
        Account cashAccount = resolveCashGlAccount(tenantId);
        String currency = dto.getCurrency() != null ? dto.getCurrency() : "INR";
        postVoucher(transaction, tenant, currentUser, cashAccount, VoucherDrCr.DR, dto.getAmount(),
                currency, businessDate, batch, "Cash deposit - cash ledger leg");
        postVoucher(transaction, tenant, currentUser, account, VoucherDrCr.CR, dto.getAmount(),
                currency, businessDate, batch, "Cash deposit - customer ledger leg");

        // Step 5: Update account balance
        account.setBalance(newBalance);
        accountRepository.save(account);
        updateAccountBalanceCache(account, newBalance);

        // PART 3: Publish event
        eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction));

        // Audit
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logTransaction(userId, transaction.getId(), txnRef, "DEPOSIT");

        log.info("Deposit completed: {} amount {} to account {}", txnRef, dto.getAmount(), account.getAccountNumber());
        return transaction;
    }

    /**
     * Withdrawal with full transaction safety and tenant/batch support.
     */
    @Transactional
    public Transaction withdraw(TransactionDTO dto) {
        // PART 4: Validate tenant business day is OPEN
        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);

        // Idempotency check
        checkIdempotency(dto);

        // Pessimistic lock on account
        Account account = accountRepository.findByAccountNumberWithLockAndTenantId(dto.getSourceAccountNumber(), tenantId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + dto.getSourceAccountNumber()));
        validateAccountActive(account);
        if (account.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance. Available: " + account.getBalance());
        }

        User currentUser = getCurrentUser();
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);
        String txnRef = generateTransactionRef("WDR");

        // PART 2: Get or create open batch
        TransactionChannel channel = parseChannel(dto.getChannel());
        TransactionBatch batch = batchService.getOrCreateOpenBatch(tenantId, channel, businessDate);

        // Create transaction record with tenant and batch
        Transaction transaction = Transaction.builder()
                .transactionRef(txnRef)
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .amount(dto.getAmount())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .channel(channel)
                .clientReferenceId(dto.getClientReferenceId())
                .sourceAccount(account)
                .description(dto.getDescription() != null ? dto.getDescription() : "Cash Withdrawal")
                .narration(dto.getNarration())
                .businessDate(businessDate)
                .performedBy(currentUser)
                .tenant(tenant)
                .batch(batch)
                .build();
        transaction = transactionRepository.save(transaction);

        // Update batch totals
        batchService.updateBatchTotals(batch.getId(), dto.getAmount(), dto.getAmount());

        // Step 3: Create transaction lines
        createTransactionLine(transaction, account, EntryType.DEBIT, dto.getAmount(),
                "Cash withdrawal - debit customer account");
        createTransactionLine(transaction, account, EntryType.CREDIT, dto.getAmount(),
                "Cash withdrawal - credit cash account");

        // PART 1: Create and post balanced vouchers (ledger entries are created only via VoucherService)
        BigDecimal newBalance = account.getBalance().subtract(dto.getAmount());
        Account cashAccount = resolveCashGlAccount(tenantId);
        String currency = dto.getCurrency() != null ? dto.getCurrency() : "INR";
        postVoucher(transaction, tenant, currentUser, account, VoucherDrCr.DR, dto.getAmount(),
                currency, businessDate, batch, "Cash withdrawal - customer ledger leg");
        postVoucher(transaction, tenant, currentUser, cashAccount, VoucherDrCr.CR, dto.getAmount(),
                currency, businessDate, batch, "Cash withdrawal - cash ledger leg");

        // Step 5: Update account balance
        account.setBalance(newBalance);
        accountRepository.save(account);
        updateAccountBalanceCache(account, newBalance);

        // PART 3: Publish event
        eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction));

        // Audit
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logTransaction(userId, transaction.getId(), txnRef, "WITHDRAWAL");

        log.info("Withdrawal completed: {} amount {} from account {}", txnRef, dto.getAmount(), account.getAccountNumber());
        return transaction;
    }

    /**
     * Transfer with full transaction safety and tenant/batch support.
     */
    @Transactional
    public Transaction transfer(TransactionDTO dto) {
        // PART 4: Validate tenant business day is OPEN
        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);

        // Idempotency check
        checkIdempotency(dto);

        // Pessimistic lock on both accounts
        Account sourceAccount = accountRepository.findByAccountNumberWithLockAndTenantId(dto.getSourceAccountNumber(), tenantId)
                .orElseThrow(() -> new RuntimeException("Source account not found: " + dto.getSourceAccountNumber()));
        Account destAccount = accountRepository.findByAccountNumberWithLockAndTenantId(dto.getDestinationAccountNumber(), tenantId)
                .orElseThrow(() -> new RuntimeException("Destination account not found: " + dto.getDestinationAccountNumber()));
        validateAccountActive(sourceAccount);
        validateAccountActive(destAccount);
        if (sourceAccount.getAccountNumber().equals(destAccount.getAccountNumber())) {
            throw new RuntimeException("Source and destination accounts cannot be the same");
        }
        if (sourceAccount.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance. Available: " + sourceAccount.getBalance());
        }

        User currentUser = getCurrentUser();
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);
        String txnRef = generateTransactionRef("TRF");

        // PART 2: Get or create open batch
        TransactionChannel channel = parseChannel(dto.getChannel());
        TransactionBatch batch = batchService.getOrCreateOpenBatch(tenantId, channel, businessDate);

        // Create transaction record with tenant and batch
        Transaction transaction = Transaction.builder()
                .transactionRef(txnRef)
                .transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .amount(dto.getAmount())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .channel(channel)
                .clientReferenceId(dto.getClientReferenceId())
                .sourceAccount(sourceAccount)
                .destinationAccount(destAccount)
                .description(dto.getDescription() != null ? dto.getDescription() : "Internal Transfer")
                .narration(dto.getNarration())
                .businessDate(businessDate)
                .performedBy(currentUser)
                .tenant(tenant)
                .batch(batch)
                .build();
        transaction = transactionRepository.save(transaction);

        // Update batch totals
        batchService.updateBatchTotals(batch.getId(), dto.getAmount(), dto.getAmount());

        // Step 3: Create transaction lines
        createTransactionLine(transaction, sourceAccount, EntryType.DEBIT, dto.getAmount(),
                "Transfer to " + destAccount.getAccountNumber());
        createTransactionLine(transaction, destAccount, EntryType.CREDIT, dto.getAmount(),
                "Transfer from " + sourceAccount.getAccountNumber());

        // PART 1: Create and post balanced vouchers (ledger entries are created only via VoucherService)
        BigDecimal sourceNewBalance = sourceAccount.getBalance().subtract(dto.getAmount());
        BigDecimal destNewBalance = destAccount.getBalance().add(dto.getAmount());
        String currency = dto.getCurrency() != null ? dto.getCurrency() : "INR";
        postVoucher(transaction, tenant, currentUser, sourceAccount, VoucherDrCr.DR, dto.getAmount(),
                currency, businessDate, batch, "Transfer to " + destAccount.getAccountNumber());
        postVoucher(transaction, tenant, currentUser, destAccount, VoucherDrCr.CR, dto.getAmount(),
                currency, businessDate, batch, "Transfer from " + sourceAccount.getAccountNumber());

        // Step 5: Update account balances
        sourceAccount.setBalance(sourceNewBalance);
        accountRepository.save(sourceAccount);
        updateAccountBalanceCache(sourceAccount, sourceNewBalance);

        destAccount.setBalance(destNewBalance);
        accountRepository.save(destAccount);
        updateAccountBalanceCache(destAccount, destNewBalance);

        // PART 3: Publish event
        eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction));

        // Audit
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logTransaction(userId, transaction.getId(), txnRef, "TRANSFER");

        log.info("Transfer completed: {} amount {} from {} to {}", txnRef, dto.getAmount(),
                sourceAccount.getAccountNumber(), destAccount.getAccountNumber());
        return transaction;
    }

    // ===== Query methods (backward compatible) =====

    public List<Transaction> getAllTransactions() {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTenantId(tenantId);
    }

    public Optional<Transaction> getTransactionById(Long id) {
        Long tenantId = requireTenantId();
        return transactionRepository.findByIdAndTenantId(id, tenantId);
    }

    public Optional<Transaction> getTransactionByRef(String ref) {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTransactionRefAndTenantId(ref, tenantId);
    }

    public List<Transaction> getTransactionsByAccountNumber(String accountNumber) {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTenantIdAndAccountNumber(tenantId, accountNumber);
    }

    public List<Transaction> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end) {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTenantIdAndDateRange(tenantId, start, end);
    }

    public List<Transaction> getTransactionsByType(TransactionType type) {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTenantIdAndTransactionType(tenantId, type);
    }

    public List<LedgerEntry> getLedgerEntriesByTransaction(Long transactionId) {
        Long tenantId = requireTenantId();
        return ledgerEntryRepository.findByTransactionIdAndTenantId(transactionId, tenantId);
    }

    public List<LedgerEntry> getLedgerEntriesByAccount(String accountNumber) {
        Long tenantId = requireTenantId();
        return ledgerEntryRepository.findByAccountNumberAndTenantId(accountNumber, tenantId);
    }

    public long countAll() {
        Long tenantId = requireTenantId();
        return transactionRepository.countByTenantId(tenantId);
    }

    public List<Transaction> getTodayTransactions() {
        Long tenantId = requireTenantId();
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return transactionRepository.findByTenantIdAndDateRange(tenantId, startOfDay, endOfDay);
    }

    // ===== Private helper methods =====

    private void createTransactionLine(Transaction transaction, Account account, EntryType lineType,
                                       BigDecimal amount, String description) {
        TransactionLine line = TransactionLine.builder()
                .transaction(transaction)
                .account(account)
                .lineType(lineType)
                .amount(amount)
                .currency(transaction.getCurrency())
                .description(description)
                .build();
        transactionLineRepository.save(line);
    }

    private void updateAccountBalanceCache(Account account, BigDecimal newLedgerBalance) {
        AccountBalance balance = accountBalanceRepository.findByAccountId(account.getId())
                .orElseGet(() -> AccountBalance.builder()
                        .account(account)
                        .holdAmount(BigDecimal.ZERO)
                        .build());
        balance.setLedgerBalance(newLedgerBalance);
        balance.setAvailableBalance(newLedgerBalance.subtract(balance.getHoldAmount()));
        accountBalanceRepository.save(balance);
    }

    private void validateAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account " + account.getAccountNumber() + " is not active. Status: " + account.getStatus());
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private String generateTransactionRef(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Voucher postVoucher(Transaction transaction, Tenant tenant, User maker,
                                Account account, VoucherDrCr drCr, BigDecimal amount,
                                String currency, LocalDate businessDate,
                                TransactionBatch batch, String narration) {
        Branch branch = resolveBranch(account, maker);
        GeneralLedger glAccount = resolveGlForAccount(account);
        String batchCode = "BATCH-" + batch.getId();

        Voucher voucher = voucherService.createVoucher(
                tenant,
                branch,
                account,
                glAccount,
                drCr,
                amount,
                amount,
                currency,
                businessDate,
                businessDate,
                batchCode,
                1,
                maker,
                narration
        );
        voucherService.authorizeVoucher(voucher.getId(), null);
        return voucherService.postVoucher(voucher.getId(), transaction);
    }

    private Branch resolveBranch(Account account, User currentUser) {
        if (account.getBranch() != null) {
            return account.getBranch();
        }
        if (account.getHomeBranch() != null) {
            return account.getHomeBranch();
        }
        if (currentUser != null && currentUser.getBranch() != null) {
            return currentUser.getBranch();
        }
        throw new RuntimeException("No branch mapping found for account " + account.getAccountNumber());
    }

    private GeneralLedger resolveGlForAccount(Account account) {
        if (account.getGlAccountCode() == null || account.getGlAccountCode().isBlank()) {
            return null;
        }
        return glRepository.findByGlCode(account.getGlAccountCode()).orElse(null);
    }

    private Account resolveCashGlAccount(Long tenantId) {
        return accountRepository.findFirstByTenantIdAndGlAccountCode(tenantId, "1100")
                .or(() -> accountRepository.findByAccountNumberAndTenantId("GL-CASH-001", tenantId))
                .orElseThrow(() -> new RuntimeException("Cash GL account with code 1100 is required for cash transactions"));
    }

    /**
     * PART 2: Idempotency check using client_reference_id + channel.
     * Prevents duplicate transaction processing.
     */
    private void checkIdempotency(TransactionDTO dto) {
        if (dto.getClientReferenceId() != null && dto.getChannel() != null) {
            TransactionChannel channel = parseChannel(dto.getChannel());
            if (channel != null) {
                transactionRepository.findByClientReferenceIdAndChannelAndTenantId(dto.getClientReferenceId(), channel, requireTenantId())
                        .ifPresent(existing -> {
                            throw new RuntimeException("Duplicate transaction detected. Existing ref: "
                                    + existing.getTransactionRef() + " for client_reference_id: "
                                    + dto.getClientReferenceId() + " channel: " + dto.getChannel());
                        });
            }
            // Also register with IdempotencyService for broader deduplication
            String idempotencyKey = dto.getClientReferenceId() + ":" + dto.getChannel();
            idempotencyService.checkExisting(idempotencyKey).ifPresent(existing -> {
                throw new RuntimeException("Duplicate transaction: idempotency key already completed");
            });
            idempotencyService.registerKey(idempotencyKey, dto.toString());
        }
    }

    private Long requireTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private TransactionChannel parseChannel(String channel) {
        if (channel == null || channel.isEmpty()) return null;
        try {
            return TransactionChannel.valueOf(channel);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
