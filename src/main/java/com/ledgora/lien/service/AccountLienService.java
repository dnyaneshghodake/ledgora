package com.ledgora.lien.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.approval.service.ApprovalService;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.balance.service.CbsBalanceEngine;
import com.ledgora.common.enums.LienStatus;
import com.ledgora.common.enums.LienType;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.lien.entity.AccountLien;
import com.ledgora.lien.repository.AccountLienRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing account liens. Lien reduces available balance. Creation/release require
 * maker-checker. Expired liens auto-release via scheduler.
 */
@Service
public class AccountLienService {

    private static final Logger log = LoggerFactory.getLogger(AccountLienService.class);
    private final AccountLienRepository lienRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final CbsBalanceEngine balanceEngine;
    private final TenantService tenantService;
    private final ApprovalService approvalService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public AccountLienService(
            AccountLienRepository lienRepository,
            AccountRepository accountRepository,
            AccountBalanceRepository accountBalanceRepository,
            CbsBalanceEngine balanceEngine,
            TenantService tenantService,
            ApprovalService approvalService,
            AuditService auditService,
            UserRepository userRepository) {
        this.lienRepository = lienRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.balanceEngine = balanceEngine;
        this.tenantService = tenantService;
        this.approvalService = approvalService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /** Create a lien (maker step). Requires approval before it takes effect. */
    @Transactional
    public AccountLien createLien(
            Long tenantId,
            Long accountId,
            BigDecimal lienAmount,
            LienType lienType,
            LocalDate startDate,
            LocalDate endDate,
            String lienReference,
            String remarks) {
        Account account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        if (lienAmount == null || lienAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Lien amount must be positive");
        }
        if (lienAmount.scale() > 2) {
            throw new RuntimeException("Lien amount must have at most 2 decimal places");
        }
        if (startDate == null) {
            throw new RuntimeException("Lien start date is required");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Lien start date cannot be in the past");
        }
        if (endDate != null && !endDate.isAfter(startDate)) {
            throw new RuntimeException("Lien end date must be after start date");
        }

        // Lien cannot exceed ledger balance
        AccountBalance balance =
                accountBalanceRepository
                        .findByAccountId(accountId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Account balance not found for account: "
                                                        + accountId));
        BigDecimal currentLiens = lienRepository.sumActiveLienAmountByAccountId(accountId);
        if (currentLiens.add(lienAmount).compareTo(balance.getLedgerBalance()) > 0) {
            throw new RuntimeException(
                    "Lien amount would exceed ledger balance. Ledger: "
                            + balance.getLedgerBalance()
                            + ", Existing liens: "
                            + currentLiens
                            + ", New lien: "
                            + lienAmount);
        }

        Tenant tenant = tenantService.getTenantById(tenantId);
        User currentUser = getCurrentUser();

        AccountLien lien =
                AccountLien.builder()
                        .tenant(tenant)
                        .account(account)
                        .lienAmount(lienAmount)
                        .lienType(lienType)
                        .startDate(startDate)
                        .endDate(endDate)
                        .status(LienStatus.ACTIVE)
                        .approvalStatus(MakerCheckerStatus.PENDING)
                        .lienReference(lienReference)
                        .remarks(remarks)
                        .createdBy(currentUser)
                        .build();

        AccountLien saved = lienRepository.save(lien);

        approvalService.submitForApproval(
                "ACCOUNT_LIEN",
                saved.getId(),
                "Lien: account="
                        + account.getAccountNumber()
                        + " amount="
                        + lienAmount
                        + " type="
                        + lienType);

        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(
                userId,
                "LIEN_CREATE",
                "ACCOUNT_LIEN",
                saved.getId(),
                "Lien created for account " + account.getAccountNumber() + " amount=" + lienAmount,
                null);

        log.info(
                "Lien created (PENDING): account={}, amount={}, type={}",
                account.getAccountNumber(),
                lienAmount,
                lienType);
        return saved;
    }

