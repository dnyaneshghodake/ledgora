package com.ledgora.loan.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loan Write-Off Service — removes unrecoverable loans from the asset book.
 *
 * <p>RBI Master Circular on Prudential Norms — Write-Off:
 *
 * <ul>
 *   <li>Only LOSS-classified loans may be written off
 *   <li>Provision must cover 100% of outstanding before write-off
 *   <li>Write-off removes the loan from active asset GL
 *   <li>Recovery after write-off is possible (posted as income)
 * </ul>
 *
 * <p>Accounting entry:
 *
 * <pre>
 *   DR Loan Provision GL (reduces provision — contra liability)
 *   CR Loan Asset GL (removes loan from asset book)
 * </pre>
 *
 * <p>After write-off: status → WRITTEN_OFF, outstanding → 0, provision → 0.
 */
@Service
public class LoanWriteOffService {

    private static final Logger log = LoggerFactory.getLogger(LoanWriteOffService.class);

    private final LoanAccountRepository loanAccountRepository;
    private final TenantService tenantService;
    private final AuditService auditService;

    public LoanWriteOffService(
            LoanAccountRepository loanAccountRepository,
            TenantService tenantService,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.tenantService = tenantService;
        this.auditService = auditService;
    }

    /**
     * Write off a loan — removes from asset book.
     *
     * <p>Pre-conditions:
     *
     * <ul>
     *   <li>Loan must be NPA with LOSS classification
     *   <li>Provision must cover 100% of outstanding
     * </ul>
     *
     * <p>NOTE: The actual GL posting (DR Provision GL, CR Loan Asset GL) should be done via the
     * voucher engine by the caller.
     */
    @Transactional
    public LoanAccount writeOff(Long loanAccountId) {
        LoanAccount loan =
                loanAccountRepository
                        .findById(loanAccountId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LOAN_NOT_FOUND",
                                                "Loan account not found: " + loanAccountId));

        // Tenant isolation: verify loan belongs to current tenant
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null
                && (loan.getTenant() == null
                        || !loan.getTenant().getId().equals(tenantId))) {
            throw new BusinessException(
                    "LOAN_NOT_FOUND",
                    "Loan account not found: " + loanAccountId);
        }

        // CBS Tier-1: validate business day is OPEN before financial operations
        Long effectiveTenantId =
                tenantId != null ? tenantId : loan.getTenant().getId();
        tenantService.validateBusinessDayOpen(effectiveTenantId);

        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new BusinessException("LOAN_CLOSED", "Cannot write off a closed loan");
        }
        if (loan.getStatus() == LoanStatus.WRITTEN_OFF) {
            throw new BusinessException(
                    "LOAN_ALREADY_WRITTEN_OFF", "Loan is already written off");
        }
        if (loan.getStatus() != LoanStatus.NPA) {
            throw new BusinessException(
                    "LOAN_NOT_NPA",
                    "Only NPA loans can be written off. Current status: " + loan.getStatus());
        }
        if (loan.getNpaClassification() != NpaClassification.LOSS) {
            throw new BusinessException(
                    "LOAN_NOT_LOSS",
                    "Only LOSS-classified loans can be written off. Current: "
                            + loan.getNpaClassification());
        }

        // Verify provision covers outstanding
        if (loan.getProvisionAmount().compareTo(loan.getOutstandingPrincipal()) < 0) {
            throw new BusinessException(
                    "INSUFFICIENT_PROVISION",
                    "Provision ("
                            + loan.getProvisionAmount()
                            + ") must cover outstanding ("
                            + loan.getOutstandingPrincipal()
                            + ") before write-off");
        }

        BigDecimal writtenOffAmount = loan.getOutstandingPrincipal();

        // Zero out the loan
        loan.setOutstandingPrincipal(BigDecimal.ZERO);
        loan.setAccruedInterest(BigDecimal.ZERO);
        loan.setProvisionAmount(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.WRITTEN_OFF);

        loan = loanAccountRepository.save(loan);

        auditService.logEvent(
                null,
                "LOAN_WRITTEN_OFF",
                "LOAN_ACCOUNT",
                loan.getId(),
                "Loan "
                        + loan.getLoanAccountNumber()
                        + " written off. Amount="
                        + writtenOffAmount
                        + " (100% provisioned, removed from asset book)",
                null);

        log.info(
                "LOAN WRITE-OFF: {} amount={} (removed from asset book)",
                loan.getLoanAccountNumber(),
                writtenOffAmount);

        return loan;
    }
}
