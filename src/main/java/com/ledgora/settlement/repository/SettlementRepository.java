package com.ledgora.settlement.repository;

import com.ledgora.common.enums.SettlementStatus;
import com.ledgora.settlement.entity.Settlement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findBySettlementRef(String settlementRef);

    List<Settlement> findByBusinessDate(LocalDate date);

    List<Settlement> findByStatus(SettlementStatus status);

    List<Settlement> findByBusinessDateBetween(LocalDate startDate, LocalDate endDate);
}
