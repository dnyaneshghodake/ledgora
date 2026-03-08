package com.ledgora.voucher.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for the Voucher lifecycle UI screens.
 * Routes: /vouchers, /vouchers/create, /vouchers/pending, /vouchers/posted, /vouchers/cancelled
 */
@Controller
@RequestMapping("/vouchers")
public class VoucherController {

    @GetMapping
    public String voucherInquiry(Model model) {
        // Voucher inquiry - search/filter view
        return "voucher/voucher-inquiry";
    }

    @GetMapping("/create")
    public String createVoucherForm(Model model) {
        return "voucher/voucher-create";
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
