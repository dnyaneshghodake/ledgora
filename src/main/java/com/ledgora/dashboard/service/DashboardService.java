package com.ledgora.dashboard.service;

import com.ledgora.dashboard.dto.DashboardDTO;
import com.ledgora.account.service.AccountService;
import com.ledgora.auth.service.AuthService;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.SettlementStatus;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.settlement.service.SettlementService;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.service.TransactionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardService {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AuthService authService;
    private final SettlementService settlementService;

    public DashboardService(AccountService accountService, TransactionService transactionService,
                            AuthService authService, SettlementService settlementService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.authService = authService;
        this.settlementService = settlementService;
    }

    public DashboardDTO getDashboardData() {
        List<Transaction> todayTxns = transactionService.getTodayTransactions();

        BigDecimal totalDeposits = todayTxns.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEPOSIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWithdrawals = todayTxns.stream()
                .filter(t -> t.getTransactionType() == TransactionType.WITHDRAWAL)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTransfers = todayTxns.stream()
                .filter(t -> t.getTransactionType() == TransactionType.TRANSFER)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardDTO.builder()
                .totalAccounts(accountService.countAll())
                .activeAccounts(accountService.countByStatus(AccountStatus.ACTIVE))
                .totalTransactions(transactionService.countAll())
                .todayTransactions(todayTxns.size())
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .totalTransfers(totalTransfers)
                .totalUsers(authService.getAllUsers().size())
                .pendingSettlements(settlementService.countByStatus(SettlementStatus.PENDING))
                .completedSettlements(settlementService.countByStatus(SettlementStatus.COMPLETED))
                .build();
    }
}
