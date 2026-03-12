package com.ledgora.config.seeder;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.*;
import com.ledgora.customer.entity.Customer;
import com.ledgora.customer.repository.CustomerRepository;
import com.ledgora.tenant.entity.Tenant;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** CBS DataSeeder: Module 6 — Customers, Accounts, AccountBalances, IBC accounts. */
@Component
public class CustomerAccountSeeder {

    private static final Logger log = LoggerFactory.getLogger(CustomerAccountSeeder.class);
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;

    public CustomerAccountSeeder(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            AccountBalanceRepository accountBalanceRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
    }

    public void seed(Tenant tenant, User admin, Branch hq, Branch br1, Branch br2) {
        if (accountRepository.count() > 0) {
            log.info("  [Accounts] Accounts already exist — skipping");
            return;
        }

        Customer c1 =
                cust(
                        "CUST-NID-001",
                        "Rajesh",
                        "Kumar",
                        LocalDate.of(1985, 3, 15),
                        "rajesh.kumar@email.com",
                        "+91-9100000001",
                        "123 MG Road, Mumbai",
                        "VERIFIED",
                        tenant,
                        admin);
        Customer c2 =
                cust(
                        "CUST-NID-002",
                        "Priya",
                        "Sharma",
                        LocalDate.of(1990, 7, 22),
                        "priya.sharma@email.com",
                        "+91-9100000002",
                        "456 Park Avenue, Delhi",
                        "VERIFIED",
                        tenant,
                        admin);
        Customer c3 =
                cust(
                        "CUST-NID-003",
                        "Amit",
                        "Patel",
                        LocalDate.of(1988, 11, 5),
                        "amit.patel@email.com",
                        "+91-9100000003",
                        "789 Lake Road, Bangalore",
                        "PENDING",
                        tenant,
                        admin);
        Customer c4 =
                cust(
                        "CUST-NID-004",
                        "Sneha",
                        "Reddy",
                        LocalDate.of(1995, 1, 30),
                        "sneha.reddy@email.com",
                        "+91-9100000004",
                        "321 Hill Street, Hyderabad",
                        "PENDING",
                        tenant,
                        admin);
        // ── Bulk customers for pagination testing (26 more) ──
        String[][] bulkCustomers = {
            {
                "CUST-NID-005",
                "Vikram",
                "Singh",
                "1982-06-10",
                "vikram.singh@email.com",
                "+91-9100000005",
                "45 Station Road, Jaipur",
                "VERIFIED"
            },
            {
                "CUST-NID-006",
                "Meera",
                "Nair",
                "1991-02-28",
                "meera.nair@email.com",
                "+91-9100000006",
                "78 Beach Road, Kochi",
                "VERIFIED"
            },
            {
                "CUST-NID-007",
                "Suresh",
                "Gupta",
                "1978-09-12",
                "suresh.gupta@email.com",
                "+91-9100000007",
                "12 Ring Road, Lucknow",
                "VERIFIED"
            },
            {
                "CUST-NID-008",
                "Anita",
                "Desai",
                "1993-04-18",
                "anita.desai@email.com",
                "+91-9100000008",
                "90 Civil Lines, Nagpur",
                "VERIFIED"
            },
            {
                "CUST-NID-009",
                "Ramesh",
                "Iyer",
                "1986-12-05",
                "ramesh.iyer@email.com",
                "+91-9100000009",
                "33 Temple Street, Chennai",
                "VERIFIED"
            },
            {
                "CUST-NID-010",
                "Kavita",
                "Joshi",
                "1989-07-22",
                "kavita.joshi@email.com",
                "+91-9100000010",
                "56 MG Marg, Pune",
                "VERIFIED"
            },
            {
                "CUST-NID-011",
                "Deepak",
                "Mehta",
                "1975-11-30",
                "deepak.mehta@email.com",
                "+91-9100000011",
                "21 Ashram Road, Ahmedabad",
                "VERIFIED"
            },
            {
                "CUST-NID-012",
                "Sunita",
                "Rao",
                "1994-01-15",
                "sunita.rao@email.com",
                "+91-9100000012",
                "67 Jubilee Hills, Hyderabad",
                "VERIFIED"
            },
            {
                "CUST-NID-013",
                "Arun",
                "Chopra",
                "1983-08-08",
                "arun.chopra@email.com",
                "+91-9100000013",
                "14 Sector 17, Chandigarh",
                "VERIFIED"
            },
            {
                "CUST-NID-014",
                "Lakshmi",
                "Menon",
                "1990-03-25",
                "lakshmi.menon@email.com",
                "+91-9100000014",
                "88 Boat Jetty, Trivandrum",
                "VERIFIED"
            },
            {
                "CUST-NID-015",
                "Manoj",
                "Tiwari",
                "1987-05-14",
                "manoj.tiwari@email.com",
                "+91-9100000015",
                "32 Hazratganj, Lucknow",
                "PENDING"
            },
            {
                "CUST-NID-016",
                "Pooja",
                "Agarwal",
                "1992-10-02",
                "pooja.agarwal@email.com",
                "+91-9100000016",
                "55 Lajpat Nagar, Delhi",
                "PENDING"
            },
            {
                "CUST-NID-017",
                "Sanjay",
                "Bhat",
                "1980-01-20",
                "sanjay.bhat@email.com",
                "+91-9100000017",
                "11 Majestic, Bangalore",
                "VERIFIED"
            },
            {
                "CUST-NID-018",
                "Divya",
                "Pillai",
                "1996-06-30",
                "divya.pillai@email.com",
                "+91-9100000018",
                "42 Marine Drive, Mumbai",
                "VERIFIED"
            },
            {
                "CUST-NID-019",
                "Ravi",
                "Verma",
                "1981-04-11",
                "ravi.verma@email.com",
                "+91-9100000019",
                "73 Mall Road, Shimla",
                "VERIFIED"
            },
            {
                "CUST-NID-020",
                "Neha",
                "Saxena",
                "1995-09-18",
                "neha.saxena@email.com",
                "+91-9100000020",
                "29 Connaught Place, Delhi",
                "PENDING"
            },
            {
                "CUST-NID-021",
                "Prakash",
                "Das",
                "1977-12-25",
                "prakash.das@email.com",
                "+91-9100000021",
                "64 Park Street, Kolkata",
                "VERIFIED"
            },
            {
                "CUST-NID-022",
                "Swati",
                "Kulkarni",
                "1988-02-14",
                "swati.kulkarni@email.com",
                "+91-9100000022",
                "17 FC Road, Pune",
                "VERIFIED"
            },
            {
                "CUST-NID-023",
                "Gaurav",
                "Pandey",
                "1984-07-07",
                "gaurav.pandey@email.com",
                "+91-9100000023",
                "38 Varanasi Ghat, Varanasi",
                "VERIFIED"
            },
            {
                "CUST-NID-024",
                "Asha",
                "Hegde",
                "1993-11-11",
                "asha.hegde@email.com",
                "+91-9100000024",
                "51 Mangalore Road, Udupi",
                "PENDING"
            },
            {
                "CUST-NID-025",
                "Kiran",
                "Shetty",
                "1979-03-03",
                "kiran.shetty@email.com",
                "+91-9100000025",
                "85 Bunder Road, Mangalore",
                "VERIFIED"
            },
            {
                "CUST-NID-026",
                "Rekha",
                "Mishra",
                "1991-08-19",
                "rekha.mishra@email.com",
                "+91-9100000026",
                "26 Boring Road, Patna",
                "VERIFIED"
            },
            {
                "CUST-NID-027",
                "Ajay",
                "Thakur",
                "1986-05-27",
                "ajay.thakur@email.com",
                "+91-9100000027",
                "70 Mall Road, Dehradun",
                "VERIFIED"
            },
            {
                "CUST-NID-028",
                "Nisha",
                "Sen",
                "1994-10-09",
                "nisha.sen@email.com",
                "+91-9100000028",
                "43 Park Circus, Kolkata",
                "PENDING"
            },
            {
                "CUST-NID-029",
                "Vivek",
                "Kapoor",
                "1982-01-16",
                "vivek.kapoor@email.com",
                "+91-9100000029",
                "19 Rajouri Garden, Delhi",
                "VERIFIED"
            },
            {
                "CUST-NID-030",
                "Tanvi",
                "Bhatt",
                "1997-06-21",
                "tanvi.bhatt@email.com",
                "+91-9100000030",
                "62 SG Highway, Ahmedabad",
                "VERIFIED"
            },
        };
        int bulkIdx = 0;
        for (String[] cd : bulkCustomers) {
            Customer bc =
                    cust(
                            cd[0],
                            cd[1],
                            cd[2],
                            LocalDate.parse(cd[3]),
                            cd[4],
                            cd[5],
                            cd[6],
                            cd[7],
                            tenant,
                            admin);
            // Each bulk customer gets a Savings account
            Branch assignedBranch = (bulkIdx % 2 == 0) ? br1 : br2;
            String savNum = String.format("SAV-%04d-0001", bulkIdx + 5);
            BigDecimal savBal = new BigDecimal((bulkIdx + 1) * 10000 + ".0000");
            Account savAcct =
                    acct(
                            savNum,
                            cd[1] + " " + cd[2] + " - Savings",
                            AccountType.SAVINGS,
                            LedgerAccountType.CUSTOMER_ACCOUNT,
                            savBal,
                            "INR",
                            assignedBranch,
                            bc,
                            cd[1] + " " + cd[2],
                            "2110",
                            null,
                            tenant,
                            admin);
            bal(savAcct, savBal);
            // Every 3rd customer also gets a Current account
            if (bulkIdx % 3 == 0) {
                String curNum = String.format("CUR-%04d-0001", bulkIdx + 5);
                BigDecimal curBal = new BigDecimal((bulkIdx + 1) * 25000 + ".0000");
                Account curAcct =
                        acct(
                                curNum,
                                cd[1] + " " + cd[2] + " - Current",
                                AccountType.CURRENT,
                                LedgerAccountType.CUSTOMER_ACCOUNT,
                                curBal,
                                "INR",
                                assignedBranch,
                                bc,
                                cd[1] + " " + cd[2],
                                "2120",
                                null,
                                tenant,
                                admin);
                bal(curAcct, curBal);
            }
            bulkIdx++;
        }
        log.info("  [Customers] 30 customers ready (bulk seeded for pagination testing)");

        // GL / System accounts
        Account glCash =
                acct(
                        "GL-CASH-001",
                        "Cash GL Account",
                        AccountType.GL_ACCOUNT,
                        LedgerAccountType.GL_ACCOUNT,
                        new BigDecimal("5000000.0000"),
                        "INR",
                        hq,
                        null,
                        null,
                        "1100",
                        null,
                        tenant,
                        admin);
        Account glLoans =
                acct(
                        "GL-LOAN-001",
                        "Loans GL Account",
                        AccountType.GL_ACCOUNT,
                        LedgerAccountType.GL_ACCOUNT,
                        new BigDecimal("2000000.0000"),
                        "INR",
                        hq,
                        null,
                        null,
                        "1200",
                        null,
                        tenant,
                        admin);
        Account glDep =
                acct(
                        "GL-DEP-001",
                        "Customer Deposits GL Account",
                        AccountType.GL_ACCOUNT,
                        LedgerAccountType.GL_ACCOUNT,
                        new BigDecimal("3000000.0000"),
                        "INR",
                        hq,
                        null,
                        null,
                        "2100",
                        null,
                        tenant,
                        admin);
        acct(
                "INT-SUSP-001",
                "Internal Suspense Account",
                AccountType.INTERNAL_ACCOUNT,
                LedgerAccountType.INTERNAL_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                hq,
                null,
                null,
                null,
                null,
                tenant,
                admin);
        acct(
                "CLR-001",
                "Main Clearing Account",
                AccountType.CLEARING_ACCOUNT,
                LedgerAccountType.CLEARING_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                hq,
                null,
                null,
                null,
                null,
                tenant,
                admin);
        acct(
                "SET-001",
                "Settlement Account",
                AccountType.SETTLEMENT_ACCOUNT,
                LedgerAccountType.SETTLEMENT_ACCOUNT,
                BigDecimal.ZERO,
                "INR",
                hq,
                null,
                null,
                null,
                null,
                tenant,
                admin);

        // IBC accounts
        for (Object[] b :
                new Object[][] {
                    {"HQ001", "Head Office", hq},
                    {"BR001", "Downtown Branch", br1},
                    {"BR002", "Uptown Branch", br2}
                }) {
            acct(
                    "IBC-OUT-" + b[0],
                    "IBC Outward - " + b[1],
                    AccountType.CLEARING_ACCOUNT,
                    LedgerAccountType.CLEARING_ACCOUNT,
                    BigDecimal.ZERO,
                    "INR",
                    (Branch) b[2],
                    null,
                    null,
                    "2910",
                    null,
                    tenant,
                    admin);
            acct(
                    "IBC-IN-" + b[0],
                    "IBC Inward - " + b[1],
                    AccountType.CLEARING_ACCOUNT,
                    LedgerAccountType.CLEARING_ACCOUNT,
                    BigDecimal.ZERO,
                    "INR",
                    (Branch) b[2],
                    null,
                    null,
                    "2910",
                    null,
                    tenant,
                    admin);
        }
        log.info("  [IBC] 6 Inter-Branch Clearing accounts seeded");

        // Customer accounts
        Account rs =
                acct(
                        "SAV-1001-0001",
                        "Rajesh Kumar - Savings",
                        AccountType.SAVINGS,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("50000.0000"),
                        "INR",
                        br1,
                        c1,
                        "Rajesh Kumar",
                        "2110",
                        null,
                        tenant,
                        admin);
        Account rc =
                acct(
                        "CUR-1001-0001",
                        "Rajesh Kumar - Current",
                        AccountType.CURRENT,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("150000.0000"),
                        "INR",
                        br1,
                        c1,
                        "Rajesh Kumar",
                        "2120",
                        null,
                        tenant,
                        admin);
        Account rl =
                acct(
                        "LN-1001-0001",
                        "Rajesh Kumar - Personal Loan",
                        AccountType.LOAN,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("500000.0000"),
                        "INR",
                        br1,
                        c1,
                        "Rajesh Kumar",
                        "1210",
                        null,
                        tenant,
                        admin);
        Account rf =
                acct(
                        "FD-1001-0001",
                        "Rajesh Kumar - Fixed Deposit",
                        AccountType.FIXED_DEPOSIT,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("200000.0000"),
                        "INR",
                        br1,
                        c1,
                        "Rajesh Kumar",
                        "2130",
                        null,
                        tenant,
                        admin);
        Account ps =
                acct(
                        "SAV-1002-0001",
                        "Priya Sharma - Savings",
                        AccountType.SAVINGS,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("75000.0000"),
                        "INR",
                        br1,
                        c2,
                        "Priya Sharma",
                        "2110",
                        null,
                        tenant,
                        admin);
        Account pc =
                acct(
                        "CUR-1002-0001",
                        "Priya Sharma - Current",
                        AccountType.CURRENT,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("250000.0000"),
                        "INR",
                        br1,
                        c2,
                        "Priya Sharma",
                        "2120",
                        null,
                        tenant,
                        admin);
        Account pf =
                acct(
                        "FD-1002-0001",
                        "Priya Sharma - Fixed Deposit",
                        AccountType.FIXED_DEPOSIT,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("500000.0000"),
                        "INR",
                        br1,
                        c2,
                        "Priya Sharma",
                        "2130",
                        null,
                        tenant,
                        admin);
        Account as =
                acct(
                        "SAV-1003-0001",
                        "Amit Patel - Savings",
                        AccountType.SAVINGS,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("10000.0000"),
                        "INR",
                        br2,
                        c3,
                        "Amit Patel",
                        "2110",
                        null,
                        tenant,
                        admin);
        Account su =
                acct(
                        "CUR-1004-0001",
                        "Sneha Reddy - USD Current",
                        AccountType.CURRENT,
                        LedgerAccountType.CUSTOMER_ACCOUNT,
                        new BigDecimal("5000.0000"),
                        "USD",
                        br2,
                        c4,
                        "Sneha Reddy",
                        "2120",
                        null,
                        tenant,
                        admin);

        log.info("  [Accounts] {} accounts created", accountRepository.count());

        // Balances
        for (Object[] ab :
                new Object[][] {
                    {rs, "50000.0000"},
                    {rc, "150000.0000"},
                    {rl, "500000.0000"},
                    {rf, "200000.0000"},
                    {ps, "75000.0000"},
                    {pc, "250000.0000"},
                    {pf, "500000.0000"},
                    {as, "10000.0000"},
                    {su, "5000.0000"},
                    {glCash, "5000000.0000"},
                    {glLoans, "2000000.0000"},
                    {glDep, "3000000.0000"}
                }) {
            bal((Account) ab[0], new BigDecimal((String) ab[1]));
        }
        log.info(
                "  [AccountBalance] {} balance cache records created",
                accountBalanceRepository.count());
    }

