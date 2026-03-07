package com.ledgora.reporting.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PART 10: General Ledger report DTO.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GeneralLedgerReport {
    private String glCode;
    private String glName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private List<GLReportEntry> entries;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GLReportEntry {
        private LocalDateTime postingTime;
        private String transactionRef;
        private String narration;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private BigDecimal runningBalance;
    }
}
