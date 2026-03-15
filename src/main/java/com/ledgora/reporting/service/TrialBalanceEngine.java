package com.ledgora.reporting.service;

import com.ledgora.common.enums.GLAccountType;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.reporting.dto.TrialBalanceReport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI-grade Trial Balance Generator — date-aware, tenant-isolated.
 *
 * <p>Computes trial balance from immutable ledger entries ONLY (never AccountBalance). Uses the
 * optimised GL-level aggregation queries added for the Financial Statement Engine.
 *
 * <p>RBI Master Circular on Financial Statements:
 *
 * <ul>
 *   <li>Trial Balance must balance: SUM(debit balances) = SUM(credit balances)
 *   <li>Each GL account classified by normal balance (DEBIT or CREDIT)
 *   <li>Asset/Expense GLs carry debit balances; Liability/Equity/Revenue carry credit balances
 *   <li>Any imbalance indicates a double-entry violation — must be investigated before EOD
 * </ul>
 *
 * <p>Finacle convention: Trial Balance is point-in-time (as-of date), not period-based. All
 * ledger entries with businessDate <= asOfDate are included.
 */
@Service
public class TrialBalanceEngine {

    private static final Logger log = LoggerFactory.getLogger(TrialBalanceEngine.class);

    private final GeneralLedgerRepository glRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TrialBalanceEngine(
            GeneralLedgerRepository glRepository,
            LedgerEntryRepository ledgerEntryRepository) {
        this.glRepository = glRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Generate a trial balance as-of a specific business date for a tenant.
     *
     * <p>Uses GL-level aggregation queries (COALESCE-protected, no null returns). Each GL's
     * closing balance is computed as cumulative DR - CR (or CR - DR for credit-normal GLs) for
     * all ledger entries with businessDate <= asOfDate.
     *
     * @param tenantId tenant isolation
     * @param asOfDate point-in-time date (inclusive)
     * @return TrialBalanceReport with balanced flag
     */
    @Transactional(readOnly = true)
    public TrialBalanceReport generate(Long tenantId, LocalDate asOfDate) {
        List<GeneralLedger> allGl = glRepository.findByTenantIdOrShared(tenantId);
        List<TrialBalanceReport.TrialBalanceLine> lines = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (GeneralLedger gl : allGl) {
            BigDecimal debits =
                    ledgerEntryRepository.sumDebitsByGlCodeAndDateRange(
                            gl.getGlCode(), tenantId, asOfDate);
            BigDecimal credits =
                    ledgerEntryRepository.sumCreditsByGlCodeAndDateRange(
                            gl.getGlCode(), tenantId, asOfDate);

            // Skip GLs with no activity
            if (debits.compareTo(BigDecimal.ZERO) == 0
                    && credits.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // Apply normal balance convention
            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;
            GLAccountType type = gl.getAccountType();

            if (type == GLAccountType.ASSET || type == GLAccountType.EXPENSE) {
                // Normal debit: balance = DR - CR
                debitBalance = debits.subtract(credits);
                if (debitBalance.compareTo(BigDecimal.ZERO) < 0) {
                    // Contra balance — show on credit side
                    creditBalance = debitBalance.negate();
                    debitBalance = BigDecimal.ZERO;
                }
            } else {
                // Normal credit: balance = CR - DR
                creditBalance = credits.subtract(debits);
                if (creditBalance.compareTo(BigDecimal.ZERO) < 0) {
                    debitBalance = creditBalance.negate();
                    creditBalance = BigDecimal.ZERO;
                }
            }

            lines.add(
                    TrialBalanceReport.TrialBalanceLine.builder()
                            .glCode(gl.getGlCode())
                            .glName(gl.getGlName())
                            .accountType(type.name())
                            .debitBalance(debitBalance)
                            .creditBalance(creditBalance)
                            .build());

            totalDebits = totalDebits.add(debitBalance);
            totalCredits = totalCredits.add(creditBalance);
        }

        boolean balanced = totalDebits.compareTo(totalCredits) == 0;

        if (!balanced) {
            log.warn(
                    "TRIAL BALANCE IMBALANCE for tenant {} date {}: DR={} CR={} diff={}",
                    tenantId,
                    asOfDate,
                    totalDebits,
                    totalCredits,
                    totalDebits.subtract(totalCredits));
        }

        return TrialBalanceReport.builder()
                .reportDate(asOfDate)
                .lines(lines)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .balanced(balanced)
                .build();
    }
}
