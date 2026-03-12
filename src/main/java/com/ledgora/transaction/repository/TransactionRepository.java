package com.ledgora.transaction.repository;

import com.ledgora.common.enums.TransactionStatus;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.transaction.entity.Transaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionRef(String transactionRef);

    List<Transaction> findByTransactionType(TransactionType type);

    List<Transaction> findByStatus(TransactionStatus status);

    Optional<Transaction> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Transaction> findByTransactionRefAndTenantId(String transactionRef, Long tenantId);

    List<Transaction> findByTenantId(Long tenantId);

    List<Transaction> findByTenantIdAndTransactionType(Long tenantId, TransactionType type);

    long countByTenantId(Long tenantId);

    /** Paginated transaction list by tenant. */
    org.springframework.data.domain.Page<Transaction> findByTenantId(
            Long tenantId, org.springframework.data.domain.Pageable pageable);

    @Query(
            "SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId AND (t.sourceAccount.accountNumber = :accountNumber OR t.destinationAccount.accountNumber = :accountNumber) ORDER BY t.createdAt DESC")
    List<Transaction> findByTenantIdAndAccountNumber(
            @Param("tenantId") Long tenantId, @Param("accountNumber") String accountNumber);

    @Query(
            "SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByTenantIdAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(
            "SELECT t FROM Transaction t WHERE t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountId(@Param("accountId") Long accountId);

    @Query(
            "SELECT t FROM Transaction t WHERE t.sourceAccount.accountNumber = :accountNumber OR t.destinationAccount.accountNumber = :accountNumber ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query(
            "SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByDateRange(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(
            "SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByStatusAndDateRange(
            @Param("status") TransactionStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(
            "SELECT t FROM Transaction t WHERE t.status = :status AND t.businessDate = :businessDate")
    List<Transaction> findByStatusAndBusinessDate(
            @Param("status") TransactionStatus status,
            @Param("businessDate") LocalDate businessDate);

    @Query(
            "SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId AND t.status = :status AND t.businessDate = :businessDate")
    List<Transaction> findByTenantIdAndStatusAndBusinessDate(
            @Param("tenantId") Long tenantId,
            @Param("status") TransactionStatus status,
            @Param("businessDate") LocalDate businessDate);

    List<Transaction> findByBusinessDate(LocalDate businessDate);

    List<Transaction> findByTenantIdAndBusinessDate(Long tenantId, LocalDate businessDate);

    // PART 2: Idempotency - find by client reference and channel
    @Query(
            "SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId AND t.clientReferenceId = :clientRefId AND t.channel = :channel")
    Optional<Transaction> findByClientReferenceIdAndChannelAndTenantId(
            @Param("clientRefId") String clientReferenceId,
            @Param("channel") com.ledgora.common.enums.TransactionChannel channel,
            @Param("tenantId") Long tenantId);

    // PART 6: Validator - get all transaction IDs for validation
    @Query("SELECT t.id FROM Transaction t")
    List<Long> findAllTransactionIds();

    // Transaction approval queue queries
    @Query(
            "SELECT t FROM Transaction t WHERE t.tenant.id = :tenantId AND t.status = :status ORDER BY t.createdAt DESC")
    List<Transaction> findByTenantIdAndStatus(
            @Param("tenantId") Long tenantId, @Param("status") TransactionStatus status);

    @Query(
            "SELECT COUNT(t) FROM Transaction t WHERE t.tenant.id = :tenantId AND t.status = :status")
    long countByTenantIdAndStatus(
            @Param("tenantId") Long tenantId, @Param("status") TransactionStatus status);

    /** Count transactions for an account in the last N minutes (velocity check). */
    @Query(
            "SELECT COUNT(t) FROM Transaction t WHERE t.tenant.id = :tenantId "
                    + "AND (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId) "
                    + "AND t.status != 'REJECTED' AND t.createdAt >= :since")
    long countRecentByAccountId(
            @Param("tenantId") Long tenantId,
            @Param("accountId") Long accountId,
            @Param("since") LocalDateTime since);

    /** Sum amount for an account in the last N minutes (velocity check). */
    @Query(
            "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.tenant.id = :tenantId "
                    + "AND (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId) "
                    + "AND t.status != 'REJECTED' AND t.createdAt >= :since")
    java.math.BigDecimal sumRecentAmountByAccountId(
            @Param("tenantId") Long tenantId,
            @Param("accountId") Long accountId,
            @Param("since") LocalDateTime since);

    /**
     * Transaction 360° View: Fetch a transaction with all header associations eagerly loaded in one
     * query. Eliminates N+1 for the 360° detail screen by JOIN FETCHing tenant, source/destination
     * accounts, maker, checker, performedBy, batch, and reversalOf.
     */
    @Query(
            "SELECT DISTINCT t FROM Transaction t "
                    + "LEFT JOIN FETCH t.tenant "
                    + "LEFT JOIN FETCH t.sourceAccount "
                    + "LEFT JOIN FETCH t.destinationAccount "
                    + "LEFT JOIN FETCH t.maker "
                    + "LEFT JOIN FETCH t.checker "
                    + "LEFT JOIN FETCH t.performedBy "
                    + "LEFT JOIN FETCH t.batch "
                    + "LEFT JOIN FETCH t.reversalOf "
                    + "WHERE t.id = :id")
    Optional<Transaction> findByIdWithFullGraph(@Param("id") Long id);
}
