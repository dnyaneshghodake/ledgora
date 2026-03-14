package com.ledgora.teller.service;

import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.common.enums.TellerStatus;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.teller.dto.DenominationEntry;
import com.ledgora.teller.dto.TellerCashRequest;
import com.ledgora.teller.entity.CashDenominationTxn;
import com.ledgora.teller.entity.TellerMaster;
import com.ledgora.teller.entity.TellerSession;
import com.ledgora.teller.repository.CashDenominationTxnRepository;
import com.ledgora.teller.repository.TellerMasterRepository;
import com.ledgora.teller.repository.TellerSessionRepository;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CashEngineService  Finacle-style teller cash wrapper around the existing TransactionService.
 *
 * <p>CRITICAL: This service does NOT rewrite the transaction engine or ledger posting. It performs
 * teller/session validations, denomination validation, operational balance updates, and then calls
 * the existing TransactionService.deposit()/withdraw() for accounting.
 */
@Service
public class CashEngineService {

    private static final Logger log = LoggerFactory.getLogger(CashEngineService.class);

    private final TenantService tenantService;
    private final TransactionService transactionService;
    private final TellerMasterRepository tellerMasterRepository;
    private final TellerSessionRepository tellerSessionRepository;
    private final CashDenominationTxnRepository cashDenominationTxnRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CashEngineService(
            TenantService tenantService,
            TransactionService transactionService,
            TellerMasterRepository tellerMasterRepository,
            TellerSessionRepository tellerSessionRepository,
            CashDenominationTxnRepository cashDenominationTxnRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.tenantService = tenantService;
        this.transactionService = transactionService;
        this.tellerMasterRepository = tellerMasterRepository;
        this.tellerSessionRepository = tellerSessionRepository;
        this.cashDenominationTxnRepository = cashDenominationTxnRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /** Cash deposit via teller. Allowed ONLY when teller session state == OPEN. */
    @Transactional
    public Transaction cashDeposit(TellerCashRequest req) {
        validateRequest(req);

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);
        LocalDate bizDate = tenantService.getCurrentBusinessDate(tenantId);

        User maker = requireCurrentUser();
        TellerMaster tellerMaster = requireTellerMaster(maker.getId(), tenantId);
        TellerSession session = requireOpenSessionWithLock(tellerMaster.getId(), bizDate);

        // RBI strict gate: only OPEN
        if (session.getState() != TellerStatus.OPEN) {
            throw new BusinessException(
                    "INVALID_TELLER_STATE",
                    "Cash deposit allowed only when teller session is OPEN. Current state: "
                            + session.getState());
        }

        BigDecimal denomTotal = validateAndSumDenominations(req.getDenominations());
        if (denomTotal.compareTo(req.getAmount()) != 0) {
            throw new BusinessException(
                    "DENOMINATION_MISMATCH",
                    "Denomination total " + denomTotal + " does not match amount " + req.getAmount());
        }

        // Limits: single deposit
        if (req.getAmount().compareTo(tellerMaster.getSingleTxnLimitDeposit()) > 0) {
            throw new BusinessException(
                    "TELLER_LIMIT_EXCEEDED",
                    "Deposit exceeds single deposit limit. Limit="
                            + tellerMaster.getSingleTxnLimitDeposit()
                            + " amount="
                            + req.getAmount());
        }

        // Daily limit: totalCreditToday + amount <= dailyTxnLimit
        BigDecimal newTotalCredit = session.getTotalCreditToday().add(req.getAmount());
        if (newTotalCredit.compareTo(tellerMaster.getDailyTxnLimit()) > 0) {
            throw new BusinessException(
                    "TELLER_DAILY_LIMIT_EXCEEDED",
                    "Daily limit exceeded. Limit="
                            + tellerMaster.getDailyTxnLimit()
                            + " wouldBeTotalCredit="
                            + newTotalCredit);
        }

        // Create transaction via existing engine (DO NOT change posting)
        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType(TransactionType.DEPOSIT.name())
                        .destinationAccountNumber(req.getAccountNumber())
                        .amount(req.getAmount())
                        .currency(tenant.getBaseCurrency())
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId(req.getClientReferenceId())
                        .description("Cash Deposit")
                        .narration(req.getNarration())
                        .build();

        Transaction txn = transactionService.deposit(dto);

        // Persist denomination breakdown (immutable) linked to transaction + teller session
        persistDenominationTxns(txn, session, req.getDenominations());

        // Update teller operational balances
        session.setCurrentBalance(session.getCurrentBalance().add(req.getAmount()));
        session.setTotalCreditToday(newTotalCredit);
        tellerSessionRepository.save(session);

        // Cash holding limit check (suggestion only; do not auto-transfer)
        if (session.getCurrentBalance().compareTo(tellerMaster.getCashHoldingLimit()) > 0) {
            log.warn(
                    "Teller cash holding limit exceeded: tellerId={}, sessionId={}, limit={}, current={}",
                    tellerMaster.getId(),
                    session.getId(),
                    tellerMaster.getCashHoldingLimit(),
                    session.getCurrentBalance());
        }

        auditService.logEvent(
                maker.getId(),
                "TELLER_CASH_DEPOSIT",
                "TELLER_SESSION",
                session.getId(),
                "Cash deposit posted. txnId="
                        + txn.getId()
                        + " amount="
                        + req.getAmount()
                        + " account="
                        + req.getAccountNumber(),
                null);

        return txn;
    }

