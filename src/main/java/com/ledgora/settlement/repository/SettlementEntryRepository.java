package com.ledgora.settlement.repository;

import com.ledgora.settlement.entity.SettlementEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementEntryRepository extends JpaRepository<SettlementEntry, Long> {
    List<SettlementEntry> findBySettlementId(Long settlementId);

    List<SettlementEntry> findByAccountId(Long accountId);
}
