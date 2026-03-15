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
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test: Idempotency key duplicate submission rejection.
 *
 * <p>CBS Rule: Duplicate clientReferenceId+channel combinations must be rejected to prevent
 * double-posting. This protects against network retries and client bugs.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IdempotencyDuplicateSubmissionTest {

    @Autowired private TransactionService transactionService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private GeneralLedgerRepository glRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @Order(1)
    @Transactional
    @DisplayName("First submission succeeds, duplicate with same clientReferenceId is rejected")
    void duplicateClientReferenceIdIsRejected() {
        TestData data = setupTestData("IDEM-01");

        TransactionDTO dto1 =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("5000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IDEM-UNIQUE-001")
                        .description("First deposit")
                        .narration("Idempotency test 1")
                        .build();

        assertDoesNotThrow(() -> transactionService.deposit(dto1), "First submission must succeed");

        TransactionDTO dto2 =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("5000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IDEM-UNIQUE-001")
                        .description("Duplicate deposit")
                        .narration("Idempotency test 2")
                        .build();

        assertThrows(
                Exception.class,
                () -> transactionService.deposit(dto2),
                "Duplicate clientReferenceId+channel must be rejected");
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Different clientReferenceId on same channel succeeds")
    void differentClientReferenceIdSucceeds() {
        TestData data = setupTestData("IDEM-02");

        TransactionDTO dto1 =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("3000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IDEM-DIFF-001")
                        .description("Deposit A")
                        .narration("Different ref 1")
                        .build();

        TransactionDTO dto2 =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("4000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IDEM-DIFF-002")
                        .description("Deposit B")
                        .narration("Different ref 2")
                        .build();

        assertDoesNotThrow(() -> transactionService.deposit(dto1));
        assertDoesNotThrow(
                () -> transactionService.deposit(dto2),
                "Different clientReferenceId should succeed on same channel");
    }

    private static LocalDate nextWeekday() {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek().getValue() > 5) d = d.plusDays(1);
        return d;
    }

    private TestData setupTestData(String suffix) {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("TEN-" + suffix)
                                .tenantName("Tenant " + suffix)
                                .status("ACTIVE")
                                .currentBusinessDate(nextWeekday())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Branch branch =
                branchRepository.save(
                        Branch.builder()
                                .branchCode("BR-" + suffix)
                                .name("Branch " + suffix)
                                .isActive(true)
                                .build());

        User user =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .username("idem-user-" + suffix)
                                .password("password")
                                .fullName("Idem User " + suffix)
                                .email("idem-" + suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        GeneralLedger glDeposit =
                glRepository.save(
                        GeneralLedger.builder()
                                .glCode("GL-IDEM-" + suffix)
                                .glName("Idem GL " + suffix)
                                .description("Idem GL")
                                .accountType(GLAccountType.LIABILITY)
                                .level(0)
                                .isActive(true)
                                .normalBalance("CREDIT")
                                .balance(BigDecimal.ZERO)
                                .build());

        glRepository
                .findByGlCode("1100")
                .orElseGet(
                        () ->
                                glRepository.save(
                                        GeneralLedger.builder()
                                                .glCode("1100")
                                                .glName("Cash GL")
                                                .description("Cash")
                                                .accountType(GLAccountType.ASSET)
                                                .level(0)
                                                .isActive(true)
                                                .normalBalance("DEBIT")
                                                .balance(BigDecimal.ZERO)
                                                .build()));

        Account cashGl =
                accountRepository
                        .findFirstByTenantIdAndGlAccountCode(tenant.getId(), "1100")
                        .orElseGet(
                                () ->
                                        accountRepository.save(
                                                Account.builder()
                                                        .tenant(tenant)
                                                        .branch(branch)
                                                        .homeBranch(branch)
                                                        .accountNumber("GL-CASH-" + suffix)
                                                        .accountName("Cash GL " + suffix)
                                                        .accountType(AccountType.SAVINGS)
                                                        .status(AccountStatus.ACTIVE)
                                                        .balance(BigDecimal.ZERO)
                                                        .currency("INR")
                                                        .glAccountCode("1100")
                                                        .build()));
        seedBalance(cashGl, new BigDecimal("100000.00"));

        Account account =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .homeBranch(branch)
                                .accountNumber("ACC-" + suffix)
                                .accountName("Account " + suffix)
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(new BigDecimal("50000.00"))
                                .currency("INR")
                                .glAccountCode(glDeposit.getGlCode())
                                .build());
        seedBalance(account, new BigDecimal("50000.00"));

        TenantContextHolder.setTenantId(tenant.getId());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_TELLER"))));

        return new TestData(account);
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

    private record TestData(Account account) {}
}
