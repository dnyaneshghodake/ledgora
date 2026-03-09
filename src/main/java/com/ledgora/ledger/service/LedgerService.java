package com.ledgora.ledger.service;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.EntryType;
import com.ledgora.events.LedgerPostedEvent;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.gl.service.GlBalanceService;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.entity.LedgerJournal;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.ledger.repository.LedgerJournalRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.transaction.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PART 1 + PART 3: Ledger service - system of record for all financial entries.
 * Creates immutable ledger journals and entries.
 * Publishes LedgerPostedEvent after successful posting.
 * Now integrates GL balance updates and multi-tenant support.
 */
@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
    private final LedgerJournalRepository journalRepository;
    private final LedgerEntryRepository entryRepository;
    private final GeneralLedgerRepository glRepository;
    private final GlBalanceService glBalanceService;
    private final ApplicationEventPublisher eventPublisher;

    public LedgerService(LedgerJournalRepository journalRepository,
                         LedgerEntryRepository entryRepository,
                         GeneralLedgerRepository glRepository,
                         GlBalanceService glBalanceService,
                         ApplicationEventPublisher eventPublisher) {
        this.journalRepository = journalRepository;
        this.entryRepository = entryRepository;
        this.glRepository = glRepository;
        this.glBalanceService = glBalanceService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a ledger journal with entries for a transaction.
     * Ensures SUM(debits) = SUM(credits) before persisting.
     * This is the core method that makes the ledger the system of record.
     */
    @Transactional
    public LedgerJournal postJournal(Transaction transaction, String description,
                                      LocalDate businessDate, List<JournalEntryRequest> entryRequests) {
        // Validate double-entry: SUM(debits) must equal SUM(credits)
        BigDecimal totalDebits = entryRequests.stream()
                .filter(e -> e.entryType == EntryType.DEBIT)
                .map(e -> e.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entryRequests.stream()
                .filter(e -> e.entryType == EntryType.CREDIT)
                .map(e -> e.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new RuntimeException("Ledger integrity violation: SUM(debits)=" + totalDebits
                    + " != SUM(credits)=" + totalCredits);
        }

        // Must have at least 2 entries (double-entry)
        if (entryRequests.size() < 2) {
            throw new RuntimeException("Ledger journal must have at least 2 entries for double-entry accounting");
        }

        // Get tenant from transaction
        Tenant tenant = transaction.getTenant();

        // Create journal
        LedgerJournal journal = LedgerJournal.builder()
                .transaction(transaction)
                .tenant(tenant)
                .description(description)
                .businessDate(businessDate)
                .build();
        journal = journalRepository.save(journal);

        // Create entries and update GL balances
        List<LedgerEntry> entries = new ArrayList<>();
        for (JournalEntryRequest req : entryRequests) {
            GeneralLedger glAccount = null;
            if (req.glCode != null) {
                glAccount = glRepository.findByGlCode(req.glCode).orElse(null);
            }

            LedgerEntry entry = LedgerEntry.builder()
                    .journal(journal)
                    .transaction(transaction)
                    .tenant(tenant)
                    .account(req.account)
                    .glAccount(glAccount)
                    .glAccountCode(req.glCode)
                    .entryType(req.entryType)
                    .amount(req.amount)
                    .balanceAfter(req.balanceAfter)
                    .currency(req.currency != null ? req.currency : "INR")
                    .businessDate(businessDate)
                    .postingTime(LocalDateTime.now())
                    .narration(req.narration)
                    .build();
            entries.add(entryRepository.save(entry));

            // PART 1: Update GL balance with accounting sign conventions
            if (glAccount != null) {
                BigDecimal debit = req.entryType == EntryType.DEBIT ? req.amount : BigDecimal.ZERO;
                BigDecimal credit = req.entryType == EntryType.CREDIT ? req.amount : BigDecimal.ZERO;
                glBalanceService.updateGlBalance(glAccount, debit, credit);
            }
        }

        // Entries are saved independently via entryRepository.save() above.
        // LedgerJournal is @Immutable (no setters) — entries are linked via journal_id FK.

        // Publish event
        eventPublisher.publishEvent(new LedgerPostedEvent(this, journal));
        log.info("Ledger journal posted: {} with {} entries for transaction {}",
                journal.getId(), entries.size(), transaction.getTransactionRef());

        return journal;
    }

    /**
     * Get journal by ID.
     */
    public LedgerJournal getJournal(Long id) {
        return journalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Journal not found: " + id));
    }

    /**
     * Get journals by transaction.
     */
    public List<LedgerJournal> getJournalsByTransaction(Long transactionId) {
        return journalRepository.findByTransactionId(transactionId);
    }

    /**
     * Get journals by business date.
     */
    public List<LedgerJournal> getJournalsByDate(LocalDate date) {
        return journalRepository.findByBusinessDate(date);
    }

    /**
     * Validate ledger integrity for a date: SUM(debits) = SUM(credits).
     */
    public boolean validateLedgerIntegrity(LocalDate date) {
        BigDecimal debits = entryRepository.sumDebitsByBusinessDate(date);
        BigDecimal credits = entryRepository.sumCreditsByBusinessDate(date);
        boolean balanced = debits.compareTo(credits) == 0;
        if (!balanced) {
            log.error("Ledger integrity check FAILED for {}: debits={} credits={}", date, debits, credits);
        }
        return balanced;
    }

    /**
     * Request object for building journal entries.
     */
    public static class JournalEntryRequest {
        public Account account;
        public EntryType entryType;
        public BigDecimal amount;
        public BigDecimal balanceAfter;
        public String currency;
        public String glCode;
        public String narration;

        public static JournalEntryRequest of(Account account, EntryType entryType, BigDecimal amount,
                                              BigDecimal balanceAfter, String currency, String glCode,
                                              String narration) {
            JournalEntryRequest req = new JournalEntryRequest();
            req.account = account;
            req.entryType = entryType;
            req.amount = amount;
            req.balanceAfter = balanceAfter;
            req.currency = currency;
            req.glCode = glCode;
            req.narration = narration;
            return req;
        }
    }
}
