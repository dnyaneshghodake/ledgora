package com.ledgora.transaction.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.EntryType;
import com.ledgora.common.enums.TransactionStatus;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.common.service.BusinessDateService;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.entity.TransactionLine;
import com.ledgora.transaction.repository.TransactionLineRepository;
import com.ledgora.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionLineRepository transactionLineRepository,
                              AccountRepository accountRepository,
                              AccountBalanceRepository accountBalanceRepository,
                              LedgerEntryRepository ledgerEntryRepository,
                              GeneralLedgerRepository glRepository,
                              UserRepository userRepository,
                              BusinessDateService businessDateService,
                              AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.transactionLineRepository = transactionLineRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.glRepository = glRepository;
        this.userRepository = userRepository;
        this.businessDateService = businessDateService;
        this.auditService = auditService;
    }

    /**
     * PART 1 + 9: Deposit with full transaction safety
     * Flow: validate -> create transaction -> create transaction lines -> create ledger entries -> update balances
     */
    @Transactional
    public Transaction deposit(TransactionDTO dto) {
        // Step 1: Validate account
        Account account = accountRepository.findByAccountNumber(dto.getDestinationAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found: " + dto.getDestinationAccountNumber()));
        validateAccountActive(account);

        User currentUser = getCurrentUser();
        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        String txnRef = generateTransactionRef("DEP");

        // Step 2: Create transaction record
        Transaction transaction = Transaction.builder()
                .transactionRef(txnRef)
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(dto.getAmount())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .destinationAccount(account)
                .description(dto.getDescription() != null ? dto.getDescription() : "Cash Deposit")
                .narration(dto.getNarration())
                .businessDate(businessDate)
                .performedBy(currentUser)
                .build();
        transaction = transactionRepository.save(transaction);

        // Step 3: Create transaction lines (debit + credit)
        createTransactionLine(transaction, account, EntryType.DEBIT, dto.getAmount(),
                "Cash deposit - debit cash account");
        createTransactionLine(transaction, account, EntryType.CREDIT, dto.getAmount(),
                "Cash deposit - credit customer account");

        // Step 4: Create ledger entries from transaction lines
        BigDecimal newBalance = account.getBalance().add(dto.getAmount());
        createLedgerEntry(transaction, account, EntryType.DEBIT, dto.getAmount(), newBalance,
                "1100", businessDate, "Cash deposit to " + account.getAccountNumber());
        createLedgerEntry(transaction, account, EntryType.CREDIT, dto.getAmount(), newBalance,
                "2100", businessDate, "Customer deposit - " + account.getCustomerName());

        // Step 5: Update account balance
        account.setBalance(newBalance);
        accountRepository.save(account);
        updateAccountBalanceCache(account, newBalance);

        // Audit
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logTransaction(userId, transaction.getId(), txnRef, "DEPOSIT");

        log.info("Deposit completed: {} amount {} to account {}", txnRef, dto.getAmount(), account.getAccountNumber());
        return transaction;
    }

    /**
     * PART 1 + 9: Withdrawal with full transaction safety
     */
    @Transactional
    public Transaction withdraw(TransactionDTO dto) {
        // Step 1: Validate account
        Account account = accountRepository.findByAccountNumber(dto.getSourceAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found: " + dto.getSourceAccountNumber()));
        validateAccountActive(account);
        if (account.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance. Available: " + account.getBalance());
        }

        User currentUser = getCurrentUser();
        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        String txnRef = generateTransactionRef("WDR");

        // Step 2: Create transaction record
        Transaction transaction = Transaction.builder()
                .transactionRef(txnRef)
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .amount(dto.getAmount())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .sourceAccount(account)
                .description(dto.getDescription() != null ? dto.getDescription() : "Cash Withdrawal")
                .narration(dto.getNarration())
                .businessDate(businessDate)
                .performedBy(currentUser)
                .build();
        transaction = transactionRepository.save(transaction);

        // Step 3: Create transaction lines
        createTransactionLine(transaction, account, EntryType.DEBIT, dto.getAmount(),
                "Cash withdrawal - debit customer account");
        createTransactionLine(transaction, account, EntryType.CREDIT, dto.getAmount(),
                "Cash withdrawal - credit cash account");

        // Step 4: Create ledger entries
        BigDecimal newBalance = account.getBalance().subtract(dto.getAmount());
        createLedgerEntry(transaction, account, EntryType.DEBIT, dto.getAmount(), newBalance,
                "2100", businessDate, "Withdrawal from " + account.getAccountNumber());
        createLedgerEntry(transaction, account, EntryType.CREDIT, dto.getAmount(), newBalance,
                "1100", businessDate, "Cash withdrawal - " + account.getCustomerName());

        // Step 5: Update account balance
        account.setBalance(newBalance);
        accountRepository.save(account);
        updateAccountBalanceCache(account, newBalance);

        // Audit
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logTransaction(userId, transaction.getId(), txnRef, "WITHDRAWAL");

        log.info("Withdrawal completed: {} amount {} from account {}", txnRef, dto.getAmount(), account.getAccountNumber());
        return transaction;
    }

    /**
     * PART 1 + 9: Transfer with full transaction safety
     */
    @Transactional
    public Transaction transfer(TransactionDTO dto) {
        // Step 1: Validate accounts
        Account sourceAccount = accountRepository.findByAccountNumber(dto.getSourceAccountNumber())
                .orElseThrow(() -> new RuntimeException("Source account not found: " + dto.getSourceAccountNumber()));
        Account destAccount = accountRepository.findByAccountNumber(dto.getDestinationAccountNumber())
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
        LocalDate businessDate = businessDateService.getCurrentBusinessDate();
        String txnRef = generateTransactionRef("TRF");

        // Step 2: Create transaction record
        Transaction transaction = Transaction.builder()
                .transactionRef(txnRef)
                .transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .amount(dto.getAmount())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .sourceAccount(sourceAccount)
                .destinationAccount(destAccount)
                .description(dto.getDescription() != null ? dto.getDescription() : "Internal Transfer")
                .narration(dto.getNarration())
                .businessDate(businessDate)
                .performedBy(currentUser)
                .build();
        transaction = transactionRepository.save(transaction);

        // Step 3: Create transaction lines
        createTransactionLine(transaction, sourceAccount, EntryType.DEBIT, dto.getAmount(),
                "Transfer to " + destAccount.getAccountNumber());
        createTransactionLine(transaction, destAccount, EntryType.CREDIT, dto.getAmount(),
                "Transfer from " + sourceAccount.getAccountNumber());

        // Step 4: Create ledger entries
        BigDecimal sourceNewBalance = sourceAccount.getBalance().subtract(dto.getAmount());
        BigDecimal destNewBalance = destAccount.getBalance().add(dto.getAmount());

        createLedgerEntry(transaction, sourceAccount, EntryType.DEBIT, dto.getAmount(), sourceNewBalance,
                "2100", businessDate, "Transfer to " + destAccount.getAccountNumber());
        createLedgerEntry(transaction, destAccount, EntryType.CREDIT, dto.getAmount(), destNewBalance,
                "2100", businessDate, "Transfer from " + sourceAccount.getAccountNumber());

        // Step 5: Update account balances
        sourceAccount.setBalance(sourceNewBalance);
        accountRepository.save(sourceAccount);
        updateAccountBalanceCache(sourceAccount, sourceNewBalance);

        destAccount.setBalance(destNewBalance);
        accountRepository.save(destAccount);
        updateAccountBalanceCache(destAccount, destNewBalance);

        // Audit
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logTransaction(userId, transaction.getId(), txnRef, "TRANSFER");

        log.info("Transfer completed: {} amount {} from {} to {}", txnRef, dto.getAmount(),
                sourceAccount.getAccountNumber(), destAccount.getAccountNumber());
        return transaction;
    }

    // ===== Query methods (backward compatible) =====

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    public Optional<Transaction> getTransactionByRef(String ref) {
        return transactionRepository.findByTransactionRef(ref);
    }

    public List<Transaction> getTransactionsByAccountNumber(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }

    public List<Transaction> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findByDateRange(start, end);
    }

    public List<Transaction> getTransactionsByType(TransactionType type) {
        return transactionRepository.findByTransactionType(type);
    }

    public List<LedgerEntry> getLedgerEntriesByTransaction(Long transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId);
    }

    public List<LedgerEntry> getLedgerEntriesByAccount(String accountNumber) {
        return ledgerEntryRepository.findByAccountNumber(accountNumber);
    }

    public long countAll() {
        return transactionRepository.count();
    }

    public List<Transaction> getTodayTransactions() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return transactionRepository.findByDateRange(startOfDay, endOfDay);
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

    private void createLedgerEntry(Transaction transaction, Account account, EntryType entryType,
                                   BigDecimal amount, BigDecimal balanceAfter, String glCode,
                                   LocalDate businessDate, String narration) {
        GeneralLedger glAccount = glRepository.findByGlCode(glCode).orElse(null);

        LedgerEntry entry = LedgerEntry.builder()
                .transaction(transaction)
                .account(account)
                .glAccount(glAccount)
                .glAccountCode(glCode)
                .entryType(entryType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .currency(transaction.getCurrency())
                .businessDate(businessDate)
                .postingTime(LocalDateTime.now())
                .narration(narration)
                .build();
        ledgerEntryRepository.save(entry);
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
}
