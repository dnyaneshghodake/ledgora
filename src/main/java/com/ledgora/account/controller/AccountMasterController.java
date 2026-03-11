package com.ledgora.account.controller;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.service.AccountService;
import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.FreezeLevel;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.ownership.entity.AccountOwnership;
import com.ledgora.ownership.repository.AccountOwnershipRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Finacle-grade Account Master controller. Serves the 7-tab account-master.jsp with full data
 * wiring from existing services and repositories. No new entities or tables — reads from Account,
 * AccountBalance, AccountOwnership, and AuditLog.
 */
@Controller
@RequestMapping("/accounts")
public class AccountMasterController {

    private final AccountService accountService;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountOwnershipRepository ownershipRepository;
    private final AuditLogRepository auditLogRepository;

    public AccountMasterController(
            AccountService accountService,
            AccountBalanceRepository accountBalanceRepository,
            AccountOwnershipRepository ownershipRepository,
            AuditLogRepository auditLogRepository) {
        this.accountService = accountService;
        this.accountBalanceRepository = accountBalanceRepository;
        this.ownershipRepository = ownershipRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /** Account Master — 7-tab Finacle-style view for a single account. */
    @GetMapping("/{id}/master")
    public String viewAccountMaster(@PathVariable Long id, Model model) {
        Account account =
                accountService
                        .getAccountById(id)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Account not found: " + id));

        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // ── Header fields (eagerly resolved to avoid LazyInitializationException) ──
        model.addAttribute("account", account);
        model.addAttribute("accountNumber", account.getAccountNumber());
        model.addAttribute(
                "tenantName",
                account.getTenant() != null ? account.getTenant().getTenantName() : "--");
        model.addAttribute(
                "branchName",
                account.getHomeBranch() != null
                        ? account.getHomeBranch().getBranchName()
                        : (account.getBranch() != null
                                ? account.getBranch().getBranchName()
                                : account.getBranchCode()));
        model.addAttribute(
                "makerCheckerStatus",
                account.getApprovalStatus() != null
                        ? account.getApprovalStatus().name()
                        : "PENDING");
        model.addAttribute(
                "freezeLevel",
                account.getFreezeLevel() != null ? account.getFreezeLevel().name() : "NONE");
        model.addAttribute("freezeReason", account.getFreezeReason());

        // ── Tab 1: General Info ──
        model.addAttribute("accountName", account.getAccountName());
        model.addAttribute(
                "accountType",
                account.getAccountType() != null ? account.getAccountType().name() : "");
        model.addAttribute(
                "accountStatus",
                account.getStatus() != null ? account.getStatus().name() : "");
        model.addAttribute("currency", account.getCurrency());
        model.addAttribute("glAccountCode", account.getGlAccountCode());
        model.addAttribute(
                "homeBranchDisplay",
                account.getHomeBranch() != null
                        ? account.getHomeBranch().getBranchCode()
                                + " - "
                                + account.getHomeBranch().getBranchName()
                        : (account.getBranchCode() != null ? account.getBranchCode() : "--"));

        // ── Tab 2: Contact Info ──
        model.addAttribute("customerName", account.getCustomerName());
        model.addAttribute("customerPhone", account.getCustomerPhone());
        model.addAttribute("customerEmail", account.getCustomerEmail());

        // ── Tab 3: KYC & Identity ──
        model.addAttribute("customerNumber", account.getCustomerNumber());

        // ── Tab 4: Tax Profile — placeholder (no direct tax link on Account) ──

        // ── Tab 5: Freeze Control ──
        model.addAttribute("freezeLevels", FreezeLevel.values());

        // ── Tab 6: Relationships (ownership records for this account) ──
        try {
            List<AccountOwnership> ownerships =
                    ownershipRepository.findApprovedByAccountIdAndTenantId(id, tenantId);
            List<Map<String, Object>> ownershipList =
                    ownerships.stream()
                            .map(
                                    o -> {
                                        Map<String, Object> m = new HashMap<>();
                                        m.put("id", o.getId());
                                        m.put(
                                                "ownershipType",
                                                o.getOwnershipType() != null
                                                        ? o.getOwnershipType().name()
                                                        : "--");
                                        m.put(
                                                "customerNumber",
                                                o.getCustomerMaster() != null
                                                        ? o.getCustomerMaster()
                                                                .getCustomerNumber()
                                                        : "--");
                                        m.put(
                                                "customerName",
                                                o.getCustomerMaster() != null
                                                        ? o.getCustomerMaster().getFullName()
                                                        : "--");
                                        m.put(
                                                "status",
                                                o.getApprovalStatus() != null
                                                        ? o.getApprovalStatus().name()
                                                        : "--");
                                        m.put("ownershipPercentage", o.getOwnershipPercentage());
                                        m.put("isOperational", o.getIsOperational());
                                        return m;
                                    })
                            .collect(Collectors.toList());
            model.addAttribute("ownerships", ownershipList);
        } catch (Exception e) {
            model.addAttribute("ownerships", List.of());
        }

        // ── Tab 7: Audit & Approval ──
        model.addAttribute(
                "createdByUsername",
                account.getCreatedBy() != null ? account.getCreatedBy().getUsername() : "--");
        model.addAttribute("createdAt", account.getCreatedAt());
        model.addAttribute(
                "approvedByUsername",
                account.getApprovedBy() != null ? account.getApprovedBy().getUsername() : "--");
        model.addAttribute(
                "approvalStatus",
                account.getApprovalStatus() != null
                        ? account.getApprovalStatus().name()
                        : "PENDING");
        model.addAttribute("updatedAt", account.getUpdatedAt());

        // Balance data
        try {
            AccountBalance balance =
                    accountBalanceRepository.findByAccountId(id).orElse(null);
            model.addAttribute("balance", balance);
        } catch (Exception e) {
            model.addAttribute("balance", null);
        }

        // Freeze history from audit logs
        try {
            List<AuditLog> freezeLogs =
                    auditLogRepository.findByEntityAndEntityId("ACCOUNT", id).stream()
                            .filter(
                                    log ->
                                            log.getAction() != null
                                                    && log.getAction().contains("FREEZE"))
                            .collect(Collectors.toList());
            List<Map<String, Object>> freezeHistory =
                    freezeLogs.stream()
                            .map(
                                    log -> {
                                        Map<String, Object> entry = new HashMap<>();
                                        entry.put("timestamp", log.getTimestamp());
                                        entry.put("action", log.getAction());
                                        entry.put(
                                                "username",
                                                log.getUsername() != null
                                                        ? log.getUsername()
                                                        : "System");
                                        entry.put("details", log.getDetails());
                                        return entry;
                                    })
                            .collect(Collectors.toList());
            model.addAttribute("freezeHistory", freezeHistory);
        } catch (Exception e) {
            model.addAttribute("freezeHistory", List.of());
        }

        // Enum values for dropdowns
        model.addAttribute("accountTypes", AccountType.values());
        model.addAttribute("accountStatuses", AccountStatus.values());
        model.addAttribute("makerCheckerStatuses", MakerCheckerStatus.values());

        return "account/account-master";
    }

    /** Save account master fields (General + Contact tabs). */
    @PostMapping("/{id}/master/save")
    public String saveAccountMaster(
            @PathVariable Long id,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String glAccountCode,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {
        try {
            com.ledgora.account.dto.AccountDTO dto =
                    com.ledgora.account.dto.AccountDTO.builder()
                            .accountName(accountName)
                            .currency(currency)
                            .glAccountCode(glAccountCode)
                            .customerName(customerName)
                            .customerPhone(customerPhone)
                            .customerEmail(customerEmail)
                            .status(status)
                            .build();
            accountService.updateAccount(id, dto);
            redirectAttributes.addFlashAttribute("message", "Account master saved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id + "/master";
    }

    /** Save account master fields (General + Contact tabs). */
    @PostMapping("/{id}/master/save")
    public String saveAccountMaster(
            @PathVariable Long id,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String glAccountCode,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {
        try {
            com.ledgora.account.dto.AccountDTO dto =
                    com.ledgora.account.dto.AccountDTO.builder()
                            .accountName(accountName)
                            .currency(currency)
                            .glAccountCode(glAccountCode)
                            .customerName(customerName)
                            .customerPhone(customerPhone)
                            .customerEmail(customerEmail)
                            .status(status)
                            .build();
            accountService.updateAccount(id, dto);
            redirectAttributes.addFlashAttribute("message", "Account master saved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id + "/master";
    }

    /** Update freeze level on account (maker step). */
    @PostMapping("/{id}/master/freeze")
    public String updateFreeze(
            @PathVariable Long id,
            @RequestParam String freezeLevel,
            @RequestParam String freezeReason,
            RedirectAttributes redirectAttributes) {
        try {
            accountService.updateFreezeStatus(id, FreezeLevel.valueOf(freezeLevel), freezeReason);
            redirectAttributes.addFlashAttribute(
                    "message", "Freeze level updated to " + freezeLevel);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id + "/master#tabFreeze";
    }

    /** Approve account from master view (checker step). */
    @PostMapping("/{id}/master/approve")
    public String approveAccount(
            @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.approveAccount(id);
            redirectAttributes.addFlashAttribute("message", "Account approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id + "/master#tabAudit";
    }

    /** Reject account from master view (checker step). */
    @PostMapping("/{id}/master/reject")
    public String rejectAccount(
            @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.rejectAccount(id);
            redirectAttributes.addFlashAttribute("message", "Account rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/" + id + "/master#tabAudit";
    }
}
