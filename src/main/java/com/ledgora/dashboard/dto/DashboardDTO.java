package com.ledgora.dashboard.dto;

import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {
    private long totalAccounts;
    private long activeAccounts;
    private long totalTransactions;
    private long todayTransactions;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal totalTransfers;
    private long totalUsers;
    private long pendingSettlements;
    private long completedSettlements;

    // Maker-checker approval queue counts
    private long pendingApprovals;
    private long pendingTransactionApprovals;
}
