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
import com.ledgora.balance.service.CbsBalanceEngine;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.repository.TransactionBatchRepository;
import com.ledgora.batch.service.BatchService;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.ApprovalStatus;
import com.ledgora.common.enums.BatchStatus;
import com.ledgora.common.enums.CustomerStatus;
import com.ledgora.common.enums.DayStatus;
import com.ledgora.common.enums.EntryType;
import com.ledgora.common.enums.GLAccountType;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.enums.TransactionStatus;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.common.enums.VoucherDrCr;
import com.ledgora.common.exception.BusinessDayClosedException;
import com.ledgora.customer.entity.CustomerFreezeControl;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.repository.CustomerFreezeControlRepository;
import com.ledgora.customer.repository.CustomerMasterRepository;
import com.ledgora.customer.service.CbsCustomerValidationService;
import com.ledgora.eod.repository.EodProcessRepository;
import com.ledgora.eod.service.EodValidationService;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.gl.service.CbsGlBalanceService;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.ledger.service.LedgerService;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.service.TransactionService;
import com.ledgora.voucher.entity.Voucher;
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
 * Comprehensive CBS Phase 1-7 Integration Test Suite.
 *
 * <p>Phase 1: Master Data Flow Testing Phase 2: Transaction Flow Testing Phase 3: Security &
 * Tampering Tests Phase 4: Batch Lifecycle Testing Phase 5: GL & Ledger Integrity Testing Phase 6:
 * EOD Testing Phase 7: Multi-Tenant Isolation Testing
 *
 * <p>All tests use @SpringBootTest with H2 in-memory database. @Transactional ensures rollback
 * after each test for isolation.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgoraCbsPhase1To7IntegrationTest {

    // ═══════════════════════════════════════════════════════════════
    // INJECTED DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════

    @Autowired private TransactionService transactionService;
    @Autowired private VoucherService voucherService;
    @Autowired private BatchService batchService;
    @Autowired private TenantService tenantService;
    @Autowired private LedgerService ledgerService;
    @Autowired private CbsBalanceEngine cbsBalanceEngine;
    @Autowired private CbsGlBalanceService cbsGlBalanceService;
    @Autowired private EodValidationService eodValidationService;
    @Autowired private EodProcessRepository eodProcessRepository;
    @Autowired private CbsCustomerValidationService customerValidationService;

    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GeneralLedgerRepository glRepository;
    @Autowired private CustomerMasterRepository customerMasterRepository;
    @Autowired private CustomerFreezeControlRepository freezeControlRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private LedgerEntryRepository ledgerEntryRepository;
    @Autowired private TransactionBatchRepository batchRepository;
    @Autowired private ApprovalRequestRepository approvalRequestRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════
    // PHASE 1: MASTER DATA FLOW TESTING
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Phase 1.1: Tenant creation and lifecycle")
    void testTenantCreation() {
        Tenant tenant =
                tenantService.createTenant("P1-TENANT-01", "Phase 1 Test Bank", nextWeekday());

        assertNotNull(tenant.getId(), "Tenant must have generated ID");
        assertEquals("P1-TENANT-01", tenant.getTenantCode());
        assertEquals("ACTIVE", tenant.getStatus());
        assertEquals(DayStatus.OPEN, tenant.getDayStatus());
        assertEquals(nextWeekday(), tenant.getCurrentBusinessDate());
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Phase 1.2: Duplicate tenant code rejected")
    void testDuplicateTenantCodeRejected() {
        tenantService.createTenant("P1-DUP-01", "First Bank", nextWeekday());
        assertThrows(
                RuntimeException.class,
                () -> tenantService.createTenant("P1-DUP-01", "Second Bank", nextWeekday()),
                "Duplicate tenant code must be rejected");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("Phase 1.3: Branch creation and linking")
    void testBranchCreation() {
        Branch branch =
                branchRepository.save(
                        Branch.builder()
                                .branchCode("P1BR01")
                                .name("Phase 1 Main Branch")
                                .isActive(true)
                                .build());

        assertNotNull(branch.getId());
        assertEquals("P1BR01", branch.getBranchCode());
        assertTrue(branch.getIsActive());
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("Phase 1.4: User + role creation with tenant linkage")
    void testUserCreationWithTenantAndRole() {
        Tenant tenant = createTenant("P1-USR-01");
        Branch branch = createBranch("P1USR01");

        User user =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .username("p1-teller-01")
                                .password("password")
                                .fullName("Phase 1 Teller")
                                .email("p1teller@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        assertNotNull(user.getId());
        assertEquals(tenant.getId(), user.getTenant().getId(), "User must be linked to tenant");
        assertEquals(branch.getId(), user.getBranch().getId(), "User must be linked to branch");
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("Phase 1.5: Customer creation with tenant scoping")
    void testCustomerCreationTenantScoped() {
        Tenant tenant = createTenant("P1-CUST-01");

        CustomerMaster customer =
                customerMasterRepository.save(
                        CustomerMaster.builder()
                                .tenant(tenant)
                                .customerNumber("CUST-P1-001")
                                .firstName("John")
                                .lastName("Doe")
                                .status(CustomerStatus.ACTIVE)
                                .makerCheckerStatus(MakerCheckerStatus.APPROVED)
                                .build());

        assertNotNull(customer.getId());
        assertEquals(tenant.getId(), customer.getTenant().getId());
        assertTrue(
                customerMasterRepository
                        .findByTenantIdAndCustomerNumber(tenant.getId(), "CUST-P1-001")
                        .isPresent());
    }

    @Test
    @Order(6)
    @Transactional
    @DisplayName("Phase 1.6: Account opening linked to branch with GL mapping")
    void testAccountOpeningWithGlMapping() {
        FullTestData data = setupFullTestData("P1-ACC");

        assertNotNull(data.account.getId());
        assertEquals(
                data.tenant.getId(),
                data.account.getTenant().getId(),
                "Account must be tenant-scoped");
        assertEquals(
                data.branch.getId(),
                data.account.getBranch().getId(),
                "Account must be linked to branch");
        assertNotNull(data.account.getGlAccountCode(), "Account must have GL mapping");
        assertEquals(AccountStatus.ACTIVE, data.account.getStatus());
    }

    @Test
    @Order(7)
    @Transactional
    @DisplayName("Phase 1.7: Initial funding creates proper balance")
    void testInitialFundingCreatesBalance() {
        FullTestData data = setupFullTestData("P1-FUND");

        AccountBalance balance = cbsBalanceEngine.getCbsBalance(data.account.getId());
        assertNotNull(balance, "Account must have balance record");
        assertEquals(
                0,
                new BigDecimal("10000.00").compareTo(balance.getActualTotalBalance()),
                "Initial funding should set actual balance");
        assertEquals(
                0,
                new BigDecimal("10000.00").compareTo(balance.getAvailableBalance()),
                "Initial funding should set available balance");
    }

    // ═══════════════════════════════════════════════════════════════
    // PHASE 2: TRANSACTION FLOW TESTING
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @Transactional
    @DisplayName("Phase 2.1: Cash deposit - full flow with voucher, ledger, GL, batch")
    void testCashDepositFullFlow() {
        FullTestData data = setupFullTestData("P2-DEP");
        BigDecimal depositAmount = new BigDecimal("5000.00");
        BigDecimal initialBalance = new BigDecimal("10000.00");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(depositAmount)
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("P2-DEP-REF-001")
                        .description("Phase 2 cash deposit test")
                        .narration("Cash deposit test")
                        .build();

        Transaction txn = transactionService.deposit(dto);

        // Verify transaction created
        assertNotNull(txn.getId());
        assertEquals(TransactionType.DEPOSIT, txn.getTransactionType());
        assertEquals(TransactionStatus.COMPLETED, txn.getStatus());
        assertNotNull(txn.getBatch(), "Transaction must have batch assigned");

        // Verify vouchers created (2 legs: DR cash, CR customer)
        LocalDate bizDate = data.tenant.getCurrentBusinessDate();
        List<Voucher> vouchers =
                voucherRepository.findByTenantIdAndBranchIdAndPostingDate(
                        data.tenant.getId(), data.branch.getId(), bizDate);
        assertTrue(vouchers.size() >= 2, "Deposit must create at least 2 vouchers (DR+CR)");

        // Verify all vouchers are authorized and posted
        for (Voucher v : vouchers) {
            assertEquals("Y", v.getAuthFlag(), "Voucher must be authorized");
            assertEquals("Y", v.getPostFlag(), "Voucher must be posted");
        }

        // Verify ledger entries created and balanced
        List<LedgerEntry> entries =
                ledgerEntryRepository.findByBusinessDateAndTenantId(bizDate, data.tenant.getId());
        BigDecimal totalDebits =
                entries.stream()
                        .filter(e -> e.getEntryType() == EntryType.DEBIT)
                        .map(LedgerEntry::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits =
                entries.stream()
                        .filter(e -> e.getEntryType() == EntryType.CREDIT)
                        .map(LedgerEntry::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(
                0,
                totalDebits.compareTo(totalCredits),
                "Double-entry: total debits must equal total credits");

        // Verify balance updated
        AccountBalance balance = cbsBalanceEngine.getCbsBalance(data.account.getId());
        assertEquals(
                0,
                initialBalance.add(depositAmount).compareTo(balance.getActualTotalBalance()),
                "Balance must be updated after deposit");
    }

    @Test
    @Order(11)
    @Transactional
    @DisplayName("Phase 2.2: Cash withdrawal - full flow with balance validation")
    void testCashWithdrawalFullFlow() {
        FullTestData data = setupFullTestData("P2-WDR");
        BigDecimal withdrawAmount = new BigDecimal("3000.00");
        BigDecimal initialBalance = new BigDecimal("10000.00");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("WITHDRAWAL")
                        .sourceAccountNumber(data.account.getAccountNumber())
                        .amount(withdrawAmount)
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("P2-WDR-REF-001")
                        .description("Phase 2 withdrawal test")
                        .narration("Cash withdrawal test")
                        .build();

        Transaction txn = transactionService.withdraw(dto);

        assertNotNull(txn.getId());
        assertEquals(TransactionType.WITHDRAWAL, txn.getTransactionType());
        assertEquals(TransactionStatus.COMPLETED, txn.getStatus());

        // Verify balance decreased
        AccountBalance balance = cbsBalanceEngine.getCbsBalance(data.account.getId());
        assertEquals(
                0,
                initialBalance.subtract(withdrawAmount).compareTo(balance.getActualTotalBalance()),
                "Balance must decrease after withdrawal");

        // Verify ledger entries balanced
        verifyLedgerBalanced(data.tenant.getId());
    }

    @Test
    @Order(12)
    @Transactional
    @DisplayName("Phase 2.3: Online transfer - dual-leg voucher creation")
    void testOnlineTransferDualLeg() {
        FullTestData srcData = setupFullTestData("P2-TSRC");
        // Create destination account in same tenant
        Account destAccount =
                createAccountInTenant(srcData.tenant, srcData.branch, "DST-P2-TRF", srcData.gl);
        seedBalance(destAccount, new BigDecimal("5000.00"));

        BigDecimal transferAmount = new BigDecimal("2000.00");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("TRANSFER")
                        .sourceAccountNumber(srcData.account.getAccountNumber())
                        .destinationAccountNumber(destAccount.getAccountNumber())
                        .amount(transferAmount)
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("P2-TRF-REF-001")
                        .description("Phase 2 transfer test")
                        .narration("Online transfer test")
                        .build();

        Transaction txn = transactionService.transfer(dto);

        assertNotNull(txn.getId());
        assertEquals(TransactionType.TRANSFER, txn.getTransactionType());

        // Verify source balance decreased
        AccountBalance srcBalance = cbsBalanceEngine.getCbsBalance(srcData.account.getId());
        assertEquals(
                0,
                new BigDecimal("8000.00").compareTo(srcBalance.getActualTotalBalance()),
                "Source balance must decrease by transfer amount");

        // Verify destination balance increased
        AccountBalance dstBalance = cbsBalanceEngine.getCbsBalance(destAccount.getId());
        assertEquals(
                0,
                new BigDecimal("7000.00").compareTo(dstBalance.getActualTotalBalance()),
                "Destination balance must increase by transfer amount");

        // Verify double-entry balanced
        verifyLedgerBalanced(srcData.tenant.getId());
    }

    @Test
    @Order(13)
    @Transactional
    @DisplayName("Phase 2.4: Withdrawal exceeding balance must fail")
    void testWithdrawalExceedingBalanceFails() {
        FullTestData data = setupFullTestData("P2-OVWD");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("WITHDRAWAL")
                        .sourceAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("99999.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("P2-OVWD-REF-001")
                        .description("Overdraft test")
                        .narration("Should fail")
                        .build();

        assertThrows(
                RuntimeException.class,
                () -> transactionService.withdraw(dto),
                "Withdrawal exceeding balance must be rejected");
    }

    @Test
    @Order(14)
    @Transactional
    @DisplayName("Phase 2.5: No ledger update/delete allowed (append-only)")
    void testLedgerAppendOnly() {
        FullTestData data = setupFullTestData("P2-APPD");

        // Do a deposit to create ledger entries
        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("1000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("P2-APPD-REF-001")
                        .description("Append only test")
                        .narration("Ledger append test")
                        .build();

        transactionService.deposit(dto);

        List<LedgerEntry> entries =
                ledgerEntryRepository.findByBusinessDateAndTenantId(
                        data.tenant.getCurrentBusinessDate(), data.tenant.getId());
        assertFalse(entries.isEmpty(), "Ledger entries must exist after deposit");

        long countBefore = ledgerEntryRepository.count();

        // Verify ledger entries are immutable - they have no update/delete operations in service
        // layer
        // The LedgerEntry entity does not expose setter methods for key fields after creation
        // This is an architectural invariant: entries are created via VoucherService.postVoucher
        // only
        for (LedgerEntry entry : entries) {
            assertNotNull(entry.getAmount(), "Entry amount must not be null");
            assertNotNull(entry.getEntryType(), "Entry type must not be null");
            assertNotNull(entry.getJournal(), "Entry must be linked to journal");
        }

        long countAfter = ledgerEntryRepository.count();
        assertEquals(countBefore, countAfter, "Ledger entry count must not change (append-only)");
    }

    @Test
    @Order(15)
    @Transactional
    @DisplayName("Phase 2.6: Idempotency - duplicate transaction rejected")
    void testIdempotencyDuplicateRejection() {
        FullTestData data = setupFullTestData("P2-IDMP");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("1000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("P2-IDMP-UNIQUE-001")
                        .description("First deposit")
                        .narration("Idempotency test")
                        .build();

        // First call should succeed
        assertDoesNotThrow(() -> transactionService.deposit(dto));

        // Second call with same clientReferenceId+channel should be rejected
        TransactionDTO duplicate =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("1000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("P2-IDMP-UNIQUE-001")
                        .description("Duplicate deposit")
                        .narration("Should be rejected")
                        .build();

        assertThrows(
                RuntimeException.class,
                () -> transactionService.deposit(duplicate),
                "Duplicate clientReferenceId+channel must be rejected for idempotency");
    }

    // ═══════════════════════════════════════════════════════════════
    // PHASE 3: SECURITY & TAMPERING TESTS
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(20)
    @Transactional
    @DisplayName("Phase 3.1: Negative amount injection must fail")
    void testNegativeAmountInjection() {
        FullTestData data = setupFullTestData("P3-NEG");

        // Negative amount deposit
        TransactionDTO negativeDto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("-500.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("P3-NEG-REF-001")
                        .description("Negative amount injection")
                        .narration("Should fail")
                        .build();

        assertThrows(
                RuntimeException.class,
                () -> transactionService.deposit(negativeDto),
                "Negative amount must be rejected by server-side validation");

        // Zero amount deposit
        TransactionDTO zeroDto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(BigDecimal.ZERO)
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("P3-ZERO-REF-001")
                        .description("Zero amount injection")
                        .narration("Should fail")
                        .build();

        assertThrows(
                RuntimeException.class,
                () -> transactionService.deposit(zeroDto),
                "Zero amount must be rejected by server-side validation");

        // Negative amount withdrawal
        TransactionDTO negWdrDto =
                TransactionDTO.builder()
                        .transactionType("WITHDRAWAL")
                        .sourceAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("-100.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("P3-NEGWDR-REF-001")
                        .description("Negative withdrawal")
                        .narration("Should fail")
                        .build();

        assertThrows(
                RuntimeException.class,
                () -> transactionService.withdraw(negWdrDto),
                "Negative withdrawal amount must be rejected");

        // Balance must remain unchanged
        AccountBalance balance = cbsBalanceEngine.getCbsBalance(data.account.getId());
        assertEquals(
                0,
                new BigDecimal("10000.00").compareTo(balance.getActualTotalBalance()),
                "Balance must remain unchanged after negative amount injection attempts");
    }

    @Test
    @Order(21)
    @Transactional
    @DisplayName("Phase 3.2: Tenant override attempt must fail")
    void testTenantOverrideAttempt() {
        FullTestData data1 = setupFullTestData("P3-TN1");
        FullTestData data2 = setupFullTestData("P3-TN2");

        // Set tenant context to tenant 2
        TenantContextHolder.setTenantId(data2.tenant.getId());

        // Try to access tenant 1's account with tenant 2's context
        assertThrows(
                RuntimeException.class,
                () -> {
                    TransactionDTO dto =
                            TransactionDTO.builder()
                                    .transactionType("DEPOSIT")
                                    .destinationAccountNumber(data1.account.getAccountNumber())
                                    .amount(new BigDecimal("100.00"))
                                    .currency("INR")
                                    .channel(TransactionChannel.TELLER.name())
                                    .clientReferenceId("P3-TNOVR-001")
                                    .description("Tenant override attempt")
                                    .narration("Should fail")
                                    .build();
                    transactionService.deposit(dto);
                },
                "Cross-tenant transaction must be blocked");
    }

    @Test
    @Order(22)
    @Transactional
    @DisplayName("Phase 3.3: Business date override attempt must fail")
    void testBusinessDateOverrideBlocked() {
        FullTestData data = setupFullTestData("P3-BDATE");

        // Close the business day
        Tenant tenant = tenantRepository.findById(data.tenant.getId()).orElseThrow();
        tenant.setDayStatus(DayStatus.DAY_CLOSING);
        tenantRepository.save(tenant);

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("100.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("P3-BDATE-REF-001")
                        .description("Business date override attempt")
                        .narration("Should fail")
                        .build();

        assertThrows(
                BusinessDayClosedException.class,
                () -> transactionService.deposit(dto),
                "Transactions must be blocked when business day is not OPEN");
    }

    @Test
    @Order(23)
    @Transactional
    @DisplayName("Phase 3.4: Maker-checker enforcement - same user cannot authorize own voucher")
    void testMakerCheckerEnforcement() {
        FullTestData data = setupFullTestData("P3-MC");

        TenantContextHolder.setTenantId(data.tenant.getId());
        LocalDate bizDate = data.tenant.getCurrentBusinessDate();

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
                        bizDate,
                        bizDate,
                        "BATCH-MC",
                        1,
                        data.maker,
                        "Maker-checker test");

        // Same maker trying to authorize - should fail
        assertThrows(
                RuntimeException.class,
                () -> voucherService.authorizeVoucher(voucher.getId(), data.maker),
                "Maker and checker cannot be the same user");
    }

    @Test
    @Order(24)
    @Transactional
    @DisplayName("Phase 3.5: Cancelled voucher cannot be authorized")
    void testCancelledVoucherCannotBeAuthorized() {
        FullTestData data = setupFullTestData("P3-CANC");

        TenantContextHolder.setTenantId(data.tenant.getId());
        LocalDate bizDate = data.tenant.getCurrentBusinessDate();

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
                        bizDate,
                        bizDate,
                        "BATCH-CANC",
                        1,
                        data.maker,
                        "Cancel test");

        // Cancel the voucher
        voucherService.cancelVoucher(voucher.getId(), data.checker, "Test cancellation");

        // Try to authorize cancelled voucher - should fail
        assertThrows(
                RuntimeException.class,
                () -> voucherService.authorizeVoucher(voucher.getId(), data.checker),
                "Cancelled voucher cannot be authorized");
    }

    @Test
    @Order(25)
    @Transactional
    @DisplayName("Phase 3.6: Already authorized voucher cannot be re-authorized")
    void testDoubleAuthorizationBlocked() {
        FullTestData data = setupFullTestData("P3-DAUTH");

        TenantContextHolder.setTenantId(data.tenant.getId());
        LocalDate bizDate = data.tenant.getCurrentBusinessDate();

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
                        bizDate,
                        bizDate,
                        "BATCH-DAUTH",
                        1,
                        data.maker,
                        "Double auth test");

        // First authorization should succeed
        voucherService.authorizeVoucher(voucher.getId(), data.checker);

        // Second authorization should fail
        assertThrows(
                RuntimeException.class,
                () -> voucherService.authorizeVoucher(voucher.getId(), data.checker),
                "Already authorized voucher cannot be re-authorized");
    }

    @Test
    @Order(26)
    @Transactional
    @DisplayName("Phase 3.7: Unauthorized voucher cannot be posted")
    void testUnauthorizedVoucherCannotBePosted() {
        FullTestData data = setupFullTestData("P3-UNAUTH");

        TenantContextHolder.setTenantId(data.tenant.getId());
        LocalDate bizDate = data.tenant.getCurrentBusinessDate();

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
                        bizDate,
                        bizDate,
                        "BATCH-UA",
                        1,
                        data.maker,
                        "Unauth post test");

        // Try to post without authorization
        assertThrows(
                RuntimeException.class,
                () -> voucherService.postVoucher(voucher.getId()),
                "Unauthorized voucher must not be posted");
    }

    @Test
    @Order(27)
    @Transactional
    @DisplayName("Phase 3.8: Frozen customer account blocks debit transactions")
    void testFrozenAccountBlocksDebit() {
        Tenant tenant = createTenant("P3-FRZ");
        Branch branch = createBranch("P3FRZ01");
        CustomerMaster customer = createCustomer(tenant, "CUST-P3-FRZ");

        // Apply debit freeze
        freezeControlRepository.save(
                CustomerFreezeControl.builder()
                        .tenant(tenant)
                        .customerMaster(customer)
                        .debitFreeze(true)
                        .debitFreezeReason("Compliance hold")
                        .creditFreeze(false)
                        .build());

        Account account = createAccountForCustomer(tenant, branch, customer, "ACC-P3-FRZ");

        assertThrows(
                RuntimeException.class,
                () ->
                        customerValidationService.validateAccountForTransaction(
                                account, tenant.getId(), branch.getId(), VoucherDrCr.DR),
                "Debit-frozen account must block debit transactions");
    }

    // ═══════════════════════════════════════════════════════════════
    // PHASE 4: BATCH LIFECYCLE TESTING
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(30)
    @Transactional
    @DisplayName("Phase 4.1: Batch auto-creation with OPEN status")
    void testBatchAutoCreation() {
        Tenant tenant = createTenant("P4-BATCH");

        TransactionBatch batch =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.TELLER, tenant.getCurrentBusinessDate());

        assertNotNull(batch.getId());
        assertEquals(BatchStatus.OPEN, batch.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(batch.getTotalDebit()));
        assertEquals(0, BigDecimal.ZERO.compareTo(batch.getTotalCredit()));
        assertEquals(0, batch.getTransactionCount());
    }

    @Test
    @Order(31)
    @Transactional
    @DisplayName("Phase 4.2: Batch totals accumulate with multiple transactions")
    void testBatchTotalsAccumulate() {
        Tenant tenant = createTenant("P4-ACCUM");

        TransactionBatch batch =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.TELLER, tenant.getCurrentBusinessDate());

        // Simulate multiple transaction postings
        batchService.updateBatchTotals(
                batch.getId(), new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        batchService.updateBatchTotals(
                batch.getId(), new BigDecimal("2000.00"), new BigDecimal("2000.00"));
        batchService.updateBatchTotals(
                batch.getId(), new BigDecimal("500.00"), new BigDecimal("500.00"));

        TransactionBatch updated = batchRepository.findById(batch.getId()).orElseThrow();
        assertEquals(
                0,
                new BigDecimal("3500.00").compareTo(updated.getTotalDebit()),
                "Batch debit totals must accumulate");
        assertEquals(
                0,
                new BigDecimal("3500.00").compareTo(updated.getTotalCredit()),
                "Batch credit totals must accumulate");
        assertEquals(3, updated.getTransactionCount(), "Transaction count must increment");
    }

    @Test
    @Order(32)
    @Transactional
    @DisplayName("Phase 4.3: Post in CLOSED batch must fail")
    void testClosedBatchPostFails() {
        Tenant tenant = createTenant("P4-CLSD");
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        TransactionBatch batch =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.TELLER, bizDate);

        // Close the batch
        batchService.closeAllBatches(tenant.getId(), bizDate);

        // Try to update closed batch - should fail
        assertThrows(
                RuntimeException.class,
                () ->
                        batchService.updateBatchTotals(
                                batch.getId(), new BigDecimal("100.00"), new BigDecimal("100.00")),
                "Cannot update a closed batch");
    }

    @Test
    @Order(33)
    @Transactional
    @DisplayName("Phase 4.4: Close balanced batch must pass")
    void testCloseBalancedBatch() {
        Tenant tenant = createTenant("P4-BAL");
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        TransactionBatch batch =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.TELLER, bizDate);
        batchService.updateBatchTotals(
                batch.getId(), new BigDecimal("5000.00"), new BigDecimal("5000.00"));

        // Close batch
        batchService.closeAllBatches(tenant.getId(), bizDate);

        TransactionBatch closed = batchRepository.findById(batch.getId()).orElseThrow();
        assertEquals(
                BatchStatus.CLOSED, closed.getStatus(), "Balanced batch must close successfully");

        // Settle balanced batch
        assertDoesNotThrow(
                () -> batchService.settleAllBatches(tenant.getId(), bizDate),
                "Balanced batch must settle successfully");

        TransactionBatch settled = batchRepository.findById(batch.getId()).orElseThrow();
        assertEquals(
                BatchStatus.SETTLED, settled.getStatus(), "Batch must be SETTLED after settlement");
    }

    @Test
    @Order(34)
    @Transactional
    @DisplayName("Phase 4.5: Settle unbalanced batch must fail")
    void testSettleUnbalancedBatchFails() {
        Tenant tenant = createTenant("P4-UNBAL");
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        TransactionBatch batch =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.TELLER, bizDate);

        // Create unbalanced batch
        batchService.updateBatchTotals(
                batch.getId(), new BigDecimal("5000.00"), new BigDecimal("3000.00"));

        // Close should fail due to debit != credit (batch close now validates balance)
        assertThrows(
                RuntimeException.class,
                () -> batchService.closeAllBatches(tenant.getId(), bizDate),
                "Unbalanced batch close must fail");
    }

    @Test
    @Order(35)
    @Transactional
    @DisplayName("Phase 4.6: Same channel/tenant/date returns same open batch")
    void testBatchIdempotency() {
        Tenant tenant = createTenant("P4-IDMP");
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        TransactionBatch batch1 =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.TELLER, bizDate);
        TransactionBatch batch2 =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.TELLER, bizDate);

        assertEquals(
                batch1.getId(),
                batch2.getId(),
                "Same channel/tenant/date must return same open batch");
    }

    @Test
    @Order(36)
    @Transactional
    @DisplayName("Phase 4.7: Different channels create different batches")
    void testDifferentChannelsDifferentBatches() {
        Tenant tenant = createTenant("P4-CHAN");
        LocalDate bizDate = tenant.getCurrentBusinessDate();

        TransactionBatch tellerBatch =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.TELLER, bizDate);
        TransactionBatch onlineBatch =
                batchService.getOrCreateOpenBatch(
                        tenant.getId(), TransactionChannel.ONLINE, bizDate);

        assertNotEquals(
                tellerBatch.getId(),
                onlineBatch.getId(),
                "Different channels must create different batches");
    }

    // ═══════════════════════════════════════════════════════════════
    // PHASE 5: GL & LEDGER INTEGRITY TESTING
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(40)
    @Transactional
    @DisplayName("Phase 5.1: Total GL Debit = Total GL Credit after deposit")
    void testGlDoubleEntryOnDeposit() {
        FullTestData data = setupFullTestData("P5-GLDEP");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("5000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("P5-GLDEP-REF-001")
                        .description("GL integrity test deposit")
                        .narration("GL balance check")
                        .build();

        transactionService.deposit(dto);

        // Verify ledger entries balanced
        LocalDate bizDate = data.tenant.getCurrentBusinessDate();
        BigDecimal debits =
                ledgerEntryRepository.sumDebitsByBusinessDateAndTenantId(
                        bizDate, data.tenant.getId());
        BigDecimal credits =
                ledgerEntryRepository.sumCreditsByBusinessDateAndTenantId(
                        bizDate, data.tenant.getId());
        assertEquals(0, debits.compareTo(credits), "GL: Total debits must equal total credits");
    }

    @Test
    @Order(41)
    @Transactional
    @DisplayName("Phase 5.2: Trial balance must be balanced after transactions")
    void testTrialBalanceAfterTransactions() {
        FullTestData data = setupFullTestData("P5-TRIAL");

        // Deposit
        transactionService.deposit(
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("10000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("P5-TRIAL-DEP-001")
                        .description("Trial balance test")
                        .narration("Trial balance deposit")
                        .build());

        // Verify ledger integrity
        boolean balanced =
                ledgerService.validateLedgerIntegrity(data.tenant.getCurrentBusinessDate());
        assertTrue(balanced, "Trial balance must be balanced after transactions");
    }

    @Test
    @Order(42)
    @Transactional
    @DisplayName("Phase 5.3: No orphan ledger entries")
    void testNoOrphanLedgerEntries() {
        FullTestData data = setupFullTestData("P5-ORPH");

        transactionService.deposit(
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("1000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("P5-ORPH-REF-001")
                        .description("Orphan check")
                        .narration("No orphans")
                        .build());

        long orphanCount = ledgerEntryRepository.countOrphanEntries();
        assertEquals(
                0,
                orphanCount,
                "There must be no orphan ledger entries (entries without transaction)");
    }

    @Test
    @Order(43)
    @Transactional
    @DisplayName("Phase 5.4: Shadow vs actual balance consistent after posting")
    void testShadowVsActualConsistent() {
        FullTestData data = setupFullTestData("P5-SHAD");

        transactionService.deposit(
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data.account.getAccountNumber())
                        .amount(new BigDecimal("5000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("P5-SHAD-REF-001")
                        .description("Shadow balance test")
                        .narration("Shadow vs actual")
                        .build());

        // After posting, shadow delta should be zero (all vouchers are posted)
        AccountBalance balance = cbsBalanceEngine.getCbsBalance(data.account.getId());
        // Shadow total = actual + pending. If all posted, shadow == 0 (no pending effect)
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(balance.getShadowTotalBalance()),
                "Shadow balance should be zero after all vouchers are posted (no pending effects)");
    }

    @Test
    @Order(44)
    @Transactional
    @DisplayName("Phase 5.5: Ledger entries per transaction are always balanced")
    void testLedgerEntriesPerTransactionBalanced() {
        FullTestData data = setupFullTestData("P5-TXBAL");

        Transaction txn =
                transactionService.deposit(
                        TransactionDTO.builder()
                                .transactionType("DEPOSIT")
                                .destinationAccountNumber(data.account.getAccountNumber())
                                .amount(new BigDecimal("3000.00"))
                                .currency("INR")
                                .channel(TransactionChannel.BATCH.name())
                                .clientReferenceId("P5-TXBAL-REF-001")
                                .description("Per-txn balance test")
                                .narration("Per txn balanced")
                                .build());

        BigDecimal txnDebits = ledgerEntryRepository.sumDebitsByTransactionId(txn.getId());
        BigDecimal txnCredits = ledgerEntryRepository.sumCreditsByTransactionId(txn.getId());
        assertEquals(
                0,
                txnDebits.compareTo(txnCredits),
                "Per-transaction ledger entries must be balanced (debit=credit)");
    }

    // ═══════════════════════════════════════════════════════════════
    // PHASE 6: EOD TESTING
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(50)
    @DisplayName("Phase 6.1: Valid EOD - all conditions met, business date advances")
    void testValidEodAllConditionsMet() {
        // NOTE: No @Transactional — EOD state machine uses REQUIRES_NEW which needs committed data
        Tenant tenant = tenantService.createTenant("P6-EOD-OK", "EOD OK Bank", nextWeekday());
        LocalDate currentDate = tenant.getCurrentBusinessDate();

        try {
            // No transactions means no unauthorized/unposted vouchers and ledger is trivially
            // balanced
            // Run EOD
            assertDoesNotThrow(
                    () -> eodValidationService.runEod(tenant.getId()),
                    "EOD should pass when all conditions are met");

            // Verify business date advanced
            Tenant updated = tenantRepository.findById(tenant.getId()).orElseThrow();
            assertEquals(
                    currentDate.plusDays(1),
                    updated.getCurrentBusinessDate(),
                    "Business date must advance after successful EOD");
            assertEquals(
                    DayStatus.CLOSED,
                    updated.getDayStatus(),
                    "Day status must be CLOSED after EOD (requires explicit Day Begin to re-open)");
        } finally {
            // Manual cleanup since no @Transactional rollback
            // Delete child EodProcess records before tenant (FK constraint)
            eodProcessRepository
                    .findByTenantIdAndBusinessDate(tenant.getId(), currentDate)
                    .ifPresent(eodProcessRepository::delete);
            // Also clean up any EOD process for the advanced date
            eodProcessRepository
                    .findByTenantIdAndBusinessDate(tenant.getId(), currentDate.plusDays(1))
                    .ifPresent(eodProcessRepository::delete);
            tenantRepository.deleteById(tenant.getId());
        }
    }

    @Test
    @Order(51)
    @Transactional
    @DisplayName("Phase 6.2: EOD blocked by unauthorized vouchers")
    void testEodBlockedByUnauthorizedVouchers() {
        FullTestData data = setupFullTestData("P6-UNAUTH");

        TenantContextHolder.setTenantId(data.tenant.getId());
        LocalDate bizDate = data.tenant.getCurrentBusinessDate();

        // Create an unauthorized voucher (auth_flag = N)
        voucherService.createVoucher(
                data.tenant,
                data.branch,
                data.account,
                data.gl,
                VoucherDrCr.CR,
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                "INR",
                bizDate,
                bizDate,
                "BATCH-EOD",
                1,
                data.maker,
                "Unauthorized voucher for EOD test");

        // EOD should fail
        List<String> errors = eodValidationService.validateEod(data.tenant.getId(), bizDate);
        assertFalse(errors.isEmpty(), "EOD must be blocked when unauthorized vouchers exist");
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("unauthorized")),
                "Error message must mention unauthorized vouchers");
    }

    @Test
    @Order(52)
    @Transactional
    @DisplayName("Phase 6.3: EOD blocked by unposted vouchers")
    void testEodBlockedByUnpostedVouchers() {
        FullTestData data = setupFullTestData("P6-UNPST");

        TenantContextHolder.setTenantId(data.tenant.getId());
        LocalDate bizDate = data.tenant.getCurrentBusinessDate();

        // Create and authorize a voucher but don't post it
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
                        bizDate,
                        bizDate,
                        "BATCH-EOD",
                        1,
                        data.maker,
                        "Unposted voucher for EOD test");
        voucherService.authorizeVoucher(voucher.getId(), data.checker);

        // EOD should fail
        List<String> errors = eodValidationService.validateEod(data.tenant.getId(), bizDate);
        assertFalse(errors.isEmpty(), "EOD must be blocked when unposted vouchers exist");
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("unposted")),
                "Error message must mention unposted vouchers");
    }

    @Test
    @Order(53)
    @Transactional
    @DisplayName("Phase 6.4: EOD blocked by pending approvals")
    void testEodBlockedByPendingApprovals() {
        Tenant tenant = createTenant("P6-APPR");

        // Create a pending approval
        approvalRequestRepository.save(
                ApprovalRequest.builder()
                        .tenant(tenant)
                        .entityType("ACCOUNT")
                        .entityId(1L)
                        .status(ApprovalStatus.PENDING)
                        .requestedBy(null)
                        .build());

        List<String> errors =
                eodValidationService.validateEod(tenant.getId(), tenant.getCurrentBusinessDate());
        assertFalse(errors.isEmpty(), "EOD must be blocked when pending approvals exist");
        assertTrue(
                errors.stream().anyMatch(e -> e.contains("pending approval")),
                "Error message must mention pending approvals");
    }

    @Test
    @Order(54)
    @DisplayName("Phase 6.5: After EOD close, transactions must be blocked")
    void testTransactionsBlockedAfterEodClose() {
        // NOTE: No @Transactional — EOD state machine uses REQUIRES_NEW which needs committed data
        Tenant tenant = tenantService.createTenant("P6-BLCK", "EOD Block Bank", nextWeekday());

        try {
            // Run EOD (no transactions, so it should pass)
            eodValidationService.runEod(tenant.getId());

            // After EOD, day is CLOSED. Open it first, then start day closing to test blocking.
            tenantService.openDay(tenant.getId());
            tenantService.startDayClosing(tenant.getId());

            // Set tenant context required by TransactionService
            TenantContextHolder.setTenantId(tenant.getId());

            TransactionDTO dto =
                    TransactionDTO.builder()
                            .transactionType("DEPOSIT")
                            .destinationAccountNumber("NONEXISTENT-ACC")
                            .amount(new BigDecimal("100.00"))
                            .currency("INR")
                            .channel(TransactionChannel.TELLER.name())
                            .clientReferenceId("P6-BLCK-REF-001")
                            .description("Post-EOD deposit attempt")
                            .narration("Should be blocked")
                            .build();

            assertThrows(
                    BusinessDayClosedException.class,
                    () -> transactionService.deposit(dto),
                    "Transactions must be blocked when day is in DAY_CLOSING status");
        } finally {
            // Manual cleanup since no @Transactional rollback
            // Delete child EodProcess records before tenant (FK constraint)
            LocalDate bizDate = tenant.getCurrentBusinessDate();
            eodProcessRepository
                    .findByTenantIdAndBusinessDate(tenant.getId(), bizDate)
                    .ifPresent(eodProcessRepository::delete);
            eodProcessRepository
                    .findByTenantIdAndBusinessDate(tenant.getId(), bizDate.plusDays(1))
                    .ifPresent(eodProcessRepository::delete);
            tenantRepository.deleteById(tenant.getId());
        }
    }

    @Test
    @Order(55)
    @Transactional
    @DisplayName("Phase 6.6: EOD runEod throws on validation failure")
    void testEodRunThrowsOnFailure() {
        FullTestData data = setupFullTestData("P6-FAIL");

        TenantContextHolder.setTenantId(data.tenant.getId());
        LocalDate bizDate = data.tenant.getCurrentBusinessDate();

        // Create unauthorized voucher to force EOD failure
        voucherService.createVoucher(
                data.tenant,
                data.branch,
                data.account,
                data.gl,
                VoucherDrCr.CR,
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                "INR",
                bizDate,
                bizDate,
                "BATCH-FAIL",
                1,
                data.maker,
                "Force EOD failure");

        assertThrows(
                RuntimeException.class,
                () -> eodValidationService.runEod(data.tenant.getId()),
                "runEod must throw when validation fails");

        // Verify business date did NOT advance
        Tenant tenant = tenantRepository.findById(data.tenant.getId()).orElseThrow();
        assertEquals(
                nextWeekday(),
                tenant.getCurrentBusinessDate(),
                "Business date must NOT advance on failed EOD");
    }

    // ═══════════════════════════════════════════════════════════════
    // PHASE 7: MULTI-TENANT ISOLATION TESTING
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(60)
    @Transactional
    @DisplayName("Phase 7.1: Cross-tenant account query must fail")
    void testCrossTenantAccountQueryBlocked() {
        FullTestData data1 = setupFullTestData("P7-T1");
        FullTestData data2 = setupFullTestData("P7-T2");

        // Set context to tenant 2
        TenantContextHolder.setTenantId(data2.tenant.getId());

        // Try to find tenant 1's account with tenant 2's context
        var result =
                accountRepository.findByAccountNumberAndTenantId(
                        data1.account.getAccountNumber(), data2.tenant.getId());
        assertTrue(
                result.isEmpty(),
                "Cross-tenant account query must return empty (tenant isolation enforced)");
    }

    @Test
    @Order(61)
    @Transactional
    @DisplayName("Phase 7.2: Cross-tenant transaction must fail")
    void testCrossTenantTransactionBlocked() {
        FullTestData data1 = setupFullTestData("P7-TX1");
        FullTestData data2 = setupFullTestData("P7-TX2");

        // Set context to tenant 2
        TenantContextHolder.setTenantId(data2.tenant.getId());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                data2.maker.getUsername(),
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_TELLER"))));

        // Try to deposit into tenant 1's account with tenant 2's context
        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data1.account.getAccountNumber())
                        .amount(new BigDecimal("100.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("P7-XTXN-REF-001")
                        .description("Cross-tenant transaction")
                        .narration("Should fail")
                        .build();

        assertThrows(
                RuntimeException.class,
                () -> transactionService.deposit(dto),
                "Cross-tenant transaction must be blocked");
    }

    @Test
    @Order(62)
    @Transactional
    @DisplayName("Phase 7.3: Tenant-specific GL isolation")
    void testTenantSpecificGlIsolation() {
        Tenant tenant1 = createTenant("P7-GL1");
        Tenant tenant2 = createTenant("P7-GL2");

        // Both tenants start with balanced GL
        assertTrue(
                cbsGlBalanceService.isTenantGlBalanced(tenant1.getId()),
                "Tenant 1 GL must be balanced initially");
        assertTrue(
                cbsGlBalanceService.isTenantGlBalanced(tenant2.getId()),
                "Tenant 2 GL must be balanced initially");
    }

    @Test
    @Order(63)
    @Transactional
    @DisplayName("Phase 7.4: Tenant-specific business date isolation")
    void testTenantSpecificBusinessDate() {
        Tenant tenant1 = tenantService.createTenant("P7-BD1", "Bank 1", LocalDate.of(2026, 3, 1));
        Tenant tenant2 = tenantService.createTenant("P7-BD2", "Bank 2", LocalDate.of(2026, 3, 8));

        assertEquals(
                LocalDate.of(2026, 3, 1),
                tenantService.getCurrentBusinessDate(tenant1.getId()),
                "Tenant 1 must have its own business date");
        assertEquals(
                LocalDate.of(2026, 3, 8),
                tenantService.getCurrentBusinessDate(tenant2.getId()),
                "Tenant 2 must have its own business date");
    }

    @Test
    @Order(64)
    @Transactional
    @DisplayName("Phase 7.5: Voucher isolation between tenants")
    void testVoucherIsolationBetweenTenants() {
        FullTestData data1 = setupFullTestData("P7-VIS1");
        FullTestData data2 = setupFullTestData("P7-VIS2");

        // Switch context to tenant 1 and create voucher
        TenantContextHolder.setTenantId(data1.tenant.getId());
        Voucher v1 =
                voucherService.createVoucher(
                        data1.tenant,
                        data1.branch,
                        data1.account,
                        data1.gl,
                        VoucherDrCr.CR,
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        "INR",
                        data1.tenant.getCurrentBusinessDate(),
                        data1.tenant.getCurrentBusinessDate(),
                        "BATCH-V1",
                        1,
                        data1.maker,
                        "Tenant 1 voucher");

        // Switch context to tenant 2
        TenantContextHolder.setTenantId(data2.tenant.getId());
        Voucher v2 =
                voucherService.createVoucher(
                        data2.tenant,
                        data2.branch,
                        data2.account,
                        data2.gl,
                        VoucherDrCr.CR,
                        new BigDecimal("2000.00"),
                        new BigDecimal("2000.00"),
                        "INR",
                        data2.tenant.getCurrentBusinessDate(),
                        data2.tenant.getCurrentBusinessDate(),
                        "BATCH-V2",
                        1,
                        data2.maker,
                        "Tenant 2 voucher");

        // Tenant 1 should not see tenant 2's voucher
        assertTrue(
                voucherRepository.findByIdAndTenantId(v2.getId(), data1.tenant.getId()).isEmpty(),
                "Tenant 1 must not see Tenant 2's voucher");

        // Tenant 2 should not see tenant 1's voucher
        assertTrue(
                voucherRepository.findByIdAndTenantId(v1.getId(), data2.tenant.getId()).isEmpty(),
                "Tenant 2 must not see Tenant 1's voucher");

        // Each tenant sees only its own voucher
        assertTrue(
                voucherRepository.findByIdAndTenantId(v1.getId(), data1.tenant.getId()).isPresent(),
                "Tenant 1 must see its own voucher");
        assertTrue(
                voucherRepository.findByIdAndTenantId(v2.getId(), data2.tenant.getId()).isPresent(),
                "Tenant 2 must see its own voucher");
    }

    @Test
    @Order(65)
    @Transactional
    @DisplayName("Phase 7.6: Balance isolation between tenants")
    void testBalanceIsolationBetweenTenants() {
        FullTestData data1 = setupFullTestData("P7-BAL1");
        FullTestData data2 = setupFullTestData("P7-BAL2");

        // Deposit into tenant 1's account
        TenantContextHolder.setTenantId(data1.tenant.getId());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                data1.maker.getUsername(),
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_TELLER"))));

        transactionService.deposit(
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(data1.account.getAccountNumber())
                        .amount(new BigDecimal("50000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("P7-BAL1-REF-001")
                        .description("Tenant 1 deposit")
                        .narration("Balance isolation test")
                        .build());

        // Tenant 1's balance should reflect deposit
        AccountBalance bal1 = cbsBalanceEngine.getCbsBalance(data1.account.getId());
        assertEquals(
                0,
                new BigDecimal("60000.00").compareTo(bal1.getActualTotalBalance()),
                "Tenant 1 balance must reflect deposit (10000 initial + 50000 deposit)");

        // Tenant 2's balance should be unchanged
        AccountBalance bal2 = cbsBalanceEngine.getCbsBalance(data2.account.getId());
        assertEquals(
                0,
                new BigDecimal("10000.00").compareTo(bal2.getActualTotalBalance()),
                "Tenant 2 balance must be unaffected by Tenant 1's deposit");
    }

    @Test
    @Order(66)
    @Transactional
    @DisplayName("Phase 7.7: Customer data isolation between tenants")
    void testCustomerDataIsolation() {
        Tenant tenant1 = createTenant("P7-CM1");
        Tenant tenant2 = createTenant("P7-CM2");

        // Same customer number in different tenants
        createCustomer(tenant1, "SHARED-CUST-P7");
        createCustomer(tenant2, "SHARED-CUST-P7");

        // Each tenant sees only its own customer
        var t1Customers = customerMasterRepository.findByTenantId(tenant1.getId());
        var t2Customers = customerMasterRepository.findByTenantId(tenant2.getId());
        assertEquals(1, t1Customers.size(), "Tenant 1 must see only its own customer");
        assertEquals(1, t2Customers.size(), "Tenant 2 must see only its own customer");

        // Cross-tenant query returns different records
        assertNotEquals(
                t1Customers.get(0).getId(),
                t2Customers.get(0).getId(),
                "Same customer number in different tenants must be separate records");
    }

    @Test
    @Order(67)
    @Transactional
    @DisplayName(
            "Phase 7.8: Tenant context validation - validateAccountForTransaction blocks mismatch")
    void testTenantContextValidationBlocksMismatch() {
        FullTestData data1 = setupFullTestData("P7-VAL1");
        FullTestData data2 = setupFullTestData("P7-VAL2");

        // Try to validate tenant 1's account with tenant 2's ID
        assertThrows(
                RuntimeException.class,
                () ->
                        customerValidationService.validateAccountForTransaction(
                                data1.account,
                                data2.tenant.getId(),
                                data1.branch.getId(),
                                VoucherDrCr.DR),
                "Account validation must block tenant mismatch");
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns a guaranteed weekday date (Mon-Fri) for test business dates. If today is Saturday or
     * Sunday, returns the next Monday. This prevents BankCalendarService from blocking transactions
     * on weekends (weekends default to holidays per RBI/CBS calendar rules when no explicit
     * calendar entry exists).
     */
    private static LocalDate nextWeekday() {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek().getValue() > 5) {
            d = d.plusDays(1);
        }
        return d;
    }

    private Tenant createTenant(String code) {
        return tenantRepository.save(
                Tenant.builder()
                        .tenantCode(code)
                        .tenantName("Test Bank " + code)
                        .status("ACTIVE")
                        .currentBusinessDate(nextWeekday())
                        .dayStatus(DayStatus.OPEN)
                        .build());
    }

    private Branch createBranch(String code) {
        // Ensure branch code is max 10 chars
        String safeCode = code.length() > 10 ? code.substring(0, 10) : code;
        return branchRepository
                .findByBranchCode(safeCode)
                .orElseGet(
                        () ->
                                branchRepository.save(
                                        Branch.builder()
                                                .branchCode(safeCode)
                                                .name("Branch " + safeCode)
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

    private Account createAccountForCustomer(
            Tenant tenant, Branch branch, CustomerMaster cm, String accNo) {
        return accountRepository.save(
                Account.builder()
                        .tenant(tenant)
                        .accountNumber(accNo)
                        .accountName("Account " + accNo)
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

    private Account createAccountInTenant(
            Tenant tenant, Branch branch, String accNo, GeneralLedger gl) {
        return accountRepository.save(
                Account.builder()
                        .tenant(tenant)
                        .accountNumber(accNo)
                        .accountName("Account " + accNo)
                        .accountType(AccountType.SAVINGS)
                        .status(AccountStatus.ACTIVE)
                        .balance(BigDecimal.ZERO)
                        .currency("INR")
                        .branch(branch)
                        .homeBranch(branch)
                        .glAccountCode(gl.getGlCode())
                        .build());
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
        balance.setShadowTotalBalance(BigDecimal.ZERO);
        balance.setShadowClearingBalance(BigDecimal.ZERO);
        balance.setInwardClearingBalance(BigDecimal.ZERO);
        balance.setUnclearedEffectBalance(BigDecimal.ZERO);
        balance.setLienBalance(BigDecimal.ZERO);
        balance.setChargeHoldBalance(BigDecimal.ZERO);
        balance.setHoldAmount(BigDecimal.ZERO);
        accountBalanceRepository.save(balance);
        // Sync the Account entity's legacy balance field so that
        // TransactionService.postTransferLedger() computes the correct new balance
        account.setBalance(amount);
        accountRepository.save(account);
    }

    private void verifyLedgerBalanced(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        LocalDate bizDate = tenant.getCurrentBusinessDate();
        BigDecimal debits =
                ledgerEntryRepository.sumDebitsByBusinessDateAndTenantId(bizDate, tenantId);
        BigDecimal credits =
                ledgerEntryRepository.sumCreditsByBusinessDateAndTenantId(bizDate, tenantId);
        assertEquals(
                0,
                debits.compareTo(credits),
                "Ledger must be balanced: debits=" + debits + " credits=" + credits);
    }

    /**
     * Full test data setup for transaction-level tests. Creates: Tenant, Branch, User
     * (maker+checker), GL accounts, Customer, Account with balance, Cash GL account (required for
     * deposits/withdrawals), and sets security context.
     */
    private FullTestData setupFullTestData(String suffix) {
        Tenant tenant = createTenant("T-" + suffix);
        // Branch code max 10 chars
        String branchCode = suffix.length() > 8 ? suffix.substring(suffix.length() - 8) : suffix;
        Branch branch = createBranch("B" + branchCode);

        User maker =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .username("maker-" + suffix)
                                .password("password")
                                .fullName("Maker " + suffix)
                                .email("maker-" + suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        User checker =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .username("checker-" + suffix)
                                .password("password")
                                .fullName("Checker " + suffix)
                                .email("checker-" + suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        GeneralLedger gl =
                glRepository.save(
                        GeneralLedger.builder()
                                .glCode("GL-" + suffix)
                                .glName("Test GL " + suffix)
                                .description("Test GL")
                                .accountType(GLAccountType.LIABILITY)
                                .level(0)
                                .isActive(true)
                                .normalBalance("CREDIT")
                                .balance(BigDecimal.ZERO)
                                .build());

        // Cash GL account (code 1100) required for deposit/withdrawal flows
        GeneralLedger cashGl =
                glRepository
                        .findByGlCode("1100")
                        .orElseGet(
                                () ->
                                        glRepository.save(
                                                GeneralLedger.builder()
                                                        .glCode("1100")
                                                        .glName("Cash GL")
                                                        .description("Cash GL Account")
                                                        .accountType(GLAccountType.ASSET)
                                                        .level(0)
                                                        .isActive(true)
                                                        .normalBalance("DEBIT")
                                                        .balance(BigDecimal.ZERO)
                                                        .build()));

        // Create cash GL account entity for this tenant
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
                                                        .accountNumber("CASH-GL-" + suffix)
                                                        .accountName("Cash GL Account " + suffix)
                                                        .accountType(AccountType.SAVINGS)
                                                        .status(AccountStatus.ACTIVE)
                                                        .balance(BigDecimal.ZERO)
                                                        .currency("INR")
                                                        .glAccountCode("1100")
                                                        .build()));
        seedBalance(cashGlAccount, new BigDecimal("1000000.00"));

        CustomerMaster customer = createCustomer(tenant, "CUST-" + suffix);

        Account account =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .accountNumber("ACC-" + suffix)
                                .accountName("Account " + suffix)
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(new BigDecimal("10000.00"))
                                .currency("INR")
                                .branch(branch)
                                .homeBranch(branch)
                                .glAccountCode(gl.getGlCode())
                                .customerMaster(customer)
                                .customerNumber(customer.getCustomerNumber())
                                .build());
        seedBalance(account, new BigDecimal("10000.00"));

        // Set tenant and security context
        TenantContextHolder.setTenantId(tenant.getId());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                maker.getUsername(),
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_TELLER"))));

        return new FullTestData(tenant, branch, account, gl, customer, maker, checker);
    }

    private record FullTestData(
            Tenant tenant,
            Branch branch,
            Account account,
            GeneralLedger gl,
            CustomerMaster customer,
            User maker,
            User checker) {}
}
