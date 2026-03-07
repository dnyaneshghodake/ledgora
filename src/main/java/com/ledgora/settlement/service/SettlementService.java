package com.ledgora.settlement.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.BusinessDateStatus;
import com.ledgora.common.enums.SettlementStatus;
import com.ledgora.common.enums.TransactionStatus;
import com.ledgora.common.service.BusinessDateService;
import com.ledgora.events.SettlementCompletedEvent;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.reporting.dto.TrialBalanceReport;
import com.ledgora.reporting.service.ReportingService;
import com.ledgora.settlement.entity.Settlement;
import com.ledgora.settlement.entity.SettlementEntry;
import com.ledgora.settlement.repository.SettlementEntryRepository;
import com.ledgora.settlement.repository.SettlementRepository;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.repository.TransactionRepository;
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

@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);
    private final SettlementRepository settlementRepository;
    private final SettlementEntryRepository settlementEntryRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final UserRepository userRepository;
    private final BusinessDateService businessDateService;
    private final AuditService auditService;
    private final ReportingService reportingService;
    private final ApplicationEventPublisher eventPublisher;

    public SettlementService(SettlementRepository settlementRepository,
                             SettlementEntryRepository settlementEntryRepository,
                             TransactionRepository transactionRepository,
                             AccountRepository accountRepository,
                             AccountBalanceRepository accountBalanceRepository,
                             LedgerEntryRepository ledgerEntryRepository,
                             UserRepository userRepository,
                             BusinessDateService businessDateService,
                             AuditService auditService,
                             ReportingService reportingService,
                             ApplicationEventPublisher eventPublisher) {
        this.settlementRepository = settlementRepository;
        this.settlementEntryRepository = settlementEntryRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userRepository = userRepository;
        this.businessDateService = businessDateService;
        this.auditService = auditService;
        this.reportingService = reportingService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * PART 12: Enhanced EOD Settlement - 7-step process with invariant validation.
     * 1. Stop transaction intake (set business date to DAY_CLOSING)
     * 2. Flush pending events (complete pending transactions)
     * 3. Validate ledger integrity (SUM debit = SUM credit)
     * 4. Generate trial balance report
     * 5. Post accruals and fees (placeholder for interest/fee posting)
     * 6. Generate settlement reports (entries per account)
     * 7. Advance business date
     */
    @Transactional
    public Settlement processSettlement(LocalDate date) {
        User currentUser = getCurrentUser();
        String ref = "SET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Settlement settlement = Settlement.builder()
                .settlementRef(ref)
                .businessDate(date)
                .status(SettlementStatus.IN_PROGRESS)
                .startTime(LocalDateTime.now())
                .processedBy(currentUser)
                .build();
        settlement = settlementRepository.save(settlement);

        try {
            // Step 1: Stop transaction intake
            log.info("Settlement [{}] Step 1: Stopping transaction intake - setting DAY_CLOSING", ref);
            businessDateService.startDayClosing();

            // Step 2: Flush pending events (complete pending transactions)
            log.info("Settlement [{}] Step 2: Flushing pending transactions", ref);
            List<Transaction> pendingTxns = transactionRepository.findByStatusAndBusinessDate(
                    TransactionStatus.PENDING, date);
            for (Transaction txn : pendingTxns) {
                txn.setStatus(TransactionStatus.COMPLETED);
                transactionRepository.save(txn);
            }
            log.info("Settlement [{}] Step 2: Flushed {} pending transactions", ref, pendingTxns.size());

            // Step 3: Validate ledger integrity (system invariant)
            log.info("Settlement [{}] Step 3: Validating ledger integrity", ref);
            BigDecimal totalDebits = ledgerEntryRepository.sumDebitsByBusinessDate(date);
            BigDecimal totalCredits = ledgerEntryRepository.sumCreditsByBusinessDate(date);
            if (totalDebits.compareTo(totalCredits) != 0) {
                throw new RuntimeException("SETTLEMENT INVARIANT VIOLATION: Total Debits ("
                        + totalDebits + ") != Total Credits (" + totalCredits + ") for date " + date);
            }
            log.info("Settlement [{}] Step 3: Ledger integrity validated - debits={} credits={}", ref, totalDebits, totalCredits);

            // Step 4: Generate trial balance report
            log.info("Settlement [{}] Step 4: Generating trial balance", ref);
            TrialBalanceReport trialBalance = reportingService.generateTrialBalance(date);
            if (!trialBalance.isBalanced()) {
                log.warn("Settlement [{}] Step 4: Trial balance is NOT balanced - debits={} credits={}",
                        ref, trialBalance.getTotalDebits(), trialBalance.getTotalCredits());
            } else {
                log.info("Settlement [{}] Step 4: Trial balance verified - {} GL accounts balanced",
                        ref, trialBalance.getLines().size());
            }

            // Step 5: Post accruals and fees
            log.info("Settlement [{}] Step 5: Posting accruals and fees (placeholder)", ref);
            // Placeholder: In production, this would calculate and post interest accruals,
            // account maintenance fees, and other periodic charges.

            // Recalculate balances for all active accounts
            List<Account> activeAccounts = accountRepository.findByStatus(AccountStatus.ACTIVE);
            for (Account account : activeAccounts) {
                BigDecimal accountDebits = ledgerEntryRepository.sumDebitsByAccountId(account.getId());
                BigDecimal accountCredits = ledgerEntryRepository.sumCreditsByAccountId(account.getId());
                BigDecimal ledgerBalance = accountCredits.subtract(accountDebits);

                AccountBalance bal = accountBalanceRepository.findByAccountId(account.getId())
                        .orElseGet(() -> AccountBalance.builder()
                                .account(account)
                                .holdAmount(BigDecimal.ZERO)
                                .build());
                bal.setLedgerBalance(ledgerBalance);
                bal.setAvailableBalance(ledgerBalance.subtract(bal.getHoldAmount()));
                accountBalanceRepository.save(bal);
            }

            // Step 6: Generate settlement reports (entries per account)
            log.info("Settlement [{}] Step 6: Generating settlement reports", ref);
            List<Transaction> completedTxns = transactionRepository.findByStatusAndBusinessDate(
                    TransactionStatus.COMPLETED, date);
            int txnCount = completedTxns.size();

            for (Account account : activeAccounts) {
                BigDecimal accountDebits = ledgerEntryRepository.sumDebitsByAccountId(account.getId());
                BigDecimal accountCredits = ledgerEntryRepository.sumCreditsByAccountId(account.getId());
                BigDecimal openingBalance = account.getBalance().subtract(accountCredits).add(accountDebits);

                SettlementEntry entry = SettlementEntry.builder()
                        .settlement(settlement)
                        .account(account)
                        .openingBalance(openingBalance)
                        .totalDebits(accountDebits)
                        .totalCredits(accountCredits)
                        .closingBalance(account.getBalance())
                        .build();
                settlementEntryRepository.save(entry);
            }

            settlement.setStatus(SettlementStatus.COMPLETED);
            settlement.setTransactionCount(txnCount);
            settlement.setEndTime(LocalDateTime.now());
            settlement.setRemarks("EOD Settlement for " + date + " - " + txnCount + " transactions processed. "
                    + "Ledger integrity: VERIFIED. Trial balance: " + (trialBalance.isBalanced() ? "BALANCED" : "UNBALANCED"));
            settlement = settlementRepository.save(settlement);

            // Step 7: Advance business date
            log.info("Settlement [{}] Step 7: Advancing business date", ref);
            businessDateService.closeDayAndAdvance();

            // Publish settlement completed event (PART 3)
            eventPublisher.publishEvent(new SettlementCompletedEvent(this, settlement));

            // Audit
            Long userId = currentUser != null ? currentUser.getId() : null;
            auditService.logSettlement(userId, settlement.getId(), ref);

            log.info("Settlement completed: {} for date {} with {} transactions", ref, date, txnCount);
            return settlement;

        } catch (Exception e) {
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setEndTime(LocalDateTime.now());
            settlement.setRemarks("Settlement failed: " + e.getMessage());
            settlementRepository.save(settlement);
            log.error("Settlement failed for date {}: {}", date, e.getMessage());
            throw new RuntimeException("Settlement processing failed: " + e.getMessage(), e);
        }
    }

    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAll();
    }

    public Optional<Settlement> getSettlementById(Long id) {
        return settlementRepository.findById(id);
    }

    public Optional<Settlement> getSettlementByRef(String ref) {
        return settlementRepository.findBySettlementRef(ref);
    }

    public List<Settlement> getSettlementsByDate(LocalDate date) {
        return settlementRepository.findByBusinessDate(date);
    }

    public List<Settlement> getSettlementsByStatus(SettlementStatus status) {
        return settlementRepository.findByStatus(status);
    }

    public List<Settlement> getSettlementsByDateRange(LocalDate start, LocalDate end) {
        return settlementRepository.findByBusinessDateBetween(start, end);
    }

    public long countByStatus(SettlementStatus status) {
        return settlementRepository.findByStatus(status).size();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
