package com.ledgora.security;

import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * RBI-F10: UserDetailsService with account lockout enforcement.
 *
 * The User entity carries failedLoginAttempts and isLocked fields.
 * This service checks both before allowing authentication:
 *   - isActive must be true
 *   - isLocked must be false
 *   - failedLoginAttempts must be below MAX_FAILED_ATTEMPTS (auto-locks at threshold)
 *
 * Lockout is triggered by AuthenticationFailureListener (increments counter, sets isLocked).
 * Unlock requires admin action via /admin/users/{id}/toggle.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    /** RBI-F10: Maximum failed login attempts before account is locked. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // RBI-F10: Enforce lockout based on failed attempts threshold
        boolean accountNonLocked = !user.getIsLocked();
        if (user.getFailedLoginAttempts() != null
                && user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            accountNonLocked = false;
            log.warn("SECURITY: User {} locked due to {} failed login attempts",
                    username, user.getFailedLoginAttempts());
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getIsActive(),       // enabled
                true,                     // accountNonExpired
                true,                     // credentialsNonExpired
                accountNonLocked,         // accountNonLocked
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .collect(Collectors.toSet())
        );
    }
}
