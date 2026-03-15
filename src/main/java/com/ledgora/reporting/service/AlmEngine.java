package com.ledgora.reporting.service;

import com.ledgora.common.enums.GLAccountType;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.reporting.dto.AlmReport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI ALM (Asset Liability Management) — Structural Liquidity Statement Engine.
 *
 * <p>RBI Master Circular on Risk Management and ALM (DBR.No.BP.BC.21/21.04.098/2017-18):
 *
 * <ul>
 *   <li>Statement of Structural Liquidity (SSL) with RBI-mandated time buckets
 *   <li>Buckets: 1-14d, 15-28d, 29d-3m, 3-6m, 6m-1y, 1-3y, 3-5y, >5y
 *   <li>Gap = Assets - Liabilities per bucket
 *   <li>Negative cumulative gap in short-term buckets = liquidity risk
 *   <li>Gap ratio = Gap / Total Liabilities × 100
 * </ul>
 *
 * <p>Simplified model: Assets and liabilities are distributed into maturity buckets based on GL
 * account type heuristics. A full implementation would use contractual maturity dates from
 * individual accounts and loan schedules.
 *
 * <p>Finacle ALM module equivalent: ALMSSL (Structural Liquidity Statement).
 */
@Service
public class AlmEngine {

    private static final Logger log = LoggerFactory.getLogger(AlmEngine.class);

    /** Gap ratio threshold for structural liquidity risk flag. */
    private static final BigDecimal RISK_THRESHOLD = new BigDecimal("-15.00");

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** RBI-mandated ALM time buckets. */
    private static final String[] BUCKET_NAMES = {
        "1-14 days",
        "15-28 days",
        "29 days - 3 months",
        "3-6 months",
        "6 months - 1 year",
        "1-3 years",
        "3-5 years",
        "Over 5 years"
    };

    /**
     * Asset distribution weights per bucket (simplified heuristic). In production, this would be
     * derived from contractual maturity dates of individual instruments.
     *
     * <p>Assumptions:
     *
     * <ul>
     *   <li>Cash/liquid assets: concentrated in 1-14d bucket
     *   <li>Short-term loans: spread across 1-14d to 6m-1y
     *   <li>Long-term loans: concentrated in 1-3y and 3-5y
     *   <li>Fixed assets: >5y bucket
     * </ul>
     */
    private static final double[][] ASSET_WEIGHTS = {
        // Cash:    {1-14d, 15-28d, 29d-3m, 3-6m, 6m-1y, 1-3y, 3-5y, >5y}
        {0.80, 0.10, 0.05, 0.03, 0.02, 0.00, 0.00, 0.00}, // CASH
        {0.05, 0.05, 0.10, 0.15, 0.25, 0.25, 0.10, 0.05}, // LOANS
        {0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.10, 0.90}, // FIXED_ASSETS
        {0.10, 0.10, 0.15, 0.15, 0.20, 0.15, 0.10, 0.05}, // OTHER_ASSETS
    };

    /** Liability distribution weights per bucket. */
    private static final double[][] LIABILITY_WEIGHTS = {
        // Demand deposits (savings):
        {0.30, 0.15, 0.15, 0.10, 0.10, 0.10, 0.05, 0.05}, // DEMAND_DEPOSITS
        // Term deposits:
        {0.05, 0.05, 0.10, 0.15, 0.25, 0.25, 0.10, 0.05}, // TERM_DEPOSITS
        // Borrowings:
        {0.10, 0.10, 0.15, 0.15, 0.20, 0.15, 0.10, 0.05}, // BORROWINGS
        // Equity (perpetual):
        {0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 1.00}, // EQUITY
    };

