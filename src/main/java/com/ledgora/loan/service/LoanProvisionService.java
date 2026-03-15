package com.ledgora.loan.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.repository.LoanAccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI Provisioning Service — calculates and posts loan loss provisions.
 *
 * <p>RBI Master Circular on Prudential Norms — Provisioning Requirements:
 *
 * <ul>
 *   <li>STANDARD: 0.40% of outstanding principal
 *   <li>SUBSTANDARD: 15% (25% if unsecured)
 *   <li>DOUBTFUL: 25% to 100% depending on age
 *   <li>LOSS: 100%
 * </ul>
 *
 * <p>Accounting entry (via voucher engine):
 *
 * <pre>
 *   DR Provision Expense GL (Expense — P&L impact)
 *   CR Loan Provision GL (Liability — Balance Sheet)
 * </pre>
 *
 * <p>Incremental provisioning: only the DIFFERENCE between required and existing provision is
 * posted. This prevents double-counting.
 *
 * <p>Called during EOD Phase VALIDATED, after NPA evaluation and before statement snapshot.
 */
@Service
public class LoanProvisionService {

    private static final Logger log = LoggerFactory.getLogger(LoanProvisionService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final LoanAccountRepository loanAccountRepository;
    private final AuditService auditService;

    public LoanProvisionService(
            LoanAccountRepository loanAccountRepository, AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.auditService = auditService;
    }

    /**
     * Calculate and update provisions for all active + NPA loans.
     *
     * @return total incremental provision amount posted
     */
    @Transactional
    public BigDecimal calculateProvisions(Long tenantId) {
        var loans = loanAccountRepository.findActiveAndNpaByTenantId(tenantId);
        BigDecimal totalIncremental = BigDecimal.ZERO;
        int updated = 0;

        for (LoanAccount loan : loans) {
            NpaClassification classification = loan.getNpaClassification();
            BigDecimal rate = classification.getProvisionRate();

            // Required provision = outstanding × rate / 100
            BigDecimal required =
                    loan.getOutstandingPrincipal()
                            .multiply(rate)
                            .divide(HUNDRED, 4, RoundingMode.HALF_UP);

            BigDecimal existing = loan.getProvisionAmount();
            BigDecimal incremental = required.subtract(existing);

            if (incremental.compareTo(BigDecimal.ZERO) > 0) {
                // Incremental provision needed
                // NOTE: The GL posting (DR Provision Expense, CR Loan Provision GL)
                // should be done via voucher engine in production:
                //   LoanProduct product = loan.getLoanProduct();
                //   VoucherService.createVoucherPair(
                //       product.getGlProvision(),  // DR Expense
                //       product.getGlProvision(),  // CR Liability (separate GL)
                //       incremental, ...)

                loan.setProvisionAmount(required);
                loanAccountRepository.save(loan);

                totalIncremental = totalIncremental.add(incremental);
                updated++;

                log.debug(
                        "Provision updated: loan={} classification={} rate={}% "
                                + "required={} existing={} incremental={}",
                        loan.getLoanAccountNumber(),
                        classification,
                        rate,
                        required,
                        existing,
                        incremental);
            }
        }

        if (updated > 0) {
            auditService.logEvent(
                    null,
                    "LOAN_PROVISIONING",
                    "LOAN_BATCH",
                    null,
                    "Provisions updated for "
                            + updated
                            + " loans (tenant "
                            + tenantId
                            + "). Incremental="
                            + totalIncremental,
                    null);
            log.info(
                    "Loan provisioning: {} loans updated, incremental={} for tenant {}",
                    updated,
                    totalIncremental,
                    tenantId);
        }

        return totalIncremental;
    }
}
