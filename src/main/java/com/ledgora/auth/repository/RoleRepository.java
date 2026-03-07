package com.ledgora.auth.repository;

import com.ledgora.auth.entity.Role;
import com.ledgora.common.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
    Boolean existsByName(RoleName name);
}
