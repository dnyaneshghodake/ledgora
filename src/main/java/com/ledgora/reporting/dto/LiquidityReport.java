package com.ledgora.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

/** PART 10: Liquidity Report DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidityReport {
    private LocalDate reportDate;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal netLiquidity;
    private BigDecimal totalCustomerDeposits;
    private BigDecimal totalCashHoldings;
    private BigDecimal liquidityRatio;
    private List<LiquidityLine> details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LiquidityLine {
        private String glCode;
        private String glName;
        private String accountType;
        private BigDecimal balance;
    }
}
