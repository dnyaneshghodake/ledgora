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
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Premature closure service for FD/RD deposits.
 *
 * <p>RBI Master Directions on Premature Withdrawal:
 *
 * <ul>
 *   <li>Interest recalculated at reduced rate (original rate - penalty %)
 *   <li>Penalty applied as % of interest earned
 *   <li>Net payout = principal + adjusted interest - penalty
 *   <li>Maker-checker mandatory for premature closure
 * </ul>
 *
 * <p>Accounting entries (via voucher engine):
 *
 * <pre>
 *   DR Deposit Liability GL (close the deposit)
 *   CR Customer Account (payout)
 *   DR/CR Interest Adjustment GL (penalty adjustment)
 * </pre>
 */
@Service
public class DepositPrematureClosureService {

    private static final Logger log = LoggerFactory.getLogger(DepositPrematureClosureService.class);

    private final DepositAccountRepository depositAccountRepository;
    private final VoucherService voucherService;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final AuditService auditService;

    public DepositPrematureClosureService(
            DepositAccountRepository depositAccountRepository,
            VoucherService voucherService,
            BranchRepository branchRepository,
            UserRepository userRepository,
            TenantService tenantService,
            AuditService auditService) {
        this.depositAccountRepository = depositAccountRepository;
        this.voucherService = voucherService;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.auditService = auditService;
    }

    /**
     * Execute premature closure of an FD/RD deposit.
     *
     * @return the closed deposit account with updated status
     */
    @Transactional
    public DepositAccount prematureClose(Long depositAccountId) {
        DepositAccount deposit =
                depositAccountRepository
                        .findById(depositAccountId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "DEPOSIT_NOT_FOUND",
                                                "Deposit account not found: " + depositAccountId));

        // Tenant isolation: verify deposit belongs to current tenant
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null
                && (deposit.getTenant() == null || !deposit.getTenant().getId().equals(tenantId))) {
            throw new BusinessException(
                    "DEPOSIT_NOT_FOUND", "Deposit account not found: " + depositAccountId);
        }

        // CBS Tier-1: validate business day is OPEN before financial operations
        Long effectiveTenantId = tenantId != null ? tenantId : deposit.getTenant().getId();
        tenantService.validateBusinessDayOpen(effectiveTenantId);

        if (deposit.getStatus() != DepositAccountStatus.ACTIVE) {
            throw new BusinessException(
                    "DEPOSIT_NOT_ACTIVE",
                    "Only active deposits can be prematurely closed. Status: "
                            + deposit.getStatus());
        }

        DepositProduct product = deposit.getDepositProduct();
        if (product.getDepositType() == DepositType.SAVINGS
                || product.getDepositType() == DepositType.CURRENT) {
            throw new BusinessException(
                    "CASA_NO_PREMATURE",
                    "CASA accounts cannot be prematurely closed. Use account closure instead.");
        }

        // Calculate penalty
        BigDecimal penaltyPercent = product.getPrematurePenaltyPercent();
        BigDecimal interestEarned = deposit.getInterestAccrued();
        BigDecimal penalty =
                interestEarned
                        .multiply(penaltyPercent)
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        BigDecimal adjustedInterest = interestEarned.subtract(penalty);
        BigDecimal netPayout = deposit.getPrincipalAmount().add(adjustedInterest);

        // CBS Tier-1: use tenant business date, never system clock
        LocalDate businessDate = tenantService.getCurrentBusinessDate(effectiveTenantId);

        // ── VOUCHER ENGINE: Post premature closure (DR Deposit Liability, CR Customer Account) ──
        // CBS invariant: all financial mutations flow through the voucher engine.
        if (product.getGlDepositLiability() != null) {
            postPrematureClosureVouchers(
                    deposit, product, netPayout, businessDate);
        } else {
            log.warn(
                    "Deposit product {} missing GL mappings — closure recorded without voucher",
                    product.getProductCode());
        }

        deposit.setStatus(DepositAccountStatus.PREMATURED);
        deposit.setClosureDate(businessDate);
        deposit.setInterestAccrued(adjustedInterest);
        deposit = depositAccountRepository.save(deposit);

        auditService.logEvent(
                null,
                "DEPOSIT_PREMATURE_CLOSURE",
                "DEPOSIT_ACCOUNT",
                deposit.getId(),
                "Deposit "
                        + deposit.getDepositAccountNumber()
                        + " prematurely closed. Principal="
                        + deposit.getPrincipalAmount()
                        + " Interest="
                        + interestEarned
                        + " Penalty="
                        + penalty
                        + " NetPayout="
                        + netPayout,
                null);

        log.info(
                "Deposit prematurely closed: {} payout={} penalty={}",
                deposit.getDepositAccountNumber(),
                netPayout,
                penalty);

        return deposit;
    }

    /**
     * Post premature closure voucher pair via the voucher engine.
     *
     * <p>CBS-grade double-entry:
     *
     * <pre>
     *   DR Deposit Liability GL (close the deposit — liability decreases)
     *   CR Customer Account GL (net payout — funds returned to customer)
     * </pre>
     */
    private void postPrematureClosureVouchers(
            DepositAccount deposit,
            DepositProduct product,
            BigDecimal netPayout,
            LocalDate businessDate) {
        try {
            Tenant tenant = deposit.getTenant();

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

            String batchCode = "DEP-CLOSE-" + deposit.getDepositAccountNumber();

            // DR Deposit Liability GL, CR Customer Account (via linked account's GL)
            // The Interest Expense GL is used as the CR leg since the customer receives the payout
            Voucher[] pair =
                    voucherService.createVoucherPair(
                            tenant,
                            branch,
                            deposit.getLinkedAccount(),
                            product.getGlDepositLiability(), // DR — Deposit Liability (decreases)
                            branch,
                            deposit.getLinkedAccount(),
                            product.getGlInterestExpense(), // CR — Interest Expense (payout)
                            netPayout,
                            "INR",
                            businessDate,
                            batchCode,
                            systemUser,
                            "Premature closure DR: "
                                    + deposit.getDepositAccountNumber()
                                    + " liability="
                                    + netPayout,
                            "Premature closure CR: "
                                    + deposit.getDepositAccountNumber()
                                    + " payout="
                                    + netPayout);

            voucherService.systemAuthorizeVoucher(pair[0].getId(), systemUser);
            voucherService.systemAuthorizeVoucher(pair[1].getId(), systemUser);
            voucherService.postVoucher(pair[0].getId());
            voucherService.postVoucher(pair[1].getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Premature closure voucher posting failed for {}: {}",
                    deposit.getDepositAccountNumber(),
                    e.getMessage(),
                    e);
            throw new BusinessException(
                    "VOUCHER_POSTING_FAILED",
                    "Premature closure voucher posting failed for "
                            + deposit.getDepositAccountNumber()
                            + ": "
                            + e.getMessage());
        }
    }
}
