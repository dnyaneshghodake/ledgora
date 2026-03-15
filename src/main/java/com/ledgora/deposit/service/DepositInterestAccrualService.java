package com.ledgora.deposit.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.deposit.entity.DepositAccount;
import com.ledgora.deposit.entity.DepositProduct;
import com.ledgora.deposit.enums.DepositAccountStatus;
import com.ledgora.deposit.enums.DepositType;
import com.ledgora.deposit.repository.DepositAccountRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI-compliant daily interest accrual for all deposit types.
 *
 * <p>RBI Master Directions on Interest Rate on Deposits:
 *
 * <ul>
 *   <li>SAVINGS: daily balance × rate / 365 — posted quarterly
 *   <li>CURRENT: zero interest (RBI regulation)
 *   <li>FD: principal × rate / 365 — accrued daily, posted at maturity or quarterly
 *   <li>RD: cumulative balance × rate / 365 — accrued daily
 * </ul>
 *
 * <p>Accounting entry (via voucher engine — NOT direct balance mutation):
 *
 * <pre>
 *   DR Interest Expense GL (Expense — P&L impact)
 *   CR Customer Deposit Account (Liability — Balance Sheet)
 * </pre>
 *
 * <p>Called during EOD Phase VALIDATED, BEFORE financial statement generation.
 */
@Service
public class DepositInterestAccrualService {

    private static final Logger log = LoggerFactory.getLogger(DepositInterestAccrualService.class);
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private final DepositAccountRepository depositAccountRepository;
    private final TenantRepository tenantRepository;
    private final VoucherService voucherService;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public DepositInterestAccrualService(
            DepositAccountRepository depositAccountRepository,
            TenantRepository tenantRepository,
            VoucherService voucherService,
            BranchRepository branchRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.depositAccountRepository = depositAccountRepository;
        this.tenantRepository = tenantRepository;
        this.voucherService = voucherService;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Accrue daily interest for all active deposit accounts of a tenant.
     *
     * @return number of accounts accrued
     */
    @Transactional
    public int accrueDailyInterest(Long tenantId) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        LocalDate businessDate = tenant.getCurrentBusinessDate();

        var activeDeposits = depositAccountRepository.findActiveByTenantId(tenantId);
        int accrued = 0;

        for (DepositAccount deposit : activeDeposits) {
            if (deposit.getStatus() != DepositAccountStatus.ACTIVE) continue;

            // Idempotency: skip if already accrued for this business date
            if (businessDate.equals(deposit.getLastAccrualDate())) continue;

            DepositProduct product = deposit.getDepositProduct();

            // Current accounts: zero interest per RBI
            if (product.getDepositType() == DepositType.CURRENT) continue;

            // Skip if zero rate
            if (product.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal dailyRate =
                    product.getInterestRate()
                            .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                            .divide(DAYS_IN_YEAR, 10, RoundingMode.HALF_UP);

            BigDecimal dailyInterest =
                    deposit.getPrincipalAmount()
                            .multiply(dailyRate, MathContext.DECIMAL128)
                            .setScale(4, RoundingMode.HALF_UP);

            if (dailyInterest.compareTo(BigDecimal.ZERO) <= 0) continue;

            // ── VOUCHER ENGINE: Post accrual (DR Interest Expense, CR Deposit Liability) ──
            // CBS invariant: all financial mutations flow through the voucher engine
            // to create immutable LedgerEntry records visible to BalanceSheetEngine/PnlEngine.
            if (product.getGlInterestExpense() != null
                    && product.getGlDepositLiability() != null) {
                postDepositAccrualVouchers(
                        tenant, product, deposit, dailyInterest, businessDate);
            } else {
                log.warn(
                        "Deposit product {} missing GL mappings — accrual recorded without voucher",
                        product.getProductCode());
            }

            deposit.setInterestAccrued(deposit.getInterestAccrued().add(dailyInterest));
            deposit.setLastAccrualDate(businessDate);
            depositAccountRepository.save(deposit);
            accrued++;
        }

        if (accrued > 0) {
            auditService.logEvent(
                    null,
                    "DEPOSIT_INTEREST_ACCRUAL",
                    "DEPOSIT_BATCH",
                    null,
                    "Daily interest accrued for "
                            + accrued
                            + " deposit accounts (tenant "
                            + tenantId
                            + ")",
                    null);
            log.info("Deposit interest accrued: {} accounts for tenant {}", accrued, tenantId);
        }

        return accrued;
    }

    /**
     * Post deposit accrual voucher pair via the voucher engine.
     *
     * <p>CBS-grade double-entry per RBI deposit interest rules:
     *
     * <pre>
     *   DR Interest Expense GL (Expense — P&L impact)
     *   CR Deposit Liability GL (Liability — Balance Sheet)
     * </pre>
     */
    private void postDepositAccrualVouchers(
            Tenant tenant,
            DepositProduct product,
            DepositAccount deposit,
            BigDecimal dailyInterest,
            LocalDate businessDate) {
        try {
            Branch branch =
                    branchRepository.findByTenantId(tenant.getId()).stream()
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    "NO_BRANCH",
                                                    "No branch configured for tenant "
                                                            + tenant.getTenantCode()));

            User systemUser =
                    userRepository
                            .findByUsername("SYSTEM_AUTO")
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    "SYSTEM_USER_MISSING",
                                                    "SYSTEM_AUTO user not configured."));

            String batchCode =
                    "DEP-ACCR-"
                            + deposit.getDepositAccountNumber()
                            + "-"
                            + businessDate.toString().replace("-", "");

            // DR Interest Expense GL, CR Deposit Liability GL
            Voucher[] pair =
                    voucherService.createVoucherPair(
                            tenant,
                            branch,
                            deposit.getLinkedAccount(),
                            product.getGlInterestExpense(), // DR — Interest Expense
                            branch,
                            deposit.getLinkedAccount(),
                            product.getGlDepositLiability(), // CR — Deposit Liability
                            dailyInterest,
                            "INR",
                            businessDate,
                            batchCode,
                            systemUser,
                            "Deposit accrual DR: "
                                    + deposit.getDepositAccountNumber()
                                    + " interest="
                                    + dailyInterest,
                            "Deposit accrual CR: "
                                    + deposit.getDepositAccountNumber()
                                    + " liability="
                                    + dailyInterest);

            voucherService.systemAuthorizeVoucher(pair[0].getId(), systemUser);
            voucherService.systemAuthorizeVoucher(pair[1].getId(), systemUser);
            voucherService.postVoucher(pair[0].getId());
            voucherService.postVoucher(pair[1].getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Deposit accrual voucher posting failed for {}: {}",
                    deposit.getDepositAccountNumber(),
                    e.getMessage(),
                    e);
            throw new BusinessException(
                    "VOUCHER_POSTING_FAILED",
                    "Deposit accrual voucher posting failed for "
                            + deposit.getDepositAccountNumber()
                            + ": "
                            + e.getMessage());
        }
    }
}
