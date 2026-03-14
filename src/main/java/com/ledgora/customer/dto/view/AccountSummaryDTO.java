package com.ledgora.customer.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account summary for Customer 360° View — Accounts tab. Populated from Account entity +
 * BalanceEngineService (ledger-derived).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountSummaryDTO {

    private Long accountId;
    private String accountNumber;
    private String accountName;
    private String branchCode;
    private String status;
    private BigDecimal ledgerBalance;
    private BigDecimal availableBalance;
    private BigDecimal lienAmount;
    private String freezeLevel;
    private String accountType;
    private String currency;
    private LocalDateTime lastTransactionDate;

    /** Last 5 transactions for expandable row (eager-loaded, no lazy). */
    private List<TransactionSummaryDTO> recentTransactions;
}
