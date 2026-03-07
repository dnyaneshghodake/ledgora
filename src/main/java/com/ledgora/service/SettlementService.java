package com.ledgora.service;

import com.ledgora.model.Settlement;
import com.ledgora.model.Transaction;
import com.ledgora.model.User;
import com.ledgora.model.enums.SettlementStatus;
import com.ledgora.model.enums.TransactionStatus;
import com.ledgora.repository.SettlementRepository;
import com.ledgora.repository.TransactionRepository;
import com.ledgora.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);
    private final SettlementRepository settlementRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public SettlementService(SettlementRepository settlementRepository,
                             TransactionRepository transactionRepository,
                             UserRepository userRepository) {
        this.settlementRepository = settlementRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Settlement processSettlement(LocalDate date) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Transaction> transactions = transactionRepository.findByStatusAndDateRange(
                TransactionStatus.COMPLETED, startOfDay, endOfDay);

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (Transaction txn : transactions) {
            switch (txn.getTransactionType()) {
                case DEPOSIT:
                    totalCredit = totalCredit.add(txn.getAmount());
                    break;
                case WITHDRAWAL:
                    totalDebit = totalDebit.add(txn.getAmount());
                    break;
                case TRANSFER:
                    totalDebit = totalDebit.add(txn.getAmount());
                    totalCredit = totalCredit.add(txn.getAmount());
                    break;
                default:
                    break;
            }
        }

        String ref = "SET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Settlement settlement = Settlement.builder()
                .settlementRef(ref)
                .settlementDate(date)
                .status(SettlementStatus.COMPLETED)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .netAmount(totalCredit.subtract(totalDebit))
                .transactionCount(transactions.size())
                .remarks("EOD Settlement for " + date)
                .processedBy(currentUser)
                .completedAt(LocalDateTime.now())
                .build();

        Settlement saved = settlementRepository.save(settlement);
        log.info("Settlement processed: {} for date {} with {} transactions", ref, date, transactions.size());
        return saved;
    }

    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAll();
    }

    public Optional<Settlement> getSettlementById(Long id) {
        return settlementRepository.findById(id);
    }

    public Optional<Settlement> getSettlementByRef(String ref) {
        return settlementRepository.findBySettlementRef(ref);
    }

    public List<Settlement> getSettlementsByDate(LocalDate date) {
        return settlementRepository.findBySettlementDate(date);
    }

    public List<Settlement> getSettlementsByStatus(SettlementStatus status) {
        return settlementRepository.findByStatus(status);
    }

    public List<Settlement> getSettlementsByDateRange(LocalDate start, LocalDate end) {
        return settlementRepository.findBySettlementDateBetween(start, end);
    }

    public long countByStatus(SettlementStatus status) {
        return settlementRepository.findByStatus(status).size();
    }
}
