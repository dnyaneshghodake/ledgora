package com.ledgora.balance.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.ledger.service.LedgerSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * PART 9: Real-time balance engine.
 * Responsibilities:
 * - Calculate available balance
 * - Apply holds
 * - Return real-time balances
 *
 * Balance calculation: snapshot_balance + ledger_entries - holds
 */
@Service
public class BalanceEngine {

    private static final Logger log = LoggerFactory.getLogger(BalanceEngine.class);
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerSnapshotService snapshotService;

    public BalanceEngine(AccountRepository accountRepository,
                         AccountBalanceRepository accountBalanceRepository,
                         LedgerEntryRepository ledgerEntryRepository,
                         LedgerSnapshotService snapshotService) {
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.snapshotService = snapshotService;
    }

    /**
     * Get real-time ledger balance from ledger entries (uses snapshot optimization).
     */
    public BigDecimal getLedgerBalance(Long accountId) {
        return snapshotService.calculateBalanceFromSnapshot(accountId);
    }

    /**
     * Get available balance = ledger balance - holds.
     */
    public BigDecimal getAvailableBalance(Long accountId) {
        BigDecimal ledgerBalance = getLedgerBalance(accountId);
        BigDecimal holdAmount = getHoldAmount(accountId);
        return ledgerBalance.subtract(holdAmount);
    }

    /**
     * Get hold amount for an account.
     */
    public BigDecimal getHoldAmount(Long accountId) {
        return accountBalanceRepository.findByAccountId(accountId)
                .map(AccountBalance::getHoldAmount)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Place a hold on an account.
     */
    @Transactional
    public void placeHold(Long accountId, BigDecimal holdAmount) {
        AccountBalance balance = getOrCreateAccountBalance(accountId);
        balance.setHoldAmount(balance.getHoldAmount().add(holdAmount));
        balance.setAvailableBalance(balance.getLedgerBalance().subtract(balance.getHoldAmount()));
        accountBalanceRepository.save(balance);
        log.info("Hold placed on account {}: amount={}, total holds={}",
                accountId, holdAmount, balance.getHoldAmount());
    }

    /**
     * Release a hold on an account.
     */
    @Transactional
    public void releaseHold(Long accountId, BigDecimal holdAmount) {
        AccountBalance balance = getOrCreateAccountBalance(accountId);
        BigDecimal newHold = balance.getHoldAmount().subtract(holdAmount);
        if (newHold.compareTo(BigDecimal.ZERO) < 0) {
            newHold = BigDecimal.ZERO;
        }
        balance.setHoldAmount(newHold);
        balance.setAvailableBalance(balance.getLedgerBalance().subtract(newHold));
        accountBalanceRepository.save(balance);
        log.info("Hold released on account {}: amount={}, remaining holds={}",
                accountId, holdAmount, newHold);
    }

    /**
     * Refresh the balance cache for an account from the ledger.
     */
    @Transactional
    public AccountBalance refreshBalanceCache(Long accountId) {
        BigDecimal ledgerBalance = getLedgerBalance(accountId);
        AccountBalance balance = getOrCreateAccountBalance(accountId);
        balance.setLedgerBalance(ledgerBalance);
        balance.setAvailableBalance(ledgerBalance.subtract(balance.getHoldAmount()));
        accountBalanceRepository.save(balance);
        log.debug("Balance cache refreshed for account {}: ledger={}, available={}",
                accountId, ledgerBalance, balance.getAvailableBalance());
        return balance;
    }

    private AccountBalance getOrCreateAccountBalance(Long accountId) {
        return accountBalanceRepository.findByAccountId(accountId)
                .orElseGet(() -> {
                    Account account = accountRepository.findById(accountId)
                            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
                    AccountBalance ab = AccountBalance.builder()
                            .account(account)
                            .ledgerBalance(BigDecimal.ZERO)
                            .availableBalance(BigDecimal.ZERO)
                            .holdAmount(BigDecimal.ZERO)
                            .build();
                    return accountBalanceRepository.save(ab);
                });
    }
}
