package com.ledgora.payment.repository;

import com.ledgora.common.enums.PaymentStatus;
import com.ledgora.payment.entity.PaymentInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentInstructionRepository extends JpaRepository<PaymentInstruction, Long> {
    List<PaymentInstruction> findByStatus(PaymentStatus status);
    Optional<PaymentInstruction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT pi FROM PaymentInstruction pi WHERE pi.sourceAccount.id = :accountId OR pi.destinationAccount.id = :accountId ORDER BY pi.createdAt DESC")
    List<PaymentInstruction> findByAccountId(@Param("accountId") Long accountId);

    List<PaymentInstruction> findBySourceAccountId(Long sourceAccountId);
    List<PaymentInstruction> findByDestinationAccountId(Long destinationAccountId);
}
