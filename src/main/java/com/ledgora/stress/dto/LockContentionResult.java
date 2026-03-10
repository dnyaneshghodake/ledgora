package com.ledgora.stress.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured result of a lock contention simulation run.
 *
 * <p>Captures concurrency metrics, lock wait events, and deadlock occurrences from parallel posting
 * + EOD execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LockContentionResult {

    private int threadCount;
    private int transactionsPerThread;
    private int totalTransactionsAttempted;
    private int totalTransactionsSucceeded;
    private int totalTransactionsFailed;

    // Timing
    private long totalDurationMs;
    private long avgTransactionTimeMs;
    private long maxTransactionTimeMs;
    private long minTransactionTimeMs;

    // Lock contention
    private int lockWaitOccurrences;
    private int deadlockCount;
    private int lockTimeoutCount;
    private int slowTransactionCount;

    // EOD
    private boolean eodAttempted;
    private boolean eodSucceeded;
    private long eodExecutionTimeMs;
    private String eodFailureReason;

    // Detailed events
    private List<String> contentionEvents;

    /** Log-friendly structured summary. */
    public String toSummary() {
        return "\n"
                + "╔══════════════════════════════════════════════════╗\n"
                + "║       LOCK CONTENTION SIMULATION SUMMARY        ║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Configuration                                   ║\n"
                + "║   Threads:          "
                + pad(threadCount)
                + "║\n"
                + "║   Txns/Thread:      "
                + pad(transactionsPerThread)
                + "║\n"
                + "║   Total Duration:   "
                + pad(totalDurationMs + "ms")
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Transactions                                    ║\n"
                + "║   Attempted:        "
                + pad(totalTransactionsAttempted)
                + "║\n"
                + "║   Succeeded:        "
                + pad(totalTransactionsSucceeded)
                + "║\n"
                + "║   Failed:           "
                + pad(totalTransactionsFailed)
                + "║\n"
                + "║   Avg Time (ms):    "
                + pad(avgTransactionTimeMs)
                + "║\n"
                + "║   Max Time (ms):    "
                + pad(maxTransactionTimeMs)
                + "║\n"
                + "║   Min Time (ms):    "
                + pad(minTransactionTimeMs)
                + "║\n"
                + "║   Slow (>2s):       "
                + pad(slowTransactionCount)
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Lock Contention                                 ║\n"
                + "║   Lock Waits:       "
                + pad(lockWaitOccurrences)
                + "║\n"
                + "║   Deadlocks:        "
                + pad(deadlockCount)
                + "║\n"
                + "║   Lock Timeouts:    "
                + pad(lockTimeoutCount)
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ EOD (Parallel)                                  ║\n"
                + "║   Attempted:        "
                + pad(eodAttempted)
                + "║\n"
                + "║   Succeeded:        "
                + pad(eodSucceeded)
                + "║\n"
                + "║   Time (ms):        "
                + pad(eodExecutionTimeMs)
                + "║\n"
                + (eodFailureReason != null
                        ? "║   Failure:          " + pad(eodFailureReason) + "║\n"
                        : "")
                + "╚══════════════════════════════════════════════════╝";
    }

    private String pad(Object val) {
        String s = String.valueOf(val);
        return s + " ".repeat(Math.max(0, 28 - s.length()));
    }
}
