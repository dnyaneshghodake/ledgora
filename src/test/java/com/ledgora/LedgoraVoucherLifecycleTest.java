package com.ledgora;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.balance.service.CbsBalanceEngine;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.repository.TransactionBatchRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.*;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.repository.CustomerMasterRepository;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.repository.VoucherRepository;
import com.ledgora.voucher.service.VoucherService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CBS Voucher Lifecycle test suite.
 * Tests: create, authorize, post, cancel voucher flow.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgoraVoucherLifecycleTest {

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Autowired private VoucherService voucherService;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private TransactionBatchRepository transactionBatchRepository;
    @Autowired private CbsBalanceEngine cbsBalanceEngine;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private GeneralLedgerRepository glRepository;
    @Autowired private CustomerMasterRepository customerMasterRepository;
    @Autowired private UserRepository userRepository;

    @Test @Order(1) @Transactional
    @DisplayName("Voucher: Create voucher updates shadow balance only")
    void testCreateVoucherUpdatesShadow() {
        TestData data = setupTestData("VCH-01");

        // Seed actual balance so debit validation passes
        AccountBalance bal = cbsBalanceEngine.getCbsBalance(data.account.getId());
        bal.setActualTotalBalance(new BigDecimal("10000.0000"));
        bal.setActualClearedBalance(new BigDecimal("10000.0000"));
        bal.setAvailableBalance(new BigDecimal("10000.0000"));
        bal.setLedgerBalance(new BigDecimal("10000.0000"));
        accountBalanceRepository.save(bal);

        Voucher voucher = voucherService.createVoucher(
                data.tenant, data.branch, data.account, data.gl,
                VoucherDrCr.DR, new BigDecimal("1000.0000"), new BigDecimal("1000.0000"),
                "INR", LocalDate.now(), LocalDate.now(), "BATCH-01", 1,
                data.maker, "Test debit voucher");

        assertNotNull(voucher.getId());
        assertEquals("N", voucher.getAuthFlag());
        assertEquals("N", voucher.getPostFlag());
        assertEquals("N", voucher.getCancelFlag());
        assertNotNull(voucher.getScrollNo());

        // Shadow should be updated
        AccountBalance balance = cbsBalanceEngine.getCbsBalance(data.account.getId());
        assertNotNull(balance);
        // Shadow delta should reflect the debit (negative for debit)
        assertTrue(balance.getShadowTotalBalance().compareTo(BigDecimal.ZERO) != 0,
                "Shadow balance should be updated after voucher creation");
    }

    @Test @Order(2) @Transactional
    @DisplayName("Voucher: Authorize sets auth_flag = Y")
    void testAuthorizeVoucher() {
        TestData data = setupTestData("VCH-02");
        seedBalance(data.account.getId(), "10000.0000");

        Voucher voucher = voucherService.createVoucher(
                data.tenant, data.branch, data.account, data.gl,
                VoucherDrCr.CR, new BigDecimal("500.0000"), new BigDecimal("500.0000"),
                "INR", LocalDate.now(), LocalDate.now(), "BATCH-02", 1,
                data.maker, "Test credit voucher");

        Voucher authorized = voucherService.authorizeVoucher(voucher.getId(), data.checker);

        assertEquals("Y", authorized.getAuthFlag());
        assertNotNull(authorized.getChecker());
    }

    @Test @Order(3) @Transactional
    @DisplayName("Voucher: Post creates ledger entry and updates actual balance")
    void testPostVoucher() {
        TestData data = setupTestData("VCH-03");
        seedBalance(data.account.getId(), "10000.0000");

        Voucher voucher = voucherService.createVoucher(
                data.tenant, data.branch, data.account, data.gl,
                VoucherDrCr.CR, new BigDecimal("2000.0000"), new BigDecimal("2000.0000"),
                "INR", LocalDate.now(), LocalDate.now(), createOpenBatchCode(data.tenant, LocalDate.now()), 1,
                data.maker, "Test post voucher");

        voucherService.authorizeVoucher(voucher.getId(), data.checker);
        Voucher posted = voucherService.postVoucher(voucher.getId());

        assertEquals("Y", posted.getPostFlag());
        assertNotNull(posted.getLedgerEntry(), "Posted voucher should have a ledger entry");

        // Actual balance should be updated
        AccountBalance balance = cbsBalanceEngine.getCbsBalance(data.account.getId());
        assertTrue(balance.getActualTotalBalance().compareTo(new BigDecimal("10000.0000")) > 0,
                "Actual balance should increase after credit post");
    }

    @Test @Order(4) @Transactional
    @DisplayName("Voucher: Cannot post unauthorized voucher")
    void testCannotPostUnauthorizedVoucher() {
        TestData data = setupTestData("VCH-04");
        seedBalance(data.account.getId(), "10000.0000");

        Voucher voucher = voucherService.createVoucher(
                data.tenant, data.branch, data.account, data.gl,
                VoucherDrCr.CR, new BigDecimal("500.0000"), new BigDecimal("500.0000"),
                "INR", LocalDate.now(), LocalDate.now(), "BATCH-04", 1,
                data.maker, "Unauth test");

        assertThrows(RuntimeException.class, () ->
                voucherService.postVoucher(voucher.getId()),
                "Should not be able to post an unauthorized voucher");
    }

    @Test @Order(5) @Transactional
    @DisplayName("Voucher: Cancel creates reversal and marks cancel_flag = Y")
    void testCancelVoucher() {
        TestData data = setupTestData("VCH-05");
        seedBalance(data.account.getId(), "10000.0000");

        Voucher voucher = voucherService.createVoucher(
                data.tenant, data.branch, data.account, data.gl,
                VoucherDrCr.CR, new BigDecimal("1000.0000"), new BigDecimal("1000.0000"),
                "INR", LocalDate.now(), LocalDate.now(), createOpenBatchCode(data.tenant, LocalDate.now()), 1,
                data.maker, "Cancel test");

        voucherService.authorizeVoucher(voucher.getId(), data.checker);
        voucherService.postVoucher(voucher.getId());

        Voucher reversal = voucherService.cancelVoucher(voucher.getId(), data.maker, "Testing reversal");

        assertNotNull(reversal.getId());
        assertNotNull(reversal.getReversalOfVoucher());

        Voucher original = voucherRepository.findById(voucher.getId()).orElseThrow();
        assertEquals("Y", original.getCancelFlag(), "Original voucher cancel_flag should be Y");
    }

    @Test @Order(6) @Transactional
    @DisplayName("Voucher: Cannot authorize already authorized voucher")
    void testCannotDoubleAuthorize() {
        TestData data = setupTestData("VCH-06");
        seedBalance(data.account.getId(), "10000.0000");

        Voucher voucher = voucherService.createVoucher(
                data.tenant, data.branch, data.account, data.gl,
                VoucherDrCr.CR, new BigDecimal("100.0000"), new BigDecimal("100.0000"),
                "INR", LocalDate.now(), LocalDate.now(), "BATCH-06", 1,
                data.maker, "Double auth test");

        voucherService.authorizeVoucher(voucher.getId(), data.checker);

        assertThrows(RuntimeException.class, () ->
                voucherService.authorizeVoucher(voucher.getId(), data.checker),
                "Should not be able to authorize an already authorized voucher");
    }

    @Test @Order(7) @Transactional
    @DisplayName("Voucher: Cannot cancel already cancelled voucher")
    void testCannotDoubleCancelVoucher() {
        TestData data = setupTestData("VCH-07");
        seedBalance(data.account.getId(), "10000.0000");

        Voucher voucher = voucherService.createVoucher(
                data.tenant, data.branch, data.account, data.gl,
                VoucherDrCr.CR, new BigDecimal("300.0000"), new BigDecimal("300.0000"),
                "INR", LocalDate.now(), LocalDate.now(), createOpenBatchCode(data.tenant, LocalDate.now()), 1,
                data.maker, "Double cancel test");

        voucherService.authorizeVoucher(voucher.getId(), data.checker);
        voucherService.postVoucher(voucher.getId());
        voucherService.cancelVoucher(voucher.getId(), data.maker, "First cancel");

        assertThrows(RuntimeException.class, () ->
                voucherService.cancelVoucher(voucher.getId(), data.maker, "Second cancel"),
                "Should not be able to cancel an already cancelled voucher");
    }

    @Test @Order(8) @Transactional
    @DisplayName("Voucher: Scroll numbers auto-increment per tenant/branch/date")
    void testScrollNumberAutoIncrement() {
        TestData data = setupTestData("VCH-08");
        seedBalance(data.account.getId(), "50000.0000");

        Voucher v1 = voucherService.createVoucher(
                data.tenant, data.branch, data.account, data.gl,
                VoucherDrCr.CR, new BigDecimal("100.0000"), new BigDecimal("100.0000"),
                "INR", LocalDate.now(), LocalDate.now(), "BATCH-08", 1,
                data.maker, "Scroll test 1");

        Voucher v2 = voucherService.createVoucher(
                data.tenant, data.branch, data.account, data.gl,
                VoucherDrCr.CR, new BigDecimal("200.0000"), new BigDecimal("200.0000"),
                "INR", LocalDate.now(), LocalDate.now(), "BATCH-08", 1,
                data.maker, "Scroll test 2");

        assertTrue(v2.getScrollNo() > v1.getScrollNo(),
                "Scroll numbers should auto-increment");
    }

    // ═══════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════

    private void seedBalance(Long accountId, String amount) {
        AccountBalance bal = cbsBalanceEngine.getCbsBalance(accountId);
        BigDecimal amt = new BigDecimal(amount);
        bal.setActualTotalBalance(amt);
        bal.setActualClearedBalance(amt);
        bal.setAvailableBalance(amt);
        bal.setLedgerBalance(amt);
        accountBalanceRepository.save(bal);
    }


    private String createOpenBatchCode(Tenant tenant, LocalDate businessDate) {
        TransactionBatch openBatch = transactionBatchRepository.save(TransactionBatch.builder()
                .tenant(tenant)
                .batchType(BatchType.BATCH)
                .businessDate(businessDate)
                .status(BatchStatus.OPEN)
                .build());
        // Set batchCode after save (needs the generated ID), matching BatchService behavior
        openBatch.setBatchCode("BATCH-" + openBatch.getId());
        openBatch = transactionBatchRepository.save(openBatch);
        return openBatch.getBatchCode();
    }

    private TestData setupTestData(String suffix) {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .tenantCode("T-" + suffix)
                .tenantName("Test Tenant " + suffix)
                .status("ACTIVE")
                .currentBusinessDate(LocalDate.now())
                .dayStatus(DayStatus.OPEN)
                .build());

        Branch branch = branchRepository.save(Branch.builder()
                .branchCode("B-" + suffix)
                .name("Test Branch " + suffix)
                .isActive(true)
                .build());

        CustomerMaster cm = customerMasterRepository.save(CustomerMaster.builder()
                .tenant(tenant)
                .customerNumber("C-" + suffix)
                .firstName("Test")
                .lastName("User")
                .status(CustomerStatus.ACTIVE)
                .makerCheckerStatus(MakerCheckerStatus.APPROVED)
                .build());

        Account account = accountRepository.save(Account.builder()
                .tenant(tenant)
                .accountNumber("A-" + suffix)
                .accountName("Test Account " + suffix)
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .branch(branch)
                .homeBranch(branch)
                .customerMaster(cm)
                .customerNumber(cm.getCustomerNumber())
                .build());

        GeneralLedger gl = glRepository.save(GeneralLedger.builder()
                .glCode("GL-" + suffix)
                .glName("Test GL " + suffix)
                .description("Test")
                .accountType(GLAccountType.LIABILITY)
                .level(0)
                .isActive(true)
                .normalBalance("CREDIT")
                .balance(BigDecimal.ZERO)
                .build());

        User maker = userRepository.save(User.builder()
                .tenant(tenant)
                .username("maker-" + suffix)
                .password("password")
                .fullName("Test Maker " + suffix)
                .email("maker-" + suffix + "@test.com")
                .isActive(true)
                .isLocked(false)
                .build());

        User checker = userRepository.save(User.builder()
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

    private record TestData(Tenant tenant, Branch branch, Account account,
                            GeneralLedger gl, CustomerMaster cm, User maker, User checker) {}
}
