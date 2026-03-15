package com.ledgora.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

/**
 * RBI ALM (Asset Liability Management) — Structural Liquidity Statement.
 *
 * <p>RBI Master Circular on ALM (DBR.No.BP.BC.21/21.04.098/2017-18):
 *
 * <ul>
 *   <li>Banks must prepare Statement of Structural Liquidity (SSL)
 *   <li>Time buckets: 1-14d, 15-28d, 29d-3m, 3-6m, 6m-1y, 1-3y, 3-5y, >5y
 *   <li>Negative gap in short-term buckets indicates liquidity risk
 *   <li>Net outflows in 1-14d bucket should not exceed 5% of total outflows
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlmReport {
    private LocalDate reportDate;
    private Long tenantId;
    private List<AlmBucket> buckets;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal overallGap;
    private boolean hasStructuralLiquidityRisk;
    private String riskAssessment;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlmBucket {
        private String bucketName;
        private int bucketOrder;
        private BigDecimal assets;
        private BigDecimal liabilities;
        private BigDecimal gap;
        private BigDecimal cumulativeGap;
        private BigDecimal gapRatioPercent;
    }
}
