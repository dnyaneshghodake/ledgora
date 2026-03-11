package com.ledgora.reconciliation.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.EntryType;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.reconciliation.entity.ReconciliationException;
import com.ledgora.reconciliation.repository.ReconciliationExceptionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBS-grade Balance Reconciliation Service. Validates that every account's cached
 * balance field matches the true ledger balance (SUM credits − SUM debits).
 *
 * <p>Runs as:
 * <ul>
 *   <li>Scheduled job every 5 minutes (lightweight — checks tenant accounts)
 *   <li>EOD pre-close validation (blocks day close if mismatch exceeds threshold)
 * </ul>
 *
 * <p>On mismatch detection:
 * <ol>
 *   <li>Log to reconciliation_exceptions table
 *   <li>Auto-correct account.balance to match ledger
 *   <li>Mark exception as auto-corrected + resolved
 * </ol>
 *
 * <p>EOD blocking: if total unresolved mismatch > EOD_BLOCK_THRESHOLD, day close
 * is prevented until manual review.
 */
@Service
public class BalanceReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(BalanceReconciliationService.class);

    /** Maximum total mismatch allowed before EOD is blocked (₹1.00 tolerance). */
    private static final BigDecimal EOD_BLOCK_THRESHOLD = new BigDecimal("1.0000");

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ReconciliationExceptionRepository exceptionRepository;

    public BalanceReconciliationService(
            AccountRepository accountRepository,
            LedgerEntryRepository ledgerEntryRepository,
            ReconciliationExceptionRepository exceptionRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.exceptionRepository = exceptionRepository;
    }

    /**
     * Scheduled reconciliation — runs every 5 minutes.
     * Lightweight: processes all accounts for tenant ID 1 (default tenant).
     * In production, this would iterate all active tenants.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void scheduledReconciliation() {
        try {
            reconcileTenant(1L, LocalDate.now());
        } catch (Exception e) {
            log.error("Scheduled reconciliation failed: {}", e.getMessage());
        }
    }

    /**
     * Run full balance reconciliation for a tenant.
     *
     * @param tenantId the tenant to reconcile
     * @param businessDate the business date for exception logging
     * @return number of mismatches found
     */
    @Transactional
    public int reconcileTenant(Long tenantId, LocalDate businessDate) {
        List<Account> accounts = accountRepository.findByTenantId(tenantId);

        int mismatches = 0;
        int corrected = 0;

        for (Account account : accounts) {
            BigDecimal cachedBalance = account.getBalance();
            BigDecimal ledgerBalance = computeLedgerBalance(account.getId());

            if (cachedBalance.compareTo(ledgerBalance) != 0) {
                BigDecimal mismatch = cachedBalance.subtract(ledgerBalance).abs();

                // Log exception
                ReconciliationException exception = ReconciliationException.builder()
                        .tenantId(tenantId)
                        .accountId(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .businessDate(businessDate)
                        .cachedBalance(cachedBalance)
                        .ledgerBalance(ledgerBalance)
                        .mismatchAmount(mismatch)
                        .build();

                // Auto-correct: set account.balance to ledger truth
                account.setBalance(ledgerBalance);
                accountRepository.save(account);

                exception.setAutoCorrected(true);
                exception.setResolved(true);
                exception.setResolvedAt(LocalDateTime.now());
                exception.setResolutionNotes(
                        "Auto-corrected from " + cachedBalance + " to " + ledgerBalance);

                exceptionRepository.save(exception);

                log.warn("RECONCILIATION MISMATCH: account={} cached={} ledger={} mismatch={} → AUTO-CORRECTED",
                        account.getAccountNumber(), cachedBalance, ledgerBalance, mismatch);

                mismatches++;
                corrected++;
            }
        }

        if (mismatches > 0) {
            log.info("Reconciliation for tenant {}: {} mismatches found, {} auto-corrected",
                    tenantId, mismatches, corrected);
        } else {
            log.debug("Reconciliation for tenant {}: all {} accounts balanced", tenantId, accounts.size());
        }

        return mismatches;
    }

    /**
     * EOD pre-close validation. Blocks day close if unresolved mismatches
     * exceed the threshold.
     *
     * @param tenantId the tenant to validate
     * @param businessDate the business date being closed
     * @throws RuntimeException if mismatch exceeds threshold
     */
    public void validateForEodClose(Long tenantId, LocalDate businessDate) {
        // First run a fresh reconciliation
        int mismatches = reconcileTenant(tenantId, businessDate);

        // Check if any unresolved exceptions remain above threshold
        BigDecimal unresolvedTotal =
                exceptionRepository.sumUnresolvedMismatchByTenantIdAndDate(tenantId, businessDate);

        if (unresolvedTotal.compareTo(EOD_BLOCK_THRESHOLD) > 0) {
            throw new RuntimeException(
                    "EOD BLOCKED: Unresolved reconciliation mismatches total "
                            + unresolvedTotal
                            + " exceeds threshold "
                            + EOD_BLOCK_THRESHOLD
                            + ". Manual review required before day close.");
        }

        long unresolvedCount =
                exceptionRepository.countByTenantIdAndBusinessDateAndResolvedFalse(
                        tenantId, businessDate);

        if (unresolvedCount > 0) {
            log.warn("EOD validation: {} unresolved exceptions for tenant {} date {} (within threshold)",
                    unresolvedCount, tenantId, businessDate);
        } else {
            log.info("EOD validation passed: all accounts reconciled for tenant {} date {}",
                    tenantId, businessDate);
        }
    }

    /**
     * Compute the true ledger balance for an account.
     * True balance = SUM(CREDIT amounts) - SUM(DEBIT amounts) from LedgerEntry.
     */
    private BigDecimal computeLedgerBalance(Long accountId) {
        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal debits = BigDecimal.ZERO;

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountId(accountId);
        for (LedgerEntry entry : entries) {
            if (entry.getEntryType() == EntryType.CREDIT) {
                credits = credits.add(entry.getAmount());
            } else if (entry.getEntryType() == EntryType.DEBIT) {
                debits = debits.add(entry.getAmount());
            }
        }

        return credits.subtract(debits);
    }

    /**
     * Get all exceptions for a tenant on a date (for EOD validation dashboard).
     */
    public List<ReconciliationException> getExceptions(Long tenantId, LocalDate businessDate) {
        return exceptionRepository.findByTenantIdAndBusinessDate(tenantId, businessDate);
    }

    /**
     * Get all unresolved exceptions for a tenant (for governance dashboard).
     */
    public List<ReconciliationException> getUnresolvedException(Long tenantId) {
        return exceptionRepository.findUnresolvedByTenantId(tenantId);
    }
}
