package com.ledgora.diagnostics.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured result of a concurrency model validation audit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConcurrencyAuditResult {

    private boolean financialIntegrity;
    private boolean ledgerBalanced;
    private boolean clearingGlZero;
    private boolean suspenseGlZero;
    private boolean noNegativeBalances;
    private boolean noOrphanEntries;
    private boolean noStuckEodProcesses;
    private boolean noStalePendingVouchers;
    private boolean noDuplicateVoucherNumbers;
    private boolean noPartialIbtReversals;
    private boolean allIbtHaveFourVouchers;
    private boolean allBatchesBalanced;

    private long orphanEntryCount;
    private long negativeBalanceCount;
    private long stuckEodCount;
    private long stalePendingVoucherCount;
    private long duplicateVoucherNumberCount;
    private int totalChecks;
    private int passedChecks;

    private List<String> violations;

    /** Log-friendly structured summary. */
    public String toSummary() {
        return "\n"
                + "╔══════════════════════════════════════════════════╗\n"
                + "║     CONCURRENCY MODEL VALIDATION AUDIT           ║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Overall:           " + pad(financialIntegrity ? "PASS ✅" : "FAIL ❌") + "║\n"
                + "║ Checks:            " + pad(passedChecks + "/" + totalChecks) + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Ledger balanced:     " + flag(ledgerBalanced) + "║\n"
                + "║ Clearing GL zero:    " + flag(clearingGlZero) + "║\n"
                + "║ Suspense GL zero:    " + flag(suspenseGlZero) + "║\n"
                + "║ No negative bal:     " + flag(noNegativeBalances) + "║\n"
                + "║ No orphan entries:   " + flag(noOrphanEntries) + "║\n"
                + "║ No stuck EOD:        " + flag(noStuckEodProcesses) + "║\n"
                + "║ No stale vouchers:   " + flag(noStalePendingVouchers) + "║\n"
                + "║ No dup voucher#:     " + flag(noDuplicateVoucherNumbers) + "║\n"
                + "║ No partial IBT rev:  " + flag(noPartialIbtReversals) + "║\n"
                + "║ All IBT have 4 vch:  " + flag(allIbtHaveFourVouchers) + "║\n"
                + "║ All batches balanced: " + flag(allBatchesBalanced) + "║\n"
                + "╚══════════════════════════════════════════════════╝";
    }

    private String flag(boolean ok) {
        return pad(ok ? "PASS ✅" : "FAIL ❌");
    }

    private String pad(Object val) {
        String s = String.valueOf(val);
        return s + " ".repeat(Math.max(0, 27 - s.length()));
    }
}
