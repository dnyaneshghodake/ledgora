package com.ledgora.stress.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.eod.service.EodValidationService;
import com.ledgora.stress.dto.LockContentionResult;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Lock contention simulation harness for CBS concurrency testing.
 *
 * <p>Active only in the "stress" profile. Simulates multiple threads posting transactions
 * concurrently while a parallel thread triggers EOD, to detect:
 *
 * <ul>
 *   <li>Deadlocks (PESSIMISTIC_WRITE on scroll_sequences + account locks)
 *   <li>Lock timeouts (H2 default 10s, production varies)
 *   <li>Slow transactions (>2s wall time — indicates lock wait)
 * </ul>
 *
 * <p>Uses existing TransactionService — all governance controls fire. No production logic modified.
 */
@Service
@Profile("stress")
public class LockContentionService {

    private static final Logger log = LoggerFactory.getLogger(LockContentionService.class);
    private static final long SLOW_THRESHOLD_MS = 2000;

    private final TransactionService transactionService;
    private final EodValidationService eodValidationService;
    private final AccountRepository accountRepository;

    public LockContentionService(
            TransactionService transactionService,
            EodValidationService eodValidationService,
            AccountRepository accountRepository) {
        this.transactionService = transactionService;
        this.eodValidationService = eodValidationService;
        this.accountRepository = accountRepository;
    }

    /**
     * Run lock contention simulation.
     *
     * @param tenantId tenant to test
     * @param threads number of concurrent posting threads
     * @param transactionsPerThread transactions each thread attempts
     * @param triggerEod whether to trigger EOD in parallel
     * @return structured contention result
     */
    public LockContentionResult simulate(
            Long tenantId, int threads, int transactionsPerThread, boolean triggerEod) {

        List<Account> accounts =
                accountRepository.findByTenantId(tenantId).stream()
                        .filter(
                                a ->
                                        a.getBalance() != null
                                                && a.getBalance().compareTo(BigDecimal.ZERO) > 0)
                        .toList();

        if (accounts.size() < 2) {
            return LockContentionResult.builder()
                    .threadCount(threads)
                    .transactionsPerThread(transactionsPerThread)
                    .totalTransactionsFailed(0)
                    .contentionEvents(List.of("ABORTED: Need at least 2 funded accounts"))
                    .build();
        }

        // Shared counters (thread-safe)
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger lockWaits = new AtomicInteger(0);
        AtomicInteger deadlocks = new AtomicInteger(0);
        AtomicInteger lockTimeouts = new AtomicInteger(0);
        AtomicInteger slowTxns = new AtomicInteger(0);
        AtomicLong maxTime = new AtomicLong(0);
        AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong totalTime = new AtomicLong(0);
        CopyOnWriteArrayList<String> events = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threads + 1);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        long simulationStart = System.currentTimeMillis();

