package com.ledgora.config;

import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.auth.entity.Role;
import com.ledgora.auth.entity.User;
import com.ledgora.common.enums.GLAccountType;
import com.ledgora.common.enums.RoleName;
import com.ledgora.common.entity.SystemDate;
import com.ledgora.common.enums.BusinessDateStatus;
import com.ledgora.branch.entity.Branch;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.auth.repository.RoleRepository;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.repository.SystemDateRepository;
import com.ledgora.branch.repository.BranchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final GeneralLedgerRepository glRepository;
    private final SystemDateRepository systemDateRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           GeneralLedgerRepository glRepository, SystemDateRepository systemDateRepository,
                           BranchRepository branchRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.glRepository = glRepository;
        this.systemDateRepository = systemDateRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        initBranches();
        initAdminUser();
        initGLHierarchy();
        initBusinessDate();
    }

    private void initRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = Role.builder()
                        .name(roleName)
                        .description(roleName.name().replace("ROLE_", "") + " role")
                        .build();
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        }
    }

    private void initBranches() {
        if (branchRepository.count() == 0) {
            branchRepository.save(Branch.builder()
                    .branchCode("HQ001").name("Head Office").address("Main Street, City Center").isActive(true).build());
            branchRepository.save(Branch.builder()
                    .branchCode("BR001").name("Branch 1").address("1st Avenue, Downtown").isActive(true).build());
            branchRepository.save(Branch.builder()
                    .branchCode("BR002").name("Branch 2").address("2nd Avenue, Uptown").isActive(true).build());
            log.info("Initialized default branches");
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));

            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .email("admin@ledgora.com")
                    .branchCode("HQ001")
                    .isActive(true)
                    .isLocked(false)
                    .failedLoginAttempts(0)
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(admin);
            log.info("Created default admin user (username: admin, password: admin123)");
        }
    }

    private void initGLHierarchy() {
        if (glRepository.count() == 0) {
            // Root GL accounts - Chart of Accounts
            GeneralLedger assets = createGL("1000", "Assets", "Total Assets", GLAccountType.ASSET, null, 0, "DEBIT");
            GeneralLedger liabilities = createGL("2000", "Liabilities", "Total Liabilities", GLAccountType.LIABILITY, null, 0, "CREDIT");
            GeneralLedger equity = createGL("3000", "Equity", "Total Equity", GLAccountType.EQUITY, null, 0, "CREDIT");
            GeneralLedger revenue = createGL("4000", "Revenue", "Total Revenue", GLAccountType.REVENUE, null, 0, "CREDIT");
            GeneralLedger expenses = createGL("5000", "Expenses", "Total Expenses", GLAccountType.EXPENSE, null, 0, "DEBIT");

            // Asset sub-accounts
            createGL("1100", "Cash and Cash Equivalents", "Cash holdings", GLAccountType.ASSET, assets, 1, "DEBIT");
            createGL("1200", "Loans and Advances", "Customer loans", GLAccountType.ASSET, assets, 1, "DEBIT");
            createGL("1300", "Fixed Assets", "Property and equipment", GLAccountType.ASSET, assets, 1, "DEBIT");
            createGL("1400", "Customer Deposits Receivable", "Deposits receivable", GLAccountType.ASSET, assets, 1, "DEBIT");

            // Liability sub-accounts
            createGL("2100", "Customer Deposits", "Savings and current deposits", GLAccountType.LIABILITY, liabilities, 1, "CREDIT");
            createGL("2200", "Borrowings", "Bank borrowings", GLAccountType.LIABILITY, liabilities, 1, "CREDIT");
            createGL("2300", "Payables", "Accounts payable", GLAccountType.LIABILITY, liabilities, 1, "CREDIT");

            // Revenue sub-accounts
            createGL("4100", "Interest Income", "Interest from loans", GLAccountType.REVENUE, revenue, 1, "CREDIT");
            createGL("4200", "Fee Income", "Service fees", GLAccountType.REVENUE, revenue, 1, "CREDIT");
            createGL("4300", "Other Income", "Miscellaneous income", GLAccountType.REVENUE, revenue, 1, "CREDIT");

            // Expense sub-accounts
            createGL("5100", "Interest Expense", "Interest on deposits", GLAccountType.EXPENSE, expenses, 1, "DEBIT");
            createGL("5200", "Operating Expenses", "General operations", GLAccountType.EXPENSE, expenses, 1, "DEBIT");
            createGL("5300", "Staff Expenses", "Salaries and benefits", GLAccountType.EXPENSE, expenses, 1, "DEBIT");

            log.info("Initialized GL hierarchy with Chart of Accounts");
        }
    }

    private void initBusinessDate() {
        if (systemDateRepository.count() == 0) {
            SystemDate sd = SystemDate.builder()
                    .businessDate(LocalDate.now())
                    .status(BusinessDateStatus.OPEN)
                    .build();
            systemDateRepository.save(sd);
            log.info("Initialized business date: {}", sd.getBusinessDate());
        }
    }

    private GeneralLedger createGL(String code, String name, String desc, GLAccountType type,
                                   GeneralLedger parent, int level, String normalBalance) {
        GeneralLedger gl = GeneralLedger.builder()
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
}
