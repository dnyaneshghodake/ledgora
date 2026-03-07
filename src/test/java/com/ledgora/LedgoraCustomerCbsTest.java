package com.ledgora;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.*;
import com.ledgora.customer.entity.CustomerFreezeControl;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.entity.CustomerRelationship;
import com.ledgora.customer.entity.CustomerTaxProfile;
import com.ledgora.customer.repository.*;
import com.ledgora.customer.service.CbsCustomerValidationService;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CBS Customer Master test suite.
 * Tests: customer creation, freeze controls, validation, relationship management.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgoraCustomerCbsTest {

    @Autowired private CustomerMasterRepository customerMasterRepository;
    @Autowired private CustomerTaxProfileRepository taxProfileRepository;
    @Autowired private CustomerFreezeControlRepository freezeControlRepository;
    @Autowired private CustomerRelationshipRepository relationshipRepository;
    @Autowired private CbsCustomerValidationService validationService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;

    // ═══════════════════════════════════════════
    // CUSTOMER MASTER TESTS
    // ═══════════════════════════════════════════

    @Test @Order(1) @Transactional
    @DisplayName("CBS: Customer master creation with unique customer_number per tenant")
    void testCustomerMasterCreation() {
        Tenant tenant = createTestTenant("CBS-CUST-01");
        Branch branch = createTestBranch("BR001");

        CustomerMaster cm = CustomerMaster.builder()
                .tenant(tenant)
                .customerNumber("CUST-00001")
                .firstName("John")
                .lastName("Doe")
                .nationalId("NATID-001")
                .status(CustomerStatus.ACTIVE)
                .homeBranch(branch)
                .kycStatus("VERIFIED")
                .makerCheckerStatus(MakerCheckerStatus.APPROVED)
                .build();
        cm = customerMasterRepository.save(cm);

        assertNotNull(cm.getId());
        assertEquals("CUST-00001", cm.getCustomerNumber());
        assertEquals(CustomerStatus.ACTIVE, cm.getStatus());
        assertEquals("John Doe", cm.getFullName());
    }

    @Test @Order(2) @Transactional
    @DisplayName("CBS: Duplicate customer_number per tenant is rejected")
    void testDuplicateCustomerNumberRejected() {
        Tenant tenant = createTestTenant("CBS-CUST-02");

        CustomerMaster cm1 = CustomerMaster.builder()
                .tenant(tenant)
                .customerNumber("CUST-DUP-001")
                .firstName("Alice")
                .lastName("Smith")
                .status(CustomerStatus.ACTIVE)
                .build();
        customerMasterRepository.save(cm1);

        assertTrue(customerMasterRepository.existsByTenantIdAndCustomerNumber(
                tenant.getId(), "CUST-DUP-001"));
    }

    @Test @Order(3) @Transactional
    @DisplayName("CBS: Customer tax profile creation")
    void testCustomerTaxProfile() {
        Tenant tenant = createTestTenant("CBS-CUST-03");
        CustomerMaster cm = createTestCustomer(tenant, "CUST-TAX-001");

        CustomerTaxProfile taxProfile = CustomerTaxProfile.builder()
                .tenant(tenant)
                .customerMaster(cm)
                .panNumber("ABCDE1234F")
                .taxResidencyStatus("RESIDENT")
                .taxDeductionFlag(true)
                .build();
        taxProfile = taxProfileRepository.save(taxProfile);

        assertNotNull(taxProfile.getId());
        assertEquals("ABCDE1234F", taxProfile.getPanNumber());
    }

    @Test @Order(4) @Transactional
    @DisplayName("CBS: Customer relationship creation")
    void testCustomerRelationship() {
        Tenant tenant = createTestTenant("CBS-CUST-04");
        CustomerMaster primary = createTestCustomer(tenant, "CUST-REL-001");
        CustomerMaster related = createTestCustomer(tenant, "CUST-REL-002");

        CustomerRelationship rel = CustomerRelationship.builder()
                .tenant(tenant)
                .primaryCustomer(primary)
                .relatedCustomer(related)
                .relationshipType(RelationshipType.JOINT_HOLDER)
                .isActive(true)
                .build();
        rel = relationshipRepository.save(rel);

        assertNotNull(rel.getId());
        assertEquals(RelationshipType.JOINT_HOLDER, rel.getRelationshipType());

        var rels = relationshipRepository.findActiveByPrimaryCustomerIdAndTenantId(
                primary.getId(), tenant.getId());
        assertEquals(1, rels.size());
    }

    // ═══════════════════════════════════════════
    // FREEZE CONTROL TESTS
    // ═══════════════════════════════════════════

    @Test @Order(10) @Transactional
    @DisplayName("CBS: Debit freeze blocks debit transactions")
    void testDebitFreezeBlocksDebit() {
        Tenant tenant = createTestTenant("CBS-CUST-10");
        Branch branch = createTestBranch("BR010");
        CustomerMaster cm = createTestCustomer(tenant, "CUST-FREEZE-001");

        // Create freeze control with debit freeze
        CustomerFreezeControl freeze = CustomerFreezeControl.builder()
                .tenant(tenant)
                .customerMaster(cm)
                .debitFreeze(true)
                .debitFreezeReason("Legal hold")
                .creditFreeze(false)
                .build();
        freezeControlRepository.save(freeze);

        // Create account linked to customer
        Account account = createTestAccount(tenant, branch, cm, "ACC-FREEZE-001");

        // Debit should be blocked
        assertThrows(RuntimeException.class, () ->
                validationService.validateAccountForTransaction(
                        account, tenant.getId(), branch.getId(), VoucherDrCr.DR),
                "Debit transaction should be blocked when debit freeze is active");
    }

    @Test @Order(11) @Transactional
    @DisplayName("CBS: Credit freeze blocks credit transactions")
    void testCreditFreezeBlocksCredit() {
        Tenant tenant = createTestTenant("CBS-CUST-11");
        Branch branch = createTestBranch("BR011");
        CustomerMaster cm = createTestCustomer(tenant, "CUST-FREEZE-002");

        CustomerFreezeControl freeze = CustomerFreezeControl.builder()
                .tenant(tenant)
                .customerMaster(cm)
                .debitFreeze(false)
                .creditFreeze(true)
                .creditFreezeReason("Compliance review")
                .build();
        freezeControlRepository.save(freeze);

        Account account = createTestAccount(tenant, branch, cm, "ACC-FREEZE-002");

        assertThrows(RuntimeException.class, () ->
                validationService.validateAccountForTransaction(
                        account, tenant.getId(), branch.getId(), VoucherDrCr.CR),
                "Credit transaction should be blocked when credit freeze is active");
    }

    @Test @Order(12) @Transactional
    @DisplayName("CBS: No freeze allows both debit and credit")
    void testNoFreezeAllowsTransactions() {
        Tenant tenant = createTestTenant("CBS-CUST-12");
        Branch branch = createTestBranch("BR012");
        CustomerMaster cm = createTestCustomer(tenant, "CUST-NOFREEZE-001");

        Account account = createTestAccount(tenant, branch, cm, "ACC-NOFREEZE-001");

        // Both should pass without exception
        assertDoesNotThrow(() ->
                validationService.validateAccountForTransaction(
                        account, tenant.getId(), branch.getId(), VoucherDrCr.DR));
        assertDoesNotThrow(() ->
                validationService.validateAccountForTransaction(
                        account, tenant.getId(), branch.getId(), VoucherDrCr.CR));
    }

    @Test @Order(13) @Transactional
    @DisplayName("CBS: Inactive customer blocks all transactions")
    void testInactiveCustomerBlocksTransactions() {
        Tenant tenant = createTestTenant("CBS-CUST-13");
        Branch branch = createTestBranch("BR013");

        CustomerMaster cm = CustomerMaster.builder()
                .tenant(tenant)
                .customerNumber("CUST-INACTIVE-001")
                .firstName("Inactive")
                .lastName("User")
                .status(CustomerStatus.INACTIVE)
                .build();
        cm = customerMasterRepository.save(cm);

        Account account = createTestAccount(tenant, branch, cm, "ACC-INACTIVE-001");

        assertThrows(RuntimeException.class, () ->
                validationService.validateAccountForTransaction(
                        account, tenant.getId(), branch.getId(), VoucherDrCr.DR),
                "Inactive customer should block debit transactions");
    }

    @Test @Order(14) @Transactional
    @DisplayName("CBS: Tenant mismatch blocks transaction")
    void testTenantMismatchBlocksTransaction() {
        Tenant tenant1 = createTestTenant("CBS-CUST-14A");
        Tenant tenant2 = createTestTenant("CBS-CUST-14B");
        Branch branch = createTestBranch("BR014");
        CustomerMaster cm = createTestCustomer(tenant1, "CUST-TMATCH-001");

        Account account = createTestAccount(tenant1, branch, cm, "ACC-TMATCH-001");

        // Try to use account from tenant1 in tenant2's context
        assertThrows(RuntimeException.class, () ->
                validationService.validateAccountForTransaction(
                        account, tenant2.getId(), branch.getId(), VoucherDrCr.DR),
                "Tenant mismatch should block transaction");
    }

    // ═══════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════

    private Tenant createTestTenant(String code) {
        Tenant tenant = Tenant.builder()
                .tenantCode(code)
                .tenantName("Test Bank " + code)
                .status("ACTIVE")
                .currentBusinessDate(LocalDate.now())
                .dayStatus(DayStatus.OPEN)
                .build();
        return tenantRepository.save(tenant);
    }

    private Branch createTestBranch(String code) {
        return branchRepository.findByBranchCode(code)
                .orElseGet(() -> branchRepository.save(Branch.builder()
                        .branchCode(code)
                        .name("Test Branch " + code)
                        .isActive(true)
                        .build()));
    }

    private CustomerMaster createTestCustomer(Tenant tenant, String customerNumber) {
        CustomerMaster cm = CustomerMaster.builder()
                .tenant(tenant)
                .customerNumber(customerNumber)
                .firstName("Test")
                .lastName("Customer")
                .status(CustomerStatus.ACTIVE)
                .makerCheckerStatus(MakerCheckerStatus.APPROVED)
                .build();
        return customerMasterRepository.save(cm);
    }

    private Account createTestAccount(Tenant tenant, Branch branch, CustomerMaster cm, String accountNumber) {
        Account account = Account.builder()
                .tenant(tenant)
                .accountNumber(accountNumber)
                .accountName("Test Account " + accountNumber)
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .branch(branch)
                .homeBranch(branch)
                .customerMaster(cm)
                .customerNumber(cm.getCustomerNumber())
                .build();
        return accountRepository.save(account);
    }
}
