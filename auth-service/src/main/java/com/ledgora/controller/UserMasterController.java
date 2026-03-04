package com.ledgora.controller;

import com.ledgora.dto.UserMasterDTO;
import com.ledgora.mapper.UserMasterMapper;
import com.ledgora.model.UserMaster;
import com.ledgora.service.UserMasterService;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for UserMaster Entity
 *
 * <p>Provides REST API endpoints for User Master management operations. Handles CRUD operations and
 * various search/filter operations.
 */
@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
@Slf4j
public class UserMasterController {

    private final UserMasterService userMasterService;
    private final UserMasterMapper userMasterMapper;

    // ========== CREATE Operations ==========

    /**
     * Create a new user POST /api/users
     *
     * @param userDTO User data to create
     * @return Created user with HTTP 201
     */
    @PostMapping
    public ResponseEntity<UserMasterDTO> createUser(@RequestBody UserMasterDTO userDTO) {
        log.info("Creating new user with code: {}", userDTO.getUsrCode1());
        UserMaster userMaster = userMasterMapper.toEntity(userDTO);
        UserMaster createdUser = userMasterService.createUser(userMaster);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMasterMapper.toDTO(createdUser));
    }

    // ========== READ Operations ==========

    /**
     * Get user by primary key (usrCode1) GET /api/users/{usrCode1}
     *
     * @param usrCode1 User code
     * @return User if found, 404 if not found
     */
    @GetMapping("/{usrCode1}")
    public ResponseEntity<UserMasterDTO> getUserByCode1(@PathVariable String usrCode1) {
        log.debug("Fetching user with code1: {}", usrCode1);
        Optional<UserMaster> user = userMasterService.getUserByCode1(usrCode1);
        return user.map(u -> ResponseEntity.ok(userMasterMapper.toDTO(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get user by alternative key (usrCode2) GET /api/users/code2/{usrCode2}
     *
     * @param usrCode2 Alternative user code
     * @return User if found
     */
    @GetMapping("/code2/{usrCode2}")
    public ResponseEntity<UserMasterDTO> getUserByCode2(@PathVariable Integer usrCode2) {
        log.debug("Fetching user with code2: {}", usrCode2);
        Optional<UserMaster> user = userMasterService.getUserByCode2(usrCode2);
        return user.map(u -> ResponseEntity.ok(userMasterMapper.toDTO(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get user by alternative key (usrCode3) GET /api/users/code3/{usrCode3}
     *
     * @param usrCode3 Alternative user code
     * @return User if found
     */
    @GetMapping("/code3/{usrCode3}")
    public ResponseEntity<UserMasterDTO> getUserByCode3(@PathVariable String usrCode3) {
        log.debug("Fetching user with code3: {}", usrCode3);
        Optional<UserMaster> user = userMasterService.getUserByCode3(usrCode3);
        return user.map(u -> ResponseEntity.ok(userMasterMapper.toDTO(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get user by email ID GET /api/users/email/{emailId}
     *
     * @param emailId Email ID
     * @return User if found
     */
    @GetMapping("/email/{emailId}")
    public ResponseEntity<UserMasterDTO> getUserByEmail(@PathVariable String emailId) {
        log.debug("Fetching user with email: {}", emailId);
        Optional<UserMaster> user = userMasterService.getUserByEmail(emailId);
        return user.map(u -> ResponseEntity.ok(userMasterMapper.toDTO(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get all users GET /api/users
     *
     * @return List of all users
     */
    @GetMapping
    public ResponseEntity<List<UserMasterDTO>> getAllUsers() {
        log.debug("Fetching all users");
        List<UserMaster> users = userMasterService.getAllUsers();
        return ResponseEntity.ok(userMasterMapper.toDTOList(users));
    }

    /**
     * Get all active users GET /api/users/active
     *
     * @return List of active users
     */
    @GetMapping("/active")
    public ResponseEntity<List<UserMasterDTO>> getAllActiveUsers() {
        log.debug("Fetching all active users");
        List<UserMaster> users = userMasterService.getAllActiveUsers();
        return ResponseEntity.ok(userMasterMapper.toDTOList(users));
    }

    // ========== FIND by Branch & Group ==========

    /**
     * Get users by branch GET /api/users/branch/{brCode}
     *
     * @param brCode Branch code
     * @return List of users in branch
     */
    @GetMapping("/branch/{brCode}")
    public ResponseEntity<List<UserMasterDTO>> getUsersByBranch(@PathVariable Integer brCode) {
        log.debug("Fetching users for branch: {}", brCode);
        List<UserMaster> users = userMasterService.getUsersByBranch(brCode);
        return ResponseEntity.ok(userMasterMapper.toDTOList(users));
    }

    /**
     * Get active users by branch GET /api/users/branch/{brCode}/active
     *
     * @param brCode Branch code
     * @return List of active users in branch
     */
    @GetMapping("/branch/{brCode}/active")
    public ResponseEntity<List<UserMasterDTO>> getActiveUsersByBranch(
            @PathVariable Integer brCode) {
        log.debug("Fetching active users for branch: {}", brCode);
        List<UserMaster> users = userMasterService.getActiveUsersByBranch(brCode);
        return ResponseEntity.ok(userMasterMapper.toDTOList(users));
    }

    /**
     * Get users by group GET /api/users/group/{grpCd}
     *
     * @param grpCd Group code
     * @return List of users in group
     */
    @GetMapping("/group/{grpCd}")
    public ResponseEntity<List<UserMasterDTO>> getUsersByGroup(@PathVariable Integer grpCd) {
        log.debug("Fetching users for group: {}", grpCd);
        List<UserMaster> users = userMasterService.getUsersByGroup(grpCd);
        return ResponseEntity.ok(userMasterMapper.toDTOList(users));
    }

    /**
     * Get users by branch and group GET /api/users/branch/{brCode}/group/{grpCd}
     *
     * @param brCode Branch code
     * @param grpCd Group code
     * @return List of users
     */
    @GetMapping("/branch/{brCode}/group/{grpCd}")
    public ResponseEntity<List<UserMasterDTO>> getUsersByBranchAndGroup(
            @PathVariable Integer brCode, @PathVariable Integer grpCd) {
        log.debug("Fetching users for branch: {} and group: {}", brCode, grpCd);
        List<UserMaster> users = userMasterService.getUsersByBranchAndGroup(brCode, grpCd);
        return ResponseEntity.ok(userMasterMapper.toDTOList(users));
    }

    // ========== SEARCH Operations ==========

    /**
     * Search users by name GET /api/users/search/name?name={usrName}
     *
     * @param name User name (partial match)
     * @return List of matching users
     */
    @GetMapping("/search/name")
    public ResponseEntity<List<UserMasterDTO>> searchUsersByName(@RequestParam String name) {
        log.debug("Searching users by name: {}", name);
        List<UserMaster> users = userMasterService.searchUsersByName(name);
        return ResponseEntity.ok(userMasterMapper.toDTOList(users));
    }

    /**
     * Search users by multiple criteria GET
     * /api/users/search?name={name}&brCode={brCode}&grpCd={grpCd}&isActive={isActive}
     *
     * @param name User name (optional)
     * @param brCode Branch code (optional)
     * @param grpCd Group code (optional)
     * @param isActive Active status (optional)
     * @return List of matching users
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserMasterDTO>> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer brCode,
            @RequestParam(required = false) Integer grpCd,
            @RequestParam(required = false) Integer isActive) {
        log.debug(
                "Searching users with filters - name: {}, branch: {}, group: {}, active: {}",
                name,
                brCode,
                grpCd,
                isActive);
        List<UserMaster> users = userMasterService.searchUsers(name, brCode, grpCd, isActive);
        return ResponseEntity.ok(userMasterMapper.toDTOList(users));
    }

    /**
     * Get users with multi-branch access GET /api/users/access/multi-branch
     *
     * @return List of users with multi-branch access
     */
    @GetMapping("/access/multi-branch")
    public ResponseEntity<List<UserMasterDTO>> getUsersWithMultiBranchAccess() {
        log.debug("Fetching users with multi-branch access");
        List<UserMaster> users = userMasterService.getUsersWithMultiBranchAccess();
        return ResponseEntity.ok(userMasterMapper.toDTOList(users));
    }

    /**
     * Get users with bad login attempts GET /api/users/security/bad-logins
     *
     * @return List of users with bad login attempts
     */
    @GetMapping("/security/bad-logins")
    public ResponseEntity<List<UserMasterDTO>> getUsersWithBadLoginAttempts() {
        log.debug("Fetching users with bad login attempts");
        List<UserMaster> users = userMasterService.getUsersWithBadLoginAttempts();
        return ResponseEntity.ok(userMasterMapper.toDTOList(users));
    }

    // ========== UPDATE Operations ==========

    /**
     * Update an existing user PUT /api/users/{usrCode1}
     *
     * @param usrCode1 User code
     * @param userDTO Updated user data
     * @return Updated user
     */
    @PutMapping("/{usrCode1}")
    public ResponseEntity<UserMasterDTO> updateUser(
            @PathVariable String usrCode1, @RequestBody UserMasterDTO userDTO) {
        log.info("Updating user with code: {}", usrCode1);
        userDTO.setUsrCode1(usrCode1);
        UserMaster userMaster = userMasterMapper.toEntity(userDTO);
        UserMaster updatedUser = userMasterService.updateUser(userMaster);
        return ResponseEntity.ok(userMasterMapper.toDTO(updatedUser));
    }

    /**
     * Update user status PATCH /api/users/{usrCode1}/status/{status}
     *
     * @param usrCode1 User code
     * @param status New status
     * @return Updated user
     */
    @PatchMapping("/{usrCode1}/status/{status}")
    public ResponseEntity<UserMasterDTO> updateUserStatus(
            @PathVariable String usrCode1, @PathVariable Integer status) {
        log.info("Updating status for user: {} to {}", usrCode1, status);
        UserMaster updatedUser = userMasterService.updateUserStatus(usrCode1, status);
        return ResponseEntity.ok(userMasterMapper.toDTO(updatedUser));
    }

    /**
     * Activate/Deactivate user PATCH /api/users/{usrCode1}/active/{isActive}
     *
     * @param usrCode1 User code
     * @param isActive Active status
     * @return Updated user
     */
    @PatchMapping("/{usrCode1}/active/{isActive}")
    public ResponseEntity<UserMasterDTO> updateUserActiveStatus(
            @PathVariable String usrCode1, @PathVariable Integer isActive) {
        log.info("Updating active status for user: {} to {}", usrCode1, isActive);
        UserMaster updatedUser = userMasterService.updateUserActiveStatus(usrCode1, isActive);
        return ResponseEntity.ok(userMasterMapper.toDTO(updatedUser));
    }

    // ========== DELETE Operations ==========

    /**
     * Delete a user DELETE /api/users/{usrCode1}
     *
     * @param usrCode1 User code to delete
     * @return No content response
     */
    @DeleteMapping("/{usrCode1}")
    public ResponseEntity<Void> deleteUser(@PathVariable String usrCode1) {
        log.info("Deleting user with code: {}", usrCode1);
        userMasterService.deleteUser(usrCode1);
        return ResponseEntity.noContent().build();
    }

    // ========== COUNT Operations ==========

    /**
     * Get total user count GET /api/users/count/total
     *
     * @return Total count
     */
    @GetMapping("/count/total")
    public ResponseEntity<Long> getTotalUserCount() {
        log.debug("Getting total user count");
        long count = userMasterService.getTotalUserCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get active user count GET /api/users/count/active
     *
     * @return Active user count
     */
    @GetMapping("/count/active")
    public ResponseEntity<Long> getActiveUserCount() {
        log.debug("Getting active user count");
        long count = userMasterService.getActiveUserCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get active user count by branch GET /api/users/count/branch/{brCode}/active
     *
     * @param brCode Branch code
     * @return Active user count in branch
     */
    @GetMapping("/count/branch/{brCode}/active")
    public ResponseEntity<Long> getActiveUserCountByBranch(@PathVariable Integer brCode) {
        log.debug("Getting active user count for branch: {}", brCode);
        long count = userMasterService.getActiveUserCountByBranch(brCode);
        return ResponseEntity.ok(count);
    }

    // ========== PASSWORD Management ==========

    /**
     * Change password for a user POST /api/users/{usrCode1}/change-password
     *
     * @param usrCode1 User code
     * @param passwordChangeRequest Request containing new password
     * @return Success message
     */
    @PostMapping("/{usrCode1}/change-password")
    public ResponseEntity<String> changePassword(
            @PathVariable String usrCode1,
            @RequestBody PasswordChangeRequest passwordChangeRequest) {
        log.info("Changing password for user: {}", usrCode1);

        boolean success =
                userMasterService.changePassword(usrCode1, passwordChangeRequest.getNewPassword());

        if (success) {
            return ResponseEntity.ok("Password changed successfully for user: " + usrCode1);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("User not found or password change failed: " + usrCode1);
    }

    /**
     * Verify user password POST /api/users/{usrCode1}/verify-password
     *
     * @param usrCode1 User code
     * @param passwordVerifyRequest Request containing password to verify
     * @return true if password matches, false otherwise
     */
    @PostMapping("/{usrCode1}/verify-password")
    public ResponseEntity<Boolean> verifyPassword(
            @PathVariable String usrCode1,
            @RequestBody PasswordVerifyRequest passwordVerifyRequest) {
        log.debug("Verifying password for user: {}", usrCode1);

        boolean isValid =
                userMasterService.verifyPassword(usrCode1, passwordVerifyRequest.getPassword());
        return ResponseEntity.ok(isValid);
    }

    /**
     * Reset user password to default POST /api/users/{usrCode1}/reset-password
     *
     * @param usrCode1 User code
     * @return Success message
     */
    @PostMapping("/{usrCode1}/reset-password")
    public ResponseEntity<String> resetPasswordToDefault(@PathVariable String usrCode1) {
        log.info("Resetting password to default for user: {}", usrCode1);

        boolean success = userMasterService.resetPasswordToDefault(usrCode1);

        if (success) {
            return ResponseEntity.ok(
                    "Password reset to default (changeme@123) for user: " + usrCode1);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("User not found or password reset failed: " + usrCode1);
    }

    // ========== Health Check ==========

    /**
     * Health check endpoint GET /api/users/health
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Master API is up and running");
    }

    // ========== Request/Response DTOs ==========

    /** Request DTO for password change */
    public static class PasswordChangeRequest {
        private String newPassword;

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /** Request DTO for password verification */
    public static class PasswordVerifyRequest {
        private String password;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
