package com.ledgora.controller;

import com.ledgora.dto.TransactionDTO;
import com.ledgora.model.Transaction;
import com.ledgora.service.AccountService;
import com.ledgora.service.TransactionService;
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
        return "transactions";
    }

    @GetMapping("/{id}")
    public String viewTransaction(@PathVariable Long id, Model model) {
        Transaction transaction = transactionService.getTransactionById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        model.addAttribute("transaction", transaction);
        model.addAttribute("ledgerEntries", transactionService.getLedgerEntriesByTransaction(id));
        return "transaction-view";
    }

    @GetMapping("/deposit")
    public String depositForm(Model model) {
        model.addAttribute("transactionDTO", new TransactionDTO());
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "transaction-deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
                          BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction-deposit";
        }
        try {
            Transaction txn = transactionService.deposit(dto);
            redirectAttributes.addFlashAttribute("message",
                    "Deposit successful! Ref: " + txn.getTransactionRef());
            return "redirect:/transactions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction-deposit";
        }
    }

    @GetMapping("/withdraw")
    public String withdrawForm(Model model) {
        model.addAttribute("transactionDTO", new TransactionDTO());
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "transaction-withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
                           BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction-withdraw";
        }
        try {
            Transaction txn = transactionService.withdraw(dto);
            redirectAttributes.addFlashAttribute("message",
                    "Withdrawal successful! Ref: " + txn.getTransactionRef());
            return "redirect:/transactions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction-withdraw";
        }
    }

    @GetMapping("/transfer")
    public String transferForm(Model model) {
        model.addAttribute("transactionDTO", new TransactionDTO());
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "transaction-transfer";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transactionDTO") TransactionDTO dto,
                           BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction-transfer";
        }
        try {
            Transaction txn = transactionService.transfer(dto);
            redirectAttributes.addFlashAttribute("message",
                    "Transfer successful! Ref: " + txn.getTransactionRef());
            return "redirect:/transactions";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accounts", accountService.getAllAccounts());
            return "transaction-transfer";
        }
    }

    @GetMapping("/history/{accountNumber}")
    public String transactionHistory(@PathVariable String accountNumber, Model model) {
        model.addAttribute("transactions", transactionService.getTransactionsByAccountNumber(accountNumber));
        model.addAttribute("ledgerEntries", transactionService.getLedgerEntriesByAccount(accountNumber));
        model.addAttribute("accountNumber", accountNumber);
        return "transaction-history";
    }
}
