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

        log.info("  [Users] 19 users ready");
        return new User[] {adminUser, managerUser, teller1User};
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
