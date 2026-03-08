package com.ledgora.eod.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for End of Day (EOD) UI screens.
 * Routes: /eod/validate, /eod/run, /eod/status
 */
@Controller
@RequestMapping("/eod")
public class EodController {

    @GetMapping("/validate")
    public String validateEod(Model model) {
        // Default validation flags - actual values populated by service layer
        model.addAttribute("allVouchersPosted", true);
        model.addAttribute("branchBalanced", true);
        model.addAttribute("clearingGlZero", true);
        model.addAttribute("noPendingAuth", true);
        return "eod/eod-validate";
    }

    @GetMapping("/run")
    public String runEodForm(Model model) {
        return "eod/eod-run";
    }

    @GetMapping("/status")
    public String businessDateStatus(Model model) {
        return "eod/eod-status";
    }
}
