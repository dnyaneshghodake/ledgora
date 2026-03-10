package com.ledgora.diagnostics.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Enterprise CBS Certification Report — structured output of the full certification pipeline. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterpriseCertificationReport {

    // Volume
    private int totalTransactions;
    private long totalVouchers;
    private int totalIbtTransfers;
    private long totalSuspenseCases;

    // Financial integrity
    private boolean ledgerBalanced;
    private boolean clearingNetZero;
    private boolean suspenseZero;
    private boolean ibtIntegrity;
    private boolean noOrphanEntries;

    // Concurrency safety
    private boolean concurrencySafe;
    private boolean noNegativeBalances;
    private boolean noDuplicateVouchers;
    private boolean noPartialIbtReversal;
    private boolean noStuckEod;

    // Crash recovery
    private boolean crashRecoverySafe;
    private boolean eodResumeSucceeded;
    private boolean singletonEodEnforced;

    // Performance
    private long totalExecutionTimeMs;
    private long eodExecutionTimeMs;
    private long loadGenerationTimeMs;
    private int deadlocksRecovered;
    private boolean performanceWithinThreshold;

    // Final grade
    private String finalGrade;
    private List<String> violations;
    private List<String> stepResults;

    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║      ENTERPRISE CBS CERTIFICATION REPORT                 ║\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        sb.append("║ Volume                                                   ║\n");
        sb.append("║   Transactions:     ").append(pad(totalTransactions, 37)).append("║\n");
        sb.append("║   Vouchers:         ").append(pad(totalVouchers, 37)).append("║\n");
        sb.append("║   IBT Transfers:    ").append(pad(totalIbtTransfers, 37)).append("║\n");
        sb.append("║   Suspense Cases:   ").append(pad(totalSuspenseCases, 37)).append("║\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        sb.append("║ Financial Integrity                                      ║\n");
        sb.append("║   Ledger Balanced:    ").append(flag(ledgerBalanced, 35)).append("║\n");
        sb.append("║   Clearing Net Zero:  ").append(flag(clearingNetZero, 35)).append("║\n");
        sb.append("║   Suspense Zero:      ").append(flag(suspenseZero, 35)).append("║\n");
        sb.append("║   IBT Integrity:      ").append(flag(ibtIntegrity, 35)).append("║\n");
        sb.append("║   No Orphan Entries:   ").append(flag(noOrphanEntries, 34)).append("║\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        sb.append("║ Concurrency Safety                                       ║\n");
        sb.append("║   No Negative Bal:    ").append(flag(noNegativeBalances, 35)).append("║\n");
        sb.append("║   No Dup Vouchers:    ").append(flag(noDuplicateVouchers, 35)).append("║\n");
        sb.append("║   No Partial IBT Rev: ").append(flag(noPartialIbtReversal, 35)).append("║\n");
        sb.append("║   No Stuck EOD:       ").append(flag(noStuckEod, 35)).append("║\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        sb.append("║ Crash Recovery                                           ║\n");
        sb.append("║   EOD Resume:         ").append(flag(eodResumeSucceeded, 35)).append("║\n");
        sb.append("║   Singleton EOD:      ").append(flag(singletonEodEnforced, 35)).append("║\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        sb.append("║ Performance                                              ║\n");
        sb.append("║   Total Time:         ")
                .append(pad(totalExecutionTimeMs + "ms", 35))
                .append("║\n");
        sb.append("║   EOD Time:           ")
                .append(pad(eodExecutionTimeMs + "ms", 35))
                .append("║\n");
        sb.append("║   Deadlocks:          ").append(pad(deadlocksRecovered, 35)).append("║\n");
        sb.append("║   Within Threshold:   ")
                .append(flag(performanceWithinThreshold, 35))
                .append("║\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");
        sb.append("║                                                          ║\n");
        sb.append("║   FINAL GRADE:  ").append(pad(finalGrade, 41)).append("║\n");
        sb.append("║                                                          ║\n");
        sb.append("╚══════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }

    private String pad(Object val, int width) {
        String s = String.valueOf(val);
        return s + " ".repeat(Math.max(0, width - s.length()));
    }

    private String flag(boolean ok, int width) {
        return pad(ok ? "PASS ✅" : "FAIL ❌", width);
    }
}
