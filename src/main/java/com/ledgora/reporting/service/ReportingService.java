package com.ledgora.reporting.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.EntryType;
import com.ledgora.common.enums.GLAccountType;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.reporting.dto.*;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PART 10: Financial reporting engine.
 * All reports read directly from ledger_entries.
 */
@Service
public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);
    private final LedgerEntryRepository ledgerEntryRepository;
    private final GeneralLedgerRepository glRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public ReportingService(LedgerEntryRepository ledgerEntryRepository,
                            GeneralLedgerRepository glRepository,
                            AccountRepository accountRepository,
                            TransactionRepository transactionRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.glRepository = glRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Generate Trial Balance report from ledger entries.
     */
    public TrialBalanceReport generateTrialBalance(LocalDate reportDate) {
        List<GeneralLedger> glAccounts = glRepository.findAll();
        List<TrialBalanceReport.TrialBalanceLine> lines = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (GeneralLedger gl : glAccounts) {
            List<LedgerEntry> entries = ledgerEntryRepository.findByGlAccountCode(gl.getGlCode());
            BigDecimal debits = entries.stream()
                    .filter(e -> e.getEntryType() == EntryType.DEBIT)
                    .map(LedgerEntry::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credits = entries.stream()
                    .filter(e -> e.getEntryType() == EntryType.CREDIT)
                    .map(LedgerEntry::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (debits.compareTo(BigDecimal.ZERO) != 0 || credits.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal debitBalance = BigDecimal.ZERO;
                BigDecimal creditBalance = BigDecimal.ZERO;
                if ("DEBIT".equals(gl.getNormalBalance())) {
                    debitBalance = debits.subtract(credits);
                } else {
                    creditBalance = credits.subtract(debits);
                }

                lines.add(TrialBalanceReport.TrialBalanceLine.builder()
                        .glCode(gl.getGlCode())
                        .glName(gl.getGlName())
                        .accountType(gl.getAccountType().name())
                        .debitBalance(debitBalance)
                        .creditBalance(creditBalance)
                        .build());

                totalDebits = totalDebits.add(debitBalance);
                totalCredits = totalCredits.add(creditBalance);
            }
        }

        return TrialBalanceReport.builder()
                .reportDate(reportDate)
                .lines(lines)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .balanced(totalDebits.compareTo(totalCredits) == 0)
                .build();
    }

    /**
     * Generate General Ledger report for a specific GL account.
     */
    public GeneralLedgerReport generateGeneralLedgerReport(String glCode, LocalDate startDate, LocalDate endDate) {
        GeneralLedger gl = glRepository.findByGlCode(glCode)
                .orElseThrow(() -> new RuntimeException("GL account not found: " + glCode));

        List<LedgerEntry> entries = ledgerEntryRepository.findByGlAccountCode(glCode);
        List<GeneralLedgerReport.GLReportEntry> reportEntries = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            BigDecimal debit = entry.getEntryType() == EntryType.DEBIT ? entry.getAmount() : BigDecimal.ZERO;
            BigDecimal credit = entry.getEntryType() == EntryType.CREDIT ? entry.getAmount() : BigDecimal.ZERO;

            if ("DEBIT".equals(gl.getNormalBalance())) {
                runningBalance = runningBalance.add(debit).subtract(credit);
            } else {
                runningBalance = runningBalance.add(credit).subtract(debit);
            }

            reportEntries.add(GeneralLedgerReport.GLReportEntry.builder()
                    .postingTime(entry.getPostingTime())
                    .transactionRef(entry.getTransaction() != null ? entry.getTransaction().getTransactionRef() : "N/A")
                    .narration(entry.getNarration())
                    .debitAmount(debit)
                    .creditAmount(credit)
                    .runningBalance(runningBalance)
                    .build());
        }

        return GeneralLedgerReport.builder()
                .glCode(glCode)
                .glName(gl.getGlName())
                .startDate(startDate)
                .endDate(endDate)
                .openingBalance(BigDecimal.ZERO)
                .closingBalance(runningBalance)
                .entries(reportEntries)
                .build();
    }

    /**
     * Generate Account Statement from ledger entries.
     */
    public AccountStatementReport generateAccountStatement(String accountNumber, LocalDate startDate, LocalDate endDate) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountNumber(accountNumber);
        List<AccountStatementReport.StatementLine> statementLines = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            BigDecimal debit = entry.getEntryType() == EntryType.DEBIT ? entry.getAmount() : BigDecimal.ZERO;
            BigDecimal credit = entry.getEntryType() == EntryType.CREDIT ? entry.getAmount() : BigDecimal.ZERO;

            totalDebits = totalDebits.add(debit);
            totalCredits = totalCredits.add(credit);

            statementLines.add(AccountStatementReport.StatementLine.builder()
                    .date(entry.getPostingTime())
                    .transactionRef(entry.getTransaction() != null ? entry.getTransaction().getTransactionRef() : "N/A")
                    .description(entry.getNarration())
                    .debitAmount(debit)
                    .creditAmount(credit)
                    .balance(entry.getBalanceAfter())
                    .build());
        }

        BigDecimal closingBalance = totalCredits.subtract(totalDebits);

        return AccountStatementReport.builder()
                .accountNumber(accountNumber)
                .accountName(account.getAccountName())
                .currency(account.getCurrency())
                .startDate(startDate)
                .endDate(endDate)
                .openingBalance(BigDecimal.ZERO)
                .closingBalance(closingBalance)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .entries(statementLines)
                .build();
    }

    /**
     * Generate Daily Transaction Summary from ledger entries.
     */
    public DailyTransactionSummary generateDailyTransactionSummary(LocalDate date) {
        List<Transaction> transactions = transactionRepository.findByBusinessDate(date);

        long depositCount = transactions.stream().filter(t -> t.getTransactionType() == TransactionType.DEPOSIT).count();
        long withdrawalCount = transactions.stream().filter(t -> t.getTransactionType() == TransactionType.WITHDRAWAL).count();
        long transferCount = transactions.stream().filter(t -> t.getTransactionType() == TransactionType.TRANSFER).count();

        BigDecimal totalDepositAmount = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEPOSIT)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWithdrawalAmount = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.WITHDRAWAL)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTransferAmount = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.TRANSFER)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = ledgerEntryRepository.sumDebitsByBusinessDate(date);
        BigDecimal totalCredits = ledgerEntryRepository.sumCreditsByBusinessDate(date);

        return DailyTransactionSummary.builder()
                .date(date)
                .totalTransactions(transactions.size())
                .depositCount(depositCount)
                .withdrawalCount(withdrawalCount)
                .transferCount(transferCount)
                .totalDepositAmount(totalDepositAmount)
                .totalWithdrawalAmount(totalWithdrawalAmount)
                .totalTransferAmount(totalTransferAmount)
                .netAmount(totalDepositAmount.subtract(totalWithdrawalAmount))
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .build();
    }

    /**
     * Generate Liquidity Report.
     */
    public LiquidityReport generateLiquidityReport() {
        List<GeneralLedger> allGL = glRepository.findAll();
        List<LiquidityReport.LiquidityLine> details = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalCustomerDeposits = BigDecimal.ZERO;
        BigDecimal totalCashHoldings = BigDecimal.ZERO;

        for (GeneralLedger gl : allGL) {
            List<LedgerEntry> entries = ledgerEntryRepository.findByGlAccountCode(gl.getGlCode());
            BigDecimal debits = entries.stream()
                    .filter(e -> e.getEntryType() == EntryType.DEBIT)
                    .map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credits = entries.stream()
                    .filter(e -> e.getEntryType() == EntryType.CREDIT)
                    .map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal balance;
            if ("DEBIT".equals(gl.getNormalBalance())) {
                balance = debits.subtract(credits);
            } else {
                balance = credits.subtract(debits);
            }

            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                details.add(LiquidityReport.LiquidityLine.builder()
                        .glCode(gl.getGlCode())
                        .glName(gl.getGlName())
                        .accountType(gl.getAccountType().name())
                        .balance(balance)
                        .build());

                if (gl.getAccountType() == GLAccountType.ASSET) {
                    totalAssets = totalAssets.add(balance);
                    if ("1100".equals(gl.getGlCode())) {
                        totalCashHoldings = totalCashHoldings.add(balance);
                    }
                } else if (gl.getAccountType() == GLAccountType.LIABILITY) {
                    totalLiabilities = totalLiabilities.add(balance);
                    if ("2100".equals(gl.getGlCode())) {
                        totalCustomerDeposits = totalCustomerDeposits.add(balance);
                    }
                }
            }
        }

        BigDecimal liquidityRatio = totalLiabilities.compareTo(BigDecimal.ZERO) != 0
                ? totalAssets.divide(totalLiabilities, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return LiquidityReport.builder()
                .reportDate(LocalDate.now())
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .netLiquidity(totalAssets.subtract(totalLiabilities))
                .totalCustomerDeposits(totalCustomerDeposits)
                .totalCashHoldings(totalCashHoldings)
                .liquidityRatio(liquidityRatio)
                .details(details)
                .build();
    }
}
