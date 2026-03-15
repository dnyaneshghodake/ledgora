package com.ledgora.reporting.service;

import com.ledgora.common.enums.GLAccountType;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.reporting.dto.CrarReport;
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
 * RBI Basel III — Capital to Risk-weighted Assets Ratio (CRAR) Engine.
 *
 * <p>RBI Master Circular on Basel III Capital Regulations (DBR.No.BP.BC.1/21.06.201/2015-16):
 *
 * <ul>
 *   <li>Minimum CRAR: 9% (RBI, vs Basel III global minimum of 8%)
 *   <li>Minimum Tier 1 Capital Ratio: 7%
 *   <li>Capital Conservation Buffer (CCB): 2.5%
 * </ul>
 *
 * <p>Capital Computation:
 *
 * <ul>
 *   <li>Tier 1 (CET1 + AT1) = Equity Capital + Retained Earnings
 *   <li>Tier 2 = General Provisions (capped at 1.25% of RWA)
 *   <li>Total Capital = Tier 1 + Tier 2
 * </ul>
 *
 * <p>Risk-Weighted Assets (Standardised Approach — simplified):
 *
 * <ul>
 *   <li>Cash / Government Securities: 0% risk weight
 *   <li>Bank deposits: 20% risk weight
 *   <li>Standard loans (performing): 100% risk weight
 *   <li>NPA loans: 150% risk weight
 *   <li>Fixed assets: 100% risk weight
 * </ul>
 *
 * <p>All computations use GL closing balances derived from immutable ledger entries.
 */
@Service
public class CrarEngine {

    private static final Logger log = LoggerFactory.getLogger(CrarEngine.class);

    /** RBI minimum CRAR: 9% (stricter than Basel III's 8%). */
    private static final BigDecimal MIN_CRAR = new BigDecimal("9.00");

