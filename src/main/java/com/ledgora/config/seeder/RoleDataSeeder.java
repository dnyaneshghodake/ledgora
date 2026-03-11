package com.ledgora.config.seeder;

import com.ledgora.auth.entity.Role;
import com.ledgora.auth.repository.RoleRepository;
import com.ledgora.common.enums.RoleName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 1 — Role seeding. Seeds all CBS roles (ADMIN, MANAGER, TELLER, CUSTOMER,
 * MAKER, CHECKER, etc.).
 */
@Component
public class RoleDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(RoleDataSeeder.class);
    private final RoleRepository roleRepository;

    public RoleDataSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void seed() {
        for (RoleName roleName : RoleName.values()) {
            if (!roleRepository.existsByName(roleName)) {
                Role role =
                        Role.builder()
                                .name(roleName)
                                .description(roleName.name().replace("ROLE_", "") + " role")
                                .build();
                roleRepository.save(role);
                log.info("  [Roles] Created role: {}", roleName);
            }
        }
    }
}
