package com.ledgora.stress.controller;

import com.ledgora.stress.dto.EodPerformanceResult;
import com.ledgora.stress.dto.LockContentionResult;
import com.ledgora.stress.service.EodLoadGeneratorService;
import com.ledgora.stress.service.EodPerformanceRunner;
import com.ledgora.stress.service.LockContentionSimulator;
import com.ledgora.tenant.context.TenantContextHolder;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EOD Performance Stress Test endpoint. Active ONLY in the "stress" Spring profile.
 *
 * <p>Generates bulk transaction load, executes EOD, and returns structured performance metrics
 * including Hibernate statistics. Restricted to ADMIN role.
 *
 * <p>Usage: POST /stress/eod with JSON body:
 *
 * <pre>
 * {
 *   "tenantId": 1,
 *   "accounts": 100,
 *   "transactions": 1000,
 *   "ibtRatio": 30
 * }
 * </pre>
 */
@RestController
@RequestMapping("/stress")
@Profile("stress")
public class StressTestController {

    private static final Logger log = LoggerFactory.getLogger(StressTestController.class);

    private final EodLoadGeneratorService loadGenerator;
    private final EodPerformanceRunner performanceRunner;
    private final LockContentionSimulator lockContentionSimulator;

    public StressTestController(
            EodLoadGeneratorService loadGenerator,
            EodPerformanceRunner performanceRunner,
            LockContentionSimulator lockContentionSimulator) {
        this.loadGenerator = loadGenerator;
        this.performanceRunner = performanceRunner;
        this.lockContentionSimulator = lockContentionSimulator;
    }

    @PostMapping("/eod")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EodPerformanceResult> runEodStressTest(
            @RequestBody Map<String, Object> params) {

        Long tenantId = ((Number) params.getOrDefault("tenantId", 1)).longValue();
        int accounts = ((Number) params.getOrDefault("accounts", 100)).intValue();
        int transactions = ((Number) params.getOrDefault("transactions", 1000)).intValue();
        int ibtRatio = ((Number) params.getOrDefault("ibtRatio", 30)).intValue();

        log.info(
                "Stress test initiated: tenant={} accounts={} txns={} ibtRatio={}%",
                tenantId,
                accounts,
                transactions,
                ibtRatio);

        // Phase 1: Generate load
        long loadStart = System.currentTimeMillis();
        TenantContextHolder.setTenantId(tenantId);

        int ibtCount = loadGenerator.generateLoad(tenantId, accounts, transactions, ibtRatio);

        long loadEnd = System.currentTimeMillis();
        long loadTimeMs = loadEnd - loadStart;

        // Phase 2: Run EOD with performance instrumentation
        EodPerformanceResult.EodPerformanceResultBuilder resultBuilder =
                EodPerformanceResult.builder()
                        .accountsGenerated(accounts)
                        .transactionsGenerated(transactions)
                        .ibtTransfersGenerated(ibtCount)
                        .loadGenerationTimeMs(loadTimeMs);

        EodPerformanceResult result =
                performanceRunner.runEodPerformanceTest(tenantId, resultBuilder);

        TenantContextHolder.clear();

        return ResponseEntity.ok(result);
    }

    /**
     * Lock contention simulation: concurrent posting threads + optional parallel EOD.
     *
     * <p>Usage: POST /stress/lock-contention with JSON body:
     *
     * <pre>
     * {
     *   "tenantId": 1,
     *   "threads": 4,
     *   "transactionsPerThread": 50,
     *   "triggerEod": true
     * }
     * </pre>
     */
    @PostMapping("/lock-contention")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LockContentionResult> runLockContentionTest(
            @RequestBody Map<String, Object> params) {

        Long tenantId = ((Number) params.getOrDefault("tenantId", 1)).longValue();
        int threads = ((Number) params.getOrDefault("threads", 4)).intValue();
        int txnsPerThread = ((Number) params.getOrDefault("transactionsPerThread", 50)).intValue();
        boolean triggerEod = Boolean.TRUE.equals(params.getOrDefault("triggerEod", true));

        log.info(
                "Lock contention test initiated: tenant={} threads={} txns/thread={} eod={}",
                tenantId,
                threads,
                txnsPerThread,
                triggerEod);

        LockContentionResult result =
                lockContentionSimulator.simulate(tenantId, threads, txnsPerThread, triggerEod);

        return ResponseEntity.ok(result);
    }
}
