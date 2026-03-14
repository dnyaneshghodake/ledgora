package com.ledgora.teller.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.*;

/**
 * Request DTO for teller cash deposit or withdrawal. Includes denomination breakdown which must sum
 * to the transaction amount.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TellerCashRequest {

    /** Customer account number (destination for deposit, source for withdrawal). */
    private String accountNumber;

    /** Transaction amount. Must equal SUM(denomination entries). */
    private BigDecimal amount;

    /** Denomination breakdown — immutable after posting. */
    private List<DenominationEntry> denominations;

    /** Optional narration / description. */
    private String narration;

    /** Optional client reference for idempotency. */
    private String clientReferenceId;
}
