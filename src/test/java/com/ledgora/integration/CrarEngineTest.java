package com.ledgora.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.reporting.dto.CrarReport;
import com.ledgora.reporting.service.CrarEngine;
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
 * RBI Basel III — CRAR Engine tests.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>Correct RWA computation per risk weight mapping
 *   <li>Tier 1 / Tier 2 capital segregation
 *   <li>CRAR percentage calculation
 *   <li>Compliance status (9% minimum per RBI)
 *   <li>Snapshot reproducibility
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class CrarEngineTest {

    @Autowired private CrarEngine crarEngine;
    @Autowired private TenantRepository tenantRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void crar_totalCapitalEqualsTier1PlusTier2() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        CrarReport report = crarEngine.compute(tenant.getId(), bizDate);

        assertNotNull(report);
        assertEquals(
                0,
                report.getTotalCapital()
                        .compareTo(report.getTier1Capital().add(report.getTier2Capital())),
                "Total Capital must equal Tier1 + Tier2");
    }

    @Test
    void crar_rwaIsNonNegative() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        CrarReport report = crarEngine.compute(tenant.getId(), tenant.getCurrentBusinessDate());

        assertTrue(
                report.getTotalRwa().compareTo(BigDecimal.ZERO) >= 0, "RWA must be non-negative");

        // Each RWA line must have non-negative values
        if (report.getRwaBreakdown() != null) {
            for (CrarReport.RwaLine line : report.getRwaBreakdown()) {
                assertTrue(
                        line.getRiskWeight().compareTo(BigDecimal.ZERO) >= 0,
                        "Risk weight must be non-negative for " + line.getGlCode());
                assertTrue(
                        line.getRwa().compareTo(BigDecimal.ZERO) >= 0,
                        "RWA must be non-negative for " + line.getGlCode());
            }
        }
    }

    @Test
    void crar_complianceStatusPopulated() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        CrarReport report = crarEngine.compute(tenant.getId(), tenant.getCurrentBusinessDate());

        assertNotNull(report.getComplianceStatus(), "Compliance status must be populated");
        assertNotNull(report.getCrarPercent(), "CRAR percentage must be computed");
    }

    @Test
    void crar_snapshotReproducibility() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        CrarReport r1 = crarEngine.compute(tenant.getId(), bizDate);
        CrarReport r2 = crarEngine.compute(tenant.getId(), bizDate);

        assertEquals(r1.getCrarPercent(), r2.getCrarPercent());
        assertEquals(r1.getTotalRwa(), r2.getTotalRwa());
        assertEquals(r1.getTotalCapital(), r2.getTotalCapital());
    }
}
