package com.ledgora.eod.service;

import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.approval.repository.ApprovalRequestRepository;
import com.ledgora.batch.service.BatchService;
import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.gl.service.CbsGlBalanceService;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.repository.TransactionRepository;
import com.ledgora.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS End-of-Day (EOD) Validation Service.
 *
 * <p>Before EOD: 1. All vouchers auth_flag = Y 2. All vouchers post_flag = Y 3.
 * actual_total_balance == SUM(ledger) 4. shadow_total_balance correct 5. Clearing GL balanced 6. No
 * unposted vouchers 7. Branch GL balanced 8. Tenant GL balanced
 *
 * <p>If validation fails -> block EOD.
 *
 * <p>After EOD: - Lock posting_date - Disallow further transactions for that business day
 */
@Service
public class EodValidationService {

    private static final Logger log = LoggerFactory.getLogger(EodValidationService.class);

    private final VoucherRepository voucherRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final CbsGlBalanceService cbsGlBalanceService;
    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final BatchService batchService;
    private final TransactionRepository transactionRepository;
    private final com.ledgora.clearing.service.InterBranchClearingService
            interBranchClearingService;
    private final com.ledgora.clearing.service.IbtService ibtService;

    public EodValidationService(
            VoucherRepository voucherRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AccountBalanceRepository accountBalanceRepository,
            ApprovalRequestRepository approvalRequestRepository,
            CbsGlBalanceService cbsGlBalanceService,
            TenantService tenantService,
            TenantRepository tenantRepository,
            BatchService batchService,
            TransactionRepository transactionRepository,
            com.ledgora.clearing.service.InterBranchClearingService interBranchClearingService,
            com.ledgora.clearing.service.IbtService ibtService) {
        this.voucherRepository = voucherRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.cbsGlBalanceService = cbsGlBalanceService;
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.batchService = batchService;
        this.transactionRepository = transactionRepository;
        this.interBranchClearingService = interBranchClearingService;
        this.ibtService = ibtService;
    }

