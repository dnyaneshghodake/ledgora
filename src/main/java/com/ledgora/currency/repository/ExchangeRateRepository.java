package com.ledgora.currency.repository;

import com.ledgora.currency.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("SELECT er FROM ExchangeRate er WHERE er.currencyFrom = :from AND er.currencyTo = :to " +
           "AND er.effectiveDate <= :date ORDER BY er.effectiveDate DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRate(@Param("from") String from, @Param("to") String to,
                                          @Param("date") LocalDate date);

    List<ExchangeRate> findByCurrencyFromAndCurrencyTo(String currencyFrom, String currencyTo);

    List<ExchangeRate> findByEffectiveDate(LocalDate effectiveDate);
}
