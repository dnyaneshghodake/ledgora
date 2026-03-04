package com.ledgora.repository;

import com.ledgora.model.Password;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository Interface for Password Entity
 *
 * <p>Provides CRUD operations for Password records. Handles database operations for user password
 * management.
 */
@Repository
public interface PasswordRepository extends JpaRepository<Password, String> {

    /**
     * Find password record by user code
     *
     * @param userCode The user code to search for
     * @return Optional containing the password record if found
     */
    Optional<Password> findByUserCode(String userCode);
}