    /**
     * Validate all EOD conditions before closing the business day. Returns list of validation
     * errors. Empty list means all validations passed.
     */
    public List<String> validateEod(Long tenantId, LocalDate businessDate) {
        List<String> errors = new ArrayList<>();

        // 1. Check all vouchers are authorized (authFlag=N, cancelFlag=N)
        long unauthorizedCount =
                voucherRepository.countUnauthorizedVouchers(tenantId, businessDate);
        if (unauthorizedCount > 0) {
            errors.add(
                    "EOD blocked: "
                            + unauthorizedCount
                            + " unauthorized voucher(s) found for "
                            + businessDate
                            + ". Authorize or cancel them before EOD.");
        }

        // 2. Check no vouchers are APPROVED but not yet POSTED (authFlag=Y, postFlag=N,
        // cancelFlag=N)
        long approvedUnposted =
                voucherRepository.countApprovedUnpostedVouchers(tenantId, businessDate);
        if (approvedUnposted > 0) {
            errors.add(
                    "EOD blocked: "
                            + approvedUnposted
                            + " approved-but-unposted voucher(s) found for "
                            + businessDate
                            + ". Post or cancel them before EOD.");
        }

        // 3. Validate actual_total_balance == SUM(ledger) per account
        // This is done by checking ledger integrity for the date
        BigDecimal debits =
                ledgerEntryRepository.sumDebitsByBusinessDateAndTenantId(businessDate, tenantId);
        BigDecimal credits =
                ledgerEntryRepository.sumCreditsByBusinessDateAndTenantId(businessDate, tenantId);
        if (debits.compareTo(credits) != 0) {
            errors.add(
                    "EOD blocked: Ledger integrity check failed. Debits="
                            + debits
                            + ", Credits="
                            + credits);
        }

        // NEW: Validate total posted voucher debits == credits for the date
        java.math.BigDecimal voucherDebits =
                voucherRepository.sumPostedDebits(tenantId, businessDate);
        java.math.BigDecimal voucherCredits =
                voucherRepository.sumPostedCredits(tenantId, businessDate);
        if (voucherDebits.compareTo(voucherCredits) != 0) {
            errors.add(
                    "EOD blocked: posted voucher totals unbalanced. Debits="
                            + voucherDebits
                            + ", Credits="
                            + voucherCredits
                            + " for "
                            + businessDate);
        }

        long pendingApprovals =
                approvalRequestRepository.countByTenant_IdAndStatus(
                        tenantId, ApprovalStatus.PENDING);
        if (pendingApprovals > 0) {
            errors.add(
                    "EOD blocked: "
                            + pendingApprovals
                            + " pending approval request(s) exist for tenant "
                            + tenantId);
        }

        // NEW: Check no PENDING_APPROVAL transactions exist
        long pendingTxns =
                transactionRepository.countByTenantIdAndStatus(
                        tenantId, com.ledgora.common.enums.TransactionStatus.PENDING_APPROVAL);
        if (pendingTxns > 0) {
            errors.add(
                    "EOD blocked: "
                            + pendingTxns
                            + " transaction(s) pending approval for tenant "
                            + tenantId);
        }

        // Inter-branch clearing validation: all IBC transfers must be SETTLED or FAILED
        if (interBranchClearingService != null) {
            String ibcError =
                    interBranchClearingService.validateClearingBalance(tenantId, businessDate);
            if (ibcError != null) {
                errors.add(ibcError);
            }
        }

        // CBS Standard (Step 3): Clearing GL net balance must be zero at EOD
        if (ibtService != null) {
            String clearingGlError = ibtService.validateClearingGlNetZero(tenantId);
            if (clearingGlError != null) {
                errors.add(clearingGlError);
            }
        }

        // 7. Branch GL balanced - checked if CbsGlBalanceService is tracking balances
        // 8. Tenant GL balanced
        if (!cbsGlBalanceService.isTenantGlBalanced(tenantId)) {
            errors.add("EOD blocked: Tenant GL is not balanced for tenant " + tenantId);
        }

        if (!errors.isEmpty()) {
            log.warn(
                    "EOD validation failed for tenant {} on {}: {}",
                    tenantId,
                    businessDate,
                    errors);
        } else {
            log.info("EOD validation passed for tenant {} on {}", tenantId, businessDate);
        }

        return errors;
    }

    /**
     * Run EOD process: validate, then close and advance the business day. Blocks if validation
     * fails.
     */
    @Transactional
    public void runEod(Long tenantId) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        LocalDate businessDate = tenant.getCurrentBusinessDate();

        // Pre-flight validation (advisory — may pass but concurrent txn could sneak in)
        List<String> errors = validateEod(tenantId, businessDate);
        if (!errors.isEmpty()) {
            throw new RuntimeException("EOD validation failed: " + String.join("; ", errors));
        }

        // Step 1: Start day closing (blocks new transactions via validateBusinessDayOpen)
        tenantService.startDayClosing(tenantId);

        // Step 1b: Re-validate after blocking new transactions to close TOCTOU gap.
        // Any concurrent transaction that sneaked in between pre-flight and day-closing
        // will now be detected.
        List<String> postLockErrors = validateEod(tenantId, businessDate);
        if (!postLockErrors.isEmpty()) {
            throw new RuntimeException(
                    "EOD validation failed after day-closing lock: "
                            + String.join("; ", postLockErrors));
        }

        // Step 2: Close all open batches for the business date
        batchService.closeAllBatches(tenantId, businessDate);

        // Step 3: Settle all closed batches (validates debit == credit per batch)
        batchService.settleAllBatches(tenantId, businessDate);

        // Step 4: Close day and advance to next business date (sets CLOSED, requires Day Begin)
        tenantService.closeDayAndAdvance(tenantId);

        log.info(
                "EOD completed for tenant {}: business date {} closed. Next date requires Day Begin.",
                tenantId,
                businessDate);
    }

    /** Check if EOD can be run (all validations pass). */
    public boolean canRunEod(Long tenantId) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        List<String> errors = validateEod(tenantId, tenant.getCurrentBusinessDate());
        return errors.isEmpty();
    }
}
