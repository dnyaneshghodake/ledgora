package com.ledgora.loan.service;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.InstallmentStatus;
import com.ledgora.loan.entity.LoanSchedule;
import com.ledgora.loan.repository.LoanScheduleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS-grade Loan Schedule Service. Generates amortization schedule on loan disbursement
 * using reducing-balance EMI formula. Updates DPD counters during EOD.
 *
 * <p>EMI Formula (reducing balance):
 *   EMI = P × r × (1+r)^n / ((1+r)^n - 1)
 *   where P = principal, r = monthly rate, n = tenure in months
 */
@Service
public class LoanScheduleService {

    private static final Logger log = LoggerFactory.getLogger(LoanScheduleService.class);
    private final LoanScheduleRepository scheduleRepository;

    public LoanScheduleService(LoanScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
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
        BigDecimal monthlyRate = annualRate
                .divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);

        // EMI = P × r × (1+r)^n / ((1+r)^n - 1)
        BigDecimal emi = calculateEmi(principal, monthlyRate, tenureMonths);

        List<LoanSchedule> schedules = new ArrayList<>();
        BigDecimal outstandingPrincipal = principal;

        for (int i = 1; i <= tenureMonths; i++) {
            LocalDate dueDate = disbursementDate.plusMonths(i);

            // Interest for this month on outstanding principal
            BigDecimal interestComponent = outstandingPrincipal
                    .multiply(monthlyRate)
                    .setScale(4, RoundingMode.HALF_UP);

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

            LoanSchedule row = LoanSchedule.builder()
                    .account(account)
                    .installmentNumber(i)
                    .dueDate(dueDate)
                    .principalComponent(principalComponent.setScale(4, RoundingMode.HALF_UP))
                    .interestComponent(interestComponent)
                    .emiAmount(emi.setScale(4, RoundingMode.HALF_UP))
                    .outstandingPrincipal(outstandingPrincipal.setScale(4, RoundingMode.HALF_UP))
                    .status(InstallmentStatus.SCHEDULED)
                    .dpdDays(0)
                    .build();
            schedules.add(row);
        }

        List<LoanSchedule> saved = scheduleRepository.saveAll(schedules);
        log.info("Loan schedule generated: account={} installments={} EMI={} principal={} rate={}%",
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
            long daysPast = java.time.temporal.ChronoUnit.DAYS.between(
                    installment.getDueDate(), businessDate);
            installment.setDpdDays((int) daysPast);
            installment.setStatus(InstallmentStatus.OVERDUE);
            scheduleRepository.save(installment);
            updated++;
        }

        if (updated > 0) {
            log.info("DPD updated for tenant {}: {} installments marked OVERDUE", tenantId, updated);
        }
        return updated;
    }

    /**
     * Get schedule for an account.
     */
    public List<LoanSchedule> getSchedule(Long accountId) {
        return scheduleRepository.findByAccountIdOrderByInstallmentNumberAsc(accountId);
    }

    /**
     * Check if account has NPA risk (DPD > 90 on any installment).
     */
    public boolean isNpaRisk(Long accountId) {
        return scheduleRepository.countByAccountIdAndDpdGreaterThan(accountId, 90) > 0;
    }

    /**
     * Reducing-balance EMI formula:
     * EMI = P × r × (1+r)^n / ((1+r)^n - 1)
     */
    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal monthlyRate, int months) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            // Zero interest — flat division
            return principal.divide(new BigDecimal(months), 4, RoundingMode.HALF_UP);
        }
        // (1 + r)^n
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(months);

        // P × r × (1+r)^n
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);

        // (1+r)^n - 1
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }
}
