package com.ledgora.stress.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Structured result of a chaos EOD state machine test. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChaosEodResult {

    private String crashAfterPhase;
    private boolean crashSimulated;
    private boolean resumeAttempted;
    private boolean resumeSucceeded;
    private String resumeFailureReason;

    // Post-recovery validation
    private boolean ledgerBalanced;
    private boolean noDuplicateVouchers;
    private boolean noStuckRunning;
    private boolean clearingGlZero;
    private boolean suspenseGlZero;
    private boolean eodCompleted;

    private List<String> events;

    /** Log-friendly structured summary. */
    public String toSummary() {
        return "\n"
                + "╔══════════════════════════════════════════════════╗\n"
                + "║       CHAOS EOD STATE MACHINE SUMMARY            ║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Crash after phase: "
                + pad(crashAfterPhase)
                + "║\n"
                + "║ Crash simulated:   "
                + pad(crashSimulated)
                + "║\n"
                + "║ Resume attempted:  "
                + pad(resumeAttempted)
                + "║\n"
                + "║ Resume succeeded:  "
                + pad(resumeSucceeded)
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Post-Recovery Validation                        ║\n"
                + "║ Ledger balanced:     "
                + pad(ledgerBalanced ? "VERIFIED ✅" : "FAILED ❌")
                + "║\n"
                + "║ No dup vouchers:     "
                + pad(noDuplicateVouchers ? "VERIFIED ✅" : "FAILED ❌")
                + "║\n"
                + "║ No stuck RUNNING:    "
                + pad(noStuckRunning ? "VERIFIED ✅" : "FAILED ❌")
                + "║\n"
                + "║ Clearing GL zero:    "
                + pad(clearingGlZero ? "VERIFIED ✅" : "FAILED ❌")
                + "║\n"
                + "║ Suspense GL zero:    "
                + pad(suspenseGlZero ? "VERIFIED ✅" : "FAILED ❌")
                + "║\n"
                + "║ EOD completed:       "
                + pad(eodCompleted ? "YES ✅" : "NO ❌")
                + "║\n"
                + "╚══════════════════════════════════════════════════╝";
    }

    private String pad(Object val) {
        String s = String.valueOf(val);
        return s + " ".repeat(Math.max(0, 28 - s.length()));
    }
}
