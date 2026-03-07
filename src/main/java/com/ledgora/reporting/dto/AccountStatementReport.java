package com.ledgora.reporting.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PART 10: Account Statement report DTO.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountStatementReport {
    private String accountNumber;
    private String accountName;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private List<StatementLine> entries;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StatementLine {
        private LocalDateTime date;
        private String transactionRef;
        private String description;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private BigDecimal balance;
    }
}
