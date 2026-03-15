package com.ledgora.loan.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.common.enums.InstallmentStatus;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.entity.LoanSchedule;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanScheduleRepository;
import com.ledgora.tenant.service.TenantService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI IRAC — NPA Classification and DPD Update Service.
 *
 * <p>RBI Master Circular on Prudential Norms on Income Recognition, Asset Classification and
 * Provisioning:
 *
 * <ul>
 *   <li>DPD updated daily for all active loans with overdue installments
 *   <li>NPA classification triggered when DPD > product.npaDaysThreshold (default 90)
 *   <li>On NPA: loan asset reclassified from Standard GL to NPA GL
 *   <li>Interest recognition STOPS for NPA loans
 *   <li>NPA is irreversible without explicit admin override (upgrade requires RBI criteria)
 * </ul>
 *
 * <p>Accounting for NPA reclassification (via voucher engine):
 *
 * <pre>
 *   DR NPA Loan Asset GL
 *   CR Standard Loan Asset GL
 * </pre>
 *
 * <p>Called during EOD Phase VALIDATED, after accrual and before provisioning.
 */
@Service
public class LoanNpaService {

    private static final Logger log = LoggerFactory.getLogger(LoanNpaService.class);

    private final LoanAccountRepository loanAccountRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final TenantService tenantService;
    private final AuditService auditService;

    public LoanNpaService(
            LoanAccountRepository loanAccountRepository,
            LoanScheduleRepository loanScheduleRepository,
            TenantService tenantService,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.tenantService = tenantService;
        this.auditService = auditService;
    }

    /**
     * Update DPD and evaluate NPA classification for all active loans.
     *
     * @return number of loans newly classified as NPA
     */
    @Transactional
    public int evaluateNpaAndUpdateDpd(Long tenantId) {
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);
        var activeLoans = loanAccountRepository.findActiveByTenantId(tenantId);
        int newNpaCount = 0;

        for (LoanAccount loan : activeLoans) {
            if (loan.getStatus() != LoanStatus.ACTIVE) {
                continue;
            }

            // RBI IRAC DPD: compute from oldest overdue installment in the schedule.
            // Mark SCHEDULED installments past due date as OVERDUE, then compute DPD.
            List<LoanSchedule> pendingInstallments =
                    loanScheduleRepository.findPendingByLoanAccountIdOrderByInstallmentAsc(
                            loan.getId());

            LocalDate oldestOverdueDate = null;
            for (LoanSchedule inst : pendingInstallments) {
                if (inst.getDueDate().isBefore(businessDate)
                        && inst.getStatus() != InstallmentStatus.OVERDUE) {
                    // Transition SCHEDULED/DUE → OVERDUE
                    inst.setStatus(InstallmentStatus.OVERDUE);
                    inst.setDpdDays(
                            (int) ChronoUnit.DAYS.between(inst.getDueDate(), businessDate));
                    loanScheduleRepository.save(inst);
                } else if (inst.getStatus() == InstallmentStatus.OVERDUE) {
                    // Update DPD on already-overdue installments
                    inst.setDpdDays(
                            (int) ChronoUnit.DAYS.between(inst.getDueDate(), businessDate));
                    loanScheduleRepository.save(inst);
                }
                // Track oldest overdue date
                if ((inst.getStatus() == InstallmentStatus.OVERDUE
                                || inst.getDueDate().isBefore(businessDate))
                        && (oldestOverdueDate == null
                                || inst.getDueDate().isBefore(oldestOverdueDate))) {
                    oldestOverdueDate = inst.getDueDate();
                }
            }

            // Compute loan-level DPD from oldest overdue installment
            int computedDpd =
                    oldestOverdueDate != null
                            ? (int) ChronoUnit.DAYS.between(oldestOverdueDate, businessDate)
                            : 0;
            loan.setDpd(computedDpd);

            LoanProduct product = loan.getLoanProduct();
            int threshold = product.getNpaDaysThreshold();

            if (computedDpd > threshold && loan.getStatus() == LoanStatus.ACTIVE) {
                // ── NPA CLASSIFICATION ──
                loan.setStatus(LoanStatus.NPA);
                loan.setNpaDate(businessDate);
                loan.setNpaClassification(NpaClassification.SUBSTANDARD);

                // NOTE: The GL reclassification (DR NPA GL, CR Standard GL)
                // should be done via voucher engine in production:
                //   VoucherService.createVoucherPair(
                //       product.getGlNpaLoanAsset(),  // DR
                //       product.getGlLoanAsset(),     // CR
                //       loan.getOutstandingPrincipal(), ...)

                loanAccountRepository.save(loan);
                newNpaCount++;

                auditService.logEvent(
                        null,
                        "LOAN_NPA_CLASSIFIED",
                        "LOAN_ACCOUNT",
                        loan.getId(),
                        "Loan "
                                + loan.getLoanAccountNumber()
                                + " classified as NPA. DPD="
                                + loan.getDpd()
                                + " threshold="
                                + threshold
                                + " outstanding="
                                + loan.getOutstandingPrincipal(),
                        null);

                log.warn(
                        "LOAN NPA: {} classified as NPA (DPD={}, threshold={})",
                        loan.getLoanAccountNumber(),
                        loan.getDpd(),
                        threshold);
            } else {
                // Save DPD update even if NPA threshold not yet breached
                loanAccountRepository.save(loan);
            }
        }

        if (newNpaCount > 0) {
            log.info(
                    "NPA evaluation: {} new NPA classifications for tenant {}",
                    newNpaCount,
                    tenantId);
        }

        return newNpaCount;
    }
}
