package com.ledgora.reporting.service;

import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.reporting.entity.StatementLineMapping;
import com.ledgora.reporting.enums.StatementSection;
import com.ledgora.reporting.enums.StatementType;
import com.ledgora.reporting.repository.StatementLineMappingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI Schedule 14 — Profit & Loss Account Engine.
 *
 * <p>Date-range based P&L computation from immutable ledger entries:
 *
 * <pre>
 *   Revenue = SUM(CREDIT on income GLs) - SUM(DEBIT on income GLs) for date range
 *   Expense = SUM(DEBIT on expense GLs) - SUM(CREDIT on expense GLs) for date range
 *   Net Profit = Revenue - Expense
 * </pre>
 *
 * <p>Supports daily, monthly, and financial year periods per RBI reporting requirements.
 */
@Service
public class PnlEngine {

    private static final Logger log = LoggerFactory.getLogger(PnlEngine.class);

    private final LedgerEntryRepository ledgerEntryRepository;
    private final StatementLineMappingRepository mappingRepository;

    public PnlEngine(
            LedgerEntryRepository ledgerEntryRepository,
            StatementLineMappingRepository mappingRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.mappingRepository = mappingRepository;
    }

    /**
     * Generate P&L for a tenant over a date range.
     *
     * @return map with keys: "sections", "totalRevenue", "totalExpense", "netProfit"
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generate(
            Long tenantId, LocalDate startDate, LocalDate endDate) {
        List<StatementLineMapping> mappings =
                mappingRepository.findWithGlByTenantAndType(tenantId, StatementType.PNL);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        Map<StatementSection, List<Map<String, Object>>> sectionLines = new LinkedHashMap<>();
        sectionLines.put(StatementSection.INCOME, new ArrayList<>());
        sectionLines.put(StatementSection.EXPENSE, new ArrayList<>());

        for (StatementLineMapping mapping : mappings) {
            String glCode = mapping.getGl().getGlCode();

            BigDecimal debits =
                    ledgerEntryRepository.sumDebitsByGlCodeAndTenantAndDateRange(
                            glCode, tenantId, startDate, endDate);
            BigDecimal credits =
                    ledgerEntryRepository.sumCreditsByGlCodeAndTenantAndDateRange(
                            glCode, tenantId, startDate, endDate);
            if (debits == null) debits = BigDecimal.ZERO;
            if (credits == null) credits = BigDecimal.ZERO;

            StatementSection section = mapping.getSection();
            BigDecimal amount;
            if (section == StatementSection.INCOME) {
                // Revenue: normal credit — CR - DR
                amount = credits.subtract(debits);
                totalRevenue = totalRevenue.add(amount);
            } else {
                // Expense: normal debit — DR - CR
                amount = debits.subtract(credits);
                totalExpense = totalExpense.add(amount);
            }

            Map<String, Object> line = new LinkedHashMap<>();
            line.put("lineName", mapping.getLineName());
            line.put("glCode", glCode);
            line.put("subSection", mapping.getSubSection());
            line.put("amount", amount);

            sectionLines.computeIfAbsent(section, k -> new ArrayList<>()).add(line);
        }

        List<Map<String, Object>> sections = new ArrayList<>();
        for (var entry : sectionLines.entrySet()) {
            BigDecimal sectionTotal =
                    entry.getValue().stream()
                            .map(l -> (BigDecimal) l.get("amount"))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> sec = new LinkedHashMap<>();
            sec.put("section", entry.getKey().name());
            sec.put("lines", entry.getValue());
            sec.put("sectionTotal", sectionTotal);
            sections.add(sec);
        }

        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("sections", sections);
        result.put("totalRevenue", totalRevenue);
        result.put("totalExpense", totalExpense);
        result.put("netProfit", netProfit);
        return result;
    }

    /** Daily P&L — convenience method for a single business date. */
    @Transactional(readOnly = true)
    public Map<String, Object> generateDaily(Long tenantId, LocalDate businessDate) {
        return generate(tenantId, businessDate, businessDate);
    }

    /** Monthly P&L — first day to last day of the month containing businessDate. */
    @Transactional(readOnly = true)
    public Map<String, Object> generateMonthly(Long tenantId, LocalDate businessDate) {
        LocalDate start = businessDate.withDayOfMonth(1);
        LocalDate end = businessDate.withDayOfMonth(businessDate.lengthOfMonth());
        return generate(tenantId, start, end);
    }

    /** Financial year P&L — April 1 to March 31 per RBI convention. */
    @Transactional(readOnly = true)
    public Map<String, Object> generateFinancialYear(Long tenantId, LocalDate businessDate) {
        int fyStartYear = businessDate.getMonthValue() >= 4
                ? businessDate.getYear()
                : businessDate.getYear() - 1;
        LocalDate start = LocalDate.of(fyStartYear, 4, 1);
        LocalDate end = LocalDate.of(fyStartYear + 1, 3, 31);
        return generate(tenantId, start, end);
    }
}
