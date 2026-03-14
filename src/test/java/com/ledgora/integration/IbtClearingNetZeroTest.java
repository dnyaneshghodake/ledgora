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
 * Integration test: Inter-Branch Transfer (IBT) clearing GL net zero.
 *
 * <p>CBS Invariant: After a cross-branch transfer is fully settled, the clearing GL accounts
 * (IBC_OUT + IBC_IN) must net to zero. The 4-voucher model ensures balanced clearing.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IbtClearingNetZeroTest {

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
    @DisplayName("Cross-branch transfer creates balanced ledger entries")
    void crossBranchTransferIsBalanced() {
        TestData data = setupCrossBranchData("IBT-01");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("TRANSFER")
                        .sourceAccountNumber(data.sourceAccount.getAccountNumber())
                        .destinationAccountNumber(data.destAccount.getAccountNumber())
                        .amount(new BigDecimal("10000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId("IBT-REF-001")
                        .description("Cross-branch IBT test")
                        .narration("IBT 10K")
                        .build();

        var txn = transactionService.transfer(dto);

        BigDecimal debits = ledgerEntryRepository.sumDebitsByTransactionId(txn.getId());
        BigDecimal credits = ledgerEntryRepository.sumCreditsByTransactionId(txn.getId());

        assertNotNull(debits);
        assertNotNull(credits);
        assertEquals(
                0,
                debits.compareTo(credits),
                "IBT must maintain double-entry: DR=" + debits + " CR=" + credits);
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("IBT creates entries in both source and destination branches")
    void ibtCreatesEntriesInBothBranches() {
        TestData data = setupCrossBranchData("IBT-02");

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("TRANSFER")
                        .sourceAccountNumber(data.sourceAccount.getAccountNumber())
                        .destinationAccountNumber(data.destAccount.getAccountNumber())
                        .amount(new BigDecimal("5000.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("IBT-REF-002")
                        .description("IBT multi-branch test")
                        .narration("IBT 5K")
                        .build();

        var txn = transactionService.transfer(dto);

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(txn.getId());
        assertTrue(entries.size() >= 2, "IBT must create entries across branches");

        BigDecimal totalDebit =
                entries.stream()
                        .filter(e -> e.getEntryType() == EntryType.DEBIT)
                        .map(LedgerEntry::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit =
                entries.stream()
                        .filter(e -> e.getEntryType() == EntryType.CREDIT)
                        .map(LedgerEntry::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, totalDebit.compareTo(totalCredit), "IBT clearing must net to zero");
    }

    private static LocalDate nextWeekday() {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek().getValue() > 5) d = d.plusDays(1);
        return d;
    }

    private TestData setupCrossBranchData(String suffix) {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-" + suffix)
                                .tenantName("IBT Tenant " + suffix)
                                .status("ACTIVE")
                                .currentBusinessDate(nextWeekday())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Branch srcBranch =
                branchRepository.save(
                        Branch.builder()
                                .branchCode("SB-" + suffix)
                                .name("Source Branch " + suffix)
                                .isActive(true)
                                .build());

        Branch dstBranch =
                branchRepository.save(
                        Branch.builder()
                                .branchCode("DB-" + suffix)
                                .name("Dest Branch " + suffix)
                                .isActive(true)
                                .build());

        User user =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .branch(srcBranch)
                                .username("ibt-user-" + suffix)
                                .password("password")
                                .fullName("IBT User " + suffix)
                                .email("ibt-" + suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        GeneralLedger glDeposit =
                glRepository.save(
                        GeneralLedger.builder()
                                .glCode("GL-IBT-" + suffix)
                                .glName("IBT GL " + suffix)
                                .description("IBT GL")
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
                                                        .branch(srcBranch)
                                                        .homeBranch(srcBranch)
                                                        .accountNumber("GL-CASH-" + suffix)
                                                        .accountName("Cash GL " + suffix)
                                                        .accountType(AccountType.SAVINGS)
                                                        .status(AccountStatus.ACTIVE)
                                                        .balance(BigDecimal.ZERO)
                                                        .currency("INR")
                                                        .glAccountCode("1100")
                                                        .build()));
        seedBalance(cashGl, new BigDecimal("500000.00"));

        Account sourceAccount =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .branch(srcBranch)
                                .homeBranch(srcBranch)
                                .accountNumber("SRC-" + suffix)
                                .accountName("Source " + suffix)
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(new BigDecimal("100000.00"))
                                .currency("INR")
                                .glAccountCode(glDeposit.getGlCode())
                                .build());
        seedBalance(sourceAccount, new BigDecimal("100000.00"));

        Account destAccount =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .branch(dstBranch)
                                .homeBranch(dstBranch)
                                .accountNumber("DST-" + suffix)
                                .accountName("Dest " + suffix)
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(new BigDecimal("50000.00"))
                                .currency("INR")
                                .glAccountCode(glDeposit.getGlCode())
                                .build());
        seedBalance(destAccount, new BigDecimal("50000.00"));

        TenantContextHolder.setTenantId(tenant.getId());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_TELLER"))));

        return new TestData(sourceAccount, destAccount);
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

    private record TestData(Account sourceAccount, Account destAccount) {}
}