        // Submit posting threads
        for (int t = 0; t < threads; t++) {
            final int threadNum = t;
            executor.submit(
                    () -> {
                        try {
                            // Each thread needs its own security + tenant context
                            SecurityContextHolder.getContext()
                                    .setAuthentication(
                                            new UsernamePasswordAuthenticationToken(
                                                    "stress-thread-" + threadNum,
                                                    "N/A",
                                                    List.of(
                                                            new SimpleGrantedAuthority(
                                                                    "ROLE_TELLER"))));
                            TenantContextHolder.setTenantId(tenantId);

                            startGate.await(); // wait for all threads to be ready
                            Random rng = new Random(threadNum * 31L);

                            for (int i = 0; i < transactionsPerThread; i++) {
                                long txStart = System.currentTimeMillis();
                                try {
                                    Account src = accounts.get(rng.nextInt(accounts.size()));
                                    Account dst = accounts.get(rng.nextInt(accounts.size()));
                                    while (dst.getAccountNumber().equals(src.getAccountNumber())) {
                                        dst = accounts.get(rng.nextInt(accounts.size()));
                                    }
                                    BigDecimal amount =
                                            new BigDecimal(rng.nextInt(90) + 10)
                                                    .setScale(2, RoundingMode.HALF_UP);

                                    transactionService.transfer(
                                            TransactionDTO.builder()
                                                    .transactionType("TRANSFER")
                                                    .sourceAccountNumber(src.getAccountNumber())
                                                    .destinationAccountNumber(
                                                            dst.getAccountNumber())
                                                    .amount(amount)
                                                    .currency("INR")
                                                    .channel(TransactionChannel.BATCH.name())
                                                    .clientReferenceId(
                                                            "LOCK-T"
                                                                    + threadNum
                                                                    + "-"
                                                                    + i
                                                                    + "-"
                                                                    + System.nanoTime())
                                                    .description("Lock contention test")
                                                    .narration("Thread " + threadNum + " txn " + i)
                                                    .build());
                                    succeeded.incrementAndGet();
                                } catch (Exception e) {
                                    failed.incrementAndGet();
                                    classifyException(
                                            e,
                                            lockWaits,
                                            deadlocks,
                                            lockTimeouts,
                                            events,
                                            threadNum,
                                            i);
                                }

                                long txTime = System.currentTimeMillis() - txStart;
                                totalTime.addAndGet(txTime);
                                maxTime.updateAndGet(cur -> Math.max(cur, txTime));
                                minTime.updateAndGet(cur -> Math.min(cur, txTime));
                                if (txTime > SLOW_THRESHOLD_MS) {
                                    slowTxns.incrementAndGet();
                                    events.add(
                                            "SLOW_TXN: thread="
                                                    + threadNum
                                                    + " txn="
                                                    + i
                                                    + " time="
                                                    + txTime
                                                    + "ms");
                                }

                                // Random sleep to simulate realistic spacing
                                Thread.sleep(rng.nextInt(46) + 5);
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

        // EOD thread (optional — fires midway through posting)
        boolean[] eodResult = {false, false}; // [attempted, succeeded]
        long[] eodTime = {0};
        String[] eodFailure = {null};

        Future<?> eodFuture = null;
        if (triggerEod) {
            eodFuture =
                    executor.submit(
                            () -> {
                                try {
                                    SecurityContextHolder.getContext()
                                            .setAuthentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            "stress-eod",
                                                            "N/A",
                                                            List.of(
                                                                    new SimpleGrantedAuthority(
                                                                            "ROLE_ADMIN"))));
                                    TenantContextHolder.setTenantId(tenantId);

                                    // Wait for posting threads to start, then delay slightly
                                    startGate.await();
                                    Thread.sleep(500);

                                    eodResult[0] = true;
                                    long eodStart = System.currentTimeMillis();
                                    eodValidationService.runEod(tenantId);
                                    eodResult[1] = true;
                                    eodTime[0] = System.currentTimeMillis() - eodStart;
                                } catch (Exception e) {
                                    eodTime[0] = System.currentTimeMillis() - eodStart;
                                    eodFailure[0] =
                                            e.getClass().getSimpleName() + ": " + e.getMessage();
                                    events.add("EOD_FAILED: " + eodFailure[0]);
                                } finally {
                                    TenantContextHolder.clear();
                                    SecurityContextHolder.clearContext();
                                }
                            });
        }

        // Release all threads simultaneously
        startGate.countDown();

        // Wait for completion
        try {
            doneLatch.await(120, TimeUnit.SECONDS);
            if (eodFuture != null) {
                eodFuture.get(60, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            events.add("SIMULATION_TIMEOUT: " + e.getMessage());
        }

        executor.shutdownNow();
        long simulationEnd = System.currentTimeMillis();

        int totalAttempted = threads * transactionsPerThread;
        int totalSucceeded = succeeded.get();
        long avgTime = totalSucceeded > 0 ? totalTime.get() / totalSucceeded : 0;
        long actualMin = minTime.get() == Long.MAX_VALUE ? 0 : minTime.get();

        LockContentionResult result =
                LockContentionResult.builder()
                        .threadCount(threads)
                        .transactionsPerThread(transactionsPerThread)
                        .totalTransactionsAttempted(totalAttempted)
                        .totalTransactionsSucceeded(totalSucceeded)
                        .totalTransactionsFailed(failed.get())
                        .totalDurationMs(simulationEnd - simulationStart)
                        .avgTransactionTimeMs(avgTime)
                        .maxTransactionTimeMs(maxTime.get())
                        .minTransactionTimeMs(actualMin)
                        .lockWaitOccurrences(lockWaits.get())
                        .deadlockCount(deadlocks.get())
                        .lockTimeoutCount(lockTimeouts.get())
                        .slowTransactionCount(slowTxns.get())
                        .eodAttempted(eodResult[0])
                        .eodSucceeded(eodResult[1])
                        .eodExecutionTimeMs(eodTime[0])
                        .eodFailureReason(eodFailure[0])
                        .contentionEvents(new ArrayList<>(events))
                        .build();

        log.info(result.toSummary());
        return result;
    }

    private void classifyException(
            Exception e,
            AtomicInteger lockWaits,
            AtomicInteger deadlocks,
            AtomicInteger lockTimeouts,
            CopyOnWriteArrayList<String> events,
            int threadNum,
            int txnNum) {

        String msg = e.getMessage() != null ? e.getMessage().toUpperCase() : "";
        String cause =
                e.getCause() != null && e.getCause().getMessage() != null
                        ? e.getCause().getMessage().toUpperCase()
                        : "";
        String combined = msg + " " + cause;

        if (combined.contains("DEADLOCK")) {
            deadlocks.incrementAndGet();
            lockWaits.incrementAndGet();
            events.add("DEADLOCK: thread=" + threadNum + " txn=" + txnNum);
            log.warn("LOCK_CONTENTION_DETECTED: Deadlock thread={} txn={}", threadNum, txnNum);
        } else if (combined.contains("LOCK") && combined.contains("TIMEOUT")) {
            lockTimeouts.incrementAndGet();
            lockWaits.incrementAndGet();
            events.add("LOCK_TIMEOUT: thread=" + threadNum + " txn=" + txnNum);
            log.warn("LOCK_CONTENTION_DETECTED: Lock timeout thread={} txn={}", threadNum, txnNum);
        } else if (combined.contains("LOCK")
                || combined.contains("PESSIMISTIC")
                || combined.contains("COULD NOT ACQUIRE")) {
            lockWaits.incrementAndGet();
            events.add(
                    "LOCK_WAIT: thread="
                            + threadNum
                            + " txn="
                            + txnNum
                            + " — "
                            + e.getClass().getSimpleName());
            log.warn("LOCK_CONTENTION_DETECTED: Lock wait thread={} txn={}", threadNum, txnNum);
        }
        // Non-lock failures (insufficient balance, frozen, etc.) are expected and not logged as
        // contention
    }
}
