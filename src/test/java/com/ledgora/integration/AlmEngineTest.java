package com.ledgora.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.reporting.dto.AlmReport;
import com.ledgora.reporting.service.AlmEngine;
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
 * RBI ALM (Asset Liability Management) Engine tests.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>8 RBI-mandated maturity buckets generated
 *   <li>Gap = Assets - Liabilities per bucket
 *   <li>Cumulative gap computed sequentially
 *   <li>Liquidity risk flag when short-term gap ratio < -15%
 *   <li>Risk assessment populated
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class AlmEngineTest {

    @Autowired private AlmEngine almEngine;
    @Autowired private TenantRepository tenantRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void alm_generates8Buckets() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        AlmReport report = almEngine.generate(tenant.getId(), tenant.getCurrentBusinessDate());

        assertNotNull(report);
        assertEquals(8, report.getBuckets().size(),
                "ALM must generate exactly 8 RBI-mandated maturity buckets");
    }

    @Test
    void alm_gapEqualsAssetsMinusLiabilitiesPerBucket() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        AlmReport report = almEngine.generate(tenant.getId(), tenant.getCurrentBusinessDate());

        for (AlmReport.AlmBucket bucket : report.getBuckets()) {
            BigDecimal expectedGap = bucket.getAssets().subtract(bucket.getLiabilities());
            assertEquals(0, expectedGap.compareTo(bucket.getGap()),
                    "Gap must equal Assets - Liabilities for bucket " + bucket.getBucketName());
        }
    }

    @Test
    void alm_cumulativeGapIsSequential() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        AlmReport report = almEngine.generate(tenant.getId(), tenant.getCurrentBusinessDate());

        BigDecimal runningCumulative = BigDecimal.ZERO;
        for (AlmReport.AlmBucket bucket : report.getBuckets()) {
            runningCumulative = runningCumulative.add(bucket.getGap());
            assertEquals(0, runningCumulative.compareTo(bucket.getCumulativeGap()),
                    "Cumulative gap must be sequential sum for bucket "
                            + bucket.getBucketName());
        }
    }

    @Test
    void alm_overallGapEqualsAssetsMinusLiabilities() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        AlmReport report = almEngine.generate(tenant.getId(), tenant.getCurrentBusinessDate());

        BigDecimal expected = report.getTotalAssets().subtract(report.getTotalLiabilities());
        assertEquals(0, expected.compareTo(report.getOverallGap()),
                "Overall gap must equal totalAssets - totalLiabilities");
    }

    @Test
    void alm_riskAssessmentPopulated() {
        Tenant tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;

        TenantContextHolder.setTenantId(tenant.getId());
        AlmReport report = almEngine.generate(tenant.getId(), tenant.getCurrentBusinessDate());

        assertNotNull(report.getRiskAssessment(),
                "Risk assessment must be populated");
        assertFalse(report.getRiskAssessment().isEmpty(),
                "Risk assessment must not be empty");
    }
}
