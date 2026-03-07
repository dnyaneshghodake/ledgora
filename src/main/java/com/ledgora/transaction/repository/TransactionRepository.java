package com.ledgora.transaction.repository;

import com.ledgora.transaction.entity.Transaction;
import com.ledgora.common.enums.TransactionStatus;
import com.ledgora.common.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionRef(String transactionRef);
    List<Transaction> findByTransactionType(TransactionType type);
    List<Transaction> findByStatus(TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount.accountNumber = :accountNumber OR t.destinationAccount.accountNumber = :accountNumber ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByStatusAndDateRange(@Param("status") TransactionStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.businessDate = :businessDate")
    List<Transaction> findByStatusAndBusinessDate(@Param("status") TransactionStatus status, @Param("businessDate") LocalDate businessDate);

    List<Transaction> findByBusinessDate(LocalDate businessDate);
}
