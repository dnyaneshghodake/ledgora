package com.ledgora.payment.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.PaymentStatus;
import com.ledgora.idempotency.service.IdempotencyService;
import com.ledgora.payment.entity.PaymentInstruction;
import com.ledgora.payment.repository.PaymentInstructionRepository;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.service.TransactionService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PART 7: Payment instruction engine. Manages the lifecycle of payment instructions: INITIATED ->
 * AUTHORIZED -> SETTLED / FAILED. Payment instructions create ledger transactions when settled.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentInstructionRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;

    public PaymentService(
            PaymentInstructionRepository paymentRepository,
            AccountRepository accountRepository,
            TransactionService transactionService,
            IdempotencyService idempotencyService) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.idempotencyService = idempotencyService;
    }

    /** Create a new payment instruction. */
    @Transactional
    public PaymentInstruction initiatePayment(
            Long sourceAccountId,
            Long destAccountId,
            java.math.BigDecimal amount,
            String currency,
            String description,
            String idempotencyKey) {
        Account source =
                accountRepository
                        .findById(sourceAccountId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Source account not found: " + sourceAccountId));
        Account dest =
                accountRepository
                        .findById(destAccountId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Destination account not found: " + destAccountId));

        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Source account is not active");
        }
        if (dest.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Destination account is not active");
        }

        PaymentInstruction payment =
                PaymentInstruction.builder()
                        .sourceAccount(source)
                        .destinationAccount(dest)
                        .amount(amount)
                        .currency(currency != null ? currency : "INR")
                        .status(PaymentStatus.INITIATED)
                        .description(description)
                        .idempotencyKey(idempotencyKey)
                        .build();

        payment = paymentRepository.save(payment);
        log.info(
                "Payment instruction initiated: {} from {} to {} amount {}",
                payment.getId(),
                source.getAccountNumber(),
                dest.getAccountNumber(),
                amount);
        return payment;
    }

    /** Authorize a payment instruction. */
    @Transactional
    public PaymentInstruction authorizePayment(Long paymentId) {
        PaymentInstruction payment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.INITIATED) {
            throw new RuntimeException(
                    "Payment cannot be authorized. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment = paymentRepository.save(payment);
        log.info("Payment instruction authorized: {}", paymentId);
        return payment;
    }

    /** Settle a payment instruction - creates the ledger transaction. */
    @Transactional
    public PaymentInstruction settlePayment(Long paymentId) {
        PaymentInstruction payment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new RuntimeException(
                    "Payment cannot be settled. Current status: " + payment.getStatus());
        }

        try {
            // Create transaction via TransactionService
            TransactionDTO dto =
                    TransactionDTO.builder()
                            .transactionType("TRANSFER")
                            .amount(payment.getAmount())
                            .currency(payment.getCurrency())
                            .sourceAccountNumber(payment.getSourceAccount().getAccountNumber())
                            .destinationAccountNumber(
                                    payment.getDestinationAccount().getAccountNumber())
                            .description(
                                    payment.getDescription() != null
                                            ? payment.getDescription()
                                            : "Payment settlement")
                            .build();

            Transaction txn = transactionService.transfer(dto);
            payment.setTransaction(txn);
            payment.setStatus(PaymentStatus.SETTLED);
            payment = paymentRepository.save(payment);
            log.info(
                    "Payment instruction settled: {} -> transaction {}",
                    paymentId,
                    txn.getTransactionRef());

        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.error("Payment settlement failed: {} - {}", paymentId, e.getMessage());
            throw new RuntimeException("Payment settlement failed: " + e.getMessage(), e);
        }

        return payment;
    }

    /** Fail a payment instruction. */
    @Transactional
    public PaymentInstruction failPayment(Long paymentId, String reason) {
        PaymentInstruction payment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setDescription(payment.getDescription() + " | Failed: " + reason);
        payment = paymentRepository.save(payment);
        log.info("Payment instruction failed: {} - {}", paymentId, reason);
        return payment;
    }

    // Query methods
    public Optional<PaymentInstruction> getById(Long id) {
        return paymentRepository.findById(id);
    }

    public List<PaymentInstruction> getByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    public List<PaymentInstruction> getByAccount(Long accountId) {
        return paymentRepository.findByAccountId(accountId);
    }
}
