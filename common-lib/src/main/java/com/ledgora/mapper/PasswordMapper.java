package com.ledgora.mapper;

import com.ledgora.dto.PasswordDTO;
import com.ledgora.model.Password;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper Class for Password Entity and DTO
 *
 * <p>Converts between Password entity and PasswordDTO for API operations.
 */
@Component
public class PasswordMapper {

    /**
     * Convert Password Entity to DTO
     *
     * @param password Entity object
     * @return DTO object
     */
    public PasswordDTO toDTO(Password password) {
        if (password == null) {
            return null;
        }

        return PasswordDTO.builder()
                .userCode(password.getUserCode())
                .password(password.getPassword())
                .build();
    }

    /**
     * Convert PasswordDTO to Entity
     *
     * @param passwordDTO DTO object
     * @return Entity object
     */
    public Password toEntity(PasswordDTO passwordDTO) {
        if (passwordDTO == null) {
            return null;
        }

        Password password = new Password();
        password.setUserCode(passwordDTO.getUserCode());
        password.setPassword(passwordDTO.getPassword());

        return password;
    }

    /**
     * Convert List of Password Entities to DTOs
     *
     * @param passwords List of entity objects
     * @return List of DTO objects
     */
    public List<PasswordDTO> toDTOList(List<Password> passwords) {
        if (passwords == null || passwords.isEmpty()) {
            return List.of();
        }

        return passwords.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Convert List of PasswordDTOs to Entities
     *
     * @param passwordDTOs List of DTO objects
     * @return List of entity objects
     */
    public List<Password> toEntityList(List<PasswordDTO> passwordDTOs) {
        if (passwordDTOs == null || passwordDTOs.isEmpty()) {
            return List.of();
        }

        return passwordDTOs.stream().map(this::toEntity).collect(Collectors.toList());
    }

    /**
     * Update existing Password entity from DTO
     *
     * @param passwordDTO Source DTO
     * @param password Target entity to update
     */
    public void updateEntityFromDTO(PasswordDTO passwordDTO, Password password) {
        if (passwordDTO == null || password == null) {
            return;
        }

        if (passwordDTO.getUserCode() != null) {
            password.setUserCode(passwordDTO.getUserCode());
        }
        if (passwordDTO.getPassword() != null) {
            password.setPassword(passwordDTO.getPassword());
        }
    }
}