    /** RBI minimum Tier 1 ratio: 7%. */
    private static final BigDecimal MIN_TIER1 = new BigDecimal("7.00");

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final GeneralLedgerRepository glRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public CrarEngine(
            GeneralLedgerRepository glRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.glRepository = glRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Compute CRAR for a tenant as of a business date.
     *
     * @return CrarReport with capital ratios and compliance status
     */
    @Transactional(readOnly = true)
    public CrarReport compute(Long tenantId, LocalDate asOfDate) {
        List<GeneralLedger> allGl = glRepository.findByTenantIdOrShared(tenantId);

        BigDecimal equityCapital = BigDecimal.ZERO;
        BigDecimal retainedEarnings = BigDecimal.ZERO;
        BigDecimal generalProvisions = BigDecimal.ZERO;
        BigDecimal totalRwa = BigDecimal.ZERO;
        List<CrarReport.RwaLine> rwaBreakdown = new ArrayList<>();

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

            // ── Capital Classification ──
            if (type == GLAccountType.EQUITY) {
                String glName = gl.getGlName().toUpperCase();
                if (glName.contains("RETAINED") || glName.contains("EARNINGS")) {
                    retainedEarnings = retainedEarnings.add(balance);
                } else {
                    equityCapital = equityCapital.add(balance);
                }
            }

            // Provisions count as Tier 2 capital (general provisions only)
            if (type == GLAccountType.EXPENSE
                    && gl.getGlName().toUpperCase().contains("PROVISION")) {
                generalProvisions = generalProvisions.add(balance);
            }

            // ── Risk-Weighted Assets (Asset GLs only) ──
            if (type == GLAccountType.ASSET && balance.compareTo(BigDecimal.ZERO) > 0) {
                String glName = gl.getGlName().toUpperCase();
                BigDecimal riskWeight = determineRiskWeight(glName, gl.getGlCode());
                BigDecimal rwa =
                        balance.multiply(riskWeight).divide(HUNDRED, 4, RoundingMode.HALF_UP);

                totalRwa = totalRwa.add(rwa);

                rwaBreakdown.add(
                        CrarReport.RwaLine.builder()
                                .glCode(gl.getGlCode())
                                .glName(gl.getGlName())
                                .assetClass(classifyAsset(glName))
                                .exposure(balance)
                                .riskWeight(riskWeight)
                                .rwa(rwa)
                                .build());
            }
        }

        // ── Capital Computation ──
        BigDecimal tier1 = equityCapital.add(retainedEarnings);
        // Tier 2: general provisions capped at 1.25% of RWA
        BigDecimal tier2Cap =
                totalRwa.multiply(new BigDecimal("1.25")).divide(HUNDRED, 4, RoundingMode.HALF_UP);
        BigDecimal tier2 = generalProvisions.min(tier2Cap);
        BigDecimal totalCapital = tier1.add(tier2);

        // ── Ratios ──
        BigDecimal crarPercent = BigDecimal.ZERO;
        BigDecimal tier1Percent = BigDecimal.ZERO;
        if (totalRwa.compareTo(BigDecimal.ZERO) > 0) {
            crarPercent = totalCapital.multiply(HUNDRED).divide(totalRwa, 2, RoundingMode.HALF_UP);
            tier1Percent = tier1.multiply(HUNDRED).divide(totalRwa, 2, RoundingMode.HALF_UP);
        }

        boolean meetsCrar = crarPercent.compareTo(MIN_CRAR) >= 0;
        boolean meetsTier1 = tier1Percent.compareTo(MIN_TIER1) >= 0;

        String compliance;
        if (meetsCrar && meetsTier1) {
            compliance = "COMPLIANT — meets RBI minimum CRAR (9%) and Tier 1 (7%)";
        } else if (!meetsCrar) {
            compliance =
                    "NON-COMPLIANT — CRAR "
                            + crarPercent
                            + "% below RBI minimum 9%. "
                            + "Prompt Corrective Action (PCA) framework may apply.";
        } else {
            compliance = "NON-COMPLIANT — Tier 1 ratio " + tier1Percent + "% below RBI minimum 7%.";
        }

        log.info(
                "CRAR computed for tenant {} date {}: CRAR={}% Tier1={}% RWA={} Capital={}",
                tenantId, asOfDate, crarPercent, tier1Percent, totalRwa, totalCapital);

        return CrarReport.builder()
                .reportDate(asOfDate)
                .tenantId(tenantId)
                .tier1Capital(tier1)
                .tier2Capital(tier2)
                .totalCapital(totalCapital)
                .equityCapital(equityCapital)
                .retainedEarnings(retainedEarnings)
                .generalProvisions(generalProvisions)
                .totalRwa(totalRwa)
                .rwaBreakdown(rwaBreakdown)
                .crarPercent(crarPercent)
                .tier1Percent(tier1Percent)
                .meetsMinimumCrar(meetsCrar)
                .meetsMinimumTier1(meetsTier1)
                .complianceStatus(compliance)
                .build();
    }

    /**
     * Determine risk weight per RBI Standardised Approach (simplified).
     *
     * <p>Full implementation would use external ratings (CRISIL/ICRA/CARE) and asset-class-specific
     * tables. This basic model uses GL name heuristics.
     */
    private BigDecimal determineRiskWeight(String glName, String glCode) {
        if (glName.contains("CASH") || glName.contains("GOVERNMENT") || glName.contains("RBI")) {
            return BigDecimal.ZERO; // 0% — sovereign/cash
        }
        if (glName.contains("BANK") || glName.contains("INTER-BANK")) {
            return new BigDecimal("20"); // 20% — bank exposures
        }
        if (glName.contains("NPA") || glName.contains("NON-PERFORMING")) {
            return new BigDecimal("150"); // 150% — NPA
        }
        if (glName.contains("LOAN") || glName.contains("ADVANCE")) {
            return HUNDRED; // 100% — standard loans
        }
        if (glName.contains("FIXED") || glName.contains("PROPERTY")) {
            return HUNDRED; // 100% — fixed assets
        }
        // Default: 100% risk weight (conservative)
        return HUNDRED;
    }

    private String classifyAsset(String glName) {
        if (glName.contains("CASH") || glName.contains("RBI")) return "SOVEREIGN";
        if (glName.contains("BANK")) return "BANK_EXPOSURE";
        if (glName.contains("NPA")) return "NPA_ASSET";
        if (glName.contains("LOAN") || glName.contains("ADVANCE")) return "STANDARD_LOAN";
        if (glName.contains("FIXED") || glName.contains("PROPERTY")) return "FIXED_ASSET";
        return "OTHER";
    }
}
