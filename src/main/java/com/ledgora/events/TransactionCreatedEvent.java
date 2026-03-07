package com.ledgora.events;

import com.ledgora.transaction.entity.Transaction;
import org.springframework.context.ApplicationEvent;

/**
 * PART 3: Domain event published when a transaction is created.
 * Triggers ledger posting via LedgerEventListener.
 */
public class TransactionCreatedEvent extends ApplicationEvent {
    private final Transaction transaction;

    public TransactionCreatedEvent(Object source, Transaction transaction) {
        super(source);
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
