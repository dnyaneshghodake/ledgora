package com.ledgora.settlement.repository;

import com.ledgora.settlement.entity.SettlementEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SettlementEntryRepository extends JpaRepository<SettlementEntry, Long> {
    List<SettlementEntry> findBySettlementId(Long settlementId);
    List<SettlementEntry> findByAccountId(Long accountId);
}
