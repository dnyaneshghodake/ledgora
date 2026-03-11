package com.ledgora.config.seeder;

import com.ledgora.common.enums.GLAccountType;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 4 — GL Chart of Accounts hierarchy. Seeds Assets, Liabilities, Equity,
 * Revenue, Expense accounts with sub-levels.
 */
@Component
public class GLHierarchySeeder {

    private static final Logger log = LoggerFactory.getLogger(GLHierarchySeeder.class);
    private final GeneralLedgerRepository glRepository;

    public GLHierarchySeeder(GeneralLedgerRepository glRepository) {
        this.glRepository = glRepository;
    }

    public void seed() {
        if (glRepository.count() > 0) {
            log.info("  [GL] GL hierarchy already exists — skipping");
            return;
        }

        // Root (Level 0)
        GeneralLedger assets =
                gl("1000", "Assets", "Total Assets", GLAccountType.ASSET, null, 0, "DEBIT");
        GeneralLedger liabilities =
                gl(
                        "2000",
                        "Liabilities",
                        "Total Liabilities",
                        GLAccountType.LIABILITY,
                        null,
                        0,
                        "CREDIT");
        GeneralLedger equity =
                gl("3000", "Equity", "Total Equity", GLAccountType.EQUITY, null, 0, "CREDIT");
        GeneralLedger revenue =
                gl("4000", "Revenue", "Total Revenue", GLAccountType.REVENUE, null, 0, "CREDIT");
        GeneralLedger expenses =
                gl("5000", "Expenses", "Total Expenses", GLAccountType.EXPENSE, null, 0, "DEBIT");

        // Assets L1
        GeneralLedger cash =
                gl(
                        "1100",
                        "Cash and Cash Equivalents",
                        "Cash holdings",
                        GLAccountType.ASSET,
                        assets,
                        1,
                        "DEBIT");
        GeneralLedger loans =
                gl(
                        "1200",
                        "Loans and Advances",
                        "Customer loans",
                        GLAccountType.ASSET,
                        assets,
                        1,
                        "DEBIT");
        gl(
                "1300",
                "Fixed Assets",
                "Property and equipment",
                GLAccountType.ASSET,
                assets,
                1,
                "DEBIT");
        gl(
                "1400",
                "Customer Deposits Receivable",
                "Deposits receivable",
                GLAccountType.ASSET,
                assets,
                1,
                "DEBIT");

        // Assets L2
        gl("1110", "ATM Cash", "Cash held in ATM machines", GLAccountType.ASSET, cash, 2, "DEBIT");
        gl(
                "1120",
                "Vault Cash",
                "Cash held in branch vaults",
                GLAccountType.ASSET,
                cash,
                2,
                "DEBIT");
        gl("1130", "Teller Cash", "Cash held by tellers", GLAccountType.ASSET, cash, 2, "DEBIT");
        gl(
                "1210",
                "Personal Loans",
                "Unsecured personal loans",
                GLAccountType.ASSET,
                loans,
                2,
                "DEBIT");
        gl("1220", "Home Loans", "Secured mortgage loans", GLAccountType.ASSET, loans, 2, "DEBIT");

        // Liabilities L1
        GeneralLedger custDep =
                gl(
                        "2100",
                        "Customer Deposits",
                        "Savings and current deposits",
                        GLAccountType.LIABILITY,
                        liabilities,
                        1,
                        "CREDIT");
        gl(
                "2200",
                "Borrowings",
                "Bank borrowings",
                GLAccountType.LIABILITY,
                liabilities,
                1,
                "CREDIT");
        gl(
                "2300",
                "Payables",
                "Accounts payable",
                GLAccountType.LIABILITY,
                liabilities,
                1,
                "CREDIT");
        GeneralLedger otherLiab =
                gl(
                        "2400",
                        "Other Liabilities",
                        "Miscellaneous liabilities",
                        GLAccountType.LIABILITY,
                        liabilities,
                        1,
                        "CREDIT");

        // Liabilities L2
        gl(
                "2910",
                "Inter-Branch Clearing",
                "IBC clearing accounts",
                GLAccountType.LIABILITY,
                otherLiab,
                2,
                "CREDIT");
        gl(
                "2110",
                "Savings Deposits",
                "Customer savings account deposits",
                GLAccountType.LIABILITY,
                custDep,
                2,
                "CREDIT");
        gl(
                "2120",
                "Current Deposits",
                "Customer current account deposits",
                GLAccountType.LIABILITY,
                custDep,
                2,
                "CREDIT");
        gl(
                "2130",
                "Fixed Deposits",
                "Customer fixed deposit accounts",
                GLAccountType.LIABILITY,
                custDep,
                2,
                "CREDIT");

        // Equity L1
        gl(
                "3100",
                "Share Capital",
                "Issued share capital",
                GLAccountType.EQUITY,
                equity,
                1,
                "CREDIT");
        gl(
                "3200",
                "Retained Earnings",
                "Accumulated profits",
                GLAccountType.EQUITY,
                equity,
                1,
                "CREDIT");

        // Revenue L1
        gl(
                "4100",
                "Interest Income",
                "Interest from loans",
                GLAccountType.REVENUE,
                revenue,
                1,
                "CREDIT");
        gl("4200", "Fee Income", "Service fees", GLAccountType.REVENUE, revenue, 1, "CREDIT");
        gl(
                "4300",
                "Other Income",
                "Miscellaneous income",
                GLAccountType.REVENUE,
                revenue,
                1,
                "CREDIT");

        // Expense L1
        gl(
                "5100",
                "Interest Expense",
                "Interest on deposits",
                GLAccountType.EXPENSE,
                expenses,
                1,
                "DEBIT");
        gl(
                "5200",
                "Operating Expenses",
                "General operations",
                GLAccountType.EXPENSE,
                expenses,
                1,
                "DEBIT");
        gl(
                "5300",
                "Staff Expenses",
                "Salaries and benefits",
                GLAccountType.EXPENSE,
                expenses,
                1,
                "DEBIT");

        log.info(
                "  [GL] Chart of Accounts hierarchy initialized ({} GL accounts)",
                glRepository.count());
    }

    private GeneralLedger gl(
            String code,
            String name,
            String desc,
            GLAccountType type,
            GeneralLedger parent,
            int level,
            String normalBalance) {
        GeneralLedger g =
                GeneralLedger.builder()
                        .glCode(code)
                        .glName(name)
                        .description(desc)
                        .accountType(type)
                        .parent(parent)
                        .level(level)
                        .isActive(true)
                        .normalBalance(normalBalance)
                        .build();
        return glRepository.save(g);
    }
}
