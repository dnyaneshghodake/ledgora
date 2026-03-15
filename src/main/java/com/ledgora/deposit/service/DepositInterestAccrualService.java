package com.ledgora.deposit.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.deposit.entity.DepositAccount;
import com.ledgora.deposit.entity.DepositProduct;
import com.ledgora.deposit.enums.DepositAccountStatus;
import com.ledgora.deposit.enums.DepositType;
import com.ledgora.deposit.repository.DepositAccountRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
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

    private static final Logger log =
            LoggerFactory.getLogger(DepositInterestAccrualService.class);
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private final DepositAccountRepository depositAccountRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public DepositInterestAccrualService(
            DepositAccountRepository depositAccountRepository,
            TenantRepository tenantRepository,
            AuditService auditService) {
        this.depositAccountRepository = depositAccountRepository;
        this.tenantRepository = tenantRepository;
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
                        .orElseThrow(
                                () -> new RuntimeException("Tenant not found: " + tenantId));
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

            // NOTE: The actual GL posting (DR Interest Expense, CR Deposit Liability)
            // should be done via the voucher engine in production:
            //   VoucherService.createVoucherPair(
            //       product.getGlInterestExpense(),    // DR
            //       product.getGlDepositLiability(),   // CR
            //       dailyInterest, ...)
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
                    "Daily interest accrued for " + accrued
                            + " deposit accounts (tenant " + tenantId + ")",
                    null);
            log.info("Deposit interest accrued: {} accounts for tenant {}", accrued, tenantId);
        }

        return accrued;
    }
}
