package com.ledgora.stress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured result of an EOD performance stress test run.
 *
 * <p>Captures execution metrics, Hibernate statistics, and validation outcomes. Returned as JSON
 * from the stress test endpoint and logged as a structured summary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EodPerformanceResult {

    // ===== Load generation stats =====
    private int accountsGenerated;
    private int transactionsGenerated;
    private int ibtTransfersGenerated;
    private long loadGenerationTimeMs;

    // ===== Pre-EOD counts =====
    private long totalVouchers;
    private long totalLedgerEntries;
    private long totalTransactions;
    private long totalIbtTransfers;
    private long totalSuspenseCases;

    // ===== EOD execution stats =====
    private long executionTimeMs;
    private long sqlStatementCount;
    private long entityLoadCount;
    private long queryExecutionCount;

    // ===== Validation =====
    private boolean clearingGlZero;
    private boolean suspenseGlZero;
    private boolean success;
    private String failureReason;

    /** Log-friendly structured summary. */
    public String toSummary() {
        return "\n"
                + "╔══════════════════════════════════════════════════╗\n"
                + "║          EOD PERFORMANCE SUMMARY                ║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Load Generation                                 ║\n"
                + "║   Accounts:       "
                + pad(accountsGenerated)
                + "║\n"
                + "║   Transactions:   "
                + pad(transactionsGenerated)
                + "║\n"
                + "║   IBT Transfers:  "
                + pad(ibtTransfersGenerated)
                + "║\n"
                + "║   Gen Time (ms):  "
                + pad(loadGenerationTimeMs)
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Pre-EOD State                                   ║\n"
                + "║   Vouchers:       "
                + pad(totalVouchers)
                + "║\n"
                + "║   Ledger Entries:  "
                + pad(totalLedgerEntries)
                + "║\n"
                + "║   Transactions:   "
                + pad(totalTransactions)
                + "║\n"
                + "║   IBT Records:    "
                + pad(totalIbtTransfers)
                + "║\n"
                + "║   Suspense Cases:  "
                + pad(totalSuspenseCases)
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ EOD Execution                                   ║\n"
                + "║   Time (ms):      "
                + pad(executionTimeMs)
                + "║\n"
                + "║   SQL Statements:  "
                + pad(sqlStatementCount)
                + "║\n"
                + "║   Entity Loads:    "
                + pad(entityLoadCount)
                + "║\n"
                + "║   Query Count:     "
                + pad(queryExecutionCount)
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Validation                                      ║\n"
                + "║   Clearing GL:     "
                + pad(clearingGlZero ? "ZERO ✅" : "NON-ZERO ❌")
                + "║\n"
                + "║   Suspense GL:     "
                + pad(suspenseGlZero ? "ZERO ✅" : "NON-ZERO ❌")
                + "║\n"
                + "║   Status:          "
                + pad(success ? "SUCCESS ✅" : "FAILURE ❌")
                + "║\n"
                + (failureReason != null ? "║   Reason: " + failureReason + "\n" : "")
                + "╚══════════════════════════════════════════════════╝";
    }

    private String pad(Object val) {
        String s = String.valueOf(val);
        return s + " ".repeat(Math.max(0, 30 - s.length()));
    }
}
