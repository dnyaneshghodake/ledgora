package com.ledgora.settlement.controller;

import com.ledgora.common.enums.SettlementStatus;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.settlement.entity.Settlement;
import com.ledgora.settlement.service.SettlementService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Settlement Dashboard Controller - TASK 4 Provides settlement status panel and 7-step progress
 * visualization. No business logic modifications - UI wiring only.
 */
@Controller
@RequestMapping("/settlement")
public class SettlementDashboardController {

    private final SettlementService settlementService;
    private final LedgerEntryRepository ledgerEntryRepository;

    public SettlementDashboardController(
            SettlementService settlementService, LedgerEntryRepository ledgerEntryRepository) {
        this.settlementService = settlementService;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Business Date info — use system date for display
        LocalDate currentDate = LocalDate.now();
        model.addAttribute("businessDate", currentDate);
        model.addAttribute("businessDateStatus", "OPEN");

        // Ledger health check
        BigDecimal totalDebits = ledgerEntryRepository.sumDebitsByBusinessDate(currentDate);
        BigDecimal totalCredits = ledgerEntryRepository.sumCreditsByBusinessDate(currentDate);
        boolean ledgerBalanced = totalDebits.compareTo(totalCredits) == 0;
        String ledgerHealth = ledgerBalanced ? "HEALTHY" : "WARNING";
        model.addAttribute("ledgerHealth", ledgerHealth);
        model.addAttribute("totalDebits", totalDebits);
        model.addAttribute("totalCredits", totalCredits);

        // Last settlement info
        List<Settlement> completedSettlements =
                settlementService.getSettlementsByStatus(SettlementStatus.COMPLETED);
        if (!completedSettlements.isEmpty()) {
            Settlement lastSettlement = completedSettlements.get(completedSettlements.size() - 1);
            model.addAttribute("lastSettlement", lastSettlement);
            model.addAttribute("lastSettlementTime", lastSettlement.getEndTime());
        }

        // Pending settlements
        List<Settlement> pendingSettlements =
                settlementService.getSettlementsByStatus(SettlementStatus.PENDING);
        List<Settlement> inProgressSettlements =
                settlementService.getSettlementsByStatus(SettlementStatus.IN_PROGRESS);
        model.addAttribute("pendingCount", pendingSettlements.size());
        model.addAttribute("inProgressCount", inProgressSettlements.size());

        // Recent settlements for history
        List<Settlement> recentSettlements = settlementService.getAllSettlements();
        model.addAttribute("recentSettlements", recentSettlements);

        return "settlement/settlement-dashboard";
    }
}
