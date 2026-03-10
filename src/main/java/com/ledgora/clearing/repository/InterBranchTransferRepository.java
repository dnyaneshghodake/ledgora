package com.ledgora.clearing.repository;

import com.ledgora.clearing.entity.InterBranchTransfer;
import com.ledgora.common.enums.InterBranchTransferStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InterBranchTransferRepository
        extends JpaRepository<InterBranchTransfer, Long>, JpaSpecificationExecutor<InterBranchTransfer> {

    List<InterBranchTransfer> findByTenantIdAndStatus(
            Long tenantId, InterBranchTransferStatus status);

    List<InterBranchTransfer> findByTenantIdAndBusinessDateAndStatus(
            Long tenantId, LocalDate businessDate, InterBranchTransferStatus status);

    @Query(
            "SELECT COUNT(t) FROM InterBranchTransfer t WHERE t.tenant.id = :tenantId "
                    + "AND t.businessDate = :businessDate AND t.status NOT IN ('SETTLED', 'FAILED')")
    long countUnsettledByTenantAndDate(
            @Param("tenantId") Long tenantId, @Param("businessDate") LocalDate businessDate);

    @Query(
            "SELECT COALESCE(SUM(t.amount), 0) FROM InterBranchTransfer t WHERE t.tenant.id = :tenantId "
                    + "AND t.businessDate = :businessDate AND t.status = 'SENT'")
    BigDecimal sumSentAmountByTenantAndDate(
            @Param("tenantId") Long tenantId, @Param("businessDate") LocalDate businessDate);

    @Query(
            "SELECT COALESCE(SUM(t.amount), 0) FROM InterBranchTransfer t WHERE t.tenant.id = :tenantId "
                    + "AND t.businessDate = :businessDate AND t.status = 'RECEIVED'")
    BigDecimal sumReceivedAmountByTenantAndDate(
            @Param("tenantId") Long tenantId, @Param("businessDate") LocalDate businessDate);

    List<InterBranchTransfer> findByTenantIdAndToBranchIdAndStatus(
            Long tenantId, Long toBranchId, InterBranchTransferStatus status);

    /** Count IBT transfers from a specific branch on a business date (velocity check). */
    @Query(
            "SELECT COUNT(t) FROM InterBranchTransfer t WHERE t.tenant.id = :tenantId "
                    + "AND t.fromBranch.id = :branchId AND t.businessDate = :businessDate "
                    + "AND t.status NOT IN ('FAILED')")
    long countByTenantAndFromBranchAndDate(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("businessDate") LocalDate businessDate);

    /** Sum IBT amount from a specific branch on a business date (exposure limit check). */
    @Query(
            "SELECT COALESCE(SUM(t.amount), 0) FROM InterBranchTransfer t WHERE t.tenant.id = :tenantId "
                    + "AND t.fromBranch.id = :branchId AND t.businessDate = :businessDate "
                    + "AND t.status NOT IN ('FAILED')")
    BigDecimal sumAmountByTenantAndFromBranchAndDate(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("businessDate") LocalDate businessDate);

    /** Find all transfers for a tenant on a specific business date. */
    List<InterBranchTransfer> findByTenantIdAndBusinessDate(Long tenantId, LocalDate businessDate);

    /** Find all transfers for a tenant (ordered by creation date descending). */
    List<InterBranchTransfer> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    /** Find transfers by tenant and reference transaction status (pending approval). */
    @Query(
            "SELECT t FROM InterBranchTransfer t WHERE t.tenant.id = :tenantId "
                    + "AND t.referenceTransaction.status = 'PENDING_APPROVAL' "
                    + "ORDER BY t.createdAt DESC")
    List<InterBranchTransfer> findPendingApprovalByTenantId(@Param("tenantId") Long tenantId);

    /** Find unsettled or failed transfers for reconciliation. */
    @Query(
            "SELECT t FROM InterBranchTransfer t WHERE t.tenant.id = :tenantId "
                    + "AND t.status NOT IN ('SETTLED') "
                    + "ORDER BY t.status, t.createdAt DESC")
    List<InterBranchTransfer> findUnsettledByTenantId(@Param("tenantId") Long tenantId);

    // ===== Eager-fetch query for IBT detail screen (N+1 prevention) =====

    /**
     * Fetch a single InterBranchTransfer with all associations eagerly loaded in one query.
     * Eliminates N+1 for the detail screen by JOIN FETCHing fromBranch, toBranch,
     * referenceTransaction, createdBy, and approvedBy.
     */
    @Query(
            "SELECT DISTINCT ibt FROM InterBranchTransfer ibt "
                    + "LEFT JOIN FETCH ibt.fromBranch "
                    + "LEFT JOIN FETCH ibt.toBranch "
                    + "LEFT JOIN FETCH ibt.referenceTransaction "
                    + "LEFT JOIN FETCH ibt.createdBy "
                    + "LEFT JOIN FETCH ibt.approvedBy "
                    + "LEFT JOIN FETCH ibt.tenant "
                    + "WHERE ibt.id = :id")
    Optional<InterBranchTransfer> findByIdWithGraph(@Param("id") Long id);

    /**
     * Find the IBT record linked to a specific reference transaction.
     * Used when redirecting from POST /ibt/create (which returns a Transaction ID).
     */
    @Query(
            "SELECT ibt FROM InterBranchTransfer ibt "
                    + "WHERE ibt.referenceTransaction.id = :transactionId "
                    + "AND ibt.tenant.id = :tenantId")
    Optional<InterBranchTransfer> findByReferenceTransactionIdAndTenantId(
            @Param("transactionId") Long transactionId, @Param("tenantId") Long tenantId);

    // ===== Paginated queries for IBT list screen =====

    /** Paginated tenant-scoped listing (default sort: createdAt desc). */
    Page<InterBranchTransfer> findByTenantId(Long tenantId, Pageable pageable);

    /** Paginated filter by status. */
    Page<InterBranchTransfer> findByTenantIdAndStatus(
            Long tenantId, InterBranchTransferStatus status, Pageable pageable);

    /** Paginated filter by business date. */
    Page<InterBranchTransfer> findByTenantIdAndBusinessDate(
            Long tenantId, LocalDate businessDate, Pageable pageable);

    /** Paginated filter by source branch. */
    Page<InterBranchTransfer> findByTenantIdAndFromBranchId(
            Long tenantId, Long fromBranchId, Pageable pageable);

    /** Paginated filter by destination branch. */
    Page<InterBranchTransfer> findByTenantIdAndToBranchId(
            Long tenantId, Long toBranchId, Pageable pageable);

    /** Paginated filter by status + business date. */
    Page<InterBranchTransfer> findByTenantIdAndStatusAndBusinessDate(
            Long tenantId, InterBranchTransferStatus status, LocalDate businessDate,
            Pageable pageable);

    /** Paginated filter by status + source branch. */
    Page<InterBranchTransfer> findByTenantIdAndStatusAndFromBranchId(
            Long tenantId, InterBranchTransferStatus status, Long fromBranchId, Pageable pageable);

    /** Paginated filter by status + destination branch. */
    Page<InterBranchTransfer> findByTenantIdAndStatusAndToBranchId(
            Long tenantId, InterBranchTransferStatus status, Long toBranchId, Pageable pageable);
}
