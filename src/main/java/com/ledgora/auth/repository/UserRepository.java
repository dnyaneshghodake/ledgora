package com.ledgora.auth.repository;

import com.ledgora.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    List<User> findByIsActive(Boolean isActive);
    List<User> findByBranchCode(String branchCode);
    long countByRolesContaining(com.ledgora.auth.entity.Role role);
}