    /** Cash withdrawal via teller. Allowed ONLY when teller session state == OPEN. */
    @Transactional
    public Transaction cashWithdrawal(TellerCashRequest req) {
        validateRequest(req);

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);
        LocalDate bizDate = tenantService.getCurrentBusinessDate(tenantId);

        User maker = requireCurrentUser();
        TellerMaster tellerMaster = requireTellerMaster(maker.getId(), tenantId);
        TellerSession session = requireOpenSessionWithLock(tellerMaster.getId(), bizDate);

        // RBI strict gate: only OPEN
        if (session.getState() != TellerStatus.OPEN) {
            throw new BusinessException(
                    "INVALID_TELLER_STATE",
                    "Cash withdrawal allowed only when teller session is OPEN. Current state: "
                            + session.getState());
        }

        BigDecimal denomTotal = validateAndSumDenominations(req.getDenominations());
        if (denomTotal.compareTo(req.getAmount()) != 0) {
            throw new BusinessException(
                    "DENOMINATION_MISMATCH",
                    "Denomination total " + denomTotal + " does not match amount " + req.getAmount());
        }

        // Limits: single withdrawal
        if (req.getAmount().compareTo(tellerMaster.getSingleTxnLimitWithdrawal()) > 0) {
            throw new BusinessException(
                    "TELLER_LIMIT_EXCEEDED",
                    "Withdrawal exceeds single withdrawal limit. Limit="
                            + tellerMaster.getSingleTxnLimitWithdrawal()
                            + " amount="
                            + req.getAmount());
        }

        // Teller cash must be sufficient
        if (session.getCurrentBalance().compareTo(req.getAmount()) < 0) {
            throw new BusinessException(
                    "TELLER_CASH_INSUFFICIENT",
                    "Insufficient teller cash. Available="
                            + session.getCurrentBalance()
                            + " amount="
                            + req.getAmount());
        }

        // Daily limit: totalDebitToday + amount <= dailyTxnLimit
        BigDecimal newTotalDebit = session.getTotalDebitToday().add(req.getAmount());
        if (newTotalDebit.compareTo(tellerMaster.getDailyTxnLimit()) > 0) {
            throw new BusinessException(
                    "TELLER_DAILY_LIMIT_EXCEEDED",
                    "Daily limit exceeded. Limit="
                            + tellerMaster.getDailyTxnLimit()
                            + " wouldBeTotalDebit="
                            + newTotalDebit);
        }

        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType(TransactionType.WITHDRAWAL.name())
                        .sourceAccountNumber(req.getAccountNumber())
                        .amount(req.getAmount())
                        .currency(tenant.getBaseCurrency())
                        .channel(TransactionChannel.TELLER.name())
                        .clientReferenceId(req.getClientReferenceId())
                        .description("Cash Withdrawal")
                        .narration(req.getNarration())
                        .build();

