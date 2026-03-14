package com.ledgora.teller.repository;

import com.ledgora.teller.entity.TellerSessionDenomination;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TellerSessionDenominationRepository
        extends JpaRepository<TellerSessionDenomination, Long> {

    List<TellerSessionDenomination> findBySessionId(Long sessionId);

    List<TellerSessionDenomination> findBySessionIdAndEventType(Long sessionId, String eventType);
}
