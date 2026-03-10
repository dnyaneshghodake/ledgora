package com.ledgora.ledger.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.entity.LedgerSnapshot;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.ledger.repository.LedgerSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PART 5: Ledger snapshot service for performance optimization. Creates periodic balance snapshots
 * and computes balances from snapshots + delta entries.
 */
@Service
public class LedgerSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(LedgerSnapshotService.class);
    private final LedgerSnapshotRepository snapshotRepository;
    private final LedgerEntryRepository entryRepository;
    private final AccountRepository accountRepository;

    public LedgerSnapshotService(
            LedgerSnapshotRepository snapshotRepository,
            LedgerEntryRepository entryRepository,
            AccountRepository accountRepository) {
        this.snapshotRepository = snapshotRepository;
        this.entryRepository = entryRepository;
        this.accountRepository = accountRepository;
    }

    /** Create a snapshot for an account. */
    @Transactional
    public LedgerSnapshot createSnapshot(Long accountId) {
        Account account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        // Calculate balance from all ledger entries
        BigDecimal totalCredits = entryRepository.sumCreditsByAccountId(accountId);
        BigDecimal totalDebits = entryRepository.sumDebitsByAccountId(accountId);
        BigDecimal balance = totalCredits.subtract(totalDebits);

        // Get the last entry ID
        List<LedgerEntry> entries = entryRepository.findByAccountId(accountId);
        Long lastEntryId =
                entries.isEmpty()
                        ? 0L
                        : entries.stream().mapToLong(LedgerEntry::getId).max().orElse(0L);

        LedgerSnapshot snapshot =
                LedgerSnapshot.builder()
                        .accountId(accountId)
                        .snapshotBalance(balance)
                        .snapshotDate(LocalDate.now())
                        .lastEntryId(lastEntryId)
                        .currency(account.getCurrency())
                        .build();

        snapshot = snapshotRepository.save(snapshot);
        log.info(
                "Snapshot created for account {}: balance={}, lastEntryId={}",
                accountId,
                balance,
                lastEntryId);
        return snapshot;
    }

    /** Create snapshots for all active accounts. */
    @Transactional
    public int createSnapshotsForAllAccounts() {
        List<Account> activeAccounts = accountRepository.findByStatus(AccountStatus.ACTIVE);
        int count = 0;
        for (Account account : activeAccounts) {
            createSnapshot(account.getId());
            count++;
        }
        log.info("Created {} snapshots for all active accounts", count);
        return count;
    }

    /**
     * Calculate balance using snapshot + delta entries (entries after snapshot). This is the
     * optimized balance calculation for PART 5.
     */
    public BigDecimal calculateBalanceFromSnapshot(Long accountId) {
        Optional<LedgerSnapshot> latestSnapshot =
                snapshotRepository.findLatestByAccountId(accountId);

        if (latestSnapshot.isPresent()) {
            LedgerSnapshot snapshot = latestSnapshot.get();
            // Get entries after the snapshot
            BigDecimal deltaCredits =
                    entryRepository.sumCreditsAfterEntryId(accountId, snapshot.getLastEntryId());
            BigDecimal deltaDebits =
                    entryRepository.sumDebitsAfterEntryId(accountId, snapshot.getLastEntryId());
            BigDecimal delta = deltaCredits.subtract(deltaDebits);
            return snapshot.getSnapshotBalance().add(delta);
        } else {
            // No snapshot, calculate from all entries
            BigDecimal totalCredits = entryRepository.sumCreditsByAccountId(accountId);
            BigDecimal totalDebits = entryRepository.sumDebitsByAccountId(accountId);
            return totalCredits.subtract(totalDebits);
        }
    }

    /** Get latest snapshot for an account. */
    public Optional<LedgerSnapshot> getLatestSnapshot(Long accountId) {
        return snapshotRepository.findLatestByAccountId(accountId);
    }
}
