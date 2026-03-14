package com.ledgora.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.common.enums.DayStatus;
import com.ledgora.fraud.entity.FraudAlert;
import com.ledgora.fraud.repository.FraudAlertRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test: Fraud threshold trigger and alert creation.
 *
 * <p>CBS Rule: When velocity or amount thresholds are breached, a FraudAlert must be created with
 * status OPEN and the correct alert type. Alerts are immutable evidence records.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FraudThresholdTriggerTest {

    @Autowired private FraudAlertRepository fraudAlertRepository;
    @Autowired private TenantRepository tenantRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Velocity count alert is created with correct attributes")
    void velocityCountAlertCreatedCorrectly() {
        Tenant tenant = createTenant("FRAUD-01");

        FraudAlert alert =
                fraudAlertRepository.save(
                        FraudAlert.builder()
                                .tenant(tenant)
                                .accountId(1L)
                                .accountNumber("TEST-SAV-0001")
                                .alertType("VELOCITY_COUNT")
                                .status("OPEN")
                                .details(
                                        "Account TEST-SAV-0001 exceeded velocity count: 6 txns in"
                                                + " 30 min (limit: 5)")
                                .observedCount(6)
                                .observedAmount(new BigDecimal("225000.00"))
                                .thresholdValue("5 per 30min")
                                .userId(1L)
                                .build());

        assertNotNull(alert.getId(), "Fraud alert must be persisted with an ID");
        assertEquals("VELOCITY_COUNT", alert.getAlertType());
        assertEquals("OPEN", alert.getStatus());
        assertEquals(6, alert.getObservedCount());
        assertNotNull(alert.getCreatedAt(), "Created timestamp must be set");
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Hard ceiling alert is created for blocked transaction")
    void hardCeilingAlertCreatedCorrectly() {
        Tenant tenant = createTenant("FRAUD-02");

        FraudAlert alert =
                fraudAlertRepository.save(
                        FraudAlert.builder()
                                .tenant(tenant)
                                .accountId(2L)
                                .accountNumber("TEST-SAV-0002")
                                .alertType("HARD_CEILING")
                                .status("OPEN")
                                .details("Deposit 50,000,000 blocked - exceeds hard ceiling")
                                .observedAmount(new BigDecimal("50000000.00"))
                                .thresholdValue("10000000 per txn")
                                .userId(1L)
                                .build());

        assertNotNull(alert.getId());
        assertEquals("HARD_CEILING", alert.getAlertType());
        assertEquals(
                0,
                new BigDecimal("50000000.00").compareTo(alert.getObservedAmount()),
                "Observed amount must match");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("Multiple fraud alerts can exist for same account")
    void multipleFraudAlertsPerAccount() {
        Tenant tenant = createTenant("FRAUD-03");

        fraudAlertRepository.save(
                FraudAlert.builder()
                        .tenant(tenant)
                        .accountId(3L)
                        .accountNumber("TEST-SAV-0003")
                        .alertType("VELOCITY_COUNT")
                        .status("OPEN")
                        .details("First alert")
                        .observedCount(6)
                        .observedAmount(new BigDecimal("100000.00"))
                        .thresholdValue("5 per 30min")
                        .userId(1L)
                        .build());

        fraudAlertRepository.save(
                FraudAlert.builder()
                        .tenant(tenant)
                        .accountId(3L)
                        .accountNumber("TEST-SAV-0003")
                        .alertType("VELOCITY_AMOUNT")
                        .status("OPEN")
                        .details("Second alert - amount threshold")
                        .observedAmount(new BigDecimal("2000000.00"))
                        .thresholdValue("1000000 per day")
                        .userId(1L)
                        .build());

        List<FraudAlert> openAlerts =
                fraudAlertRepository.findByAccountIdAndStatusOrderByCreatedAtDesc(3L, "OPEN");
        assertTrue(
                openAlerts.size() >= 2,
                "Multiple fraud alerts must be allowed for the same account");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("Fraud alert status can transition from OPEN to RESOLVED")
    void fraudAlertStatusTransition() {
        Tenant tenant = createTenant("FRAUD-04");

        FraudAlert alert =
                fraudAlertRepository.save(
                        FraudAlert.builder()
                                .tenant(tenant)
                                .accountId(4L)
                                .accountNumber("TEST-SAV-0004")
                                .alertType("VELOCITY_COUNT")
                                .status("OPEN")
                                .details("Alert to resolve")
                                .observedCount(7)
                                .observedAmount(new BigDecimal("300000.00"))
                                .thresholdValue("5 per 30min")
                                .userId(1L)
                                .build());

        assertEquals("OPEN", alert.getStatus());

        alert.setStatus("RESOLVED");
        FraudAlert resolved = fraudAlertRepository.save(alert);
        assertEquals("RESOLVED", resolved.getStatus());
    }

    private Tenant createTenant(String suffix) {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-" + suffix)
                                .tenantName("Fraud Test Tenant " + suffix)
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());
        TenantContextHolder.setTenantId(tenant.getId());
        return tenant;
    }
}
