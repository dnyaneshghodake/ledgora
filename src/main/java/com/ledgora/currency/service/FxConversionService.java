package com.ledgora.currency.service;

import com.ledgora.currency.entity.ExchangeRate;
import com.ledgora.currency.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Finacle-grade FX Conversion Service.
 *
 * <p>Provides real-time currency conversion using the latest effective exchange rate. CBS/RBI
 * compliance:
 *
 * <ul>
 *   <li>Rate lookup uses the most recent rate on or before the business date
 *   <li>Conversion uses HALF_UP rounding to 4 decimal places (CBS precision)
 *   <li>Same-currency conversion returns the original amount (no rate lookup)
 *   <li>Missing rate throws a clear error — no silent fallback to 1:1
 * </ul>
 */
@Service
public class FxConversionService {

    private static final Logger log = LoggerFactory.getLogger(FxConversionService.class);
    private final ExchangeRateRepository exchangeRateRepository;

    public FxConversionService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    /**
     * Convert an amount from one currency to another using the latest effective rate.
     *
     * @param amount the amount in the source currency
     * @param fromCurrency ISO 4217 source currency code (e.g. USD)
     * @param toCurrency ISO 4217 target currency code (e.g. INR)
     * @param businessDate the business date for rate lookup
     * @return the converted amount in the target currency (4 decimal places, HALF_UP)
     * @throws RuntimeException if no exchange rate is found for the currency pair
     */
    public BigDecimal convert(
            BigDecimal amount, String fromCurrency, String toCurrency, LocalDate businessDate) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount; // Same currency — no conversion needed
        }

        BigDecimal rate = getRate(fromCurrency, toCurrency, businessDate);
        BigDecimal converted = amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);

        log.info(
                "FX conversion: {} {} → {} {} (rate={}, date={})",
                amount,
                fromCurrency,
                converted,
                toCurrency,
                rate,
                businessDate);

        return converted;
    }

    /**
     * Get the exchange rate for a currency pair on or before the given date.
     *
     * @return the rate (1 unit of fromCurrency = rate units of toCurrency)
     * @throws RuntimeException if no rate is found
     */
    public BigDecimal getRate(String fromCurrency, String toCurrency, LocalDate businessDate) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return BigDecimal.ONE;
        }

        return exchangeRateRepository
                .findLatestRate(fromCurrency.toUpperCase(), toCurrency.toUpperCase(), businessDate)
                .map(ExchangeRate::getRate)
                .orElseThrow(
                        () ->
                                new com.ledgora.common.exception.BusinessException(
                                        "FX_RATE_NOT_FOUND",
                                        "No exchange rate found for "
                                                + fromCurrency
                                                + " → "
                                                + toCurrency
                                                + " on or before "
                                                + businessDate
                                                + ". Configure FX rates before processing"
                                                + " cross-currency transactions."));
    }
}
