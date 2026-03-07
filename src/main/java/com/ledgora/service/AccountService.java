package com.ledgora.service;

import com.ledgora.dto.AccountDTO;
import com.ledgora.model.Account;
import com.ledgora.model.User;
import com.ledgora.model.enums.AccountStatus;
import com.ledgora.model.enums.AccountType;
import com.ledgora.repository.AccountRepository;
import com.ledgora.repository.UserRepository;
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
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Account createAccount(AccountDTO dto) {
        String accountNumber = generateAccountNumber();

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        Account account = Account.builder()
                .accountNumber(accountNumber)
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
                .build();

        Account saved = accountRepository.save(account);
        log.info("Account created: {} for customer: {}", saved.getAccountNumber(), saved.getCustomerName());
        return saved;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    public Optional<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public List<Account> getAccountsByStatus(AccountStatus status) {
        return accountRepository.findByStatus(status);
    }

    public List<Account> getAccountsByType(AccountType type) {
        return accountRepository.findByAccountType(type);
    }

    public List<Account> searchByCustomerName(String name) {
        return accountRepository.findByCustomerNameContainingIgnoreCase(name);
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
