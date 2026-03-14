package com.ledgora.stress.service;

import com.ledgora.account.repository.AccountRepository;
import com.ledgora.clearing.repository.InterBranchTransferRepository;
import com.ledgora.common.enums.AccountType;
import com.ledgora.eod.service.EodValidationService;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.stress.dto.EodPerformanceResult;
import com.ledgora.suspense.repository.SuspenseCaseRepository;
import com.ledgora.transaction.repository.TransactionRepository;
import com.ledgora.voucher.repository.VoucherRepository;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Executes EOD under Hibernate statistics monitoring and produces a structured performance report.
 *
 * <p>Active only in the "stress" profile. Does NOT modify EOD logic — just wraps the existing
 * {@link EodValidationService#runEod(Long)} with timing + statistics capture.
 *
 * <p>Performance metrics captured:
 *
 * <ul>
 *   <li>Wall-clock execution time (ms)
 *   <li>Hibernate prepareStatementCount
 *   <li>Hibernate entityLoadCount
 *   <li>Hibernate queryExecutionCount
 * </ul>
 *
 * <p>Post-EOD validation:
 *
 * <ul>
 *   <li>Clearing GL net = 0
 *   <li>Suspense GL net = 0
 *   <li>EOD completed without exception
 * </ul>
 */
@Service
@Profile("stress")
public class EodPerformanceService {

    private static final Logger log = LoggerFactory.getLogger(EodPerformanceService.class);

    private final EodValidationService eodValidationService;
    private final EntityManagerFactory entityManagerFactory;
    private final TransactionRepository transactionRepository;
    private final VoucherRepository voucherRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final InterBranchTransferRepository ibtRepository;
    private final SuspenseCaseRepository suspenseCaseRepository;
    private final AccountRepository accountRepository;

    public EodPerformanceService(
            EodValidationService eodValidationService,
            EntityManagerFactory entityManagerFactory,
            TransactionRepository transactionRepository,
            VoucherRepository voucherRepository,
            LedgerEntryRepository ledgerEntryRepository,
            InterBranchTransferRepository ibtRepository,
            SuspenseCaseRepository suspenseCaseRepository,
            AccountRepository accountRepository) {
        this.eodValidationService = eodValidationService;
        this.entityManagerFactory = entityManagerFactory;
        this.transactionRepository = transactionRepository;
        this.voucherRepository = voucherRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ibtRepository = ibtRepository;
        this.suspenseCaseRepository = suspenseCaseRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Run EOD with full performance instrumentation.
     *
     * @param tenantId tenant to run EOD for
     * @param loadResult partial result from load generation (accounts/transactions/ibt counts)
     * @return complete performance result with EOD metrics
     */
    public EodPerformanceResult runEodPerformanceTest(
            Long tenantId, EodPerformanceResult.EodPerformanceResultBuilder loadResult) {

        // Capture pre-EOD counts
        long totalTxns = transactionRepository.countByTenantId(tenantId);
        long totalVouchers = voucherRepository.count();
        long totalLedgerEntries = ledgerEntryRepository.count();
        long totalIbt = ibtRepository.count();
        long totalSuspense = suspenseCaseRepository.countOpenByTenantId(tenantId);

        loadResult
                .totalTransactions(totalTxns)
                .totalVouchers(totalVouchers)
                .totalLedgerEntries(totalLedgerEntries)
                .totalIbtTransfers(totalIbt)
                .totalSuspenseCases(totalSuspense);

        log.info(
                "Stress: Pre-EOD state — txns={} vouchers={} ledger={} ibt={} suspense={}",
                totalTxns,
                totalVouchers,
                totalLedgerEntries,
                totalIbt,
                totalSuspense);

        // Get Hibernate statistics
        Statistics stats = getHibernateStatistics();
        if (stats != null) {
            stats.clear();
        }

        // Execute EOD with timing
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String failureReason = null;

        try {
            eodValidationService.runEod(tenantId);
            success = true;
            log.info("Stress: EOD completed successfully");
        } catch (Exception e) {
            failureReason = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("Stress: EOD failed — {}", failureReason, e);
        }

        long endTime = System.currentTimeMillis();
        long executionTimeMs = endTime - startTime;

        // Capture Hibernate stats
        long sqlCount = 0;
        long entityLoads = 0;
        long queryCount = 0;
        if (stats != null) {
            sqlCount = stats.getPrepareStatementCount();
            entityLoads = stats.getEntityLoadCount();
            queryCount = stats.getQueryExecutionCount();
        }

        // Post-EOD validation
        BigDecimal clearingNet =
                accountRepository.sumBalanceByTenantIdAndAccountType(
                        tenantId, AccountType.CLEARING_ACCOUNT);
        BigDecimal suspenseNet =
                accountRepository.sumBalanceByTenantIdAndAccountType(
                        tenantId, AccountType.SUSPENSE_ACCOUNT);

        EodPerformanceResult result =
                loadResult
                        .executionTimeMs(executionTimeMs)
                        .sqlStatementCount(sqlCount)
                        .entityLoadCount(entityLoads)
                        .queryExecutionCount(queryCount)
                        .clearingGlZero(clearingNet.compareTo(BigDecimal.ZERO) == 0)
                        .suspenseGlZero(suspenseNet.compareTo(BigDecimal.ZERO) == 0)
                        .success(success)
                        .failureReason(failureReason)
                        .build();

        log.info(result.toSummary());
        return result;
    }

    private Statistics getHibernateStatistics() {
        try {
            SessionFactory sf = entityManagerFactory.unwrap(SessionFactory.class);
            Statistics stats = sf.getStatistics();
            if (!stats.isStatisticsEnabled()) {
                log.warn(
                        "Hibernate statistics not enabled. Set "
                                + "spring.jpa.properties.hibernate.generate_statistics=true "
                                + "in application-stress.properties");
                return null;
            }
            return stats;
        } catch (Exception e) {
            log.warn("Could not access Hibernate statistics: {}", e.getMessage());
            return null;
        }
    }
}