        Transaction txn = transactionService.withdraw(dto);

        persistDenominationTxns(txn, session, req.getDenominations());

        // Update teller operational balances
        session.setCurrentBalance(session.getCurrentBalance().subtract(req.getAmount()));
        session.setTotalDebitToday(newTotalDebit);
        tellerSessionRepository.save(session);

        auditService.logEvent(
                maker.getId(),
                "TELLER_CASH_WITHDRAWAL",
                "TELLER_SESSION",
                session.getId(),
                "Cash withdrawal posted. txnId="
                        + txn.getId()
                        + " amount="
                        + req.getAmount()
                        + " account="
                        + req.getAccountNumber(),
                null);

        return txn;
    }

    private void validateRequest(TellerCashRequest req) {
        if (req == null) {
            throw new BusinessException("INVALID_REQUEST", "Request must not be null");
        }
        if (req.getAccountNumber() == null || req.getAccountNumber().isBlank()) {
            throw new BusinessException("INVALID_REQUEST", "Account number is required");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_REQUEST", "Amount must be positive");
        }
        if (req.getDenominations() == null || req.getDenominations().isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "Denominations are required");
        }
    }

    private BigDecimal validateAndSumDenominations(List<DenominationEntry> entries) {
        BigDecimal sum = BigDecimal.ZERO;
        for (DenominationEntry e : entries) {
            if (e == null || e.getDenominationValue() == null) {
                throw new BusinessException("INVALID_DENOMINATION", "Denomination value is required");
            }
            if (e.getCount() == null || e.getCount() < 0) {
                throw new BusinessException(
                        "INVALID_DENOMINATION", "Denomination count must be >= 0");
            }
            }             
            sum = sum.add(e.getDenominationValue().multiply(new BigDecimal(e.getCount())));
        }
        return sum;
    }

    private void persistDenominationTxns(
            Transaction transaction, TellerSession session, List<DenominationEntry> denominations) {
        for (DenominationEntry e : denominations) {
            if (e.getCount() == null || e.getCount() == 0) {
                continue;
            }
            CashDenominationTxn row =
                    CashDenominationTxn.builder()
                            .transaction(transaction)
                            .session(session)
                            .denominationValue(e.getDenominationValue())
                            .count(e.getCount())
                            // totalAmount computed in @PrePersist
                            .totalAmount(BigDecimal.ZERO)
                            .build();
            cashDenominationTxnRepository.save(row);
        }
    }

    private TellerSession requireOpenSessionWithLock(Long tellerId, LocalDate businessDate) {
        // Lock not perfect here (unique index enforces uniqueness). We lock by reading the session
        // row; updates are then versioned.
        TellerSession session =
                tellerSessionRepository
                        .findByTellerIdAndBusinessDate(tellerId, businessDate)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "NO_TELLER_SESSION",
                                                "No teller session found for business date "
                                                        + businessDate));
        // Lock row for balance updates
        return tellerSessionRepository
                .findByIdWithLock(session.getId())
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        "NO_TELLER_SESSION",
                                        "No teller session found: " + session.getId()));
    }

    private TellerMaster requireTellerMaster(Long userId, Long tenantId) {
        TellerMaster tm =
                tellerMasterRepository
                        .findByUserIdAndTenantId(userId, tenantId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "TELLER_NOT_CONFIGURED",
                                                "No teller master configured for user " + userId));
        if (tm.getActiveFlag() == null || !tm.getActiveFlag()) {
            throw new BusinessException(
                    "TELLER_INACTIVE", "Teller is inactive for user " + userId);
        }
        return tm;
    }

    private User requireCurrentUser() {
        try {
            org.springframework.security.core.Authentication auth =
                    SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                throw new BusinessException("IDENTITY_REQUIRED", "Authentication is missing");
            }
            User u = userRepository.findByUsername(auth.getName()).orElse(null);
            if (u == null) {
                throw new BusinessException(
                        "IDENTITY_REQUIRED", "User not found for principal: " + auth.getName());
            }
            return u;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("IDENTITY_REQUIRED", "Failed to resolve current user");
        }
    }

    private Long requireTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }
}
