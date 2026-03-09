package com.ledgora.transaction.controller;

import com.ledgora.common.enums.TransactionType;
import com.ledgora.common.exception.TransactionNotFoundException;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.account.service.AccountService;
import com.ledgora.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    public TransactionController(TransactionService transactionService, AccountService accountService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    @GetMapping
    public String listTransactions(@RequestParam(value = "accountNumber", required = false) String accountNumber,
                                   Model model) {
        if (accountNumber != null && !accountNumber.isEmpty()) {
            model.addAttribute("transactions", transactionService.getTransactionsByAccountNumber(accountNumber));
            model.addAttribute("accountNumber", accountNumber);
        } else {
            model.addAttribute("transactions", transactionService.getAllTransactions());
        }
        return "transaction/transactions";
    }

    @GetMapping("/{id}")
    public String viewTransaction(@PathVariable Long id, Model model) {
        // PART 11: Use custom TransactionNotFoundException instead of raw RuntimeException
        Transaction transaction = transactionService.getTransactionById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        model.addAttribute("transaction", transaction);
        model.addAttribute("ledgerEntries", transactionService.getLedgerEntriesByTransaction(id));
        return "transaction/transaction-view";
    }

    @GetMapping("/deposit")
    public String depositForm(Model model) {
        model.addAttribute("transactionDTO", new TransactionDTO());
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "transaction/transaction-deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
                          BindingResult result, Model model, RedirectAttributes redirectAttributes) {
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
                redirectAttributes.addFlashAttribute("message",
                        "Deposit submitted for approval. Ref: " + txn.getTransactionRef());
            } else {
                redirectAttributes.addFlashAttribute("message",
                        "Deposit successful! Ref: " + txn.getTransactionRef());
            }
            return "redirect:/transactions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-deposit";
        }
    }

    @GetMapping("/withdraw")
    public String withdrawForm(Model model) {
        model.addAttribute("transactionDTO", new TransactionDTO());
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "transaction/transaction-withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
                           BindingResult result, Model model, RedirectAttributes redirectAttributes) {
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
                redirectAttributes.addFlashAttribute("message",
                        "Withdrawal submitted for approval. Ref: " + txn.getTransactionRef());
            } else {
                redirectAttributes.addFlashAttribute("message",
                        "Withdrawal successful! Ref: " + txn.getTransactionRef());
            }
            return "redirect:/transactions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-withdraw";
        }
    }

    @GetMapping("/transfer")
    public String transferForm(Model model) {
        model.addAttribute("transactionDTO", new TransactionDTO());
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "transaction/transaction-transfer";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
                           BindingResult result, Model model, RedirectAttributes redirectAttributes) {
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
                redirectAttributes.addFlashAttribute("message",
                        "Transfer submitted for approval. Ref: " + txn.getTransactionRef());
            } else {
                redirectAttributes.addFlashAttribute("message",
                        "Transfer successful! Ref: " + txn.getTransactionRef());
            }
            return "redirect:/transactions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-transfer";
        }
    }

    /**
     * RBI-F8: Transaction history endpoint.
     *
     * IDOR protection: TransactionService.getTransactionsByAccountNumber() and
     * getLedgerEntriesByAccount() already filter by the current tenant context
     * (TenantContextHolder.getRequiredTenantId()), so cross-tenant access is blocked.
     *
     * For CUSTOMER-role users, a future enhancement should additionally validate
     * that the requested account belongs to the authenticated customer entity.
     * Current mitigation: CUSTOMER role has limited UI visibility (no history link
     * for other accounts), but server-side ownership check is recommended.
     */
    @GetMapping("/history/{accountNumber}")
    public String transactionHistory(@PathVariable String accountNumber, Model model) {
        // Validate account number format to prevent path traversal / injection
        if (accountNumber == null || !accountNumber.matches("^[A-Za-z0-9\\-]+$")) {
            throw new com.ledgora.common.exception.BusinessException("INVALID_ACCOUNT",
                    "Invalid account number format");
        }
        model.addAttribute("transactions", transactionService.getTransactionsByAccountNumber(accountNumber));
        model.addAttribute("ledgerEntries", transactionService.getLedgerEntriesByAccount(accountNumber));
        model.addAttribute("accountNumber", accountNumber);
        return "transaction/transaction-history";
    }

    /**
     * Server-side validation to prevent parameter tampering of financial fields.
     * Validates transaction type against allowed enum values and amount ranges.
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
        if (dto.getDestinationAccountNumber() != null && !dto.getDestinationAccountNumber().isEmpty()) {
            if (!dto.getDestinationAccountNumber().matches("^[A-Za-z0-9\\-]+$")) {
                return "Invalid destination account number format.";
            }
        }
        return null;
    }
}
