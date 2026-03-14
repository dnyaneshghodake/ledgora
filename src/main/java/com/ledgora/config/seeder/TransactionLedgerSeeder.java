package com.ledgora.config.seeder;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.repository.TransactionBatchRepository;
import com.ledgora.common.enums.*;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.entity.LedgerJournal;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.ledger.repository.LedgerJournalRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.repository.TransactionRepository;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 7 — Production-grade sample Transactions with full chain: Transaction →
 * Batch → Voucher(DR+CR) → LedgerJournal → LedgerEntry. All flags set to authorized + posted.
 * SUM(debits) = SUM(credits) for every journal (double-entry).
 */
@Component
public class TransactionLedgerSeeder {

    private static final Logger log = LoggerFactory.getLogger(TransactionLedgerSeeder.class);
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final GeneralLedgerRepository glRepository;
    private final LedgerJournalRepository journalRepository;
    private final LedgerEntryRepository entryRepository;
    private final TransactionBatchRepository batchRepository;
    private final VoucherRepository voucherRepository;

    public TransactionLedgerSeeder(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            AccountBalanceRepository accountBalanceRepository,
            GeneralLedgerRepository glRepository,
            LedgerJournalRepository journalRepository,
            LedgerEntryRepository entryRepository,
            TransactionBatchRepository batchRepository,
            VoucherRepository voucherRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.glRepository = glRepository;
        this.journalRepository = journalRepository;
        this.entryRepository = entryRepository;
        this.batchRepository = batchRepository;
        this.voucherRepository = voucherRepository;
    }

    public void seed(Tenant tenant, User teller) {
        if (transactionRepository.count() > 0) {
            log.info("  [Transactions] Transactions already exist — skipping");
            return;
        }

        LocalDate biz = tenant.getCurrentBusinessDate();
        Account rSav = accountRepository.findByAccountNumber("SAV-1001-0001").orElse(null);
        Account pSav = accountRepository.findByAccountNumber("SAV-1002-0001").orElse(null);
        Account rCur = accountRepository.findByAccountNumber("CUR-1001-0001").orElse(null);
        if (rSav == null || pSav == null || rCur == null) {
            log.warn("  [Transactions] Customer accounts not found — skipping");
            return;
        }

        txnAndJournal(
                tenant,
                teller,
                biz,
                "DEP-SEED-0001",
                TransactionType.DEPOSIT,
                TransactionChannel.TELLER,
                null,
                rSav,
                new BigDecimal("10000.0000"),
                "Opening Deposit - Rajesh Savings",
                "1100",
                "2110",
                new BigDecimal("60000.0000"),
                new BigDecimal("60000.0000"));

        txnAndJournal(
                tenant,
                teller,
                biz,
                "DEP-SEED-0002",
                TransactionType.DEPOSIT,
                TransactionChannel.TELLER,
                null,
                pSav,
                new BigDecimal("15000.0000"),
                "Opening Deposit - Priya Savings",
                "1100",
                "2110",
                new BigDecimal("90000.0000"),
                new BigDecimal("90000.0000"));

        txnAndJournal(
                tenant,
                teller,
                biz,
                "TRF-SEED-0001",
                TransactionType.TRANSFER,
                TransactionChannel.ONLINE,
                rSav,
                pSav,
                new BigDecimal("5000.0000"),
                "Internal Transfer - Rajesh to Priya",
                "2100",
                "2100",
                new BigDecimal("55000.0000"),
                new BigDecimal("95000.0000"));

        txnAndJournal(
                tenant,
                teller,
                biz,
                "WDR-SEED-0001",
                TransactionType.WITHDRAWAL,
                TransactionChannel.ATM,
                rCur,
                null,
                new BigDecimal("2000.0000"),
                "ATM Withdrawal - Rajesh Current",
                "2100",
                "1100",
                new BigDecimal("148000.0000"),
                new BigDecimal("148000.0000"));

        // ── Bulk transactions for pagination testing (26 more deposits across bulk accounts) ──
        for (int i = 5; i <= 30; i++) {
            String savNum = String.format("SAV-%04d-0001", i);
            Account bulkAcct = accountRepository.findByAccountNumber(savNum).orElse(null);
            if (bulkAcct == null) continue;
            BigDecimal amt = new BigDecimal((i * 1000) + ".0000");
            BigDecimal balAfter = bulkAcct.getBalance().add(amt);
            txnAndJournal(
                    tenant,
                    teller,
                    biz,
                    String.format("DEP-SEED-%04d", i),
                    TransactionType.DEPOSIT,
                    TransactionChannel.TELLER,
                    null,
                    bulkAcct,
                    amt,
                    "Bulk Deposit - " + bulkAcct.getAccountName(),
                    "1100",
                    "2110",
                    balAfter,
                    balAfter);
        }

        // ── Sync Account.balance and AccountBalance to reflect seeded transactions ──
        // The seeder creates ledger entries with correct balanceAfter values, but the
        // Account.balance and AccountBalance records were set by CustomerAccountSeeder
        // BEFORE these transactions. We must sync them to the post-transaction state.
        syncBalanceAfterSeed(rSav, new BigDecimal("10000.0000").subtract(new BigDecimal("5000.0000")));
        // rSav: +10000 (DEP-SEED-0001) -5000 (TRF-SEED-0001) = net +5000
        syncBalanceAfterSeed(pSav, new BigDecimal("15000.0000").add(new BigDecimal("5000.0000")));
        // pSav: +15000 (DEP-SEED-0002) +5000 (TRF-SEED-0001) = net +20000
        syncBalanceAfterSeed(rCur, new BigDecimal("2000.0000").negate());
        // rCur: -2000 (WDR-SEED-0001) = net -2000

        // Bulk accounts: each got a deposit of (i * 1000)
        for (int i = 5; i <= 30; i++) {
            String savNum = String.format("SAV-%04d-0001", i);
            Account bulkAcct = accountRepository.findByAccountNumber(savNum).orElse(null);
            if (bulkAcct != null) {
                syncBalanceAfterSeed(bulkAcct, new BigDecimal((i * 1000) + ".0000"));
            }
        }

        // ── Sync GL account balances to reflect seeded transactions ──
        // Cash GL 1100: DR from deposits (+10000+15000+bulk) minus CR from withdrawal (-2000)
        BigDecimal bulkDepositTotal = BigDecimal.ZERO;
        for (int i = 5; i <= 30; i++) {
            bulkDepositTotal = bulkDepositTotal.add(new BigDecimal(i * 1000));
        }
        BigDecimal cashGlDelta =
                new BigDecimal("10000")
                        .add(new BigDecimal("15000"))
                        .add(bulkDepositTotal)
                        .subtract(new BigDecimal("2000"));
        Account glCash =
                accountRepository.findByAccountNumber("GL-CASH-001").orElse(null);
        if (glCash != null) {
            syncBalanceAfterSeed(glCash, cashGlDelta);
        }

        // Savings Deposits GL 2110: CR from all deposits
        // (GL account entity tracks net balance; for liability GL, credits increase it)
        // Note: GL-DEP-001 maps to GL code 2100, not 2110. The savings deposits sub-GL
        // doesn't have a separate Account entity — only the parent 2100 does.
        // Customer Deposits GL 2100: net DR from withdrawal = -2000
        Account glDep =
                accountRepository.findByAccountNumber("GL-DEP-001").orElse(null);
        if (glDep != null) {
            syncBalanceAfterSeed(glDep, new BigDecimal("-2000"));
        }

        log.info("  [Transactions] 30 sample transactions with balanced ledger journals created");
        log.info("  [Balances] Account + GL balances synced to post-transaction state");
    }

