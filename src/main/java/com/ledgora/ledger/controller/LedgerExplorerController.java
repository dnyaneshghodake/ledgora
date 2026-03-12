package com.ledgora.ledger.controller;

import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.ledger.repository.LedgerJournalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Ledger Explorer Controller - TASK 3 Provides read-only access to the immutable ledger entries
 * (system of record). No business logic modifications - UI wiring only.
 */
@Controller
@RequestMapping("/ledger")
public class LedgerExplorerController {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerJournalRepository ledgerJournalRepository;

    public LedgerExplorerController(
            LedgerEntryRepository ledgerEntryRepository,
            LedgerJournalRepository ledgerJournalRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerJournalRepository = ledgerJournalRepository;
    }

    @GetMapping("/explorer")
    public String explorer(
            @RequestParam(value = "glCode", required = false) String glCode,
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            @RequestParam(value = "dateFrom", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate dateTo,
            @RequestParam(value = "businessDate", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate businessDate,
            Model model) {

        List<LedgerEntry> entries;

        if (businessDate != null) {
            entries = ledgerEntryRepository.findByBusinessDate(businessDate);
        } else if (glCode != null && !glCode.isEmpty()) {
            entries = ledgerEntryRepository.findByGlAccountCode(glCode);
        } else if (accountNumber != null && !accountNumber.isEmpty()) {
            entries = ledgerEntryRepository.findByAccountNumber(accountNumber);
        } else if (dateFrom != null && dateTo != null) {
            LocalDateTime startDateTime = dateFrom.atStartOfDay();
            LocalDateTime endDateTime = dateTo.atTime(LocalTime.MAX);
            entries = ledgerEntryRepository.findByDateRange(startDateTime, endDateTime);
        } else {
            // Default: show today's business date entries
            LocalDate currentDate = businessDateService.getCurrentBusinessDate();
            entries = ledgerEntryRepository.findByBusinessDate(currentDate);
            businessDate = currentDate;
        }

        // PART 12: Calculate totals with null-safe access for entries
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            if (entry.getEntryType() == null || entry.getAmount() == null) {
                continue; // Skip entries with null type or amount
            }
            if ("DEBIT".equals(entry.getEntryType().name())) {
                totalDebits = totalDebits.add(entry.getAmount());
            } else {
                totalCredits = totalCredits.add(entry.getAmount());
            }
        }
        boolean isBalanced = totalDebits.compareTo(totalCredits) == 0;

        model.addAttribute("entries", entries);
        model.addAttribute("totalDebits", totalDebits);
        model.addAttribute("totalCredits", totalCredits);
        model.addAttribute("isBalanced", isBalanced);
        model.addAttribute("entryCount", entries.size());

        // Preserve filter values
        model.addAttribute("filterGlCode", glCode);
        model.addAttribute("filterAccountNumber", accountNumber);
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);
        model.addAttribute("filterBusinessDate", businessDate);

        return "ledger/explorer";
    }

    /** AJAX endpoint to fetch journal details for modal display. */
    @GetMapping("/journal/{journalId}/entries")
    @ResponseBody
    public Map<String, Object> getJournalEntries(@PathVariable Long journalId) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByJournalId(journalId);
        Map<String, Object> result = new HashMap<>();
        result.put("journalId", journalId);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            // PART 12: Null-safe access for AJAX endpoint
            if (entry.getEntryType() == null || entry.getAmount() == null) {
                continue;
            }
            if ("DEBIT".equals(entry.getEntryType().name())) {
                totalDebits = totalDebits.add(entry.getAmount());
            } else {
                totalCredits = totalCredits.add(entry.getAmount());
            }
        }

        // Build a simple list of maps for JSON response
        List<Map<String, Object>> entryList =
                entries.stream()
                        .filter(e -> e.getEntryType() != null && e.getAmount() != null)
                        .map(
                                e -> {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("id", e.getId());
                                    map.put("entryType", e.getEntryType().name());
                                    map.put("amount", e.getAmount());
                                    map.put("currency", e.getCurrency());
                                    map.put("glAccountCode", e.getGlAccountCode());
                                    map.put("narration", e.getNarration());
                                    map.put("balanceAfter", e.getBalanceAfter());
                                    if (e.getAccount() != null) {
                                        map.put("accountNumber", e.getAccount().getAccountNumber());
                                    }
                                    return map;
                                })
                        .toList();

        result.put("entries", entryList);
        result.put("totalDebits", totalDebits);
        result.put("totalCredits", totalCredits);
        result.put("isBalanced", totalDebits.compareTo(totalCredits) == 0);

        if (!entries.isEmpty() && entries.get(0).getJournal() != null) {
            result.put("description", entries.get(0).getJournal().getDescription());
            result.put("businessDate", entries.get(0).getJournal().getBusinessDate().toString());
        }

        return result;
    }
}
