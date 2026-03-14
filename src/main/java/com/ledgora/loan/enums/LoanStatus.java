package com.ledgora.loan.enums;

/**
 * Loan account lifecycle status per RBI IRAC norms.
 *
 * <p>ACTIVE: Performing loan — interest accrual on accrual basis. NPA: Non-Performing Asset — dpd >
 * 90 days per RBI Prudential Norms. Interest recognition stops. WRITTEN_OFF: Fully provided and
 * written off — removed from loan asset GL. CLOSED: Fully repaid — zero outstanding.
 */
public enum LoanStatus {
    ACTIVE,
    NPA,
    WRITTEN_OFF,
    CLOSED
}
