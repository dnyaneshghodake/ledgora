package com.ledgora.service;

import com.ledgora.common.PasswordEncoderUtil;
import com.ledgora.dto.PasswordDTO;
import com.ledgora.model.UserMaster;
import com.ledgora.repository.UserMasterRepository;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Class for UserMaster Entity
 *
 * <p>Provides business logic and operations for User Master management. Handles CRUD operations,
 * validation, and business rules. Automatically creates default encrypted password for new users.
 */
@Service
@AllArgsConstructor
@Slf4j
public class UserMasterService {

    private final UserMasterRepository userMasterRepository;
    private final PasswordService passwordService;
    private final PasswordEncoderUtil passwordEncoderUtil;

    // ========== CREATE Operations ==========

    /**
     * Create a new user
     *
     * @param userMaster User object to create
     * @return Created user
     */
    @Transactional
    public UserMaster createUser(UserMaster userMaster) {
        log.info("Creating new user with code: {}", userMaster.getUsrCode1());

        // Validation can be added here
        if (userMaster.getUsrCode1() == null || userMaster.getUsrCode1().isEmpty()) {
            throw new IllegalArgumentException("User Code 1 cannot be null or empty");
        }

        // Save user first
        UserMaster savedUser = userMasterRepository.save(userMaster);
        log.info("User created successfully with code: {}", savedUser.getUsrCode1());

        // Create default password for the new user
        try {
            String encryptedPassword = passwordEncoderUtil.generateDefaultPassword();
            PasswordDTO passwordDTO =
                    PasswordDTO.builder()
                            .userCode(savedUser.getUsrCode1())
                            .password(encryptedPassword)
                            .build();

            passwordService.createPassword(passwordDTO);
            log.info("Default password created for user: {}", savedUser.getUsrCode1());
        } catch (Exception e) {
            log.error("Error creating default password for user: {}", savedUser.getUsrCode1(), e);
            // Continue even if password creation fails, user is already created
        }

        return savedUser;
    }

    // ========== READ Operations ==========

    /**
     * Get user by primary key (usrCode1)
     *
     * @param usrCode1 Primary key
     * @return User if found
     */
    public Optional<UserMaster> getUserByCode1(String usrCode1) {
        log.debug("Fetching user with code1: {}", usrCode1);
        return userMasterRepository.findById(usrCode1);
    }

    /**
     * Get user by alternative key (usrCode2)
     *
     * @param usrCode2 Alternative key
     * @return User if found
     */
    public Optional<UserMaster> getUserByCode2(Integer usrCode2) {
        log.debug("Fetching user with code2: {}", usrCode2);
        return userMasterRepository.findByUsrCode2(usrCode2);
    }

    /**
     * Get user by alternative key (usrCode3)
     *
     * @param usrCode3 Alternative key
     * @return User if found
     */
    public Optional<UserMaster> getUserByCode3(String usrCode3) {
        log.debug("Fetching user with code3: {}", usrCode3);
        return userMasterRepository.findByUsrCode3(usrCode3);
    }

    /**
     * Get user by email ID
     *
     * @param emailId Email ID
     * @return User if found
     */
    public Optional<UserMaster> getUserByEmail(String emailId) {
        log.debug("Fetching user with email: {}", emailId);
        return userMasterRepository.findByEmailIdIgnoreCase(emailId);
    }

    /**
     * Get all users
     *
     * @return List of all users
     */
    public List<UserMaster> getAllUsers() {
        log.debug("Fetching all users");
        return userMasterRepository.findAll();
    }

    /**
     * Get all active users
     *
     * @return List of active users
     */
    public List<UserMaster> getAllActiveUsers() {
        log.debug("Fetching all active users");
        return userMasterRepository.findByIsActive(1);
    }

    // ========== FIND by Branch & Group ==========

    /**
     * Get all users in a specific branch
     *
     * @param brCode Branch code
     * @return List of users in branch
     */
    public List<UserMaster> getUsersByBranch(Integer brCode) {
        log.debug("Fetching users for branch: {}", brCode);
        return userMasterRepository.findByUsrBrCode(brCode);
    }

    /**
     * Get all users in a specific group
     *
     * @param grpCd Group code
     * @return List of users in group
     */
    public List<UserMaster> getUsersByGroup(Integer grpCd) {
        log.debug("Fetching users for group: {}", grpCd);
        return userMasterRepository.findByGrpCd(grpCd);
    }

    /**
     * Get active users in a specific branch
     *
     * @param brCode Branch code
     * @return List of active users in branch
     */
    public List<UserMaster> getActiveUsersByBranch(Integer brCode) {
        log.debug("Fetching active users for branch: {}", brCode);
        return userMasterRepository.findActiveUsersByBranch(brCode, 1);
    }