    private Customer cust(
            String nid,
            String first,
            String last,
            LocalDate dob,
            String email,
            String phone,
            String addr,
            String kyc,
            Tenant tenant,
            User admin) {
        return customerRepository
                .findByNationalId(nid)
                .orElseGet(
                        () -> {
                            Customer c =
                                    Customer.builder()
                                            .nationalId(nid)
                                            .firstName(first)
                                            .lastName(last)
                                            .dob(dob)
                                            .email(email)
                                            .phone(phone)
                                            .address(addr)
                                            .kycStatus(kyc)
                                            .tenant(tenant)
                                            .createdBy(admin)
                                            .build();
                            return customerRepository.save(c);
                        });
    }

    private Account acct(
            String num,
            String name,
            AccountType type,
            LedgerAccountType ledger,
            BigDecimal balance,
            String ccy,
            Branch branch,
            Customer cust,
            String custName,
            String glCode,
            Account parent,
            Tenant tenant,
            User admin) {
        return accountRepository.save(
                Account.builder()
                        .accountNumber(num)
                        .accountName(name)
                        .accountType(type)
                        .ledgerAccountType(ledger)
                        .tenant(tenant)
                        .status(AccountStatus.ACTIVE)
                        // Seeded accounts are pre-approved (represent post-checker state).
                        // Real accounts created via UI start as PENDING and require checker approval.
                        .approvalStatus(MakerCheckerStatus.APPROVED)
                        .approvedBy(admin)
                        .balance(balance)
                        .currency(ccy)
                        .branch(branch)
                        .branchCode(branch.getBranchCode())
                        .customer(cust)
                        .customerName(custName)
                        .customerEmail(cust != null ? cust.getEmail() : null)
                        .customerPhone(cust != null ? cust.getPhone() : null)
                        .glAccountCode(glCode)
                        .parentAccount(parent)
                        .createdBy(admin)
                        .build());
    }

    private void bal(Account account, BigDecimal ledgerBalance) {
        accountBalanceRepository.save(
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
                        .build());
    }
}
