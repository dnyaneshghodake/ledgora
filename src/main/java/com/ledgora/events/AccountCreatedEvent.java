package com.ledgora.events;

import com.ledgora.account.entity.Account;
import org.springframework.context.ApplicationEvent;

/**
 * PART 3: Domain event published when an account is created.
 */
public class AccountCreatedEvent extends ApplicationEvent {
    private final Account account;

    public AccountCreatedEvent(Object source, Account account) {
        super(source);
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }
}
