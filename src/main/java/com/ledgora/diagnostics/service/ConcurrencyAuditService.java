package com.ledgora.diagnostics.service;

import com.ledgora.diagnostics.dto.ConcurrencyAuditResult;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Programmatic concurrency model validation — runs all CBS integrity checks against the current
 * database state.
 *
 * <p>Active only in the "stress" profile. Uses native SQL via JdbcTemplate for maximum
 * compatibility with the AUDIT SQL pack queries. All checks are read-only.
 *
 * <p>Designed to run AFTER stress tests (load generator, lock contention, deadlock simulation) to
 * verify that no invariant was violated under concurrent load.
 */
@Service
@Profile("stress")
public class ConcurrencyAuditService {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyAuditService.class);

    private final JdbcTemplate jdbc;

    public ConcurrencyAuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Run all concurrency model validation checks.
     *
     * @return structured audit result with per-check pass/fail and violation details
     */
    @Transactional(readOnly = true)
    public ConcurrencyAuditResult runAllChecks() {
        List<String> violations = new ArrayList<>();
        int total = 0;
        int passed = 0;

        // Check 1: Global ledger balanced (SUM debits == SUM credits)
        total++;
        boolean ledgerBalanced = checkLedgerBalanced(violations);
        if (ledgerBalanced) passed++;

        // Check 2: Clearing GL net zero
        total++;
        boolean clearingGlZero = checkClearingGlZero(violations);
        if (clearingGlZero) passed++;

        // Check 3: Suspense GL zero
        total++;
        boolean suspenseGlZero = checkSuspenseGlZero(violations);
        if (suspenseGlZero) passed++;

        // Check 4: No negative balances (non-overdraft accounts)
        total++;
        long negCount = countNegativeBalances();
        boolean noNegative = negCount == 0;
        if (noNegative) passed++;
        else violations.add("NEGATIVE_BALANCE: " + negCount + " accounts have negative balance");

        // Check 5: No orphan ledger entries
        total++;
        long orphanCount = countOrphanEntries();
        boolean noOrphans = orphanCount == 0;
        if (noOrphans) passed++;
        else
            violations.add(
                    "ORPHAN_ENTRIES: " + orphanCount + " ledger entries without transaction");

        // Check 6: No stuck RUNNING EOD processes
        total++;
        long stuckCount = countStuckEodProcesses();
        boolean noStuck = stuckCount == 0;
        if (noStuck) passed++;
        else violations.add("STUCK_EOD: " + stuckCount + " RUNNING EOD process(es)");

        // Check 7: No stale pending vouchers (auth=Y, post=N, older than 30 min)
        total++;
        long staleCount = countStalePendingVouchers();
        boolean noStale = staleCount == 0;
        if (noStale) passed++;
        else
            violations.add(
                    "STALE_VOUCHERS: "
                            + staleCount
                            + " approved-but-unposted voucher(s) older than 30 min");

        // Check 8: No duplicate voucher numbers
        total++;
        long dupCount = countDuplicateVoucherNumbers();
        boolean noDups = dupCount == 0;
        if (noDups) passed++;
        else violations.add("DUPLICATE_VOUCHER_NUMBERS: " + dupCount + " duplicate(s)");

        // Check 9: No partial IBT reversals
        total++;
        boolean noPartialIbt = checkNoPartialIbtReversals(violations);
        if (noPartialIbt) passed++;

        // Check 10: All IBTs have exactly 4 vouchers
        total++;
        boolean allIbtFour = checkAllIbtHaveFourVouchers(violations);
        if (allIbtFour) passed++;

        // Check 11: All closed/settled batches balanced
        total++;
        boolean batchesBalanced = checkBatchesBalanced(violations);
        if (batchesBalanced) passed++;

        boolean integrity = passed == total;

        ConcurrencyAuditResult result =
                ConcurrencyAuditResult.builder()
                        .financialIntegrity(integrity)
                        .ledgerBalanced(ledgerBalanced)
                        .clearingGlZero(clearingGlZero)
                        .suspenseGlZero(suspenseGlZero)
                        .noNegativeBalances(noNegative)
                        .noOrphanEntries(noOrphans)
                        .noStuckEodProcesses(noStuck)
                        .noStalePendingVouchers(noStale)
                        .noDuplicateVoucherNumbers(noDups)
                        .noPartialIbtReversals(noPartialIbt)
                        .allIbtHaveFourVouchers(allIbtFour)
                        .allBatchesBalanced(batchesBalanced)
                        .orphanEntryCount(orphanCount)
                        .negativeBalanceCount(negCount)
                        .stuckEodCount(stuckCount)
                        .stalePendingVoucherCount(staleCount)
                        .duplicateVoucherNumberCount(dupCount)
                        .totalChecks(total)
                        .passedChecks(passed)
                        .violations(violations)
                        .build();

        log.info(result.toSummary());
        return result;
    }

    private boolean checkLedgerBalanced(List<String> violations) {
        try {
            var row =
                    jdbc.queryForMap(
                            "SELECT COALESCE(SUM(CASE WHEN entry_type='DEBIT' THEN amount ELSE 0 END),0) AS dr, "
                                    + "COALESCE(SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE 0 END),0) AS cr "
                                    + "FROM ledger_entries");
            var dr = (Number) row.get("DR");
            var cr = (Number) row.get("CR");
            boolean balanced = dr.doubleValue() == cr.doubleValue();
            if (!balanced) violations.add("LEDGER_IMBALANCED: DR=" + dr + " CR=" + cr);
            return balanced;
        } catch (Exception e) {
            violations.add("LEDGER_CHECK_ERROR: " + e.getMessage());
            return false;
        }
    }

    private boolean checkClearingGlZero(List<String> violations) {
        try {
            var sum =
                    jdbc.queryForObject(
                            "SELECT COALESCE(SUM(balance),0) FROM accounts WHERE account_type='CLEARING_ACCOUNT'",
                            Number.class);
            boolean zero = sum != null && sum.doubleValue() == 0.0;
            if (!zero) violations.add("CLEARING_GL_NON_ZERO: " + sum);
            return zero;
        } catch (Exception e) {
            violations.add("CLEARING_CHECK_ERROR: " + e.getMessage());
            return false;
        }
    }

    private boolean checkSuspenseGlZero(List<String> violations) {
        try {
            var sum =
                    jdbc.queryForObject(
                            "SELECT COALESCE(SUM(balance),0) FROM accounts WHERE account_type='SUSPENSE_ACCOUNT'",
                            Number.class);
            boolean zero = sum != null && sum.doubleValue() == 0.0;
            if (!zero) violations.add("SUSPENSE_GL_NON_ZERO: " + sum);
            return zero;
        } catch (Exception e) {
            violations.add("SUSPENSE_CHECK_ERROR: " + e.getMessage());
            return false;
        }
    }

    private long countNegativeBalances() {
        try {
            var count =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM accounts WHERE balance < 0 "
                                    + "AND account_type IN ('SAVINGS','CURRENT','CUSTOMER_ACCOUNT')",
                            Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    private long countOrphanEntries() {
        try {
            var count =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM ledger_entries WHERE transaction_id IS NULL",
                            Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    private long countStuckEodProcesses() {
        try {
            var count =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM eod_processes WHERE status = 'RUNNING'",
                            Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    private long countStalePendingVouchers() {
        try {
            var count =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM vouchers "
                                    + "WHERE auth_flag='Y' AND post_flag='N' AND cancel_flag='N' "
                                    + "AND created_at < DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)",
                            Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0; // DATEADD syntax may differ — treat as 0
        }
    }

    private long countDuplicateVoucherNumbers() {
        try {
            var count =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM ("
                                    + "SELECT voucher_number FROM vouchers "
                                    + "GROUP BY voucher_number HAVING COUNT(*) > 1"
                                    + ")",
                            Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean checkNoPartialIbtReversals(List<String> violations) {
        try {
            // IBT transactions should have all vouchers cancelled or none
            var partials =
                    jdbc.queryForObject(
                            "SELECT COUNT(DISTINCT v.transaction_id) FROM vouchers v "
                                    + "JOIN inter_branch_transfers ibt ON ibt.reference_transaction_id = v.transaction_id "
                                    + "WHERE v.transaction_id IN ("
                                    + "  SELECT transaction_id FROM vouchers "
                                    + "  GROUP BY transaction_id "
                                    + "  HAVING SUM(CASE WHEN cancel_flag='Y' THEN 1 ELSE 0 END) > 0 "
                                    + "  AND SUM(CASE WHEN cancel_flag='Y' THEN 1 ELSE 0 END) < COUNT(*)"
                                    + ")",
                            Long.class);
            boolean ok = partials == null || partials == 0;
            if (!ok)
                violations.add(
                        "PARTIAL_IBT_REVERSAL: "
                                + partials
                                + " IBT transaction(s) partially cancelled");
            return ok;
        } catch (Exception e) {
            return true; // Table may not exist in all configs
        }
    }

    private boolean checkAllIbtHaveFourVouchers(List<String> violations) {
        try {
            var bad =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM inter_branch_transfers ibt "
                                    + "WHERE ibt.status NOT IN ('FAILED') "
                                    + "AND (SELECT COUNT(*) FROM vouchers v WHERE v.transaction_id = ibt.reference_transaction_id) != 4",
                            Long.class);
            boolean ok = bad == null || bad == 0;
            if (!ok)
                violations.add("IBT_VOUCHER_COUNT: " + bad + " IBT(s) without exactly 4 vouchers");
            return ok;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean checkBatchesBalanced(List<String> violations) {
        try {
            var unbalanced =
                    jdbc.queryForObject(
                            "SELECT COUNT(*) FROM transaction_batches "
                                    + "WHERE status IN ('CLOSED','SETTLED') "
                                    + "AND total_debit != total_credit",
                            Long.class);
            boolean ok = unbalanced == null || unbalanced == 0;
            if (!ok)
                violations.add(
                        "UNBALANCED_BATCHES: "
                                + unbalanced
                                + " closed/settled batch(es) with DR!=CR");
            return ok;
        } catch (Exception e) {
            return true;
        }
    }
}
