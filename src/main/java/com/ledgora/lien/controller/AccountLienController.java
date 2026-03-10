package com.ledgora.lien.controller;

import com.ledgora.common.enums.LienType;
import com.ledgora.lien.entity.AccountLien;
import com.ledgora.lien.service.AccountLienService;
import com.ledgora.tenant.context.TenantContextHolder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Controller for Account Lien management. */
@Controller
@RequestMapping("/liens")
public class AccountLienController {

    private final AccountLienService lienService;

    public AccountLienController(AccountLienService lienService) {
        this.lienService = lienService;
    }

    @GetMapping
    public String listPending(Model model) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<AccountLien> pending = lienService.getPendingLiens(tenantId);
        model.addAttribute("pendingLiens", pending);
        model.addAttribute("lienTypes", LienType.values());
        return "lien/lien-list";
    }

    @GetMapping("/account/{accountId}")
    public String listByAccount(@PathVariable Long accountId, Model model) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<AccountLien> liens = lienService.getLiensByAccount(accountId, tenantId);
        BigDecimal totalLien = lienService.getTotalActiveLienAmount(accountId);
        model.addAttribute("liens", liens);
        model.addAttribute("accountId", accountId);
        model.addAttribute("totalLienAmount", totalLien);
        return "lien/lien-account";
    }

    @PostMapping("/create")
    public String createLien(
            @RequestParam("accountId") Long accountId,
            @RequestParam("lienAmount") BigDecimal lienAmount,
            @RequestParam("lienType") String lienType,
            @RequestParam("startDate") String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr,
            @RequestParam(value = "lienReference", required = false) String lienReference,
            @RequestParam(value = "remarks", required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate =
                    endDateStr != null && !endDateStr.isEmpty()
                            ? LocalDate.parse(endDateStr)
                            : null;
            lienService.createLien(
                    tenantId,
                    accountId,
                    lienAmount,
                    LienType.valueOf(lienType),
                    startDate,
                    endDate,
                    lienReference,
                    remarks);
            redirectAttributes.addFlashAttribute("success", "Lien submitted for approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/liens";
    }

    @PostMapping("/approve/{id}")
    public String approveLien(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            lienService.approveLien(id);
            redirectAttributes.addFlashAttribute("success", "Lien approved and applied.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/liens";
    }

    @PostMapping("/release/{id}")
    public String releaseLien(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            lienService.releaseLien(id);
            redirectAttributes.addFlashAttribute("success", "Lien released.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/liens";
    }
}
