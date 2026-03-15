package com.ledgora;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.approval.entity.ApprovalRequest;
import com.ledgora.approval.repository.ApprovalRequestRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.balance.service.CbsBalanceService;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.repository.TransactionBatchRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.*;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.repository.CustomerMasterRepository;
import com.ledgora.eod.service.EodValidationService;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
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
 * CBS EOD Validation test suite. Tests: EOD blocks incomplete data, validates all conditions before
 * closing.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgoraEodValidationTest {

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Autowired private EodValidationService eodValidationService;
    @Autowired private VoucherService voucherService;
    @Autowired private ApprovalRequestRepository approvalRequestRepository;
    @Autowired private TransactionBatchRepository transactionBatchRepository;
    @Autowired private CbsBalanceService cbsBalanceEngine;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private GeneralLedgerRepository glRepository;
    @Autowired private CustomerMasterRepository customerMasterRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @Order(1)
    @Transactional
    @DisplayName("EOD: Validation passes when no vouchers exist (clean state)")
    void testEodValidationPassesCleanState() {
        Tenant tenant = createTestTenant("EOD-01");
        List<String> errors = eodValidationService.validateEod(tenant.getId(), LocalDate.now());
        assertTrue(errors.isEmpty(), "EOD validation should pass with no vouchers: " + errors);
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("EOD: Blocks when unauthorized vouchers exist")
    void testEodBlocksUnauthorizedVouchers() {
        TestData data = setupTestData("EOD-02");
        seedBalance(data.account.getId(), "50000.0000");

        voucherService.createVoucher(
                data.tenant,
                data.branch,
                data.account,
                data.gl,
                VoucherDrCr.CR,
                new BigDecimal("1000.0000"),
                new BigDecimal("1000.0000"),
                "INR",
                LocalDate.now(),
                LocalDate.now(),
                "BATCH-EOD",
                1,
                data.maker,
                "Unauthorized voucher");

        List<String> errors =
                eodValidationService.validateEod(data.tenant.getId(), LocalDate.now());
        assertFalse(errors.isEmpty(), "EOD should be blocked with unauthorized vouchers");
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("unauthorized")),
                "Error should mention unauthorized vouchers");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("EOD: Blocks when unposted vouchers exist")
    void testEodBlocksUnpostedVouchers() {
        TestData data = setupTestData("EOD-03");
        seedBalance(data.account.getId(), "50000.0000");

        var voucher =
                voucherService.createVoucher(
                        data.tenant,
                        data.branch,
                        data.account,
                        data.gl,
                        VoucherDrCr.CR,
                        new BigDecimal("1000.0000"),
                        new BigDecimal("1000.0000"),
                        "INR",
                        LocalDate.now(),
                        LocalDate.now(),
                        "BATCH-EOD",
                        1,
                        data.maker,
                        "Unposted voucher");
        voucherService.authorizeVoucher(voucher.getId(), data.checker);

        List<String> errors =
                eodValidationService.validateEod(data.tenant.getId(), LocalDate.now());
        assertFalse(errors.isEmpty(), "EOD should be blocked with unposted vouchers");
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("unposted")),
                "Error should mention unposted vouchers");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("EOD: Passes voucher checks when all vouchers are authorized and posted")
    void testEodPassesVoucherChecksAllPosted() {
        TestData data = setupTestData("EOD-04");
        seedBalance(data.account.getId(), "50000.0000");

        String batchCode = createOpenBatchCode(data.tenant, LocalDate.now());
        var voucher =
                voucherService.createVoucher(
                        data.tenant,
                        data.branch,
                        data.account,
                        data.gl,
                        VoucherDrCr.CR,
                        new BigDecimal("1000.0000"),
                        new BigDecimal("1000.0000"),
                        "INR",
                        LocalDate.now(),
                        LocalDate.now(),
                        batchCode,
                        1,
                        data.maker,
                        "Fully posted voucher");
        voucherService.authorizeVoucher(voucher.getId(), data.checker);
        voucherService.postVoucher(voucher.getId());

        List<String> errors =
                eodValidationService.validateEod(data.tenant.getId(), LocalDate.now());
        boolean hasVoucherErrors =
                errors.stream().anyMatch(e -> e.contains("unauthorized") || e.contains("unposted"));
        assertFalse(hasVoucherErrors, "No voucher-related EOD errors expected when all posted");
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("EOD: runEod blocks on validation failure")
    void testRunEodBlocksOnFailure() {
        TestData data = setupTestData("EOD-05");
        seedBalance(data.account.getId(), "50000.0000");

        voucherService.createVoucher(
                data.tenant,
                data.branch,
                data.account,
                data.gl,
                VoucherDrCr.CR,
                new BigDecimal("500.0000"),
                new BigDecimal("500.0000"),
                "INR",
                LocalDate.now(),
                LocalDate.now(),
                "BATCH-EOD",
                1,
                data.maker,
                "Block EOD test");

        assertThrows(
                RuntimeException.class,
                () -> eodValidationService.runEod(data.tenant.getId()),
                "runEod should throw when validation fails");
    }

    @Test
    @Order(6)
    @Transactional
    @DisplayName("EOD: canRunEod returns false when blocked")
    void testCanRunEodReturnsFalse() {
        TestData data = setupTestData("EOD-06");
        seedBalance(data.account.getId(), "50000.0000");

        voucherService.createVoucher(
                data.tenant,
                data.branch,
                data.account,
                data.gl,
                VoucherDrCr.CR,
                new BigDecimal("500.0000"),
                new BigDecimal("500.0000"),
                "INR",
                LocalDate.now(),
                LocalDate.now(),
                "BATCH-EOD",
                1,
                data.maker,
                "canRunEod test");

        assertFalse(
                eodValidationService.canRunEod(data.tenant.getId()),
                "canRunEod should return false when unauthorized vouchers exist");
    }

    @Test
    @Order(7)
    @Transactional
    @DisplayName("EOD: Blocks when ledger debits and credits are not balanced")
    void testEodBlocksLedgerImbalance() {
        TestData data = setupTestData("EOD-07");
        seedBalance(data.account.getId(), "50000.0000");

        String batchCode = createOpenBatchCode(data.tenant, LocalDate.now());
        var voucher =
                voucherService.createVoucher(
                        data.tenant,
                        data.branch,
                        data.account,
                        data.gl,
                        VoucherDrCr.CR,
                        new BigDecimal("1000.0000"),
                        new BigDecimal("1000.0000"),
                        "INR",
                        LocalDate.now(),
                        LocalDate.now(),
                        batchCode,
                        1,
                        data.maker,
                        "Ledger imbalance check");
        voucherService.authorizeVoucher(voucher.getId(), data.checker);
        voucherService.postVoucher(voucher.getId());

        List<String> errors =
                eodValidationService.validateEod(data.tenant.getId(), LocalDate.now());
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("Ledger integrity check failed")),
                "EOD should fail when tenant debits and credits are not balanced");
    }

    @Test
    @Order(8)
    @Transactional
    @DisplayName("EOD: Blocks when pending approvals exist")
    void testEodBlocksPendingApprovals() {
        TestData data = setupTestData("EOD-08");

        approvalRequestRepository.save(
                ApprovalRequest.builder()
                        .tenant(data.tenant)
                        .entityType("TRANSACTION")
                        .entityId(999L)
                        .requestedBy(data.maker)
                        .status(ApprovalStatus.PENDING)
                        .remarks("Pending approval before EOD")
                        .build());

        List<String> errors =
                eodValidationService.validateEod(data.tenant.getId(), LocalDate.now());
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("pending approval")),
                "EOD should fail when pending approvals exist");
    }

    @Test
    @Order(9)
    @Transactional
    @DisplayName("Voucher posting requires OPEN batch")
    void testVoucherPostingRequiresOpenBatch() {
        TestData data = setupTestData("EOD-09");
        seedBalance(data.account.getId(), "50000.0000");

        TransactionBatch closedBatch =
                transactionBatchRepository.save(
                        TransactionBatch.builder()
                                .tenant(data.tenant)
                                .batchType(BatchType.BATCH)
                                .businessDate(LocalDate.now())
                                .status(BatchStatus.CLOSED)
                                .build());
        closedBatch.setBatchCode("BATCH-" + closedBatch.getId());
        closedBatch = transactionBatchRepository.save(closedBatch);

        var voucher =
                voucherService.createVoucher(
                        data.tenant,
                        data.branch,
                        data.account,
                        data.gl,
                        VoucherDrCr.CR,
                        new BigDecimal("1000.0000"),
                        new BigDecimal("1000.0000"),
                        "INR",
                        LocalDate.now(),
                        LocalDate.now(),
                        closedBatch.getBatchCode(),
                        1,
                        data.maker,
                        "Closed batch should block post");
        voucherService.authorizeVoucher(voucher.getId(), data.checker);

        RuntimeException ex =
                assertThrows(
                        RuntimeException.class, () -> voucherService.postVoucher(voucher.getId()));
        assertTrue(ex.getMessage().contains("must be OPEN"));
    }

    private Tenant createTestTenant(String suffix) {
        return tenantRepository.save(
                Tenant.builder()
                        .tenantCode("T-" + suffix)
                        .tenantName("Test Tenant " + suffix)
                        .status("ACTIVE")
                        .currentBusinessDate(LocalDate.now())
                        .dayStatus(DayStatus.OPEN)
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

    private String createOpenBatchCode(Tenant tenant, LocalDate businessDate) {
        TransactionBatch openBatch =
                transactionBatchRepository.save(
                        TransactionBatch.builder()
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
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-" + suffix)
                                .tenantName("Test Tenant " + suffix)
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Branch branch =
                branchRepository.save(
                        Branch.builder()
                                .branchCode("B-" + suffix)
                                .name("Test Branch " + suffix)
                                .isActive(true)
                                .build());

        CustomerMaster cm =
                customerMasterRepository.save(
                        CustomerMaster.builder()
                                .tenant(tenant)
                                .customerNumber("C-" + suffix)
                                .firstName("Test")
                                .lastName("User")
                                .status(CustomerStatus.ACTIVE)
                                .makerCheckerStatus(MakerCheckerStatus.APPROVED)
                                .build());

        Account account =
                accountRepository.save(
                        Account.builder()
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
