package com.ledgora.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.auth.entity.Role;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.RoleRepository;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.DayStatus;
import com.ledgora.common.enums.RoleName;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test: Role-based access control (RBAC).
 *
 * <p>CBS Rules:
 *
 * <ul>
 *   <li>All 12+ CBS roles exist in the system
 *   <li>Users can be assigned multiple roles
 *   <li>Segregation of duties: Maker and Checker cannot coexist for same user
 *   <li>Risk role exists for fraud monitoring
 *   <li>Compliance role exists for AML/CFT oversight
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoleBasedAccessControlTest {

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    @Order(1)
    @Transactional
    @DisplayName("All CBS roles exist in the database")
    void allCbsRolesExist() {
        for (RoleName roleName : RoleName.values()) {
            assertTrue(
                    roleRepository.existsByName(roleName),
                    "Role " + roleName + " must exist in the database");
        }
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("ROLE_RISK exists for fraud monitoring")
    void riskRoleExists() {
        Optional<Role> riskRole = roleRepository.findByName(RoleName.ROLE_RISK);
        assertTrue(riskRole.isPresent(), "ROLE_RISK must exist for fraud monitoring");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("ROLE_COMPLIANCE_OFFICER exists for AML/CFT")
    void complianceRoleExists() {
        Optional<Role> complianceRole = roleRepository.findByName(RoleName.ROLE_COMPLIANCE_OFFICER);
        assertTrue(
                complianceRole.isPresent(),
                "ROLE_COMPLIANCE_OFFICER must exist for AML/CFT oversight");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("ROLE_SYSTEM exists for STP flows")
    void systemRoleExists() {
        Optional<Role> systemRole = roleRepository.findByName(RoleName.ROLE_SYSTEM);
        assertTrue(systemRole.isPresent(), "ROLE_SYSTEM must exist for STP auto-checker flows");
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("User can be assigned multiple roles")
    void userCanHaveMultipleRoles() {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("T-RBAC-05")
                                .tenantName("RBAC Multi-Role Tenant")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Branch branch =
                branchRepository.save(
                        Branch.builder()
                                .branchCode("B-RBAC-05")
                                .name("RBAC Branch")
                                .isActive(true)
                                .build());

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        Role managerRole = roleRepository.findByName(RoleName.ROLE_MANAGER).orElseThrow();

        User multiRoleUser =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .username("multi-role-user")
                                .password("password")
                                .fullName("Multi Role User")
                                .email("multi@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .roles(Set.of(adminRole, managerRole))
                                .build());

        User retrieved = userRepository.findById(multiRoleUser.getId()).orElseThrow();
        assertTrue(retrieved.getRoles().size() >= 2, "User must have at least 2 roles assigned");
    }

    @Test
    @Order(6)
    @Transactional
    @DisplayName("ROLE_AUDITOR exists for read-only audit access")
    void auditorRoleExists() {
        Optional<Role> auditorRole = roleRepository.findByName(RoleName.ROLE_AUDITOR);
        assertTrue(auditorRole.isPresent(), "ROLE_AUDITOR must exist for read-only audit access");
    }

    @Test
    @Order(7)
    @Transactional
    @DisplayName("ROLE_OPERATIONS exists for operational tasks")
    void operationsRoleExists() {
        Optional<Role> opsRole = roleRepository.findByName(RoleName.ROLE_OPERATIONS);
        assertTrue(opsRole.isPresent(), "ROLE_OPERATIONS must exist for operational tasks");
    }
}
