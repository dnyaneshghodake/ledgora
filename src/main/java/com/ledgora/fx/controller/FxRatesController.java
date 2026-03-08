package com.ledgora.fx.controller;

import com.ledgora.currency.entity.ExchangeRate;
import com.ledgora.currency.repository.ExchangeRateRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * PART 6: FX Rates Controller.
 * Provides the /fx-rates endpoint referenced in the sidebar navigation.
 */
@Controller
@RequestMapping("/fx-rates")
public class FxRatesController {

    private final ExchangeRateRepository exchangeRateRepository;

    public FxRatesController(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @GetMapping
    public String fxRates(Model model) {
        List<ExchangeRate> rates = exchangeRateRepository.findAll();
        model.addAttribute("rates", rates);
        return "admin/fx-rates";
    }
}
