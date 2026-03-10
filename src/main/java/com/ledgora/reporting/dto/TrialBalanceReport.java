package com.ledgora.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

/** PART 10: Trial Balance report DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialBalanceReport {
    private LocalDate reportDate;
    private List<TrialBalanceLine> lines;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private boolean balanced;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrialBalanceLine {
        private String glCode;
        private String glName;
        private String accountType;
        private BigDecimal debitBalance;
        private BigDecimal creditBalance;
    }
}
