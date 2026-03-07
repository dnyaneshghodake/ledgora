package com.ledgora.controller;

import com.ledgora.dto.AccountDTO;
import com.ledgora.model.Account;
import com.ledgora.model.enums.AccountStatus;
import com.ledgora.model.enums.AccountType;
import com.ledgora.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
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
}
