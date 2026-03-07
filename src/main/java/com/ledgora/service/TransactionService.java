package com.ledgora.service;

import com.ledgora.dto.TransactionDTO;
import com.ledgora.model.Account;
import com.ledgora.model.LedgerEntry;
import com.ledgora.model.Transaction;
import com.ledgora.model.User;
import com.ledgora.model.enums.AccountStatus;
import com.ledgora.model.enums.EntryType;
import com.ledgora.model.enums.TransactionStatus;
import com.ledgora.model.enums.TransactionType;
import com.ledgora.repository.AccountRepository;
import com.ledgora.repository.LedgerEntryRepository;
import com.ledgora.repository.TransactionRepository;
import com.ledgora.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              LedgerEntryRepository ledgerEntryRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Transaction deposit(TransactionDTO dto) {
        Account account = accountRepository.findByAccountNumber(dto.getDestinationAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found: " + dto.getDestinationAccountNumber()));
        validateAccountActive(account);
        User currentUser = getCurrentUser();
        String txnRef = generateTransactionRef("DEP");

        Transaction transaction = Transaction.builder()
                .transactionRef(txnRef)
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(dto.getAmount())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .destinationAccount(account)
                .description(dto.getDescription() != null ? dto.getDescription() : "Cash Deposit")
                .narration(dto.getNarration())
                .performedBy(currentUser)
                .build();
        transaction = transactionRepository.save(transaction);

        BigDecimal newBalance = account.getBalance().add(dto.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Double-entry: Debit Cash (Asset increases), Credit Customer Deposit (Liability increases)
        createLedgerEntry(transaction, account, EntryType.DEBIT, dto.getAmount(), newBalance,
                "1100", "Cash deposit to " + account.getAccountNumber());
        createLedgerEntry(transaction, account, EntryType.CREDIT, dto.getAmount(), newBalance,
                "2100", "Customer deposit - " + account.getCustomerName());

        log.info("Deposit completed: {} amount {} to account {}", txnRef, dto.getAmount(), account.getAccountNumber());
        return transaction;
    }

    @Transactional
    public Transaction withdraw(TransactionDTO dto) {
        Account account = accountRepository.findByAccountNumber(dto.getSourceAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found: " + dto.getSourceAccountNumber()));
        validateAccountActive(account);
        if (account.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance. Available: " + account.getBalance());
        }
        User currentUser = getCurrentUser();
        String txnRef = generateTransactionRef("WDR");

        Transaction transaction = Transaction.builder()
                .transactionRef(txnRef)
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .amount(dto.getAmount())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .sourceAccount(account)
                .description(dto.getDescription() != null ? dto.getDescription() : "Cash Withdrawal")
                .narration(dto.getNarration())
                .performedBy(currentUser)
                .build();
        transaction = transactionRepository.save(transaction);

        BigDecimal newBalance = account.getBalance().subtract(dto.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Double-entry: Debit Customer Deposit (Liability decreases), Credit Cash (Asset decreases)
        createLedgerEntry(transaction, account, EntryType.DEBIT, dto.getAmount(), newBalance,
                "2100", "Withdrawal from " + account.getAccountNumber());
        createLedgerEntry(transaction, account, EntryType.CREDIT, dto.getAmount(), newBalance,
                "1100", "Cash withdrawal - " + account.getCustomerName());

        log.info("Withdrawal completed: {} amount {} from account {}", txnRef, dto.getAmount(), account.getAccountNumber());
        return transaction;
    }

    @Transactional
    public Transaction transfer(TransactionDTO dto) {
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
        String txnRef = generateTransactionRef("TRF");

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
                .performedBy(currentUser)
                .build();
        transaction = transactionRepository.save(transaction);

        BigDecimal sourceNewBalance = sourceAccount.getBalance().subtract(dto.getAmount());
        sourceAccount.setBalance(sourceNewBalance);
        accountRepository.save(sourceAccount);

        BigDecimal destNewBalance = destAccount.getBalance().add(dto.getAmount());
        destAccount.setBalance(destNewBalance);
        accountRepository.save(destAccount);

        // Double-entry: Debit source, Credit destination
        createLedgerEntry(transaction, sourceAccount, EntryType.DEBIT, dto.getAmount(), sourceNewBalance,
                "2100", "Transfer to " + destAccount.getAccountNumber());
        createLedgerEntry(transaction, destAccount, EntryType.CREDIT, dto.getAmount(), destNewBalance,
                "2100", "Transfer from " + sourceAccount.getAccountNumber());

        log.info("Transfer completed: {} amount {} from {} to {}", txnRef, dto.getAmount(),
                sourceAccount.getAccountNumber(), destAccount.getAccountNumber());
        return transaction;
    }

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

    private void createLedgerEntry(Transaction transaction, Account account, EntryType entryType,
                                   BigDecimal amount, BigDecimal balanceAfter, String glCode, String narration) {
        LedgerEntry entry = LedgerEntry.builder()
                .transaction(transaction)
                .account(account)
                .entryType(entryType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .glAccountCode(glCode)
                .narration(narration)
                .build();
        ledgerEntryRepository.save(entry);
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
