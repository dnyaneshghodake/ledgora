package com.ledgora.account.repository;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    Boolean existsByAccountNumber(String accountNumber);
    List<Account> findByStatus(AccountStatus status);
    List<Account> findByAccountType(AccountType accountType);
    List<Account> findByBranchCode(String branchCode);
    List<Account> findByCustomerNameContainingIgnoreCase(String customerName);

    @Query("SELECT a FROM Account a WHERE a.status = :status AND a.accountType = :type")
    List<Account> findByStatusAndType(@Param("status") AccountStatus status, @Param("type") AccountType type);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.status = :status")
    long countByStatus(@Param("status") AccountStatus status);
}
