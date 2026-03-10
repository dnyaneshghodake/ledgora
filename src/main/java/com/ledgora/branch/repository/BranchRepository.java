package com.ledgora.branch.repository;

import com.ledgora.branch.entity.Branch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByBranchCode(String branchCode);

    Boolean existsByBranchCode(String branchCode);
}
