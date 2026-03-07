package com.ledgora.transaction.repository;

import com.ledgora.transaction.entity.TransactionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionLineRepository extends JpaRepository<TransactionLine, Long> {
    List<TransactionLine> findByTransactionId(Long transactionId);
    List<TransactionLine> findByAccountId(Long accountId);

    @Query("SELECT tl FROM TransactionLine tl WHERE tl.account.accountNumber = :accountNumber ORDER BY tl.createdAt DESC")
    List<TransactionLine> findByAccountNumber(@Param("accountNumber") String accountNumber);
}
