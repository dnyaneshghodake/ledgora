package com.ledgora.teller.repository;

import com.ledgora.teller.entity.CashDenominationMaster;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashDenominationMasterRepository
        extends JpaRepository<CashDenominationMaster, Long> {

    Optional<CashDenominationMaster> findByDenominationValue(BigDecimal denominationValue);

    List<CashDenominationMaster> findByActiveTrueOrderBySortOrderAsc();
}
