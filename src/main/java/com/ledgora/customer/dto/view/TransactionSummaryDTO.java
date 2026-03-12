package com.ledgora.customer.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transaction summary for Customer 360° View — Transactions tab. Populated from Transaction entity
 * with tenant isolation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSummaryDTO {

    private Long transactionId;
    private String transactionRef;
    private String transactionType;
    private String channel;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDate businessDate;
    private String makerUsername;
    private String checkerUsername;
    private LocalDateTime createdAt;
    private String description;
}
