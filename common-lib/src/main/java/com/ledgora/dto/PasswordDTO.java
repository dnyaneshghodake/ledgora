package com.ledgora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Password DTO - For API Request/Response Used for transferring password data between layers.
 * Contains user code and password information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordDTO {

    // ========== Keys ==========
    /** User Code - Unique identifier for the user Type: String (S), Size: 16 */
    private String userCode;

    // ========== Password Information ==========
    /** User Password Type: String (S), Size: 16 Note: Should be encrypted/hashed before storage */
    private String password;
}
