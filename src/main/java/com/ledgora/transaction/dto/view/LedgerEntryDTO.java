package com.ledgora.transaction.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Immutable ledger entry for Transaction 360° View. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryDTO {
    private Long id;
    private Long journalId;
    private String entryType;
    private String glCode;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDate businessDate;
    private Long batchId;
    private Long voucherId;
    private Long reversalOfEntryId;
    private String narration;
    private LocalDateTime postingTime;
}
