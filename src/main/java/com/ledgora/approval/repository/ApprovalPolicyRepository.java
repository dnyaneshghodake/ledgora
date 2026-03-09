package com.ledgora.approval.repository;

import com.ledgora.approval.entity.ApprovalPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalPolicyRepository extends JpaRepository<ApprovalPolicy, Long> {

    /**
     * Find active policies matching tenant + transaction type + specific channel.
     * Ordered by most specific first (exact channel match before wildcard).
     */
    @Query("SELECT p FROM ApprovalPolicy p WHERE p.tenant.id = :tenantId " +
           "AND p.transactionType = :txnType " +
           "AND (p.channel = :channel OR p.channel = '*') " +
           "AND p.isActive = true " +
           "ORDER BY CASE WHEN p.channel = :channel THEN 0 ELSE 1 END, p.maxAmount ASC NULLS LAST")
    List<ApprovalPolicy> findMatchingPolicies(
            @Param("tenantId") Long tenantId,
            @Param("txnType") String transactionType,
            @Param("channel") String channel);

    List<ApprovalPolicy> findByTenant_IdAndIsActiveTrue(Long tenantId);

    List<ApprovalPolicy> findByTenant_Id(Long tenantId);
}
