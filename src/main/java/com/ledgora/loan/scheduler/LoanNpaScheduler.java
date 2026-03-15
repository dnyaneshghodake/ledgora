package com.ledgora.loan.scheduler;

import com.ledgora.loan.service.LoanEmiPaymentService;
import com.ledgora.loan.service.LoanNpaService;
import com.ledgora.loan.service.LoanProvisionService;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Loan NPA Scheduler — invoked during EOD Phase VALIDATED, after accrual.
 *
 * <p>EOD integration order per CBS/Finacle:
 *
 * <ol>
 *   <li>Interest Accrual (LoanAccrualScheduler)
 *   <li><strong>DPD + NPA classification</strong> (this scheduler — step 1)
 *   <li><strong>Penal interest accrual</strong> (this scheduler — step 2)
 *   <li><strong>Provision update</strong> (this scheduler — step 3)
 *   <li>Trial Balance / CRAR (reporting engine)
 * </ol>
 *
 * <p>NPA evaluation per RBI IRAC:
 *
 * <ul>
 *   <li>DPD computed from oldest overdue installment in the schedule
 *   <li>SCHEDULED/DUE installments past due date → OVERDUE
 *   <li>DPD > 90 → NPA (SUBSTANDARD/DOUBTFUL/LOSS based on age)
 *   <li>Provision rates: STANDARD 0.4%, SUBSTANDARD 15%, DOUBTFUL 25%, LOSS 100%
 * </ul>
 */
@Component
public class LoanNpaScheduler {

    private static final Logger log = LoggerFactory.getLogger(LoanNpaScheduler.class);

    private final LoanNpaService loanNpaService;
    private final LoanEmiPaymentService loanEmiPaymentService;
    private final LoanProvisionService loanProvisionService;
    private final TenantRepository tenantRepository;

    public LoanNpaScheduler(
            LoanNpaService loanNpaService,
            LoanEmiPaymentService loanEmiPaymentService,
            LoanProvisionService loanProvisionService,
            TenantRepository tenantRepository) {
        this.loanNpaService = loanNpaService;
        this.loanEmiPaymentService = loanEmiPaymentService;
        this.loanProvisionService = loanProvisionService;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Run NPA evaluation + provision calculation for a specific tenant.
     *
     * <p>Called by EodStateMachineService during Phase VALIDATED, after accrual.
     *
     * @param tenantId the tenant to process
     */
    public void runForTenant(Long tenantId) {
        log.info("Loan NPA scheduler started for tenant {}", tenantId);
        try {
            // Step 1: DPD update + SMA/NPA classification + interest reversal + NPA upgrade
            int newNpaCount = loanNpaService.evaluateNpaAndUpdateDpd(tenantId);
            log.info("NPA evaluation: tenant={} newNpaClassifications={}", tenantId, newNpaCount);

            // Step 2: Penal interest accrual (after DPD is computed, respects grace days)
            int penalized = loanEmiPaymentService.accruePenalInterest(tenantId);
            log.info("Penal accrual: tenant={} loansWithPenal={}", tenantId, penalized);

            // Step 3: Provision recalculation (must run AFTER NPA classification)
            BigDecimal incremental = loanProvisionService.calculateProvisions(tenantId);
            log.info(
                    "Provision update: tenant={} incrementalProvision={}",
                    tenantId,
                    incremental);

            log.info("Loan NPA scheduler completed for tenant {}", tenantId);
        } catch (Exception e) {
            log.error(
                    "Loan NPA scheduler failed for tenant {}: {}", tenantId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Run NPA evaluation + provision for all active tenants.
     *
     * <p>Used for batch EOD processing across all tenants.
     */
    public void runForAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            try {
                runForTenant(tenant.getId());
            } catch (Exception e) {
                log.error(
                        "NPA scheduler failed for tenant {} ({}): {}",
                        tenant.getId(),
                        tenant.getTenantCode(),
                        e.getMessage());
                // Continue with other tenants
            }
        }
        log.info("Loan NPA scheduler completed for all tenants");
    }
}
