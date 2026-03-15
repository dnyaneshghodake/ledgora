package com.ledgora.stress.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.stress.dto.LoadGeneratorResult;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Production-style multi-thread load generator with rate limiting and workload mix.
 *
 * <p>Active only in the "stress" profile. Simulates realistic CBS workload:
 *
 * <ul>
 *   <li>40% deposits
 *   <li>30% withdrawals
 *   <li>25% same-branch transfers
 *   <li>5% cross-branch IBT transfers
 * </ul>
 *
 * <p>Rate control via token-bucket semaphore: releases {@code targetTps} permits per second via a
 * background refill thread. Worker threads acquire a permit before each transaction.
 *
 * <p>Captures per-transaction latencies for percentile calculation (P50, P95, P99).
 */
@Service
@Profile("stress")
public class ProductionLoadService {

    private static final Logger log = LoggerFactory.getLogger(ProductionLoadService.class);

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final BranchRepository branchRepository;

    public ProductionLoadService(
            TransactionService transactionService,
            AccountRepository accountRepository,
            BranchRepository branchRepository) {
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
        this.branchRepository = branchRepository;
    }

    /**
     * Run production-style load test.
     *
     * @param tenantId tenant to test
     * @param threads worker thread count
     * @param targetTps target transactions per second
     * @param durationSeconds how long to run
     * @param ibtRatioPercent percentage of transfers that are cross-branch (within the 30% transfer
     *     + IBT bucket)
     * @return structured result with TPS, latency percentiles, error breakdown
     */
    public LoadGeneratorResult generate(
            Long tenantId, int threads, int targetTps, int durationSeconds, int ibtRatioPercent) {

        List<Account> accounts =
                accountRepository.findByTenantId(tenantId).stream()
                        .filter(
                                a ->
                                        a.getBalance() != null
                                                && a.getBalance().compareTo(new BigDecimal("100"))
                                                        > 0)
                        .toList();

        if (accounts.size() < 2) {
            return LoadGeneratorResult.builder()
                    .threadCount(threads)
                    .targetTps(targetTps)
                    .totalFailed(0)
                    .build();
        }

        List<Branch> branches = branchRepository.findAll();

        // Shared metrics (thread-safe)
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger deposits = new AtomicInteger(0);
        AtomicInteger withdrawals = new AtomicInteger(0);
        AtomicInteger transfers = new AtomicInteger(0);
        AtomicInteger ibts = new AtomicInteger(0);
        AtomicInteger balanceErrors = new AtomicInteger(0);
        AtomicInteger velocityErrors = new AtomicInteger(0);
        AtomicInteger ceilingErrors = new AtomicInteger(0);
        AtomicInteger lockErrors = new AtomicInteger(0);
        AtomicInteger otherErrors = new AtomicInteger(0);
        CopyOnWriteArrayList<Long> latencies = new CopyOnWriteArrayList<>();

        // Token bucket rate limiter
        Semaphore rateLimiter = new Semaphore(0);
        long durationMs = durationSeconds * 1000L;

        ExecutorService executor = Executors.newFixedThreadPool(threads + 1);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        long startTime = System.currentTimeMillis();

        // Rate limiter refill thread — releases targetTps permits every second
        executor.submit(
                () -> {
                    try {
                        startGate.await();
                        long elapsed = 0;
                        while (elapsed < durationMs) {
                            rateLimiter.release(targetTps);
                            Thread.sleep(1000);
                            elapsed = System.currentTimeMillis() - startTime;
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    // Release extra permits to unblock any waiting threads at shutdown
                    rateLimiter.release(threads * 10);
                });

        // Worker threads
        for (int t = 0; t < threads; t++) {
            final int threadNum = t;
            executor.submit(
                    () -> {
                        SecurityContextHolder.getContext()
                                .setAuthentication(
                                        new UsernamePasswordAuthenticationToken(
                                                "load-" + threadNum,
                                                "N/A",
                                                List.of(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_TELLER"))));
                        TenantContextHolder.setTenantId(tenantId);

                        try {
                            startGate.await();
                            Random rng = new Random(threadNum * 37L + System.nanoTime());

                            while (System.currentTimeMillis() - startTime < durationMs) {
                                // Acquire rate limiter permit
                                if (!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)) {
                                    break; // duration likely expired
                                }

                                long txStart = System.currentTimeMillis();
                                try {
                                    int roll = rng.nextInt(100);
                                    Account target = accounts.get(rng.nextInt(accounts.size()));
                                    BigDecimal amount =
                                            new BigDecimal(rng.nextInt(900) + 100)
                                                    .setScale(2, RoundingMode.HALF_UP);
                                    String ref = "LOAD-" + threadNum + "-" + System.nanoTime();

                                    if (roll < 40) {
                                        // 40% deposits
                                        transactionService.deposit(
                                                TransactionDTO.builder()
                                                        .transactionType("DEPOSIT")
                                                        .destinationAccountNumber(
                                                                target.getAccountNumber())
                                                        .amount(amount)
                                                        .currency("INR")
                                                        .channel(TransactionChannel.TELLER.name())
                                                        .clientReferenceId(ref)
                                                        .description("Load deposit")
                                                        .narration("Load test deposit")
                                                        .build());
                                        deposits.incrementAndGet();
                                    } else if (roll < 70) {
                                        // 30% withdrawals
                                        transactionService.withdraw(
                                                TransactionDTO.builder()
                                                        .transactionType("WITHDRAWAL")
                                                        .sourceAccountNumber(
                                                                target.getAccountNumber())
                                                        .amount(
                                                                new BigDecimal(rng.nextInt(90) + 10)
                                                                        .setScale(
                                                                                2,
                                                                                RoundingMode
                                                                                        .HALF_UP))
                                                        .currency("INR")
                                                        .channel(TransactionChannel.TELLER.name())
                                                        .clientReferenceId(ref)
                                                        .description("Load withdrawal")
                                                        .narration("Load test withdrawal")
                                                        .build());
                                        withdrawals.incrementAndGet();
                                    } else {
                                        // 30% transfers (25% same-branch + 5% IBT)
                                        Account dst = accounts.get(rng.nextInt(accounts.size()));
                                        while (dst.getAccountNumber()
                                                .equals(target.getAccountNumber())) {
                                            dst = accounts.get(rng.nextInt(accounts.size()));
                                        }

                                        boolean doIbt =
                                                rng.nextInt(100) < ibtRatioPercent
                                                        && branches.size() >= 2;
                                        if (doIbt) {
                                            // Pick from different branch
                                            Account ibtDst =
                                                    findAccountOnDifferentBranch(
                                                            accounts, target, branches);
                                            if (ibtDst != null) {
                                                dst = ibtDst;
                                                ibts.incrementAndGet();
                                            }
                                        }

                                        transactionService.transfer(
                                                TransactionDTO.builder()
                                                        .transactionType("TRANSFER")
                                                        .sourceAccountNumber(
                                                                target.getAccountNumber())
                                                        .destinationAccountNumber(
                                                                dst.getAccountNumber())
                                                        .amount(
                                                                new BigDecimal(rng.nextInt(90) + 10)
                                                                        .setScale(
                                                                                2,
                                                                                RoundingMode
                                                                                        .HALF_UP))
                                                        .currency("INR")
                                                        .channel(TransactionChannel.BATCH.name())
                                                        .clientReferenceId(ref)
                                                        .description("Load transfer")
                                                        .narration("Load test transfer")
                                                        .build());
                                        transfers.incrementAndGet();
                                    }
                                    succeeded.incrementAndGet();
                                } catch (Exception e) {
                                    failed.incrementAndGet();
                                    classifyError(
                                            e,
                                            balanceErrors,
                                            velocityErrors,
                                            ceilingErrors,
                                            lockErrors,
                                            otherErrors);
                                }

                                long txTime = System.currentTimeMillis() - txStart;
                                latencies.add(txTime);
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        } finally {
                            TenantContextHolder.clear();
                            SecurityContextHolder.clearContext();
                            doneLatch.countDown();
                        }
                    });
        }

        // Go!
        startGate.countDown();

        try {
            doneLatch.await(durationSeconds + 30, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        executor.shutdownNow();

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        // Compute percentiles
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);

        int total = succeeded.get() + failed.get();
        double actualTps = total > 0 ? (total * 1000.0) / totalDuration : 0;
        long avg =
                sorted.isEmpty()
                        ? 0
                        : sorted.stream().mapToLong(Long::longValue).sum() / sorted.size();

        LoadGeneratorResult result =
                LoadGeneratorResult.builder()
                        .threadCount(threads)
                        .targetTps(targetTps)
                        .durationSeconds(durationSeconds)
                        .totalAttempted(total)
                        .totalSucceeded(succeeded.get())
                        .totalFailed(failed.get())
                        .depositCount(deposits.get())
                        .withdrawalCount(withdrawals.get())
                        .transferCount(transfers.get())
                        .ibtCount(ibts.get())
                        .actualTps(actualTps)
                        .totalDurationMs(totalDuration)
                        .avgLatencyMs(avg)
                        .p50LatencyMs(percentile(sorted, 50))
                        .p95LatencyMs(percentile(sorted, 95))
                        .p99LatencyMs(percentile(sorted, 99))
                        .maxLatencyMs(sorted.isEmpty() ? 0 : sorted.get(sorted.size() - 1))
                        .minLatencyMs(sorted.isEmpty() ? 0 : sorted.get(0))
                        .errorRate(total > 0 ? (double) failed.get() / total : 0)
                        .insufficientBalanceErrors(balanceErrors.get())
                        .velocityBreachErrors(velocityErrors.get())
                        .hardCeilingErrors(ceilingErrors.get())
                        .lockContentionErrors(lockErrors.get())
                        .otherErrors(otherErrors.get())
                        .build();

        log.info(result.toSummary());
        return result;
    }

    private long percentile(List<Long> sorted, int pct) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(pct / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }

    private Account findAccountOnDifferentBranch(
            List<Account> accounts, Account source, List<Branch> branches) {
        Long sourceBranchId = source.getBranch() != null ? source.getBranch().getId() : null;
        return accounts.stream()
                .filter(a -> a.getBranch() != null && !a.getBranch().getId().equals(sourceBranchId))
                .findFirst()
                .orElse(null);
    }

    private void classifyError(
            Exception e,
            AtomicInteger balance,
            AtomicInteger velocity,
            AtomicInteger ceiling,
            AtomicInteger lock,
            AtomicInteger other) {
        String msg = (e.getMessage() != null ? e.getMessage() : "").toUpperCase();
        String cause =
                e.getCause() != null && e.getCause().getMessage() != null
                        ? e.getCause().getMessage().toUpperCase()
                        : "";
        String combined = msg + " " + cause;

        if (combined.contains("INSUFFICIENT") || combined.contains("BALANCE")) {
            balance.incrementAndGet();
        } else if (combined.contains("VELOCITY") || combined.contains("VELOCITY_LIMIT")) {
            velocity.incrementAndGet();
        } else if (combined.contains("HARD_LIMIT") || combined.contains("CEILING")) {
            ceiling.incrementAndGet();
        } else if (combined.contains("LOCK")
                || combined.contains("DEADLOCK")
                || combined.contains("PESSIMISTIC")) {
            lock.incrementAndGet();
        } else {
            other.incrementAndGet();
        }
    }
}
