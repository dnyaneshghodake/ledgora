package com.ledgora.interest.repository;

import com.ledgora.interest.entity.InterestAccrualLog;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InterestAccrualLogRepository extends JpaRepository<InterestAccrualLog, Long> {

    boolean existsByTenantIdAndAccountIdAndBusinessDate(
            Long tenantId, Long accountId, LocalDate businessDate);

    @Query(
            "SELECT ial FROM InterestAccrualLog ial WHERE ial.tenantId = :tenantId AND ial.businessDate = :date")
    List<InterestAccrualLog> findByTenantIdAndBusinessDate(
            @Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    long countByTenantIdAndBusinessDate(Long tenantId, LocalDate businessDate);
}
