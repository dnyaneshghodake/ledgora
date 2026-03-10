package com.ledgora.balance.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.balance.entity.BalanceDriftAlert;
import com.ledgora.balance.repository.BalanceDriftAlertRepository;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled reconciliation service: detects drift between the account balance cache and the
 * authoritative ledger.
 *
 * <p>RBI IT Framework — Data Validation / Internal Audit Reconciliation Standard:
 *
 * <ul>
 *   <li>Runs every 15 minutes via {@code @Scheduled}
 *   <li>Authoritative balance = SUM(credits) - SUM(debits) from ledger_entries per account
 *   <li>Compares with Account.balance (performance cache)
 *   <li>On mismatch: creates BalanceDriftAlert, logs CRITICAL audit entry, increments metric
 *   <li>Multi-tenant: iterates all tenants independently
 *   <li>Scales via pagination (configurable page size)
 * </ul>
 */
@Service
public class AccountBalanceReconciliationService {

    private static final Logger log =
            LoggerFactory.getLogger(AccountBalanceReconciliationService.class);

    /** Page size for account iteration — tune for DB performance. */
    private static final int PAGE_SIZE = 100;

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BalanceDriftAlertRepository driftAlertRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;
    private final Counter driftCounter;

    public AccountBalanceReconciliationService(
            AccountRepository accountRepository,
            LedgerEntryRepository ledgerEntryRepository,
            BalanceDriftAlertRepository driftAlertRepository,
            TenantRepository tenantRepository,
            AuditService auditService,
            MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.driftAlertRepository = driftAlertRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
        this.driftCounter =
                Counter.builder("ledgora.balance.drift")
                        .description("Balance drift alerts detected between cache and ledger")
                        .register(meterRegistry);
    }

    /**
     * Scheduled reconciliation: runs every 15 minutes. Iterates all tenants, paginates accounts,
     * compares cache vs ledger for each.
     */
    @Scheduled(fixedDelayString = "${ledgora.reconciliation.interval-ms:900000}")
    public void reconcileAll() {
        log.info("Balance reconciliation started");
        long startTime = System.currentTimeMillis();
        int totalDrifts = 0;
        int totalAccounts = 0;

        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            int tenantDrifts = reconcileTenant(tenant);
            totalDrifts += tenantDrifts;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info(
                "Balance reconciliation completed in {}ms: {} drift(s) detected across all tenants",
                elapsed,
                totalDrifts);
    }

    /**
     * Reconcile all accounts for a single tenant. Uses pagination to scale.
     *
     * @return number of drifts detected
     */
    public int reconcileTenant(Tenant tenant) {
        Long tenantId = tenant.getId();
        int driftCount = 0;
        int pageNumber = 0;

        Page<Account> page;
        do {
            page =
                    accountRepository.findByTenantId(
                            tenantId, PageRequest.of(pageNumber, PAGE_SIZE));

            for (Account account : page.getContent()) {
                boolean drifted = reconcileAccount(tenant, account);
                if (drifted) {
                    driftCount++;
                }
            }

            pageNumber++;
        } while (page.hasNext());

        if (driftCount > 0) {
            log.warn(
                    "Tenant {} reconciliation: {} drift(s) detected across {} accounts",
                    tenantId,
                    driftCount,
                    page.getTotalElements());
        }

        return driftCount;
    }

    /**
     * Reconcile a single account: compare cache vs ledger.
     *
     * @return true if drift detected
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reconcileAccount(Tenant tenant, Account account) {
        BigDecimal cachedBalance =
                account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;

        // Authoritative balance from ledger: SUM(credits) - SUM(debits)
        BigDecimal totalCredits = ledgerEntryRepository.sumCreditsByAccountId(account.getId());
        BigDecimal totalDebits = ledgerEntryRepository.sumDebitsByAccountId(account.getId());
        BigDecimal ledgerBalance = totalCredits.subtract(totalDebits);

        if (cachedBalance.compareTo(ledgerBalance) != 0) {
            BigDecimal drift = cachedBalance.subtract(ledgerBalance);

            // Avoid duplicate alerts for the same account
            if (driftAlertRepository.existsByAccountIdAndStatus(account.getId(), "OPEN")) {
                return true; // Already alerted
            }

            // 1. Create BalanceDriftAlert
            BalanceDriftAlert alert =
                    BalanceDriftAlert.builder()
                            .tenant(tenant)
                            .accountId(account.getId())
                            .accountNumber(account.getAccountNumber())
                            .cachedBalance(cachedBalance)
                            .ledgerBalance(ledgerBalance)
                            .driftAmount(drift)
                            .status("OPEN")
                            .build();
            alert = driftAlertRepository.save(alert);

            // 2. Increment metric
            driftCounter.increment();

            // 3. CRITICAL audit log
            auditService.logEvent(
                    null,
                    "BALANCE_DRIFT_DETECTED",
                    "BALANCE_DRIFT_ALERT",
                    alert.getId(),
                    "CRITICAL: Balance drift on account "
                            + account.getAccountNumber()
                            + " — cached="
                            + cachedBalance
                            + " ledger="
                            + ledgerBalance
                            + " drift="
                            + drift
                            + " tenant="
                            + tenant.getId(),
                    null);

            log.error(
                    "BALANCE DRIFT: account={} cached={} ledger={} drift={} alertId={}",
                    account.getAccountNumber(),
                    cachedBalance,
                    ledgerBalance,
                    drift,
                    alert.getId());

            return true;
        }

        return false;
    }

    /** Get all open drift alerts for a tenant (for dashboard). */
    public List<BalanceDriftAlert> getOpenAlerts(Long tenantId) {
        return driftAlertRepository.findByTenantIdAndStatus(tenantId, "OPEN");
    }

    /** Count open drift alerts for a tenant. */
    public long countOpenAlerts(Long tenantId) {
        return driftAlertRepository.countOpenByTenantId(tenantId);
    }

    /** Get all drift alerts for a tenant (for audit history). */
    public List<BalanceDriftAlert> getAllAlerts(Long tenantId) {
        return driftAlertRepository.findByTenantIdOrderByDetectedAtDesc(tenantId);
    }
}
