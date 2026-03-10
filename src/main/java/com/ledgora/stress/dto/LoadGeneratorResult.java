package com.ledgora.stress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Structured result of a production-style multi-thread load generation run. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadGeneratorResult {

    // Configuration
    private int threadCount;
    private int targetTps;
    private int durationSeconds;

    // Volume
    private int totalAttempted;
    private int totalSucceeded;
    private int totalFailed;
    private int depositCount;
    private int withdrawalCount;
    private int transferCount;
    private int ibtCount;

    // Throughput
    private double actualTps;
    private long totalDurationMs;

    // Latency (milliseconds)
    private long avgLatencyMs;
    private long p50LatencyMs;
    private long p95LatencyMs;
    private long p99LatencyMs;
    private long maxLatencyMs;
    private long minLatencyMs;

    // Error analysis
    private double errorRate;
    private int insufficientBalanceErrors;
    private int velocityBreachErrors;
    private int hardCeilingErrors;
    private int lockContentionErrors;
    private int otherErrors;

    /** Log-friendly structured summary. */
    public String toSummary() {
        return "\n"
                + "╔══════════════════════════════════════════════════╗\n"
                + "║     PRODUCTION LOAD GENERATOR SUMMARY            ║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Config: "
                + threadCount
                + " threads, target "
                + targetTps
                + " TPS"
                + " ".repeat(Math.max(0, 22 - String.valueOf(targetTps).length()))
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Volume                                          ║\n"
                + "║   Total:       "
                + pad(totalAttempted)
                + "║\n"
                + "║   Succeeded:   "
                + pad(totalSucceeded)
                + "║\n"
                + "║   Failed:      "
                + pad(totalFailed)
                + "║\n"
                + "║   Deposits:    "
                + pad(depositCount)
                + "║\n"
                + "║   Withdrawals: "
                + pad(withdrawalCount)
                + "║\n"
                + "║   Transfers:   "
                + pad(transferCount)
                + "║\n"
                + "║   IBTs:        "
                + pad(ibtCount)
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Throughput                                      ║\n"
                + "║   Actual TPS:  "
                + pad(String.format("%.1f", actualTps))
                + "║\n"
                + "║   Duration:    "
                + pad(totalDurationMs + "ms")
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Latency (ms)                                    ║\n"
                + "║   Avg:         "
                + pad(avgLatencyMs)
                + "║\n"
                + "║   P50:         "
                + pad(p50LatencyMs)
                + "║\n"
                + "║   P95:         "
                + pad(p95LatencyMs)
                + "║\n"
                + "║   P99:         "
                + pad(p99LatencyMs)
                + "║\n"
                + "║   Max:         "
                + pad(maxLatencyMs)
                + "║\n"
                + "╠══════════════════════════════════════════════════╣\n"
                + "║ Errors                                          ║\n"
                + "║   Rate:        "
                + pad(String.format("%.2f%%", errorRate * 100))
                + "║\n"
                + "║   Balance:     "
                + pad(insufficientBalanceErrors)
                + "║\n"
                + "║   Velocity:    "
                + pad(velocityBreachErrors)
                + "║\n"
                + "║   Ceiling:     "
                + pad(hardCeilingErrors)
                + "║\n"
                + "║   Lock:        "
                + pad(lockContentionErrors)
                + "║\n"
                + "║   Other:       "
                + pad(otherErrors)
                + "║\n"
                + "╚══════════════════════════════════════════════════╝";
    }

    private String pad(Object val) {
        String s = String.valueOf(val);
        return s + " ".repeat(Math.max(0, 33 - s.length()));
    }
}
