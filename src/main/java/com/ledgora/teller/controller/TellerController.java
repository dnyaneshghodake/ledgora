package com.ledgora.teller.controller;

import com.ledgora.common.exception.BusinessException;
import com.ledgora.teller.dto.TellerCashRequest;
import com.ledgora.teller.dto.TellerCloseRequest;
import com.ledgora.teller.dto.TellerOpenRequest;
import com.ledgora.teller.entity.TellerSession;
import com.ledgora.teller.entity.VaultTransfer;
import com.ledgora.teller.service.CashEngineService;
import com.ledgora.teller.service.TellerReportService;
import com.ledgora.teller.service.TellerSessionService;
import com.ledgora.teller.service.VaultTransferService;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.entity.Transaction;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * TellerController  Finacle-grade teller operations UI endpoints.
 *
 * <p>Note: this module is implemented as JSP MVC controllers (not REST). All operations use
 * service-layer validations for RBI-grade enforcement.
 */
@Controller
@RequestMapping("/teller")
public class TellerController {

    private final TellerSessionService tellerSessionService;
    private final CashEngineService cashEngineService;
    private final VaultTransferService vaultTransferService;
    private final TellerReportService tellerReportService;
    private final TenantService tenantService;

    public TellerController(
            TellerSessionService tellerSessionService,
            CashEngineService cashEngineService,
            VaultTransferService vaultTransferService,
            TellerReportService tellerReportService,
            TenantService tenantService) {
        this.tellerSessionService = tellerSessionService;
        this.cashEngineService = cashEngineService;
        this.vaultTransferService = vaultTransferService;
        this.tellerReportService = tellerReportService;
        this.tenantService = tenantService;
    }

