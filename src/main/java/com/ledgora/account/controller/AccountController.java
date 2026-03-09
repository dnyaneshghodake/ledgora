package com.ledgora.account.controller;

import com.ledgora.account.dto.AccountDTO;
import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.service.AccountService;
import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.balance.service.CbsBalanceEngine;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.lien.entity.AccountLien;
import com.ledgora.lien.service.AccountLienService;
import com.ledgora.ownership.entity.AccountOwnership;
import com.ledgora.ownership.service.AccountOwnershipService;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.repository.TransactionRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CbsBalanceEngine balanceEngine;
    private final AuditLogRepository auditLogRepository;
    private final AccountOwnershipService ownershipService;
    private final AccountLienService lienService;
    private final TransactionRepository transactionRepository;

    public AccountController(AccountService accountService, CbsBalanceEngine balanceEngine,
                             AuditLogRepository auditLogRepository,
                             AccountOwnershipService ownershipService,
                             AccountLienService lienService,
                             TransactionRepository transactionRepository) {
        this.accountService = accountService;
        this.balanceEngine = balanceEngine;
        this.auditLogRepository = auditLogRepository;
        this.ownershipService = ownershipService;
        this.lienService = lienService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public String listAccounts(@RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = "search", required = false) String search,
                               Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("accounts", accountService.searchByCustomerName(search));
            model.addAttribute("search", search);
        } else if (status != null && !status.isEmpty()) {
            model.addAttribute("accounts", accountService.getAccountsByStatus(AccountStatus.valueOf(status)));
            model.addAttribute("selectedStatus", status);
        } else if (type != null && !type.isEmpty()) {
            model.addAttribute("accounts", accountService.getAccountsByType(AccountType.valueOf(type)));
            model.addAttribute("selectedType", type);
        } else {
            model.addAttribute("accounts", accountService.getAllAccounts());
        }
        model.addAttribute("accountTypes", AccountType.values());
        model.addAttribute("accountStatuses", AccountStatus.values());
        return "account/accounts";
    }

    @GetMapping("/create")
    public String createAccountForm(Model model) {
        model.addAttribute("accountDTO", new AccountDTO());
        model.addAttribute("accountTypes", AccountType.values());
        return "account/account-create";
    }

    @PostMapping("/create")
    public String createAccount(@Valid @ModelAttribute("accountDTO") AccountDTO accountDTO,
                                BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accountTypes", AccountType.values());
            return "account/account-create";
        }
        try {
            Account account = accountService.createAccount(accountDTO);
            redirectAttributes.addFlashAttribute("message",
                    "Account created successfully: " + account.getAccountNumber());
            return "redirect:/accounts";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accountTypes", AccountType.values());
            return "account/account-create";
        }
    }

    @GetMapping("/{id}")
    public String viewAccount(@PathVariable Long id, Model model) {
        Account account = accountService.getAccountById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        model.addAttribute("account", account);
        model.addAttribute("accountTypes", AccountType.values());

        // Eagerly resolve lazy User fields for JSP audit section to avoid LazyInitializationException
        String createdByUsername = "";
        try { if (account.getCreatedBy() != null) createdByUsername = account.getCreatedBy().getUsername(); } catch (Exception e) { /* lazy proxy failed */ }
        model.addAttribute("createdByUsername", createdByUsername);
        String approvedByUsername = "";
        try { if (account.getApprovedBy() != null) approvedByUsername = account.getApprovedBy().getUsername(); } catch (Exception e) { /* lazy proxy failed */ }
        model.addAttribute("approvedByUsername", approvedByUsername);

        // Load balance data for Balances tab
        try {
            AccountBalance cbsBalance = balanceEngine.getCbsBalance(id);
            model.addAttribute("availableBalance", cbsBalance.getAvailableBalance());
            model.addAttribute("totalLien", cbsBalance.getLienBalance());
        } catch (Exception e) {
            model.addAttribute("availableBalance", account.getBalance());
            model.addAttribute("totalLien", "0.00");
        }

        // Load ownership data for Ownership tab (safe projection to avoid lazy-loading)
        try {
            Long tenantId = TenantContextHolder.getTenantId();
            List<AccountOwnership> ownershipEntities = ownershipService.getOwnershipsByAccount(id, tenantId);
            List<Map<String, Object>> ownerships = ownershipEntities.stream().map(o -> {
                Map<String, Object> m = new HashMap<>();
                try { m.put("customerName", o.getCustomerMaster() != null ? o.getCustomerMaster().getFullName() : ""); } catch (Exception e) { m.put("customerName", ""); }
                m.put("ownershipType", o.getOwnershipType() != null ? o.getOwnershipType().name() : "");
                m.put("ownershipPercentage", o.getOwnershipPercentage());
                m.put("operational", o.getIsOperational());
                return m;
            }).collect(Collectors.toList());
            model.addAttribute("ownerships", ownerships);
        } catch (Exception e) {
            model.addAttribute("ownerships", List.of());
        }

        // Load lien data for Liens tab (safe projection)
        try {
            List<AccountLien> lienEntities = lienService.getActiveLiensByAccount(id);
            List<Map<String, Object>> liens = lienEntities.stream().map(l -> {
                Map<String, Object> m = new HashMap<>();
                m.put("lienAmount", l.getLienAmount());
                m.put("lienType", l.getLienType() != null ? l.getLienType().name() : "");
                m.put("startDate", l.getStartDate());
                m.put("endDate", l.getEndDate());
                m.put("status", l.getStatus() != null ? l.getStatus().name() : "");
                return m;
            }).collect(Collectors.toList());
            model.addAttribute("liens", liens);
        } catch (Exception e) {
            model.addAttribute("liens", List.of());
        }

        // Load recent transactions for Transactions tab (safe projection)
        try {
            List<Transaction> txEntities = transactionRepository.findByAccountId(id);
            List<Map<String, Object>> recentTransactions = txEntities.stream().limit(20).map(tx -> {
                Map<String, Object> m = new HashMap<>();
                m.put("transactionDate", tx.getCreatedAt());
                m.put("transactionType", tx.getTransactionType() != null ? tx.getTransactionType().name() : "");
                m.put("amount", tx.getAmount());
                m.put("balanceAfter", "--");
                return m;
            }).collect(Collectors.toList());
            model.addAttribute("recentTransactions", recentTransactions);
        } catch (Exception e) {
            model.addAttribute("recentTransactions", List.of());
        }

        // Load freeze history from audit logs
        try {
            List<AuditLog> freezeLogs = auditLogRepository.findByEntityAndEntityId("ACCOUNT", id)
                    .stream()
                    .filter(log -> log.getAction() != null && log.getAction().contains("FREEZE"))
                    .collect(Collectors.toList());
            List<Map<String, Object>> freezeHistory = freezeLogs.stream().map(log -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("timestamp", log.getTimestamp());
                entry.put("action", log.getAction());
                entry.put("username", log.getUsername() != null ? log.getUsername() : "System");
                entry.put("checker", "--");
                entry.put("details", log.getDetails());
                return entry;
            }).collect(Collectors.toList());
            model.addAttribute("freezeHistory", freezeHistory);
        } catch (Exception e) {
            model.addAttribute("freezeHistory", List.of());
        }
        // Load lien history from audit logs
        try {
            List<AuditLog> lienLogs = auditLogRepository.findByEntityAndEntityId("ACCOUNT", id)
                    .stream()
                    .filter(log -> log.getAction() != null && log.getAction().contains("LIEN"))
                    .collect(Collectors.toList());
            List<Map<String, Object>> lienHistory = lienLogs.stream().map(log -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("timestamp", log.getTimestamp());
                entry.put("action", log.getAction());
                entry.put("username", log.getUsername() != null ? log.getUsername() : "System");
                entry.put("checker", "--");
                entry.put("details", log.getDetails());
                entry.put("amount", "--");
                return entry;
            }).collect(Collectors.toList());
            model.addAttribute("lienHistory", lienHistory);
        } catch (Exception e) {
            model.addAttribute("lienHistory", List.of());
        }
        return "account/account-view";
    }

    @GetMapping("/{id}/edit")
    public String editAccountForm(@PathVariable Long id, Model model) {
        Account account = accountService.getAccountById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setAccountName(account.getAccountName());
        dto.setAccountType(account.getAccountType().name());
        dto.setBalance(account.getBalance());
        dto.setCurrency(account.getCurrency());
        dto.setBranchCode(account.getBranchCode());
        dto.setCustomerName(account.getCustomerName());
        dto.setCustomerEmail(account.getCustomerEmail());
        dto.setCustomerPhone(account.getCustomerPhone());
        dto.setGlAccountCode(account.getGlAccountCode());
        dto.setStatus(account.getStatus() != null ? account.getStatus().name() : null);
        dto.setFreezeLevel(account.getFreezeLevel() != null ? account.getFreezeLevel().name() : null);
        dto.setApprovalStatus(account.getApprovalStatus() != null ? account.getApprovalStatus().name() : null);
        dto.setCreatedAt(account.getCreatedAt() != null ? account.getCreatedAt().toString() : null);
        model.addAttribute("accountDTO", dto);
        model.addAttribute("accountTypes", AccountType.values());
        return "account/account-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateAccount(@PathVariable Long id,
                                @Valid @ModelAttribute("accountDTO") AccountDTO accountDTO,
                                BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accountTypes", AccountType.values());
            return "account/account-edit";
        }
        try {
            accountService.updateAccount(id, accountDTO);
            redirectAttributes.addFlashAttribute("message", "Account updated successfully");
            return "redirect:/accounts/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accountTypes", AccountType.values());
            return "account/account-edit";
        }
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        try {
            accountService.updateAccountStatus(id, AccountStatus.valueOf(status));
            redirectAttributes.addFlashAttribute("message", "Account status updated to " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id;
    }

    /**
     * H2: Approve an account (checker step with maker-checker enforcement).
     */
    @PostMapping("/{id}/approve")
    public String approveAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.approveAccount(id);
            redirectAttributes.addFlashAttribute("message", "Account approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id;
    }

    /**
     * H2: Reject an account (checker step with maker-checker enforcement).
     */
    @PostMapping("/{id}/reject")
    public String rejectAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.rejectAccount(id);
            redirectAttributes.addFlashAttribute("error", "Account rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id;
    }

    /**
     * API endpoint for AJAX account search by partial account number or name.
     * Used by lookup modals on transaction and account opening screens.
     */
    @GetMapping("/api/search")
    @ResponseBody
    public List<Map<String, Object>> searchAccountsApi(@RequestParam("q") String query) {
        List<Account> accounts = accountService.searchAccounts(query);
        return accounts.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("accountNumber", a.getAccountNumber());
            m.put("accountName", a.getAccountName());
            m.put("accountType", a.getAccountType() != null ? a.getAccountType().name() : null);
            m.put("status", a.getStatus() != null ? a.getStatus().name() : null);
            m.put("balance", a.getBalance());
            m.put("currency", a.getCurrency());
            m.put("customerName", a.getCustomerName());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * API endpoint for AJAX account lookup by exact account number.
     * Used by transaction screens and account selection components.
     */
    @GetMapping("/api/lookup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> lookupAccount(
            @RequestParam("accountNumber") String accountNumber) {
        Optional<Account> accountOpt = accountService.getAccountByNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Account account = accountOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("id", account.getId());
        result.put("accountNumber", account.getAccountNumber());
        result.put("accountName", account.getAccountName());
        result.put("accountType", account.getAccountType() != null ? account.getAccountType().name() : null);
        result.put("status", account.getStatus() != null ? account.getStatus().name() : null);
        result.put("balance", account.getBalance());
        result.put("currency", account.getCurrency());
        result.put("freezeLevel", account.getFreezeLevel() != null ? account.getFreezeLevel().name() : null);
        result.put("freezeReason", account.getFreezeReason());
        result.put("customerName", account.getCustomerName());
        // Use CbsBalanceEngine for real available balance and lien data
        try {
            AccountBalance cbsBalance = balanceEngine.getCbsBalance(account.getId());
            result.put("availableBalance", cbsBalance.getAvailableBalance());
            result.put("totalLien", cbsBalance.getLienBalance());
            result.put("ledgerBalance", cbsBalance.getLedgerBalance());
            result.put("actualBalance", cbsBalance.getActualTotalBalance());
            result.put("shadowBalance", cbsBalance.getShadowTotalBalance());
        } catch (Exception e) {
            // Fallback to raw balance if balance engine fails
            result.put("availableBalance", account.getBalance());
            result.put("totalLien", "0.00");
        }
        return ResponseEntity.ok(result);
    }
}
