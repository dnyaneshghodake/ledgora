package com.ledgora.validation.controller;

import com.ledgora.validation.dto.ValidationResult;
import com.ledgora.validation.service.LedgerValidatorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * PART 6: JSP controller for ledger validation admin views. Provides browser-accessible pages for
 * ledger health monitoring.
 */
@Controller
@RequestMapping("/admin/ledger")
public class LedgerValidationViewController {

    private final LedgerValidatorService validatorService;

    public LedgerValidationViewController(LedgerValidatorService validatorService) {
        this.validatorService = validatorService;
    }

    @GetMapping("/view")
    public String viewLedgerStatus(Model model) {
        ValidationResult result = validatorService.getLastResult();
        model.addAttribute("result", result);
        return "validation/ledger-status";
    }

    @GetMapping("/view/validate")
    public String runAndViewValidation(Model model) {
        ValidationResult result = validatorService.runFullValidation();
        model.addAttribute("result", result);
        return "validation/ledger-status";
    }
}
