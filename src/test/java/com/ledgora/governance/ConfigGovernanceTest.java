package com.ledgora.governance;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.governance.entity.ConfigChangeRequest;
import com.ledgora.governance.service.ConfigGovernanceService;
import com.ledgora.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS Tier-1 Integration Tests: Config Governance.
 *
 * <p>Verifies that:
 *
 * <ol>
 *   <li>Config change requests are created in PENDING status
 *   <li>Maker-checker is enforced (same user cannot approve own request)
 *   <li>Tenant isolation is enforced on governance operations
 * </ol>
 *
 * <p>Each test is {@code @Transactional} + {@code @Rollback} to prevent PENDING config change
 * requests written by one test from leaking into subsequent tests and making
 * {@code countPending_reflectsSubmittedRequests} non-deterministic.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class ConfigGovernanceTest {

    @Autowired private ConfigGovernanceService governanceService;

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Config change request is created in PENDING status")
    @WithMockUser(
            username = "maker1",
            roles = {"MAKER", "ADMIN"})
    void submitChange_createsPendingRequest() {
        TenantContextHolder.setTenantId(1L);

        ConfigChangeRequest request =
                governanceService.submitChange(
                        "APPROVAL_POLICY",
                        "ApprovalPolicy",
                        100L,
                        "Increase teller limit to 500000",
                        "{\"maxAmount\": 100000}",
                        "{\"maxAmount\": 500000}",
                        "maxAmount",
                        null);

        assertNotNull(request.getId());
        assertEquals(ApprovalStatus.PENDING, request.getStatus());
        assertEquals("APPROVAL_POLICY", request.getConfigType());
        assertEquals("{\"maxAmount\": 500000}", request.getNewValue());
    }

    @Test
    @DisplayName("Maker cannot approve own config change request")
    @WithMockUser(
            username = "maker1",
            roles = {"MAKER", "ADMIN"})
    void approve_ownRequest_throws() {
        TenantContextHolder.setTenantId(1L);

        ConfigChangeRequest request =
                governanceService.submitChange(
                        "HARD_LIMIT",
                        "HardTransactionLimit",
                        200L,
                        "Increase hard ceiling",
                        null,
                        "{\"absoluteMaxAmount\": 10000000}",
                        "absoluteMaxAmount",
                        null);

        // Same user tries to approve — should throw maker-checker violation
        RuntimeException ex =
                assertThrows(
                        RuntimeException.class,
                        () -> governanceService.approve(request.getId(), "Self-approved"),
                        "Maker cannot approve own config change");
        assertTrue(
                ex.getMessage().contains("maker-checker")
                        || ex.getMessage().contains("approve your own"),
                "Exception must be a maker-checker violation, not a null-identity error. Got: "
                        + ex.getMessage());
    }

    @Test
    @DisplayName("Config governance throws when no tenant context is set")
    @WithMockUser(
            username = "maker1",
            roles = {"MAKER"})
    void submitChange_noTenantContext_throws() {
        TenantContextHolder.clear();
        assertThrows(
                IllegalStateException.class,
                () ->
                        governanceService.submitChange(
                                "GL_ACCOUNT",
                                "GeneralLedger",
                                null,
                                "Create new GL",
                                null,
                                "{}",
                                null,
                                null),
                "Must throw when tenant context is not set");
    }

    @Test
    @DisplayName("Pending count reflects submitted requests")
    @WithMockUser(
            username = "maker1",
            roles = {"MAKER", "ADMIN"})
    void countPending_reflectsSubmittedRequests() {
        TenantContextHolder.setTenantId(1L);
        long beforeCount = governanceService.countPending();

        governanceService.submitChange(
                "VELOCITY_LIMIT", "VelocityLimit", 300L, "Test change", null, "{}", null, null);

        long afterCount = governanceService.countPending();
        assertEquals(beforeCount + 1, afterCount, "Pending count should increment by 1");
    }

    @Test
    @DisplayName("Unresolvable user identity throws on submitChange")
    @WithMockUser(username = "nonexistent-user-xyz", roles = {"MAKER"})
    void submitChange_unknownUser_throws() {
        TenantContextHolder.setTenantId(1L);
        RuntimeException ex =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                governanceService.submitChange(
                                        "HARD_LIMIT",
                                        "HardTransactionLimit",
                                        null,
                                        "Test",
                                        null,
                                        "{}",
                                        null,
                                        null),
                        "Must throw when user cannot be resolved from DB");
        assertTrue(
                ex.getMessage().contains("identity could not be resolved"),
                "Exception must indicate identity resolution failure. Got: " + ex.getMessage());
    }
}
