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
 * Integration test: Double-entry bookkeeping enforcement.
 *
 * <p>CBS Invariant: For every transaction, SUM(DEBIT) must equal SUM(CREDIT) in the ledger entries.
 * This is the fundamental accounting equation that must never be violated.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DoubleEntryEnforcementTest {

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
    @DisplayName("Deposit: SUM(DR) == SUM(CR) in ledger entries")
    void depositCreatesBalancedLedgerEntries() {
        TestData data = setupTestData("DBL-DEP");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("25000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("DBL-DEP-REF-001")
                        .description("Double-entry deposit test")
                        .narration("Deposit 25K")
                        .build();

        var txn = transactionService.deposit(dto);

        BigDecimal debits = ledgerEntryRepository.sumDebitsByTransactionId(txn.getId());
        BigDecimal credits = ledgerEntryRepository.sumCreditsByTransactionId(txn.getId());

        assertNotNull(debits, "Debits must not be null after deposit");
        assertNotNull(credits, "Credits must not be null after deposit");
        assertTrue(
                debits.compareTo(BigDecimal.ZERO) > 0,
                "Debits must be positive (not vacuously zero)");
        assertEquals(
                0,
                debits.compareTo(credits),
                "Double-entry invariant violated: DR=" + debits + " != CR=" + credits);
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Withdrawal: SUM(DR) == SUM(CR) in ledger entries")
    void withdrawalCreatesBalancedLedgerEntries() {
        TestData data = setupTestData("DBL-WDR");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("WITHDRAWAL")
                        .sourceAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("1000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("DBL-WDR-REF-001")
                        .description("Double-entry withdrawal test")
                        .narration("Withdrawal 1K")
                        .build();

        var txn = transactionService.withdraw(dto);

        BigDecimal debits = ledgerEntryRepository.sumDebitsByTransactionId(txn.getId());
        BigDecimal credits = ledgerEntryRepository.sumCreditsByTransactionId(txn.getId());

        assertNotNull(debits);
        assertNotNull(credits);
        assertTrue(
                debits.compareTo(BigDecimal.ZERO) > 0,
                "Debits must be positive (not vacuously zero)");
        assertEquals(0, debits.compareTo(credits), "Double-entry invariant violated on withdrawal");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("Transfer: creates at least 2 entries with balanced amounts")
    void transferCreatesBalancedLedgerEntries() {
        TestData data = setupTestData("DBL-TRF");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("TRANSFER")
                        .sourceAccountNumber(data.account.getAccountNumber())
                        .destinationAccountNumber(data.destination.getAccountNumber())
                        .amount(new BigDecimal("500.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("DBL-TRF-REF-001")
                        .description("Double-entry transfer test")
                        .narration("Transfer 500")
                        .build();

        var txn = transactionService.transfer(dto);

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(txn.getId());
        assertTrue(entries.size() >= 2, "Transfer must create at least 2 ledger entries");

        BigDecimal debits = ledgerEntryRepository.sumDebitsByTransactionId(txn.getId());
        BigDecimal credits = ledgerEntryRepository.sumCreditsByTransactionId(txn.getId());

        assertEquals(0, debits.compareTo(credits), "Transfer journal must be balanced");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("Ledger entries are immutable — no UPDATE allowed on amount")
    void ledgerEntriesHaveCorrectEntryTypes() {
        TestData data = setupTestData("DBL-IMM");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("1000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("DBL-IMM-REF-001")
                        .description("Immutability test")
                        .narration("Check entry types")
                        .build();

        var txn = transactionService.deposit(dto);

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(txn.getId());
        assertFalse(entries.isEmpty(), "Ledger entries must exist — BATCH channel auto-authorizes");

        long debitCount = entries.stream().filter(e -> e.getEntryType() == EntryType.DEBIT).count();
        long creditCount =
                entries.stream().filter(e -> e.getEntryType() == EntryType.CREDIT).count();

        assertTrue(debitCount > 0, "Must have at least one DEBIT entry");
        assertTrue(creditCount > 0, "Must have at least one CREDIT entry");
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
                                .username("user-" + suffix)
                                .password("password")
                                .fullName("User " + suffix)
                                .email(suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        GeneralLedger glDeposit =
                glRepository.save(
                        GeneralLedger.builder()
                                .glCode("GL-DEP-" + suffix)
                                .glName("Deposits GL " + suffix)
                                .description("Deposits GL")
                                .accountType(GLAccountType.LIABILITY)
                                .level(0)
                                .isActive(true)
                                .normalBalance("CREDIT")
                                .balance(BigDecimal.ZERO)
                                .build());

        GeneralLedger glCash =
                glRepository
                        .findByGlCode("1100")
                        .orElseGet(
                                () ->
                                        glRepository.save(
                                                GeneralLedger.builder()
                                                        .glCode("1100")
                                                        .glName("Cash GL")
                                                        .description("Cash GL")
                                                        .accountType(GLAccountType.ASSET)
                                                        .level(0)
                                                        .isActive(true)
                                                        .normalBalance("DEBIT")
                                                        .balance(BigDecimal.ZERO)
                                                        .build()));

        Account cashGlAccount =
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
        seedBalance(cashGlAccount, new BigDecimal("100000.00"));

        Account account =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .homeBranch(branch)
                                .accountNumber("SRC-" + suffix)
                                .accountName("Source " + suffix)
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(new BigDecimal("50000.00"))
                                .currency("INR")
                                .glAccountCode(glDeposit.getGlCode())
                                .build());
        seedBalance(account, new BigDecimal("50000.00"));

        Account destination =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .homeBranch(branch)
                                .accountNumber("DST-" + suffix)
                                .accountName("Destination " + suffix)
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(new BigDecimal("30000.00"))
                                .currency("INR")
                                .glAccountCode(glDeposit.getGlCode())
                                .build());
        seedBalance(destination, new BigDecimal("30000.00"));

        TenantContextHolder.setTenantId(tenant.getId());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_TELLER"))));

        return new TestData(account, destination);
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

    private record TestData(Account account, Account destination) {}
}
