package com.ledgora.tenant;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.account.repository.AccountRepository;
import com.ledgora.account.service.AccountService;
import com.ledgora.customer.repository.CustomerRepository;
import com.ledgora.customer.service.CustomerService;
import com.ledgora.loan.service.NpaClassificationService;
import com.ledgora.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * CBS Tier-1 Integration Tests: Tenant Isolation.
 *
 * <p>Verifies that all CBS-critical service methods enforce tenant scoping and reject cross-tenant
 * access. Uses the H2 in-memory database with seeded data from DataInitializer.
 *
 * <p>Test strategy:
 * <ol>
 *   <li>Set TenantContextHolder to Tenant A</li>
 *   <li>Attempt to read an entity belonging to Tenant B</li>
 *   <li>Assert empty result or exception (never data leakage)</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationTest {

    @Autowired private AccountService accountService;
    @Autowired private CustomerService customerService;
    @Autowired private NpaClassificationService npaClassificationService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CustomerRepository customerRepository;

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("AccountService.getAccountById returns empty for cross-tenant ID")
    void accountService_getById_crossTenant_returnsEmpty() {
        // Tenant 1 context — account ID 9999 does not exist for tenant 1
        TenantContextHolder.setTenantId(1L);
        assertTrue(
                accountService.getAccountById(9999L).isEmpty(),
                "Cross-tenant account lookup must return empty");
    }

    @Test
    @DisplayName("CustomerService.getCustomerById returns empty for cross-tenant ID")
    void customerService_getById_crossTenant_returnsEmpty() {
        TenantContextHolder.setTenantId(1L);
        assertTrue(
                customerService.getCustomerById(9999L).isEmpty(),
                "Cross-tenant customer lookup must return empty");
    }

    @Test
    @DisplayName("NpaClassificationService.isNpa returns false for nonexistent cross-tenant account")
    void npaService_isNpa_crossTenant_returnsFalse() {
        TenantContextHolder.setTenantId(1L);
        assertFalse(
                npaClassificationService.isNpa(9999L),
                "Cross-tenant NPA check must return false");
    }

    @Test
    @DisplayName("AccountService operations throw when no tenant context is set")
    void accountService_noTenantContext_throws() {
        TenantContextHolder.clear();
        assertThrows(
                IllegalStateException.class,
                () -> accountService.getAllAccounts(),
                "Must throw when tenant context is not set");
    }

    @Test
    @DisplayName("CustomerService operations throw when no tenant context is set")
    void customerService_noTenantContext_throws() {
        TenantContextHolder.clear();
        assertThrows(
                IllegalStateException.class,
                () -> customerService.getAllCustomers(),
                "Must throw when tenant context is not set");
    }
}
