package com.ledgora.currency.controller;

import com.ledgora.currency.entity.ExchangeRate;
import com.ledgora.currency.repository.ExchangeRateRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * FX Exchange Rate viewer.
 * Route: GET /fx-rates
 */
@Controller
@RequestMapping("/fx-rates")
public class FxRateController {

    private final ExchangeRateRepository exchangeRateRepository;

    public FxRateController(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TENANT_ADMIN', 'SUPER_ADMIN', 'OPERATIONS', 'TELLER')")
    public String fxRates(
            @RequestParam(value = "date", required = false) String dateStr,
            Model model) {
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
        List<ExchangeRate> rates = exchangeRateRepository.findByEffectiveDate(date);
        if (rates.isEmpty()) {
            // Fallback: show all rates if no rates for selected date
            rates = exchangeRateRepository.findAll();
        }
        model.addAttribute("rates", rates);
        model.addAttribute("selectedDate", date);
        model.addAttribute("today", LocalDate.now());
        return "fx/fx-rates";
    }
}
