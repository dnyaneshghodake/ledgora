package com.ledgora.account.service;

import com.ledgora.account.dto.AccountDTO;
import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.tenant.context.TenantContextHolder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final com.ledgora.account.repository.AccountProductSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final com.ledgora.product.repository.ProductRepository productRepository;
    private final com.ledgora.product.repository.ProductVersionRepository productVersionRepository;
    private final com.ledgora.product.repository.ProductGlMappingRepository productGlMappingRepository;

    public AccountService(
            AccountRepository accountRepository,
            AccountBalanceRepository accountBalanceRepository,
            com.ledgora.account.repository.AccountProductSnapshotRepository snapshotRepository,
            UserRepository userRepository,
            AuditService auditService,
            com.ledgora.product.repository.ProductRepository productRepository,
            com.ledgora.product.repository.ProductVersionRepository productVersionRepository,
            com.ledgora.product.repository.ProductGlMappingRepository productGlMappingRepository) {
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.snapshotRepository = snapshotRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.productRepository = productRepository;
        this.productVersionRepository = productVersionRepository;
        this.productGlMappingRepository = productGlMappingRepository;
    }

    /**
     * Create account. If productId is provided, derives accountType and GL codes from the
     * product's effective version and creates an immutable AccountProductSnapshot. If productId
     * is null, falls back to legacy enum-based creation (deprecated path).
     */
    @Transactional
    public Account createAccount(AccountDTO dto) {
        String accountNumber = generateAccountNumber();
        User currentUser = getCurrentUser();
        Long tenantId = requireTenantId();

        // ── Product-driven path (CBS-grade) ──
        com.ledgora.product.entity.Product product = null;
        com.ledgora.product.entity.ProductVersion effectiveVersion = null;
        com.ledgora.product.entity.ProductGlMapping glMapping = null;
        AccountType resolvedType;
        String resolvedGlCode;

        if (dto.getProductId() != null) {
            product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + dto.getProductId()));
            if (!tenantId.equals(product.getTenant().getId())) {
                throw new RuntimeException("Cross-tenant product access is not allowed");
            }
            if (product.getStatus() != com.ledgora.common.enums.ProductStatus.ACTIVE) {
                throw new RuntimeException("Product " + product.getProductCode() + " is not ACTIVE");
            }

            effectiveVersion = productVersionRepository
                    .findEffectiveVersion(product.getId(), java.time.LocalDate.now())
                    .orElseThrow(() -> new RuntimeException(
                            "No effective version for product " + product.getProductCode()));

            glMapping = productGlMappingRepository
                    .findByProductVersionId(effectiveVersion.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "GL mapping missing for product version " + effectiveVersion.getVersionNumber()));

            // Derive account type from product type
            resolvedType = switch (product.getProductType()) {
                case SAVINGS -> AccountType.SAVINGS;
                case CURRENT -> AccountType.CURRENT;
                case FIXED_DEPOSIT -> AccountType.FIXED_DEPOSIT;
                case LOAN -> AccountType.LOAN;
            };
            resolvedGlCode = glMapping.getCrGlCode(); // Customer deposit GL for the account

            log.info("Account opening via Product engine: product={} version={} type={}",
                    product.getProductCode(), effectiveVersion.getVersionNumber(), resolvedType);
        } else {
            // ── Legacy path (deprecated — retained for backward compatibility) ──
            log.warn("Account created without productId — using legacy enum-based path (deprecated)");
            resolvedType = AccountType.valueOf(dto.getAccountType());
            resolvedGlCode = dto.getGlAccountCode();
        }

        Account account =
                Account.builder()
                        .accountNumber(accountNumber)
                        .tenant(resolveCurrentTenant())
                        .accountName(dto.getAccountName())
                        .accountType(resolvedType)
                        .product(product)
                        .status(AccountStatus.ACTIVE)
                        .balance(BigDecimal.ZERO)
                        .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                        .branchCode(dto.getBranchCode())
                        .customerName(dto.getCustomerName())
                        .customerEmail(dto.getCustomerEmail())
                        .customerPhone(dto.getCustomerPhone())
                        .glAccountCode(resolvedGlCode)
                        .createdBy(currentUser)
                        .approvalStatus(MakerCheckerStatus.PENDING)
                        .build();

        Account saved = accountRepository.save(account);

        // ── Create product snapshot (immutable record of product config at opening time) ──
        if (product != null && effectiveVersion != null && glMapping != null) {
            com.ledgora.account.entity.AccountProductSnapshot snapshot =
                    com.ledgora.account.entity.AccountProductSnapshot.builder()
                            .account(saved)
                            .productId(product.getId())
                            .productCode(product.getProductCode())
                            .productName(product.getName())
                            .productType(product.getProductType().name())
                            .productVersionNumber(effectiveVersion.getVersionNumber())
                            .effectiveFrom(effectiveVersion.getEffectiveFrom())
                            .drGlCode(glMapping.getDrGlCode())
                            .crGlCode(glMapping.getCrGlCode())
                            .clearingGlCode(glMapping.getClearingGlCode())
                            .suspenseGlCode(glMapping.getSuspenseGlCode())
                            .interestAccrualGlCode(glMapping.getInterestAccrualGlCode())
                            .build();
            snapshotRepository.save(snapshot);
            log.info("AccountProductSnapshot created for account {} product {}",
                    saved.getAccountNumber(), product.getProductCode());
        }

        // Create account balance cache
        AccountBalance balance =
                AccountBalance.builder()
                        .account(saved)
                        .ledgerBalance(BigDecimal.ZERO)
                        .availableBalance(BigDecimal.ZERO)
                        .holdAmount(BigDecimal.ZERO)
                        .build();
        accountBalanceRepository.save(balance);

        // Audit log
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logAccountCreation(userId, saved.getId(), saved.getAccountNumber());

        log.info(
                "Account created: {} for customer: {} product: {}",
                saved.getAccountNumber(),
                saved.getCustomerName(),
                product != null ? product.getProductCode() : "LEGACY");
        return saved;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findByTenantId(requireTenantId());
    }

    public org.springframework.data.domain.Page<Account> getAllAccountsPaged(int page, int size) {
        return accountRepository.findByTenantId(
                requireTenantId(),
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    public Optional<Account> getAccountById(Long id) {
        return accountRepository
                .findById(id)
                .filter(
                        a ->
                                a.getTenant() != null
                                        && requireTenantId().equals(a.getTenant().getId()));
    }

    public Optional<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumberAndTenantId(accountNumber, requireTenantId());
    }

    public List<Account> getAccountsByStatus(AccountStatus status) {
        return accountRepository.findByTenantIdAndStatus(requireTenantId(), status);
    }

    public List<Account> getAccountsByType(AccountType type) {
        return accountRepository.findByTenantIdAndAccountType(requireTenantId(), type);
    }

    public List<Account> searchByCustomerName(String name) {
        return accountRepository.findByTenantIdAndCustomerNameContainingIgnoreCase(
                requireTenantId(), name);
    }

    public List<Account> searchAccounts(String query) {
        return accountRepository.searchByTenantId(requireTenantId(), query);
    }

    public List<Account> getAccountsByCustomerId(Long customerId) {
        return accountRepository.findByTenantIdAndCustomerId(requireTenantId(), customerId);
    }

    @Transactional
    public Account updateAccount(Long id, AccountDTO dto) {
        Account account = requireAccount(id);

        if (dto.getAccountName() != null) account.setAccountName(dto.getAccountName());
        if (dto.getCustomerName() != null) account.setCustomerName(dto.getCustomerName());
        if (dto.getCustomerEmail() != null) account.setCustomerEmail(dto.getCustomerEmail());
        if (dto.getCustomerPhone() != null) account.setCustomerPhone(dto.getCustomerPhone());
        if (dto.getBranchCode() != null) account.setBranchCode(dto.getBranchCode());
        if (dto.getGlAccountCode() != null) account.setGlAccountCode(dto.getGlAccountCode());
        if (dto.getInterestRate() != null) account.setInterestRate(dto.getInterestRate());
        if (dto.getOverdraftLimit() != null) account.setOverdraftLimit(dto.getOverdraftLimit());
        if (dto.getCurrency() != null && !dto.getCurrency().isBlank())
            account.setCurrency(dto.getCurrency());
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            account.setStatus(AccountStatus.valueOf(dto.getStatus()));
        }
        if (dto.getFreezeLevel() != null && !dto.getFreezeLevel().isBlank()) {
            account.setFreezeLevel(
                    com.ledgora.common.enums.FreezeLevel.valueOf(dto.getFreezeLevel()));
        }
        if (dto.getFreezeReason() != null) {
            account.setFreezeReason(dto.getFreezeReason());
        }

        return accountRepository.save(account);
    }

    /** Finacle-grade: Update freeze controls at account level (maker step). */
    @Transactional
    public Account updateFreezeStatus(
            Long id, com.ledgora.common.enums.FreezeLevel freezeLevel, String freezeReason) {
        Account account = requireAccount(id);
        account.setFreezeLevel(
                freezeLevel != null ? freezeLevel : com.ledgora.common.enums.FreezeLevel.NONE);
        account.setFreezeReason(freezeReason);

        Account saved = accountRepository.save(account);

        User currentUser = getCurrentUser();
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(
                userId,
                "ACCOUNT_FREEZE_UPDATE",
                "ACCOUNT",
                saved.getId(),
                "Account freeze updated: account="
                        + saved.getAccountNumber()
                        + " level="
                        + saved.getFreezeLevel()
                        + (freezeReason != null && !freezeReason.isBlank()
                                ? " reason=" + freezeReason
                                : ""),
                null);

        return saved;
    }

    @Transactional
    public Account updateAccountStatus(Long id, AccountStatus status) {
        Account account = requireAccount(id);
        account.setStatus(status);
        log.info("Account {} status changed to {}", account.getAccountNumber(), status);
        return accountRepository.save(account);
    }

    /** H2: Approve an account (checker step). Enforces maker-checker + tenant isolation. */
    @Transactional
    public Account approveAccount(Long id) {
        Account account = requireAccount(id);

        if (account.getApprovalStatus() != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(
                    "Account is not pending approval. Current status: "
                            + account.getApprovalStatus());
        }

        User currentUser = getCurrentUser();
        // Maker-checker: approver must differ from creator
        if (account.getCreatedBy() != null
                && currentUser != null
                && account.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot approve your own account (maker-checker violation)");
        }

        account.setApprovalStatus(MakerCheckerStatus.APPROVED);
        account.setApprovedBy(currentUser);
        Account saved = accountRepository.save(account);

        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(
                userId,
                "ACCOUNT_APPROVE",
                "ACCOUNT",
                saved.getId(),
                "Account approved: " + saved.getAccountNumber(),
                null);

        log.info(
                "Account {} approved by user {}",
                saved.getAccountNumber(),
                currentUser != null ? currentUser.getUsername() : "system");
        return saved;
    }

    /** H2: Reject an account (checker step). Enforces maker-checker + tenant isolation. */
    @Transactional
    public Account rejectAccount(Long id) {
        Account account = requireAccount(id);

        if (account.getApprovalStatus() != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(
                    "Account is not pending approval. Current status: "
                            + account.getApprovalStatus());
        }

        User currentUser = getCurrentUser();
        // Maker-checker: rejector must differ from creator
        if (account.getCreatedBy() != null
                && currentUser != null
                && account.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot reject your own account (maker-checker violation)");
        }

        account.setApprovalStatus(MakerCheckerStatus.REJECTED);
        Account saved = accountRepository.save(account);

        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(
                userId,
                "ACCOUNT_REJECT",
                "ACCOUNT",
                saved.getId(),
                "Account rejected: " + saved.getAccountNumber(),
                null);

        log.info(
                "Account {} rejected by user {}",
                saved.getAccountNumber(),
                currentUser != null ? currentUser.getUsername() : "system");
        return saved;
    }

    @Transactional
    public void updateBalance(Long accountId, BigDecimal newBalance) {
        Account account = requireAccount(accountId);
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    public long countByStatus(AccountStatus status) {
        return accountRepository.countByStatus(status);
    }

    public long countAll() {
        return accountRepository.count();
    }

    private Account requireAccount(Long accountId) {
        Account account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Account not found with id: " + accountId));
        Long tenantId = requireTenantId();
        if (account.getTenant() == null || account.getTenant().getId() == null) {
            throw new RuntimeException("Account tenant missing for account: " + accountId);
        }
        if (!tenantId.equals(account.getTenant().getId())) {
            throw new RuntimeException("Cross-tenant account access is not allowed");
        }
        return account;
    }

    private Long requireTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    private com.ledgora.tenant.entity.Tenant resolveCurrentTenant() {
        Long tenantId = requireTenantId();
        return com.ledgora.tenant.entity.Tenant.builder().id(tenantId).build();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private String generateAccountNumber() {
        String prefix = "LED";
        String uniquePart =
                UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
        String accountNumber = prefix + uniquePart;

        while (accountRepository.existsByAccountNumber(accountNumber)) {
            uniquePart =
                    UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
            accountNumber = prefix + uniquePart;
        }

        return accountNumber;
    }
}
