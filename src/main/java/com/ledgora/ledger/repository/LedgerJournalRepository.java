package com.ledgora.ledger.repository;

import com.ledgora.ledger.entity.LedgerJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerJournalRepository extends JpaRepository<LedgerJournal, Long> {
    List<LedgerJournal> findByTransactionId(Long transactionId);
    List<LedgerJournal> findByBusinessDate(LocalDate businessDate);

    @Query("SELECT lj FROM LedgerJournal lj WHERE lj.businessDate BETWEEN :startDate AND :endDate ORDER BY lj.createdAt DESC")
    List<LedgerJournal> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
