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
 * Slim orchestrator for CBS data seeding. Delegates to module-wise seeders in dependency order.
 * Each seeder is idempotent (checks before creating).
 *
 * <p>Execution order: 0. Tenants → 1. Roles → 2. Branches → 3. Users → 4. GL Hierarchy → 5.
 * Business Date → 6. Customers & Accounts → 7. Transactions & Ledger → 8. Exchange Rates → 9.
 * Idempotency Keys → 10. CBS CustomerMaster + Tax
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
