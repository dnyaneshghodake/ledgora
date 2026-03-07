package com.ledgora.validation.controller;

import com.ledgora.validation.dto.ValidationResult;
import com.ledgora.validation.service.LedgerValidatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PART 6: Admin endpoint for ledger validation.
 * GET /admin/ledger/validate -> HEALTHY, WARNING, CORRUPTED
 * Restricted to ADMIN role via SecurityConfig.
 */
@RestController
@RequestMapping("/admin/ledger")
public class LedgerValidationController {

    private final LedgerValidatorService validatorService;

    public LedgerValidationController(LedgerValidatorService validatorService) {
        this.validatorService = validatorService;
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidationResult> validateLedger() {
        ValidationResult result = validatorService.runFullValidation();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<String> getLedgerStatus() {
        ValidationResult result = validatorService.getLastResult();
        return ResponseEntity.ok(result.getStatus().name());
    }
}
