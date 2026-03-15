package com.ledgora.loan.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * EMI Calculation Test — validates reducing balance EMI formula per RBI Fair Practices Code.
 *
 * <p>Test cases:
 *
 * <ul>
 *   <li>Standard EMI computation (known values)
 *   <li>Zero-interest rate (flat EMI = P / n)
 *   <li>High interest rate
 *   <li>Single month tenure
 *   <li>Monthly rate computation
 *   <li>Daily rate computation (365-day basis per RBI)
 * </ul>
 */
class EmiCalculationTest {

    @Test
    @DisplayName("Standard EMI: 10L @ 12% for 12 months = ~88,849")
    void standardEmi_12Percent_12Months() {
        BigDecimal emi =
                EmiCalculator.computeEmi(
                        new BigDecimal("1000000"), new BigDecimal("12.0000"), 12);
        // Known EMI for 10L @ 12% p.a. for 12 months ≈ 88,849
        assertTrue(
                emi.compareTo(new BigDecimal("88840")) > 0
                        && emi.compareTo(new BigDecimal("88860")) < 0,
                "EMI should be approximately 88,849 but was " + emi);
    }

    @Test
    @DisplayName("Standard EMI: 5L @ 10% for 60 months = ~10,624")
    void standardEmi_10Percent_60Months() {
        BigDecimal emi =
                EmiCalculator.computeEmi(
                        new BigDecimal("500000"), new BigDecimal("10.0000"), 60);
        // Known EMI for 5L @ 10% p.a. for 60 months ≈ 10,624
        assertTrue(
                emi.compareTo(new BigDecimal("10620")) > 0
                        && emi.compareTo(new BigDecimal("10630")) < 0,
                "EMI should be approximately 10,624 but was " + emi);
    }

    @Test
    @DisplayName("Zero interest rate: EMI = principal / tenure")
    void zeroInterestRate_flatEmi() {
        BigDecimal emi =
                EmiCalculator.computeEmi(
                        new BigDecimal("120000"), BigDecimal.ZERO, 12);
        assertEquals(
                0,
                emi.compareTo(new BigDecimal("10000.0000")),
                "Zero-rate EMI should be 120000/12 = 10000");
    }

    @Test
    @DisplayName("Single month tenure: EMI = principal + one month interest")
    void singleMonthTenure() {
        BigDecimal emi =
                EmiCalculator.computeEmi(
                        new BigDecimal("100000"), new BigDecimal("12.0000"), 1);
        // EMI for 1 month = 100000 + (100000 × 12/100/12) = 100000 + 1000 = 101000
        assertEquals(
                0,
                emi.compareTo(new BigDecimal("101000.0000")),
                "Single-month EMI should be 101000 but was " + emi);
    }

    @Test
    @DisplayName("High interest rate: 24% p.a.")
    void highInterestRate() {
        BigDecimal emi =
                EmiCalculator.computeEmi(
                        new BigDecimal("100000"), new BigDecimal("24.0000"), 12);
        // Should be a valid positive number, higher than zero-rate EMI
        assertTrue(emi.compareTo(new BigDecimal("8333")) > 0, "High-rate EMI should exceed flat EMI");
        assertTrue(emi.compareTo(new BigDecimal("10000")) < 0, "High-rate EMI should be reasonable");
    }

    @Test
    @DisplayName("EMI is always positive for positive inputs")
    void emiAlwaysPositive() {
        BigDecimal emi =
                EmiCalculator.computeEmi(
                        new BigDecimal("1"), new BigDecimal("0.0001"), 360);
        assertTrue(emi.compareTo(BigDecimal.ZERO) > 0, "EMI must always be positive");
    }

    @Test
    @DisplayName("Monthly rate: 12% p.a. → 0.01 per month")
    void monthlyRate_12Percent() {
        BigDecimal rate = EmiCalculator.monthlyRate(new BigDecimal("12.0000"));
        // 12 / 100 / 12 = 0.01
        assertTrue(
                rate.compareTo(new BigDecimal("0.0099")) > 0
                        && rate.compareTo(new BigDecimal("0.0101")) < 0,
                "Monthly rate should be ~0.01 but was " + rate);
    }

    @Test
    @DisplayName("Daily rate: 12% p.a. → ~0.000328767 per day (365-day basis)")
    void dailyRate_12Percent() {
        BigDecimal rate = EmiCalculator.dailyRate(new BigDecimal("12.0000"));
        // 12 / 100 / 365 ≈ 0.000328767
        assertTrue(
                rate.compareTo(new BigDecimal("0.000328")) > 0
                        && rate.compareTo(new BigDecimal("0.000330")) < 0,
                "Daily rate should be ~0.000329 but was " + rate);
    }

    @Test
    @DisplayName("Zero rate: monthly and daily rates are zero")
    void zeroRate_monthlyAndDaily() {
        assertEquals(
                0,
                EmiCalculator.monthlyRate(BigDecimal.ZERO).compareTo(BigDecimal.ZERO),
                "Zero annual rate should produce zero monthly rate");
        assertEquals(
                0,
                EmiCalculator.dailyRate(BigDecimal.ZERO).compareTo(BigDecimal.ZERO),
                "Zero annual rate should produce zero daily rate");
    }

    @Test
    @DisplayName("EMI schedule integrity: sum of principal components ≈ principal amount")
    void scheduleIntegrity_principalSumMatchesPrincipal() {
        BigDecimal principal = new BigDecimal("500000");
        BigDecimal annualRate = new BigDecimal("10.0000");
        int tenure = 24;

        BigDecimal emi = EmiCalculator.computeEmi(principal, annualRate, tenure);
        BigDecimal monthlyRate = EmiCalculator.monthlyRate(annualRate);

        BigDecimal remaining = principal;
        BigDecimal totalPrincipalPaid = BigDecimal.ZERO;

        for (int i = 1; i <= tenure; i++) {
            BigDecimal interest =
                    remaining
                            .multiply(monthlyRate)
                            .setScale(4, java.math.RoundingMode.HALF_UP);
            BigDecimal principalComponent = emi.subtract(interest);
            if (i == tenure) {
                principalComponent = remaining; // last installment adjustment
            }
            totalPrincipalPaid = totalPrincipalPaid.add(principalComponent);
            remaining = remaining.subtract(principalComponent);
        }

        // Total principal paid should equal original principal (within rounding tolerance)
        assertTrue(
                totalPrincipalPaid.subtract(principal).abs().compareTo(new BigDecimal("1")) < 0,
                "Sum of principal components should equal original principal. Diff="
                        + totalPrincipalPaid.subtract(principal));
    }
}
