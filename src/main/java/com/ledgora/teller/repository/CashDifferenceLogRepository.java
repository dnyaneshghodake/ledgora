package com.ledgora.teller.repository;

import com.ledgora.teller.entity.CashDifferenceLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashDifferenceLogRepository extends JpaRepository<CashDifferenceLog, Long> {

    List<CashDifferenceLog> findBySessionId(Long sessionId);

    List<CashDifferenceLog> findBySessionIdAndResolvedFlagFalse(Long sessionId);
}
