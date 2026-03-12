package com.ledgora;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.balance.service.CbsBalanceEngine;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.*;
import com.ledgora.customer.entity.CustomerFreezeControl;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.repository.CustomerFreezeControlRepository;
import com.ledgora.customer.repository.CustomerMasterRepository;
import com.ledgora.customer.service.CbsCustomerValidationService;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.repository.VoucherRepository;
import com.ledgora.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS Multi-Tenant Isolation test suite. Tests: tenant isolation for vouchers, balances, customers,
 * GL.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgoraMultiTenantIsolationTest {

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Autowired private VoucherService voucherService;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private CbsBalanceEngine cbsBalanceEngine;
    @Autowired private CbsCustomerValidationService validationService;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private GeneralLedgerRepository glRepository;
    @Autowired private CustomerMasterRepository customerMasterRepository;
    @Autowired private CustomerFreezeControlRepository freezeControlRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Multi-Tenant: Cross-tenant account access blocked")
    void testCrossTenantAccessBlocked() {
        // Setup tenant 1
        Tenant tenant1 = createTenant("MT-ISO-01A");
        Branch branch1 = createBranch("BRMT01A");
        CustomerMaster cm1 = createCustomer(tenant1, "CM-MT01A");
        Account account1 = createAccount(tenant1, branch1, cm1, "ACC-MT01A");

        // Setup tenant 2
        Tenant tenant2 = createTenant("MT-ISO-01B");

        // Try to validate tenant1's account with tenant2's context
        assertThrows(
                RuntimeException.class,
                () ->
                        validationService.validateAccountForTransaction(
                                account1, tenant2.getId(), branch1.getId(), VoucherDrCr.DR),
                "Cross-tenant account access should be blocked");
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Multi-Tenant: Each tenant has independent vouchers")
    void testTenantVoucherIsolation() {
        TestData data1 = setupFullTestData("MTI-02A");
        TestData data2 = setupFullTestData("MTI-02B");
        TenantContextHolder.setTenantId(data1.tenant.getId());
        seedBalance(data1.account.getId(), "50000.0000");
        TenantContextHolder.setTenantId(data2.tenant.getId());
        seedBalance(data2.account.getId(), "50000.0000");

        // Switch context to tenant 1 before creating voucher for tenant 1
        TenantContextHolder.setTenantId(data1.tenant.getId());

        // Create voucher for tenant 1
        Voucher v1 =
                voucherService.createVoucher(
                        data1.tenant,
                        data1.branch,
                        data1.account,
                        data1.gl,
                        VoucherDrCr.CR,
                        new BigDecimal("1000.0000"),
                        new BigDecimal("1000.0000"),
                        "INR",
                        LocalDate.now(),
                        LocalDate.now(),
                        "BATCH-MT",
                        1,
                        data1.maker,
                        "Tenant 1 voucher");

        // Voucher should not be visible to tenant 2
        assertTrue(
                voucherRepository.findByIdAndTenantId(v1.getId(), data2.tenant.getId()).isEmpty(),
                "Tenant 2 should not see Tenant 1's voucher");

        // Voucher should be visible to tenant 1
        assertTrue(
                voucherRepository.findByIdAndTenantId(v1.getId(), data1.tenant.getId()).isPresent(),
                "Tenant 1 should see its own voucher");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("Multi-Tenant: Each tenant has independent balances")
    void testTenantBalanceIsolation() {
        TestData data1 = setupFullTestData("MTI-03A");
        TestData data2 = setupFullTestData("MTI-03B");

        // Update balance for tenant 1's account — set correct tenant context first
        TenantContextHolder.setTenantId(data1.tenant.getId());
        cbsBalanceEngine.updateActualBalance(
                data1.account.getId(), new BigDecimal("10000.0000"), VoucherDrCr.CR);

        // Tenant 1's account should have 10000
        AccountBalance bal1 = cbsBalanceEngine.getCbsBalance(data1.account.getId());
        assertEquals(0, new BigDecimal("10000.0000").compareTo(bal1.getActualTotalBalance()));

        // Tenant 2's account should still be 0 — set correct tenant context
        TenantContextHolder.setTenantId(data2.tenant.getId());
        AccountBalance bal2 = cbsBalanceEngine.getCbsBalance(data2.account.getId());
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(bal2.getActualTotalBalance()),
                "Tenant 2's balance should be unaffected by Tenant 1's operations");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("Multi-Tenant: Customer freeze is tenant-scoped")
    void testTenantFreezeIsolation() {
        Tenant tenant1 = createTenant("MT-ISO-04A");
        Tenant tenant2 = createTenant("MT-ISO-04B");
        Branch branch = createBranch("BRMT04");

        CustomerMaster cm1 = createCustomer(tenant1, "CM-MT04A");
        CustomerMaster cm2 = createCustomer(tenant2, "CM-MT04B");

        // Freeze tenant 1's customer
        freezeControlRepository.save(
                CustomerFreezeControl.builder()
                        .tenant(tenant1)
                        .customerMaster(cm1)
                        .debitFreeze(true)
                        .debitFreezeReason("Tenant 1 freeze")
                        .creditFreeze(false)
                        .build());

        Account acc1 = createAccount(tenant1, branch, cm1, "ACC-MT04A");
        Account acc2 = createAccount(tenant2, branch, cm2, "ACC-MT04B");

        // Tenant 1's account should be frozen for debit
        assertThrows(
                RuntimeException.class,
                () ->
                        validationService.validateAccountForTransaction(
                                acc1, tenant1.getId(), branch.getId(), VoucherDrCr.DR));

        // Tenant 2's account should NOT be frozen
        assertDoesNotThrow(
                () ->
                        validationService.validateAccountForTransaction(
                                acc2, tenant2.getId(), branch.getId(), VoucherDrCr.DR));
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("Multi-Tenant: Voucher list is tenant-scoped")
    void testVoucherListTenantScoped() {
        TestData data1 = setupFullTestData("MTI-05A");
        TestData data2 = setupFullTestData("MTI-05B");
        seedBalance(data1.account.getId(), "50000.0000");
        seedBalance(data2.account.getId(), "50000.0000");

        // Switch context to tenant 1 before creating voucher
        TenantContextHolder.setTenantId(data1.tenant.getId());
        voucherService.createVoucher(
                data1.tenant,
                data1.branch,
                data1.account,
                data1.gl,
                VoucherDrCr.CR,
                new BigDecimal("100.0000"),
                new BigDecimal("100.0000"),
                "INR",
                LocalDate.now(),
                LocalDate.now(),
                "BATCH-MT",
                1,
                data1.maker,
                "T1 voucher");

        // Switch context to tenant 2 before creating voucher
        TenantContextHolder.setTenantId(data2.tenant.getId());
        voucherService.createVoucher(
                data2.tenant,
                data2.branch,
                data2.account,
                data2.gl,
                VoucherDrCr.CR,
                new BigDecimal("200.0000"),
                new BigDecimal("200.0000"),
                "INR",
                LocalDate.now(),
                LocalDate.now(),
                "BATCH-MT",
                1,
                data2.maker,
                "T2 voucher");

        List<Voucher> t1Vouchers =
                voucherRepository.findByTenantIdAndBranchIdAndPostingDate(
                        data1.tenant.getId(), data1.branch.getId(), LocalDate.now());
        List<Voucher> t2Vouchers =
                voucherRepository.findByTenantIdAndBranchIdAndPostingDate(
                        data2.tenant.getId(), data2.branch.getId(), LocalDate.now());

        assertEquals(1, t1Vouchers.size(), "Tenant 1 should see only its voucher");
        assertEquals(1, t2Vouchers.size(), "Tenant 2 should see only its voucher");
    }

    @Test
    @Order(6)
    @Transactional
    @DisplayName("Multi-Tenant: Customer master is tenant-scoped")
    void testCustomerMasterTenantScoped() {
        Tenant tenant1 = createTenant("MT-ISO-06A");
        Tenant tenant2 = createTenant("MT-ISO-06B");

        // Same customer number in different tenants should be allowed
        createCustomer(tenant1, "SHARED-CUST-001");
        createCustomer(tenant2, "SHARED-CUST-001");

        // Each tenant should find only its own customer
        assertTrue(
                customerMasterRepository
                        .findByTenantIdAndCustomerNumber(tenant1.getId(), "SHARED-CUST-001")
                        .isPresent());
        assertTrue(
                customerMasterRepository
                        .findByTenantIdAndCustomerNumber(tenant2.getId(), "SHARED-CUST-001")
                        .isPresent());

        // Cross-tenant lookup should not find the other tenant's customer
        var t1Customers = customerMasterRepository.findByTenantId(tenant1.getId());
        var t2Customers = customerMasterRepository.findByTenantId(tenant2.getId());
        assertEquals(1, t1Customers.size());
        assertEquals(1, t2Customers.size());
    }

    // ═══════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════

    private Tenant createTenant(String code) {
        return tenantRepository.save(
                Tenant.builder()
                        .tenantCode(code)
                        .tenantName("Test Bank " + code)
                        .status("ACTIVE")
                        .currentBusinessDate(LocalDate.now())
                        .dayStatus(DayStatus.OPEN)
                        .build());
    }

    private Branch createBranch(String code) {
        return branchRepository
                .findByBranchCode(code)
                .orElseGet(
                        () ->
                                branchRepository.save(
                                        Branch.builder()
                                                .branchCode(code)
                                                .name("Test Branch " + code)
                                                .isActive(true)
                                                .build()));
    }

    private CustomerMaster createCustomer(Tenant tenant, String custNo) {
        return customerMasterRepository.save(
                CustomerMaster.builder()
                        .tenant(tenant)
                        .customerNumber(custNo)
                        .firstName("Test")
                        .lastName("Customer")
                        .status(CustomerStatus.ACTIVE)
                        .makerCheckerStatus(MakerCheckerStatus.APPROVED)
                        .build());
    }

    private Account createAccount(Tenant tenant, Branch branch, CustomerMaster cm, String accNo) {
        return accountRepository.save(
                Account.builder()
                        .tenant(tenant)
                        .accountNumber(accNo)
                        .accountName("Test Account " + accNo)
                        .accountType(AccountType.SAVINGS)
                        .status(AccountStatus.ACTIVE)
                        .balance(BigDecimal.ZERO)
                        .currency("INR")
                        .branch(branch)
                        .homeBranch(branch)
                        .customerMaster(cm)
                        .customerNumber(cm.getCustomerNumber())
                        .build());
    }

    private void seedBalance(Long accountId, String amount) {
        AccountBalance bal = cbsBalanceEngine.getCbsBalance(accountId);
        BigDecimal amt = new BigDecimal(amount);
        bal.setActualTotalBalance(amt);
        bal.setActualClearedBalance(amt);
        bal.setAvailableBalance(amt);
        bal.setLedgerBalance(amt);
        accountBalanceRepository.save(bal);
    }

    private TestData setupFullTestData(String suffix) {
        Tenant tenant = createTenant("T-" + suffix);
        // Branch codes limited to 10 chars
        String branchCode = suffix.length() > 8 ? suffix.substring(suffix.length() - 8) : suffix;
        Branch branch = createBranch("B" + branchCode);
        CustomerMaster cm = createCustomer(tenant, "C-" + suffix);
        Account account = createAccount(tenant, branch, cm, "A-" + suffix);

        GeneralLedger gl =
                glRepository.save(
                        GeneralLedger.builder()
                                .glCode("GL-" + suffix)
                                .glName("Test GL " + suffix)
                                .description("Test")
                                .accountType(GLAccountType.LIABILITY)
                                .level(0)
                                .isActive(true)
                                .normalBalance("CREDIT")
                                .balance(BigDecimal.ZERO)
                                .build());

        User maker =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .username("maker-" + suffix)
                                .password("password")
                                .fullName("Test Maker " + suffix)
                                .email("maker-" + suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        User checker =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .username("checker-" + suffix)
                                .password("password")
                                .fullName("Test Checker " + suffix)
                                .email("checker-" + suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        TenantContextHolder.setTenantId(tenant.getId());
        return new TestData(tenant, branch, account, gl, cm, maker, checker);
    }

    private record TestData(
            Tenant tenant,
            Branch branch,
            Account account,
            GeneralLedger gl,
            CustomerMaster cm,
            User maker,
            User checker) {}
}
