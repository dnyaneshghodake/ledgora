package com.ledgora.events;

import com.ledgora.settlement.entity.Settlement;
import org.springframework.context.ApplicationEvent;

/** PART 3: Domain event published when settlement is completed. */
public class SettlementCompletedEvent extends ApplicationEvent {
    private final Settlement settlement;

    public SettlementCompletedEvent(Object source, Settlement settlement) {
        super(source);
        this.settlement = settlement;
    }

    public Settlement getSettlement() {
        return settlement;
    }
}
