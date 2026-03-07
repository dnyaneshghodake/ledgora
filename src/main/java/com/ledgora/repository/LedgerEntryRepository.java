package com.ledgora.repository;

import com.ledgora.model.LedgerEntry;
import com.ledgora.model.enums.EntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
