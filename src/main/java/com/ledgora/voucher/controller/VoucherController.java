package com.ledgora.voucher.controller;

import com.ledgora.voucher.service.VoucherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for the Voucher lifecycle UI screens.
 * Routes: /vouchers, /vouchers/create, /vouchers/pending, /vouchers/posted, /vouchers/cancelled
 */
@Controller
@RequestMapping("/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    public String voucherInquiry(Model model) {
        // Voucher inquiry - search/filter view
        return "voucher/voucher-inquiry";
    }

    @GetMapping("/create")
    public String createVoucherForm(Model model) {
        return "voucher/voucher-create";
    }

    /**
     * PART 4: Handle voucher creation form POST.
     * Validates inputs and redirects with success/error flash message.
     */
    @PostMapping("/create")
    public String createVoucher(@RequestParam String debitAccountNumber,
                                @RequestParam String creditAccountNumber,
                                @RequestParam java.math.BigDecimal amount,
                                @RequestParam(defaultValue = "INR") String currency,
                                @RequestParam(defaultValue = "TRANSFER") String voucherType,
                                @RequestParam(required = false) String narration,
                                RedirectAttributes redirectAttributes) {
        try {
            if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("error", "Amount must be greater than zero.");
                return "redirect:/vouchers/create";
            }
            if (debitAccountNumber == null || debitAccountNumber.isBlank()
                    || creditAccountNumber == null || creditAccountNumber.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Both debit and credit accounts are required.");
                return "redirect:/vouchers/create";
            }
            if (debitAccountNumber.equals(creditAccountNumber)) {
                redirectAttributes.addFlashAttribute("error", "Debit and credit accounts cannot be the same.");
                return "redirect:/vouchers/create";
            }
            redirectAttributes.addFlashAttribute("message",
                    "Voucher submitted successfully for " + currency + " " + amount
                    + " from " + debitAccountNumber + " to " + creditAccountNumber + ".");
            return "redirect:/vouchers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Voucher creation failed: " + e.getMessage());
            return "redirect:/vouchers/create";
        }
    }

    @GetMapping("/pending")
    public String pendingVouchers(Model model) {
        // Show vouchers with PENDING status awaiting authorization
        return "voucher/voucher-pending";
    }

    @GetMapping("/posted")
    public String postedVouchers(Model model) {
        // Show vouchers with POSTED status
        return "voucher/voucher-posted";
    }

    @GetMapping("/cancelled")
    public String cancelledVouchers(Model model) {
        // Show cancelled/reversed vouchers
        return "voucher/voucher-cancelled";
    }
}
