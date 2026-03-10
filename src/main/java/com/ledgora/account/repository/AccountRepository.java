package com.ledgora.account.repository;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.LedgerAccountType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query(
            "SELECT a FROM Account a WHERE a.tenant.id = :tenantId AND LOWER(a.customerName) LIKE LOWER(CONCAT('%', :customerName, '%'))")
    List<Account> findByTenantIdAndCustomerNameContainingIgnoreCase(
            @Param("tenantId") Long tenantId, @Param("customerName") String customerName);

    @Query(
            "SELECT a FROM Account a WHERE a.tenant.id = :tenantId AND (LOWER(a.accountNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.accountName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.customerName) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Account> searchByTenantId(@Param("tenantId") Long tenantId, @Param("query") String query);

    @Query("SELECT a FROM Account a WHERE a.status = :status AND a.accountType = :type")
    List<Account> findByStatusAndType(
            @Param("status") AccountStatus status, @Param("type") AccountType type);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.status = :status")
    long countByStatus(@Param("status") AccountStatus status);

    /** Count accounts by tenant and status (e.g., UNDER_REVIEW for velocity dashboard). */
    long countByTenantIdAndStatus(Long tenantId, AccountStatus status);

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
    @Query(
            "SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.tenant.id = :tenantId")
    Optional<Account> findByAccountNumberWithLockAndTenantId(
            @Param("accountNumber") String accountNumber, @Param("tenantId") Long tenantId);

    Optional<Account> findFirstByTenantIdAndGlAccountCode(Long tenantId, String glAccountCode);

    // Customer-linked accounts
    @Query("SELECT a FROM Account a WHERE a.customer.customerId = :customerId")
    List<Account> findByCustomerId(@Param("customerId") Long customerId);

    @Query(
            "SELECT a FROM Account a WHERE a.tenant.id = :tenantId AND a.customer.customerId = :customerId")
    List<Account> findByTenantIdAndCustomerId(
            @Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    /** Find all accounts linked to a CustomerMaster (for freeze propagation). */
    @Query(
            "SELECT a FROM Account a WHERE a.customerMaster.id = :customerMasterId AND a.tenant.id = :tenantId")
    List<Account> findByCustomerMasterIdAndTenantId(
            @Param("customerMasterId") Long customerMasterId, @Param("tenantId") Long tenantId);

    /** Sum balance of all accounts of a given type for a tenant (e.g., clearing GL net check). */
    @Query(
            "SELECT COALESCE(SUM(a.balance), 0) FROM Account a "
                    + "WHERE a.tenant.id = :tenantId AND a.accountType = :accountType")
    java.math.BigDecimal sumBalanceByTenantIdAndAccountType(
            @Param("tenantId") Long tenantId, @Param("accountType") AccountType accountType);

    /** Find all IBC clearing accounts for a tenant (IBC-OUT + IBC-IN pattern). */
    @Query(
            "SELECT a FROM Account a WHERE a.tenant.id = :tenantId "
                    + "AND (a.accountNumber LIKE 'IBC-OUT-%' OR a.accountNumber LIKE 'IBC-IN-%')")
    List<Account> findIbcClearingAccountsByTenantId(@Param("tenantId") Long tenantId);

    /** Paginated account list for reconciliation (scales with large account counts). */
    org.springframework.data.domain.Page<Account> findByTenantId(
            Long tenantId, org.springframework.data.domain.Pageable pageable);
}
