package com.ledgora.events.listener;

import com.ledgora.events.TransactionCreatedEvent;
import com.ledgora.events.LedgerPostedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * PART 3: Event listener for ledger-related events.
 * Listens to TransactionCreatedEvent and triggers ledger posting.
 * Note: In the current implementation, ledger posting is done synchronously
 * within the transaction service. This listener provides the extensibility
 * point for async processing if needed in the future.
 */
@Component
public class LedgerEventListener {

    private static final Logger log = LoggerFactory.getLogger(LedgerEventListener.class);

    @EventListener
    public void onTransactionCreated(TransactionCreatedEvent event) {
        log.info("TransactionCreatedEvent received for transaction: {}",
                event.getTransaction().getTransactionRef());
        // Ledger posting is handled synchronously within TransactionService
        // This listener provides a hook for additional async processing
    }

    @EventListener
    public void onLedgerPosted(LedgerPostedEvent event) {
        log.info("LedgerPostedEvent received for journal: {} (transaction: {})",
                event.getJournal().getId(),
                event.getJournal().getTransaction().getTransactionRef());
        // Balance cache update is triggered by BalanceEventListener
    }
}
