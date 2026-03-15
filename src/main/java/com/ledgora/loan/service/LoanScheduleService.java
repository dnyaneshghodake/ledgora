package com.ledgora.loan.service;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.InstallmentStatus;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanSchedule;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanScheduleRepository;
import com.ledgora.loan.validation.EmiCalculator;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS-grade Loan Schedule Service — Finacle LACHST equivalent.
 *
 * <p>Schedule operations:
 *
 * <ul>
 *   <li>Generate amortization schedule on loan disbursement
 *   <li>Regenerate schedule after rate change (EMI restructuring)
 *   <li>Regenerate schedule after part-prepayment (tenure/EMI reduction)
 *   <li>Update DPD counters during EOD
 * </ul>
 *
 * <p>EMI Formula (reducing balance): EMI = P × r × (1+r)^n / ((1+r)^n - 1)
 * via centralized {@link EmiCalculator}.
 */
@Service
public class LoanScheduleService {

    private static final Logger log = LoggerFactory.getLogger(LoanScheduleService.class);
    private final LoanScheduleRepository scheduleRepository;
    private final LoanAccountRepository loanAccountRepository;

    public LoanScheduleService(
            LoanScheduleRepository scheduleRepository,
            LoanAccountRepository loanAccountRepository) {
        this.scheduleRepository = scheduleRepository;
        this.loanAccountRepository = loanAccountRepository;
    }

    /**
     * Generate full amortization schedule on loan disbursement.
     *
     * @param account the LOAN account being disbursed
     * @param principal loan principal amount
     * @param annualRate annual interest rate (e.g., 12.00 for 12%)
     * @param tenureMonths loan tenure in months
     * @param disbursementDate date of disbursement (first EMI due 1 month later)
     * @return list of generated schedule rows
     */
    @Transactional
    public List<LoanSchedule> generateSchedule(
            Account account,
            BigDecimal principal,
            BigDecimal annualRate,
            int tenureMonths,
            LocalDate disbursementDate) {

        if (scheduleRepository.existsByAccountId(account.getId())) {
            throw new RuntimeException(
                    "Loan schedule already exists for account " + account.getAccountNumber());
        }

        // Monthly interest rate: annual / 12 / 100
        BigDecimal monthlyRate =
                annualRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);

        // EMI = P × r × (1+r)^n / ((1+r)^n - 1)
        BigDecimal emi = calculateEmi(principal, monthlyRate, tenureMonths);

        List<LoanSchedule> schedules = new ArrayList<>();
        BigDecimal outstandingPrincipal = principal;

        for (int i = 1; i <= tenureMonths; i++) {
            LocalDate dueDate = disbursementDate.plusMonths(i);

            // Interest for this month on outstanding principal
            BigDecimal interestComponent =
                    outstandingPrincipal.multiply(monthlyRate).setScale(4, RoundingMode.HALF_UP);

            // Principal component = EMI - interest
            BigDecimal principalComponent = emi.subtract(interestComponent);

            // Last installment adjustment (absorb rounding difference)
            if (i == tenureMonths) {
                principalComponent = outstandingPrincipal;
                emi = principalComponent.add(interestComponent);
            }

            outstandingPrincipal = outstandingPrincipal.subtract(principalComponent);
            if (outstandingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                outstandingPrincipal = BigDecimal.ZERO;
            }

            LoanSchedule row =
                    LoanSchedule.builder()
                            .account(account)
                            .installmentNumber(i)
                            .dueDate(dueDate)
                            .principalComponent(
                                    principalComponent.setScale(4, RoundingMode.HALF_UP))
                            .interestComponent(interestComponent)
                            .emiAmount(emi.setScale(4, RoundingMode.HALF_UP))
                            .outstandingPrincipal(
                                    outstandingPrincipal.setScale(4, RoundingMode.HALF_UP))
                            .status(InstallmentStatus.SCHEDULED)
                            .dpdDays(0)
                            .build();
            schedules.add(row);
        }

