package com.ledgora.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

/**
 * RBI Basel III — Capital to Risk-weighted Assets Ratio (CRAR) Report.
 *
 * <p>RBI Master Circular on Basel III Capital Regulations:
 *
 * <ul>
 *   <li>Minimum CRAR: 9% (RBI mandate, stricter than Basel III's 8%)
 *   <li>Minimum CET1: 5.5%
 *   <li>Minimum Tier 1: 7%
 *   <li>Capital Conservation Buffer: 2.5%
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrarReport {
    private LocalDate reportDate;
    private Long tenantId;

    // ── Capital Components ──
    private BigDecimal tier1Capital;
    private BigDecimal tier2Capital;
    private BigDecimal totalCapital;

    // ── Capital Breakdown ──
    private BigDecimal equityCapital;
    private BigDecimal retainedEarnings;
    private BigDecimal generalProvisions;

    // ── Risk-Weighted Assets ──
    private BigDecimal totalRwa;
    private List<RwaLine> rwaBreakdown;

    // ── Ratios ──
    private BigDecimal crarPercent;
    private BigDecimal tier1Percent;

    // ── Compliance ──
    private boolean meetsMinimumCrar;
    private boolean meetsMinimumTier1;
    private String complianceStatus;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RwaLine {
        private String glCode;
        private String glName;
        private String assetClass;
        private BigDecimal exposure;
        private BigDecimal riskWeight;
        private BigDecimal rwa;
    }
}
