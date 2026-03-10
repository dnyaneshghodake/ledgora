package com.ledgora.ledger.repository;

import com.ledgora.ledger.entity.LedgerJournal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for immutable ledger journals.
 *
 * <p>CBS Rule: LedgerJournals must NEVER be deleted or updated. The delete methods inherited from
 * JpaRepository are overridden to throw exceptions. Corrections must be made via reversal entries
 * (opposite DR/CR postings).
 */
@Repository
public interface LedgerJournalRepository extends JpaRepository<LedgerJournal, Long> {

    /**
     * @deprecated CBS: Ledger journals are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteById(Long id) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger journals cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger journals are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void delete(LedgerJournal entity) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger journals cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger journals are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger journals cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger journals are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAllById(Iterable<? extends Long> ids) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger journals cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger journals are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAll(Iterable<? extends LedgerJournal> entities) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger journals cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger journals are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAllInBatch() {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger journals cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger journals are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAllByIdInBatch(Iterable<Long> ids) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger journals cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger journals are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAllInBatch(Iterable<LedgerJournal> entities) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger journals cannot be deleted. Use reversal entries.");
    }

    List<LedgerJournal> findByTransactionId(Long transactionId);

    List<LedgerJournal> findByBusinessDate(LocalDate businessDate);

    @Query(
            "SELECT lj FROM LedgerJournal lj WHERE lj.businessDate BETWEEN :startDate AND :endDate ORDER BY lj.createdAt DESC")
    List<LedgerJournal> findByDateRange(
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
