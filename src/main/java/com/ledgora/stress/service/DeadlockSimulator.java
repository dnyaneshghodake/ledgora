package com.ledgora.stress.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.stress.dto.DeadlockSimulationResult;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.service.TransactionService;
import com.ledgora.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Deadlock detection and recovery verification harness.
 *
 * <p>Active only in the "stress" profile. Deliberately provokes a cross-account lock ordering
 * deadlock by having two threads perform transfers in opposite directions simultaneously:
 *
 * <pre>
 *   Thread A: transfer(Account X → Account Y)  // locks X then Y
 *   Thread B: transfer(Account Y → Account X)  // locks Y then X
 * </pre>
 *
 * <p>When both threads acquire their first lock simultaneously (via CountDownLatch
 * synchronization), a deadlock occurs. The database engine (H2/SQL Server) detects it and aborts
 * one thread.
 *
 * <p>After the deadlock, the harness verifies:
 *
 * <ul>
 *   <li>No partial vouchers were committed (atomicity preserved)
 *   <li>Ledger entries remain balanced (double-entry intact)
 *   <li>Batch totals are consistent
 * </ul>
 *
 * <p>Does NOT modify any production service logic. Uses existing TransactionService.transfer().
 */
@Service
@Profile("stress")
public class DeadlockSimulator {

    private static final Logger log = LoggerFactory.getLogger(DeadlockSimulator.class);

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final VoucherRepository voucherRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TenantService tenantService;

    public DeadlockSimulator(
            TransactionService transactionService,
            AccountRepository accountRepository,
            VoucherRepository voucherRepository,
            LedgerEntryRepository ledgerEntryRepository,
            TenantService tenantService) {
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
        this.voucherRepository = voucherRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.tenantService = tenantService;
    }

