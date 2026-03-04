package com.ledgora.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Password Encoder Utility Component
 *
 * <p>Handles password encryption and validation using BCrypt algorithm. Provides secure password
 * hashing and comparison functionality.
 */
@Component
public class PasswordEncoderUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Encrypt/Hash a plain text password
     *
     * @param plainPassword Plain text password
     * @return Encrypted password hash
     */
    public String encryptPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return encoder.encode(plainPassword);
    }

    /**
     * Verify if a plain text password matches the encrypted password
     *
     * @param plainPassword Plain text password to verify
     * @param encryptedPassword Encrypted password hash to compare against
     * @return true if passwords match, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String encryptedPassword) {
        if (plainPassword == null || encryptedPassword == null) {
            return false;
        }
        return encoder.matches(plainPassword, encryptedPassword);
    }

    /**
     * Generate default encrypted password for new users Default password: changeme@123
     *
     * @return Encrypted default password
     */
    public String generateDefaultPassword() {
        return encryptPassword("changeme@123");
    }
}
