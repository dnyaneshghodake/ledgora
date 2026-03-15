package com.ledgora.reporting.enums;

/**
 * Statement section classification per RBI Schedule 5/14 and Finacle GL grouping.
 *
 * <p>Balance Sheet sections: ASSET, LIABILITY, EQUITY. P&L sections: INCOME, EXPENSE.
 *
 * <p>Normal balance rules (Finacle accounting convention):
 *
 * <ul>
 *   <li>ASSET / EXPENSE → normal DEBIT (balance = SUM(DR) - SUM(CR))
 *   <li>LIABILITY / EQUITY / INCOME → normal CREDIT (balance = SUM(CR) - SUM(DR))
 * </ul>
 */
public enum StatementSection {
    ASSET,
    LIABILITY,
    EQUITY,
    INCOME,
    EXPENSE
}
