package com.ledgora.repository;

import com.ledgora.model.Settlement;
import com.ledgora.model.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findBySettlementRef(String settlementRef);

    List<Settlement> findBySettlementDate(LocalDate date);

    List<Settlement> findByStatus(SettlementStatus status);

    List<Settlement> findBySettlementDateBetween(LocalDate startDate, LocalDate endDate);
}