    private final GeneralLedgerRepository glRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AlmEngine(
            GeneralLedgerRepository glRepository,
            LedgerEntryRepository ledgerEntryRepository) {
        this.glRepository = glRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Generate ALM Structural Liquidity Statement for a tenant.
     *
     * @return AlmReport with maturity bucket analysis and risk assessment
     */
    @Transactional(readOnly = true)
    public AlmReport generate(Long tenantId, LocalDate asOfDate) {
        List<GeneralLedger> allGl = glRepository.findByTenantIdOrShared(tenantId);

        BigDecimal[] assetBuckets = new BigDecimal[8];
        BigDecimal[] liabilityBuckets = new BigDecimal[8];
        for (int i = 0; i < 8; i++) {
            assetBuckets[i] = BigDecimal.ZERO;
            liabilityBuckets[i] = BigDecimal.ZERO;
        }

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (GeneralLedger gl : allGl) {
            BigDecimal debits =
                    ledgerEntryRepository.sumDebitsByGlCodeAndDateRange(
                            gl.getGlCode(), tenantId, asOfDate);
            BigDecimal credits =
                    ledgerEntryRepository.sumCreditsByGlCodeAndDateRange(
                            gl.getGlCode(), tenantId, asOfDate);

            GLAccountType type = gl.getAccountType();
            BigDecimal balance;
            if (type == GLAccountType.ASSET || type == GLAccountType.EXPENSE) {
                balance = debits.subtract(credits);
            } else {
                balance = credits.subtract(debits);
            }

            if (balance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            if (type == GLAccountType.ASSET) {
                totalAssets = totalAssets.add(balance);
                double[] weights = classifyAssetWeights(gl.getGlName());
                for (int i = 0; i < 8; i++) {
                    assetBuckets[i] =
                            assetBuckets[i].add(
                                    balance.multiply(BigDecimal.valueOf(weights[i]))
                                            .setScale(4, RoundingMode.HALF_UP));
                }
            } else if (type == GLAccountType.LIABILITY || type == GLAccountType.EQUITY) {
                totalLiabilities = totalLiabilities.add(balance);
                double[] weights =
                        type == GLAccountType.EQUITY
                                ? LIABILITY_WEIGHTS[3]
                                : classifyLiabilityWeights(gl.getGlName());
                for (int i = 0; i < 8; i++) {
                    liabilityBuckets[i] =
                            liabilityBuckets[i].add(
                                    balance.multiply(BigDecimal.valueOf(weights[i]))
                                            .setScale(4, RoundingMode.HALF_UP));
                }
            }
        }

        // Build bucket report
        List<AlmReport.AlmBucket> buckets = new ArrayList<>();
        BigDecimal cumulativeGap = BigDecimal.ZERO;
        boolean hasRisk = false;

        for (int i = 0; i < 8; i++) {
            BigDecimal gap = assetBuckets[i].subtract(liabilityBuckets[i]);
            cumulativeGap = cumulativeGap.add(gap);

            BigDecimal gapRatio = BigDecimal.ZERO;
            if (totalLiabilities.compareTo(BigDecimal.ZERO) > 0) {
                gapRatio =
                        gap.multiply(HUNDRED)
                                .divide(totalLiabilities, 2, RoundingMode.HALF_UP);
            }

            // Flag risk if cumulative gap ratio < -15% in short-term buckets (first 3)
            if (i < 3 && gapRatio.compareTo(RISK_THRESHOLD) < 0) {
                hasRisk = true;
            }

            buckets.add(
                    AlmReport.AlmBucket.builder()
                            .bucketName(BUCKET_NAMES[i])
                            .bucketOrder(i + 1)
                            .assets(assetBuckets[i])
                            .liabilities(liabilityBuckets[i])
                            .gap(gap)
                            .cumulativeGap(cumulativeGap)
                            .gapRatioPercent(gapRatio)
                            .build());
        }

        BigDecimal overallGap = totalAssets.subtract(totalLiabilities);

        String riskAssessment;
        if (hasRisk) {
            riskAssessment =
                    "ELEVATED RISK — Negative gap in short-term buckets exceeds -15% threshold. "
                            + "Review funding sources per RBI ALM guidelines.";
        } else if (overallGap.compareTo(BigDecimal.ZERO) < 0) {
            riskAssessment =
                    "MODERATE — Overall negative gap but within short-term tolerance. "
                            + "Monitor funding concentration.";
        } else {
            riskAssessment = "LOW RISK — Positive or neutral gap across maturity buckets.";
        }

        log.info(
                "ALM generated for tenant {} date {}: totalAssets={} totalLiabilities={} gap={} risk={}",
                tenantId,
                asOfDate,
                totalAssets,
                totalLiabilities,
                overallGap,
                hasRisk ? "ELEVATED" : "LOW");

        return AlmReport.builder()
                .reportDate(asOfDate)
                .tenantId(tenantId)
                .buckets(buckets)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .overallGap(overallGap)
                .hasStructuralLiquidityRisk(hasRisk)
                .riskAssessment(riskAssessment)
                .build();
    }

    /** Classify asset GL into maturity weight profile based on GL name heuristics. */
    private double[] classifyAssetWeights(String glName) {
        String upper = glName.toUpperCase();
        if (upper.contains("CASH") || upper.contains("VAULT") || upper.contains("ATM")) {
            return ASSET_WEIGHTS[0]; // Cash
        }
        if (upper.contains("LOAN") || upper.contains("ADVANCE") || upper.contains("RECEIVABLE")) {
            return ASSET_WEIGHTS[1]; // Loans
        }
        if (upper.contains("FIXED") || upper.contains("PROPERTY") || upper.contains("EQUIPMENT")) {
            return ASSET_WEIGHTS[2]; // Fixed assets
        }
        return ASSET_WEIGHTS[3]; // Other
    }

    /** Classify liability GL into maturity weight profile. */
    private double[] classifyLiabilityWeights(String glName) {
        String upper = glName.toUpperCase();
        if (upper.contains("SAVING") || upper.contains("CURRENT") || upper.contains("DEMAND")) {
            return LIABILITY_WEIGHTS[0]; // Demand deposits
        }
        if (upper.contains("TERM") || upper.contains("FIXED") || upper.contains("DEPOSIT")) {
            return LIABILITY_WEIGHTS[1]; // Term deposits
        }
        if (upper.contains("BORROW") || upper.contains("PAYABLE")) {
            return LIABILITY_WEIGHTS[2]; // Borrowings
        }
        return LIABILITY_WEIGHTS[0]; // Default to demand deposit profile
    }
}
