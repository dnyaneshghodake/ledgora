package com.ledgora.loan.scheduler;

import com.ledgora.loan.service.LoanAccrualService;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Loan Accrual Scheduler — invoked during EOD Phase VALIDATED.
 *
 * <p>EOD integration order per CBS/Finacle:
 *
 * <ol>
 *   <li><strong>Interest Accrual</strong> (this scheduler)
 *   <li>NPA classification (LoanNpaScheduler)
 *   <li>Provision update (LoanNpaScheduler)
 *   <li>Trial Balance / CRAR (reporting engine)
 * </ol>
 *
 * <p>Accrual runs for all active tenants. Each tenant's performing loans are accrued using the
 * tenant's current business date. Idempotent — loans already accrued for the date are skipped.
 *
 * <p>Accounting entry per loan (via voucher engine):
 *
 * <pre>
 *   DR Interest Receivable GL (Asset)
 *   CR Interest Income GL (Revenue)
 * </pre>
 */
@Component
public class LoanAccrualScheduler {

    private static final Logger log = LoggerFactory.getLogger(LoanAccrualScheduler.class);

    private final LoanAccrualService loanAccrualService;
    private final TenantRepository tenantRepository;

    public LoanAccrualScheduler(
            LoanAccrualService loanAccrualService, TenantRepository tenantRepository) {
        this.loanAccrualService = loanAccrualService;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Run daily interest accrual for a specific tenant.
     *
     * <p>Called by EodStateMachineService during Phase VALIDATED.
     *
     * @param tenantId the tenant to process
     * @return number of loans accrued
     */
    public int runForTenant(Long tenantId) {
        log.info("Loan accrual scheduler started for tenant {}", tenantId);
        try {
            int accrued = loanAccrualService.accrueDailyInterest(tenantId);
            log.info(
                    "Loan accrual scheduler completed: tenant={} loansAccrued={}",
                    tenantId,
                    accrued);
            return accrued;
        } catch (Exception e) {
            log.error(
                    "Loan accrual scheduler failed for tenant {}: {}", tenantId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Run daily interest accrual for all active tenants.
     *
     * <p>Used for batch EOD processing across all tenants.
     */
    public void runForAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        int totalAccrued = 0;
        for (Tenant tenant : tenants) {
            try {
                totalAccrued += runForTenant(tenant.getId());
            } catch (Exception e) {
                log.error(
                        "Loan accrual failed for tenant {} ({}): {}",
                        tenant.getId(),
                        tenant.getTenantCode(),
                        e.getMessage());
                // Continue with other tenants — don't let one failure block all
            }
        }
        log.info("Loan accrual scheduler completed for all tenants: totalAccrued={}", totalAccrued);
    }
}
