package com.ledgora.config;

import com.ledgora.auth.entity.User;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.config.seeder.*;
import com.ledgora.tenant.entity.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Slim orchestrator for CBS data seeding. Delegates to module-wise seeders in dependency order.
 * Each seeder is idempotent (checks before creating).
 *
 * <p>Execution order: 0. Tenants → 1. Roles → 2. Branches → 3. Users → 4. GL Hierarchy → 5.
 * Business Date → 6. Customers & Accounts → 7. Transactions & Ledger → 8. Exchange Rates → 9.
 * Idempotency Keys → 10. CBS CustomerMaster + Tax → 11. Teller Operations Master Data
 *
 * <p>Set {@code ledgora.seeder.enabled=false} in application.properties (or via environment
 * variable) to skip seeding on UAT/PROD deployments where real data already exists.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /**
     * Controls whether the data seeder runs on startup. Default: true (for local/dev). Set to false
     * for UAT/PROD. Override via environment variable: LEDGORA_SEEDER_ENABLED=false
     */
    @Value("${ledgora.seeder.enabled:true}")
    private boolean seederEnabled;

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
    private final TellerDataSeeder tellerDataSeeder;
    private final BranchRepository branchRepository;

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
            CbsCustomerSeeder cbsCustomerSeeder,
            TellerDataSeeder tellerDataSeeder,
            BranchRepository branchRepository) {
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
        this.tellerDataSeeder = tellerDataSeeder;
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seederEnabled) {
            log.info("═══════════════════════════════════════════════════════════");
            log.info("  Ledgora DataInitializer — SKIPPED (ledgora.seeder.enabled=false)");
            log.info("  This is correct for UAT/PROD deployments with real data.");
            log.info("═══════════════════════════════════════════════════════════");
            return;
        }
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Ledgora DataInitializer — seeding reference data ...");
        log.info("═══════════════════════════════════════════════════════════");

        // 0. Tenants (5 realistic Indian financial institutions)
        Tenant defaultTenant = tenantSeeder.seedDefaultTenant();
        Tenant secondTenant = tenantSeeder.seedSecondTenant();
        Tenant ucbTenant = tenantSeeder.seedCooperativeBank();
        Tenant rrbTenant = tenantSeeder.seedRuralBank();
        Tenant nbfcTenant = tenantSeeder.seedNbfc();
        log.info("  [Tenants] 5 tenants ready (Main Bank, Partner Bank, UCB, RRB, NBFC)");

        // 1. Roles
        roleSeeder.seed();

        // 2. Branches (per tenant — realistic branch networks)
        Branch[] branches = branchSeeder.seed(defaultTenant);
        Branch hq = branches[0];
        Branch br1 = branches[1];
        Branch br2 = branches[2];
        branchSeeder.seedForTenant2(secondTenant);
        branchSeeder.seedForTenant3(ucbTenant);
        branchSeeder.seedForTenant4(rrbTenant);
        branchSeeder.seedForTenant5(nbfcTenant);

        // 3. Users (Tenant 1 + global users)
        User[] users = userSeeder.seed(defaultTenant, secondTenant, hq, br1, br2);
        User adminUser = users[0];
        User teller1User = users[2];

        // 3b. Users for additional tenants (T2–T5)
        userSeeder.seedForAdditionalTenants(
                secondTenant, ucbTenant, rrbTenant, nbfcTenant, branchRepository);

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

        // 10. CBS CustomerMaster + Tax Profiles (all tenants)
        cbsCustomerSeeder.seed(defaultTenant);
        cbsCustomerSeeder.seedForAdditionalTenants(
                secondTenant, ucbTenant, rrbTenant, nbfcTenant);

        // 11. Teller Operations Master Data (denominations, vaults, teller masters)
        tellerDataSeeder.seed(defaultTenant, hq, br1, br2);

        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Ledgora DataInitializer — seeding complete.");
        log.info("═══════════════════════════════════════════════════════════");
    }
}
