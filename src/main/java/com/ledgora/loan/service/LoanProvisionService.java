package com.ledgora.loan.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProvision;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanProvisionRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final LoanProvisionRepository loanProvisionRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public LoanProvisionService(
            LoanAccountRepository loanAccountRepository,
            LoanProvisionRepository loanProvisionRepository,
            TenantRepository tenantRepository,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanProvisionRepository = loanProvisionRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    /**
     * Calculate and update provisions for all active + NPA loans.
     *
     * @return total incremental provision amount posted
     */
    @Transactional
    public BigDecimal calculateProvisions(Long tenantId) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        LocalDate businessDate = tenant.getCurrentBusinessDate();

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

            // CBS/RBI IRAC: Provision must be adjusted BOTH upward AND downward.
            // Upward: NPA tier progression (STANDARD→SUBSTANDARD) increases provision.
            // Downward: NPA upgrade (SUBSTANDARD→STANDARD after full clearance) or
            // principal repayment reduces the required provision. Excess provision
            // must be released to avoid overstating the provision expense.
            if (incremental.compareTo(BigDecimal.ZERO) != 0) {
                loan.setProvisionAmount(required);
                loanAccountRepository.save(loan);

                // ── CBS AUDIT: Record immutable LoanProvision snapshot ──
                loanProvisionRepository.save(
                        LoanProvision.builder()
                                .tenant(tenant)
                                .loanAccount(loan)
                                .businessDate(businessDate)
                                .npaClassification(classification)
                                .provisionRate(rate)
                                .outstandingPrincipal(loan.getOutstandingPrincipal())
                                .requiredProvision(required)
                                .previousProvision(existing)
                                .incrementalProvision(incremental)
                                .build());

                totalIncremental = totalIncremental.add(incremental);
                updated++;

                log.debug(
                        "Provision {}: loan={} classification={} rate={}% "
                                + "required={} existing={} change={}",
                        incremental.compareTo(BigDecimal.ZERO) > 0 ? "increased" : "released",
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
