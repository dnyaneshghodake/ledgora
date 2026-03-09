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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AccountService(AccountRepository accountRepository,
                          AccountBalanceRepository accountBalanceRepository,
                          UserRepository userRepository,
                          AuditService auditService) {
        this.accountRepository = accountRepository;
        this.accountBalanceRepository = accountBalanceRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Account createAccount(AccountDTO dto) {
        String accountNumber = generateAccountNumber();
        User currentUser = getCurrentUser();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .tenant(resolveCurrentTenant())
                .accountName(dto.getAccountName())
                .accountType(AccountType.valueOf(dto.getAccountType()))
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .branchCode(dto.getBranchCode())
                .customerName(dto.getCustomerName())
                .customerEmail(dto.getCustomerEmail())
                .customerPhone(dto.getCustomerPhone())
                .glAccountCode(dto.getGlAccountCode())
                .createdBy(currentUser)
                // H2: Account starts PENDING approval; requires checker to approve
                .approvalStatus(MakerCheckerStatus.PENDING)
                .build();

        Account saved = accountRepository.save(account);

        // Create account balance cache
        AccountBalance balance = AccountBalance.builder()
                .account(saved)
                .ledgerBalance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .holdAmount(BigDecimal.ZERO)
                .build();
        accountBalanceRepository.save(balance);

        // Audit log
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logAccountCreation(userId, saved.getId(), saved.getAccountNumber());

        log.info("Account created: {} for customer: {}", saved.getAccountNumber(), saved.getCustomerName());
        return saved;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findByTenantId(requireTenantId());
    }

    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id)
                .filter(a -> a.getTenant() != null && requireTenantId().equals(a.getTenant().getId()));
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
        return accountRepository.findByTenantIdAndCustomerNameContainingIgnoreCase(requireTenantId(), name);
    }

    public List<Account> searchAccounts(String query) {
        return accountRepository.searchByTenantId(requireTenantId(), query);
    }

    public List<Account> getAccountsByCustomerId(Long customerId) {
        return accountRepository.findByTenantIdAndCustomerId(requireTenantId(), customerId);
    }

    @Transactional
    public Account updateAccount(Long id, AccountDTO dto) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        account.setAccountName(dto.getAccountName());
        if (dto.getCustomerName() != null) account.setCustomerName(dto.getCustomerName());
        if (dto.getCustomerEmail() != null) account.setCustomerEmail(dto.getCustomerEmail());
        if (dto.getCustomerPhone() != null) account.setCustomerPhone(dto.getCustomerPhone());
        if (dto.getBranchCode() != null) account.setBranchCode(dto.getBranchCode());
        if (dto.getGlAccountCode() != null) account.setGlAccountCode(dto.getGlAccountCode());

        return accountRepository.save(account);
    }

    @Transactional
    public Account updateAccountStatus(Long id, AccountStatus status) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        account.setStatus(status);
        log.info("Account {} status changed to {}", account.getAccountNumber(), status);
        return accountRepository.save(account);
    }

    /**
     * H2: Approve an account (checker step). Enforces maker-checker.
     */
    @Transactional
    public Account approveAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        if (account.getApprovalStatus() != MakerCheckerStatus.PENDING) {
            throw new RuntimeException("Account is not pending approval. Current status: " + account.getApprovalStatus());
        }

        User currentUser = getCurrentUser();
        // Maker-checker: approver must differ from creator
        if (account.getCreatedBy() != null && currentUser != null
                && account.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot approve your own account (maker-checker violation)");
        }

        account.setApprovalStatus(MakerCheckerStatus.APPROVED);
        account.setApprovedBy(currentUser);
        Account saved = accountRepository.save(account);

        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(userId, "ACCOUNT_APPROVE", "ACCOUNT", saved.getId(),
                "Account approved: " + saved.getAccountNumber(), null);

        log.info("Account {} approved by user {}", saved.getAccountNumber(),
                currentUser != null ? currentUser.getUsername() : "system");
        return saved;
    }

    /**
     * H2: Reject an account (checker step). Enforces maker-checker.
     */
    @Transactional
    public Account rejectAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        if (account.getApprovalStatus() != MakerCheckerStatus.PENDING) {
            throw new RuntimeException("Account is not pending approval. Current status: " + account.getApprovalStatus());
        }

        User currentUser = getCurrentUser();
        // Maker-checker: rejector must differ from creator
        if (account.getCreatedBy() != null && currentUser != null
                && account.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot reject your own account (maker-checker violation)");
        }

        account.setApprovalStatus(MakerCheckerStatus.REJECTED);
        Account saved = accountRepository.save(account);

        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(userId, "ACCOUNT_REJECT", "ACCOUNT", saved.getId(),
                "Account rejected: " + saved.getAccountNumber(), null);

        log.info("Account {} rejected by user {}", saved.getAccountNumber(),
                currentUser != null ? currentUser.getUsername() : "system");
        return saved;
    }

    @Transactional
    public void updateBalance(Long accountId, BigDecimal newBalance) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    public long countByStatus(AccountStatus status) {
        return accountRepository.countByStatus(status);
    }

    public long countAll() {
        return accountRepository.count();
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
        String uniquePart = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
        String accountNumber = prefix + uniquePart;

        while (accountRepository.existsByAccountNumber(accountNumber)) {
            uniquePart = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
            accountNumber = prefix + uniquePart;
        }

        return accountNumber;
    }
}