    /**
     * Get users by branch and group
     *
     * @param brCode Branch code
     * @param grpCd Group code
     * @return List of users
     */
    public List<UserMaster> getUsersByBranchAndGroup(Integer brCode, Integer grpCd) {
        log.debug("Fetching users for branch: {} and group: {}", brCode, grpCd);
        return userMasterRepository.findByUsrBrCodeAndGrpCd(brCode, grpCd);
    }

    // ========== FIND by Name ==========

    /**
     * Search users by name
     *
     * @param usrName User name (partial match)
     * @return List of matching users
     */
    public List<UserMaster> searchUsersByName(String usrName) {
        log.debug("Searching users by name: {}", usrName);
        return userMasterRepository.findByUsrNameContainingIgnoreCase(usrName);
    }

    /**
     * Search users by short name
     *
     * @param shortName Short name (partial match)
     * @return List of matching users
     */
    public List<UserMaster> searchUsersByShortName(String shortName) {
        log.debug("Searching users by short name: {}", shortName);
        return userMasterRepository.findByUsrShortNameContainingIgnoreCase(shortName);
    }

    // ========== FIND by Access Control ==========

    /**
     * Get all users with multi-branch access
     *
     * @return List of users with multi-branch access
     */
    public List<UserMaster> getUsersWithMultiBranchAccess() {
        log.debug("Fetching users with multi-branch access");
        return userMasterRepository.findByMultiBrAccess("Y");
    }

    /**
     * Get all users who can login from other branches
     *
     * @return List of users
     */
    public List<UserMaster> getUsersWithOtherBranchLogin() {
        log.debug("Fetching users with other branch login access");
        return userMasterRepository.findByLgnFrmOthBr("Y");
    }

    /**
     * Get users with screen lock enabled
     *
     * @return List of users with screen lock
     */
    public List<UserMaster> getUsersWithScreenLock() {
        log.debug("Fetching users with screen lock enabled");
        return userMasterRepository.findUsersWithScreenLockEnabled();
    }

    /**
     * Get users with auto logout enabled
     *
     * @return List of users with auto logout
     */
    public List<UserMaster> getUsersWithAutoLogout() {
        log.debug("Fetching users with auto logout enabled");
        return userMasterRepository.findUsersWithAutoLogoutEnabled();
    }

    /**
     * Get users with forced password change
     *
     * @return List of users
     */
    public List<UserMaster> getUsersWithForcedPasswordChange() {
        log.debug("Fetching users with forced password change");
        return userMasterRepository.findUsersWithForcedPasswordChange();
    }

    /**
     * Get users with bad login attempts
     *
     * @return List of users
     */
    public List<UserMaster> getUsersWithBadLoginAttempts() {
        log.debug("Fetching users with bad login attempts");
        return userMasterRepository.findUsersWithBadLoginAttempts();
    }

    // ========== SEARCH/FILTER Operations ==========

    /**
     * Search users by multiple criteria
     *
     * @param usrName User name (optional)
     * @param brCode Branch code (optional)
     * @param grpCd Group code (optional)
     * @param isActive Active status (optional)
     * @return List of matching users
     */
    public List<UserMaster> searchUsers(
            String usrName, Integer brCode, Integer grpCd, Integer isActive) {
        log.debug(
                "Searching users with filters - name: {}, branch: {}, group: {}, active: {}",
                usrName,
                brCode,
                grpCd,
                isActive);
        return userMasterRepository.searchUsers(usrName, brCode, grpCd, isActive);
    }

    // ========== UPDATE Operations ==========

    /**
     * Update an existing user
     *
     * @param userMaster Updated user object
     * @return Updated user
     */
    @Transactional
    public UserMaster updateUser(UserMaster userMaster) {
        log.info("Updating user with code: {}", userMaster.getUsrCode1());

        if (!userMasterRepository.existsById(userMaster.getUsrCode1())) {
            throw new IllegalArgumentException(
                    "User not found with code: " + userMaster.getUsrCode1());
        }

        return userMasterRepository.save(userMaster);
    }

    /**
     * Update user status
     *
     * @param usrCode1 User code
     * @param status New status
     * @return Updated user
     */
    @Transactional
    public UserMaster updateUserStatus(String usrCode1, Integer status) {
        log.info("Updating status for user: {} to {}", usrCode1, status);

        Optional<UserMaster> userOptional = userMasterRepository.findById(usrCode1);
        if (userOptional.isPresent()) {
            UserMaster user = userOptional.get();
            user.setStatus(status);
            return userMasterRepository.save(user);
        }

        throw new IllegalArgumentException("User not found with code: " + usrCode1);
    }

