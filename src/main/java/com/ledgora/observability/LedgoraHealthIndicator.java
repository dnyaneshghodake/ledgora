package com.ledgora.observability;

import com.ledgora.common.service.BusinessDateService;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.transaction.repository.TransactionRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PART 14: Custom health indicator for the Ledgora platform.
 * Checks: ledger integrity (SUM debits = SUM credits), business date status,
 * and basic repository connectivity.
 */
@Component
public class LedgoraHealthIndicator implements HealthIndicator {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRepository transactionRepository;
    private final BusinessDateService businessDateService;

    public LedgoraHealthIndicator(LedgerEntryRepository ledgerEntryRepository,
                                   TransactionRepository transactionRepository,
                                   BusinessDateService businessDateService) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionRepository = transactionRepository;
        this.businessDateService = businessDateService;
    }

    @Override
    public Health health() {
        try {
            // Check business date
            LocalDate businessDate = businessDateService.getCurrentBusinessDate();

            // Check ledger integrity for today
            BigDecimal todayDebits = ledgerEntryRepository.sumDebitsByBusinessDate(businessDate);
            BigDecimal todayCredits = ledgerEntryRepository.sumCreditsByBusinessDate(businessDate);
            boolean ledgerBalanced = todayDebits.compareTo(todayCredits) == 0;

            // Get transaction count
            long totalTransactions = transactionRepository.count();

            Health.Builder builder = ledgerBalanced ? Health.up() : Health.down();
            return builder
                    .withDetail("businessDate", businessDate.toString())
                    .withDetail("ledgerBalanced", ledgerBalanced)
                    .withDetail("todayDebits", todayDebits)
                    .withDetail("todayCredits", todayCredits)
                    .withDetail("totalTransactions", totalTransactions)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
