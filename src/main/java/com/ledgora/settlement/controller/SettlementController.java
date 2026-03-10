package com.ledgora.settlement.controller;

import com.ledgora.common.enums.SettlementStatus;
import com.ledgora.settlement.entity.Settlement;
import com.ledgora.settlement.service.SettlementService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public String listSettlements(
            @RequestParam(value = "status", required = false) String status, Model model) {
        if (status != null && !status.isEmpty()) {
            model.addAttribute(
                    "settlements",
                    settlementService.getSettlementsByStatus(SettlementStatus.valueOf(status)));
            model.addAttribute("selectedStatus", status);
        } else {
            model.addAttribute("settlements", settlementService.getAllSettlements());
        }
        model.addAttribute("statuses", SettlementStatus.values());
        return "settlement/settlements";
    }

    @GetMapping("/{id}")
    public String viewSettlement(@PathVariable Long id, Model model) {
        Settlement settlement =
                settlementService
                        .getSettlementById(id)
                        .orElseThrow(() -> new RuntimeException("Settlement not found"));
        model.addAttribute("settlement", settlement);
        return "settlement/settlement-view";
    }

    @GetMapping("/process")
    public String processForm(Model model) {
        model.addAttribute("settlementDate", LocalDate.now());
        return "settlement/settlement-process";
    }

    @PostMapping("/process")
    public String processSettlement(
            @RequestParam("settlementDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date,
            RedirectAttributes redirectAttributes) {
        try {
            Settlement settlement = settlementService.processSettlement(date);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Settlement processed successfully! Ref: "
                            + settlement.getSettlementRef()
                            + " | Transactions: "
                            + settlement.getTransactionCount());
            return "redirect:/settlements";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/settlements/process";
        }
    }
}
