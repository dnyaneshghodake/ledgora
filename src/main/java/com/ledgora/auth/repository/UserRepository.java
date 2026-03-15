package com.ledgora.auth.repository;

import com.ledgora.auth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    List<User> findByIsActive(Boolean isActive);

    List<User> findByBranchCode(String branchCode);

    /** All users for a tenant — tenant-scoped user management. */
    List<User> findByTenantId(Long tenantId);

    /** Active users for a tenant. */
    List<User> findByTenantIdAndIsActive(Long tenantId, Boolean isActive);

    long countByRolesContaining(com.ledgora.auth.entity.Role role);
}
