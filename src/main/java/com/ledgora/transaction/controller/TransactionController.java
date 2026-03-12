package com.ledgora.transaction.controller;

import com.ledgora.account.repository.AccountRepository;
import com.ledgora.account.service.AccountService;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.TransactionType;
import com.ledgora.common.exception.TransactionNotFoundException;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.service.TransactionService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public TransactionController(
            TransactionService transactionService,
            AccountService accountService,
            AccountRepository accountRepository,
            UserRepository userRepository) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String listTransactions(
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {
        StringBuilder qs = new StringBuilder();
        if (accountNumber != null && !accountNumber.isEmpty()) {
            qs.append("&accountNumber=").append(accountNumber);
            model.addAttribute(
                    "transactions",
                    transactionService.getTransactionsByAccountNumber(accountNumber));
            model.addAttribute("accountNumber", accountNumber);
        } else {
            org.springframework.data.domain.Page<com.ledgora.transaction.entity.Transaction>
                    txnPage = transactionService.getAllTransactionsPaged(page, size);
            model.addAttribute("transactions", txnPage.getContent());
            model.addAttribute("currentPage", txnPage.getNumber());
            model.addAttribute("totalPages", txnPage.getTotalPages());
            model.addAttribute("totalElements", txnPage.getTotalElements());
            model.addAttribute("pageSize", size);
        }
        model.addAttribute("baseUrl", "/transactions");
        model.addAttribute("queryString", qs.toString());
        return "transaction/transactions";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String viewTransaction(@PathVariable Long id, Model model) {
        // PART 11: Use custom TransactionNotFoundException instead of raw RuntimeException
        Transaction transaction =
                transactionService
                        .getTransactionById(id)
                        .orElseThrow(() -> new TransactionNotFoundException(id));
        model.addAttribute("transaction", transaction);
        model.addAttribute("ledgerEntries", transactionService.getLedgerEntriesByTransaction(id));
        return "transaction/transaction-view";
    }

    @GetMapping("/deposit")
    @PreAuthorize("hasAnyRole('MAKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
    public String depositForm(Model model) {
        model.addAttribute("transactionDTO", new TransactionDTO());
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "transaction/transaction-deposit";
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('MAKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
    public String deposit(
            @Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-deposit";
        }
        String paramError = validateFinancialParams(dto, "DEPOSIT");
        if (paramError != null) {
            model.addAttribute("error", paramError);
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-deposit";
        }
        try {
            Transaction txn = transactionService.deposit(dto);
            if (txn.getStatus() == com.ledgora.common.enums.TransactionStatus.PENDING_APPROVAL) {
                redirectAttributes.addFlashAttribute(
                        "message",
                        "Deposit submitted for approval. Ref: " + txn.getTransactionRef());
            } else {
                redirectAttributes.addFlashAttribute(
                        "message", "Deposit successful! Ref: " + txn.getTransactionRef());
            }
            return "redirect:/transactions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-deposit";
        }
    }

    @GetMapping("/withdraw")
    @PreAuthorize("hasAnyRole('MAKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
    public String withdrawForm(Model model) {
        model.addAttribute("transactionDTO", new TransactionDTO());
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "transaction/transaction-withdraw";
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyRole('MAKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
    public String withdraw(
            @Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-withdraw";
        }
        String paramError = validateFinancialParams(dto, "WITHDRAWAL");
        if (paramError != null) {
            model.addAttribute("error", paramError);
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-withdraw";
        }
        try {
            Transaction txn = transactionService.withdraw(dto);
            if (txn.getStatus() == com.ledgora.common.enums.TransactionStatus.PENDING_APPROVAL) {
                redirectAttributes.addFlashAttribute(
                        "message",
                        "Withdrawal submitted for approval. Ref: " + txn.getTransactionRef());
            } else {
                redirectAttributes.addFlashAttribute(
                        "message", "Withdrawal successful! Ref: " + txn.getTransactionRef());
            }
            return "redirect:/transactions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-withdraw";
        }
    }

    @GetMapping("/transfer")
    @PreAuthorize("hasAnyRole('MAKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
    public String transferForm(Model model) {
        model.addAttribute("transactionDTO", new TransactionDTO());
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "transaction/transaction-transfer";
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('MAKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
    public String transfer(
            @Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-transfer";
        }
        String paramError = validateFinancialParams(dto, "TRANSFER");
        if (paramError != null) {
            model.addAttribute("error", paramError);
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-transfer";
        }
        try {
            Transaction txn = transactionService.transfer(dto);
            if (txn.getStatus() == com.ledgora.common.enums.TransactionStatus.PENDING_APPROVAL) {
                redirectAttributes.addFlashAttribute(
                        "message",
                        "Transfer submitted for approval. Ref: " + txn.getTransactionRef());
            } else {
                redirectAttributes.addFlashAttribute(
                        "message", "Transfer successful! Ref: " + txn.getTransactionRef());
            }
            return "redirect:/transactions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-transfer";
        }
    }

    // ===== Maker-Checker Approval Workflow (UI-1 fix) =====

    /**
     * Pending transactions awaiting checker approval. Only CHECKER, ADMIN, MANAGER can see this
     * queue.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String pendingTransactions(Model model) {
        model.addAttribute(
                "pendingTransactions", transactionService.getPendingApprovalTransactions());
        model.addAttribute("pendingCount", transactionService.countPendingApproval());
        return "transaction/transaction-pending";
    }

    /**
     * Approve a pending transaction (checker action). Maker-checker enforcement: checker must
     * differ from maker (enforced by TransactionService).
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String approveTransaction(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Approved") String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            Transaction approved = transactionService.approveTransaction(id, remarks);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Transaction " + approved.getTransactionRef() + " approved and posted.");
            return "redirect:/transactions/pending";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Approval failed: " + e.getMessage());
            return "redirect:/transactions/pending";
        }
    }

    /**
     * Reject a pending transaction (checker action). Maker-checker enforcement: checker must differ
     * from maker (enforced by TransactionService).
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String rejectTransaction(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Rejected by checker") String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            Transaction rejected = transactionService.rejectTransaction(id, remarks);
            redirectAttributes.addFlashAttribute(
                    "message", "Transaction " + rejected.getTransactionRef() + " rejected.");
            return "redirect:/transactions/pending";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Rejection failed: " + e.getMessage());
            return "redirect:/transactions/pending";
        }
    }

    /**
     * RBI-F8: Transaction history endpoint.
     *
     * <p>IDOR protection: TransactionService.getTransactionsByAccountNumber() and
     * getLedgerEntriesByAccount() already filter by the current tenant context
     * (TenantContextHolder.getRequiredTenantId()), so cross-tenant access is blocked.
     *
     * <p>For CUSTOMER-role users, a future enhancement should additionally validate that the
     * requested account belongs to the authenticated customer entity. Current mitigation: CUSTOMER
     * role has limited UI visibility (no history link for other accounts), but server-side
     * ownership check is recommended.
     */
    @GetMapping("/history/{accountNumber}")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR', 'CUSTOMER')")
    public String transactionHistory(@PathVariable String accountNumber, Model model) {
        // Validate account number format to prevent path traversal / injection
        if (accountNumber == null || !accountNumber.matches("^[A-Za-z0-9\\-]+$")) {
            throw new com.ledgora.common.exception.BusinessException(
                    "INVALID_ACCOUNT", "Invalid account number format");
        }

        // UI-2: IDOR prevention for CUSTOMER role.
        // Tenant isolation blocks cross-tenant access, but within a tenant
        // a CUSTOMER could access another customer's account by URL manipulation.
        // Validate that the requested account belongs to the authenticated user.
        org.springframework.security.core.Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        boolean isCustomerRole =
                auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
        if (isCustomerRole) {
            // For CUSTOMER users, verify account ownership via the user's linked accounts
            String username = auth.getName();
            Long tenantId = com.ledgora.tenant.context.TenantContextHolder.getRequiredTenantId();
            com.ledgora.account.entity.Account account =
                    accountRepository
                            .findByAccountNumberAndTenantId(accountNumber, tenantId)
                            .orElseThrow(
                                    () ->
                                            new com.ledgora.common.exception.BusinessException(
                                                    "ACCOUNT_NOT_FOUND",
                                                    "Account not found: " + accountNumber));
            // Check if the account's customer is linked to the authenticated user
            if (account.getCustomerName() == null || account.getCustomer() == null) {
                throw new com.ledgora.common.exception.BusinessException(
                        "ACCESS_DENIED",
                        "Account " + accountNumber + " is not a customer account.");
            }
            com.ledgora.auth.entity.User user =
                    userRepository
                            .findByUsername(username)
                            .orElseThrow(
                                    () ->
                                            new com.ledgora.common.exception.BusinessException(
                                                    "USER_NOT_FOUND", "Current user not found"));
            // Match by comparing the user's full name with the account's customer name
            // In production, this should use a direct User→Customer FK relationship
            if (user.getFullName() == null
                    || !user.getFullName().equalsIgnoreCase(account.getCustomerName())) {
                throw new com.ledgora.common.exception.BusinessException(
                        "ACCESS_DENIED", "You do not have access to account " + accountNumber);
            }
        }

        model.addAttribute(
                "transactions", transactionService.getTransactionsByAccountNumber(accountNumber));
        model.addAttribute(
                "ledgerEntries", transactionService.getLedgerEntriesByAccount(accountNumber));
        model.addAttribute("accountNumber", accountNumber);
        return "transaction/transaction-history";
    }

    /**
     * Server-side validation to prevent parameter tampering of financial fields. Validates
     * transaction type against allowed enum values and amount ranges.
     */
    private String validateFinancialParams(TransactionDTO dto, String expectedType) {
        // Validate transaction type matches expected operation and is a known enum value
        if (dto.getTransactionType() != null) {
            try {
                TransactionType.valueOf(dto.getTransactionType());
            } catch (IllegalArgumentException e) {
                return "Invalid transaction type.";
            }
            if (!dto.getTransactionType().equals(expectedType)) {
                return "Invalid transaction type for this operation.";
            }
        }
        // Validate amount is within acceptable range (prevent extreme values)
        if (dto.getAmount() != null) {
            if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return "Amount must be greater than zero.";
            }
            if (dto.getAmount().compareTo(new BigDecimal("999999999999.99")) > 0) {
                return "Amount exceeds maximum allowed value.";
            }
            if (dto.getAmount().scale() > 2) {
                return "Amount cannot have more than 2 decimal places.";
            }
        }
        // Validate account number format (alphanumeric with hyphens only)
        if (dto.getSourceAccountNumber() != null && !dto.getSourceAccountNumber().isEmpty()) {
            if (!dto.getSourceAccountNumber().matches("^[A-Za-z0-9\\-]+$")) {
                return "Invalid source account number format.";
            }
        }
        if (dto.getDestinationAccountNumber() != null
                && !dto.getDestinationAccountNumber().isEmpty()) {
            if (!dto.getDestinationAccountNumber().matches("^[A-Za-z0-9\\-]+$")) {
                return "Invalid destination account number format.";
            }
        }
        return null;
    }
}
