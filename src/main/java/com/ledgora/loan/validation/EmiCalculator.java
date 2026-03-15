package com.ledgora.loan.validation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * EMI Calculator — Finacle-grade reducing balance EMI computation.
 *
 * <p>Standard reducing balance EMI formula per RBI Fair Practices Code:
 *
 * <pre>
 *   EMI = P × r × (1+r)^n / ((1+r)^n - 1)
 * </pre>
 *
 * <p>Where: P = principal, r = monthly interest rate, n = tenure in months.
 *
 * <p>Zero-rate guard: when interest rate is 0%, EMI = P / n (simple equal installments).
 *
 * <p>This utility is stateless and thread-safe. Used by:
 *
 * <ul>
 *   <li>{@code LoanDisbursementService} — EMI stored at disbursement
 *   <li>{@code LoanScheduleService} — amortization schedule generation
 *   <li>{@code LoanDashboardController} — pre-disbursement preview
 * </ul>
 */
public final class EmiCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private EmiCalculator() {
        // Utility class — no instantiation
    }

    /**
     * Compute monthly EMI using reducing balance formula.
     *
     * @param principal loan principal amount (must be positive)
     * @param annualRate annual interest rate in percent (e.g. 12.5 for 12.5%)
     * @param tenureMonths loan tenure in months (must be positive)
     * @return EMI amount rounded to 4 decimal places
     */
    public static BigDecimal computeEmi(
            BigDecimal principal, BigDecimal annualRate, int tenureMonths) {

        BigDecimal monthlyRate =
                annualRate
                        .divide(HUNDRED, 10, RoundingMode.HALF_UP)
                        .divide(TWELVE, 10, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            // Zero-interest loan: EMI = principal / tenure
            return principal.divide(new BigDecimal(tenureMonths), 4, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(tenureMonths, MathContext.DECIMAL128);
        return principal
                .multiply(monthlyRate, MathContext.DECIMAL128)
                .multiply(onePlusRPowerN, MathContext.DECIMAL128)
                .divide(onePlusRPowerN.subtract(BigDecimal.ONE), 4, RoundingMode.HALF_UP);
    }

    /**
     * Compute monthly interest rate from annual rate.
     *
     * @param annualRate annual interest rate in percent
     * @return monthly rate as a decimal (e.g. 0.01 for 12% p.a.)
     */
    public static BigDecimal monthlyRate(BigDecimal annualRate) {
        return annualRate
                .divide(HUNDRED, 10, RoundingMode.HALF_UP)
                .divide(TWELVE, 10, RoundingMode.HALF_UP);
    }

    /**
     * Compute daily interest rate from annual rate (365-day basis per RBI).
     *
     * @param annualRate annual interest rate in percent
     * @return daily rate as a decimal
     */
    public static BigDecimal dailyRate(BigDecimal annualRate) {
        return annualRate
                .divide(HUNDRED, 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP);
    }
}
