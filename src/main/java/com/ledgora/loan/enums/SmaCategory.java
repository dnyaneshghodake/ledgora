package com.ledgora.loan.enums;

/**
 * SMA (Special Mention Account) Classification per RBI circular on Early Warning Signals.
 *
 * <p>RBI/2023-24/XXX — Framework for Early Recognition of Stress:
 *
 * <ul>
 *   <li>SMA_0: Principal/interest overdue 1–30 days
 *   <li>SMA_1: Principal/interest overdue 31–60 days
 *   <li>SMA_2: Principal/interest overdue 61–90 days
 *   <li>NPA: Principal/interest overdue > 90 days (transitions to LoanStatus.NPA)
 *   <li>NONE: No overdue — performing asset
 * </ul>
 *
 * <p>SMA reporting is mandatory for all loans ≥ ₹5 crore to CRILC (Central Repository
 * of Information on Large Credits). Used for consortium lending early warning.
 */
public enum SmaCategory {
    /** No overdue — performing asset. */
    NONE,
    /** SMA-0: 1–30 days overdue. */
    SMA_0,
    /** SMA-1: 31–60 days overdue. */
    SMA_1,
    /** SMA-2: 61–90 days overdue. Immediate attention required. */
    SMA_2
}
