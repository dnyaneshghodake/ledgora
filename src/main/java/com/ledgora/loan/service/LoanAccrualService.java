package com.ledgora.loan.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.repository.LoanAccountRepository;
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
 * RBI-compliant daily interest accrual for performing loans.
 *
 * <p>RBI Master Circular on Income Recognition (IRAC):
 *
 * <ul>
 *   <li>Interest on ACTIVE (performing) loans accrued on accrual basis
 *   <li>Interest on NPA loans: recognition STOPS — no accrual entry
 *   <li>Daily interest = outstandingPrincipal × (annualRate / 365)
 * </ul>
 *
 * <p>Accounting entry (via voucher engine — NOT direct balance mutation):
 *
 * <pre>
 *   DR Interest Receivable GL (Asset)
 *   CR Interest Income GL (Revenue)
 * </pre>
 *
 * <p>Called during EOD Phase VALIDATED, BEFORE statement snapshot generation.
 */
@Service
public class LoanAccrualService {

    private static final Logger log = LoggerFactory.getLogger(LoanAccrualService.class);
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private final LoanAccountRepository loanAccountRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public LoanAccrualService(
            LoanAccountRepository loanAccountRepository,
            TenantRepository tenantRepository,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    /**
     * Accrue daily interest for all performing loans of a tenant.
     *
     * <p>RBI IRAC: Only ACTIVE loans accrue interest. NPA/WRITTEN_OFF/CLOSED are skipped.
     *
     * @return number of loans accrued
     */
    @Transactional
    public int accrueDailyInterest(Long tenantId) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        LocalDate businessDate = tenant.getCurrentBusinessDate();

        var activeLoans = loanAccountRepository.findActiveByTenantId(tenantId);
        int accrued = 0;
        int skippedAlreadyAccrued = 0;

        for (LoanAccount loan : activeLoans) {
            if (loan.getStatus() != LoanStatus.ACTIVE) {
                continue; // defensive — query should only return ACTIVE
            }
            if (loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
                continue; // fully repaid
            }

            // Idempotency: skip if already accrued for this business date
            if (businessDate.equals(loan.getLastAccrualDate())) {
                skippedAlreadyAccrued++;
                continue;
            }

            LoanProduct product = loan.getLoanProduct();
            BigDecimal dailyRate =
                    product.getInterestRate()
                            .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                            .divide(DAYS_IN_YEAR, 10, RoundingMode.HALF_UP);

            BigDecimal dailyInterest =
                    loan.getOutstandingPrincipal()
                            .multiply(dailyRate, MathContext.DECIMAL128)
                            .setScale(4, RoundingMode.HALF_UP);

            if (dailyInterest.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Update accrued interest on the loan account
            // NOTE: The actual GL posting (DR Interest Receivable, CR Interest Income)
            // should be done via the voucher engine in a production implementation.
            // Here we update the tracking field; the voucher posting would be:
            //   VoucherService.createVoucherPair(
            //       product.getGlInterestReceivable(), // DR
            //       product.getGlInterestIncome(),     // CR
            //       dailyInterest, ...)
            loan.setAccruedInterest(loan.getAccruedInterest().add(dailyInterest));
            loan.setLastAccrualDate(businessDate);
            loanAccountRepository.save(loan);

            accrued++;
            log.debug(
                    "Interest accrued: loan={} principal={} rate={}% daily={} total_accrued={}",
                    loan.getLoanAccountNumber(),
                    loan.getOutstandingPrincipal(),
                    product.getInterestRate(),
                    dailyInterest,
                    loan.getAccruedInterest());
        }

        if (skippedAlreadyAccrued > 0) {
            log.info(
                    "Loan accrual idempotency: skipped {} loans already accrued for date {} (tenant {})",
                    skippedAlreadyAccrued,
                    businessDate,
                    tenantId);
        }

        if (accrued > 0) {
            auditService.logEvent(
                    null,
                    "LOAN_INTEREST_ACCRUAL",
                    "LOAN_BATCH",
                    null,
                    "Daily interest accrued for "
                            + accrued
                            + " loans (tenant "
                            + tenantId
                            + ") date "
                            + businessDate,
                    null);
            log.info("Loan interest accrued: {} loans for tenant {}", accrued, tenantId);
        }

        return accrued;
    }
}
