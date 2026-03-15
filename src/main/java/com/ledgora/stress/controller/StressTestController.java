package com.ledgora.stress.controller;

import com.ledgora.stress.dto.ChaosEodResult;
import com.ledgora.stress.dto.DeadlockSimulationResult;
import com.ledgora.stress.dto.EodPerformanceResult;
import com.ledgora.stress.dto.LoadGeneratorResult;
import com.ledgora.stress.dto.LockContentionResult;
import com.ledgora.stress.service.ChaosEodTestService;
import com.ledgora.stress.service.DeadlockSimulationService;
import com.ledgora.stress.service.EodLoadGeneratorService;
import com.ledgora.stress.service.EodPerformanceService;
import com.ledgora.stress.service.LockContentionService;
import com.ledgora.stress.service.ProductionLoadService;
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
    private final EodPerformanceService performanceRunner;
    private final LockContentionService lockContentionSimulator;
    private final DeadlockSimulationService deadlockSimulator;
    private final ProductionLoadService productionLoadGenerator;
    private final ChaosEodTestService chaosEodTester;

    public StressTestController(
            EodLoadGeneratorService loadGenerator,
            EodPerformanceService performanceRunner,
            LockContentionService lockContentionSimulator,
            DeadlockSimulationService deadlockSimulator,
            ProductionLoadService productionLoadGenerator,
            ChaosEodTestService chaosEodTester) {
        this.loadGenerator = loadGenerator;
        this.performanceRunner = performanceRunner;
        this.lockContentionSimulator = lockContentionSimulator;
        this.deadlockSimulator = deadlockSimulator;
        this.productionLoadGenerator = productionLoadGenerator;
        this.chaosEodTester = chaosEodTester;
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
                tenantId, accounts, transactions, ibtRatio);

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

    /**
     * Deadlock simulation: provoke cross-account lock ordering deadlock and verify recovery.
     *
     * <p>Usage: POST /stress/deadlock with JSON body:
     *
     * <pre>
     * {
     *   "tenantId": 1,
     *   "accountA": "SAV-1001-0001",
     *   "accountB": "SAV-1002-0001",
     *   "rounds": 3
     * }
     * </pre>
     */
    @PostMapping("/deadlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeadlockSimulationResult> runDeadlockTest(
            @RequestBody Map<String, Object> params) {

        Long tenantId = ((Number) params.getOrDefault("tenantId", 1)).longValue();
        String accountA = (String) params.getOrDefault("accountA", "SAV-1001-0001");
        String accountB = (String) params.getOrDefault("accountB", "SAV-1002-0001");
        int rounds = ((Number) params.getOrDefault("rounds", 3)).intValue();

        log.info(
                "Deadlock simulation initiated: tenant={} accountA={} accountB={} rounds={}",
                tenantId,
                accountA,
                accountB,
                rounds);

        DeadlockSimulationResult result =
                deadlockSimulator.simulate(tenantId, accountA, accountB, rounds);

        return ResponseEntity.ok(result);
    }

    /**
     * Production-style load test with rate limiting and workload mix.
     *
     * <p>Usage: POST /stress/load with JSON body:
     *
     * <pre>
     * {
     *   "tenantId": 1,
     *   "threads": 10,
     *   "targetTps": 50,
     *   "durationSeconds": 30,
     *   "ibtRatio": 15
     * }
     * </pre>
     */
    @PostMapping("/load")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoadGeneratorResult> runProductionLoad(
            @RequestBody Map<String, Object> params) {

        Long tenantId = ((Number) params.getOrDefault("tenantId", 1)).longValue();
        int threads = ((Number) params.getOrDefault("threads", 10)).intValue();
        int targetTps = ((Number) params.getOrDefault("targetTps", 50)).intValue();
        int duration = ((Number) params.getOrDefault("durationSeconds", 30)).intValue();
        int ibtRatio = ((Number) params.getOrDefault("ibtRatio", 15)).intValue();

        log.info(
                "Production load test: tenant={} threads={} tps={} duration={}s ibt={}%",
                tenantId, threads, targetTps, duration, ibtRatio);

        LoadGeneratorResult result =
                productionLoadGenerator.generate(tenantId, threads, targetTps, duration, ibtRatio);

        return ResponseEntity.ok(result);
    }

    /**
     * Chaos EOD test: simulate crash at a specific phase and verify resume + integrity.
     *
     * <p>Usage: POST /stress/chaos-eod with JSON body:
     *
     * <pre>
     * {
     *   "tenantId": 1,
     *   "crashAfterPhase": "DAY_CLOSING"
     * }
     * </pre>
     *
     * <p>Valid phases: VALIDATED, DAY_CLOSING, BATCH_CLOSED, SETTLED
     */
    @PostMapping("/chaos-eod")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChaosEodResult> runChaosEodTest(@RequestBody Map<String, Object> params) {

        Long tenantId = ((Number) params.getOrDefault("tenantId", 1)).longValue();
        String crashAfterPhase = (String) params.getOrDefault("crashAfterPhase", "DAY_CLOSING");

        log.info(
                "Chaos EOD test initiated: tenant={} crashAfterPhase={}",
                tenantId,
                crashAfterPhase);

        ChaosEodResult result = chaosEodTester.runChaosTest(tenantId, crashAfterPhase);

        return ResponseEntity.ok(result);
    }
}
