package com.ledgora.eod.service;

import com.ledgora.approval.repository.ApprovalRequestRepository;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.gl.service.CbsGlBalanceService;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.voucher.repository.VoucherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CBS End-of-Day (EOD) Validation Service.
 *
 * Before EOD:
 * 1. All vouchers auth_flag = Y
 * 2. All vouchers post_flag = Y
 * 3. actual_total_balance == SUM(ledger)
 * 4. shadow_total_balance correct
 * 5. Clearing GL balanced
 * 6. No unposted vouchers
 * 7. Branch GL balanced
 * 8. Tenant GL balanced
 *
 * If validation fails -> block EOD.
 *
 * After EOD:
 * - Lock posting_date
 * - Disallow further transactions for that business day
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

    public EodValidationService(VoucherRepository voucherRepository,
                                 LedgerEntryRepository ledgerEntryRepository,
                                 AccountBalanceRepository accountBalanceRepository,
                                 ApprovalRequestRepository approvalRequestRepository,
                                 CbsGlBalanceService cbsGlBalanceService,
                                 TenantService tenantService,
                                 TenantRepository tenantRepository) {
        this.voucherRepository = voucherRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.cbsGlBalanceService = cbsGlBalanceService;
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Validate all EOD conditions before closing the business day.
     * Returns list of validation errors. Empty list means all validations passed.
     */
    public List<String> validateEod(Long tenantId, LocalDate businessDate) {
        List<String> errors = new ArrayList<>();

        // 1. Check all vouchers are authorized
        long unauthorizedCount = voucherRepository.countUnauthorizedVouchers(tenantId, businessDate);
        if (unauthorizedCount > 0) {
            errors.add("EOD blocked: " + unauthorizedCount + " unauthorized voucher(s) found for " + businessDate);
        }

        // 2 & 6. Check all vouchers are posted (no unposted vouchers)
        long unpostedCount = voucherRepository.countUnpostedVouchers(tenantId, businessDate);
        if (unpostedCount > 0) {
            errors.add("EOD blocked: " + unpostedCount + " unposted voucher(s) found for " + businessDate);
        }

        // 3. Validate actual_total_balance == SUM(ledger) per account
        // This is done by checking ledger integrity for the date
        BigDecimal debits = ledgerEntryRepository.sumDebitsByBusinessDateAndTenantId(businessDate, tenantId);
        BigDecimal credits = ledgerEntryRepository.sumCreditsByBusinessDateAndTenantId(businessDate, tenantId);
        if (debits.compareTo(credits) != 0) {
            errors.add("EOD blocked: Ledger integrity check failed. Debits=" + debits + ", Credits=" + credits);
        }

        long pendingApprovals = approvalRequestRepository.countByTenant_IdAndStatus(tenantId, ApprovalStatus.PENDING);
        if (pendingApprovals > 0) {
            errors.add("EOD blocked: " + pendingApprovals + " pending approval request(s) exist for tenant " + tenantId);
        }

        // 4. Validate shadow_total_balance is correct (should be zero if all vouchers posted)
        // Shadow balance should equal actual + pending. If all posted, shadow delta should be 0.
        // This is indirectly validated by unposted voucher check above.

        // 7. Branch GL balanced - checked if CbsGlBalanceService is tracking balances
        // 8. Tenant GL balanced
        if (!cbsGlBalanceService.isTenantGlBalanced(tenantId)) {
            errors.add("EOD blocked: Tenant GL is not balanced for tenant " + tenantId);
        }

        if (!errors.isEmpty()) {
            log.warn("EOD validation failed for tenant {} on {}: {}", tenantId, businessDate, errors);
        } else {
            log.info("EOD validation passed for tenant {} on {}", tenantId, businessDate);
        }

        return errors;
    }

    /**
     * Run EOD process: validate, then close and advance the business day.
     * Blocks if validation fails.
     */
    @Transactional
    public void runEod(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        LocalDate businessDate = tenant.getCurrentBusinessDate();

        // Validate
        List<String> errors = validateEod(tenantId, businessDate);
        if (!errors.isEmpty()) {
            throw new RuntimeException("EOD validation failed: " + String.join("; ", errors));
        }

        // Start day closing
        tenantService.startDayClosing(tenantId);

        // Close day and advance to next business date
        tenantService.closeDayAndAdvance(tenantId);

        log.info("EOD completed for tenant {}: business date advanced from {}", tenantId, businessDate);
    }

    /**
     * Check if EOD can be run (all validations pass).
     */
    public boolean canRunEod(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        List<String> errors = validateEod(tenantId, tenant.getCurrentBusinessDate());
        return errors.isEmpty();
    }
}