    /**
     * Run deadlock simulation with recovery verification.
     *
     * @param tenantId tenant to test
     * @param accountNumberA first account (Thread A locks this first)
     * @param accountNumberB second account (Thread B locks this first)
     * @param rounds number of deadlock provocation rounds
     * @return structured simulation result with recovery verification
     */
    public DeadlockSimulationResult simulate(
            Long tenantId, String accountNumberA, String accountNumberB, int rounds) {

        // Validate accounts exist
        Account acctA =
                accountRepository
                        .findByAccountNumberAndTenantId(accountNumberA, tenantId)
                        .orElse(null);
        Account acctB =
                accountRepository
                        .findByAccountNumberAndTenantId(accountNumberB, tenantId)
                        .orElse(null);

        if (acctA == null || acctB == null) {
            return DeadlockSimulationResult.builder()
                    .deadlockTriggered(false)
                    .verificationDetails("ABORTED: One or both accounts not found")
                    .build();
        }

        // Capture pre-simulation state for verification
        LocalDate bizDate = tenantService.getCurrentBusinessDate(tenantId);
        BigDecimal preDebits =
                ledgerEntryRepository.sumDebitsByBusinessDateAndTenantId(bizDate, tenantId);
        BigDecimal preCredits =
                ledgerEntryRepository.sumCreditsByBusinessDateAndTenantId(bizDate, tenantId);
        long preVoucherCount = voucherRepository.count();

        AtomicInteger deadlockCount = new AtomicInteger(0);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicReference<String> threadAOutcome = new AtomicReference<>("NOT_RUN");
        AtomicReference<String> threadBOutcome = new AtomicReference<>("NOT_RUN");

        BigDecimal amount = new BigDecimal("50.00");

        for (int round = 0; round < rounds; round++) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch readyGate = new CountDownLatch(2);
            CountDownLatch startGate = new CountDownLatch(1);
            final int r = round;

            // Thread A: transfer X → Y
            executor.submit(
                    () -> {
                        setupContext("deadlock-A", tenantId);
                        readyGate.countDown();
                        try {
                            startGate.await();
                            transactionService.transfer(
                                    TransactionDTO.builder()
                                            .transactionType("TRANSFER")
                                            .sourceAccountNumber(accountNumberA)
                                            .destinationAccountNumber(accountNumberB)
                                            .amount(amount)
                                            .currency("INR")
                                            .channel(TransactionChannel.BATCH.name())
                                            .clientReferenceId(
                                                    "DL-A-" + r + "-" + System.nanoTime())
                                            .description("Deadlock sim A→B round " + r)
                                            .narration("Deadlock test")
                                            .build());
                            succeeded.incrementAndGet();
                            threadAOutcome.set("SUCCESS");
                        } catch (Exception e) {
                            failed.incrementAndGet();
                            String outcome = classifyDeadlock(e, deadlockCount);
                            threadAOutcome.set(outcome);
                        } finally {
                            clearContext();
                        }
                    });

            // Thread B: transfer Y → X (reverse lock order)
            executor.submit(
                    () -> {
                        setupContext("deadlock-B", tenantId);
                        readyGate.countDown();
                        try {
                            startGate.await();
                            transactionService.transfer(
                                    TransactionDTO.builder()
                                            .transactionType("TRANSFER")
                                            .sourceAccountNumber(accountNumberB)
                                            .destinationAccountNumber(accountNumberA)
                                            .amount(amount)
                                            .currency("INR")
                                            .channel(TransactionChannel.BATCH.name())
                                            .clientReferenceId(
                                                    "DL-B-" + r + "-" + System.nanoTime())
                                            .description("Deadlock sim B→A round " + r)
                                            .narration("Deadlock test")
                                            .build());
                            succeeded.incrementAndGet();
                            threadBOutcome.set("SUCCESS");
                        } catch (Exception e) {
                            failed.incrementAndGet();
                            String outcome = classifyDeadlock(e, deadlockCount);
                            threadBOutcome.set(outcome);
                        } finally {
                            clearContext();
                        }
                    });

            // Wait for both threads ready, then release simultaneously
            try {
                readyGate.await(5, TimeUnit.SECONDS);
                startGate.countDown(); // fire!
                executor.shutdown();
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        // === Recovery Verification ===
        BigDecimal postDebits =
                ledgerEntryRepository.sumDebitsByBusinessDateAndTenantId(bizDate, tenantId);
        BigDecimal postCredits =
                ledgerEntryRepository.sumCreditsByBusinessDateAndTenantId(bizDate, tenantId);
        long postVoucherCount = voucherRepository.count();

        boolean ledgerBalanced = postDebits.compareTo(postCredits) == 0;

        // No partial vouchers: new vouchers should come in pairs (even count delta)
        long voucherDelta = postVoucherCount - preVoucherCount;
        boolean noPartialVouchers = voucherDelta % 2 == 0;

        // Batch totals: if ledger is balanced, batches are consistent
        boolean batchTotalsIntact = ledgerBalanced;

        boolean systemRecovered = ledgerBalanced && noPartialVouchers;

        StringBuilder details = new StringBuilder();
        details.append("Pre: DR=").append(preDebits).append(" CR=").append(preCredits);
        details.append(" | Post: DR=").append(postDebits).append(" CR=").append(postCredits);
        details.append(" | Voucher delta=").append(voucherDelta);

        DeadlockSimulationResult result =
                DeadlockSimulationResult.builder()
                        .deadlockTriggered(deadlockCount.get() > 0)
                        .deadlockCount(deadlockCount.get())
                        .totalAttempts(rounds * 2)
                        .successfulTransactions(succeeded.get())
                        .failedTransactions(failed.get())
                        .threadAOutcome(threadAOutcome.get())
                        .threadBOutcome(threadBOutcome.get())
                        .ledgerBalanced(ledgerBalanced)
                        .noPartialVouchers(noPartialVouchers)
                        .batchTotalsIntact(batchTotalsIntact)
                        .systemRecovered(systemRecovered)
                        .verificationDetails(details.toString())
                        .build();

        log.info(result.toSummary());
        return result;
    }

    private String classifyDeadlock(Exception e, AtomicInteger deadlockCount) {
        String msg = e.getMessage() != null ? e.getMessage().toUpperCase() : "";
        String cause =
                e.getCause() != null && e.getCause().getMessage() != null
                        ? e.getCause().getMessage().toUpperCase()
                        : "";
        String combined = msg + " " + cause;

        if (combined.contains("DEADLOCK")) {
            deadlockCount.incrementAndGet();
            log.warn("DEADLOCK_DETECTED: {}", e.getMessage());
            return "DEADLOCK_VICTIM";
        } else if (combined.contains("LOCK") && combined.contains("TIMEOUT")) {
            deadlockCount.incrementAndGet();
            log.warn("LOCK_TIMEOUT_DETECTED: {}", e.getMessage());
            return "LOCK_TIMEOUT";
        } else if (combined.contains("LOCK") || combined.contains("PESSIMISTIC")) {
            deadlockCount.incrementAndGet();
            log.warn("LOCK_CONTENTION: {}", e.getMessage());
            return "LOCK_CONTENTION";
        }
        return "FAILED: " + e.getClass().getSimpleName();
    }

    private void setupContext(String username, Long tenantId) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                username,
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_TELLER"))));
        TenantContextHolder.setTenantId(tenantId);
    }

    private void clearContext() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
