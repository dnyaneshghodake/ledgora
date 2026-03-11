package com.ledgora.config;

import com.ledgora.auth.entity.User;
import com.ledgora.branch.entity.Branch;
import com.ledgora.config.seeder.*;
import com.ledgora.tenant.entity.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Slim orchestrator for CBS data seeding. Delegates to module-wise seeders
 * in dependency order. Each seeder is idempotent (checks before creating).
 *
 * <p>Execution order:
 * 0. Tenants → 1. Roles → 2. Branches → 3. Users → 4. GL Hierarchy →
 * 5. Business Date → 6. Customers & Accounts → 7. Transactions & Ledger →
 * 8. Exchange Rates → 9. Idempotency Keys → 10. CBS CustomerMaster + Tax
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TenantDataSeeder tenantSeeder;
    private final RoleDataSeeder roleSeeder;
    private final BranchDataSeeder branchSeeder;
    private final UserDataSeeder userSeeder;
    private final GLHierarchySeeder glSeeder;
    private final BusinessDateSeeder businessDateSeeder;
    private final CustomerAccountSeeder customerAccountSeeder;
    private final TransactionLedgerSeeder transactionLedgerSeeder;
    private final ExchangeRateSeeder exchangeRateSeeder;
    private final IdempotencyKeySeeder idempotencyKeySeeder;
    private final CbsCustomerSeeder cbsCustomerSeeder;

    public DataInitializer(
            TenantDataSeeder tenantSeeder,
            RoleDataSeeder roleSeeder,
            BranchDataSeeder branchSeeder,
            UserDataSeeder userSeeder,
            GLHierarchySeeder glSeeder,
            BusinessDateSeeder businessDateSeeder,
            CustomerAccountSeeder customerAccountSeeder,
            TransactionLedgerSeeder transactionLedgerSeeder,
            ExchangeRateSeeder exchangeRateSeeder,
            IdempotencyKeySeeder idempotencyKeySeeder,
            CbsCustomerSeeder cbsCustomerSeeder) {
        this.tenantSeeder = tenantSeeder;
        this.roleSeeder = roleSeeder;
        this.branchSeeder = branchSeeder;
        this.userSeeder = userSeeder;
        this.glSeeder = glSeeder;
        this.businessDateSeeder = businessDateSeeder;
        this.customerAccountSeeder = customerAccountSeeder;
        this.transactionLedgerSeeder = transactionLedgerSeeder;
        this.exchangeRateSeeder = exchangeRateSeeder;
        this.idempotencyKeySeeder = idempotencyKeySeeder;
        this.cbsCustomerSeeder = cbsCustomerSeeder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Ledgora DataInitializer — seeding reference data ...");
        log.info("═══════════════════════════════════════════════════════════");

        // 0. Tenants
        Tenant defaultTenant = tenantSeeder.seedDefaultTenant();
        Tenant secondTenant = tenantSeeder.seedSecondTenant();
        tenantSeeder.seed();

        // 1. Roles
        roleSeeder.seed();

        // 2. Branches
        Branch[] branches = branchSeeder.seed(defaultTenant);
        Branch hq = branches[0];
        Branch br1 = branches[1];
        Branch br2 = branches[2];

        // 3. Users
        User[] users = userSeeder.seed(defaultTenant, secondTenant, hq, br1, br2);
        User adminUser = users[0];
        User teller1User = users[2];

        // 4. GL Chart of Accounts
        glSeeder.seed();

        // 5. Business Date
        businessDateSeeder.seed();

        // 6. Customers & Accounts & Balances
        customerAccountSeeder.seed(defaultTenant, adminUser, hq, br1, br2);

        // 7. Sample Transactions & Ledger
        transactionLedgerSeeder.seed(defaultTenant, teller1User);

        // 8. Exchange Rates
        exchangeRateSeeder.seed();

        // 9. Idempotency Keys
        idempotencyKeySeeder.seed();

        // 10. CBS CustomerMaster + Tax Profiles
        cbsCustomerSeeder.seed(defaultTenant);

        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Ledgora DataInitializer — seeding complete.");
        log.info("═══════════════════════════════════════════════════════════");
    }
}

















        GeneralLedger liabilities =
                createGL(
                        "2000",
                        "Liabilities",
                        "Total Liabilities",
                        GLAccountType.LIABILITY,
                        null,
                        0,
                        "CREDIT");
        GeneralLedger equity =
                createGL("3000", "Equity", "Total Equity", GLAccountType.EQUITY, null, 0, "CREDIT");
        GeneralLedger revenue =
                createGL(
                        "4000",
                        "Revenue",
                        "Total Revenue",
                        GLAccountType.REVENUE,
                        null,
                        0,
                        "CREDIT");
        GeneralLedger expenses =
                createGL(
                        "5000",
                        "Expenses",
                        "Total Expenses",
                        GLAccountType.EXPENSE,
                        null,
                        0,
                        "DEBIT");

        // ── Assets -> Level 1 sub-accounts ──
        GeneralLedger cash =
                createGL(
                        "1100",
                        "Cash and Cash Equivalents",
                        "Cash holdings",
                        GLAccountType.ASSET,
                        assets,
                        1,
                        "DEBIT");
        GeneralLedger loans =
                createGL(
                        "1200",
                        "Loans and Advances",
                        "Customer loans",
                        GLAccountType.ASSET,
                        assets,
                        1,
                        "DEBIT");
        createGL(
                "1300",
                "Fixed Assets",
                "Property and equipment",
                GLAccountType.ASSET,
                assets,
                1,
                "DEBIT");
        createGL(
                "1400",
                "Customer Deposits Receivable",
                "Deposits receivable",
                GLAccountType.ASSET,
                assets,
                1,
                "DEBIT");

        // ── Assets -> Level 2 (Cash sub-accounts) ──
        createGL(
                "1110",
                "ATM Cash",
                "Cash held in ATM machines",
                GLAccountType.ASSET,
                cash,
                2,
                "DEBIT");
        createGL(
                "1120",
                "Vault Cash",
                "Cash held in branch vaults",
                GLAccountType.ASSET,
                cash,
                2,
                "DEBIT");
        createGL(
                "1130",
                "Teller Cash",
                "Cash held by tellers",
                GLAccountType.ASSET,
                cash,
                2,
                "DEBIT");

        // ── Assets -> Level 2 (Loan sub-accounts) ──
        createGL(
                "1210",
                "Personal Loans",
                "Unsecured personal loans",
                GLAccountType.ASSET,
                loans,
                2,
                "DEBIT");
        createGL(
                "1220",
                "Home Loans",
                "Secured mortgage loans",
                GLAccountType.ASSET,
                loans,
                2,
                "DEBIT");

        // ── Liabilities -> Level 1 sub-accounts ──
        GeneralLedger customerDeposits =
                createGL(
                        "2100",
                        "Customer Deposits",
                        "Savings and current deposits",
                        GLAccountType.LIABILITY,
                        liabilities,
                        1,
                        "CREDIT");
        createGL(
                "2200",
                "Borrowings",
                "Bank borrowings",
                GLAccountType.LIABILITY,
                liabilities,
                1,
                "CREDIT");
        createGL(
                "2300",
                "Payables",
                "Accounts payable",
                GLAccountType.LIABILITY,
                liabilities,
                1,
                "CREDIT");
        GeneralLedger otherLiabilities =
                createGL(
                        "2400",
                        "Other Liabilities",
                        "Miscellaneous liabilities",
                        GLAccountType.LIABILITY,
                        liabilities,
                        1,
                        "CREDIT");

        // ── Liabilities -> Level 2 (Inter-Branch Clearing GL — RBI requirement) ──
        createGL(
                "2910",
                "Inter-Branch Clearing",
                "IBC clearing accounts for branch-independent books",
                GLAccountType.LIABILITY,
                otherLiabilities,
                2,
                "CREDIT");

        // ── Liabilities -> Level 2 (Customer Deposits sub-accounts) ──
        createGL(
                "2110",
                "Savings Deposits",
                "Customer savings account deposits",
                GLAccountType.LIABILITY,
                customerDeposits,
                2,
                "CREDIT");
        createGL(
                "2120",
                "Current Deposits",
                "Customer current account deposits",
                GLAccountType.LIABILITY,
                customerDeposits,
                2,
                "CREDIT");
        createGL(
                "2130",
                "Fixed Deposits",
                "Customer fixed deposit accounts",
                GLAccountType.LIABILITY,
                customerDeposits,
                2,
                "CREDIT");

        // ── Equity sub-accounts ──
        createGL(
                "3100",
                "Share Capital",
                "Issued share capital",
                GLAccountType.EQUITY,
                equity,
                1,
                "CREDIT");
        createGL(
                "3200",
                "Retained Earnings",
                "Accumulated profits",
                GLAccountType.EQUITY,
                equity,
                1,
                "CREDIT");

        // ── Revenue sub-accounts ──
        createGL(
                "4100",
                "Interest Income",
                "Interest from loans",
                GLAccountType.REVENUE,
                revenue,
                1,
                "CREDIT");
        createGL("4200", "Fee Income", "Service fees", GLAccountType.REVENUE, revenue, 1, "CREDIT");
        createGL(
                "4300",
                "Other Income",
                "Miscellaneous income",
                GLAccountType.REVENUE,
                revenue,
                1,
                "CREDIT");

        // ── Expense sub-accounts ──
        createGL(
                "5100",
                "Interest Expense",
                "Interest on deposits",
                GLAccountType.EXPENSE,
                expenses,
                1,
                "DEBIT");
        createGL(
                "5200",
                "Operating Expenses",
                "General operations",
                GLAccountType.EXPENSE,
                expenses,
                1,
                "DEBIT");
        createGL(
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

    private GeneralLedger createGL(
            String code,
            String name,
            String desc,
            GLAccountType type,
            GeneralLedger parent,
            int level,
            String normalBalance) {
        GeneralLedger gl =
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
        return glRepository.save(gl);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. SEED SYSTEM DATE — current business date, status OPEN
    // ════════════════════════════════════════════════════════════════════════
    private void initBusinessDate() {
        if (systemDateRepository.count() == 0) {
            SystemDate sd =
                    SystemDate.builder()
                            .businessDate(LocalDate.now())
                            .status(BusinessDateStatus.OPEN)
                            .build();
            systemDateRepository.save(sd);
            log.info("  [SystemDate] Initialized business date: {} (OPEN)", sd.getBusinessDate());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. SEED CUSTOMERS & ACCOUNTS
    //    - 4 customers (2 VERIFIED, 2 PENDING KYC)
    //    - Each verified customer gets SAVINGS + CURRENT accounts
    //    - GL, INTERNAL, CLEARING, SETTLEMENT system accounts
    //    - Parent-child account relationships for Chart of Accounts
    //    - AccountBalance cache records with starting balances
    // ════════════════════════════════════════════════════════════════════════
    private void initCustomersAndAccounts() {
        if (accountRepository.count() > 0) {
            log.info("  [Accounts] Accounts already exist — skipping");
            return;
        }

        // ── Create Customers ──
        Customer cust1 =
                createCustomerIfMissing(
                        "CUST-NID-001",
                        "Rajesh",
                        "Kumar",
                        LocalDate.of(1985, 3, 15),
                        "rajesh.kumar@email.com",
                        "+91-9100000001",
                        "123 MG Road, Mumbai",
                        "VERIFIED");

        Customer cust2 =
                createCustomerIfMissing(
                        "CUST-NID-002",
                        "Priya",
                        "Sharma",
                        LocalDate.of(1990, 7, 22),
                        "priya.sharma@email.com",
                        "+91-9100000002",
                        "456 Park Avenue, Delhi",
                        "VERIFIED");

        Customer cust3 =
                createCustomerIfMissing(
                        "CUST-NID-003",
                        "Amit",
                        "Patel",
                        LocalDate.of(1988, 11, 5),
                        "amit.patel@email.com",
                        "+91-9100000003",
                        "789 Lake Road, Bangalore",
                        "PENDING");

        Customer cust4 =
                createCustomerIfMissing(
                        "CUST-NID-004",
                        "Sneha",
                        "Reddy",
                        LocalDate.of(1995, 1, 30),
                        "sneha.reddy@email.com",
                        "+91-9100000004",
                        "321 Hill Street, Hyderabad",
                        "PENDING");

        log.info("  [Customers] 4 customers ready (2 VERIFIED, 2 PENDING)");

        // ── GL / System Accounts (no customer link) ──
        // These represent the bank's own ledger positions
        Account glCashAccount =
                createAccount(
                        "GL-CASH-001",
                        "Cash GL Account",
                        AccountType.GL_ACCOUNT,
                        LedgerAccountType.GL_ACCOUNT,
                        new BigDecimal("5000000.0000"),
                        "INR",
                        hqBranch,
                        null,
                        null,
                        "1100",
                        null);

        Account glLoansAccount =
                createAccount(
                        "GL-LOAN-001",
                        "Loans GL Account",
                        AccountType.GL_ACCOUNT,
                        LedgerAccountType.GL_ACCOUNT,
                        new BigDecimal("2000000.0000"),
                        "INR",
                        hqBranch,
                        null,
                        null,
                        "1200",
                        null);

        Account glDepositsAccount =
                createAccount(
                        "GL-DEP-001",
                        "Customer Deposits GL Account",
                        AccountType.GL_ACCOUNT,
                        LedgerAccountType.GL_ACCOUNT,
                        new BigDecimal("3000000.0000"),
                        "INR",
                        hqBranch,
                        null,
                        null,
                        "2100",
                        null);

        createAccount(
                "INT-SUSP-001",
                "Internal Suspense Account",
                AccountType.INTERNAL_ACCOUNT,
                LedgerAccountType.INTERNAL_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                hqBranch,
                null,
                null,
                null,
                null);

        createAccount(
                "CLR-001",
                "Main Clearing Account",
                AccountType.CLEARING_ACCOUNT,
                LedgerAccountType.CLEARING_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                hqBranch,
                null,
                null,
                null,
                null);

        createAccount(
                "SET-001",
                "Settlement Account",
                AccountType.SETTLEMENT_ACCOUNT,
                LedgerAccountType.SETTLEMENT_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                hqBranch,
                null,
                null,
                null,
                null);

        // ── Inter-Branch Clearing (IBC) accounts — per branch, GL 2910 ──
        // RBI: Each branch must independently balance. IBC accounts enable this.
        // IBC-OUT-<branch>: credited when funds leave the branch
        // IBC-IN-<branch>: debited when funds arrive at the branch
        // At settlement: SUM(IBC-OUT) == SUM(IBC-IN) must net to zero.
        createAccount(
                "IBC-OUT-HQ001",
                "IBC Outward - Head Office",
                AccountType.CLEARING_ACCOUNT,
                LedgerAccountType.CLEARING_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                hqBranch,
                null,
                null,
                "2910",
                null);
        createAccount(
                "IBC-IN-HQ001",
                "IBC Inward - Head Office",
                AccountType.CLEARING_ACCOUNT,
                LedgerAccountType.CLEARING_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                hqBranch,
                null,
                null,
                "2910",
                null);

        createAccount(
                "IBC-OUT-BR001",
                "IBC Outward - Downtown Branch",
                AccountType.CLEARING_ACCOUNT,
                LedgerAccountType.CLEARING_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                branch1,
                null,
                null,
                "2910",
                null);
        createAccount(
                "IBC-IN-BR001",
                "IBC Inward - Downtown Branch",
                AccountType.CLEARING_ACCOUNT,
                LedgerAccountType.CLEARING_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                branch1,
                null,
                null,
                "2910",
                null);

        createAccount(
                "IBC-OUT-BR002",
                "IBC Outward - Uptown Branch",
                AccountType.CLEARING_ACCOUNT,
                LedgerAccountType.CLEARING_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                branch2,
                null,
                null,
                "2910",
                null);
        createAccount(
                "IBC-IN-BR002",
                "IBC Inward - Uptown Branch",
                AccountType.CLEARING_ACCOUNT,
                LedgerAccountType.CLEARING_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                branch2,
                null,
                null,
                "2910",
                null);

        log.info("  [IBC] 6 Inter-Branch Clearing accounts seeded (IBC-OUT + IBC-IN per branch)");

        // ── Customer 1: Rajesh Kumar — SAVINGS + CURRENT + LOAN + FD ──
        Account rajeshSavings =
                createAccount(
                        "SAV-1001-0001",
                        "Rajesh Kumar - Savings",
                        AccountType.SAVINGS,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("50000.0000"),
                        "INR",
                        branch1,
                        cust1,
                        "Rajesh Kumar",
                        "2110",
                        null);

        Account rajeshCurrent =
                createAccount(
                        "CUR-1001-0001",
                        "Rajesh Kumar - Current",
                        AccountType.CURRENT,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("150000.0000"),
                        "INR",
                        branch1,
                        cust1,
                        "Rajesh Kumar",
                        "2120",
                        null);

        Account rajeshLoan =
                createAccount(
                        "LN-1001-0001",
                        "Rajesh Kumar - Personal Loan",
                        AccountType.LOAN,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("500000.0000"),
                        "INR",
                        branch1,
                        cust1,
                        "Rajesh Kumar",
                        "1210",
                        null);

        Account rajeshFD =
                createAccount(
                        "FD-1001-0001",
                        "Rajesh Kumar - Fixed Deposit",
                        AccountType.FIXED_DEPOSIT,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("200000.0000"),
                        "INR",
                        branch1,
                        cust1,
                        "Rajesh Kumar",
                        "2130",
                        null);

        // ── Customer 2: Priya Sharma — SAVINGS + CURRENT + FD ──
        Account priyaSavings =
                createAccount(
                        "SAV-1002-0001",
                        "Priya Sharma - Savings",
                        AccountType.SAVINGS,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("75000.0000"),
                        "INR",
                        branch1,
                        cust2,
                        "Priya Sharma",
                        "2110",
                        null);

        Account priyaCurrent =
                createAccount(
                        "CUR-1002-0001",
                        "Priya Sharma - Current",
                        AccountType.CURRENT,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("250000.0000"),
                        "INR",
                        branch1,
                        cust2,
                        "Priya Sharma",
                        "2120",
                        null);

        Account priyaFD =
                createAccount(
                        "FD-1002-0001",
                        "Priya Sharma - Fixed Deposit",
                        AccountType.FIXED_DEPOSIT,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("500000.0000"),
                        "INR",
                        branch1,
                        cust2,
                        "Priya Sharma",
                        "2130",
                        null);

        // ── Customer 3: Amit Patel — SAVINGS (pending KYC, limited) ──
        Account amitSavings =
                createAccount(
                        "SAV-1003-0001",
                        "Amit Patel - Savings",
                        AccountType.SAVINGS,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("10000.0000"),
                        "INR",
                        branch2,
                        cust3,
                        "Amit Patel",
                        "2110",
                        null);

        // ── Customer 4: Sneha Reddy — USD CURRENT (pending KYC, multi-currency) ──
        Account snehaCurrentUSD =
                createAccount(
                        "CUR-1004-0001",
                        "Sneha Reddy - USD Current",
                        AccountType.CURRENT,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("5000.0000"),
                        "USD",
                        branch2,
                        cust4,
                        "Sneha Reddy",
                        "2120",
                        null);

        log.info(
                "  [Accounts] {} accounts created (GL, Internal, Clearing, Settlement, Customer)",
                accountRepository.count());

        // ── Create AccountBalance cache entries for all accounts with balances ──
        createAccountBalance(rajeshSavings, new BigDecimal("50000.0000"));
        createAccountBalance(rajeshCurrent, new BigDecimal("150000.0000"));
        createAccountBalance(rajeshLoan, new BigDecimal("500000.0000"));
        createAccountBalance(rajeshFD, new BigDecimal("200000.0000"));
        createAccountBalance(priyaSavings, new BigDecimal("75000.0000"));
        createAccountBalance(priyaCurrent, new BigDecimal("250000.0000"));
        createAccountBalance(priyaFD, new BigDecimal("500000.0000"));
        createAccountBalance(amitSavings, new BigDecimal("10000.0000"));
        createAccountBalance(snehaCurrentUSD, new BigDecimal("5000.0000"));
        createAccountBalance(glCashAccount, new BigDecimal("5000000.0000"));
        createAccountBalance(glLoansAccount, new BigDecimal("2000000.0000"));
        createAccountBalance(glDepositsAccount, new BigDecimal("3000000.0000"));

        log.info(
                "  [AccountBalance] {} balance cache records created",
                accountBalanceRepository.count());
    }

    /** Helper — creates a Customer if the nationalId is not already registered. */
    private Customer createCustomerIfMissing(
            String nationalId,
            String firstName,
            String lastName,
            LocalDate dob,
            String email,
            String phone,
            String address,
            String kycStatus) {
        return customerRepository
                .findByNationalId(nationalId)
                .orElseGet(
                        () -> {
                            Customer c =
                                    Customer.builder()
                                            .nationalId(nationalId)
                                            .firstName(firstName)
                                            .lastName(lastName)
                                            .dob(dob)
                                            .email(email)
                                            .phone(phone)
                                            .address(address)
                                            .kycStatus(kycStatus)
                                            .tenant(defaultTenant)
                                            .createdBy(adminUser)
                                            .build();
                            return customerRepository.save(c);
                        });
    }

    /**
     * Helper — creates an Account with all relationship fields.
     *
     * @param accountNumber unique account number
     * @param accountName display name
     * @param accountType enum: SAVINGS, CURRENT, LOAN, GL_ACCOUNT, etc.
     * @param ledgerType enum: CUSTOMER_ACCOUNT, GL_ACCOUNT, INTERNAL_ACCOUNT, etc.
     * @param balance opening balance
     * @param currency ISO currency code
     * @param branch branch entity
     * @param customer customer entity (null for system accounts)
     * @param customerName denormalized customer name (null for system accounts)
     * @param glAccountCode GL code this account maps to (nullable)
     * @param parentAccount parent account for hierarchy (nullable)
     */
    private Account createAccount(
            String accountNumber,
            String accountName,
            AccountType accountType,
            LedgerAccountType ledgerType,
            BigDecimal balance,
            String currency,
            Branch branch,
            Customer customer,
            String customerName,
            String glAccountCode,
            Account parentAccount) {
        Account account =
                Account.builder()
                        .accountNumber(accountNumber)
                        .accountName(accountName)
                        .accountType(accountType)
                        .ledgerAccountType(ledgerType)
                        .tenant(defaultTenant)
                        .status(AccountStatus.ACTIVE)
                        .balance(balance)
                        .currency(currency)
                        .branch(branch)
                        .branchCode(branch.getBranchCode())
                        .customer(customer)
                        .customerName(customerName)
                        .customerEmail(customer != null ? customer.getEmail() : null)
                        .customerPhone(customer != null ? customer.getPhone() : null)
                        .glAccountCode(glAccountCode)
                        .parentAccount(parentAccount)
                        .createdBy(adminUser)
                        .build();
        return accountRepository.save(account);
    }

    /** Helper — creates an AccountBalance cache record. */
    private void createAccountBalance(Account account, BigDecimal ledgerBalance) {
        AccountBalance ab =
                AccountBalance.builder()
                        .account(account)
                        .ledgerBalance(ledgerBalance)
                        .availableBalance(ledgerBalance)
                        .holdAmount(BigDecimal.ZERO)
                        .actualTotalBalance(ledgerBalance)
                        .actualClearedBalance(ledgerBalance)
                        .shadowTotalBalance(BigDecimal.ZERO)
                        .shadowClearingBalance(BigDecimal.ZERO)
                        .inwardClearingBalance(BigDecimal.ZERO)
                        .unclearedEffectBalance(BigDecimal.ZERO)
                        .lienBalance(BigDecimal.ZERO)
                        .chargeHoldBalance(BigDecimal.ZERO)
                        .build();
        accountBalanceRepository.save(ab);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. SEED SAMPLE TRANSACTIONS + LEDGER JOURNALS + ENTRIES
    //    Ensures SUM(debits) = SUM(credits) for every journal.
    //    Uses today's business date and INR currency.
    // ════════════════════════════════════════════════════════════════════════
    private void initSampleTransactionsAndLedger() {
        if (transactionRepository.count() > 0) {
            log.info("  [Transactions] Transactions already exist — skipping");
            return;
        }

        LocalDate businessDate = LocalDate.now();

        // Look up accounts for the sample transactions
        Account rajeshSavings = accountRepository.findByAccountNumber("SAV-1001-0001").orElse(null);
        Account priyaSavings = accountRepository.findByAccountNumber("SAV-1002-0001").orElse(null);
        Account rajeshCurrent = accountRepository.findByAccountNumber("CUR-1001-0001").orElse(null);

        if (rajeshSavings == null || priyaSavings == null || rajeshCurrent == null) {
            log.warn("  [Transactions] Customer accounts not found — skipping sample transactions");
            return;
        }

        // ── Transaction 1: Deposit 10,000 INR to Rajesh Savings ──
        Transaction dep1 =
                Transaction.builder()
                        .transactionRef("DEP-SEED-0001")
                        .transactionType(TransactionType.DEPOSIT)
                        .status(TransactionStatus.COMPLETED)
                        .amount(new BigDecimal("10000.0000"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER)
                        .destinationAccount(rajeshSavings)
                        .description("Opening deposit - Rajesh Kumar Savings")
                        .narration("Cash deposit at branch counter")
                        .businessDate(businessDate)
                        .performedBy(teller1User)
                        .tenant(defaultTenant)
                        .build();
        dep1 = transactionRepository.save(dep1);

        // Journal for deposit: DEBIT Cash GL (1100), CREDIT Customer Deposits GL (2110)
        // SUM(debits) = 10,000 = SUM(credits) — balanced
        createBalancedJournal(
                dep1,
                "Opening Deposit - Rajesh Savings",
                businessDate,
                rajeshSavings,
                new BigDecimal("10000.0000"),
                "INR",
                "1100",
                "2110",
                new BigDecimal("60000.0000"),
                new BigDecimal("60000.0000"));

        // ── Transaction 2: Deposit 15,000 INR to Priya Savings ──
        Transaction dep2 =
                Transaction.builder()
                        .transactionRef("DEP-SEED-0002")
                        .transactionType(TransactionType.DEPOSIT)
                        .status(TransactionStatus.COMPLETED)
                        .amount(new BigDecimal("15000.0000"))
                        .currency("INR")
                        .channel(TransactionChannel.TELLER)
                        .destinationAccount(priyaSavings)
                        .description("Opening deposit - Priya Sharma Savings")
                        .narration("Cash deposit at branch counter")
                        .businessDate(businessDate)
                        .performedBy(teller1User)
                        .tenant(defaultTenant)
                        .build();
        dep2 = transactionRepository.save(dep2);

        createBalancedJournal(
                dep2,
                "Opening Deposit - Priya Savings",
                businessDate,
                priyaSavings,
                new BigDecimal("15000.0000"),
                "INR",
                "1100",
                "2110",
                new BigDecimal("90000.0000"),
                new BigDecimal("90000.0000"));

        // ── Transaction 3: Transfer 5,000 INR from Rajesh Savings -> Priya Savings ──
        Transaction trf1 =
                Transaction.builder()
                        .transactionRef("TRF-SEED-0001")
                        .transactionType(TransactionType.TRANSFER)
                        .status(TransactionStatus.COMPLETED)
                        .amount(new BigDecimal("5000.0000"))
                        .currency("INR")
                        .channel(TransactionChannel.ONLINE)
                        .sourceAccount(rajeshSavings)
                        .destinationAccount(priyaSavings)
                        .description("Internal transfer - Rajesh to Priya")
                        .narration("Funds transfer via online banking")
                        .businessDate(businessDate)
                        .performedBy(teller1User)
                        .tenant(defaultTenant)
                        .build();
        trf1 = transactionRepository.save(trf1);

        // Transfer journal: DEBIT source (2100), CREDIT destination (2100)
        // SUM(debits) = 5,000 = SUM(credits) — balanced
        createBalancedJournal(
                trf1,
                "Internal Transfer - Rajesh to Priya",
                businessDate,
                rajeshSavings,
                new BigDecimal("5000.0000"),
                "INR",
                "2100",
                "2100",
                new BigDecimal("55000.0000"),
                new BigDecimal("95000.0000"));

        // ── Transaction 4: Withdrawal 2,000 INR from Rajesh Current ──
        Transaction wdr1 =
                Transaction.builder()
                        .transactionRef("WDR-SEED-0001")
                        .transactionType(TransactionType.WITHDRAWAL)
                        .status(TransactionStatus.COMPLETED)
                        .amount(new BigDecimal("2000.0000"))
                        .currency("INR")
                        .channel(TransactionChannel.ATM)
                        .sourceAccount(rajeshCurrent)
                        .description("ATM Withdrawal - Rajesh Current")
                        .narration("ATM cash withdrawal")
                        .businessDate(businessDate)
                        .performedBy(teller1User)
                        .tenant(defaultTenant)
                        .build();
        wdr1 = transactionRepository.save(wdr1);

        // Withdrawal journal: DEBIT Customer Deposits (2100), CREDIT Cash (1100)
        // SUM(debits) = 2,000 = SUM(credits) — balanced
        createBalancedJournal(
                wdr1,
                "ATM Withdrawal - Rajesh Current",
                businessDate,
                rajeshCurrent,
                new BigDecimal("2000.0000"),
                "INR",
                "2100",
                "1100",
                new BigDecimal("148000.0000"),
                new BigDecimal("148000.0000"));

        log.info("  [Transactions] 4 sample transactions with balanced ledger journals created");
    }

    /**
     * Helper — creates a balanced LedgerJournal with exactly 2 LedgerEntry records: one DEBIT and
     * one CREDIT, ensuring SUM(debits) = SUM(credits).
     *
     * @param transaction the parent transaction
     * @param description journal description
     * @param businessDate business date for the entries
     * @param account account linked to both entries
     * @param amount amount for both debit and credit (must be equal for balance)
     * @param currency ISO currency code
     * @param debitGlCode GL code for the debit entry
     * @param creditGlCode GL code for the credit entry
     * @param debitBalanceAfter balance after debit
     * @param creditBalanceAfter balance after credit
     */
    private void createBalancedJournal(
            Transaction transaction,
            String description,
            LocalDate businessDate,
            Account account,
            BigDecimal amount,
            String currency,
            String debitGlCode,
            String creditGlCode,
            BigDecimal debitBalanceAfter,
            BigDecimal creditBalanceAfter) {
        // Resolve GL accounts by code
        GeneralLedger debitGL = glRepository.findByGlCode(debitGlCode).orElse(null);
        GeneralLedger creditGL = glRepository.findByGlCode(creditGlCode).orElse(null);

        // Create the journal header
        LedgerJournal journal =
                LedgerJournal.builder()
                        .transaction(transaction)
                        .tenant(defaultTenant)
                        .description(description)
                        .businessDate(businessDate)
                        .build();
        journal = ledgerJournalRepository.save(journal);

        // DEBIT entry
        LedgerEntry debitEntry =
                LedgerEntry.builder()
                        .journal(journal)
                        .transaction(transaction)
                        .tenant(defaultTenant)
                        .account(account)
                        .glAccount(debitGL)
                        .glAccountCode(debitGlCode)
                        .entryType(EntryType.DEBIT)
                        .amount(amount)
                        .balanceAfter(debitBalanceAfter)
                        .currency(currency)
                        .businessDate(businessDate)
                        .postingTime(LocalDateTime.now())
                        .narration(description + " [DEBIT]")
                        .build();
        ledgerEntryRepository.save(debitEntry);

        // CREDIT entry (same amount to maintain double-entry balance)
        LedgerEntry creditEntry =
                LedgerEntry.builder()
                        .journal(journal)
                        .transaction(transaction)
                        .tenant(defaultTenant)
                        .account(account)
                        .glAccount(creditGL)
                        .glAccountCode(creditGlCode)
                        .entryType(EntryType.CREDIT)
                        .amount(amount)
                        .balanceAfter(creditBalanceAfter)
                        .currency(currency)
                        .businessDate(businessDate)
                        .postingTime(LocalDateTime.now())
                        .narration(description + " [CREDIT]")
                        .build();
        ledgerEntryRepository.save(creditEntry);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. SEED EXCHANGE RATES — multi-currency support
    //    USD->INR, USD->EUR, INR->EUR, EUR->INR, GBP->INR, INR->USD
    // ════════════════════════════════════════════════════════════════════════
    private void initExchangeRates() {
        if (exchangeRateRepository.count() > 0) {
            log.info("  [FX] Exchange rates already exist — skipping");
            return;
        }

        LocalDate today = LocalDate.now();

        createExchangeRate("USD", "INR", new BigDecimal("83.12500000"), today);
        createExchangeRate("USD", "EUR", new BigDecimal("0.92150000"), today);
        createExchangeRate("INR", "EUR", new BigDecimal("0.01108500"), today);
        createExchangeRate("EUR", "INR", new BigDecimal("90.21400000"), today);
        createExchangeRate("GBP", "INR", new BigDecimal("105.47000000"), today);
        createExchangeRate("INR", "USD", new BigDecimal("0.01203000"), today);

        log.info("  [FX] 6 exchange rates seeded (USD/INR/EUR/GBP pairs)");
    }

    private void createExchangeRate(
            String from, String to, BigDecimal rate, LocalDate effectiveDate) {
        ExchangeRate er =
                ExchangeRate.builder()
                        .currencyFrom(from)
                        .currencyTo(to)
                        .rate(rate)
                        .effectiveDate(effectiveDate)
                        .build();
        exchangeRateRepository.save(er);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9. SEED IDEMPOTENCY KEYS — sample keys for testing deduplication
    // ════════════════════════════════════════════════════════════════════════
    private void initIdempotencyKeys() {
        if (idempotencyKeyRepository.count() > 0) {
            log.info("  [Idempotency] Keys already exist — skipping");
            return;
        }

        createIdempotencyKey("DEP-SEED-0001:TELLER", "deposit-hash-001", "COMPLETED");
        createIdempotencyKey("DEP-SEED-0002:TELLER", "deposit-hash-002", "COMPLETED");
        createIdempotencyKey("TRF-SEED-0001:ONLINE", "transfer-hash-001", "COMPLETED");
        createIdempotencyKey("WDR-SEED-0001:ATM", "withdrawal-hash-001", "COMPLETED");
        createIdempotencyKey("TEST-IDEM-001:MOBILE", "test-hash-001", "PROCESSING");

        log.info("  [Idempotency] 5 sample idempotency keys seeded");
    }

    private void createIdempotencyKey(String key, String requestHash, String status) {
        IdempotencyKey ik =
                IdempotencyKey.builder().key(key).requestHash(requestHash).status(status).build();
        idempotencyKeyRepository.save(ik);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 11. SEED CBS CUSTOMERS & TAX PROFILES
    //     Sample verified customers with tax profiles (tenant-scoped)
    // ════════════════════════════════════════════════════════════════════════
    private void initCbsCustomersAndTaxProfiles() {
        if (customerMasterRepository.count() > 0) {
            log.info("  [CBS Customers] CustomerMaster records already exist — skipping");
            return;
        }

        // ── Sample verified customer 1: Rajesh Kumar ──
        CustomerMaster cm1 =
                CustomerMaster.builder()
                        .tenant(defaultTenant)
                        .customerNumber("CBS-CUST-001")
                        .firstName("Rajesh")
                        .lastName("Kumar")
                        .fullName("Rajesh Kumar")
                        .dob(LocalDate.of(1985, 3, 15))
                        .nationalId("ABCDE1234F")
                        .phone("9100000001")
                        .email("rajesh.kumar@email.com")
                        .address("123 MG Road, Mumbai")
                        .kycStatus("VERIFIED")
                        .customerType("INDIVIDUAL")
                        .makerCheckerStatus(MakerCheckerStatus.APPROVED)
                        .approvalStatus(MakerCheckerStatus.APPROVED)
                        .build();
        cm1 = customerMasterRepository.save(cm1);

        CustomerTaxProfile tp1 =
                CustomerTaxProfile.builder()
                        .tenant(defaultTenant)
                        .customerMaster(cm1)
                        .panNumber("ABCDE1234F")
                        .aadhaarNumber("123456789012")
                        .tdsApplicable(true)
                        .tdsRate(new BigDecimal("10.00"))
                        .fatcaDeclaration(true)
                        .taxResidencyStatus("RESIDENT")
                        .taxDeductionFlag(true)
                        .build();
        tp1 = customerTaxProfileRepository.save(tp1);
        cm1.setTaxProfile(tp1);
        customerMasterRepository.save(cm1);

        // ── Sample verified customer 2: Priya Sharma ──
        CustomerMaster cm2 =
                CustomerMaster.builder()
                        .tenant(defaultTenant)
                        .customerNumber("CBS-CUST-002")
                        .firstName("Priya")
                        .lastName("Sharma")
                        .fullName("Priya Sharma")
                        .dob(LocalDate.of(1990, 7, 22))
                        .nationalId("FGHIJ5678K")
                        .phone("9100000002")
                        .email("priya.sharma@email.com")
                        .address("456 Park Avenue, Delhi")
                        .kycStatus("VERIFIED")
                        .customerType("INDIVIDUAL")
                        .makerCheckerStatus(MakerCheckerStatus.APPROVED)
                        .approvalStatus(MakerCheckerStatus.APPROVED)
                        .build();
        cm2 = customerMasterRepository.save(cm2);

        CustomerTaxProfile tp2 =
                CustomerTaxProfile.builder()
                        .tenant(defaultTenant)
                        .customerMaster(cm2)
                        .panNumber("FGHIJ5678K")
                        .aadhaarNumber("987654321098")
                        .tdsApplicable(true)
                        .tdsRate(new BigDecimal("10.00"))
                        .fatcaDeclaration(true)
                        .taxResidencyStatus("RESIDENT")
                        .taxDeductionFlag(true)
                        .build();
        tp2 = customerTaxProfileRepository.save(tp2);
        cm2.setTaxProfile(tp2);
        customerMasterRepository.save(cm2);

        // ── Sample corporate customer 3 ──
        CustomerMaster cm3 =
                CustomerMaster.builder()
                        .tenant(defaultTenant)
                        .customerNumber("CBS-CUST-003")
                        .firstName("Acme")
                        .lastName("Corp")
                        .fullName("Acme Corp")
                        .nationalId("KLMNO9012P")
                        .phone("9100000003")
                        .email("finance@acmecorp.com")
                        .address("789 Business Park, Bangalore")
                        .kycStatus("VERIFIED")
                        .customerType("CORPORATE")
                        .makerCheckerStatus(MakerCheckerStatus.APPROVED)
                        .approvalStatus(MakerCheckerStatus.APPROVED)
                        .build();
        cm3 = customerMasterRepository.save(cm3);

        CustomerTaxProfile tp3 =
                CustomerTaxProfile.builder()
                        .tenant(defaultTenant)
                        .customerMaster(cm3)
                        .panNumber("KLMNO9012P")
                        .gstNumber("29KLMNO9012PZAB")
                        .tdsApplicable(true)
                        .tdsRate(new BigDecimal("2.00"))
                        .fatcaDeclaration(false)
                        .taxResidencyStatus("RESIDENT")
                        .taxDeductionFlag(true)
                        .build();
        tp3 = customerTaxProfileRepository.save(tp3);
        cm3.setTaxProfile(tp3);
        customerMasterRepository.save(cm3);

        // ── Sample pending customer 4 (not yet approved) ──
        CustomerMaster cm4 =
                CustomerMaster.builder()
                        .tenant(defaultTenant)
                        .customerNumber("CBS-CUST-004")
                        .firstName("Amit")
                        .lastName("Patel")
                        .fullName("Amit Patel")
                        .dob(LocalDate.of(1988, 11, 5))
                        .nationalId("PQRST3456U")
                        .phone("9100000004")
                        .email("amit.patel@email.com")
                        .address("321 Hill Street, Hyderabad")
                        .kycStatus("PENDING")
                        .customerType("INDIVIDUAL")
                        .makerCheckerStatus(MakerCheckerStatus.PENDING)
                        .approvalStatus(MakerCheckerStatus.PENDING)
                        .build();
        customerMasterRepository.save(cm4);

        log.info(
                "  [CBS Customers] 4 CustomerMaster records seeded (3 APPROVED, 1 PENDING) with tax profiles");
    }
}
