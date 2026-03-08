package com.ledgora.account.repository;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.LedgerAccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    Optional<Account> findByAccountNumberAndTenantId(String accountNumber, Long tenantId);
    List<Account> findByTenantId(Long tenantId);
    List<Account> findByTenantIdAndStatus(Long tenantId, AccountStatus status);
    List<Account> findByTenantIdAndAccountType(Long tenantId, AccountType accountType);

    @Query("SELECT a FROM Account a WHERE a.tenant.id = :tenantId AND LOWER(a.customerName) LIKE LOWER(CONCAT('%', :customerName, '%'))")
    List<Account> findByTenantIdAndCustomerNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("customerName") String customerName);

    @Query("SELECT a FROM Account a WHERE a.status = :status AND a.accountType = :type")
    List<Account> findByStatusAndType(@Param("status") AccountStatus status, @Param("type") AccountType type);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.status = :status")
    long countByStatus(@Param("status") AccountStatus status);

    // PART 2: Hierarchy support
    List<Account> findByParentAccountId(Long parentAccountId);
    List<Account> findByLedgerAccountType(LedgerAccountType ledgerAccountType);

    // PART 8: Pessimistic locking for financial operations
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.tenant.id = :tenantId")
    Optional<Account> findByAccountNumberWithLockAndTenantId(@Param("accountNumber") String accountNumber, @Param("tenantId") Long tenantId);

    Optional<Account> findFirstByTenantIdAndGlAccountCode(Long tenantId, String glAccountCode);
}
