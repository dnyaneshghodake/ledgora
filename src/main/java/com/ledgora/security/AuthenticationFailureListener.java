package com.ledgora.security;

import com.ledgora.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI-F10: Listener for authentication events to enforce brute-force protection.
 *
 * <p>On failure: increments failedLoginAttempts; auto-locks at MAX_FAILED_ATTEMPTS. On success:
 * resets failedLoginAttempts to 0.
 */
@Component
public class AuthenticationFailureListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFailureListener.class);

    private final UserRepository userRepository;

    public AuthenticationFailureListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener
    @Transactional
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        userRepository
                .findByUsername(username)
                .ifPresent(
                        user -> {
                            int attempts =
                                    (user.getFailedLoginAttempts() != null
                                                    ? user.getFailedLoginAttempts()
                                                    : 0)
                                            + 1;
                            user.setFailedLoginAttempts(attempts);

                            if (attempts >= CustomUserDetailsService.MAX_FAILED_ATTEMPTS
                                    && !user.getIsLocked()) {
                                user.setIsLocked(true);
                                log.warn(
                                        "SECURITY: Account {} LOCKED after {} failed login attempts",
                                        username,
                                        attempts);
                            } else {
                                log.info(
                                        "SECURITY: Failed login attempt {} of {} for user {}",
                                        attempts,
                                        CustomUserDetailsService.MAX_FAILED_ATTEMPTS,
                                        username);
                            }
                            userRepository.save(user);
                        });
    }

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        userRepository
                .findByUsername(username)
                .ifPresent(
                        user -> {
                            if (user.getFailedLoginAttempts() != null
                                    && user.getFailedLoginAttempts() > 0) {
                                user.setFailedLoginAttempts(0);
                                userRepository.save(user);
                                log.info(
                                        "SECURITY: Failed login counter reset for user {}",
                                        username);
                            }
                        });
    }
}
