package com.ledgora.ledger.repository;

import com.ledgora.common.enums.EntryType;
import com.ledgora.ledger.entity.LedgerEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for immutable ledger entries.
 *
 * <p>CBS Rule: LedgerEntries must NEVER be deleted or updated. The delete methods inherited from
 * JpaRepository are overridden to throw exceptions. Corrections must be made via reversal entries
 * (opposite DR/CR postings).
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /**
     * @deprecated CBS: Ledger entries are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteById(Long id) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger entries cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger entries are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void delete(LedgerEntry entity) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger entries cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger entries are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger entries cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger entries are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAllById(Iterable<? extends Long> ids) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger entries cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger entries are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAll(Iterable<? extends LedgerEntry> entities) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger entries cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger entries are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAllInBatch() {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger entries cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger entries are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAllByIdInBatch(Iterable<Long> ids) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger entries cannot be deleted. Use reversal entries.");
    }

    /**
     * @deprecated CBS: Ledger entries are immutable. Use reversal entries instead.
     */
    @Deprecated
    @Override
    default void deleteAllInBatch(Iterable<LedgerEntry> entities) {
        throw new UnsupportedOperationException(
                "CBS violation: Ledger entries cannot be deleted. Use reversal entries.");
    }

    List<LedgerEntry> findByTransactionId(Long transactionId);

    /**
     * Fetch ledger entries for a transaction with account and journal eagerly loaded. Prevents N+1
     * when accessing entry.getAccount() or entry.getJournal() in the view layer.
     */
    @Query(
            "SELECT le FROM LedgerEntry le "
                    + "LEFT JOIN FETCH le.account "
                    + "LEFT JOIN FETCH le.journal "
                    + "WHERE le.transaction.id = :transactionId "
                    + "ORDER BY le.id")
    List<LedgerEntry> findByTransactionIdWithGraph(@Param("transactionId") Long transactionId);

    List<LedgerEntry> findByTransactionIdAndTenantId(Long transactionId, Long tenantId);

    List<LedgerEntry> findByAccountId(Long accountId);

    List<LedgerEntry> findByEntryType(EntryType entryType);

    @Query(
            "SELECT le FROM LedgerEntry le WHERE le.account.accountNumber = :accountNumber ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query(
            "SELECT le FROM LedgerEntry le WHERE le.tenant.id = :tenantId AND le.account.accountNumber = :accountNumber ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByAccountNumberAndTenantId(
            @Param("accountNumber") String accountNumber, @Param("tenantId") Long tenantId);

    @Query(
            "SELECT le FROM LedgerEntry le WHERE le.glAccountCode = :glCode ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByGlAccountCode(@Param("glCode") String glCode);

    @Query(
            "SELECT le FROM LedgerEntry le WHERE le.tenant.id = :tenantId AND le.glAccountCode = :glCode ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByGlAccountCodeAndTenantId(
            @Param("glCode") String glCode, @Param("tenantId") Long tenantId);

    @Query(
            "SELECT le FROM LedgerEntry le WHERE le.createdAt BETWEEN :startDate AND :endDate ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByDateRange(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(
            "SELECT le FROM LedgerEntry le WHERE le.businessDate = :businessDate ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByBusinessDate(@Param("businessDate") LocalDate businessDate);

    @Query(
            "SELECT le FROM LedgerEntry le WHERE le.tenant.id = :tenantId AND le.businessDate = :businessDate ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByBusinessDateAndTenantId(
            @Param("businessDate") LocalDate businessDate, @Param("tenantId") Long tenantId);

    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.businessDate = :businessDate")
    BigDecimal sumDebitsByBusinessDate(@Param("businessDate") LocalDate businessDate);

    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.tenant.id = :tenantId AND le.businessDate = :businessDate")
    BigDecimal sumDebitsByBusinessDateAndTenantId(
            @Param("businessDate") LocalDate businessDate, @Param("tenantId") Long tenantId);

    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.businessDate = :businessDate")
    BigDecimal sumCreditsByBusinessDate(@Param("businessDate") LocalDate businessDate);

    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.tenant.id = :tenantId AND le.businessDate = :businessDate")
    BigDecimal sumCreditsByBusinessDateAndTenantId(
            @Param("businessDate") LocalDate businessDate, @Param("tenantId") Long tenantId);

    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.account.id = :accountId")
    BigDecimal sumDebitsByAccountId(@Param("accountId") Long accountId);

    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.account.id = :accountId")
    BigDecimal sumCreditsByAccountId(@Param("accountId") Long accountId);

    // PART 5: Snapshot-based balance queries - entries after a given entry ID
    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.account.id = :accountId AND le.id > :afterEntryId")
    BigDecimal sumCreditsAfterEntryId(
            @Param("accountId") Long accountId, @Param("afterEntryId") Long afterEntryId);

    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.account.id = :accountId AND le.id > :afterEntryId")
    BigDecimal sumDebitsAfterEntryId(
            @Param("accountId") Long accountId, @Param("afterEntryId") Long afterEntryId);

    // PART 1: Journal-based queries
    List<LedgerEntry> findByJournalId(Long journalId);

    // PART 6: Validator queries - sum debits/credits per transaction
    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.transaction.id = :transactionId")
    BigDecimal sumDebitsByTransactionId(@Param("transactionId") Long transactionId);

    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) FROM LedgerEntry le WHERE le.transaction.id = :transactionId")
    BigDecimal sumCreditsByTransactionId(@Param("transactionId") Long transactionId);

    // PART 6: Count orphan entries (entries without valid transaction)
    @Query("SELECT COUNT(le) FROM LedgerEntry le WHERE le.transaction IS NULL")
    long countOrphanEntries();

    // ── Financial Statement Engine queries (Balance Sheet + P&L) ──

    /**
     * Balance Sheet: cumulative DEBIT total for a GL code up to and including businessDate. Used by
     * BalanceSheetEngine to compute closing balance per GL.
     */
    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) "
                    + "FROM LedgerEntry le "
                    + "WHERE le.glAccountCode = :glCode AND le.tenant.id = :tenantId "
                    + "AND le.businessDate <= :asOfDate")
    BigDecimal sumDebitsByGlCodeAndDateRange(
            @Param("glCode") String glCode,
            @Param("tenantId") Long tenantId,
            @Param("asOfDate") LocalDate asOfDate);

    /** Balance Sheet: cumulative CREDIT total for a GL code up to and including businessDate. */
    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) "
                    + "FROM LedgerEntry le "
                    + "WHERE le.glAccountCode = :glCode AND le.tenant.id = :tenantId "
                    + "AND le.businessDate <= :asOfDate")
    BigDecimal sumCreditsByGlCodeAndDateRange(
            @Param("glCode") String glCode,
            @Param("tenantId") Long tenantId,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * P&L: DEBIT total for a GL code within a date range (inclusive). Used by PnlEngine for
     * period-based expense computation.
     */
    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE 0 END), 0) "
                    + "FROM LedgerEntry le "
                    + "WHERE le.glAccountCode = :glCode AND le.tenant.id = :tenantId "
                    + "AND le.businessDate >= :startDate AND le.businessDate <= :endDate")
    BigDecimal sumDebitsByGlCodeAndTenantAndDateRange(
            @Param("glCode") String glCode,
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * P&L: CREDIT total for a GL code within a date range (inclusive). Used by PnlEngine for
     * period-based revenue computation.
     */
    @Query(
            "SELECT COALESCE(SUM(CASE WHEN le.entryType = 'CREDIT' THEN le.amount ELSE 0 END), 0) "
                    + "FROM LedgerEntry le "
                    + "WHERE le.glAccountCode = :glCode AND le.tenant.id = :tenantId "
                    + "AND le.businessDate >= :startDate AND le.businessDate <= :endDate")
    BigDecimal sumCreditsByGlCodeAndTenantAndDateRange(
            @Param("glCode") String glCode,
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
