package com.ledgora.reporting.service;

import com.ledgora.common.enums.GLAccountType;
import com.ledgora.common.exception.AccountingException;
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
 * RBI Schedule 5 — Balance Sheet Engine.
 *
 * <p>Computes closing balance per GL using ONLY immutable ledger entries:
 *
 * <pre>
 *   closingBalance(GL) = SUM(DEBIT entries where GL) - SUM(CREDIT entries where GL)
 *                         for posting_date <= businessDate
 * </pre>
 *
 * <p>Normal balance convention (Finacle / RBI):
 *
 * <ul>
 *   <li>ASSET / EXPENSE → normal DEBIT → balance = DR - CR (positive = debit balance)
 *   <li>LIABILITY / EQUITY / REVENUE → normal CREDIT → balance = CR - DR (positive = credit
 *       balance)
 * </ul>
 *
 * <p>Validates the accounting equation: TOTAL_ASSETS = TOTAL_LIABILITIES + TOTAL_EQUITY. If
 * violated, throws {@link AccountingException} and prevents snapshot generation.
 */
@Service
public class BalanceSheetEngine {

    private static final Logger log = LoggerFactory.getLogger(BalanceSheetEngine.class);

    private final LedgerEntryRepository ledgerEntryRepository;
    private final StatementLineMappingRepository mappingRepository;

    public BalanceSheetEngine(
            LedgerEntryRepository ledgerEntryRepository,
            StatementLineMappingRepository mappingRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.mappingRepository = mappingRepository;
    }

    /**
     * Generate balance sheet data for a tenant as of a business date.
     *
     * @return map with keys: "sections" (List), "totalAssets", "totalLiabilities", "totalEquity",
     *     "balanceCheck" (boolean)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generate(Long tenantId, LocalDate businessDate) {
        List<StatementLineMapping> mappings =
                mappingRepository.findWithGlByTenantAndType(tenantId, StatementType.BALANCE_SHEET);

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;

        // Group by section
        Map<StatementSection, List<Map<String, Object>>> sectionLines = new LinkedHashMap<>();
        for (StatementSection s :
                new StatementSection[] {
                    StatementSection.ASSET, StatementSection.LIABILITY, StatementSection.EQUITY
                }) {
            sectionLines.put(s, new ArrayList<>());
        }

        for (StatementLineMapping mapping : mappings) {
            String glCode = mapping.getGl().getGlCode();
            GLAccountType glType = mapping.getGl().getAccountType();

            // Compute closing balance from ledger entries
            BigDecimal debits =
                    ledgerEntryRepository.sumDebitsByGlCodeAndDateRange(
                            glCode, tenantId, businessDate);
            BigDecimal credits =
                    ledgerEntryRepository.sumCreditsByGlCodeAndDateRange(
                            glCode, tenantId, businessDate);
            if (debits == null) debits = BigDecimal.ZERO;
            if (credits == null) credits = BigDecimal.ZERO;

            // Apply normal balance rule
            BigDecimal balance = applyNormalBalance(glType, debits, credits);

            Map<String, Object> line = new LinkedHashMap<>();
            line.put("lineName", mapping.getLineName());
            line.put("glCode", glCode);
            line.put("subSection", mapping.getSubSection());
            line.put("amount", balance);

            StatementSection section = mapping.getSection();
            sectionLines.computeIfAbsent(section, k -> new ArrayList<>()).add(line);

            switch (section) {
                case ASSET -> totalAssets = totalAssets.add(balance);
                case LIABILITY -> totalLiabilities = totalLiabilities.add(balance);
                case EQUITY -> totalEquity = totalEquity.add(balance);
                default -> {
                    /* ignore INCOME/EXPENSE in balance sheet */
                }
            }
        }

        // Build section summaries
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

        // Validate accounting equation: A = L + E
        boolean balanced = totalAssets.compareTo(totalLiabilities.add(totalEquity)) == 0;

        if (!balanced) {
            log.error(
                    "BALANCE SHEET EQUATION VIOLATED for tenant {} date {}: "
                            + "Assets={} != Liabilities({}) + Equity({})",
                    tenantId,
                    businessDate,
                    totalAssets,
                    totalLiabilities,
                    totalEquity);
            throw new AccountingException(
                    "BALANCE_SHEET_IMBALANCE",
                    "Balance sheet equation violated: Assets ("
                            + totalAssets
                            + ") != Liabilities ("
                            + totalLiabilities
                            + ") + Equity ("
                            + totalEquity
                            + ") for tenant "
                            + tenantId
                            + " date "
                            + businessDate);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statementDate", businessDate.toString());
        result.put("sections", sections);
        result.put("totalAssets", totalAssets);
        result.put("totalLiabilities", totalLiabilities);
        result.put("totalEquity", totalEquity);
        result.put("balanceCheck", true);
        return result;
    }

    /**
     * Apply normal balance convention.
     *
     * <p>ASSET/EXPENSE: balance = DR - CR (positive = debit balance). LIABILITY/EQUITY/REVENUE:
     * balance = CR - DR (positive = credit balance).
     */
    private BigDecimal applyNormalBalance(
            GLAccountType type, BigDecimal debits, BigDecimal credits) {
        return switch (type) {
            case ASSET, EXPENSE -> debits.subtract(credits);
            case LIABILITY, EQUITY, REVENUE -> credits.subtract(debits);
        };
    }
}
