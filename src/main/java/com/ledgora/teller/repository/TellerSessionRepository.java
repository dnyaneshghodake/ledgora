package com.ledgora.teller.repository;

import com.ledgora.common.enums.TellerStatus;
import com.ledgora.teller.entity.TellerSession;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TellerSessionRepository extends JpaRepository<TellerSession, Long> {

    Optional<TellerSession> findByTellerIdAndBusinessDate(Long tellerId, LocalDate businessDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TellerSession s WHERE s.id = :id")
    Optional<TellerSession> findByIdWithLock(@Param("id") Long id);

    List<TellerSession> findByBranchIdAndBusinessDate(Long branchId, LocalDate businessDate);

    List<TellerSession> findByTenantIdAndBusinessDate(Long tenantId, LocalDate businessDate);

    Optional<TellerSession> findByTellerIdAndBusinessDateAndStateIn(
            Long tellerId, LocalDate businessDate, List<TellerStatus> states);

    long countByBranchIdAndBusinessDateAndState(
            Long branchId, LocalDate businessDate, TellerStatus state);
}
