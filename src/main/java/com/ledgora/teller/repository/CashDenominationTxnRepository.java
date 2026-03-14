package com.ledgora.teller.repository;

import com.ledgora.teller.entity.CashDenominationTxn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashDenominationTxnRepository extends JpaRepository<CashDenominationTxn, Long> {

    List<CashDenominationTxn> findByTransactionId(Long transactionId);

    List<CashDenominationTxn> findBySessionId(Long sessionId);
}
