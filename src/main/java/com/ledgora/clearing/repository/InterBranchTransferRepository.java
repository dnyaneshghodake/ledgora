package com.ledgora.clearing.repository;

import com.ledgora.clearing.entity.InterBranchTransfer;
import com.ledgora.common.enums.InterBranchTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InterBranchTransferRepository extends JpaRepository<InterBranchTransfer, Long> {

    List<InterBranchTransfer> findByTenantIdAndStatus(Long tenantId, InterBranchTransferStatus status);

    List<InterBranchTransfer> findByTenantIdAndBusinessDateAndStatus(
            Long tenantId, LocalDate businessDate, InterBranchTransferStatus status);

    @Query("SELECT COUNT(t) FROM InterBranchTransfer t WHERE t.tenant.id = :tenantId " +
           "AND t.businessDate = :businessDate AND t.status NOT IN ('SETTLED', 'FAILED')")
    long countUnsettledByTenantAndDate(@Param("tenantId") Long tenantId,
                                       @Param("businessDate") LocalDate businessDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM InterBranchTransfer t WHERE t.tenant.id = :tenantId " +
           "AND t.businessDate = :businessDate AND t.status = 'SENT'")
    BigDecimal sumSentAmountByTenantAndDate(@Param("tenantId") Long tenantId,
                                            @Param("businessDate") LocalDate businessDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM InterBranchTransfer t WHERE t.tenant.id = :tenantId " +
           "AND t.businessDate = :businessDate AND t.status = 'RECEIVED'")
    BigDecimal sumReceivedAmountByTenantAndDate(@Param("tenantId") Long tenantId,
                                                @Param("businessDate") LocalDate businessDate);

    List<InterBranchTransfer> findByTenantIdAndToBranchIdAndStatus(
            Long tenantId, Long toBranchId, InterBranchTransferStatus status);
}