    /** Approve a lien (checker step). Applies the lien to balance engine. */
    @Transactional
    public AccountLien approveLien(Long lienId) {
        AccountLien lien =
                lienRepository
                        .findById(lienId)
                        .orElseThrow(() -> new RuntimeException("Lien not found: " + lienId));

        if (lien.getApprovalStatus() != MakerCheckerStatus.PENDING) {
            throw new RuntimeException("Lien is not pending approval");
        }

        User currentUser = getCurrentUser();
        if (lien.getCreatedBy() != null
                && currentUser != null
                && lien.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException(
                    "Cannot approve your own lien request (maker-checker violation)");
        }

        // Apply lien to balance engine
        balanceEngine.applyLien(lien.getAccount().getId(), lien.getLienAmount());

        lien.setApprovalStatus(MakerCheckerStatus.APPROVED);
        lien.setApprovedBy(currentUser);
        AccountLien saved = lienRepository.save(lien);

        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(
                userId,
                "LIEN_APPROVE",
                "ACCOUNT_LIEN",
                saved.getId(),
                "Lien approved: amount=" + lien.getLienAmount(),
                null);

        log.info("Lien approved and applied: id={}, amount={}", lienId, lien.getLienAmount());
        return saved;
    }

    /**
     * Release a lien (requires maker-checker: creator cannot release their own lien). Only approved
     * and active liens can be released.
     */
    @Transactional
    public AccountLien releaseLien(Long lienId) {
        AccountLien lien =
                lienRepository
                        .findById(lienId)
                        .orElseThrow(() -> new RuntimeException("Lien not found: " + lienId));

        if (lien.getStatus() != LienStatus.ACTIVE) {
            throw new RuntimeException("Lien is not active");
        }

        if (lien.getApprovalStatus() != MakerCheckerStatus.APPROVED) {
            throw new RuntimeException("Cannot release a lien that has not been approved");
        }

        // Maker-checker: creator cannot release their own lien
        User currentUser = getCurrentUser();
        if (lien.getCreatedBy() != null
                && currentUser != null
                && lien.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot release your own lien (maker-checker violation)");
        }

        balanceEngine.releaseLien(lien.getAccount().getId(), lien.getLienAmount());

        lien.setStatus(LienStatus.RELEASED);
        AccountLien saved = lienRepository.save(lien);

        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(
                userId,
                "LIEN_RELEASE",
                "ACCOUNT_LIEN",
                saved.getId(),
                "Lien released: amount=" + lien.getLienAmount(),
                null);

        log.info("Lien released: id={}, amount={}", lienId, lien.getLienAmount());
        return saved;
    }

    /** Scheduler to auto-release expired liens. */
    @Scheduled(cron = "0 0 1 * * ?") // 1 AM daily
    @Transactional
    public void autoReleaseExpiredLiens() {
        List<AccountLien> expiredLiens = lienRepository.findExpiredLiens(LocalDate.now());
        int releasedCount = 0;
        for (AccountLien lien : expiredLiens) {
            try {
                balanceEngine.releaseLien(lien.getAccount().getId(), lien.getLienAmount());
                lien.setStatus(LienStatus.EXPIRED);
                lienRepository.save(lien);
                releasedCount++;
                log.info(
                        "Auto-released expired lien: id={}, account={}, amount={}",
                        lien.getId(),
                        lien.getAccount().getAccountNumber(),
                        lien.getLienAmount());
            } catch (Exception e) {
                log.error("Failed to auto-release lien {}: {}", lien.getId(), e.getMessage());
            }
        }
        if (!expiredLiens.isEmpty()) {
            log.info("Auto-released {}/{} expired liens", releasedCount, expiredLiens.size());
        }
    }

    public List<AccountLien> getActiveLiensByAccount(Long accountId) {
        return lienRepository.findActiveApprovedByAccountId(accountId);
    }

    public BigDecimal getTotalActiveLienAmount(Long accountId) {
        return lienRepository.sumActiveLienAmountByAccountId(accountId);
    }

    public List<AccountLien> getLiensByAccount(Long accountId, Long tenantId) {
        return lienRepository.findByAccountIdAndTenantId(accountId, tenantId);
    }

    public List<AccountLien> getPendingLiens(Long tenantId) {
        return lienRepository.findByTenantIdAndApprovalStatus(tenantId, MakerCheckerStatus.PENDING);
    }

    private User getCurrentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