        List<LoanSchedule> saved = scheduleRepository.saveAll(schedules);
        log.info(
                "Loan schedule generated: account={} installments={} EMI={} principal={} rate={}%",
                account.getAccountNumber(), tenureMonths, emi, principal, annualRate);
        return saved;
    }

    /**
     * Update DPD counters for overdue installments. Called by EOD.
     *
     * @param tenantId tenant to process
     * @param businessDate current business date
     * @return number of installments updated
     */
    @Transactional
    public int updateDpdCounters(Long tenantId, LocalDate businessDate) {
        List<LoanSchedule> overdueInstallments =
                scheduleRepository.findOverdueByTenantId(tenantId, businessDate);

        int updated = 0;
        for (LoanSchedule installment : overdueInstallments) {
            long daysPast =
                    java.time.temporal.ChronoUnit.DAYS.between(
                            installment.getDueDate(), businessDate);
            installment.setDpdDays((int) daysPast);
            installment.setStatus(InstallmentStatus.OVERDUE);
            scheduleRepository.save(installment);
            updated++;
        }

        if (updated > 0) {
            log.info(
                    "DPD updated for tenant {}: {} installments marked OVERDUE", tenantId, updated);
        }
        return updated;
    }

    /** Get schedule for an account. */
    public List<LoanSchedule> getSchedule(Long accountId) {
        return scheduleRepository.findByAccountIdOrderByInstallmentNumberAsc(accountId);
    }

    /** Check if account has NPA risk (DPD > 90 on any installment). */
    public boolean isNpaRisk(Long accountId) {
        return scheduleRepository.countByAccountIdAndDpdGreaterThan(accountId, 90) > 0;
    }

    /** Reducing-balance EMI formula — delegates to centralized EmiCalculator. */
    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal monthlyRate, int months) {
        // Convert monthlyRate back to annual % for EmiCalculator
        BigDecimal annualRate =
                monthlyRate
                        .multiply(new BigDecimal("12"))
                        .multiply(new BigDecimal("100"));
        return EmiCalculator.computeEmi(principal, annualRate, months);
    }

    /**
     * Regenerate remaining schedule after a rate change or prepayment.
     *
     * <p>Only unpaid installments (SCHEDULED, DUE, OVERDUE, PARTIALLY_PAID) are replaced.
     * PAID installments are immutable — they represent historical fact.
     *
     * @param loanAccountId the loan to restructure
     * @param newRate new annual interest rate (null = use current loan rate)
     * @return number of installments regenerated
     */
    @Transactional
    public int regenerateSchedule(Long loanAccountId, BigDecimal newRate) {
        LoanAccount loan =
                loanAccountRepository
                        .findById(loanAccountId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LOAN_NOT_FOUND",
                                                "Loan account not found: " + loanAccountId));

        BigDecimal effectiveRate = newRate != null ? newRate : loan.getInterestRate();
        BigDecimal outstanding = loan.getOutstandingPrincipal();

        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        List<LoanSchedule> pending =
                scheduleRepository.findPendingByLoanAccountIdOrderByInstallmentAsc(loanAccountId);
        if (pending.isEmpty()) {
            return 0;
        }

        int remainingTenure = pending.size();
        LocalDate nextDueDate = pending.get(0).getDueDate();
        int startInstallmentNumber = pending.get(0).getInstallmentNumber();

        scheduleRepository.deleteAll(pending);

        BigDecimal monthlyRate = EmiCalculator.monthlyRate(effectiveRate);
        BigDecimal emi = EmiCalculator.computeEmi(outstanding, effectiveRate, remainingTenure);

        List<LoanSchedule> newSchedule = new ArrayList<>();
        BigDecimal remaining = outstanding;

        for (int i = 0; i < remainingTenure; i++) {
            BigDecimal interestComponent =
                    remaining
                            .multiply(monthlyRate, MathContext.DECIMAL128)
                            .setScale(4, RoundingMode.HALF_UP);
            BigDecimal principalComponent = emi.subtract(interestComponent);

            if (i == remainingTenure - 1) {
                principalComponent = remaining;
                emi = principalComponent.add(interestComponent);
            }

            remaining = remaining.subtract(principalComponent);

            newSchedule.add(
                    LoanSchedule.builder()
                            .loanAccount(loan)
                            .account(loan.getLinkedAccount())
                            .installmentNumber(startInstallmentNumber + i)
                            .dueDate(nextDueDate.plusMonths(i))
                            .principalComponent(principalComponent)
                            .interestComponent(interestComponent)
                            .emiAmount(emi)
                            .outstandingPrincipal(
                                    remaining.compareTo(BigDecimal.ZERO) < 0
                                            ? BigDecimal.ZERO
                                            : remaining)
                            .build());
        }

        scheduleRepository.saveAll(newSchedule);

        loan.setEmiAmount(EmiCalculator.computeEmi(outstanding, effectiveRate, remainingTenure));
        loanAccountRepository.save(loan);

        log.info(
                "Schedule regenerated: loan={} installments={} newEmi={} rate={}%",
                loan.getLoanAccountNumber(),
                remainingTenure,
                loan.getEmiAmount(),
                effectiveRate);

        return remainingTenure;
    }
}
