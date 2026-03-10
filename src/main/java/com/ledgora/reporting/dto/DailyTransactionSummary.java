package com.ledgora.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

/** PART 10: Daily Transaction Summary report DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTransactionSummary {
    private LocalDate date;
    private long totalTransactions;
    private long depositCount;
    private long withdrawalCount;
    private long transferCount;
    private BigDecimal totalDepositAmount;
    private BigDecimal totalWithdrawalAmount;
    private BigDecimal totalTransferAmount;
    private BigDecimal netAmount;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
}