    /**
     * Sync Account.balance and AccountBalance after seeded transactions. Adds the net transaction
     * delta to the existing balance (which was set by CustomerAccountSeeder).
     */
    private void syncBalanceAfterSeed(Account account, BigDecimal netDelta) {
        BigDecimal newBalance = account.getBalance().add(netDelta);
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Also sync AccountBalance to match
        accountBalanceRepository
                .findByAccountId(account.getId())
                .ifPresent(
                        ab -> {
                            ab.setLedgerBalance(newBalance);
                            ab.setActualTotalBalance(newBalance);
                            ab.setActualClearedBalance(newBalance);
                            ab.setAvailableBalance(newBalance);
                            accountBalanceRepository.save(ab);
                        });
    }

    /** Scroll counter for voucher numbering within this seeder run. */
    private long scrollCounter = 0;

    /**
     * Create a complete production-grade transaction chain: Transaction → Batch → Voucher(DR+CR) →
     * LedgerJournal → LedgerEntry(DR+CR). All flags set to authorized + posted state.
     */
    private void txnAndJournal(
            Tenant tenant,
            User teller,
            LocalDate biz,
            String ref,
            TransactionType type,
            TransactionChannel channel,
            Account src,
            Account dst,
            BigDecimal amt,
            String desc,
            String drGl,
            String crGl,
            BigDecimal drBalAfter,
            BigDecimal crBalAfter) {
        LocalDateTime bizTimestamp = biz.atStartOfDay();

        // ── 1. Create or reuse batch for this channel + date ──
        BatchType batchType =
                channel == TransactionChannel.TELLER
                        ? BatchType.TELLER
                        : channel == TransactionChannel.ATM
                                ? BatchType.ATM
                                : BatchType.ONLINE;
        String batchCode = "SEED-" + batchType.name() + "-" + biz.toString().replace("-", "");
        TransactionBatch batch =
                batchRepository
                        .findByBatchCode(batchCode)
                        .orElseGet(
                                () ->
                                        batchRepository.save(
                                                TransactionBatch.builder()
                                                        .tenant(tenant)
                                                        .batchType(batchType)
                                                        .batchCode(batchCode)
                                                        .businessDate(biz)
                                                        .status(BatchStatus.SETTLED)
                                                        .totalDebit(BigDecimal.ZERO)
                                                        .totalCredit(BigDecimal.ZERO)
                                                        .transactionCount(0)
                                                        .build()));
        // Update batch totals
        batch.setTotalDebit(batch.getTotalDebit().add(amt));
        batch.setTotalCredit(batch.getTotalCredit().add(amt));
        batch.setTransactionCount(batch.getTransactionCount() + 1);
        batchRepository.save(batch);

        // ── 2. Create transaction with batch + valueDate ──
        Transaction txn =
                Transaction.builder()
                        .transactionRef(ref)
                        .transactionType(type)
                        .status(TransactionStatus.COMPLETED)
                        .amount(amt)
                        .currency("INR")
                        .channel(channel)
                        .sourceAccount(src)
                        .destinationAccount(dst)
                        .description(desc)
                        .narration(desc)
                        .businessDate(biz)
                        .valueDate(biz)
                        .performedBy(teller)
                        .maker(teller)
                        .makerTimestamp(bizTimestamp)
                        .tenant(tenant)
                        .batch(batch)
                        .build();
        txn = transactionRepository.save(txn);

        // ── 3. Resolve accounts and GL ──
        Account drAccount = src != null ? src : dst;
        Account crAccount = dst != null ? dst : src;
        GeneralLedger debitGL = glRepository.findByGlCode(drGl).orElse(null);
        GeneralLedger creditGL = glRepository.findByGlCode(crGl).orElse(null);

        // ── 4. Create journal ──
        LedgerJournal journal =
                LedgerJournal.builder()
                        .transaction(txn)
                        .tenant(tenant)
                        .description(desc)
                        .businessDate(biz)
                        .build();
        journal = journalRepository.save(journal);

        // ── 5. Create ledger entries ──
        LedgerEntry drEntry =
                entryRepository.save(
                        LedgerEntry.builder()
                                .journal(journal)
                                .transaction(txn)
                                .tenant(tenant)
                                .account(drAccount)
                                .glAccount(debitGL)
                                .glAccountCode(drGl)
                                .entryType(EntryType.DEBIT)
                                .amount(amt)
                                .balanceAfter(drBalAfter)
                                .currency("INR")
                                .businessDate(biz)
                                .postingTime(bizTimestamp)
                                .narration(desc + " [DEBIT]")
                                .build());

        LedgerEntry crEntry =
                entryRepository.save(
                        LedgerEntry.builder()
                                .journal(journal)
                                .transaction(txn)
                                .tenant(tenant)
                                .account(crAccount)
                                .glAccount(creditGL)
                                .glAccountCode(crGl)
                                .entryType(EntryType.CREDIT)
                                .amount(amt)
                                .balanceAfter(crBalAfter)
                                .currency("INR")
                                .businessDate(biz)
                                .postingTime(bizTimestamp)
                                .narration(desc + " [CREDIT]")
                                .build());

        // ── 6. Create vouchers (DR + CR) — authorized + posted ──
        String tenantCode = tenant.getTenantCode();
        String branchCode =
                drAccount.getBranch() != null ? drAccount.getBranch().getBranchCode() : "HQ001";
        String dateStr = biz.toString().replace("-", "");

        scrollCounter++;
        Voucher drVoucher =
                Voucher.builder()
                        .voucherNumber(
                                tenantCode + "-" + branchCode + "-" + dateStr + "-"
                                        + String.format("%06d", scrollCounter))
                        .tenant(tenant)
                        .branch(drAccount.getBranch() != null ? drAccount.getBranch() : dst.getBranch())
                        .transaction(txn)
                        .batchCode(batchCode)
                        .setNo(1)
                        .scrollNo(scrollCounter)
                        .entryDate(biz)
                        .postingDate(biz)
                        .valueDate(biz)
                        .effectiveDate(biz)
                        .drCr(VoucherDrCr.DR)
                        .account(drAccount)
                        .glAccount(debitGL)
                        .transactionAmount(amt)
                        .localCurrencyAmount(amt)
                        .currency("INR")
                        .maker(teller)
                        .authFlag("Y")
                        .postFlag("Y")
                        .cancelFlag("N")
                        .financialEffectFlag("Y")
                        .narration(desc + " [DEBIT]")
                        .ledgerEntry(drEntry)
                        .build();
        voucherRepository.save(drVoucher);

        scrollCounter++;
        Voucher crVoucher =
                Voucher.builder()
                        .voucherNumber(
                                tenantCode + "-" + branchCode + "-" + dateStr + "-"
                                        + String.format("%06d", scrollCounter))
                        .tenant(tenant)
                        .branch(crAccount.getBranch() != null ? crAccount.getBranch() : src.getBranch())
                        .transaction(txn)
                        .batchCode(batchCode)
                        .setNo(1)
                        .scrollNo(scrollCounter)
                        .entryDate(biz)
                        .postingDate(biz)
                        .valueDate(biz)
                        .effectiveDate(biz)
                        .drCr(VoucherDrCr.CR)
                        .account(crAccount)
                        .glAccount(creditGL)
                        .transactionAmount(amt)
                        .localCurrencyAmount(amt)
                        .currency("INR")
                        .maker(teller)
                        .authFlag("Y")
                        .postFlag("Y")
                        .cancelFlag("N")
                        .financialEffectFlag("Y")
                        .narration(desc + " [CREDIT]")
                        .ledgerEntry(crEntry)
                        .build();
        voucherRepository.save(crVoucher);
    }
}
