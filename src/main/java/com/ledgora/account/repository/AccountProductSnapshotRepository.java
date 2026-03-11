package com.ledgora.account.repository;

import com.ledgora.account.entity.AccountProductSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountProductSnapshotRepository
        extends JpaRepository<AccountProductSnapshot, Long> {

    Optional<AccountProductSnapshot> findByAccountId(Long accountId);

    boolean existsByAccountId(Long accountId);
}
