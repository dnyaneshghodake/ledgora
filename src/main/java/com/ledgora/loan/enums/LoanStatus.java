package com.ledgora.loan.enums;

/**
 * Loan account lifecycle status per CBS/Finacle loan lifecycle.
 *
 * <p>Full lifecycle: PENDING → SANCTIONED → ACTIVE → NPA → WRITTEN_OFF / CLOSED
 *
 * <ul>
 *   <li>PENDING: Loan application submitted, awaiting sanction (maker entry)
 *   <li>SANCTIONED: Approved by checker, awaiting disbursement. No GL impact yet.
 *   <li>ACTIVE: Disbursed and performing — interest accrual on accrual basis.
 *   <li>NPA: Non-Performing Asset — DPD > 90 per RBI Prudential Norms. Interest stops.
 *   <li>RESTRUCTURED: Loan restructured per RBI guidelines — separate classification tracked.
 *   <li>WRITTEN_OFF: Fully provided and written off — removed from loan asset GL.
 *   <li>CLOSED: Fully repaid or foreclosed — zero outstanding.
 * </ul>
 */
public enum LoanStatus {
    /** Loan application submitted, awaiting sanction. */
    PENDING,
    /** Sanctioned by checker, awaiting disbursement. No GL impact. */
    SANCTIONED,
    /** Disbursed and performing. */
    ACTIVE,
    /** Non-Performing Asset — DPD > 90. */
    NPA,
    /** Restructured per RBI guidelines. */
    RESTRUCTURED,
    /** Written off — removed from asset book. */
    WRITTEN_OFF,
    /** Fully repaid or foreclosed. */
    CLOSED
}
