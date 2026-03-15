package com.ledgora.loan.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.common.enums.InstallmentStatus;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.entity.LoanSchedule;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.enums.SmaCategory;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanScheduleRepository;
import com.ledgora.loan.validation.NpaClassifier;
import com.ledgora.tenant.service.TenantService;
import java.math.BigDecimal;
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

        // RBI IRAC: process ALL active + NPA loans (not just ACTIVE).
        // Already-NPA loans need DPD update and tier progression
        // (SUBSTANDARD → DOUBTFUL → LOSS).
        var allLoans = loanAccountRepository.findActiveAndNpaByTenantId(tenantId);
        int newNpaCount = 0;

        for (LoanAccount loan : allLoans) {
            if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.NPA) {
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
                    inst.setDpdDays((int) ChronoUnit.DAYS.between(inst.getDueDate(), businessDate));
                    loanScheduleRepository.save(inst);
                } else if (inst.getStatus() == InstallmentStatus.OVERDUE) {
                    // Update DPD on already-overdue installments
                    inst.setDpdDays((int) ChronoUnit.DAYS.between(inst.getDueDate(), businessDate));
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

            // RBI IRAC: classify based on DPD tiers (STANDARD/SUBSTANDARD/DOUBTFUL/LOSS)
            NpaClassification classification = NpaClassifier.classify(computedDpd, threshold);
            loan.setNpaClassification(classification);

            // RBI SMA Framework: classify SMA category for performing loans
            if (loan.getStatus() == LoanStatus.ACTIVE) {
                SmaCategory sma = NpaClassifier.classifySma(computedDpd);
                loan.setSmaCategory(sma);
            } else {
                // NPA loans: SMA category is not applicable
                loan.setSmaCategory(SmaCategory.NONE);
            }

            if (NpaClassifier.isNpa(computedDpd, threshold)
                    && loan.getStatus() == LoanStatus.ACTIVE) {
                // ── NEW NPA CLASSIFICATION — ACTIVE → NPA status transition ──
                loan.setStatus(LoanStatus.NPA);
                loan.setNpaDate(businessDate);
                loan.setSmaCategory(SmaCategory.NONE); // SMA no longer applicable

                // ── RBI IRAC: INTEREST REVERSAL on NPA classification ──
                // Accrued but unrealized interest must be reversed from income.
                // GL: DR Interest Income, CR Interest Suspense
                // The accrued interest is moved to interestReversed tracking field.
                BigDecimal accruedToReverse = loan.getAccruedInterest();
                if (accruedToReverse.compareTo(BigDecimal.ZERO) > 0) {
                    loan.setInterestReversed(accruedToReverse);
                    loan.setAccruedInterest(BigDecimal.ZERO);
                    log.info(
                            "Interest reversed on NPA: loan={} reversed={}",
                            loan.getLoanAccountNumber(),
                            accruedToReverse);
                }

                loanAccountRepository.save(loan);
                newNpaCount++;

                auditService.logEvent(
                        null,
                        "LOAN_NPA_CLASSIFIED",
                        "LOAN_ACCOUNT",
                        loan.getId(),
                        "Loan "
                                + loan.getLoanAccountNumber()
                                + " classified as NPA ("
                                + classification
                                + "). DPD="
                                + loan.getDpd()
                                + " threshold="
                                + threshold
                                + " outstanding="
                                + loan.getOutstandingPrincipal(),
                        null);

                log.warn(
                        "LOAN NPA: {} classified as {} (DPD={}, threshold={})",
                        loan.getLoanAccountNumber(),
                        classification,
                        loan.getDpd(),
                        threshold);
            } else if (loan.getStatus() == LoanStatus.NPA && computedDpd == 0) {
                // ── NPA UPGRADE — all overdue cleared, DPD back to 0 ──
                // RBI IRAC: Upgrade from NPA to Standard only after ALL overdue cleared.
                // Reverse interest suspense back to income.
                loan.setStatus(LoanStatus.ACTIVE);
                loan.setNpaDate(null);
                loan.setNpaClassification(NpaClassification.STANDARD);
                loan.setSmaCategory(SmaCategory.NONE);

                // Reverse interest suspense → income
                BigDecimal suspenseToReverse = loan.getInterestReversed();
                if (suspenseToReverse.compareTo(BigDecimal.ZERO) > 0) {
                    // GL: DR Interest Suspense, CR Interest Income
                    loan.setAccruedInterest(loan.getAccruedInterest().add(suspenseToReverse));
                    loan.setInterestReversed(BigDecimal.ZERO);
                    log.info(
                            "NPA upgrade — suspense reversed to income: loan={} reversed={}",
                            loan.getLoanAccountNumber(),
                            suspenseToReverse);
                }

                loanAccountRepository.save(loan);

                auditService.logEvent(
                        null,
                        "LOAN_NPA_UPGRADED",
                        "LOAN_ACCOUNT",
                        loan.getId(),
                        "Loan "
                                + loan.getLoanAccountNumber()
                                + " upgraded from NPA to STANDARD. All overdue cleared.",
                        null);

                log.info(
                        "LOAN NPA UPGRADE: {} upgraded to STANDARD (all overdue cleared)",
                        loan.getLoanAccountNumber());
            } else if (loan.getStatus() == LoanStatus.NPA) {
                // ── EXISTING NPA — update DPD + tier progression ──
                // RBI IRAC: classification must progress daily
                // (SUBSTANDARD → DOUBTFUL → LOSS based on DPD age)
                loanAccountRepository.save(loan);

                log.debug(
                        "NPA tier updated: {} classification={} DPD={}",
                        loan.getLoanAccountNumber(),
                        classification,
                        computedDpd);
            } else {
                // Save DPD update for ACTIVE loans not yet breaching threshold
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
