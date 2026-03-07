package com.ledgora.events;

import com.ledgora.ledger.entity.LedgerJournal;
import org.springframework.context.ApplicationEvent;

/**
 * PART 3: Domain event published when ledger entries are posted.
 * Triggers balance cache update via BalanceEventListener.
 */
public class LedgerPostedEvent extends ApplicationEvent {
    private final LedgerJournal journal;

    public LedgerPostedEvent(Object source, LedgerJournal journal) {
        super(source);
        this.journal = journal;
    }

    public LedgerJournal getJournal() {
        return journal;
    }
}
