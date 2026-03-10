package com.ledgora.account.repository;

import com.ledgora.account.entity.AccountBalance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {
    Optional<AccountBalance> findByAccountId(Long accountId);
}
