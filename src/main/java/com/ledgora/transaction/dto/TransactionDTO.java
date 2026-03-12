package com.ledgora.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {
    private Long id;
    private String transactionRef;

    @NotNull(message = "Transaction type is required")
    private String transactionType;

    private String status;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @Size(max = 3, min = 3, message = "Currency must be a 3-character ISO code")
    private String currency;

    @Size(max = 30, message = "Account number must not exceed 30 characters")
    private String sourceAccountNumber;

    @Size(max = 30, message = "Account number must not exceed 30 characters")
    private String destinationAccountNumber;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @Size(max = 500, message = "Narration must not exceed 500 characters")
    private String narration;
    private String createdAt;

    // PART 2: Transaction channel and client reference for idempotency
    private String channel;

    @Size(max = 100, message = "Client reference ID must not exceed 100 characters")
    private String clientReferenceId;
}
