package com.ledgora.ledger.repository;

import com.ledgora.ledger.entity.LedgerSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerSnapshotRepository extends JpaRepository<LedgerSnapshot, Long> {

    @Query("SELECT ls FROM LedgerSnapshot ls WHERE ls.accountId = :accountId ORDER BY ls.snapshotDate DESC LIMIT 1")
    Optional<LedgerSnapshot> findLatestByAccountId(@Param("accountId") Long accountId);

    List<LedgerSnapshot> findByAccountId(Long accountId);

    List<LedgerSnapshot> findBySnapshotDate(LocalDate snapshotDate);

    @Query("SELECT ls FROM LedgerSnapshot ls WHERE ls.accountId = :accountId AND ls.snapshotDate <= :date ORDER BY ls.snapshotDate DESC LIMIT 1")
    Optional<LedgerSnapshot> findLatestByAccountIdAndDate(@Param("accountId") Long accountId, @Param("date") LocalDate date);
}
