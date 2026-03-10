package com.ledgora.stress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured result of a deadlock simulation and recovery verification run.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadlockSimulationResult {

    private boolean deadlockTriggered;
    private int deadlockCount;
    private int totalAttempts;
    private int successfulTransactions;
    private int failedTransactions;
    private String threadAOutcome;
    private String threadBOutcome;

    // Recovery verification
    private boolean ledgerBalanced;
    private boolean noPartialVouchers;
    private boolean batchTotalsIntact;
    private boolean systemRecovered;

    private String verificationDetails;

    /** Log-friendly structured summary. */
    public String toSummary() {
        return "\n"
                + "╔══════════════════════════════════════════════════╗\n"
                + "║        DEADLOCK SIMULATION SUMMARY               ║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Deadlock triggered:  " + pad(deadlockTriggered ? "YES" : "NO") + "║\n"
                + "║ Deadlock count:      " + pad(deadlockCount) + "║\n"
                + "║ Total attempts:      " + pad(totalAttempts) + "║\n"
                + "║ Succeeded:           " + pad(successfulTransactions) + "║\n"
                + "║ Failed:              " + pad(failedTransactions) + "║\n"
                + "║ Thread A:            " + pad(threadAOutcome) + "║\n"
                + "║ Thread B:            " + pad(threadBOutcome) + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Recovery Verification                           ║\n"
                + "║ Ledger balanced:     " + pad(ledgerBalanced ? "VERIFIED ✅" : "FAILED ❌") + "║\n"
                + "║ No partial vouchers: " + pad(noPartialVouchers ? "VERIFIED ✅" : "FAILED ❌") + "║\n"
                + "║ Batch totals intact: " + pad(batchTotalsIntact ? "VERIFIED ✅" : "FAILED ❌") + "║\n"
                + "║ System recovered:    " + pad(systemRecovered ? "YES ✅" : "NO ❌") + "║\n"
                + "╚══════════════════════════════════════════════════╝";
    }

    private String pad(Object val) {
        String s = String.valueOf(val);
        return s + " ".repeat(Math.max(0, 27 - s.length()));
    }
}
