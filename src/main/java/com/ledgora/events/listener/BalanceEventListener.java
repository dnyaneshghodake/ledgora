package com.ledgora.events.listener;

import com.ledgora.balance.service.BalanceEngineService;
import com.ledgora.events.AccountCreatedEvent;
import com.ledgora.events.LedgerPostedEvent;
import com.ledgora.events.SettlementCompletedEvent;
import com.ledgora.ledger.entity.LedgerEntry;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * PART 3 + PART 9: Event listener for balance updates. Listens to LedgerPostedEvent and refreshes
 * balance cache.
 */
@Component
public class BalanceEventListener {

    private static final Logger log = LoggerFactory.getLogger(BalanceEventListener.class);
    private final BalanceEngineService balanceEngine;

    public BalanceEventListener(BalanceEngineService balanceEngine) {
        this.balanceEngine = balanceEngine;
    }

    @EventListener
    public void onLedgerPosted(LedgerPostedEvent event) {
        log.info(
                "BalanceEventListener: Refreshing balances for journal {}",
                event.getJournal().getId());

        // Collect unique account IDs from journal entries
        Set<Long> accountIds = new HashSet<>();
        for (LedgerEntry entry : event.getJournal().getEntries()) {
            if (entry.getAccount() != null) {
                accountIds.add(entry.getAccount().getId());
            }
        }

        // Refresh balance cache for each affected account
        for (Long accountId : accountIds) {
            balanceEngine.refreshBalanceCache(accountId);
        }
    }

    @EventListener
    public void onAccountCreated(AccountCreatedEvent event) {
        log.info(
                "BalanceEventListener: Account created event for {}",
                event.getAccount().getAccountNumber());
    }

    @EventListener
    public void onSettlementCompleted(SettlementCompletedEvent event) {
        log.info(
                "BalanceEventListener: Settlement completed event for {}",
                event.getSettlement().getSettlementRef());
    }
}
