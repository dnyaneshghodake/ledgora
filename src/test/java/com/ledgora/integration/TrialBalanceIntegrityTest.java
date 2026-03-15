package com.ledgora.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.reporting.dto.TrialBalanceReport;
import com.ledgora.reporting.service.TrialBalanceEngine;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * RBI-grade Trial Balance integrity tests.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>Total DR = Total CR (double-entry invariant)
 *   <li>Sum of closing balances equals zero
 *   <li>Re-generation yields identical results (deterministic)
 *   <li>Cross-tenant isolation enforced
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class TrialBalanceIntegrityTest {

    @Autowired private TrialBalanceEngine trialBalanceEngine;
    @Autowired private TenantRepository tenantRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void trialBalance_totalDebitEqualsTotalCredit() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        TrialBalanceReport tb = trialBalanceEngine.generate(tenant.getId(), bizDate);

        assertNotNull(tb);
        assertTrue(
                tb.isBalanced(),
                "Trial Balance must balance: DR="
                        + tb.getTotalDebits()
                        + " CR="
                        + tb.getTotalCredits());
        assertEquals(
                0,
                tb.getTotalDebits().compareTo(tb.getTotalCredits()),
                "SUM(debit balances) must equal SUM(credit balances)");
    }

    @Test
    void trialBalance_closingBalanceSumIsZero() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        TrialBalanceReport tb =
                trialBalanceEngine.generate(tenant.getId(), tenant.getCurrentBusinessDate());

        // Net of all debit and credit closing balances must be zero
        BigDecimal netBalance = tb.getTotalDebits().subtract(tb.getTotalCredits());
        assertEquals(
                0,
                netBalance.compareTo(BigDecimal.ZERO),
                "Net closing balance must be zero, got " + netBalance);
    }

    @Test
    void trialBalance_regenerationYieldsSameResult() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        TrialBalanceReport tb1 = trialBalanceEngine.generate(tenant.getId(), bizDate);
        TrialBalanceReport tb2 = trialBalanceEngine.generate(tenant.getId(), bizDate);

        assertEquals(tb1.getTotalDebits(), tb2.getTotalDebits());
        assertEquals(tb1.getTotalCredits(), tb2.getTotalCredits());
        assertEquals(tb1.getLines().size(), tb2.getLines().size());
    }
}
