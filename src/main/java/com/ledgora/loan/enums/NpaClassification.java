package com.ledgora.loan.enums;

import java.math.BigDecimal;

/**
 * RBI IRAC (Income Recognition and Asset Classification) — NPA classification with provisioning
 * rates per RBI Master Circular on Prudential Norms.
 *
 * <p>Provisioning norms (basic model):
 *
 * <ul>
 *   <li>STANDARD: 0.40% of outstanding
 *   <li>SUBSTANDARD: 15% (unsecured: 25%)
 *   <li>DOUBTFUL: 25% to 100% depending on age
 *   <li>LOSS: 100%
 * </ul>
 */
public enum NpaClassification {
    STANDARD(new BigDecimal("0.40")),
    SUBSTANDARD(new BigDecimal("15.00")),
    DOUBTFUL(new BigDecimal("25.00")),
    LOSS(new BigDecimal("100.00"));

    private final BigDecimal provisionRate;

    NpaClassification(BigDecimal provisionRate) {
        this.provisionRate = provisionRate;
    }

    /** Default provisioning percentage for this classification. */
    public BigDecimal getProvisionRate() {
        return provisionRate;
    }
}
