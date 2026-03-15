package com.ledgora.deposit.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.deposit.entity.DepositAccount;
import com.ledgora.deposit.entity.DepositProduct;
import com.ledgora.deposit.enums.DepositAccountStatus;
import com.ledgora.deposit.enums.DepositType;
import com.ledgora.deposit.repository.DepositAccountRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
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

    private static final Logger log =
            LoggerFactory.getLogger(DepositPrematureClosureService.class);

    private final DepositAccountRepository depositAccountRepository;
    private final TenantService tenantService;
    private final AuditService auditService;

    public DepositPrematureClosureService(
            DepositAccountRepository depositAccountRepository,
            TenantService tenantService,
            AuditService auditService) {
        this.depositAccountRepository = depositAccountRepository;
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
                && (deposit.getTenant() == null
                        || !deposit.getTenant().getId().equals(tenantId))) {
            throw new BusinessException(
                    "DEPOSIT_NOT_FOUND",
                    "Deposit account not found: " + depositAccountId);
        }

        // CBS Tier-1: validate business day is OPEN before financial operations
        Long effectiveTenantId =
                tenantId != null ? tenantId : deposit.getTenant().getId();
        tenantService.validateBusinessDayOpen(effectiveTenantId);

        if (deposit.getStatus() != DepositAccountStatus.ACTIVE) {
            throw new BusinessException(
                    "DEPOSIT_NOT_ACTIVE",
                    "Only active deposits can be prematurely closed. Status: " + deposit.getStatus());
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

        // NOTE: The actual GL postings should be done via voucher engine:
        //   DR Deposit Liability GL (principal + original interest)
        //   CR Customer Account (net payout)
        //   DR Interest Adjustment GL (penalty)

        // CBS Tier-1: use tenant business date, never system clock
        LocalDate businessDate = tenantService.getCurrentBusinessDate(effectiveTenantId);

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
}
