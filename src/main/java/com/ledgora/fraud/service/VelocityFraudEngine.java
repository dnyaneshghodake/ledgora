package com.ledgora.fraud.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.exception.GovernanceException;
import com.ledgora.fraud.entity.FraudAlert;
import com.ledgora.fraud.entity.VelocityLimit;
import com.ledgora.fraud.repository.FraudAlertRepository;
import com.ledgora.fraud.repository.VelocityLimitRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.transaction.repository.TransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Velocity Fraud Engine — proactive runtime prevention of transaction bursts.
 *
 * <p>RBI Master Direction on Fraud Risk Management (2023) / AML-KYC Transaction Monitoring:
 *
 * <ul>
 *   <li>Queries past 60-minute transaction history for the account
 *   <li>If count or amount threshold exceeded: block transaction, freeze account to UNDER_REVIEW,
 *       create FraudAlert, emit Micrometer metric, log audit trail
 *   <li>Limits resolved per-account first, then tenant-wide default
 *   <li>No bypass path — fraud engine runs before voucher creation
 * </ul>
 */
@Service
public class VelocityFraudEngine {

    private static final Logger log = LoggerFactory.getLogger(VelocityFraudEngine.class);

    /** Default velocity window: 60 minutes. */
    private static final int VELOCITY_WINDOW_MINUTES = 60;

    private final VelocityLimitRepository velocityLimitRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;
    private final Counter velocityBlockedCounter;

    public VelocityFraudEngine(
            VelocityLimitRepository velocityLimitRepository,
            TransactionRepository transactionRepository,
            FraudAlertRepository fraudAlertRepository,
            AccountRepository accountRepository,
            AuditService auditService,
            MeterRegistry meterRegistry) {
        this.velocityLimitRepository = velocityLimitRepository;
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
        this.velocityBlockedCounter =
                Counter.builder("ledgora.velocity.blocked")
                        .description("Transactions blocked by velocity fraud engine")
                        .register(meterRegistry);
    }

    /**
     * Evaluate velocity limits for an account before posting.
     *
     * <p>Called by TransactionService BEFORE any transaction persistence. If velocity is breached:
     *
     * <ol>
     *   <li>Block the transaction (throw GovernanceException)
     *   <li>Freeze the account (status → UNDER_REVIEW)
     *   <li>Create a FraudAlert record
     *   <li>Emit ledgora.velocity.blocked metric
     *   <li>Log audit trail
     * </ol>
     *
     * @param tenant the tenant context
     * @param account the account to check
     * @param amount the new transaction amount
     * @param userId the user initiating the transaction
     * @throws GovernanceException if velocity limit is breached — transaction is blocked
     */
    @Transactional
    public void evaluateVelocity(Tenant tenant, Account account, BigDecimal amount, Long userId) {
        Long tenantId = tenant.getId();
        Long accountId = account.getId();

        // Resolve limit: account-specific first, then tenant default
        VelocityLimit limit =
                velocityLimitRepository
                        .findByTenantIdAndAccountIdAndIsActiveTrue(tenantId, accountId)
                        .orElseGet(
                                () ->
                                        velocityLimitRepository
                                                .findByTenantIdAndAccountIdIsNullAndIsActiveTrue(
                                                        tenantId)
                                                .orElse(null));

        if (limit == null) {
            return; // No velocity limit configured — allow
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);

        // Check transaction count in window
        long recentCount =
                transactionRepository.countRecentByAccountId(tenantId, accountId, since);
        if (recentCount >= limit.getMaxTxnCountPerHour()) {
            handleVelocityBreach(
                    tenant,
                    account,
                    userId,
                    "VELOCITY_COUNT",
                    "Account "
                            + account.getAccountNumber()
                            + " has "
                            + (recentCount + 1)
                            + " transactions in the last "
                            + VELOCITY_WINDOW_MINUTES
                            + " minutes (limit: "
                            + limit.getMaxTxnCountPerHour()
                            + ")",
                    (int) (recentCount + 1),
                    null,
                    "maxCount=" + limit.getMaxTxnCountPerHour());
        }

        // Check cumulative amount in window
        BigDecimal recentAmount =
                transactionRepository.sumRecentAmountByAccountId(tenantId, accountId, since);
        BigDecimal projectedTotal = recentAmount.add(amount);
        if (projectedTotal.compareTo(limit.getMaxTotalAmountPerHour()) > 0) {
            handleVelocityBreach(
                    tenant,
                    account,
                    userId,
                    "VELOCITY_AMOUNT",
                    "Account "
                            + account.getAccountNumber()
                            + " projected hourly total "
                            + projectedTotal
                            + " exceeds limit "
                            + limit.getMaxTotalAmountPerHour()
                            + " (existing="
                            + recentAmount
                            + " + new="
                            + amount
                            + ")",
                    null,
                    projectedTotal,
                    "maxAmount=" + limit.getMaxTotalAmountPerHour());
        }
    }

    private void handleVelocityBreach(
            Tenant tenant,
            Account account,
            Long userId,
            String alertType,
            String details,
            Integer observedCount,
            BigDecimal observedAmount,
            String thresholdValue) {

        // 1. Increment metric
        velocityBlockedCounter.increment();

        // 2. Freeze account → UNDER_REVIEW
        account.setStatus(AccountStatus.UNDER_REVIEW);
        accountRepository.save(account);

        // 3. Create FraudAlert
        FraudAlert alert =
                FraudAlert.builder()
                        .tenant(tenant)
                        .accountId(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .alertType(alertType)
                        .status("OPEN")
                        .details(details)
                        .observedCount(observedCount)
                        .observedAmount(observedAmount)
                        .thresholdValue(thresholdValue)
                        .userId(userId)
                        .build();
        alert = fraudAlertRepository.save(alert);

        // 4. Audit log
        auditService.logEvent(
                userId,
                "VELOCITY_BREACH_" + alertType,
                "FRAUD_ALERT",
                alert.getId(),
                details + " | Account frozen to UNDER_REVIEW. FraudAlert id=" + alert.getId(),
                null);

        log.warn(
                "VELOCITY BREACH: type={} account={} alertId={} details={}",
                alertType,
                account.getAccountNumber(),
                alert.getId(),
                details);

        // 5. Block transaction
        throw new GovernanceException(
                "VELOCITY_LIMIT_EXCEEDED",
                "Transaction blocked: "
                        + details
                        + ". Account "
                        + account.getAccountNumber()
                        + " has been placed under fraud review (UNDER_REVIEW). "
                        + "Contact Operations or Compliance to resolve FraudAlert #"
                        + alert.getId()
                        + ".");
    }
}
