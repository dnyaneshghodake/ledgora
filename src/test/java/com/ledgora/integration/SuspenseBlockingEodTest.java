package com.ledgora.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.*;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.repository.CustomerMasterRepository;
import com.ledgora.eod.service.EodValidationService;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.suspense.entity.SuspenseCase;
import com.ledgora.suspense.repository.SuspenseCaseRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test: Suspense cases must block EOD.
 *
 * <p>CBS Invariant: EOD cannot proceed if there are unresolved suspense cases. Suspense GL must net
 * to zero before end-of-day processing can complete.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SuspenseBlockingEodTest {

    @Autowired private EodValidationService eodValidationService;
    @Autowired private SuspenseCaseRepository suspenseCaseRepository;
    @Autowired private TransactionRepository transactionRepository;
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
    @DisplayName("EOD blocks when open suspense cases exist")
    void eodBlocksWithOpenSuspenseCases() {
        TestData data = setupTestData("SUSP-01");

        Transaction txn =
                transactionRepository.save(
                        Transaction.builder()
                                .transactionRef("SUSP-TXN-001")
                                .transactionType(TransactionType.DEPOSIT)
                                .status(TransactionStatus.COMPLETED)
                                .amount(new BigDecimal("5000.00"))
                                .currency("INR")
                                .channel(TransactionChannel.TELLER)
                                .destinationAccount(data.account)
                                .description("Suspense test txn")
                                .businessDate(LocalDate.now())
                                .valueDate(LocalDate.now())
                                .performedBy(data.user)
                                .maker(data.user)
                                .tenant(data.tenant)
                                .build());

        suspenseCaseRepository.save(
                SuspenseCase.builder()
                        .tenant(data.tenant)
                        .originalTransaction(txn)
                        .intendedAccount(data.account)
                        .suspenseAccount(data.suspenseAccount)
                        .amount(new BigDecimal("5000.00"))
                        .currency("INR")
                        .reasonCode("ACCOUNT_FROZEN")
                        .reasonDetail("Target account frozen during deposit")
                        .status("OPEN")
                        .businessDate(LocalDate.now())
                        .build());

        List<String> errors =
                eodValidationService.validateEod(data.tenant.getId(), LocalDate.now());

        boolean hasSuspenseError =
                errors.stream()
                        .anyMatch(
                                e ->
                                        e.toLowerCase().contains("suspense")
                                                || e.toLowerCase().contains("blocked"));
        assertTrue(
                hasSuspenseError,
                "EOD must be blocked when open suspense cases exist. Errors: " + errors);
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("EOD passes suspense check when all cases are resolved")
    void eodPassesWhenAllSuspenseCasesResolved() {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-SUSP-02")
                                .tenantName("Suspense Resolved Tenant")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        TenantContextHolder.setTenantId(tenant.getId());

        List<String> errors = eodValidationService.validateEod(tenant.getId(), LocalDate.now());

        boolean hasSuspenseError =
                errors.stream().anyMatch(e -> e.toLowerCase().contains("suspense"));
        assertFalse(
                hasSuspenseError, "EOD should not have suspense errors when no open cases exist");
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
                                .name("Branch " + suffix)
                                .isActive(true)
                                .build());

        User user =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .username("susp-user-" + suffix)
                                .password("password")
                                .fullName("Suspense User " + suffix)
                                .email("susp-" + suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        CustomerMaster cm =
                customerMasterRepository.save(
                        CustomerMaster.builder()
                                .tenant(tenant)
                                .customerNumber("C-" + suffix)
                                .firstName("Test")
                                .lastName("Customer")
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
                                .balance(new BigDecimal("50000.00"))
                                .currency("INR")
                                .branch(branch)
                                .homeBranch(branch)
                                .customerMaster(cm)
                                .customerNumber(cm.getCustomerNumber())
                                .build());

        Account suspenseAccount =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .accountNumber("SUSP-" + suffix)
                                .accountName("Suspense Account " + suffix)
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(BigDecimal.ZERO)
                                .currency("INR")
                                .branch(branch)
                                .homeBranch(branch)
                                .build());

        TenantContextHolder.setTenantId(tenant.getId());
        return new TestData(tenant, branch, user, account, suspenseAccount);
    }

    private record TestData(
            Tenant tenant, Branch branch, User user, Account account, Account suspenseAccount) {}
}
