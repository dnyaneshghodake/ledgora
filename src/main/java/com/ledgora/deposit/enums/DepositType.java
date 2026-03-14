package com.ledgora.deposit.enums;

/**
 * Deposit product types per RBI classification.
 *
 * <p>CASA: Current Account Savings Account (demand deposits — NDTL component). FD: Fixed Deposit
 * (time deposit — NDTL component). RD: Recurring Deposit (time deposit with periodic installments).
 *
 * <p>RBI CRR/SLR: All deposit types contribute to NDTL computation. DICGC insurance covers up to
 * ₹5,00,000 per depositor per bank.
 */
public enum DepositType {
    SAVINGS,
    CURRENT,
    FIXED_DEPOSIT,
    RECURRING_DEPOSIT
}
