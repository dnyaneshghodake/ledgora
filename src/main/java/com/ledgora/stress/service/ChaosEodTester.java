package com.ledgora.stress.service;

import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.AccountType;
import com.ledgora.eod.entity.EodProcess;
import com.ledgora.eod.entity.EodProcess.EodPhase;
import com.ledgora.eod.repository.EodProcessRepository;
import com.ledgora.eod.service.EodStateMachineService;
import com.ledgora.eod.service.EodValidationService;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.stress.dto.ChaosEodResult;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Chaos testing for the EOD state machine — simulates crash-and-resume scenarios.
 *
 * <p>Active only in the "stress" profile. Does NOT modify EodStateMachineService logic.
 *
 * <p>Strategy: Since we cannot inject faults directly into the state machine (no service
 * modifications allowed), we test the recovery mechanism by:
 *
 * <ol>
 *   <li>Running EOD which may fail due to validation (creating a FAILED EodProcess)
 *   <li>Manually setting an EodProcess to FAILED at a specific phase (simulating crash)
 *   <li>Calling executeEod() again — which should detect the FAILED process and resume
 *   <li>Verifying post-recovery integrity: ledger balanced, no duplicates, no stuck processes
 * </ol>
 *
 * <p>The EOD state machine's own resume logic handles the actual recovery. This tester only creates
 * the "crashed" precondition and then validates the outcome.
 */
@Service
@Profile("stress")
public class ChaosEodTester {

    private static final Logger log = LoggerFactory.getLogger(ChaosEodTester.class);

    private final EodValidationService eodValidationService;
    private final EodStateMachineService eodStateMachineService;
    private final EodProcessRepository eodProcessRepository;
    private final TenantRepository tenantRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final VoucherRepository voucherRepository;
    private final AccountRepository accountRepository;

