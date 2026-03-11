package com.ledgora.common.enums;

/** CBS Loan installment status. Tracks payment lifecycle per EMI. */
public enum InstallmentStatus {
    /** Upcoming — not yet due. */
    SCHEDULED,
    /** Due date reached, payment pending. */
    DUE,
    /** Full payment received. */
    PAID,
    /** Past due date, payment not received. DPD counter starts. */
    OVERDUE,
    /** Partially paid — residual balance carried forward. */
    PARTIALLY_PAID,
    /** Written off — unrecoverable. */
    WRITTEN_OFF
}
