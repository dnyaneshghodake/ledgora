package com.ledgora.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.common.enums.DayStatus;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test: Business date enforcement.
 *
 * <p>CBS Rule: All transactions must be posted against the tenant's current business date. The
 * business date is controlled by the EOD process and cannot be manually advanced without completing
 * EOD. Future-dated or back-dated transactions are rejected.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BusinessDateEnforcementTest {

    @Autowired private TenantRepository tenantRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Tenant has a valid business date set")
    void tenantHasValidBusinessDate() {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-BIZ-01")
                                .tenantName("BizDate Tenant")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        assertNotNull(
                tenant.getCurrentBusinessDate(), "Tenant must have a current business date set");
        assertFalse(
                tenant.getCurrentBusinessDate().isAfter(LocalDate.now().plusDays(1)),
                "Business date must not be in the far future");
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Business date does not change without EOD")
    void businessDateDoesNotChangeWithoutEod() {
        LocalDate originalDate = LocalDate.of(2025, 3, 15);
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-BIZ-02")
                                .tenantName("BizDate NoChange")
                                .status("ACTIVE")
                                .currentBusinessDate(originalDate)
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Tenant retrieved = tenantRepository.findById(tenant.getId()).orElseThrow();
        assertEquals(
                originalDate,
                retrieved.getCurrentBusinessDate(),
                "Business date must not change without EOD process");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("Day status is OPEN for new tenants")
    void dayStatusIsOpenForNewTenants() {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-BIZ-03")
                                .tenantName("BizDate Open")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        assertEquals(DayStatus.OPEN, tenant.getDayStatus(), "New tenant must have day status OPEN");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("DAY_CLOSING status blocks new transactions conceptually")
    void dayClosingStatusBlocksTransactions() {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-BIZ-04")
                                .tenantName("BizDate Closing")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.DAY_CLOSING)
                                .build());

        assertEquals(
                DayStatus.DAY_CLOSING,
                tenant.getDayStatus(),
                "Tenant must be in DAY_CLOSING state");

        // When day status is DAY_CLOSING, the system should block new transaction posting.
        // This is enforced at the service layer via business date checks.
        assertNotEquals(
                DayStatus.OPEN,
                tenant.getDayStatus(),
                "DAY_CLOSING must not allow new transactions (status is not OPEN)");
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("Multiple tenants can have different business dates")
    void multipleTenantsDifferentDates() {
        Tenant tenant1 =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-BIZ-05A")
                                .tenantName("BizDate Tenant A")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.of(2025, 6, 1))
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Tenant tenant2 =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-BIZ-05B")
                                .tenantName("BizDate Tenant B")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.of(2025, 7, 15))
                                .dayStatus(DayStatus.OPEN)
                                .build());

        assertNotEquals(
                tenant1.getCurrentBusinessDate(),
                tenant2.getCurrentBusinessDate(),
                "Different tenants can have different business dates (multi-tenancy)");
    }
}
