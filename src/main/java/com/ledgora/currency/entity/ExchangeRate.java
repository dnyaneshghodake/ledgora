package com.ledgora.currency.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PART 6: Exchange rate entity for multi-currency support.
 */
@Entity
@Table(name = "exchange_rates", indexes = {
    @Index(name = "idx_exchange_rate_pair_date", columnList = "currency_from, currency_to, effective_date")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency_from", length = 3, nullable = false)
    private String currencyFrom;

    @Column(name = "currency_to", length = 3, nullable = false)
    private String currencyTo;

    @Column(name = "rate", precision = 19, scale = 8, nullable = false)
    private BigDecimal rate;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
