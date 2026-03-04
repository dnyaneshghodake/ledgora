package com.ledgora.controller;

import com.ledgora.dto.PasswordDTO;
import com.ledgora.service.PasswordService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Password Management REST Controller
 *
 * <p>Provides endpoints for password management operations including password creation, retrieval,
 * update, and deletion.
 *
 * <p>Base URL: /api/passwords
 */
@RestController
@RequestMapping("/api/passwords")
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final PasswordService passwordService;

    /**
     * Create a new password record
     *
     * @param passwordDTO Password DTO with userCode and password
     * @return Created password DTO
     */
    @PostMapping
    public ResponseEntity<PasswordDTO> createPassword(@RequestBody PasswordDTO passwordDTO) {
        log.info("POST /api/passwords - Creating password for user: {}", passwordDTO.getUserCode());

        PasswordDTO createdPassword = passwordService.createPassword(passwordDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPassword);
    }

    /**
     * Get password record by user code
     *
     * @param userCode The user code
     * @return Password DTO if found, 404 if not found
     */
    @GetMapping("/{userCode}")
    public ResponseEntity<PasswordDTO> getPasswordByUserCode(@PathVariable String userCode) {
        log.debug("GET /api/passwords/{} - Fetching password for user: {}", userCode, userCode);

        Optional<PasswordDTO> passwordDTO = passwordService.getPasswordByUserCode(userCode);

        if (passwordDTO.isPresent()) {
            return ResponseEntity.ok(passwordDTO.get());
        }

        log.warn("Password record not found for user: {}", userCode);
        return ResponseEntity.notFound().build();
    }

    /**
     * Get all password records
     *
     * @return List of all password DTOs
     */
    @GetMapping
    public ResponseEntity<List<PasswordDTO>> getAllPasswords() {
        log.debug("GET /api/passwords - Fetching all password records");

        List<PasswordDTO> passwords = passwordService.getAllPasswords();
        return ResponseEntity.ok(passwords);
    }

    /**
     * Update password record for a user
     *
     * @param userCode The user code
     * @param passwordDTO Updated password DTO
     * @return Updated password DTO if found, 404 if not found
     */
    @PutMapping("/{userCode}")
    public ResponseEntity<PasswordDTO> updatePassword(
            @PathVariable String userCode, @RequestBody PasswordDTO passwordDTO) {
        log.info("PUT /api/passwords/{} - Updating password for user: {}", userCode, userCode);

        Optional<PasswordDTO> updatedPassword =
                passwordService.updatePassword(userCode, passwordDTO);

        if (updatedPassword.isPresent()) {
            return ResponseEntity.ok(updatedPassword.get());
        }

        log.warn("Password record not found for user: {}", userCode);
        return ResponseEntity.notFound().build();
    }

    /**
     * Delete password record for a user
     *
     * @param userCode The user code
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{userCode}")
    public ResponseEntity<Void> deletePassword(@PathVariable String userCode) {
        log.info("DELETE /api/passwords/{} - Deleting password for user: {}", userCode, userCode);

        boolean deleted = passwordService.deletePassword(userCode);

        if (deleted) {
            return ResponseEntity.noContent().build();
        }

        log.warn("Password record not found for deletion - user: {}", userCode);
        return ResponseEntity.notFound().build();
    }

    /**
     * Check if password record exists for a user
     *
     * @param userCode The user code
     * @return 200 with true/false
     */
    @GetMapping("/{userCode}/exists")
    public ResponseEntity<Boolean> passwordExists(@PathVariable String userCode) {
        log.debug(
                "GET /api/passwords/{}/exists - Checking if password exists for user: {}",
                userCode,
                userCode);

        boolean exists = passwordService.passwordExists(userCode);
        return ResponseEntity.ok(exists);
    }

    /**
     * Get total count of password records
     *
     * @return Total count
     */
    @GetMapping("/count/total")
    public ResponseEntity<Long> getTotalPasswordCount() {
        log.debug("GET /api/passwords/count/total - Getting total password count");

        long count = passwordService.getTotalPasswordCount();
        return ResponseEntity.ok(count);
    }
}
