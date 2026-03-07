package com.ledgora.settlement.controller;

import com.ledgora.common.entity.SystemDate;
import com.ledgora.common.service.BusinessDateService;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.settlement.entity.Settlement;
import com.ledgora.settlement.service.SettlementService;
import com.ledgora.common.enums.SettlementStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Settlement Dashboard Controller - TASK 4
 * Provides settlement status panel and 7-step progress visualization.
 * No business logic modifications - UI wiring only.
 */
@Controller
@RequestMapping("/settlement")
public class SettlementDashboardController {

    private final SettlementService settlementService;
    private final BusinessDateService businessDateService;
    private final LedgerEntryRepository ledgerEntryRepository;

    public SettlementDashboardController(SettlementService settlementService,
                                          BusinessDateService businessDateService,
                                          LedgerEntryRepository ledgerEntryRepository) {
        this.settlementService = settlementService;
        this.businessDateService = businessDateService;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Business Date info
        SystemDate systemDate = businessDateService.getCurrentSystemDate();
        model.addAttribute("businessDate", systemDate.getBusinessDate());
        model.addAttribute("businessDateStatus", systemDate.getStatus().name());

        // Ledger health check
        LocalDate currentDate = systemDate.getBusinessDate();
        BigDecimal totalDebits = ledgerEntryRepository.sumDebitsByBusinessDate(currentDate);
        BigDecimal totalCredits = ledgerEntryRepository.sumCreditsByBusinessDate(currentDate);
        boolean ledgerBalanced = totalDebits.compareTo(totalCredits) == 0;
        String ledgerHealth = ledgerBalanced ? "HEALTHY" : "WARNING";
        model.addAttribute("ledgerHealth", ledgerHealth);
        model.addAttribute("totalDebits", totalDebits);
        model.addAttribute("totalCredits", totalCredits);

        // Last settlement info
        List<Settlement> completedSettlements = settlementService.getSettlementsByStatus(SettlementStatus.COMPLETED);
        if (!completedSettlements.isEmpty()) {
            Settlement lastSettlement = completedSettlements.get(completedSettlements.size() - 1);
            model.addAttribute("lastSettlement", lastSettlement);
            model.addAttribute("lastSettlementTime", lastSettlement.getEndTime());
        }

        // Pending settlements
        List<Settlement> pendingSettlements = settlementService.getSettlementsByStatus(SettlementStatus.PENDING);
        List<Settlement> inProgressSettlements = settlementService.getSettlementsByStatus(SettlementStatus.IN_PROGRESS);
        model.addAttribute("pendingCount", pendingSettlements.size());
        model.addAttribute("inProgressCount", inProgressSettlements.size());

        // Recent settlements for history
        List<Settlement> recentSettlements = settlementService.getAllSettlements();
        model.addAttribute("recentSettlements", recentSettlements);

        return "settlement/settlement-dashboard";
    }
}
