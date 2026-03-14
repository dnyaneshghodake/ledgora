package com.ledgora.config.seeder;

import com.ledgora.currency.entity.ExchangeRate;
import com.ledgora.currency.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 8 — Exchange Rate seeding. Seeds USD/INR/EUR/GBP pairs for multi-currency
 * support.
 */
@Component
public class ExchangeRateSeeder {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateSeeder.class);
    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateSeeder(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public void seed() {
        if (exchangeRateRepository.count() > 0) {
            log.info("  [FX] Exchange rates already exist — skipping");
            return;
        }

        LocalDate today = nextWeekday();
        createRate("USD", "INR", new BigDecimal("83.12500000"), today);
        createRate("USD", "EUR", new BigDecimal("0.92150000"), today);
        createRate("INR", "EUR", new BigDecimal("0.01108500"), today);
        createRate("EUR", "INR", new BigDecimal("90.21400000"), today);
        createRate("GBP", "INR", new BigDecimal("105.47000000"), today);
        createRate("INR", "USD", new BigDecimal("0.01203000"), today);

        log.info("  [FX] 6 exchange rates seeded (USD/INR/EUR/GBP pairs)");
    }

    /**
     * Returns a guaranteed weekday date (Mon-Fri). CBS exchange rates are effective on working days.
     */
    private static LocalDate nextWeekday() {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek().getValue() > 5) {
            d = d.plusDays(1);
        }
        return d;
    }

    private void createRate(String from, String to, BigDecimal rate, LocalDate effectiveDate) {
        ExchangeRate er =
                ExchangeRate.builder()
                        .currencyFrom(from)
                        .currencyTo(to)
                        .rate(rate)
                        .effectiveDate(effectiveDate)
                        .build();
        exchangeRateRepository.save(er);
    }
}
