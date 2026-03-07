package com.ledgora.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PART 14: Observability - metrics configuration.
 * Exposes metrics for transaction throughput, ledger posting latency, and settlement duration.
 */
@Configuration
public class LedgoraMetricsConfig {

    @Bean
    public Counter transactionCounter(MeterRegistry registry) {
        return Counter.builder("ledgora.transactions.total")
                .description("Total number of transactions processed")
                .register(registry);
    }

    @Bean
    public Counter depositCounter(MeterRegistry registry) {
        return Counter.builder("ledgora.transactions.deposits")
                .description("Total number of deposit transactions")
                .register(registry);
    }

    @Bean
    public Counter withdrawalCounter(MeterRegistry registry) {
        return Counter.builder("ledgora.transactions.withdrawals")
                .description("Total number of withdrawal transactions")
                .register(registry);
    }

    @Bean
    public Counter transferCounter(MeterRegistry registry) {
        return Counter.builder("ledgora.transactions.transfers")
                .description("Total number of transfer transactions")
                .register(registry);
    }

    @Bean
    public Timer ledgerPostingTimer(MeterRegistry registry) {
        return Timer.builder("ledgora.ledger.posting.duration")
                .description("Time taken to post ledger journal entries")
                .register(registry);
    }

    @Bean
    public Timer settlementTimer(MeterRegistry registry) {
        return Timer.builder("ledgora.settlement.duration")
                .description("Time taken to complete end-of-day settlement")
                .register(registry);
    }

    @Bean
    public Counter ledgerEntryCounter(MeterRegistry registry) {
        return Counter.builder("ledgora.ledger.entries.total")
                .description("Total number of ledger entries created")
                .register(registry);
    }

    @Bean
    public Counter settlementCounter(MeterRegistry registry) {
        return Counter.builder("ledgora.settlements.total")
                .description("Total number of settlements processed")
                .register(registry);
    }

    @Bean
    public Counter settlementFailureCounter(MeterRegistry registry) {
        return Counter.builder("ledgora.settlements.failures")
                .description("Total number of failed settlements")
                .register(registry);
    }
}
