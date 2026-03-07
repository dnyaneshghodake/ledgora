package com.ledgora.transaction.controller;

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
        Transaction transaction = transactionService.getTransactionById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
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
        try {
            Transaction txn = transactionService.deposit(dto);
            redirectAttributes.addFlashAttribute("message",
                    "Deposit successful! Ref: " + txn.getTransactionRef());
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
        try {
            Transaction txn = transactionService.withdraw(dto);
            redirectAttributes.addFlashAttribute("message",
                    "Withdrawal successful! Ref: " + txn.getTransactionRef());
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
        try {
            Transaction txn = transactionService.transfer(dto);
            redirectAttributes.addFlashAttribute("message",
                    "Transfer successful! Ref: " + txn.getTransactionRef());
            return "redirect:/transactions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction/transaction-transfer";
        }
    }

    @GetMapping("/history/{accountNumber}")
    public String transactionHistory(@PathVariable String accountNumber, Model model) {
        model.addAttribute("transactions", transactionService.getTransactionsByAccountNumber(accountNumber));
        model.addAttribute("ledgerEntries", transactionService.getLedgerEntriesByAccount(accountNumber));
        model.addAttribute("accountNumber", accountNumber);
        return "transaction/transaction-history";
    }
}
