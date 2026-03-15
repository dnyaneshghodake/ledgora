package com.ledgora.loan.validation;

import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.enums.SmaCategory;

/**
 * NPA Classifier — RBI IRAC (Income Recognition and Asset Classification) logic.
 *
 * <p>RBI Master Circular on Prudential Norms + SMA Framework:
 *
 * <ul>
 *   <li>NONE: DPD = 0 (performing, no overdue)
 *   <li>SMA-0: DPD 1–30 days
 *   <li>SMA-1: DPD 31–60 days
 *   <li>SMA-2: DPD 61–90 days
 *   <li>STANDARD: DPD ≤ 90 days (performing — includes SMA categories)
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
     * Classify SMA category based on DPD per RBI Early Warning Framework.
     *
     * <p>SMA categories apply ONLY to performing (STANDARD) loans.
     * Once a loan is NPA (DPD > threshold), SMA category is no longer relevant.
     *
     * @param dpd days past due
     * @return the SMA category
     */
    public static SmaCategory classifySma(int dpd) {
        if (dpd <= 0) {
            return SmaCategory.NONE;
        } else if (dpd <= 30) {
            return SmaCategory.SMA_0;
        } else if (dpd <= 60) {
            return SmaCategory.SMA_1;
        } else {
            return SmaCategory.SMA_2;
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
