package com.ledgora.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionDTO {
    private Long id;
    private String transactionRef;

    @NotNull(message = "Transaction type is required")
    private String transactionType;

    private String status;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String currency;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String description;
    private String narration;
    private String createdAt;

    // PART 2: Transaction channel and client reference for idempotency
    private String channel;
    private String clientReferenceId;
}
