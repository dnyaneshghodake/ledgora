package com.ledgora.lien.controller;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.LienType;
import com.ledgora.lien.entity.AccountLien;
import com.ledgora.lien.service.AccountLienService;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Account Lien management controller.
 * Routes: /lien (list), /lien/account/{accountId} (per-account), /lien/{id}/approve, /lien/{id}/release
 */
@Controller
@RequestMapping("/lien")
public class LienController {

    private final AccountLienService lienService;
    private final AccountRepository accountRepository;

    public LienController(AccountLienService lienService, AccountRepository accountRepository) {
        this.lienService = lienService;
        this.accountRepository = accountRepository;
    }

    /** All pending liens for current tenant. */
    @GetMapping
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String listPendingLiens(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        List<AccountLien> pendingLiens = lienService.getPendingLiens(tenantId);
        model.addAttribute("liens", pendingLiens);
        model.addAttribute("pendingCount", pendingLiens.size());
        return "lien/lien-list";
    }

    /** Liens for a specific account. */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'ADMIN', 'MANAGER', 'TELLER', 'OPERATIONS', 'AUDITOR')")
    public String liensForAccount(@PathVariable Long accountId, Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Account account = accountRepository.findByIdAndTenantId(accountId, tenantId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        List<AccountLien> liens = lienService.getLiensByAccount(accountId, tenantId);
        BigDecimal totalLienAmount = lienService.getTotalActiveLienAmount(accountId);
        model.addAttribute("account", account);
        model.addAttribute("liens", liens);
        model.addAttribute("totalLienAmount", totalLienAmount);
        model.addAttribute("lienTypes", LienType.values());
        return "lien/lien-account";
    }

    /** Create a lien (maker step). */
    @PostMapping("/account/{accountId}/create")
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN', 'MANAGER')")
    public String createLien(
            @PathVariable Long accountId,
            @RequestParam BigDecimal lienAmount,
            @RequestParam String lienType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String lienReference,
            @RequestParam(required = false) String remarks,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Long tenantId = resolveTenantId(session);
            lienService.createLien(tenantId, accountId, lienAmount,
                    LienType.valueOf(lienType), startDate, endDate, lienReference, remarks);
            redirectAttributes.addFlashAttribute("message", "Lien submitted for approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lien/account/" + accountId;
    }

    /** Approve a lien (checker step). */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String approveLien(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            AccountLien lien = lienService.approveLien(id);
            redirectAttributes.addFlashAttribute("message", "Lien approved and applied to balance.");
            return "redirect:/lien/account/" + lien.getAccount().getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/lien";
        }
    }

    /** Release a lien. */
    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String releaseLien(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            AccountLien lien = lienService.releaseLien(id);
            redirectAttributes.addFlashAttribute("message", "Lien released.");
            return "redirect:/lien/account/" + lien.getAccount().getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/lien";
        }
    }

    private Long resolveTenantId(HttpSession session) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            Object sessionTenantId = session.getAttribute("tenantId");
            if (sessionTenantId instanceof Number n) tenantId = n.longValue();
            else if (sessionTenantId instanceof String s && !s.isBlank()) tenantId = Long.valueOf(s);
        }
        if (tenantId == null) throw new IllegalStateException("Tenant context not set");
        return tenantId;
    }
}
