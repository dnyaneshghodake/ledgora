package com.ledgora.batch.repository;

import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.common.enums.BatchStatus;
import com.ledgora.common.enums.BatchType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionBatchRepository extends JpaRepository<TransactionBatch, Long> {

    @Query(
            "SELECT tb FROM TransactionBatch tb WHERE tb.tenant.id = :tenantId AND tb.batchType = :batchType AND tb.businessDate = :businessDate AND tb.status = :status")
    Optional<TransactionBatch> findByTenantIdAndBatchTypeAndBusinessDateAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("batchType") BatchType batchType,
            @Param("businessDate") LocalDate businessDate,
            @Param("status") BatchStatus status);

    @Query(
            "SELECT tb FROM TransactionBatch tb WHERE tb.tenant.id = :tenantId AND tb.businessDate = :businessDate")
    List<TransactionBatch> findByTenantIdAndBusinessDate(
            @Param("tenantId") Long tenantId, @Param("businessDate") LocalDate businessDate);

    @Query(
            "SELECT tb FROM TransactionBatch tb WHERE tb.tenant.id = :tenantId AND tb.status = :status")
    List<TransactionBatch> findByTenantIdAndStatus(
            @Param("tenantId") Long tenantId, @Param("status") BatchStatus status);

    @Query("SELECT tb FROM TransactionBatch tb WHERE tb.tenant.id = :tenantId")
    List<TransactionBatch> findByTenantId(@Param("tenantId") Long tenantId);

    /** Tenant-isolated lookup by primary key. Use instead of findById() for CBS operations. */
    @Query("SELECT tb FROM TransactionBatch tb WHERE tb.id = :id AND tb.tenant.id = :tenantId")
    Optional<TransactionBatch> findByIdAndTenantId(
            @Param("id") Long id, @Param("tenantId") Long tenantId);

    Optional<TransactionBatch> findByBatchCode(String batchCode);

    List<TransactionBatch> findByStatus(BatchStatus status);

    @Query(
            "SELECT tb FROM TransactionBatch tb WHERE tb.tenant.id = :tenantId AND tb.businessDate = :businessDate AND tb.status = :status")
    List<TransactionBatch> findByTenantIdAndBusinessDateAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("businessDate") LocalDate businessDate,
            @Param("status") BatchStatus status);
}
