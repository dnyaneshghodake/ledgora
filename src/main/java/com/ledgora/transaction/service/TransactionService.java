package com.ledgora.transaction.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.service.BatchService;
import com.ledgora.branch.entity.Branch;
import com.ledgora.calendar.service.BankCalendarService;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.EntryType;
import com.ledgora.common.enums.FreezeLevel;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.enums.TransactionStatus;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.common.enums.VoucherDrCr;
import com.ledgora.events.TransactionCreatedEvent;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.idempotency.service.IdempotencyService;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.entity.TransactionLine;
import com.ledgora.transaction.repository.TransactionLineRepository;
import com.ledgora.transaction.repository.TransactionRepository;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction service with CBS-grade maker-checker approval workflow.
 *
 * <p>Flow: 1. Maker initiates transaction (deposit/withdraw/transfer) 2. ApprovalPolicyService
 * decides: auto-authorize or PENDING_APPROVAL 3. If auto-authorized: post immediately (vouchers +
 * ledger + balances) 4. If PENDING_APPROVAL: save transaction, create ApprovalRequest, wait for
 * checker 5. Checker approves: re-validate, then post 6. Checker rejects: mark REJECTED, no posting
 *
 * <p>Governance overrides (always require approval regardless of policy): - Reversals - Backdated
 * entries
 *
 * <p>System-only (always auto-authorize via BATCH channel): - Interest accrual, charges, EOD
 * adjustments
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionLineRepository transactionLineRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final GeneralLedgerRepository glRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final VoucherService voucherService;
    private final ApplicationEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final TenantService tenantService;
    private final BatchService batchService;
    private final BankCalendarService bankCalendarService;
    private final com.ledgora.approval.service.ApprovalPolicyService approvalPolicyService;
    private final com.ledgora.approval.service.ApprovalService approvalService;
    private final com.ledgora.clearing.service.InterBranchClearingService
            interBranchClearingService;
    private final com.ledgora.clearing.service.IbtService ibtService;
    private final com.ledgora.approval.service.HardTransactionCeilingService
            hardTransactionCeilingService;
    private final com.ledgora.fraud.service.VelocityFraudEngine velocityFraudEngine;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionLineRepository transactionLineRepository,
            AccountRepository accountRepository,
            AccountBalanceRepository accountBalanceRepository,
            LedgerEntryRepository ledgerEntryRepository,
            GeneralLedgerRepository glRepository,
            UserRepository userRepository,
            AuditService auditService,
            VoucherService voucherService,
            ApplicationEventPublisher eventPublisher,
            IdempotencyService idempotencyService,
            TenantService tenantService,
            BatchService batchService,
            BankCalendarService bankCalendarService,
            com.ledgora.approval.service.ApprovalPolicyService approvalPolicyService,
            com.ledgora.approval.service.ApprovalService approvalService,
            com.ledgora.clearing.service.InterBranchClearingService interBranchClearingService,
            com.ledgora.clearing.service.IbtService ibtService,
            com.ledgora.approval.service.HardTransactionCeilingService
                    hardTransactionCeilingService,
            com.ledgora.fraud.service.VelocityFraudEngine velocityFraudEngine) {
        this.transactionRepository = transactionRepository;
        this.transactionLineRepository = transactionLineRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.glRepository = glRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.voucherService = voucherService;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.tenantService = tenantService;
        this.batchService = batchService;
        this.bankCalendarService = bankCalendarService;
        this.approvalPolicyService = approvalPolicyService;
        this.approvalService = approvalService;
        this.interBranchClearingService = interBranchClearingService;
        this.ibtService = ibtService;
        this.hardTransactionCeilingService = hardTransactionCeilingService;
        this.velocityFraudEngine = velocityFraudEngine;
    }

    /**
     * Deposit: Maker step. Validates, creates transaction record, then consults
     * ApprovalPolicyService: - Auto-authorized -> post immediately (vouchers + ledger + balances) -
     * Requires approval -> save as PENDING_APPROVAL, create ApprovalRequest
     */
    @Transactional
    public Transaction deposit(TransactionDTO dto) {
        validateAmountPositive(dto.getAmount());

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);

        // ── HARD CEILING: absolute limit enforced BEFORE any persistence ──
        TransactionChannel channelForLimit = parseChannel(dto.getChannel());
        User currentUserForLimit = getCurrentUser();
        hardTransactionCeilingService.enforceHardCeiling(
                tenantId,
                channelForLimit,
                dto.getAmount(),
                currentUserForLimit != null ? currentUserForLimit.getId() : null);

        checkIdempotency(dto);

        Account account =
                accountRepository
                        .findByAccountNumberWithLockAndTenantId(
                                dto.getDestinationAccountNumber(), tenantId)
                        .orElseThrow(
                                () ->
                                        new com.ledgora.common.exception.BusinessException(
                                                "ACCOUNT_NOT_FOUND",
                                                "Account not found: "
                                                        + dto.getDestinationAccountNumber()));
        validateAccountActive(account);
        validateAccountFreezeLevel(account, VoucherDrCr.CR);

        User currentUser = getCurrentUser();
        // For non-BATCH channels, a human maker identity is required for audit trail and
        // maker-checker enforcement. BATCH channel is used by system/EOD processes.
        if (currentUser == null && channelForLimit != TransactionChannel.BATCH) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot initiate deposit: maker identity could not be resolved");
        }
        Long userId = currentUser != null ? currentUser.getId() : null;

        // ── VELOCITY FRAUD CHECK: proactive burst detection ──
        velocityFraudEngine.evaluateVelocity(tenant, account, dto.getAmount(), userId);

        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);

        TransactionChannel channel = parseChannel(dto.getChannel());
        validateHolidayCalendar(tenantId, businessDate, channel);

        String txnRef = generateTransactionRef("DEP");
        TransactionBatch batch = batchService.getOrCreateOpenBatch(tenantId, channel, businessDate);

        // Consult approval policy engine
        boolean autoAuth =
                approvalPolicyService.isAutoAuthorized(
                        tenantId, TransactionType.DEPOSIT, channel, dto.getAmount(), false, false);

        // Create transaction record (maker step)
        Transaction transaction =
                Transaction.builder()
                        .transactionRef(txnRef)
                        .transactionType(TransactionType.DEPOSIT)
                        .status(
                                autoAuth
                                        ? TransactionStatus.COMPLETED
                                        : TransactionStatus.PENDING_APPROVAL)
                        .amount(dto.getAmount())
                        .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                        .channel(channel)
                        .clientReferenceId(dto.getClientReferenceId())
                        .destinationAccount(account)
                        .description(
                                dto.getDescription() != null
                                        ? dto.getDescription()
                                        : "Cash Deposit")
                        .narration(dto.getNarration())
                        .businessDate(businessDate)
                        .valueDate(businessDate)
                        .performedBy(currentUser)
                        .maker(currentUser)
                        .makerTimestamp(LocalDateTime.now())
                        .tenant(tenant)
                        .batch(batch)
                        .branch(resolveBranch(account, currentUser))
                        .build();
        transaction = transactionRepository.save(transaction);

        if (autoAuth) {
            // Auto-authorized: post immediately
            postDepositLedger(transaction, account, tenant, currentUser, businessDate, batch, dto);
            auditService.logTransaction(userId, transaction.getId(), txnRef, "DEPOSIT");
            log.info(
                    "Deposit auto-authorized and posted: {} amount {} to account {}",
                    txnRef,
                    dto.getAmount(),
                    account.getAccountNumber());
        } else {
            // Requires checker approval: create ApprovalRequest, do NOT post
            approvalService.submitForApproval(
                    "TRANSACTION",
                    transaction.getId(),
                    "DEPOSIT " + dto.getAmount() + " to " + dto.getDestinationAccountNumber());
            auditService.logTransaction(
                    userId, transaction.getId(), txnRef, "DEPOSIT_PENDING_APPROVAL");
            log.info(
                    "Deposit pending approval: {} amount {} to account {}",
                    txnRef,
                    dto.getAmount(),
                    account.getAccountNumber());
        }

        return transaction;
    }

    /** Withdrawal: Maker step with approval policy check. */
    @Transactional
    public Transaction withdraw(TransactionDTO dto) {
        validateAmountPositive(dto.getAmount());

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);

        // ── HARD CEILING: absolute limit enforced BEFORE any persistence ──
        TransactionChannel channelForLimit = parseChannel(dto.getChannel());
        User currentUserForLimit = getCurrentUser();
        hardTransactionCeilingService.enforceHardCeiling(
                tenantId,
                channelForLimit,
                dto.getAmount(),
                currentUserForLimit != null ? currentUserForLimit.getId() : null);

        checkIdempotency(dto);

        Account account =
                accountRepository
                        .findByAccountNumberWithLockAndTenantId(
                                dto.getSourceAccountNumber(), tenantId)
                        .orElseThrow(
                                () ->
                                        new com.ledgora.common.exception.BusinessException(
                                                "ACCOUNT_NOT_FOUND",
                                                "Account not found: "
                                                        + dto.getSourceAccountNumber()));
        validateAccountActive(account);
        validateAccountFreezeLevel(account, VoucherDrCr.DR);
        if (account.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new com.ledgora.common.exception.InsufficientBalanceException(
                    account.getAccountNumber(),
                    "Insufficient balance in account "
                            + account.getAccountNumber()
                            + ". Available: "
                            + account.getBalance());
        }

        User currentUser = getCurrentUser();
        if (currentUser == null && channelForLimit != TransactionChannel.BATCH) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot initiate withdrawal: maker identity could not be resolved");
        }
        Long userId = currentUser != null ? currentUser.getId() : null;

        // ── VELOCITY FRAUD CHECK: proactive burst detection ──
        velocityFraudEngine.evaluateVelocity(tenant, account, dto.getAmount(), userId);

        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);

        TransactionChannel channel = parseChannel(dto.getChannel());
        validateHolidayCalendar(tenantId, businessDate, channel);

        String txnRef = generateTransactionRef("WDR");
        TransactionBatch batch = batchService.getOrCreateOpenBatch(tenantId, channel, businessDate);

        boolean autoAuth =
                approvalPolicyService.isAutoAuthorized(
                        tenantId,
                        TransactionType.WITHDRAWAL,
                        channel,
                        dto.getAmount(),
                        false,
                        false);

        Transaction transaction =
                Transaction.builder()
                        .transactionRef(txnRef)
                        .transactionType(TransactionType.WITHDRAWAL)
                        .status(
                                autoAuth
                                        ? TransactionStatus.COMPLETED
                                        : TransactionStatus.PENDING_APPROVAL)
                        .amount(dto.getAmount())
                        .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                        .channel(channel)
                        .clientReferenceId(dto.getClientReferenceId())
                        .sourceAccount(account)
                        .description(
                                dto.getDescription() != null
                                        ? dto.getDescription()
                                        : "Cash Withdrawal")
                        .narration(dto.getNarration())
                        .businessDate(businessDate)
                        .valueDate(businessDate)
                        .performedBy(currentUser)
                        .maker(currentUser)
                        .makerTimestamp(LocalDateTime.now())
                        .tenant(tenant)
                        .batch(batch)
                        .branch(resolveBranch(account, currentUser))
                        .build();
        transaction = transactionRepository.save(transaction);

        if (autoAuth) {
            postWithdrawalLedger(
                    transaction, account, tenant, currentUser, businessDate, batch, dto);
            auditService.logTransaction(userId, transaction.getId(), txnRef, "WITHDRAWAL");
            log.info(
                    "Withdrawal auto-authorized and posted: {} amount {} from account {}",
                    txnRef,
                    dto.getAmount(),
                    account.getAccountNumber());
        } else {
            approvalService.submitForApproval(
                    "TRANSACTION",
                    transaction.getId(),
                    "WITHDRAWAL " + dto.getAmount() + " from " + dto.getSourceAccountNumber());
            auditService.logTransaction(
                    userId, transaction.getId(), txnRef, "WITHDRAWAL_PENDING_APPROVAL");
            log.info(
                    "Withdrawal pending approval: {} amount {} from account {}",
                    txnRef,
                    dto.getAmount(),
                    account.getAccountNumber());
        }

        return transaction;
    }

    /** Transfer: Maker step with approval policy check. */
    @Transactional
    public Transaction transfer(TransactionDTO dto) {
        validateAmountPositive(dto.getAmount());

        Long tenantId = requireTenantId();
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);

        // ── HARD CEILING: absolute limit enforced BEFORE any persistence ──
        TransactionChannel channelForLimit = parseChannel(dto.getChannel());
        User currentUserForLimit = getCurrentUser();
        hardTransactionCeilingService.enforceHardCeiling(
                tenantId,
                channelForLimit,
                dto.getAmount(),
                currentUserForLimit != null ? currentUserForLimit.getId() : null);

        checkIdempotency(dto);

        Account sourceAccount =
                accountRepository
                        .findByAccountNumberWithLockAndTenantId(
                                dto.getSourceAccountNumber(), tenantId)
                        .orElseThrow(
                                () ->
                                        new com.ledgora.common.exception.BusinessException(
                                                "ACCOUNT_NOT_FOUND",
                                                "Source account not found: "
                                                        + dto.getSourceAccountNumber()));
        Account destAccount =
                accountRepository
                        .findByAccountNumberWithLockAndTenantId(
                                dto.getDestinationAccountNumber(), tenantId)
                        .orElseThrow(
                                () ->
                                        new com.ledgora.common.exception.BusinessException(
                                                "ACCOUNT_NOT_FOUND",
                                                "Destination account not found: "
                                                        + dto.getDestinationAccountNumber()));
        validateAccountActive(sourceAccount);
        validateAccountActive(destAccount);
        validateAccountFreezeLevel(sourceAccount, VoucherDrCr.DR);
        validateAccountFreezeLevel(destAccount, VoucherDrCr.CR);
        if (sourceAccount.getAccountNumber().equals(destAccount.getAccountNumber())) {
            throw new com.ledgora.common.exception.BusinessException(
                    "SAME_ACCOUNT", "Source and destination accounts cannot be the same");
        }
        if (sourceAccount.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new com.ledgora.common.exception.InsufficientBalanceException(
                    sourceAccount.getAccountNumber(),
                    "Insufficient balance in account "
                            + sourceAccount.getAccountNumber()
                            + ". Available: "
                            + sourceAccount.getBalance());
        }

        User currentUser = getCurrentUser();
        if (currentUser == null && channelForLimit != TransactionChannel.BATCH) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot initiate transfer: maker identity could not be resolved");
        }
        Long userId = currentUser != null ? currentUser.getId() : null;

        // ── VELOCITY FRAUD CHECK: proactive burst detection (check source account) ──
        velocityFraudEngine.evaluateVelocity(tenant, sourceAccount, dto.getAmount(), userId);

        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);

        TransactionChannel channel = parseChannel(dto.getChannel());
        validateHolidayCalendar(tenantId, businessDate, channel);

        String txnRef = generateTransactionRef("TRF");
        TransactionBatch batch = batchService.getOrCreateOpenBatch(tenantId, channel, businessDate);

        boolean autoAuth =
                approvalPolicyService.isAutoAuthorized(
                        tenantId, TransactionType.TRANSFER, channel, dto.getAmount(), false, false);

        Transaction transaction =
                Transaction.builder()
                        .transactionRef(txnRef)
                        .transactionType(TransactionType.TRANSFER)
                        .status(
                                autoAuth
                                        ? TransactionStatus.COMPLETED
                                        : TransactionStatus.PENDING_APPROVAL)
                        .amount(dto.getAmount())
                        .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                        .channel(channel)
                        .clientReferenceId(dto.getClientReferenceId())
                        .sourceAccount(sourceAccount)
                        .destinationAccount(destAccount)
                        .description(
                                dto.getDescription() != null
                                        ? dto.getDescription()
                                        : "Internal Transfer")
                        .narration(dto.getNarration())
                        .businessDate(businessDate)
                        .valueDate(businessDate)
                        .performedBy(currentUser)
                        .maker(currentUser)
                        .makerTimestamp(LocalDateTime.now())
                        .tenant(tenant)
                        .batch(batch)
                        .branch(resolveBranch(sourceAccount, currentUser))
                        .build();
        transaction = transactionRepository.save(transaction);

        if (autoAuth) {
            postTransferLedger(
                    transaction,
                    sourceAccount,
                    destAccount,
                    tenant,
                    currentUser,
                    businessDate,
                    batch,
                    dto);
            auditService.logTransaction(userId, transaction.getId(), txnRef, "TRANSFER");
            log.info(
                    "Transfer auto-authorized and posted: {} amount {} from {} to {}",
                    txnRef,
                    dto.getAmount(),
                    sourceAccount.getAccountNumber(),
                    destAccount.getAccountNumber());
        } else {
            approvalService.submitForApproval(
                    "TRANSACTION",
                    transaction.getId(),
                    "TRANSFER "
                            + dto.getAmount()
                            + " from "
                            + dto.getSourceAccountNumber()
                            + " to "
                            + dto.getDestinationAccountNumber());
            auditService.logTransaction(
                    userId, transaction.getId(), txnRef, "TRANSFER_PENDING_APPROVAL");
            log.info(
                    "Transfer pending approval: {} amount {} from {} to {}",
                    txnRef,
                    dto.getAmount(),
                    sourceAccount.getAccountNumber(),
                    destAccount.getAccountNumber());
        }

        return transaction;
    }

    // ===== Checker approval / rejection (called by ApprovalService) =====

    /**
     * Approve a pending transaction (checker step). Re-validates all conditions, then posts
     * vouchers + ledger + balances. Enforces maker != checker.
     */
    @Transactional
    public Transaction approveTransaction(Long transactionId, String remarks) {
        Long tenantId = requireTenantId();
        Transaction transaction =
                transactionRepository
                        .findByIdAndTenantId(transactionId, tenantId)
                        .orElseThrow(
                                () ->
                                        new com.ledgora.common.exception.BusinessException(
                                                "TRANSACTION_NOT_FOUND",
                                                "Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING_APPROVAL) {
            throw new com.ledgora.common.exception.BusinessException(
                    "INVALID_STATUS",
                    "Transaction "
                            + transactionId
                            + " is not pending approval. Status: "
                            + transaction.getStatus());
        }

        User checker = getCurrentUser();
        if (checker == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot approve transaction: approver identity could not be resolved");
        }
        // Maker-checker enforcement: checker must differ from maker
        if (transaction.getMaker() != null
                && transaction.getMaker().getId().equals(checker.getId())) {
            throw new com.ledgora.common.exception.BusinessException(
                    "MAKER_CHECKER_VIOLATION",
                    "Cannot approve your own transaction (maker-checker violation)");
        }

        // Re-validate business day
        tenantService.validateBusinessDayOpen(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);

        // Re-validate accounts with pessimistic lock
        TransactionType txnType = transaction.getTransactionType();

        // Get a fresh open batch for the current business date (original batch may be CLOSED)
        TransactionChannel channel =
                transaction.getChannel() != null
                        ? transaction.getChannel()
                        : TransactionChannel.TELLER;
        TransactionBatch batch = batchService.getOrCreateOpenBatch(tenantId, channel, businessDate);
        transaction.setBatch(batch);

        if (txnType == TransactionType.DEPOSIT) {
            Account account =
                    accountRepository
                            .findByIdWithLock(transaction.getDestinationAccount().getId())
                            .orElseThrow(
                                    () ->
                                            new com.ledgora.common.exception.BusinessException(
                                                    "ACCOUNT_NOT_FOUND",
                                                    "Destination account not found"));
            validateAccountActive(account);
            validateAccountFreezeLevel(account, VoucherDrCr.CR);
            postDepositLedger(
                    transaction,
                    account,
                    tenant,
                    checker,
                    businessDate,
                    batch,
                    buildDtoFromTransaction(transaction));
        } else if (txnType == TransactionType.WITHDRAWAL) {
            Account account =
                    accountRepository
                            .findByIdWithLock(transaction.getSourceAccount().getId())
                            .orElseThrow(
                                    () ->
                                            new com.ledgora.common.exception.BusinessException(
                                                    "ACCOUNT_NOT_FOUND",
                                                    "Source account not found"));
            validateAccountActive(account);
            validateAccountFreezeLevel(account, VoucherDrCr.DR);
            if (account.getBalance().compareTo(transaction.getAmount()) < 0) {
                throw new com.ledgora.common.exception.InsufficientBalanceException(
                        account.getAccountNumber(),
                        "Insufficient balance at approval time. Available: "
                                + account.getBalance());
            }
            postWithdrawalLedger(
                    transaction,
                    account,
                    tenant,
                    checker,
                    businessDate,
                    batch,
                    buildDtoFromTransaction(transaction));
        } else if (txnType == TransactionType.TRANSFER) {
            Account sourceAccount =
                    accountRepository
                            .findByIdWithLock(transaction.getSourceAccount().getId())
                            .orElseThrow(
                                    () ->
                                            new com.ledgora.common.exception.BusinessException(
                                                    "ACCOUNT_NOT_FOUND",
                                                    "Source account not found"));
            Account destAccount =
                    accountRepository
                            .findByIdWithLock(transaction.getDestinationAccount().getId())
                            .orElseThrow(
                                    () ->
                                            new com.ledgora.common.exception.BusinessException(
                                                    "ACCOUNT_NOT_FOUND",
                                                    "Destination account not found"));
            validateAccountActive(sourceAccount);
            validateAccountActive(destAccount);
            validateAccountFreezeLevel(sourceAccount, VoucherDrCr.DR);
            validateAccountFreezeLevel(destAccount, VoucherDrCr.CR);
            if (sourceAccount.getBalance().compareTo(transaction.getAmount()) < 0) {
                throw new com.ledgora.common.exception.InsufficientBalanceException(
                        sourceAccount.getAccountNumber(),
                        "Insufficient balance at approval time. Available: "
                                + sourceAccount.getBalance());
            }
            postTransferLedger(
                    transaction,
                    sourceAccount,
                    destAccount,
                    tenant,
                    checker,
                    businessDate,
                    batch,
                    buildDtoFromTransaction(transaction));
        }

        // Update transaction with checker info (event already published by post*Ledger methods)
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setChecker(checker);
        transaction.setCheckerTimestamp(LocalDateTime.now());
        transaction.setCheckerRemarks(remarks);
        transactionRepository.save(transaction);

        auditService.logTransaction(
                checker.getId(),
                transaction.getId(),
                transaction.getTransactionRef(),
                txnType.name() + "_APPROVED");

        log.info(
                "Transaction {} approved and posted by checker {}",
                transaction.getTransactionRef(),
                checker.getUsername());
        return transaction;
    }

    /**
     * Reject a pending transaction (checker step). Marks REJECTED, no posting. Enforces maker !=
     * checker.
     */
    @Transactional
    public Transaction rejectTransaction(Long transactionId, String remarks) {
        Long tenantId = requireTenantId();
        Transaction transaction =
                transactionRepository
                        .findByIdAndTenantId(transactionId, tenantId)
                        .orElseThrow(
                                () ->
                                        new com.ledgora.common.exception.BusinessException(
                                                "TRANSACTION_NOT_FOUND",
                                                "Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING_APPROVAL) {
            throw new com.ledgora.common.exception.BusinessException(
                    "INVALID_STATUS",
                    "Transaction "
                            + transactionId
                            + " is not pending approval. Status: "
                            + transaction.getStatus());
        }

        User checker = getCurrentUser();
        if (checker == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot reject transaction: reviewer identity could not be resolved");
        }
        // Maker-checker enforcement: checker must differ from maker for rejection too
        if (transaction.getMaker() != null
                && transaction.getMaker().getId().equals(checker.getId())) {
            throw new com.ledgora.common.exception.BusinessException(
                    "MAKER_CHECKER_VIOLATION",
                    "Cannot reject your own transaction (maker-checker violation)");
        }

        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setChecker(checker);
        transaction.setCheckerTimestamp(LocalDateTime.now());
        transaction.setCheckerRemarks(remarks);
        transactionRepository.save(transaction);

        auditService.logTransaction(
                checker.getId(),
                transaction.getId(),
                transaction.getTransactionRef(),
                transaction.getTransactionType().name() + "_REJECTED");

        log.info(
                "Transaction {} rejected by checker {}: {}",
                transaction.getTransactionRef(),
                checker.getUsername(),
                remarks);
        return transaction;
    }

    /** Get all transactions pending approval for the current tenant. */
    public List<Transaction> getPendingApprovalTransactions() {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTenantIdAndStatus(
                tenantId, TransactionStatus.PENDING_APPROVAL);
    }

    /** Count transactions pending approval for the current tenant. */
    public long countPendingApproval() {
        Long tenantId = requireTenantId();
        return transactionRepository.countByTenantIdAndStatus(
                tenantId, TransactionStatus.PENDING_APPROVAL);
    }

    // ===== Internal posting methods (extracted for reuse by auto-auth and checker-approve) =====

    private void postDepositLedger(
            Transaction transaction,
            Account account,
            Tenant tenant,
            User poster,
            LocalDate businessDate,
            TransactionBatch batch,
            TransactionDTO dto) {
        batchService.updateBatchTotals(batch.getId(), dto.getAmount(), dto.getAmount());

        createTransactionLine(
                transaction,
                account,
                EntryType.DEBIT,
                dto.getAmount(),
                "Cash deposit - debit cash account");
        createTransactionLine(
                transaction,
                account,
                EntryType.CREDIT,
                dto.getAmount(),
                "Cash deposit - credit customer account");

        BigDecimal newBalance = account.getBalance().add(dto.getAmount());
        Account cashAccount = resolveCashGlAccount(tenant.getId());
        String currency = dto.getCurrency() != null ? dto.getCurrency() : "INR";
        postVoucher(
                transaction,
                tenant,
                poster,
                cashAccount,
                VoucherDrCr.DR,
                dto.getAmount(),
                currency,
                businessDate,
                batch,
                "Cash deposit - cash ledger leg");
        postVoucher(
                transaction,
                tenant,
                poster,
                account,
                VoucherDrCr.CR,
                dto.getAmount(),
                currency,
                businessDate,
                batch,
                "Cash deposit - customer ledger leg");

        account.setBalance(newBalance);
        accountRepository.save(account);
        updateAccountBalanceCache(account, newBalance);

        eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction));
    }

    private void postWithdrawalLedger(
            Transaction transaction,
            Account account,
            Tenant tenant,
            User poster,
            LocalDate businessDate,
            TransactionBatch batch,
            TransactionDTO dto) {
        batchService.updateBatchTotals(batch.getId(), dto.getAmount(), dto.getAmount());

        createTransactionLine(
                transaction,
                account,
                EntryType.DEBIT,
                dto.getAmount(),
                "Cash withdrawal - debit customer account");
        createTransactionLine(
                transaction,
                account,
                EntryType.CREDIT,
                dto.getAmount(),
                "Cash withdrawal - credit cash account");

        BigDecimal newBalance = account.getBalance().subtract(dto.getAmount());
        Account cashAccount = resolveCashGlAccount(tenant.getId());
        String currency = dto.getCurrency() != null ? dto.getCurrency() : "INR";
        postVoucher(
                transaction,
                tenant,
                poster,
                account,
                VoucherDrCr.DR,
                dto.getAmount(),
                currency,
                businessDate,
                batch,
                "Cash withdrawal - customer ledger leg");
        postVoucher(
                transaction,
                tenant,
                poster,
                cashAccount,
                VoucherDrCr.CR,
                dto.getAmount(),
                currency,
                businessDate,
                batch,
                "Cash withdrawal - cash ledger leg");

        account.setBalance(newBalance);
        accountRepository.save(account);
        updateAccountBalanceCache(account, newBalance);

        eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction));
    }

    private void postTransferLedger(
            Transaction transaction,
            Account sourceAccount,
            Account destAccount,
            Tenant tenant,
            User poster,
            LocalDate businessDate,
            TransactionBatch batch,
            TransactionDTO dto) {
        batchService.updateBatchTotals(batch.getId(), dto.getAmount(), dto.getAmount());

        createTransactionLine(
                transaction,
                sourceAccount,
                EntryType.DEBIT,
                dto.getAmount(),
                "Transfer to " + destAccount.getAccountNumber());
        createTransactionLine(
                transaction,
                destAccount,
                EntryType.CREDIT,
                dto.getAmount(),
                "Transfer from " + sourceAccount.getAccountNumber());

        String currency = dto.getCurrency() != null ? dto.getCurrency() : "INR";

        // ── INTER-BRANCH CLEARING DETECTION ──
        // CBS Standard: Direct cross-branch posting is strictly prohibited.
        // If source and dest are at different branches, route through IBC clearing accounts
        // so each branch independently balances per RBI requirements.
        Branch sourceBranch = resolveBranch(sourceAccount, poster);
        Branch destBranch = resolveBranch(destAccount, poster);
        boolean crossBranch =
                sourceBranch != null
                        && destBranch != null
                        && !sourceBranch.getId().equals(destBranch.getId());

        if (crossBranch) {
            // ── IBT GOVERNANCE VALIDATION (Steps 1, 7) ──
            // Validate both branches are ACTIVE, have clearing GL mappings, etc.
            ibtService.validateBranchesForIbt(tenant, sourceBranch, destBranch);

            // ── CROSS-BRANCH: 4-voucher clearing flow (Step 2) ──
            // Branch A: DR Customer A, CR IBC_OUT_A (Branch A balanced)
            // Branch B: DR IBC_IN_B, CR Customer B (Branch B balanced)
            com.ledgora.clearing.entity.InterBranchTransfer ibcTransfer =
                    interBranchClearingService.createTransfer(
                            tenant,
                            sourceBranch,
                            destBranch,
                            dto.getAmount(),
                            currency,
                            transaction,
                            businessDate,
                            poster,
                            "IBC: "
                                    + sourceAccount.getAccountNumber()
                                    + " → "
                                    + destAccount.getAccountNumber());

            // Branch A leg: DR Customer A, CR IBC_OUT_A
            Account ibcOutAccount =
                    interBranchClearingService.resolveIbcOutAccount(tenant.getId(), sourceBranch);
            postVoucher(
                    transaction,
                    tenant,
                    poster,
                    sourceAccount,
                    VoucherDrCr.DR,
                    dto.getAmount(),
                    currency,
                    businessDate,
                    batch,
                    "IBC Transfer DR: " + sourceAccount.getAccountNumber());
            postVoucher(
                    transaction,
                    tenant,
                    poster,
                    ibcOutAccount,
                    VoucherDrCr.CR,
                    dto.getAmount(),
                    currency,
                    businessDate,
                    batch,
                    "IBC OUT CR: " + sourceBranch.getBranchCode());
            interBranchClearingService.markSent(ibcTransfer.getId());

            // Branch B leg: DR IBC_IN_B, CR Customer B
            Account ibcInAccount =
                    interBranchClearingService.resolveIbcInAccount(tenant.getId(), destBranch);
            postVoucher(
                    transaction,
                    tenant,
                    poster,
                    ibcInAccount,
                    VoucherDrCr.DR,
                    dto.getAmount(),
                    currency,
                    businessDate,
                    batch,
                    "IBC IN DR: " + destBranch.getBranchCode());
            postVoucher(
                    transaction,
                    tenant,
                    poster,
                    destAccount,
                    VoucherDrCr.CR,
                    dto.getAmount(),
                    currency,
                    businessDate,
                    batch,
                    "IBC Transfer CR: " + destAccount.getAccountNumber());
            interBranchClearingService.markReceived(ibcTransfer.getId());

            // ── IBT VOUCHER COUNT VALIDATION (Step 2) ──
            // Verify exactly 4 vouchers were created for this IBT transaction
            ibtService.validateIbtVoucherCount(transaction.getId());

            log.info(
                    "Cross-branch transfer posted via IBC: {} → {} amount={} ibcId={}",
                    sourceBranch.getBranchCode(),
                    destBranch.getBranchCode(),
                    dto.getAmount(),
                    ibcTransfer.getId());
        } else {
            // ── SAME BRANCH: direct DR/CR posting (existing behavior) ──
            postVoucher(
                    transaction,
                    tenant,
                    poster,
                    sourceAccount,
                    VoucherDrCr.DR,
                    dto.getAmount(),
                    currency,
                    businessDate,
                    batch,
                    "Transfer to " + destAccount.getAccountNumber());
            postVoucher(
                    transaction,
                    tenant,
                    poster,
                    destAccount,
                    VoucherDrCr.CR,
                    dto.getAmount(),
                    currency,
                    businessDate,
                    batch,
                    "Transfer from " + sourceAccount.getAccountNumber());
        }

        BigDecimal sourceNewBalance = sourceAccount.getBalance().subtract(dto.getAmount());
        BigDecimal destNewBalance = destAccount.getBalance().add(dto.getAmount());

        sourceAccount.setBalance(sourceNewBalance);
        accountRepository.save(sourceAccount);
        updateAccountBalanceCache(sourceAccount, sourceNewBalance);

        destAccount.setBalance(destNewBalance);
        accountRepository.save(destAccount);
        updateAccountBalanceCache(destAccount, destNewBalance);

        eventPublisher.publishEvent(new TransactionCreatedEvent(this, transaction));
    }

    /** Build a minimal TransactionDTO from a saved Transaction (for posting after approval). */
    private TransactionDTO buildDtoFromTransaction(Transaction transaction) {
        return TransactionDTO.builder()
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .sourceAccountNumber(
                        transaction.getSourceAccount() != null
                                ? transaction.getSourceAccount().getAccountNumber()
                                : null)
                .destinationAccountNumber(
                        transaction.getDestinationAccount() != null
                                ? transaction.getDestinationAccount().getAccountNumber()
                                : null)
                .description(transaction.getDescription())
                .narration(transaction.getNarration())
                .channel(transaction.getChannel() != null ? transaction.getChannel().name() : null)
                .clientReferenceId(transaction.getClientReferenceId())
                .build();
    }

    // ===== Query methods (backward compatible) =====

    public List<Transaction> getAllTransactions() {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTenantId(tenantId);
    }

    public org.springframework.data.domain.Page<Transaction> getAllTransactionsPaged(
            int page, int size) {
        return transactionRepository.findByTenantId(
                requireTenantId(), org.springframework.data.domain.PageRequest.of(page, size));
    }

    public Optional<Transaction> getTransactionById(Long id) {
        Long tenantId = requireTenantId();
        return transactionRepository.findByIdAndTenantId(id, tenantId);
    }

    public Optional<Transaction> getTransactionByRef(String ref) {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTransactionRefAndTenantId(ref, tenantId);
    }

    public List<Transaction> getTransactionsByAccountNumber(String accountNumber) {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTenantIdAndAccountNumber(tenantId, accountNumber);
    }

    public List<Transaction> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end) {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTenantIdAndDateRange(tenantId, start, end);
    }

    public List<Transaction> getTransactionsByType(TransactionType type) {
        Long tenantId = requireTenantId();
        return transactionRepository.findByTenantIdAndTransactionType(tenantId, type);
    }

    public List<LedgerEntry> getLedgerEntriesByTransaction(Long transactionId) {
        Long tenantId = requireTenantId();
        return ledgerEntryRepository.findByTransactionIdAndTenantId(transactionId, tenantId);
    }

    public List<LedgerEntry> getLedgerEntriesByAccount(String accountNumber) {
        Long tenantId = requireTenantId();
        return ledgerEntryRepository.findByAccountNumberAndTenantId(accountNumber, tenantId);
    }

    public long countAll() {
        Long tenantId = requireTenantId();
        return transactionRepository.countByTenantId(tenantId);
    }

    public List<Transaction> getTodayTransactions() {
        Long tenantId = requireTenantId();
        // Use tenant's business date instead of system clock for multi-tenant consistency
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);
        LocalDateTime startOfDay = businessDate.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return transactionRepository.findByTenantIdAndDateRange(tenantId, startOfDay, endOfDay);
    }

    // ===== Private helper methods =====

    private void createTransactionLine(
            Transaction transaction,
            Account account,
            EntryType lineType,
            BigDecimal amount,
            String description) {
        TransactionLine line =
                TransactionLine.builder()
                        .transaction(transaction)
                        .account(account)
                        .lineType(lineType)
                        .amount(amount)
                        .currency(transaction.getCurrency())
                        .description(description)
                        .build();
        transactionLineRepository.save(line);
    }

    private void updateAccountBalanceCache(Account account, BigDecimal newLedgerBalance) {
        AccountBalance balance =
                accountBalanceRepository
                        .findByAccountId(account.getId())
                        .orElseGet(
                                () ->
                                        AccountBalance.builder()
                                                .account(account)
                                                .holdAmount(BigDecimal.ZERO)
                                                .build());
        balance.setLedgerBalance(newLedgerBalance);
        balance.setAvailableBalance(newLedgerBalance.subtract(balance.getHoldAmount()));
        accountBalanceRepository.save(balance);
    }

    /**
     * CBS: Server-side validation that transaction amount is positive and scale ≤ 2. Never trust
     * client-side validation alone for financial operations. Uses centralized RbiFieldValidator as
     * the final defense before persistence.
     */
    private void validateAmountPositive(BigDecimal amount) {
        com.ledgora.common.validation.RbiFieldValidator.validateTransactionAmount(amount);
    }

    private void validateAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new com.ledgora.common.exception.BusinessException(
                    "ACCOUNT_INACTIVE",
                    "Account "
                            + account.getAccountNumber()
                            + " is not active. Status: "
                            + account.getStatus());
        }
        // H2: Ensure account is approved before allowing transactions
        if (account.getApprovalStatus() != null
                && account.getApprovalStatus() != MakerCheckerStatus.APPROVED) {
            throw new com.ledgora.common.exception.BusinessException(
                    "ACCOUNT_NOT_APPROVED",
                    "Account "
                            + account.getAccountNumber()
                            + " is not approved. Approval status: "
                            + account.getApprovalStatus());
        }
    }

    /**
     * C3: Validate account-level freeze controls server-side. FreezeLevel.DEBIT_ONLY blocks debit
     * operations. FreezeLevel.CREDIT_ONLY blocks credit operations. FreezeLevel.FULL blocks all
     * operations.
     */
    private void validateAccountFreezeLevel(Account account, VoucherDrCr drCr) {
        FreezeLevel freezeLevel = account.getFreezeLevel();
        if (freezeLevel == null || freezeLevel == FreezeLevel.NONE) {
            return;
        }
        if (freezeLevel == FreezeLevel.FULL) {
            throw new com.ledgora.common.exception.BusinessException(
                    "ACCOUNT_FROZEN",
                    "Account "
                            + account.getAccountNumber()
                            + " is fully frozen. Reason: "
                            + account.getFreezeReason());
        }
        if (freezeLevel == FreezeLevel.DEBIT_ONLY && drCr == VoucherDrCr.DR) {
            throw new com.ledgora.common.exception.BusinessException(
                    "ACCOUNT_DEBIT_FROZEN",
                    "Account "
                            + account.getAccountNumber()
                            + " has debit freeze active. Reason: "
                            + account.getFreezeReason());
        }
        if (freezeLevel == FreezeLevel.CREDIT_ONLY && drCr == VoucherDrCr.CR) {
            throw new com.ledgora.common.exception.BusinessException(
                    "ACCOUNT_CREDIT_FROZEN",
                    "Account "
                            + account.getAccountNumber()
                            + " has credit freeze active. Reason: "
                            + account.getFreezeReason());
        }
    }

    /**
     * H4: Validate that the transaction is allowed on the current business date per holiday
     * calendar.
     */
    private void validateHolidayCalendar(
            Long tenantId, LocalDate businessDate, TransactionChannel channel) {
        if (channel == null) {
            channel = TransactionChannel.TELLER;
        }
        bankCalendarService.validateTransactionAllowed(tenantId, businessDate, channel);
    }

    private User getCurrentUser() {
        try {
            org.springframework.security.core.Authentication auth =
                    SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return null;
            return userRepository.findByUsername(auth.getName()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String generateTransactionRef(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Voucher postVoucher(
            Transaction transaction,
            Tenant tenant,
            User maker,
            Account account,
            VoucherDrCr drCr,
            BigDecimal amount,
            String currency,
            LocalDate businessDate,
            TransactionBatch batch,
            String narration) {
        Branch branch = resolveBranch(account, maker);
        GeneralLedger glAccount = resolveGlForAccount(account);
        String batchCode =
                batch.getBatchCode() != null ? batch.getBatchCode() : "BATCH-" + batch.getId();

        // Target flow: Transaction → Voucher (with FK) → authorize → post → LedgerJournal →
        // LedgerEntry
        Voucher voucher =
                voucherService.createVoucher(
                        tenant,
                        branch,
                        account,
                        glAccount,
                        drCr,
                        amount,
                        amount,
                        currency,
                        businessDate,
                        businessDate,
                        batchCode,
                        1,
                        maker,
                        narration,
                        transaction // link voucher to originating transaction
                        );
        voucherService.systemAuthorizeVoucher(voucher.getId(), maker);
        return voucherService.postVoucher(voucher.getId(), transaction);
    }

    private Branch resolveBranch(Account account, User currentUser) {
        if (account.getBranch() != null) {
            return account.getBranch();
        }
        if (account.getHomeBranch() != null) {
            return account.getHomeBranch();
        }
        if (currentUser != null && currentUser.getBranch() != null) {
            return currentUser.getBranch();
        }
        throw new com.ledgora.common.exception.BusinessException(
                "NO_BRANCH_MAPPING",
                "No branch mapping found for account " + account.getAccountNumber());
    }

    /**
     * Resolve the GL account for a customer account. CBS rule: every customer account MUST have a
     * valid GL mapping. If the GL code is set but the GL account doesn't exist in the chart of
     * accounts, this is a configuration error that must block posting (not silently skip).
     *
     * @return the GeneralLedger entity, or null only if no GL code is configured on the account
     * @throws BusinessException if GL code is set but GL account not found
     */
    private GeneralLedger resolveGlForAccount(Account account) {
        if (account.getGlAccountCode() == null || account.getGlAccountCode().isBlank()) {
            return null;
        }
        return glRepository
                .findByGlCode(account.getGlAccountCode())
                .orElseThrow(
                        () ->
                                new com.ledgora.common.exception.BusinessException(
                                        "GL_ACCOUNT_NOT_FOUND",
                                        "GL account "
                                                + account.getGlAccountCode()
                                                + " not found in chart of accounts for account "
                                                + account.getAccountNumber()
                                                + ". CBS requires valid GL mapping for all postings."));
    }

    private Account resolveCashGlAccount(Long tenantId) {
        return accountRepository
                .findFirstByTenantIdAndGlAccountCode(tenantId, "1100")
                .or(() -> accountRepository.findByAccountNumberAndTenantId("GL-CASH-001", tenantId))
                .orElseThrow(
                        () ->
                                new com.ledgora.common.exception.BusinessException(
                                        "GL_ACCOUNT_NOT_FOUND",
                                        "Cash GL account with code 1100 is required for cash transactions"));
    }

    /**
     * PART 2: Idempotency check using client_reference_id + channel. Prevents duplicate transaction
     * processing.
     */
    private void checkIdempotency(TransactionDTO dto) {
        if (dto.getClientReferenceId() != null && dto.getChannel() != null) {
            TransactionChannel channel = parseChannel(dto.getChannel());
            if (channel != null) {
                transactionRepository
                        .findByClientReferenceIdAndChannelAndTenantId(
                                dto.getClientReferenceId(), channel, requireTenantId())
                        .ifPresent(
                                existing -> {
                                    throw new com.ledgora.common.exception.BusinessException(
                                            "DUPLICATE_TRANSACTION",
                                            "Duplicate transaction detected. Existing ref: "
                                                    + existing.getTransactionRef()
                                                    + " for client_reference_id: "
                                                    + dto.getClientReferenceId()
                                                    + " channel: "
                                                    + dto.getChannel());
                                });
            }
            // Also register with IdempotencyService for broader deduplication
            String idempotencyKey = dto.getClientReferenceId() + ":" + dto.getChannel();
            idempotencyService
                    .checkExisting(idempotencyKey)
                    .ifPresent(
                            existing -> {
                                throw new com.ledgora.common.exception.BusinessException(
                                        "DUPLICATE_TRANSACTION",
                                        "Duplicate transaction: idempotency key already completed");
                            });
            idempotencyService.registerKey(idempotencyKey, dto.toString());
        }
    }

    private Long requireTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private TransactionChannel parseChannel(String channel) {
        if (channel == null || channel.isEmpty()) return null;
        try {
            return TransactionChannel.valueOf(channel);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
