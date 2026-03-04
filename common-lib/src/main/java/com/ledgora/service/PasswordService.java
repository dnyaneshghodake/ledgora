package com.ledgora.service;

import com.ledgora.dto.PasswordDTO;
import com.ledgora.mapper.PasswordMapper;
import com.ledgora.model.Password;
import com.ledgora.repository.PasswordRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Class for Password Management
 *
 * <p>Handles business logic for password operations including creating, updating, retrieving, and
 * deleting password records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordService {

    private final PasswordRepository passwordRepository;
    private final PasswordMapper passwordMapper;

    /**
     * Create a new password record
     *
     * @param passwordDTO Password DTO object
     * @return Created password DTO
     */
    public PasswordDTO createPassword(PasswordDTO passwordDTO) {
        log.info("Creating new password record for user: {}", passwordDTO.getUserCode());

        Password password = passwordMapper.toEntity(passwordDTO);
        Password savedPassword = passwordRepository.save(password);

        log.info("Password record created successfully for user: {}", savedPassword.getUserCode());
        return passwordMapper.toDTO(savedPassword);
    }

    /**
     * Get password record by user code
     *
     * @param userCode The user code to search for
     * @return Optional containing password DTO if found
     */
    @Transactional(readOnly = true)
    public Optional<PasswordDTO> getPasswordByUserCode(String userCode) {
        log.debug("Fetching password record for user: {}", userCode);

        Optional<Password> password = passwordRepository.findByUserCode(userCode);
        return password.map(passwordMapper::toDTO);
    }

    /**
     * Get all password records
     *
     * @return List of all password DTOs
     */
    @Transactional(readOnly = true)
    public List<PasswordDTO> getAllPasswords() {
        log.debug("Fetching all password records");

        List<Password> passwords = passwordRepository.findAll();
        return passwordMapper.toDTOList(passwords);
    }

    /**
     * Update an existing password record
     *
     * @param userCode The user code to update
     * @param passwordDTO Updated password DTO
     * @return Updated password DTO
     */
    public Optional<PasswordDTO> updatePassword(String userCode, PasswordDTO passwordDTO) {
        log.info("Updating password record for user: {}", userCode);

        Optional<Password> existingPassword = passwordRepository.findByUserCode(userCode);

        if (existingPassword.isPresent()) {
            Password password = existingPassword.get();
            passwordMapper.updateEntityFromDTO(passwordDTO, password);
            Password updatedPassword = passwordRepository.save(password);

            log.info("Password record updated successfully for user: {}", userCode);
            return Optional.of(passwordMapper.toDTO(updatedPassword));
        }

        log.warn("Password record not found for user: {}", userCode);
        return Optional.empty();
    }

    /**
     * Delete a password record
     *
     * @param userCode The user code to delete
     * @return true if deleted, false if not found
     */
    public boolean deletePassword(String userCode) {
        log.info("Deleting password record for user: {}", userCode);

        if (passwordRepository.existsById(userCode)) {
            passwordRepository.deleteById(userCode);
            log.info("Password record deleted successfully for user: {}", userCode);
            return true;
        }

        log.warn("Password record not found for deletion - user: {}", userCode);
        return false;
    }

    /**
     * Check if password record exists
     *
     * @param userCode The user code to check
     * @return true if password record exists
     */
    @Transactional(readOnly = true)
    public boolean passwordExists(String userCode) {
        log.debug("Checking if password record exists for user: {}", userCode);
        return passwordRepository.existsById(userCode);
    }

    /**
     * Get total count of password records
     *
     * @return Total count of password records
     */
    @Transactional(readOnly = true)
    public long getTotalPasswordCount() {
        log.debug("Getting total password record count");
        return passwordRepository.count();
    }
}
