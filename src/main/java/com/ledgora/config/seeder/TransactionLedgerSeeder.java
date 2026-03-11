package com.ledgora.config.seeder;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 7 — Sample Transactions + Ledger Journals + Entries. Ensures SUM(debits) =
 * SUM(credits) for every journal (double-entry).
 */
@Component
public class TransactionLedgerSeeder {

    private static final Logger log = LoggerFactory.getLogger(TransactionLedgerSeeder.class);
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final GeneralLedgerRepository glRepository;
    private final LedgerJournalRepository journalRepository;
    private final LedgerEntryRepository entryRepository;

    public TransactionLedgerSeeder(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            GeneralLedgerRepository glRepository,
            LedgerJournalRepository journalRepository,
            LedgerEntryRepository entryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.glRepository = glRepository;
        this.journalRepository = journalRepository;
        this.entryRepository = entryRepository;
    }

    public void seed(Tenant tenant, User teller) {
        if (transactionRepository.count() > 0) {
            log.info("  [Transactions] Transactions already exist — skipping");
            return;
        }

        LocalDate biz = LocalDate.now();
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
            txnAndJournal(tenant, teller, biz,
                    String.format("DEP-SEED-%04d", i),
                    TransactionType.DEPOSIT, TransactionChannel.TELLER,
                    null, bulkAcct, amt,
                    "Bulk Deposit - " + bulkAcct.getAccountName(),
                    "1100", "2110", balAfter, balAfter);
        }

        log.info("  [Transactions] 30 sample transactions with balanced ledger journals created");
    }

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
                        .performedBy(teller)
                        .tenant(tenant)
                        .build();
        txn = transactionRepository.save(txn);

        Account account = dst != null ? dst : src;
        GeneralLedger debitGL = glRepository.findByGlCode(drGl).orElse(null);
        GeneralLedger creditGL = glRepository.findByGlCode(crGl).orElse(null);

        LedgerJournal journal =
                LedgerJournal.builder()
                        .transaction(txn)
                        .tenant(tenant)
                        .description(desc)
                        .businessDate(biz)
                        .build();
        journal = journalRepository.save(journal);

        entryRepository.save(
                LedgerEntry.builder()
                        .journal(journal)
                        .transaction(txn)
                        .tenant(tenant)
                        .account(account)
                        .glAccount(debitGL)
                        .glAccountCode(drGl)
                        .entryType(EntryType.DEBIT)
                        .amount(amt)
                        .balanceAfter(drBalAfter)
                        .currency("INR")
                        .businessDate(biz)
                        .postingTime(LocalDateTime.now())
                        .narration(desc + " [DEBIT]")
                        .build());

        entryRepository.save(
                LedgerEntry.builder()
                        .journal(journal)
                        .transaction(txn)
                        .tenant(tenant)
                        .account(account)
                        .glAccount(creditGL)
                        .glAccountCode(crGl)
                        .entryType(EntryType.CREDIT)
                        .amount(amt)
                        .balanceAfter(crBalAfter)
                        .currency("INR")
                        .businessDate(biz)
                        .postingTime(LocalDateTime.now())
                        .narration(desc + " [CREDIT]")
                        .build());
    }
}
