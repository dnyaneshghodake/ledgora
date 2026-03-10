package com.ledgora;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.balance.service.CbsBalanceEngine;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.*;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS Balance Engine test suite. Tests: shadow balance, actual balance, clearing flow, lien logic,
 * available balance.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgoraCbsBalanceTest {

    @Autowired private CbsBalanceEngine cbsBalanceEngine;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Balance: Shadow balance updates on voucher create")
    void testShadowBalanceUpdate() {
        Account account = setupAccount("BAL-01");

        cbsBalanceEngine.updateShadowBalance(
                account.getId(), new BigDecimal("5000.0000"), VoucherDrCr.CR);

        AccountBalance balance = cbsBalanceEngine.getCbsBalance(account.getId());
        assertEquals(
                0,
                new BigDecimal("5000.0000").compareTo(balance.getShadowTotalBalance()),
                "Shadow balance should reflect pending credit");
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Balance: Actual balance updates on voucher post")
    void testActualBalanceUpdate() {
        Account account = setupAccount("BAL-02");

        cbsBalanceEngine.updateActualBalance(
                account.getId(), new BigDecimal("3000.0000"), VoucherDrCr.CR);

        AccountBalance balance = cbsBalanceEngine.getCbsBalance(account.getId());
        assertEquals(
                0,
                new BigDecimal("3000.0000").compareTo(balance.getActualTotalBalance()),
                "Actual total balance should be updated");
        assertEquals(
                0,
                new BigDecimal("3000.0000").compareTo(balance.getActualClearedBalance()),
                "Actual cleared balance should equal actual total (no uncleared)");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("Balance: Clearing flow works (update clearing -> move to actual)")
    void testClearingFlow() {
        Account account = setupAccount("BAL-03");

        // First add actual balance
        cbsBalanceEngine.updateActualBalance(
                account.getId(), new BigDecimal("10000.0000"), VoucherDrCr.CR);

        // Add clearing amount
        cbsBalanceEngine.updateClearingBalance(account.getId(), new BigDecimal("2000.0000"));

        AccountBalance afterClearing = cbsBalanceEngine.getCbsBalance(account.getId());
        assertEquals(
                0,
                new BigDecimal("2000.0000").compareTo(afterClearing.getUnclearedEffectBalance()),
                "Uncleared effect should be 2000");
        assertEquals(
                0,
                new BigDecimal("8000.0000").compareTo(afterClearing.getActualClearedBalance()),
                "Cleared balance should be actual - uncleared = 8000");

        // Move clearing to actual
        cbsBalanceEngine.moveClearingToActual(account.getId(), new BigDecimal("2000.0000"));

        AccountBalance afterMove = cbsBalanceEngine.getCbsBalance(account.getId());
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(afterMove.getUnclearedEffectBalance()),
                "Uncleared effect should be 0 after move");
        assertEquals(
                0,
                new BigDecimal("10000.0000").compareTo(afterMove.getActualClearedBalance()),
                "Cleared balance should equal actual total after clearing");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("Balance: Lien logic correctly reduces available balance")
    void testLienLogic() {
        Account account = setupAccount("BAL-04");

        cbsBalanceEngine.updateActualBalance(
                account.getId(), new BigDecimal("10000.0000"), VoucherDrCr.CR);

        cbsBalanceEngine.applyLien(account.getId(), new BigDecimal("3000.0000"));

        AccountBalance balance = cbsBalanceEngine.getCbsBalance(account.getId());
        assertEquals(
                0,
                new BigDecimal("3000.0000").compareTo(balance.getLienBalance()),
                "Lien balance should be 3000");
        assertEquals(
                0,
                new BigDecimal("7000.0000").compareTo(balance.getAvailableBalance()),
                "Available balance should be 10000 - 3000 = 7000");
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("Balance: Lien release increases available balance")
    void testLienRelease() {
        Account account = setupAccount("BAL-05");

        cbsBalanceEngine.updateActualBalance(
                account.getId(), new BigDecimal("10000.0000"), VoucherDrCr.CR);
        cbsBalanceEngine.applyLien(account.getId(), new BigDecimal("5000.0000"));
        cbsBalanceEngine.releaseLien(account.getId(), new BigDecimal("2000.0000"));

        AccountBalance balance = cbsBalanceEngine.getCbsBalance(account.getId());
        assertEquals(
                0,
                new BigDecimal("3000.0000").compareTo(balance.getLienBalance()),
                "Lien balance should be 5000 - 2000 = 3000");
        assertEquals(
                0,
                new BigDecimal("7000.0000").compareTo(balance.getAvailableBalance()),
                "Available balance should be 10000 - 3000 = 7000");
    }

    @Test
    @Order(6)
    @Transactional
    @DisplayName("Balance: Lien exceeding available balance fails (no OD)")
    void testLienExceedingAvailableFails() {
        Account account = setupAccount("BAL-06");

        cbsBalanceEngine.updateActualBalance(
                account.getId(), new BigDecimal("1000.0000"), VoucherDrCr.CR);

        assertThrows(
                RuntimeException.class,
                () -> cbsBalanceEngine.applyLien(account.getId(), new BigDecimal("5000.0000")),
                "Lien exceeding available balance should fail when OD not permitted");
    }

    @Test
    @Order(7)
    @Transactional
    @DisplayName("Balance: Available balance formula = cleared - lien - charge_hold")
    void testAvailableBalanceFormula() {
        Account account = setupAccount("BAL-07");

        // Set up balance components
        cbsBalanceEngine.updateActualBalance(
                account.getId(), new BigDecimal("20000.0000"), VoucherDrCr.CR);
        cbsBalanceEngine.updateClearingBalance(
                account.getId(), new BigDecimal("5000.0000")); // uncleared
        cbsBalanceEngine.applyLien(account.getId(), new BigDecimal("2000.0000"));

        AccountBalance balance = cbsBalanceEngine.getCbsBalance(account.getId());

        // actual_total = 20000
        // uncleared = 5000
        // actual_cleared = 20000 - 5000 = 15000
        // lien = 2000
        // available = 15000 - 2000 - 0 = 13000
        assertEquals(0, new BigDecimal("20000.0000").compareTo(balance.getActualTotalBalance()));
        assertEquals(0, new BigDecimal("15000.0000").compareTo(balance.getActualClearedBalance()));
        assertEquals(0, new BigDecimal("2000.0000").compareTo(balance.getLienBalance()));
        assertEquals(0, new BigDecimal("13000.0000").compareTo(balance.getAvailableBalance()));
    }

    @Test
    @Order(8)
    @Transactional
    @DisplayName("Balance: Debit reduces actual balance")
    void testDebitReducesActualBalance() {
        Account account = setupAccount("BAL-08");

        cbsBalanceEngine.updateActualBalance(
                account.getId(), new BigDecimal("10000.0000"), VoucherDrCr.CR);
        cbsBalanceEngine.updateActualBalance(
                account.getId(), new BigDecimal("3000.0000"), VoucherDrCr.DR);

        AccountBalance balance = cbsBalanceEngine.getCbsBalance(account.getId());
        assertEquals(
                0,
                new BigDecimal("7000.0000").compareTo(balance.getActualTotalBalance()),
                "Actual balance should be 10000 - 3000 = 7000");
    }

    // ═══════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════

    private Account setupAccount(String suffix) {
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

        return accountRepository.save(
                Account.builder()
                        .tenant(tenant)
                        .accountNumber("A-" + suffix)
                        .accountName("Test Account " + suffix)
                        .accountType(AccountType.SAVINGS)
                        .status(AccountStatus.ACTIVE)
                        .balance(BigDecimal.ZERO)
                        .currency("INR")
                        .branch(branch)
                        .build());
    }
}
