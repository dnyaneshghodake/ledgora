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

/**
 * CBS DataSeeder: Module 6 — Customers, Accounts, AccountBalances, IBC accounts.
 */
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

        Customer c1 = cust("CUST-NID-001", "Rajesh", "Kumar", LocalDate.of(1985, 3, 15),
                "rajesh.kumar@email.com", "+91-9100000001", "123 MG Road, Mumbai", "VERIFIED", tenant, admin);
        Customer c2 = cust("CUST-NID-002", "Priya", "Sharma", LocalDate.of(1990, 7, 22),
                "priya.sharma@email.com", "+91-9100000002", "456 Park Avenue, Delhi", "VERIFIED", tenant, admin);
        Customer c3 = cust("CUST-NID-003", "Amit", "Patel", LocalDate.of(1988, 11, 5),
                "amit.patel@email.com", "+91-9100000003", "789 Lake Road, Bangalore", "PENDING", tenant, admin);
        Customer c4 = cust("CUST-NID-004", "Sneha", "Reddy", LocalDate.of(1995, 1, 30),
                "sneha.reddy@email.com", "+91-9100000004", "321 Hill Street, Hyderabad", "PENDING", tenant, admin);
        log.info("  [Customers] 4 customers ready (2 VERIFIED, 2 PENDING)");

        // GL / System accounts
        Account glCash = acct("GL-CASH-001", "Cash GL Account", AccountType.GL_ACCOUNT, LedgerAccountType.GL_ACCOUNT, new BigDecimal("5000000.0000"), "INR", hq, null, null, "1100", null, tenant, admin);
        Account glLoans = acct("GL-LOAN-001", "Loans GL Account", AccountType.GL_ACCOUNT, LedgerAccountType.GL_ACCOUNT, new BigDecimal("2000000.0000"), "INR", hq, null, null, "1200", null, tenant, admin);
        Account glDep = acct("GL-DEP-001", "Customer Deposits GL Account", AccountType.GL_ACCOUNT, LedgerAccountType.GL_ACCOUNT, new BigDecimal("3000000.0000"), "INR", hq, null, null, "2100", null, tenant, admin);
        acct("INT-SUSP-001", "Internal Suspense Account", AccountType.INTERNAL_ACCOUNT, LedgerAccountType.INTERNAL_ACCOUNT, BigDecimal.ZERO, "INR", hq, null, null, null, null, tenant, admin);
        acct("CLR-001", "Main Clearing Account", AccountType.CLEARING_ACCOUNT, LedgerAccountType.CLEARING_ACCOUNT, BigDecimal.ZERO, "INR", hq, null, null, null, null, tenant, admin);
        acct("SET-001", "Settlement Account", AccountType.SETTLEMENT_ACCOUNT, LedgerAccountType.SETTLEMENT_ACCOUNT, BigDecimal.ZERO, "INR", hq, null, null, null, null, tenant, admin);

        // IBC accounts
        for (Object[] b : new Object[][] {{"HQ001", "Head Office", hq}, {"BR001", "Downtown Branch", br1}, {"BR002", "Uptown Branch", br2}}) {
            acct("IBC-OUT-" + b[0], "IBC Outward - " + b[1], AccountType.CLEARING_ACCOUNT, LedgerAccountType.CLEARING_ACCOUNT, BigDecimal.ZERO, "INR", (Branch) b[2], null, null, "2910", null, tenant, admin);
            acct("IBC-IN-" + b[0], "IBC Inward - " + b[1], AccountType.CLEARING_ACCOUNT, LedgerAccountType.CLEARING_ACCOUNT, BigDecimal.ZERO, "INR", (Branch) b[2], null, null, "2910", null, tenant, admin);
        }
        log.info("  [IBC] 6 Inter-Branch Clearing accounts seeded");

        // Customer accounts
        Account rs = acct("SAV-1001-0001", "Rajesh Kumar - Savings", AccountType.SAVINGS, LedgerAccountType.CUSTOMER_ACCOUNT, new BigDecimal("50000.0000"), "INR", br1, c1, "Rajesh Kumar", "2110", null, tenant, admin);
        Account rc = acct("CUR-1001-0001", "Rajesh Kumar - Current", AccountType.CURRENT, LedgerAccountType.CUSTOMER_ACCOUNT, new BigDecimal("150000.0000"), "INR", br1, c1, "Rajesh Kumar", "2120", null, tenant, admin);
        Account rl = acct("LN-1001-0001", "Rajesh Kumar - Personal Loan", AccountType.LOAN, LedgerAccountType.CUSTOMER_ACCOUNT, new BigDecimal("500000.0000"), "INR", br1, c1, "Rajesh Kumar", "1210", null, tenant, admin);
        Account rf = acct("FD-1001-0001", "Rajesh Kumar - Fixed Deposit", AccountType.FIXED_DEPOSIT, LedgerAccountType.CUSTOMER_ACCOUNT, new BigDecimal("200000.0000"), "INR", br1, c1, "Rajesh Kumar", "2130", null, tenant, admin);
        Account ps = acct("SAV-1002-0001", "Priya Sharma - Savings", AccountType.SAVINGS, LedgerAccountType.CUSTOMER_ACCOUNT, new BigDecimal("75000.0000"), "INR", br1, c2, "Priya Sharma", "2110", null, tenant, admin);
        Account pc = acct("CUR-1002-0001", "Priya Sharma - Current", AccountType.CURRENT, LedgerAccountType.CUSTOMER_ACCOUNT, new BigDecimal("250000.0000"), "INR", br1, c2, "Priya Sharma", "2120", null, tenant, admin);
        Account pf = acct("FD-1002-0001", "Priya Sharma - Fixed Deposit", AccountType.FIXED_DEPOSIT, LedgerAccountType.CUSTOMER_ACCOUNT, new BigDecimal("500000.0000"), "INR", br1, c2, "Priya Sharma", "2130", null, tenant, admin);
        Account as = acct("SAV-1003-0001", "Amit Patel - Savings", AccountType.SAVINGS, LedgerAccountType.CUSTOMER_ACCOUNT, new BigDecimal("10000.0000"), "INR", br2, c3, "Amit Patel", "2110", null, tenant, admin);
        Account su = acct("CUR-1004-0001", "Sneha Reddy - USD Current", AccountType.CURRENT, LedgerAccountType.CUSTOMER_ACCOUNT, new BigDecimal("5000.0000"), "USD", br2, c4, "Sneha Reddy", "2120", null, tenant, admin);

        log.info("  [Accounts] {} accounts created", accountRepository.count());

        // Balances
        for (Object[] ab : new Object[][] {
                {rs, "50000.0000"}, {rc, "150000.0000"}, {rl, "500000.0000"}, {rf, "200000.0000"},
                {ps, "75000.0000"}, {pc, "250000.0000"}, {pf, "500000.0000"},
                {as, "10000.0000"}, {su, "5000.0000"},
                {glCash, "5000000.0000"}, {glLoans, "2000000.0000"}, {glDep, "3000000.0000"}}) {
            bal((Account) ab[0], new BigDecimal((String) ab[1]));
        }
        log.info("  [AccountBalance] {} balance cache records created", accountBalanceRepository.count());
    }

    private Customer cust(String nid, String first, String last, LocalDate dob,
                          String email, String phone, String addr, String kyc,
                          Tenant tenant, User admin) {
        return customerRepository.findByNationalId(nid).orElseGet(() -> {
            Customer c = Customer.builder().nationalId(nid).firstName(first).lastName(last)
                    .dob(dob).email(email).phone(phone).address(addr).kycStatus(kyc)
                    .tenant(tenant).createdBy(admin).build();
            return customerRepository.save(c);
        });
    }

    private Account acct(String num, String name, AccountType type, LedgerAccountType ledger,
                         BigDecimal balance, String ccy, Branch branch, Customer cust,
                         String custName, String glCode, Account parent,
                         Tenant tenant, User admin) {
        return accountRepository.save(Account.builder()
                .accountNumber(num).accountName(name).accountType(type).ledgerAccountType(ledger)
                .tenant(tenant).status(AccountStatus.ACTIVE).balance(balance).currency(ccy)
                .branch(branch).branchCode(branch.getBranchCode())
                .customer(cust).customerName(custName)
                .customerEmail(cust != null ? cust.getEmail() : null)
                .customerPhone(cust != null ? cust.getPhone() : null)
                .glAccountCode(glCode).parentAccount(parent).createdBy(admin).build());
    }

    private void bal(Account account, BigDecimal ledgerBalance) {
        accountBalanceRepository.save(AccountBalance.builder()
                .account(account).ledgerBalance(ledgerBalance).availableBalance(ledgerBalance)
                .holdAmount(BigDecimal.ZERO).actualTotalBalance(ledgerBalance)
                .actualClearedBalance(ledgerBalance).shadowTotalBalance(BigDecimal.ZERO)
                .shadowClearingBalance(BigDecimal.ZERO).inwardClearingBalance(BigDecimal.ZERO)
                .unclearedEffectBalance(BigDecimal.ZERO).lienBalance(BigDecimal.ZERO)
                .chargeHoldBalance(BigDecimal.ZERO).build());
    }
}
