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
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.service.TransactionService;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class LedgoraTransactionVoucherInvariantIntegrationTest {

    @Autowired private TransactionService transactionService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private GeneralLedgerRepository glRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private LedgerEntryRepository ledgerEntryRepository;
    @Autowired private TransactionBatchRepository batchRepository;

    @AfterEach
    void cleanupThreadLocals() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    @DisplayName("Transfer flow creates balanced ledger via vouchers only")
    void transferCreatesBalancedLedgerThroughVouchers() {
        TestData data = setupTestData("INV-TRF");

        long voucherCountBefore = voucherRepository.count();

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("TRANSFER")
                        .sourceAccountNumber(data.source.getAccountNumber())
                        .destinationAccountNumber(data.destination.getAccountNumber())
                        .amount(new BigDecimal("250.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("INV-TRF-REF-1")
                        .description("Invariant transfer")
                        .narration("Invariant transfer test")
                        .build();

        Transaction txn = transactionService.transfer(dto);

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(txn.getId());
        assertEquals(2, entries.size(), "Transfer should create exactly 2 ledger entries");

        BigDecimal debits = ledgerEntryRepository.sumDebitsByTransactionId(txn.getId());
        BigDecimal credits = ledgerEntryRepository.sumCreditsByTransactionId(txn.getId());
        assertEquals(
                0,
                debits.compareTo(credits),
                "Transaction journal must be balanced (debit == credit)");

        long voucherCountAfter = voucherRepository.count();
        assertEquals(
                voucherCountBefore + 2,
                voucherCountAfter,
                "Transfer should create two vouchers (debit leg and credit leg)");

        TransactionBatch batch = batchRepository.findById(txn.getBatch().getId()).orElseThrow();
        String expectedBatchCode =
                batch.getBatchCode() != null ? batch.getBatchCode() : "BATCH-" + batch.getId();

        List<Voucher> linkedVouchers =
                voucherRepository.findAll().stream()
                        .filter(v -> expectedBatchCode.equals(v.getBatchCode()))
                        .filter(v -> v.getPostingDate().equals(txn.getBusinessDate()))
                        .filter(
                                v ->
                                        v.getNarration() != null
                                                && v.getNarration().contains("Transfer"))
                        .collect(Collectors.toList());

        assertTrue(linkedVouchers.size() >= 2, "Expected posted vouchers for transfer legs");
        linkedVouchers.forEach(
                v -> {
                    assertEquals(
                            "Y", v.getAuthFlag(), "Voucher leg must be authorized before posting");
                    assertEquals("Y", v.getPostFlag(), "Voucher leg must be posted");
                    assertNotNull(
                            v.getLedgerEntry(),
                            "Posted voucher must link to immutable ledger entry");
                });
    }

    @Test
    @Transactional
    @DisplayName(
            "Ledger repository delete methods throw UnsupportedOperationException (CBS immutability)")
    void ledgerRepositoryIsAppendOnlyByContract() {
        // CBS Rule: LedgerEntries must never be deleted. Delete methods exist but throw.
        assertThrows(
                UnsupportedOperationException.class,
                () -> ledgerEntryRepository.deleteById(999L),
                "deleteById must throw UnsupportedOperationException for CBS immutability");
        assertThrows(
                UnsupportedOperationException.class,
                () -> ledgerEntryRepository.deleteAll(),
                "deleteAll must throw UnsupportedOperationException for CBS immutability");
    }

    /**
     * Returns a guaranteed weekday date (Mon-Fri) for test business dates.
     * Prevents BankCalendarService from blocking transactions on weekends.
     */
    private static LocalDate nextWeekday() {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek().getValue() > 5) {
            d = d.plusDays(1);
        }
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
                                .username("txn-user-" + suffix)
                                .password("password")
                                .fullName("Txn User " + suffix)
                                .email("txn-" + suffix + "@test.com")
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
                                .balance(new BigDecimal("2000.00"))
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
                                .balance(new BigDecimal("1000.00"))
                                .currency("INR")
                                .glAccountCode(glDeposit.getGlCode())
                                .build());

        Account cash =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .homeBranch(branch)
                                .accountNumber("GL-CASH-" + suffix)
                                .accountName("Cash GL Account " + suffix)
                                .accountType(AccountType.GL_ACCOUNT)
                                .status(AccountStatus.ACTIVE)
                                .balance(new BigDecimal("5000.00"))
                                .currency("INR")
                                .glAccountCode(glCash.getGlCode())
                                .build());

        seedBalance(source, new BigDecimal("2000.00"));
        seedBalance(destination, new BigDecimal("1000.00"));
        seedBalance(cash, new BigDecimal("5000.00"));

        TenantContextHolder.setTenantId(tenant.getId());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(), "N/A", List.of()));

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
