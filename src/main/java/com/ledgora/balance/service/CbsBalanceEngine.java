package com.ledgora.balance.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.VoucherDrCr;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS Balance Engine - manages shadow, clearing, lien, and actual balances.
 *
 * <p>Balance formulas: actual_total_balance = SUM(ledger_entries) actual_cleared_balance =
 * actual_total_balance - uncleared_effect_balance shadow_total_balance = actual_total_balance +
 * pending_voucher_effect available_balance = actual_cleared_balance - lien_balance -
 * charge_hold_balance
 *
 * <p>Never allow negative available_balance unless OD permitted.
 */
@Service
public class CbsBalanceEngine {

    private static final Logger log = LoggerFactory.getLogger(CbsBalanceEngine.class);
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;

    public CbsBalanceEngine(
            AccountBalanceRepository accountBalanceRepository,
            AccountRepository accountRepository) {
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Update shadow balance when a voucher is created (before posting). Shadow reflects pending
     * financial effect.
     */
    @Transactional
    public void updateShadowBalance(Long accountId, BigDecimal amount, VoucherDrCr drCr) {
        AccountBalance balance = getOrCreateAccountBalance(accountId);
        BigDecimal delta = drCr == VoucherDrCr.CR ? amount : amount.negate();
        balance.setShadowTotalBalance(balance.getShadowTotalBalance().add(delta));
        recomputeAvailableBalance(balance);
        accountBalanceRepository.save(balance);
        log.debug(
                "Shadow balance updated for account {}: delta={}, shadow_total={}",
                accountId,
                delta,
                balance.getShadowTotalBalance());
    }

    /**
     * Update actual balance when a voucher is posted (creates ledger entry). Also reduces the
     * shadow delta since the entry is now actual.
     */
    @Transactional
    public void updateActualBalance(Long accountId, BigDecimal amount, VoucherDrCr drCr) {
        AccountBalance balance = getOrCreateAccountBalance(accountId);
        BigDecimal delta = drCr == VoucherDrCr.CR ? amount : amount.negate();

        // Update actual
        balance.setActualTotalBalance(balance.getActualTotalBalance().add(delta));
        balance.setActualClearedBalance(
                balance.getActualTotalBalance().subtract(balance.getUnclearedEffectBalance()));

        // Reduce shadow delta (the pending effect is now actual)
        balance.setShadowTotalBalance(balance.getShadowTotalBalance().subtract(delta));

        // Update legacy fields for backward compatibility
        balance.setLedgerBalance(balance.getActualTotalBalance());

        recomputeAvailableBalance(balance);
        accountBalanceRepository.save(balance);
        log.debug(
                "Actual balance updated for account {}: actual_total={}, shadow_total={}",
                accountId,
                balance.getActualTotalBalance(),
                balance.getShadowTotalBalance());
    }

    /** Update clearing balance (inward clearing instruments). */
    @Transactional
    public void updateClearingBalance(Long accountId, BigDecimal amount) {
        AccountBalance balance = getOrCreateAccountBalance(accountId);
        balance.setInwardClearingBalance(balance.getInwardClearingBalance().add(amount));
        balance.setUnclearedEffectBalance(balance.getUnclearedEffectBalance().add(amount));
        balance.setActualClearedBalance(
                balance.getActualTotalBalance().subtract(balance.getUnclearedEffectBalance()));
        recomputeAvailableBalance(balance);
        accountBalanceRepository.save(balance);
        log.debug(
                "Clearing balance updated for account {}: inward_clearing={}, uncleared={}",
                accountId,
                balance.getInwardClearingBalance(),
                balance.getUnclearedEffectBalance());
    }

    /** Move clearing balance to actual (when instrument clears). */
    @Transactional
    public void moveClearingToActual(Long accountId, BigDecimal amount) {
        AccountBalance balance = getOrCreateAccountBalance(accountId);
        balance.setUnclearedEffectBalance(balance.getUnclearedEffectBalance().subtract(amount));
        if (balance.getUnclearedEffectBalance().compareTo(BigDecimal.ZERO) < 0) {
            balance.setUnclearedEffectBalance(BigDecimal.ZERO);
        }
        balance.setInwardClearingBalance(balance.getInwardClearingBalance().subtract(amount));
        if (balance.getInwardClearingBalance().compareTo(BigDecimal.ZERO) < 0) {
            balance.setInwardClearingBalance(BigDecimal.ZERO);
        }
        balance.setActualClearedBalance(
                balance.getActualTotalBalance().subtract(balance.getUnclearedEffectBalance()));
        recomputeAvailableBalance(balance);
        accountBalanceRepository.save(balance);
        log.debug("Clearing moved to actual for account {}: cleared_amount={}", accountId, amount);
    }

    /** Apply a lien on an account. */
    @Transactional
    public void applyLien(Long accountId, BigDecimal lienAmount) {
        AccountBalance balance = getOrCreateAccountBalance(accountId);
        balance.setLienBalance(balance.getLienBalance().add(lienAmount));
        recomputeAvailableBalance(balance);

        // Check if available balance goes negative (unless OD permitted)
        if (!Boolean.TRUE.equals(balance.getOdPermitted())
                && balance.getAvailableBalance().compareTo(BigDecimal.ZERO) < 0) {
            // Rollback the lien
            balance.setLienBalance(balance.getLienBalance().subtract(lienAmount));
            recomputeAvailableBalance(balance);
            accountBalanceRepository.save(balance);
            throw new RuntimeException(
                    "Insufficient available balance to apply lien of "
                            + lienAmount
                            + " on account "
                            + accountId);
        }

        accountBalanceRepository.save(balance);
        log.info(
                "Lien applied on account {}: lien_amount={}, total_lien={}, available={}",
                accountId,
                lienAmount,
                balance.getLienBalance(),
                balance.getAvailableBalance());
    }

    /** Release a lien on an account. */
    @Transactional
    public void releaseLien(Long accountId, BigDecimal lienAmount) {
        AccountBalance balance = getOrCreateAccountBalance(accountId);
        BigDecimal newLien = balance.getLienBalance().subtract(lienAmount);
        if (newLien.compareTo(BigDecimal.ZERO) < 0) {
            newLien = BigDecimal.ZERO;
        }
        balance.setLienBalance(newLien);
        recomputeAvailableBalance(balance);
        accountBalanceRepository.save(balance);
        log.info(
                "Lien released on account {}: released={}, remaining_lien={}, available={}",
                accountId,
                lienAmount,
                balance.getLienBalance(),
                balance.getAvailableBalance());
    }

    /**
     * Recompute available balance based on CBS formula: available_balance = actual_cleared_balance
     * - lien_balance - charge_hold_balance
     */
    public void recomputeAvailableBalance(AccountBalance balance) {
        BigDecimal available =
                balance.getActualClearedBalance()
                        .subtract(balance.getLienBalance())
                        .subtract(balance.getChargeHoldBalance());
        balance.setAvailableBalance(available);
    }

    /** Get the current CBS balance for an account. */
    public AccountBalance getCbsBalance(Long accountId) {
        return getOrCreateAccountBalance(accountId);
    }

    /** Validate available balance is sufficient for a debit transaction. */
    public void validateSufficientBalance(Long accountId, BigDecimal amount) {
        AccountBalance balance = getOrCreateAccountBalance(accountId);
        recomputeAvailableBalance(balance);
        if (!Boolean.TRUE.equals(balance.getOdPermitted())
                && balance.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException(
                    "Insufficient available balance on account "
                            + accountId
                            + ": available="
                            + balance.getAvailableBalance()
                            + ", required="
                            + amount);
        }
    }

    private AccountBalance getOrCreateAccountBalance(Long accountId) {
        return accountBalanceRepository
                .findByAccountId(accountId)
                .orElseGet(
                        () -> {
                            Long tenantId =
                                    com.ledgora.tenant.context.TenantContextHolder
                                            .getRequiredTenantId();
                            Account account =
                                    accountRepository
                                            .findByIdAndTenantId(accountId, tenantId)
                                            .orElseThrow(
                                                    () ->
                                                            new RuntimeException(
                                                                    "Account not found: "
                                                                            + accountId));
                            AccountBalance ab =
                                    AccountBalance.builder()
                                            .account(account)
                                            .ledgerBalance(BigDecimal.ZERO)
                                            .availableBalance(BigDecimal.ZERO)
                                            .holdAmount(BigDecimal.ZERO)
                                            .actualTotalBalance(BigDecimal.ZERO)
                                            .actualClearedBalance(BigDecimal.ZERO)
                                            .shadowTotalBalance(BigDecimal.ZERO)
                                            .shadowClearingBalance(BigDecimal.ZERO)
                                            .inwardClearingBalance(BigDecimal.ZERO)
                                            .unclearedEffectBalance(BigDecimal.ZERO)
                                            .lienBalance(BigDecimal.ZERO)
                                            .chargeHoldBalance(BigDecimal.ZERO)
                                            .odPermitted(false)
                                            .build();
                            return accountBalanceRepository.save(ab);
                        });
    }
}
