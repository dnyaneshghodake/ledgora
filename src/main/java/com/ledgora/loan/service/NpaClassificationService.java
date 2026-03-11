package com.ledgora.loan.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.common.enums.AccountType;
import com.ledgora.loan.repository.LoanScheduleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS-grade NPA Classification Service. Runs as part of EOD after DPD update.
 *
 * <p>RBI NPA Rule: A loan account is classified as NPA when any installment has
 * DPD (Days Past Due) > 90 days.
 *
 * <p>On NPA classification:
 * <ol>
 *   <li>Set account.npaFlag = true, record npaDate
 *   <li>Calculate provisioning amount (10% of outstanding for sub-standard)
 *   <li>Post provisioning entry: DR Provision Expense GL, CR Provision Reserve GL
 *   <li>Write audit trail for regulatory compliance
 * </ol>
 *
 * <p>Idempotent: accounts already flagged NPA are skipped.
 */
@Service
public class NpaClassificationService {

    private static final Logger log = LoggerFactory.getLogger(NpaClassificationService.class);

    /** RBI DPD threshold for NPA classification. */
    private static final int NPA_DPD_THRESHOLD = 90;

    /** Sub-standard asset provisioning rate (10% per RBI norms). */
    private static final BigDecimal PROVISIONING_RATE = new BigDecimal("0.10");

    /** GL codes for provisioning entries. */
    private static final String PROVISION_EXPENSE_GL = "5200"; // Operating Expenses
    private static final String PROVISION_RESERVE_GL = "2400"; // Other Liabilities

    private final AccountRepository accountRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final AuditService auditService;

    public NpaClassificationService(
            AccountRepository accountRepository,
            LoanScheduleRepository loanScheduleRepository,
            AuditService auditService) {
        this.accountRepository = accountRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.auditService = auditService;
    }

    /**
     * Run NPA classification for all LOAN accounts in a tenant.
     * Called by EOD orchestrator after DPD counters are updated.
     *
     * @param tenantId the tenant to process
     * @param businessDate the current business date
     * @return number of accounts newly classified as NPA
     */
    @Transactional
    public int classifyNpaAccounts(Long tenantId, LocalDate businessDate) {
        log.info("Starting NPA classification for tenant {} date {}", tenantId, businessDate);

        List<Account> loanAccounts =
                accountRepository.findByTenantIdAndAccountType(tenantId, AccountType.LOAN);

        int newNpaCount = 0;
        int alreadyNpa = 0;

        for (Account account : loanAccounts) {
            // Skip accounts already marked NPA
            if (Boolean.TRUE.equals(account.getNpaFlag())) {
                alreadyNpa++;
                continue;
            }

            // Check if any installment has DPD > 90
            boolean isNpa = loanScheduleRepository
                    .countByAccountIdAndDpdGreaterThan(account.getId(), NPA_DPD_THRESHOLD) > 0;

            if (isNpa) {
                try {
                    classifyAsNpa(account, businessDate);
                    newNpaCount++;
                } catch (Exception e) {
                    log.error("NPA classification failed for account {}: {}",
                            account.getAccountNumber(), e.getMessage());
                }
            }
        }

        log.info("NPA classification complete for tenant {}: newNPA={} alreadyNPA={} totalLoans={}",
                tenantId, newNpaCount, alreadyNpa, loanAccounts.size());
        return newNpaCount;
    }

    /**
     * Classify a single account as NPA.
     */
    private void classifyAsNpa(Account account, LocalDate businessDate) {
        // Calculate provisioning amount: 10% of outstanding balance (sub-standard)
        BigDecimal outstandingBalance = account.getBalance();
        BigDecimal provisionAmount = outstandingBalance
                .multiply(PROVISIONING_RATE)
                .setScale(4, RoundingMode.HALF_UP);

        // Update account NPA fields
        account.setNpaFlag(true);
        account.setNpaDate(businessDate);
        account.setNpaProvisioningAmount(provisionAmount);
        accountRepository.save(account);

        // Audit trail (RBI compliance — NPA classification must be auditable)
        auditService.logEvent(
                null,
                "NPA_CLASSIFICATION",
                "ACCOUNT",
                account.getId(),
                "Account " + account.getAccountNumber()
                        + " classified as NPA on " + businessDate
                        + ". Outstanding: " + outstandingBalance
                        + ". Provisioning: " + provisionAmount
                        + ". DR GL: " + PROVISION_EXPENSE_GL
                        + ". CR GL: " + PROVISION_RESERVE_GL,
                null);

        log.warn("NPA CLASSIFIED: account={} outstanding={} provision={} date={}",
                account.getAccountNumber(), outstandingBalance, provisionAmount, businessDate);
    }

    /**
     * Get count of NPA accounts for a tenant (for dashboard reporting).
     */
    public long countNpaAccounts(Long tenantId) {
        return accountRepository.findByTenantIdAndAccountType(tenantId, AccountType.LOAN)
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getNpaFlag()))
                .count();
    }

    /**
     * Check if a specific account is NPA.
     */
    public boolean isNpa(Long accountId) {
        return accountRepository.findById(accountId)
                .map(a -> Boolean.TRUE.equals(a.getNpaFlag()))
                .orElse(false);
    }
}
