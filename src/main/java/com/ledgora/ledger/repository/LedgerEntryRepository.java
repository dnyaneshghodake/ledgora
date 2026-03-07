package com.ledgora.ledger.repository;

import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.common.enums.EntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTransactionId(Long transactionId);
    List<LedgerEntry> findByAccountId(Long accountId);
    List<LedgerEntry> findByEntryType(EntryType entryType);

    @Query("SELECT le FROM LedgerEntry le WHERE le.account.accountNumber = :accountNumber ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT le FROM LedgerEntry le WHERE le.glAccountCode = :glCode ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByGlAccountCode(@Param("glCode") String glCode);

    @Query("SELECT le FROM LedgerEntry le WHERE le.createdAt BETWEEN :startDate AND :endDate ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT le FROM LedgerEntry le WHERE le.businessDate = :businessDate ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByBusinessDate(@Param("businessDate") LocalDate businessDate);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.businessDate = :businessDate")
    BigDecimal sumDebitsByBusinessDate(@Param("businessDate") LocalDate businessDate);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.businessDate = :businessDate")
    BigDecimal sumCreditsByBusinessDate(@Param("businessDate") LocalDate businessDate);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.account.id = :accountId")
    BigDecimal sumDebitsByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.account.id = :accountId")
    BigDecimal sumCreditsByAccountId(@Param("accountId") Long accountId);
}