    /**
     * Activate/Deactivate user
     *
     * @param usrCode1 User code
     * @param isActive Active status (1 for active, 0 for inactive)
     * @return Updated user
     */
    @Transactional
    public UserMaster updateUserActiveStatus(String usrCode1, Integer isActive) {
        log.info("Updating active status for user: {} to {}", usrCode1, isActive);

        Optional<UserMaster> userOptional = userMasterRepository.findById(usrCode1);
        if (userOptional.isPresent()) {
            UserMaster user = userOptional.get();
            user.setIsActive(isActive);
            return userMasterRepository.save(user);
        }

        throw new IllegalArgumentException("User not found with code: " + usrCode1);
    }

    // ========== DELETE Operations ==========

    /**
     * Delete user by primary key
     *
     * @param usrCode1 User code to delete
     */
    @Transactional
    public void deleteUser(String usrCode1) {
        log.info("Deleting user with code: {}", usrCode1);

        // Delete associated password record first
        try {
            passwordService.deletePassword(usrCode1);
            log.info("Password record deleted for user: {}", usrCode1);
        } catch (Exception e) {
            log.warn("Error deleting password record for user: {}", usrCode1, e);
        }

        userMasterRepository.deleteById(usrCode1);
    }

    // ========== PASSWORD Management ==========

    /**
     * Change password for a user
     *
     * @param usrCode1 User code
     * @param newPassword New plain text password
     * @return true if password changed successfully
     */
    @Transactional
    public boolean changePassword(String usrCode1, String newPassword) {
        log.info("Changing password for user: {}", usrCode1);

        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("New password cannot be null or empty");
        }

        // Encrypt the new password
        String encryptedPassword = passwordEncoderUtil.encryptPassword(newPassword);
        PasswordDTO passwordDTO =
                PasswordDTO.builder().userCode(usrCode1).password(encryptedPassword).build();

        Optional<PasswordDTO> updated = passwordService.updatePassword(usrCode1, passwordDTO);

        if (updated.isPresent()) {
            log.info("Password changed successfully for user: {}", usrCode1);
            return true;
        }

        log.warn("Password change failed - User not found: {}", usrCode1);
        return false;
    }

    /**
     * Verify user password
     *
     * @param usrCode1 User code
     * @param plainPassword Plain text password to verify
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(String usrCode1, String plainPassword) {
        log.debug("Verifying password for user: {}", usrCode1);

        Optional<PasswordDTO> passwordDTO = passwordService.getPasswordByUserCode(usrCode1);

        if (passwordDTO.isPresent()) {
            return passwordEncoderUtil.verifyPassword(
                    plainPassword, passwordDTO.get().getPassword());
        }

        log.warn("Password record not found for user: {}", usrCode1);
        return false;
    }

    /**
     * Reset user password to default
     *
     * @param usrCode1 User code
     * @return true if password reset successfully
     */
    @Transactional
    public boolean resetPasswordToDefault(String usrCode1) {
        log.info("Resetting password to default for user: {}", usrCode1);

        String encryptedPassword = passwordEncoderUtil.generateDefaultPassword();
        PasswordDTO passwordDTO =
                PasswordDTO.builder().userCode(usrCode1).password(encryptedPassword).build();

        Optional<PasswordDTO> updated = passwordService.updatePassword(usrCode1, passwordDTO);

        if (updated.isPresent()) {
            log.info("Password reset to default successfully for user: {}", usrCode1);
            return true;
        }

        log.warn("Password reset failed - User not found: {}", usrCode1);
        return false;
    }

    // ========== COUNT Operations ==========

    /**
     * Count total users
     *
     * @return Total count
     */
    public long getTotalUserCount() {
        log.debug("Counting total users");
        return userMasterRepository.count();
    }

    /**
     * Count active users
     *
     * @return Active user count
     */
    public long getActiveUserCount() {
        log.debug("Counting active users");
        return userMasterRepository.findByIsActive(1).size();
    }

    /**
     * Count active users in a branch
     *
     * @param brCode Branch code
     * @return Active user count in branch
     */
    public long getActiveUserCountByBranch(Integer brCode) {
        log.debug("Counting active users in branch: {}", brCode);
        return userMasterRepository.countActiveUsersByBranch(brCode, 1);
    }

    // ========== Existence Check Operations ==========

    /**
     * Check if user exists by primary key
     *
     * @param usrCode1 User code
     * @return true if exists, false otherwise
     */
    public boolean userExists(String usrCode1) {
        log.debug("Checking if user exists with code: {}", usrCode1);
        return userMasterRepository.existsById(usrCode1);
    }

    /**
     * Check if user exists by email
     *
     * @param emailId Email ID
     * @return true if exists, false otherwise
     */
    public boolean userExistsByEmail(String emailId) {
        log.debug("Checking if user exists with email: {}", emailId);
        return userMasterRepository.findByEmailIdIgnoreCase(emailId).isPresent();
    }
}
