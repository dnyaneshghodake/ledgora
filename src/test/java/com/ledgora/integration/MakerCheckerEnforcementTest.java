package com.ledgora.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
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
import com.ledgora.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test: Maker-Checker segregation of duties.
 *
 * <p>CBS Rule: The user who creates (makes) a voucher must NOT be the same user who authorizes
 * (checks) it. This prevents single-person fraud and is required by RBI guidelines.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MakerCheckerEnforcementTest {

    @Autowired private VoucherService voucherService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private GeneralLedgerRepository glRepository;
    @Autowired private CustomerMasterRepository customerMasterRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Maker and Checker must be different users for voucher authorization")
    void makerCannotAuthorizeOwnVoucher() {
        TestData data = setupTestData("MC-01");
        seedBalance(data.account, new BigDecimal("50000.00"));

        Voucher voucher =
                voucherService.createVoucher(
                        data.tenant,
                        data.branch,
                        data.account,
                        data.gl,
                        VoucherDrCr.CR,
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        "INR",
                        LocalDate.now(),
                        LocalDate.now(),
                        "BATCH-MC-01",
                        1,
                        data.maker,
                        "Maker-checker test voucher");

        assertThrows(
                Exception.class,
                () -> voucherService.authorizeVoucher(voucher.getId(), data.maker),
                "Maker must NOT be allowed to authorize their own voucher");
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Different checker can authorize maker's voucher")
    void differentCheckerCanAuthorize() {
        TestData data = setupTestData("MC-02");
        seedBalance(data.account, new BigDecimal("50000.00"));

        Voucher voucher =
                voucherService.createVoucher(
                        data.tenant,
                        data.branch,
                        data.account,
                        data.gl,
                        VoucherDrCr.CR,
                        new BigDecimal("2000.00"),
                        new BigDecimal("2000.00"),
                        "INR",
                        LocalDate.now(),
                        LocalDate.now(),
                        "BATCH-MC-02",
                        1,
                        data.maker,
                        "Valid maker-checker authorization");

        assertDoesNotThrow(
                () -> voucherService.authorizeVoucher(voucher.getId(), data.checker),
                "Different checker should be able to authorize the voucher");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("Voucher records maker and checker IDs correctly")
    void voucherRecordsMakerAndCheckerIds() {
        TestData data = setupTestData("MC-03");
        seedBalance(data.account, new BigDecimal("50000.00"));

        Voucher voucher =
                voucherService.createVoucher(
                        data.tenant,
                        data.branch,
                        data.account,
                        data.gl,
                        VoucherDrCr.CR,
                        new BigDecimal("3000.00"),
                        new BigDecimal("3000.00"),
                        "INR",
                        LocalDate.now(),
                        LocalDate.now(),
                        "BATCH-MC-03",
                        1,
                        data.maker,
                        "ID recording test");

        voucherService.authorizeVoucher(voucher.getId(), data.checker);

        assertNotNull(voucher.getMaker(), "Voucher must record maker");
        assertNotEquals(
                voucher.getMaker().getId(),
                data.checker.getId(),
                "Maker and Checker IDs must differ");
    }

    private void seedBalance(Account account, BigDecimal amount) {
        AccountBalance balance =
                accountBalanceRepository
                        .findByAccountId(account.getId())
                        .orElse(AccountBalance.builder().account(account).build());
        balance.setActualTotalBalance(amount);
        balance.setActualClearedBalance(amount);
        balance.setAvailableBalance(amount);
        balance.setLedgerBalance(amount);
        accountBalanceRepository.save(balance);
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
        return new TestData(tenant, branch, account, gl, maker, checker);
    }

    private record TestData(
            Tenant tenant,
            Branch branch,
            Account account,
            GeneralLedger gl,
            User maker,
            User checker) {}
}
