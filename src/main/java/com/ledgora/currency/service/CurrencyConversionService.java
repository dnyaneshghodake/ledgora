package com.ledgora.currency.service;

import com.ledgora.currency.entity.ExchangeRate;
import com.ledgora.currency.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * PART 6: Currency conversion service for multi-currency support.
 */
@Service
public class CurrencyConversionService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionService.class);
    private final ExchangeRateRepository exchangeRateRepository;

    public CurrencyConversionService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    /**
     * Convert amount from one currency to another using the latest applicable rate.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency, LocalDate date) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        ExchangeRate rate = exchangeRateRepository.findLatestRate(fromCurrency, toCurrency, date)
                .orElseThrow(() -> new RuntimeException(
                        "Exchange rate not found for " + fromCurrency + " -> " + toCurrency + " on " + date));

        BigDecimal converted = amount.multiply(rate.getRate()).setScale(4, RoundingMode.HALF_UP);
        log.debug("Currency conversion: {} {} -> {} {} (rate: {})",
                amount, fromCurrency, converted, toCurrency, rate.getRate());
        return converted;
    }

    /**
     * Get the latest exchange rate for a currency pair.
     */
    public Optional<ExchangeRate> getLatestRate(String from, String to) {
        return exchangeRateRepository.findLatestRate(from, to, LocalDate.now());
    }

    /**
     * Create or update an exchange rate.
     */
    @Transactional
    public ExchangeRate setRate(String from, String to, BigDecimal rate, LocalDate effectiveDate) {
        ExchangeRate exchangeRate = ExchangeRate.builder()
                .currencyFrom(from)
                .currencyTo(to)
                .rate(rate)
                .effectiveDate(effectiveDate)
                .build();
        ExchangeRate saved = exchangeRateRepository.save(exchangeRate);
        log.info("Exchange rate set: {} -> {} = {} (effective {})", from, to, rate, effectiveDate);
        return saved;
    }

    /**
     * Get all rates for a specific date.
     */
    public List<ExchangeRate> getRatesByDate(LocalDate date) {
        return exchangeRateRepository.findByEffectiveDate(date);
    }
}
