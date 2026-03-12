package com.ledgora.validation.controller;

import com.ledgora.validation.dto.ValidationResult;
import com.ledgora.validation.service.LedgerValidatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ledger Validation controller — serves both the JSP dashboard and the REST API.
 *
 * <p>JSP routes (for sidebar):
 *
 * <ul>
 *   <li>GET /admin/ledger/view/validate — Show current ledger status (validation/ledger-status.jsp)
 *   <li>POST /admin/ledger/view/validate — Run full validation and refresh
 * </ul>
 *
 * <p>REST API routes (for programmatic access):
 *
 * <ul>
 *   <li>GET /admin/ledger/validate — Full validation result (JSON)
 *   <li>GET /admin/ledger/status — Status string (JSON)
 * </ul>
 */
@Controller
@RequestMapping("/admin/ledger")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS', 'AUDITOR', 'SUPER_ADMIN')")
public class LedgerValidationController {

    private final LedgerValidatorService validatorService;

    public LedgerValidationController(LedgerValidatorService validatorService) {
        this.validatorService = validatorService;
    }

    /** JSP dashboard — show current cached validation status. */
    @GetMapping("/view/validate")
    public String showValidationStatus(Model model) {
        ValidationResult result = validatorService.getLastResult();
        model.addAttribute("result", result);
        return "validation/ledger-status";
    }

    /** JSP dashboard — trigger full validation and redirect back. */
    @PostMapping("/view/validate")
    public String runFullValidation(RedirectAttributes redirectAttributes) {
        try {
            ValidationResult result = validatorService.runFullValidation();
            redirectAttributes.addFlashAttribute("result", result);
            if (result.getStatus() == ValidationResult.Status.HEALTHY) {
                redirectAttributes.addFlashAttribute(
                        "message",
                        "Full validation completed: HEALTHY. "
                                + result.getTransactionsChecked()
                                + " transactions, "
                                + result.getAccountsChecked()
                                + " accounts checked.");
            } else {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Validation issues found: "
                                + result.getErrors().size()
                                + " error(s), "
                                + result.getWarnings().size()
                                + " warning(s).");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Validation failed: " + e.getMessage());
        }
        return "redirect:/admin/ledger/view/validate";
    }

    /** REST API — run full validation (JSON response). */
    @GetMapping("/validate")
    @ResponseBody
    public ResponseEntity<ValidationResult> validateLedger() {
        ValidationResult result = validatorService.runFullValidation();
        return ResponseEntity.ok(result);
    }

    /** REST API — get current status string (JSON response). */
    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<String> getLedgerStatus() {
        ValidationResult result = validatorService.getLastResult();
        return ResponseEntity.ok(result.getStatus().name());
    }
}
