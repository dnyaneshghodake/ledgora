package com.ledgora.clearing.repository;

import com.ledgora.clearing.entity.InterBranchTransfer;
import com.ledgora.common.enums.InterBranchTransferStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InterBranchTransferRepository extends JpaRepository<InterBranchTransfer, Long> {

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
    List<InterBranchTransfer> findByTenantIdAndBusinessDate(
            Long tenantId, LocalDate businessDate);

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
}
