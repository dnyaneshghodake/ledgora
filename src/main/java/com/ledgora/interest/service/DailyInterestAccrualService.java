package com.ledgora.interest.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountProductSnapshot;
import com.ledgora.account.repository.AccountProductSnapshotRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.interest.entity.InterestAccrualLog;
import com.ledgora.interest.repository.InterestAccrualLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS-grade Daily Interest Accrual Service. Runs as part of EOD processing
 * (before BATCH_CLOSED). For each ACTIVE SAVINGS account:
 *
 * <ol>
 *   <li>Calculate daily interest: balance × rate / 365
 *   <li>Post accrual voucher: DR Interest Expense GL, CR Interest Accrual GL
 *   <li>Log to InterestAccrualLog for idempotency
 * </ol>
 *
 * <p>Idempotent per (tenant, account, business_date) — safe to retry on EOD failure.
 *
 * <p>GL codes are resolved from:
 *   - AccountProductSnapshot (if account was opened under Product engine)
 *   - Default GL codes (5100 Interest Expense, 2110 Savings Deposits) for legacy accounts
 */
@Service
public class DailyInterestAccrualService {

    private static final Logger log = LoggerFactory.getLogger(DailyInterestAccrualService.class);

    /** Default interest rate for savings accounts without product-level configuration. */
    private static final BigDecimal DEFAULT_SAVINGS_RATE = new BigDecimal("4.0000"); // 4% p.a.
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final String DEFAULT_DR_GL = "5100"; // Interest Expense
    private static final String DEFAULT_CR_GL = "2110"; // Savings Deposits (accrual)

    private final AccountRepository accountRepository;
    private final AccountProductSnapshotRepository snapshotRepository;
    private final InterestAccrualLogRepository accrualLogRepository;

    public DailyInterestAccrualService(
            AccountRepository accountRepository,
            AccountProductSnapshotRepository snapshotRepository,
            InterestAccrualLogRepository accrualLogRepository) {
        this.accountRepository = accountRepository;
        this.snapshotRepository = snapshotRepository;
        this.accrualLogRepository = accrualLogRepository;
    }

    /**
     * Run daily interest accrual for all SAVINGS accounts in a tenant.
     * Called by EOD orchestrator before BATCH_CLOSED.
     *
     * @param tenantId the tenant to process
     * @param businessDate the business date for accrual
     * @return number of accounts accrued
     */
    @Transactional
    public int runDailyAccrual(Long tenantId, LocalDate businessDate) {
        log.info("Starting daily interest accrual for tenant {} date {}", tenantId, businessDate);

        // Find all ACTIVE SAVINGS accounts for this tenant
        List<Account> savingsAccounts =
                accountRepository.findByTenantIdAndAccountType(tenantId, AccountType.SAVINGS)
                        .stream()
                        .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                        .filter(a -> a.getBalance().compareTo(BigDecimal.ZERO) > 0)
                        .toList();

        int accrued = 0;
        int skipped = 0;

        for (Account account : savingsAccounts) {
            // Idempotency check: skip if already accrued for this date
            if (accrualLogRepository.existsByTenantIdAndAccountIdAndBusinessDate(
                    tenantId, account.getId(), businessDate)) {
                skipped++;
                continue;
            }

            try {
                accrueInterest(tenantId, account, businessDate);
                accrued++;
            } catch (Exception e) {
                log.error("Interest accrual failed for account {}: {}",
                        account.getAccountNumber(), e.getMessage());
                // Continue processing other accounts — don't fail the entire batch
            }
        }

        log.info("Daily interest accrual complete for tenant {}: accrued={} skipped={} total={}",
                tenantId, accrued, skipped, savingsAccounts.size());
        return accrued;
    }

    /**
     * Accrue interest for a single account on a given business date.
     * Formula: daily_interest = balance × (annual_rate / 100) / 365
     */
    private void accrueInterest(Long tenantId, Account account, LocalDate businessDate) {
        BigDecimal balance = account.getBalance();
        BigDecimal annualRate = resolveInterestRate(account);
        String drGl = DEFAULT_DR_GL;
        String crGl = DEFAULT_CR_GL;

        // If account has a product snapshot, use its GL codes
        snapshotRepository.findByAccountId(account.getId()).ifPresent(snapshot -> {
            // Interest accrual GL from product config takes precedence
            // (drGl and crGl are effectively final in the log builder below)
        });

        // Resolve GL from product snapshot if available
        AccountProductSnapshot snapshot =
                snapshotRepository.findByAccountId(account.getId()).orElse(null);
        if (snapshot != null) {
            drGl = snapshot.getInterestAccrualGlCode(); // e.g., "5100" Interest Expense
            crGl = snapshot.getCrGlCode(); // e.g., "2110" Savings Deposits
        }

        // Calculate: balance × (rate / 100) / 365
        BigDecimal dailyInterest = balance
                .multiply(annualRate)
                .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                .divide(DAYS_IN_YEAR, 4, RoundingMode.HALF_UP);

        if (dailyInterest.compareTo(BigDecimal.ZERO) <= 0) {
            return; // No interest to accrue
        }

        // Create idempotency log (the voucher posting would be done by the EOD orchestrator
        // using VoucherService — here we record the accrual calculation)
        InterestAccrualLog logEntry = InterestAccrualLog.builder()
                .tenantId(tenantId)
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .businessDate(businessDate)
                .balanceUsed(balance)
                .annualRate(annualRate)
                .accruedAmount(dailyInterest)
                .drGlCode(drGl)
                .crGlCode(crGl)
                .build();
        accrualLogRepository.save(logEntry);

        log.debug("Interest accrued: account={} balance={} rate={}% daily={}",
                account.getAccountNumber(), balance, annualRate, dailyInterest);
    }

    /**
     * Resolve interest rate for an account. Priority:
     * 1. Account-level interestRate (if set)
     * 2. Default savings rate (4% p.a.)
     */
    private BigDecimal resolveInterestRate(Account account) {
        if (account.getInterestRate() != null
                && account.getInterestRate().compareTo(BigDecimal.ZERO) > 0) {
            return account.getInterestRate();
        }
        return DEFAULT_SAVINGS_RATE;
    }

    /**
     * Check if accrual has already been run for a tenant on a given date.
     * Used by EOD orchestrator to decide whether to skip this phase.
     */
    public boolean isAccrualComplete(Long tenantId, LocalDate businessDate) {
        return accrualLogRepository.countByTenantIdAndBusinessDate(tenantId, businessDate) > 0;
    }

    /**
     * Get accrual summary for a tenant and date (for EOD validation/reporting).
     */
    public List<InterestAccrualLog> getAccrualLog(Long tenantId, LocalDate businessDate) {
        return accrualLogRepository.findByTenantIdAndBusinessDate(tenantId, businessDate);
    }
}
