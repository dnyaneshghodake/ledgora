package com.ledgora.loan.validation;

import com.ledgora.loan.enums.NpaClassification;

/**
 * NPA Classifier — RBI IRAC (Income Recognition and Asset Classification) logic.
 *
 * <p>RBI Master Circular on Prudential Norms:
 *
 * <ul>
 *   <li>STANDARD: DPD ≤ 90 days (performing)
 *   <li>SUBSTANDARD: DPD 91–365 days (NPA < 12 months)
 *   <li>DOUBTFUL: DPD 366–1095 days (NPA 1–3 years)
 *   <li>LOSS: DPD > 1095 days or identified as unrecoverable
 * </ul>
 *
 * <p>Stateless utility — used by {@code LoanNpaService} during EOD evaluation.
 */
public final class NpaClassifier {

    private NpaClassifier() {
        // Utility class — no instantiation
    }

    /**
     * Classify loan based on DPD per RBI IRAC norms.
     *
     * @param dpd days past due
     * @param threshold product-level NPA threshold (default 90)
     * @return the NPA classification tier
     */
    public static NpaClassification classify(int dpd, int threshold) {
        if (dpd <= threshold) {
            return NpaClassification.STANDARD;
        } else if (dpd <= 365) {
            return NpaClassification.SUBSTANDARD;
        } else if (dpd <= 1095) {
            return NpaClassification.DOUBTFUL;
        } else {
            return NpaClassification.LOSS;
        }
    }

    /**
     * Check if a loan should be classified as NPA based on DPD.
     *
     * @param dpd days past due
     * @param threshold product-level NPA threshold (default 90)
     * @return true if DPD exceeds the NPA threshold
     */
    public static boolean isNpa(int dpd, int threshold) {
        return dpd > threshold;
    }
}
