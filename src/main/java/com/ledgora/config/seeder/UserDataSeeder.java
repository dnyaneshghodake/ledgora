package com.ledgora.config.seeder;

import com.ledgora.auth.entity.Role;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.RoleRepository;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.RoleName;
import com.ledgora.common.enums.TenantScope;
import com.ledgora.tenant.entity.Tenant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 3 — User seeding. Seeds admin, manager, tellers, customers, maker,
 * checker, ops, auditor, superadmin, tenantadmin, branchmgr, SYSTEM_AUTO users.
 */
@Component
public class UserDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(UserDataSeeder.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDataSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Seeds all CBS users. Returns [adminUser, managerUser, teller1User] for downstream seeders.
     */
    public User[] seed(
            Tenant defaultTenant,
            Tenant secondTenant,
            Branch hqBranch,
            Branch branch1,
            Branch branch2) {

        Role adminRole = role(RoleName.ROLE_ADMIN);
        Role managerRole = role(RoleName.ROLE_MANAGER);
        Role tellerRole = role(RoleName.ROLE_TELLER);
        Role customerRole = role(RoleName.ROLE_CUSTOMER);
        Role makerRole = role(RoleName.ROLE_MAKER);
        Role checkerRole = role(RoleName.ROLE_CHECKER);
        Role opsRole = role(RoleName.ROLE_OPERATIONS);
        Role auditorRole = role(RoleName.ROLE_AUDITOR);
        Role superAdminRole = role(RoleName.ROLE_SUPER_ADMIN);
        Role tenantAdminRole = role(RoleName.ROLE_TENANT_ADMIN);
        Role branchMgrRole = role(RoleName.ROLE_BRANCH_MANAGER);
        Role systemRole = role(RoleName.ROLE_SYSTEM);
        Role complianceRole = role(RoleName.ROLE_COMPLIANCE_OFFICER);
        Role riskRole = role(RoleName.ROLE_RISK);

        User adminUser =
                create(
                        "admin",
                        "admin123",
                        "System Administrator",
                        "admin@ledgora.com",
                        "+91-9000000001",
                        hqBranch,
                        Set.of(adminRole),
                        defaultTenant,
                        TenantScope.MULTI);

        User managerUser =
                create(
                        "manager",
                        "manager123",
                        "Branch Manager",
                        "manager@ledgora.com",
                        "+91-9000000002",
                        hqBranch,
                        Set.of(managerRole),
                        defaultTenant,
                        TenantScope.SINGLE);

        User teller1User =
                create(
                        "teller1",
                        "teller123",
                        "Teller One",
                        "teller1@ledgora.com",
                        "+91-9000000003",
                        branch1,
                        Set.of(tellerRole),
                        defaultTenant,
                        TenantScope.SINGLE);

        create(
                "teller2",
                "teller123",
                "Teller Two",
                "teller2@ledgora.com",
                "+91-9000000004",
                branch2,
                Set.of(tellerRole),
                defaultTenant,
                TenantScope.SINGLE);

        create(
                "customer1",
                "cust123",
                "Rajesh Kumar",
                "rajesh.kumar@email.com",
                "+91-9100000001",
                branch1,
                Set.of(customerRole),
                defaultTenant,
                TenantScope.SINGLE);
        create(
                "customer2",
                "cust123",
                "Priya Sharma",
                "priya.sharma@email.com",
                "+91-9100000002",
                branch1,
                Set.of(customerRole),
                defaultTenant,
                TenantScope.SINGLE);
        create(
                "customer3",
                "cust123",
                "Amit Patel",
                "amit.patel@email.com",
                "+91-9100000003",
                branch2,
                Set.of(customerRole),
                defaultTenant,
                TenantScope.SINGLE);
        create(
                "customer4",
                "cust123",
                "Sneha Reddy",
                "sneha.reddy@email.com",
                "+91-9100000004",
                branch2,
                Set.of(customerRole),
                defaultTenant,
                TenantScope.SINGLE);

        create(
                "maker1",
                "maker123",
                "Maker One",
                "maker1@ledgora.com",
                "+91-9000000010",
                branch1,
                Set.of(makerRole),
                defaultTenant,
                TenantScope.SINGLE);
        create(
                "checker1",
                "checker123",
                "Checker One",
                "checker1@ledgora.com",
                "+91-9000000011",
                branch1,
                Set.of(checkerRole),
                defaultTenant,
                TenantScope.SINGLE);

        create(
                "ops1",
                "ops123",
                "Operations Officer",
                "ops1@ledgora.com",
                "+91-9000000012",
                hqBranch,
                Set.of(opsRole),
                defaultTenant,
                TenantScope.SINGLE);
        create(
                "auditor1",
                "auditor123",
                "Auditor One",
                "auditor1@ledgora.com",
                "+91-9000000013",
                hqBranch,
                Set.of(auditorRole),
                defaultTenant,
                TenantScope.SINGLE);

        create(
                "superadmin",
                "super123",
                "Super Administrator",
                "superadmin@ledgora.com",
                "+91-9000000020",
                hqBranch,
                Set.of(superAdminRole, adminRole),
                defaultTenant,
                TenantScope.MULTI);
        create(
                "tenantadmin",
                "tenant123",
                "Tenant Administrator",
                "tenantadmin@ledgora.com",
                "+91-9000000021",
                hqBranch,
                Set.of(tenantAdminRole),
                defaultTenant,
                TenantScope.MULTI);
        create(
                "branchmgr1",
                "branch123",
                "Branch Manager One",
                "branchmgr1@ledgora.com",
                "+91-9000000022",
                branch1,
                Set.of(branchMgrRole),
                defaultTenant,
                TenantScope.SINGLE);

        // Compliance Officer — RBI KYC Master Direction: designated CO for AML/CFT
        create(
                "compliance1",
                "compliance123",
                "Compliance Officer",
                "compliance1@ledgora.com",
                "+91-9000000014",
                hqBranch,
                Set.of(complianceRole),
                defaultTenant,
                TenantScope.SINGLE);

        // Risk Officer — RBI Basel III / ICAAP: view fraud flags, velocity alerts, risk dashboards
        create(
                "risk1",
                "risk123",
                "Risk Officer",
                "risk1@ledgora.com",
                "+91-9000000015",
                hqBranch,
                Set.of(riskRole),
                defaultTenant,
                TenantScope.SINGLE);

        // ATM System user — channel-level system identity for ATM transactions
        Role atmRole = role(RoleName.ROLE_ATM_SYSTEM);
        create(
                "ATM_SYSTEM",
                "ATM_SYSTEM_NO_LOGIN_" + java.util.UUID.randomUUID(),
                "ATM System Channel",
                "atm@ledgora.internal",
                "+00-0000000001",
                hqBranch,
                Set.of(atmRole),
                defaultTenant,
                TenantScope.MULTI);

        create(
                "teller3",
                "teller123",
                "Teller Three (Partner Bank)",
                "teller3@ledgora.com",
                "+91-9000000030",
                branch2,
                Set.of(tellerRole),
                secondTenant,
                TenantScope.SINGLE);

        create(
                "SYSTEM_AUTO",
                "SYSTEM_AUTO_NO_LOGIN_" + java.util.UUID.randomUUID(),
                "System Auto-Authorization",
                "system@ledgora.internal",
                "+00-0000000000",
                hqBranch,
                Set.of(systemRole),
                defaultTenant,
                TenantScope.MULTI);

        log.info("  [Users] 20 users ready (Tenant 1 + global)");
        return new User[] {adminUser, managerUser, teller1User};
    }

    /**
     * Seeds realistic users for additional tenants (T2–T5).
     * Each tenant gets: admin, manager, teller, maker, checker, auditor (minimum CBS staff).
     */
    public void seedForAdditionalTenants(
            Tenant t2, Tenant t3, Tenant t4, Tenant t5,
            com.ledgora.branch.repository.BranchRepository branchRepo) {

        Role adminRole = role(RoleName.ROLE_ADMIN);
        Role managerRole = role(RoleName.ROLE_MANAGER);
        Role tellerRole = role(RoleName.ROLE_TELLER);
        Role makerRole = role(RoleName.ROLE_MAKER);
        Role checkerRole = role(RoleName.ROLE_CHECKER);
        Role auditorRole = role(RoleName.ROLE_AUDITOR);
        Role tenantAdminRole = role(RoleName.ROLE_TENANT_ADMIN);
        Role opsRole = role(RoleName.ROLE_OPERATIONS);
        Role riskRole = role(RoleName.ROLE_RISK);
        Role branchMgrRole = role(RoleName.ROLE_BRANCH_MANAGER);

        // ── Tenant 2: Partner Bank (Pune) ──
        Branch t2Hq = branchRepo.findByBranchCode("T2-HQ01").orElse(null);
        Branch t2Br1 = branchRepo.findByBranchCode("T2-BR01").orElse(null);
        if (t2Hq != null && t2 != null) {
            create("t2_admin", "admin123", "Suresh Mehta", "suresh@partner-bank.in",
                    "+91-9200000001", t2Hq, Set.of(adminRole, tenantAdminRole), t2, TenantScope.SINGLE);
            create("t2_manager", "mgr123", "Kavita Joshi", "kavita@partner-bank.in",
                    "+91-9200000002", t2Hq, Set.of(managerRole, checkerRole), t2, TenantScope.SINGLE);
            create("t2_teller", "teller123", "Rahul Verma", "rahul@partner-bank.in",
                    "+91-9200000003", t2Br1 != null ? t2Br1 : t2Hq, Set.of(tellerRole, makerRole), t2, TenantScope.SINGLE);
            create("t2_maker", "maker123", "Anita Desai", "anita@partner-bank.in",
                    "+91-9200000004", t2Br1 != null ? t2Br1 : t2Hq, Set.of(makerRole), t2, TenantScope.SINGLE);
            create("t2_checker", "checker123", "Manoj Tiwari", "manoj@partner-bank.in",
                    "+91-9200000005", t2Hq, Set.of(checkerRole), t2, TenantScope.SINGLE);
            create("t2_auditor", "auditor123", "Pooja Gupta", "pooja@partner-bank.in",
                    "+91-9200000006", t2Hq, Set.of(auditorRole), t2, TenantScope.SINGLE);
            log.info("  [Users] T2: 6 users ready (Partner Bank)");
        }

        // ── Tenant 3: Sahyadri UCB (Pune — small staff) ──
        Branch t3Hq = branchRepo.findByBranchCode("T3-HQ01").orElse(null);
        if (t3Hq != null && t3 != null) {
            create("t3_admin", "admin123", "Prakash Kulkarni", "prakash@sahyadri-ucb.in",
                    "+91-9300000001", t3Hq, Set.of(adminRole, tenantAdminRole), t3, TenantScope.SINGLE);
            create("t3_manager", "mgr123", "Sunanda Patil", "sunanda@sahyadri-ucb.in",
                    "+91-9300000002", t3Hq, Set.of(managerRole, checkerRole), t3, TenantScope.SINGLE);
            create("t3_teller", "teller123", "Ganesh Deshpande", "ganesh@sahyadri-ucb.in",
                    "+91-9300000003", t3Hq, Set.of(tellerRole, makerRole), t3, TenantScope.SINGLE);
            create("t3_auditor", "auditor123", "Lata Jog", "lata@sahyadri-ucb.in",
                    "+91-9300000004", t3Hq, Set.of(auditorRole), t3, TenantScope.SINGLE);
            log.info("  [Users] T3: 4 users ready (Sahyadri UCB — small staff)");
        }

        // ── Tenant 4: Maharashtra Gramin Bank (Nashik — rural banking staff) ──
        Branch t4Hq = branchRepo.findByBranchCode("T4-HQ01").orElse(null);
        Branch t4Br1 = branchRepo.findByBranchCode("T4-BR01").orElse(null);
        Branch t4Br2 = branchRepo.findByBranchCode("T4-BR02").orElse(null);
        if (t4Hq != null && t4 != null) {
            create("t4_admin", "admin123", "Ramchandra Pawar", "ramchandra@maha-gramin.in",
                    "+91-9400000001", t4Hq, Set.of(adminRole, tenantAdminRole), t4, TenantScope.SINGLE);
            create("t4_manager", "mgr123", "Savita Jadhav", "savita@maha-gramin.in",
                    "+91-9400000002", t4Hq, Set.of(managerRole, checkerRole), t4, TenantScope.SINGLE);
            create("t4_teller1", "teller123", "Bharat Gaikwad", "bharat@maha-gramin.in",
                    "+91-9400000003", t4Br1 != null ? t4Br1 : t4Hq, Set.of(tellerRole, makerRole), t4, TenantScope.SINGLE);
            create("t4_teller2", "teller123", "Mangal Kale", "mangal@maha-gramin.in",
                    "+91-9400000004", t4Br2 != null ? t4Br2 : t4Hq, Set.of(tellerRole, makerRole), t4, TenantScope.SINGLE);
            create("t4_checker", "checker123", "Vinod Shinde", "vinod@maha-gramin.in",
                    "+91-9400000005", t4Hq, Set.of(checkerRole), t4, TenantScope.SINGLE);
            create("t4_ops", "ops123", "Nirmala Bhosle", "nirmala@maha-gramin.in",
                    "+91-9400000006", t4Hq, Set.of(opsRole), t4, TenantScope.SINGLE);
            create("t4_branchmgr", "branch123", "Ashok More", "ashok@maha-gramin.in",
                    "+91-9400000007", t4Br1 != null ? t4Br1 : t4Hq, Set.of(branchMgrRole, checkerRole), t4, TenantScope.SINGLE);
            log.info("  [Users] T4: 7 users ready (Gramin Bank — rural staff)");
        }

        // ── Tenant 5: Finserv Capital NBFC (metro — lending-focused staff) ──
        Branch t5Hq = branchRepo.findByBranchCode("T5-HQ01").orElse(null);
        Branch t5Br1 = branchRepo.findByBranchCode("T5-BR01").orElse(null);
        if (t5Hq != null && t5 != null) {
            create("t5_admin", "admin123", "Vikram Malhotra", "vikram@finserv-capital.in",
                    "+91-9500000001", t5Hq, Set.of(adminRole, tenantAdminRole), t5, TenantScope.SINGLE);
            create("t5_manager", "mgr123", "Deepika Nair", "deepika@finserv-capital.in",
                    "+91-9500000002", t5Hq, Set.of(managerRole, checkerRole), t5, TenantScope.SINGLE);
            create("t5_maker", "maker123", "Arjun Reddy", "arjun@finserv-capital.in",
                    "+91-9500000003", t5Br1 != null ? t5Br1 : t5Hq, Set.of(makerRole), t5, TenantScope.SINGLE);
            create("t5_checker", "checker123", "Priya Menon", "priya.m@finserv-capital.in",
                    "+91-9500000004", t5Hq, Set.of(checkerRole), t5, TenantScope.SINGLE);
            create("t5_risk", "risk123", "Sanjay Kapoor", "sanjay@finserv-capital.in",
                    "+91-9500000005", t5Hq, Set.of(riskRole), t5, TenantScope.SINGLE);
            create("t5_ops", "ops123", "Meera Iyer", "meera@finserv-capital.in",
                    "+91-9500000006", t5Hq, Set.of(opsRole), t5, TenantScope.SINGLE);
            log.info("  [Users] T5: 6 users ready (Finserv NBFC — lending staff)");
        }
    }

    private Role role(RoleName name) {
        return roleRepository
                .findByName(name)
                .orElseThrow(() -> new RuntimeException(name + " not found"));
    }

    private User create(
            String username,
            String rawPassword,
            String fullName,
            String email,
            String phone,
            Branch branch,
            Set<Role> roles,
            Tenant tenant,
            TenantScope tenantScope) {
        return userRepository
                .findByUsername(username)
                .orElseGet(
                        () -> {
                            User user =
                                    User.builder()
                                            .username(username)
                                            .password(passwordEncoder.encode(rawPassword))
                                            .fullName(fullName)
                                            .email(email)
                                            .phone(phone)
                                            .branch(branch)
                                            .branchCode(branch.getBranchCode())
                                            .tenant(tenant)
                                            .tenantScope(tenantScope)
                                            .isActive(true)
                                            .isLocked(false)
                                            .failedLoginAttempts(0)
                                            .roles(roles)
                                            .build();
                            return userRepository.save(user);
                        });
    }
}
