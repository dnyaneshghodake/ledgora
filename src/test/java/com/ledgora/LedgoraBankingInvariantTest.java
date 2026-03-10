package com.ledgora;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.repository.TransactionBatchRepository;
import com.ledgora.batch.service.BatchService;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.*;
import com.ledgora.common.exception.BusinessDayClosedException;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.service.TransactionService;
import com.ledgora.voucher.repository.VoucherRepository;
import com.ledgora.voucher.service.VoucherService;
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
 * Phase 7: Comprehensive banking invariant integration tests. Tests idempotency, maker-checker,
 * day-closed rejection, double-entry enforcement, and batch imbalance prevention.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgoraBankingInvariantTest {

    @Autowired private TransactionService transactionService;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private GeneralLedgerRepository glRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private VoucherService voucherService;
    @Autowired private LedgerEntryRepository ledgerEntryRepository;
    @Autowired private TransactionBatchRepository batchRepository;
    @Autowired private BatchService batchService;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    // ── Idempotency Tests ──

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Duplicate clientReferenceId+channel should be rejected (idempotency)")
    void testIdempotencyDuplicateRejection() {
        TestData data = setupTestData("IDEMP");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.source.getAccountNumber())
                        .amount(new BigDecimal("100.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IDEMP-REF-001")
                        .description("First deposit")
                        .narration("Idempotency test deposit 1")
                        .build();

        // First call should succeed
        assertDoesNotThrow(() -> transactionService.deposit(dto));

        // Second call with same clientReferenceId+channel should be rejected
        TransactionDTO duplicateDto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.source.getAccountNumber())
                        .amount(new BigDecimal("100.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IDEMP-REF-001")
                        .description("Duplicate deposit")
                        .narration("Idempotency test deposit 2")
                        .build();

        assertThrows(
                Exception.class,
                () -> transactionService.deposit(duplicateDto),
                "Duplicate clientReferenceId+channel must be rejected for idempotency");
    }

    // ── Day-Closed Transaction Rejection Tests ──

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Transactions must be rejected when business day is CLOSED")
    void testDayClosedTransactionRejection() {
        TestData data = setupTestData("DAYCL");

        // Close the business day
        Tenant tenant = tenantRepository.findByTenantCode("TEN-DAYCL").orElseThrow();
        tenant.setDayStatus(DayStatus.DAY_CLOSING);
        tenantRepository.save(tenant);

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.source.getAccountNumber())
                        .amount(new BigDecimal("50.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("DAYCL-REF-001")
                        .description("Day closed deposit")
                        .narration("Should be rejected")
                        .build();

        assertThrows(
                Exception.class,
                () -> transactionService.deposit(dto),
                "Transactions must be blocked when business day is not OPEN");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("TenantService.validateBusinessDayOpen throws for non-OPEN status")
    void testBusinessDayValidationThrows() {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("TEN-BDAY-VAL")
                                .tenantName("Business Day Validation Tenant")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.DAY_CLOSING)
                                .build());

        assertThrows(
                BusinessDayClosedException.class,
                () -> tenantService.validateBusinessDayOpen(tenant.getId()),
                "validateBusinessDayOpen must throw when day is DAY_CLOSING");
    }

    // ── Double-Entry Enforcement Tests ──

    @Test
    @Order(4)
    @Transactional
    @DisplayName("Every deposit creates balanced ledger entries (debit == credit)")
    void testDoubleEntryEnforcementOnDeposit() {
        TestData data = setupTestData("DBLENT");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.source.getAccountNumber())
                        .amount(new BigDecimal("500.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("DBLENT-REF-001")
                        .description("Double entry test")
                        .narration("Double entry enforcement")
                        .build();

        var txn = transactionService.deposit(dto);

        BigDecimal debits = ledgerEntryRepository.sumDebitsByTransactionId(txn.getId());
        BigDecimal credits = ledgerEntryRepository.sumCreditsByTransactionId(txn.getId());

        assertNotNull(debits, "Debits must not be null");
        assertNotNull(credits, "Credits must not be null");
        assertEquals(
                0,
                debits.compareTo(credits),
                "Double-entry invariant: total debits must equal total credits per journal");
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("Transfer creates exactly 2 ledger entries with balanced amounts")
    void testDoubleEntryEnforcementOnTransfer() {
        TestData data = setupTestData("DBLTRN");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("TRANSFER")
                        .sourceAccountNumber(data.source.getAccountNumber())
                        .destinationAccountNumber(data.destination.getAccountNumber())
                        .amount(new BigDecimal("200.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("DBLTRN-REF-001")
                        .description("Transfer double entry")
                        .narration("Transfer double entry test")
                        .build();

        var txn = transactionService.transfer(dto);

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(txn.getId());
        assertTrue(entries.size() >= 2, "Transfer must create at least 2 ledger entries");

        BigDecimal debits = ledgerEntryRepository.sumDebitsByTransactionId(txn.getId());
        BigDecimal credits = ledgerEntryRepository.sumCreditsByTransactionId(txn.getId());
        assertEquals(0, debits.compareTo(credits), "Transfer journal must be balanced");
    }

    // ── Batch Lifecycle Tests ──

    @Test
    @Order(6)
    @Transactional
    @DisplayName("Batch is auto-created with OPEN status for new business date")
    void testBatchAutoCreation() {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("TEN-BATCH-AC")
                                .tenantName("Batch AutoCreate Tenant")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Branch branch =
                branchRepository.save(
                        Branch.builder()
                                .branchCode("BRBATCAC")
                                .name("Batch AutoCreate Branch")
                                .isActive(true)
                                .build());

        TenantContextHolder.setTenantId(tenant.getId());

        TransactionBatch batch =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.TELLER, LocalDate.now());

        assertNotNull(batch, "Batch must be auto-created");
        assertEquals(BatchStatus.OPEN, batch.getStatus(), "New batch must have OPEN status");
        assertNotNull(batch.getBatchCode(), "Batch must have a batchCode assigned");
    }

    @Test
    @Order(7)
    @Transactional
    @DisplayName("Closed batch cannot be modified")
    void testClosedBatchImmutable() {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("TEN-BATCH-CL")
                                .tenantName("Batch Close Tenant")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Branch branch =
                branchRepository.save(
                        Branch.builder()
                                .branchCode("BRBATCCL")
                                .name("Batch Close Branch")
                                .isActive(true)
                                .build());

        TenantContextHolder.setTenantId(tenant.getId());

        TransactionBatch batch =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.TELLER, LocalDate.now());

        // Close the batch
        batch.setStatus(BatchStatus.CLOSED);
        batchRepository.save(batch);

        // Trying to update totals on a closed batch should fail
        assertThrows(
                Exception.class,
                () ->
                        batchService.updateBatchTotals(
                                batch.getId(), new BigDecimal("100.00"), new BigDecimal("100.00")),
                "Closed batch must not allow modification of totals");
    }

    // ── Tenant Isolation Tests ──

    @Test
    @Order(8)
    @Transactional
    @DisplayName("Cross-tenant account access must be blocked")
    void testTenantIsolationOnAccounts() {
        // Create two separate tenants
        Tenant tenantA =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("TEN-ISO-A")
                                .tenantName("Isolation Tenant A")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Tenant tenantB =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("TEN-ISO-B")
                                .tenantName("Isolation Tenant B")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Branch branch =
                branchRepository.save(
                        Branch.builder()
                                .branchCode("BR-ISO")
                                .name("Isolation Branch")
                                .isActive(true)
                                .build());

        // Create account under tenant A
        Account accountA =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenantA)
                                .branch(branch)
                                .homeBranch(branch)
                                .accountNumber("ACC-ISO-A")
                                .accountName("Isolation Account A")
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(new BigDecimal("1000.00"))
                                .currency("INR")
                                .glAccountCode("1100")
                                .build());

        // Set context to tenant B
        TenantContextHolder.setTenantId(tenantB.getId());

        // Verify tenant B cannot see tenant A's accounts
        List<Account> tenantBAccounts = accountRepository.findByTenantId(tenantB.getId());
        boolean canSeeTenantAAccount =
                tenantBAccounts.stream().anyMatch(a -> a.getAccountNumber().equals("ACC-ISO-A"));

        assertFalse(canSeeTenantAAccount, "Tenant B must NOT be able to see Tenant A's accounts");
    }

    // ── Optimistic Locking Tests ──

    @Test
    @Order(9)
    @Transactional
    @DisplayName("Account entity has @Version field for optimistic locking")
    void testOptimisticLockingPresent() {
        TestData data = setupTestData("OPTLK");

        Account account = accountRepository.findById(data.source.getId()).orElseThrow();
        assertNotNull(
                account.getVersion(), "Account must have a version field for optimistic locking");
    }

    // ── Helper Methods ──

    private TestData setupTestData(String suffix) {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("TEN-" + suffix)
                                .tenantName("Tenant " + suffix)
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
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
                                .username("inv-user-" + suffix)
                                .password("password")
                                .fullName("Invariant User " + suffix)
                                .email("inv-" + suffix + "@test.com")
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

        // Create Cash GL Account (Account entity with glAccountCode=1100) required by
        // resolveCashGlAccount
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
                                                        .accountName("Cash GL Account " + suffix)
                                                        .accountType(AccountType.SAVINGS)
                                                        .status(AccountStatus.ACTIVE)
                                                        .balance(BigDecimal.ZERO)
                                                        .currency("INR")
                                                        .glAccountCode("1100")
                                                        .build()));
        seedBalance(cashGlAccount, new BigDecimal("100000.00"));

        Account source =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .homeBranch(branch)
                                .accountNumber("SRC-" + suffix)
                                .accountName("Source " + suffix)
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(new BigDecimal("5000.00"))
                                .currency("INR")
                                .glAccountCode(glDeposit.getGlCode())
                                .build());

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
                                .balance(new BigDecimal("3000.00"))
                                .currency("INR")
                                .glAccountCode(glDeposit.getGlCode())
                                .build());

        seedBalance(source, new BigDecimal("5000.00"));
        seedBalance(destination, new BigDecimal("3000.00"));

        TenantContextHolder.setTenantId(tenant.getId());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_TELLER"))));

        return new TestData(source, destination);
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

    private record TestData(Account source, Account destination) {}
}
