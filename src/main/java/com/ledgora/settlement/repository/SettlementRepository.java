package com.ledgora.settlement.repository;

import com.ledgora.settlement.entity.Settlement;
import com.ledgora.common.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findBySettlementRef(String settlementRef);
    List<Settlement> findByBusinessDate(LocalDate date);
    List<Settlement> findByStatus(SettlementStatus status);
    List<Settlement> findByBusinessDateBetween(LocalDate startDate, LocalDate endDate);
}
