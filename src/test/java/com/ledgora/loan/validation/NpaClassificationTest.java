package com.ledgora.loan.validation;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.loan.enums.NpaClassification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * NPA Classification Test — validates RBI IRAC DPD-based classification tiers.
 *
 * <p>RBI Master Circular on Prudential Norms:
 *
 * <ul>
 *   <li>DPD ≤ 90 → STANDARD
 *   <li>DPD 91–365 → SUBSTANDARD
 *   <li>DPD 366–1095 → DOUBTFUL
 *   <li>DPD > 1095 → LOSS
 * </ul>
 */
class NpaClassificationTest {

    private static final int DEFAULT_THRESHOLD = 90;

    @Test
    @DisplayName("DPD = 0 → STANDARD")
    void zeroDpd_standard() {
        assertEquals(NpaClassification.STANDARD, NpaClassifier.classify(0, DEFAULT_THRESHOLD));
        assertFalse(NpaClassifier.isNpa(0, DEFAULT_THRESHOLD));
    }

    @Test
    @DisplayName("DPD = 90 (exactly at threshold) → STANDARD")
    void exactThreshold_standard() {
        assertEquals(NpaClassification.STANDARD, NpaClassifier.classify(90, DEFAULT_THRESHOLD));
        assertFalse(NpaClassifier.isNpa(90, DEFAULT_THRESHOLD));
    }

    @Test
    @DisplayName("DPD = 91 → SUBSTANDARD (first day of NPA)")
    void justOverThreshold_substandard() {
        assertEquals(NpaClassification.SUBSTANDARD, NpaClassifier.classify(91, DEFAULT_THRESHOLD));
        assertTrue(NpaClassifier.isNpa(91, DEFAULT_THRESHOLD));
    }

    @Test
    @DisplayName("DPD = 365 → SUBSTANDARD (last day before DOUBTFUL)")
    void endOfSubstandard() {
        assertEquals(NpaClassification.SUBSTANDARD, NpaClassifier.classify(365, DEFAULT_THRESHOLD));
    }

    @Test
    @DisplayName("DPD = 366 → DOUBTFUL")
    void startOfDoubtful() {
        assertEquals(NpaClassification.DOUBTFUL, NpaClassifier.classify(366, DEFAULT_THRESHOLD));
    }

    @Test
    @DisplayName("DPD = 1095 → DOUBTFUL (last day before LOSS)")
    void endOfDoubtful() {
        assertEquals(NpaClassification.DOUBTFUL, NpaClassifier.classify(1095, DEFAULT_THRESHOLD));
    }

    @Test
    @DisplayName("DPD = 1096 → LOSS")
    void startOfLoss() {
        assertEquals(NpaClassification.LOSS, NpaClassifier.classify(1096, DEFAULT_THRESHOLD));
    }

    @Test
    @DisplayName("DPD = 2000 → LOSS")
    void deepLoss() {
        assertEquals(NpaClassification.LOSS, NpaClassifier.classify(2000, DEFAULT_THRESHOLD));
    }

    @ParameterizedTest(name = "DPD={0} with threshold={1} → {2}")
    @CsvSource({
        "0, 90, STANDARD",
        "89, 90, STANDARD",
        "90, 90, STANDARD",
        "91, 90, SUBSTANDARD",
        "180, 90, SUBSTANDARD",
        "365, 90, SUBSTANDARD",
        "366, 90, DOUBTFUL",
        "730, 90, DOUBTFUL",
        "1095, 90, DOUBTFUL",
        "1096, 90, LOSS",
        "0, 60, STANDARD",
        "60, 60, STANDARD",
        "61, 60, SUBSTANDARD",
    })
    @DisplayName("Parameterized DPD classification")
    void parameterizedClassification(int dpd, int threshold, String expected) {
        assertEquals(
                NpaClassification.valueOf(expected),
                NpaClassifier.classify(dpd, threshold),
                "DPD=" + dpd + " threshold=" + threshold + " should be " + expected);
    }

    @Test
    @DisplayName("Custom threshold: 60 days (stricter than RBI default)")
    void customThreshold_60Days() {
        assertEquals(NpaClassification.STANDARD, NpaClassifier.classify(60, 60));
        assertEquals(NpaClassification.SUBSTANDARD, NpaClassifier.classify(61, 60));
        assertFalse(NpaClassifier.isNpa(60, 60));
        assertTrue(NpaClassifier.isNpa(61, 60));
    }

    @Test
    @DisplayName("Provisioning rates match RBI norms")
    void provisioningRates() {
        assertEquals(
                0,
                NpaClassification.STANDARD
                        .getProvisionRate()
                        .compareTo(new java.math.BigDecimal("0.40")),
                "STANDARD provision should be 0.40%");
        assertEquals(
                0,
                NpaClassification.SUBSTANDARD
                        .getProvisionRate()
                        .compareTo(new java.math.BigDecimal("15.00")),
                "SUBSTANDARD provision should be 15%");
        assertEquals(
                0,
                NpaClassification.DOUBTFUL
                        .getProvisionRate()
                        .compareTo(new java.math.BigDecimal("25.00")),
                "DOUBTFUL provision should be 25%");
        assertEquals(
                0,
                NpaClassification.LOSS
                        .getProvisionRate()
                        .compareTo(new java.math.BigDecimal("100.00")),
                "LOSS provision should be 100%");
    }
}
