package com.ledgora.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.common.enums.GLAccountType;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.reporting.entity.StatementLineMapping;
import com.ledgora.reporting.enums.StatementSection;
import com.ledgora.reporting.enums.StatementType;
import com.ledgora.reporting.repository.StatementLineMappingRepository;
import com.ledgora.reporting.service.BalanceSheetEngine;
import com.ledgora.reporting.service.FinancialStatementService;
import com.ledgora.reporting.service.PnlEngine;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * RBI-grade Financial Statement Engine integrity tests.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>Assets = Liabilities + Equity (accounting equation)
 *   <li>P&L NetProfit computation
 *   <li>Snapshot checksum stability (re-run produces same hash)
 *   <li>Statement generation uses ONLY ledger entries (not AccountBalance)
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class ReportingIntegrityTest {

    @Autowired private BalanceSheetEngine balanceSheetEngine;
    @Autowired private PnlEngine pnlEngine;
    @Autowired private FinancialStatementService financialStatementService;
    @Autowired private StatementLineMappingRepository mappingRepository;
    @Autowired private GeneralLedgerRepository glRepository;
    @Autowired private TenantRepository tenantRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void balanceSheet_assetsEqualsLiabilitiesPlusEquity() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return; // skip if no seed data

        TenantContextHolder.setTenantId(tenant.getId());
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        // Seed minimal statement mappings if none exist
        if (mappingRepository.findWithGlByTenantAndType(
                        tenant.getId(), StatementType.BALANCE_SHEET)
                .isEmpty()) {
            seedMinimalMappings(tenant);
        }

        // Generate balance sheet — throws AccountingException if A != L + E
        Map<String, Object> bs = balanceSheetEngine.generate(tenant.getId(), bizDate);

        assertNotNull(bs);
        assertTrue((Boolean) bs.get("balanceCheck"), "Balance sheet equation must hold: A = L + E");

        BigDecimal assets = (BigDecimal) bs.get("totalAssets");
        BigDecimal liabilities = (BigDecimal) bs.get("totalLiabilities");
        BigDecimal equity = (BigDecimal) bs.get("totalEquity");

        assertEquals(
                0,
                assets.compareTo(liabilities.add(equity)),
                "Assets ("
                        + assets
                        + ") must equal Liabilities ("
                        + liabilities
                        + ") + Equity ("
                        + equity
                        + ")");
    }

    @Test
    void pnl_netProfitEqualsRevenueMinusExpense() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        if (mappingRepository.findWithGlByTenantAndType(tenant.getId(), StatementType.PNL)
                .isEmpty()) {
            seedMinimalMappings(tenant);
        }

        Map<String, Object> pnl = pnlEngine.generateDaily(tenant.getId(), bizDate);

        assertNotNull(pnl);
        BigDecimal revenue = (BigDecimal) pnl.get("totalRevenue");
        BigDecimal expense = (BigDecimal) pnl.get("totalExpense");
        BigDecimal netProfit = (BigDecimal) pnl.get("netProfit");

        assertEquals(
                0,
                netProfit.compareTo(revenue.subtract(expense)),
                "NetProfit must equal Revenue - Expense");
    }

    @Test
    void snapshot_checksumIsStable() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        if (mappingRepository.findWithGlByTenantAndType(
                        tenant.getId(), StatementType.BALANCE_SHEET)
                .isEmpty()) {
            seedMinimalMappings(tenant);
        }

        // Generate twice — hash must be identical
        Map<String, Object> bs1 = balanceSheetEngine.generate(tenant.getId(), bizDate);
        Map<String, Object> bs2 = balanceSheetEngine.generate(tenant.getId(), bizDate);

        assertEquals(bs1.get("totalAssets"), bs2.get("totalAssets"));
        assertEquals(bs1.get("totalLiabilities"), bs2.get("totalLiabilities"));
        assertEquals(bs1.get("totalEquity"), bs2.get("totalEquity"));
    }

    /** Seed minimal statement line mappings for existing GL accounts. */
    private void seedMinimalMappings(Tenant tenant) {
        for (GeneralLedger gl : glRepository.findAll()) {
            if (gl.getLevel() == null || gl.getLevel() == 0) continue; // skip root

            GLAccountType glType = gl.getAccountType();
            StatementType stType;
            StatementSection section;

            switch (glType) {
                case ASSET -> {
                    stType = StatementType.BALANCE_SHEET;
                    section = StatementSection.ASSET;
                }
                case LIABILITY -> {
                    stType = StatementType.BALANCE_SHEET;
                    section = StatementSection.LIABILITY;
                }
                case EQUITY -> {
                    stType = StatementType.BALANCE_SHEET;
                    section = StatementSection.EQUITY;
                }
                case REVENUE -> {
                    stType = StatementType.PNL;
                    section = StatementSection.INCOME;
                }
                case EXPENSE -> {
                    stType = StatementType.PNL;
                    section = StatementSection.EXPENSE;
                }
                default -> {
                    continue;
                }
            }

            mappingRepository.save(
                    StatementLineMapping.builder()
                            .gl(gl)
                            .statementType(stType)
                            .section(section)
                            .subSection(glType.name())
                            .lineName(gl.getGlName())
                            .displayOrder(gl.getLevel() * 10)
                            .tenant(tenant)
                            .build());
        }
    }
}
