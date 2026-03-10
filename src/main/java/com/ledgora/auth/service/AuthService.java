package com.ledgora.auth.service;

import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.dto.RegisterRequest;
import com.ledgora.auth.entity.Role;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.RoleRepository;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.RoleName;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        RoleName resolvedRoleName = RoleName.ROLE_CUSTOMER;
        if (request.getRole() != null) {
            try {
                resolvedRoleName = RoleName.valueOf("ROLE_" + request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                resolvedRoleName = RoleName.ROLE_CUSTOMER;
            }
        }
        final RoleName roleName = resolvedRoleName;

        Role role =
                roleRepository
                        .findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        User user =
                User.builder()
                        .username(request.getUsername())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .fullName(request.getFullName())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .branchCode(request.getBranchCode())
                        .isActive(true)
                        .isLocked(false)
                        .failedLoginAttempts(0)
                        .roles(Set.of(role))
                        .build();

        User savedUser = userRepository.save(user);
        log.info("User registered: {}", savedUser.getUsername());
        return savedUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
