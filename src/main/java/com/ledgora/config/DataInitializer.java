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