    public ChaosEodTester(
            EodValidationService eodValidationService,
            EodStateMachineService eodStateMachineService,
            EodProcessRepository eodProcessRepository,
            TenantRepository tenantRepository,
            LedgerEntryRepository ledgerEntryRepository,
            VoucherRepository voucherRepository,
            AccountRepository accountRepository) {
        this.eodValidationService = eodValidationService;
        this.eodStateMachineService = eodStateMachineService;
        this.eodProcessRepository = eodProcessRepository;
        this.tenantRepository = tenantRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.voucherRepository = voucherRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Run chaos EOD test: simulate crash at specified phase, then verify resume + integrity.
     *
     * @param tenantId tenant to test
     * @param crashAfterPhase phase after which to simulate crash (VALIDATED, DAY_CLOSING,
     *     BATCH_CLOSED, SETTLED). If null, tests full EOD + resume of a manufactured FAILED state.
     * @return structured chaos test result
     */
    public ChaosEodResult runChaosTest(Long tenantId, String crashAfterPhase) {
        setupContext(tenantId);
        List<String> events = new ArrayList<>();

        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        // Capture pre-test state
        long preVoucherCount = voucherRepository.count();
        events.add("Pre-test voucher count: " + preVoucherCount);

        // Phase 1: Attempt EOD — it may succeed or fail depending on data state
        boolean initialEodFailed = false;
        try {
            eodValidationService.runEod(tenantId);
            events.add("Initial EOD completed successfully");
        } catch (Exception e) {
            initialEodFailed = true;
            events.add("Initial EOD failed (expected for chaos): " + e.getMessage());
        }

        // Phase 2: Simulate crash by manufacturing a FAILED EodProcess
        boolean crashSimulated = false;
        EodPhase targetPhase =
                crashAfterPhase != null ? EodPhase.valueOf(crashAfterPhase) : EodPhase.DAY_CLOSING;

        // Refresh tenant (date may have advanced)
        tenant = tenantRepository.findById(tenantId).orElseThrow();
        bizDate = tenant.getCurrentBusinessDate();

        Optional<EodProcess> existingProcess =
                eodProcessRepository.findByTenantIdAndBusinessDate(tenantId, bizDate);

        if (existingProcess.isPresent()) {
            EodProcess process = existingProcess.get();
            if ("COMPLETED".equals(process.getStatus())) {
                // EOD already completed — simulate crash by deleting and re-creating as FAILED
                eodProcessRepository.delete(process);
                eodProcessRepository.flush();
                events.add("Deleted completed EodProcess to simulate crash scenario");
            }
        }

        // Check again after potential deletion
        existingProcess = eodProcessRepository.findByTenantIdAndBusinessDate(tenantId, bizDate);

        if (existingProcess.isEmpty()) {
            // Create a FAILED EodProcess at the target phase to simulate crash
            EodProcess crashedProcess =
                    EodProcess.builder()
                            .tenant(tenant)
                            .businessDate(bizDate)
                            .phase(targetPhase)
                            .status("FAILED")
                            .failureReason("CHAOS_TEST: Simulated crash after " + targetPhase)
                            .build();
            eodProcessRepository.save(crashedProcess);
            crashSimulated = true;
            events.add("Manufactured FAILED EodProcess at phase " + targetPhase);
        } else if ("FAILED".equals(existingProcess.get().getStatus())) {
            crashSimulated = true;
            events.add(
                    "Existing FAILED EodProcess found at phase "
                            + existingProcess.get().getPhase());
        } else {
            events.add("EodProcess exists with status: " + existingProcess.get().getStatus());
        }

        // Phase 3: Resume — call executeEod() which should detect FAILED and resume
        boolean resumeAttempted = false;
        boolean resumeSucceeded = false;
        String resumeFailure = null;

        if (crashSimulated) {
            resumeAttempted = true;
            try {
                eodValidationService.runEod(tenantId);
                resumeSucceeded = true;
                events.add("Resume EOD completed successfully");
            } catch (Exception e) {
                resumeFailure = e.getClass().getSimpleName() + ": " + e.getMessage();
                events.add("Resume EOD failed: " + resumeFailure);
            }
        }

        // Phase 4: Post-recovery validation
        // Refresh state
        tenant = tenantRepository.findById(tenantId).orElseThrow();
        bizDate = tenant.getCurrentBusinessDate();

        // Ledger balanced
        BigDecimal debits = ledgerEntryRepository.sumDebitsByBusinessDate(bizDate);
        BigDecimal credits = ledgerEntryRepository.sumCreditsByBusinessDate(bizDate);
        boolean ledgerBalanced = debits.compareTo(credits) == 0;
        events.add("Ledger check: DR=" + debits + " CR=" + credits);

        // No duplicate vouchers (count should be consistent — no orphans from partial commits)
        long postVoucherCount = voucherRepository.count();
        long voucherDelta = postVoucherCount - preVoucherCount;
        boolean noDuplicateVouchers = voucherDelta % 2 == 0; // vouchers come in pairs
        events.add("Voucher delta: " + voucherDelta + " (pairs=" + (voucherDelta / 2) + ")");

        // No stuck RUNNING processes
        List<EodProcess> runningProcesses = eodStateMachineService.findIncompleteProcesses();
        boolean noStuckRunning = runningProcesses.isEmpty();
        events.add("Running processes: " + runningProcesses.size());

        // Clearing GL zero
        BigDecimal clearingNet =
                accountRepository.sumBalanceByTenantIdAndAccountType(
                        tenantId, AccountType.CLEARING_ACCOUNT);
        boolean clearingGlZero = clearingNet.compareTo(BigDecimal.ZERO) == 0;

        // Suspense GL zero
        BigDecimal suspenseNet =
                accountRepository.sumBalanceByTenantIdAndAccountType(
                        tenantId, AccountType.SUSPENSE_ACCOUNT);
        boolean suspenseGlZero = suspenseNet.compareTo(BigDecimal.ZERO) == 0;

        // EOD completed check
        Optional<EodProcess> finalProcess =
                eodProcessRepository.findByTenantIdAndBusinessDate(tenantId, bizDate);
        boolean eodCompleted =
                finalProcess.isPresent() && "COMPLETED".equals(finalProcess.get().getStatus());

        ChaosEodResult result =
                ChaosEodResult.builder()
                        .crashAfterPhase(crashAfterPhase != null ? crashAfterPhase : "DAY_CLOSING")
                        .crashSimulated(crashSimulated)
                        .resumeAttempted(resumeAttempted)
                        .resumeSucceeded(resumeSucceeded)
                        .resumeFailureReason(resumeFailure)
                        .ledgerBalanced(ledgerBalanced)
                        .noDuplicateVouchers(noDuplicateVouchers)
                        .noStuckRunning(noStuckRunning)
                        .clearingGlZero(clearingGlZero)
                        .suspenseGlZero(suspenseGlZero)
                        .eodCompleted(eodCompleted)
                        .events(events)
                        .build();

        log.info(result.toSummary());
        clearContext();
        return result;
    }

    private void setupContext(Long tenantId) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "chaos-eod",
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        TenantContextHolder.setTenantId(tenantId);
    }

    private void clearContext() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
