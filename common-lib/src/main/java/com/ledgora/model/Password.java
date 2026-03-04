package com.ledgora.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Password Entity - POJO Model
 *
 * <p>File Name : D002002.DDF File Desc : Password File Module Type : 002/Application Level Maint.
 *
 * <p>This entity represents user password records for authentication purposes.
 */
@Entity
@Table(name = "D002002", schema = "OASYS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Password {

    // ========== Primary Key ==========
    /** Primary Key - User Code Type: String (S), Size: 16 Unique identifier for the user */
    @Id
    @Column(name = "USER_CODE", length = 16, nullable = false)
    private String userCode;

    // ========== Password Information ==========
    /** User Password Type: String (S), Size: 16 Encrypted/Hashed password field */
    @Column(name = "PASSWORD", length = 16, nullable = false)
    private String password;
}