    // ── Teller Session Open ─────────────────────────────────────────────────

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER')")
    public String openForm(Model model) {
        model.addAttribute("tellerOpenRequest", new TellerOpenRequest());
        return "teller/teller-open";
    }

    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER')")
    public String requestOpen(
            @Valid @ModelAttribute("tellerOpenRequest") TellerOpenRequest req,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "teller/teller-open";
        }
        try {
            TellerSession session = tellerSessionService.requestOpen(req);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Teller session open request submitted. sessionId=" + session.getId());
            return "redirect:/teller/inquiry";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "teller/teller-open";
        }
    }

    @PostMapping("/authorize-open")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHECKER', 'ADMIN')")
    public String authorizeOpen(
            @RequestParam("sessionId") Long sessionId, RedirectAttributes redirectAttributes) {
        try {
            TellerSession session = tellerSessionService.authorizeOpen(sessionId);
            redirectAttributes.addFlashAttribute(
                    "message", "Teller session authorized OPEN. sessionId=" + session.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teller/inquiry";
    }

    // ── Cash Deposit / Withdrawal ───────────────────────────────────────────

    @GetMapping("/deposit")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER')")
    public String depositForm(Model model) {
        model.addAttribute("tellerCashRequest", new TellerCashRequest());
        return "teller/teller-deposit";
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER')")
    public String deposit(
            @Valid @ModelAttribute("tellerCashRequest") TellerCashRequest req,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "teller/teller-deposit";
        }
        try {
            Transaction txn = cashEngineService.cashDeposit(req);
            redirectAttributes.addFlashAttribute("message", "Deposit posted. txnId=" + txn.getId());
            return "redirect:/transactions/" + txn.getId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "teller/teller-deposit";
        }
    }

    @GetMapping("/withdraw")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER')")
    public String withdrawForm(Model model) {
        model.addAttribute("tellerCashRequest", new TellerCashRequest());
        return "teller/teller-withdraw";
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER')")
    public String withdraw(
            @Valid @ModelAttribute("tellerCashRequest") TellerCashRequest req,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "teller/teller-withdraw";
        }
        try {
            Transaction txn = cashEngineService.cashWithdrawal(req);
            redirectAttributes.addFlashAttribute(
                    "message", "Withdrawal posted. txnId=" + txn.getId());
            return "redirect:/transactions/" + txn.getId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "teller/teller-withdraw";
        }
    }

    // ── Vault Transfer (dual custody) ───────────────────────────────────────

    @GetMapping("/vault-transfer")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER')")
    public String vaultTransferForm(Model model) {
        model.addAttribute("amount", "");
        model.addAttribute("remarks", "");
        model.addAttribute("direction", "TELLER_TO_VAULT");
        return "teller/teller-vault-transfer";
    }

    @PostMapping("/vault-transfer")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER')")
    public String initiateVaultTransfer(
            @RequestParam("direction") String direction,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "remarks", required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            VaultTransfer vt;
            if ("VAULT_TO_TELLER".equalsIgnoreCase(direction)) {
                vt = vaultTransferService.initiateVaultToTeller(amount, remarks);
            } else if ("TELLER_TO_VAULT".equalsIgnoreCase(direction)) {
                vt = vaultTransferService.initiateTellerToVault(amount, remarks);
            } else {
                throw new BusinessException("INVALID_REQUEST", "Invalid direction: " + direction);
            }
            redirectAttributes.addFlashAttribute(
                    "message", "Vault transfer initiated. transferId=" + vt.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teller/inquiry";
    }

    @PostMapping("/authorize-vault-transfer")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHECKER', 'ADMIN')")
    public String authorizeVaultTransfer(
            @RequestParam("transferId") Long transferId,
            @RequestParam(value = "remarks", required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            VaultTransfer vt = vaultTransferService.authorize(transferId, remarks);
            redirectAttributes.addFlashAttribute(
                    "message", "Vault transfer authorized. transferId=" + vt.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teller/inquiry";
    }

    @PostMapping("/reject-vault-transfer")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHECKER', 'ADMIN')")
    public String rejectVaultTransfer(
            @RequestParam("transferId") Long transferId,
            @RequestParam(value = "remarks", required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            VaultTransfer vt = vaultTransferService.reject(transferId, remarks);
            redirectAttributes.addFlashAttribute(
                    "message", "Vault transfer rejected. transferId=" + vt.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teller/inquiry";
    }

    // ── Teller Close ────────────────────────────────────────────────────────

    @GetMapping("/close")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER')")
    public String closeForm(Model model) {
        model.addAttribute("tellerCloseRequest", new TellerCloseRequest());
        return "teller/teller-close";
    }

    @PostMapping("/close")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER')")
    public String requestClose(
            @Valid @ModelAttribute("tellerCloseRequest") TellerCloseRequest req,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "teller/teller-close";
        }
        try {
            TellerSession session = tellerSessionService.requestClose(req);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Teller session close request submitted. sessionId=" + session.getId());
            return "redirect:/teller/inquiry";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "teller/teller-close";
        }
    }

    @PostMapping("/authorize-close")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHECKER', 'ADMIN')")
    public String authorizeClose(
            @RequestParam("sessionId") Long sessionId, RedirectAttributes redirectAttributes) {
        try {
            TellerSession session = tellerSessionService.authorizeClose(sessionId);
            redirectAttributes.addFlashAttribute(
                    "message", "Teller session CLOSED. sessionId=" + session.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teller/inquiry";
    }

    // ── Inquiry (minimal page for now; JSP will show summary + pending actions) ───────────────

    @GetMapping("/inquiry")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'MANAGER', 'CHECKER', 'AUDITOR')")
    public String inquiry(Model model) {
        // Phase 4 will populate this with session/vault position details.
        return "teller/teller-inquiry";
    }

    // ── Reports ─────────────────────────────────────────────────────────────

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String reportsIndex(Model model) {
        return "teller/teller-reports";
    }

    @GetMapping("/reports/cash-position")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR', 'TELLER')")
    public String cashPositionReport(
            @RequestParam(value = "date", required = false) String dateStr, Model model) {
        LocalDate bizDate = resolveReportDate(dateStr);
        model.addAttribute("reportDate", bizDate);
        model.addAttribute("rows", tellerReportService.getTellerCashPositionReport(bizDate));
        return "teller/report-cash-position";
    }

    @GetMapping("/reports/vault-position")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String vaultPositionReport(Model model) {
        model.addAttribute("rows", tellerReportService.getVaultPositionReport());
        return "teller/report-vault-position";
    }

    @GetMapping("/reports/daily-movement")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String dailyMovementReport(
            @RequestParam(value = "date", required = false) String dateStr, Model model) {
        LocalDate bizDate = resolveReportDate(dateStr);
        model.addAttribute("reportDate", bizDate);
        model.addAttribute("report", tellerReportService.getDailyCashMovementReport(bizDate));
        return "teller/report-daily-movement";
    }

    @GetMapping("/reports/denomination-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR', 'TELLER')")
    public String denominationSummary(@RequestParam("sessionId") Long sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("rows", tellerReportService.getDenominationSummary(sessionId));
        return "teller/report-denomination-summary";
    }

    @GetMapping("/reports/short-excess")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String shortExcessReport(
            @RequestParam(value = "date", required = false) String dateStr, Model model) {
        LocalDate bizDate = resolveReportDate(dateStr);
        model.addAttribute("reportDate", bizDate);
        model.addAttribute("rows", tellerReportService.getCashShortExcessReport(bizDate));
        return "teller/report-short-excess";
    }

    /** Resolve report date from request param or fall back to current business date. */
    private LocalDate resolveReportDate(String dateStr) {
        if (dateStr != null && !dateStr.isBlank()) {
            try {
                return LocalDate.parse(dateStr);
            } catch (Exception ignored) {
                // Fall through to business date
            }
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return tenantService.getCurrentBusinessDate(tenantId);
    }
}
