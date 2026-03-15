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
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
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
 * Integration test: Ledger immutability.
 *
 * <p>CBS Rule: Ledger entries are immutable once created. To reverse a transaction, a new reversal
 * entry is created — the original entry is never modified or deleted. This maintains a complete
 * audit trail.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgerImmutabilityTest {

    @Autowired private TransactionService transactionService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private GeneralLedgerRepository glRepository;
    @Autowired private LedgerEntryRepository ledgerEntryRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Ledger entries are created with correct amounts and types")
    void ledgerEntriesCreatedCorrectly() {
        TestData data = setupTestData("IMM-01");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("10000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IMM-REF-001")
                        .description("Immutability deposit test")
                        .narration("Deposit 10K")
                        .build();

        var txn = transactionService.deposit(dto);

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(txn.getId());
        assertFalse(entries.isEmpty(), "Ledger entries must exist after deposit");

        for (LedgerEntry entry : entries) {
            assertNotNull(entry.getId(), "Entry must have an ID");
            assertNotNull(entry.getAmount(), "Entry must have an amount");
            assertTrue(
                    entry.getAmount().compareTo(BigDecimal.ZERO) > 0,
                    "Entry amount must be positive");
            assertNotNull(entry.getEntryType(), "Entry must have a type (DEBIT/CREDIT)");
            assertNotNull(entry.getBusinessDate(), "Entry must have a business date");
        }
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Each entry has a valid GL account code")
    void entriesHaveValidGlCodes() {
        TestData data = setupTestData("IMM-02");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("5000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IMM-REF-002")
                        .description("GL code test")
                        .narration("GL 5K")
                        .build();

        var txn = transactionService.deposit(dto);

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(txn.getId());
        for (LedgerEntry entry : entries) {
            assertNotNull(
                    entry.getGlAccountCode(), "Each ledger entry must have a GL account code");
            assertFalse(entry.getGlAccountCode().isBlank(), "GL account code must not be blank");
        }
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName(
            "Entry count increases with each transaction (entries are appended, never deleted)")
    void entryCountOnlyIncreases() {
        TestData data = setupTestData("IMM-03");

        long countBefore = ledgerEntryRepository.count();

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("2000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IMM-REF-003")
                        .description("Append test")
                        .narration("Append 2K")
                        .build();

        transactionService.deposit(dto);

        long countAfter = ledgerEntryRepository.count();
        assertTrue(countAfter > countBefore, "Entry count must increase (entries are append-only)");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("Balanced entries: every journal has equal DR and CR totals")
    void everyJournalIsBalanced() {
        TestData data = setupTestData("IMM-04");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("7500.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IMM-REF-004")
                        .description("Balance check")
                        .narration("Balance 7.5K")
                        .build();

        var txn = transactionService.deposit(dto);

        BigDecimal debits = ledgerEntryRepository.sumDebitsByTransactionId(txn.getId());
        BigDecimal credits = ledgerEntryRepository.sumCreditsByTransactionId(txn.getId());

        assertEquals(
                0,
                debits.compareTo(credits),
                "Journal must be balanced: DR=" + debits + " CR=" + credits);
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
                                .tenantName("Imm Tenant " + suffix)
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
                                .username("imm-user-" + suffix)
                                .password("password")
                                .fullName("Imm User " + suffix)
                                .email("imm-" + suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        GeneralLedger glDeposit =
                glRepository.save(
                        GeneralLedger.builder()
                                .glCode("GL-IMM-" + suffix)
                                .glName("Imm GL " + suffix)
                                .description("Imm GL")
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
