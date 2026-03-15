package com.ledgora.deposit.controller;

import com.ledgora.deposit.entity.DepositAccount;
import com.ledgora.deposit.enums.DepositAccountStatus;
import com.ledgora.deposit.repository.DepositAccountRepository;
import com.ledgora.deposit.service.DepositPrematureClosureService;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Finacle-grade Deposit Module UI Controller.
 *
 * <p>CASA + FD + RD portfolio management. All balances derive from LedgerEntry-based aggregates.
 * Interest accrual runs at EOD via voucher engine. Tenant-scoped, RBAC-enforced.
 */
@Controller
@RequestMapping("/deposit")
public class DepositDashboardController {

    private final DepositAccountRepository depositAccountRepository;
    private final DepositPrematureClosureService prematureClosureService;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TenantRepository tenantRepository;

    public DepositDashboardController(
            DepositAccountRepository depositAccountRepository,
            DepositPrematureClosureService prematureClosureService,
            LedgerEntryRepository ledgerEntryRepository,
            TenantRepository tenantRepository) {
        this.depositAccountRepository = depositAccountRepository;
        this.prematureClosureService = prematureClosureService;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK', 'OPERATIONS')")
    public String dashboard(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            model.addAttribute("error", "Tenant not found");
            model.addAttribute("totalPortfolio", BigDecimal.ZERO);
            model.addAttribute("casaBalance", BigDecimal.ZERO);
            model.addAttribute("fdBalance", BigDecimal.ZERO);
            model.addAttribute("rdBalance", BigDecimal.ZERO);
            model.addAttribute("interestPayable", BigDecimal.ZERO);
            model.addAttribute("maturingCount", 0);
            return "deposit/deposit-dashboard";
        }

        BigDecimal casaBalance = depositAccountRepository.sumCasaBalanceByTenantId(tenantId);
        BigDecimal fdBalance = depositAccountRepository.sumFdBalanceByTenantId(tenantId);
        BigDecimal rdBalance = depositAccountRepository.sumRdBalanceByTenantId(tenantId);
        BigDecimal interestPayable =
                depositAccountRepository.sumInterestPayableByTenantId(tenantId);

        // CBS: Use tenant business date, not system clock, for all date-based queries.
        // System clock may differ from tenant's business date (e.g., after weekend EOD).
        List<DepositAccount> maturingThisMonth =
                depositAccountRepository.findMaturingByDate(
                        tenantId, tenant.getCurrentBusinessDate().plusMonths(1));

        model.addAttribute("tenantName", tenant.getTenantName());
        model.addAttribute("casaBalance", casaBalance);
        model.addAttribute("fdBalance", fdBalance);
        model.addAttribute("rdBalance", rdBalance);
        model.addAttribute("interestPayable", interestPayable);
        model.addAttribute("totalPortfolio", casaBalance.add(fdBalance).add(rdBalance));
        model.addAttribute("maturingCount", maturingThisMonth.size());
        return "deposit/deposit-dashboard";
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK', 'OPERATIONS')")
    public String list(
            @RequestParam(required = false) String status, Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        List<DepositAccount> deposits;
        if (status != null && !status.isEmpty()) {
            deposits =
                    depositAccountRepository.findByTenantIdAndStatus(
                            tenantId, DepositAccountStatus.valueOf(status));
        } else {
            deposits = depositAccountRepository.findActiveAndMaturedByTenantId(tenantId);
        }
        model.addAttribute("deposits", deposits);
        model.addAttribute("selectedStatus", status);
        return "deposit/deposit-account-list";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK', 'OPERATIONS')")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        DepositAccount deposit = depositAccountRepository.findById(id).orElse(null);
        // Tenant isolation: verify deposit belongs to current tenant
        if (deposit == null
                || deposit.getTenant() == null
                || !deposit.getTenant().getId().equals(tenantId)) {
            model.addAttribute("error", "Deposit account not found");
            model.addAttribute("deposit", null);
            return "deposit/deposit-account-detail";
        }
        model.addAttribute("deposit", deposit);

        // ── CBS AUDIT TRAIL: Load immutable ledger entries for this deposit's linked account ──
        // Per CBS/RBI/Finacle Tier-1: the auditor must be able to trace any deposit balance
        // back to the immutable ledger entries (vouchers → ledger entries → trial balance).
        // Load entries by the linked account ID to show all deposit-related postings
        // (accrual, premature closure, interest posting).
        if (deposit.getLinkedAccount() != null) {
            List<LedgerEntry> ledgerEntries =
                    ledgerEntryRepository.findByAccountId(deposit.getLinkedAccount().getId());
            model.addAttribute("ledgerEntries", ledgerEntries);
        }

        return "deposit/deposit-account-detail";
    }

    @PostMapping("/{id}/premature-close")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String prematureClose(
            @PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        // Tenant isolation: verify deposit belongs to current tenant before closure
        DepositAccount deposit = depositAccountRepository.findById(id).orElse(null);
        if (deposit == null
                || deposit.getTenant() == null
                || !deposit.getTenant().getId().equals(tenantId)) {
            redirectAttributes.addFlashAttribute(
                    "error", "Deposit account not found or access denied");
            return "redirect:/deposit/list";
        }
        try {
            prematureClosureService.prematureClose(id);
            redirectAttributes.addFlashAttribute(
                    "message", "Deposit prematurely closed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "Premature closure failed: " + e.getMessage());
        }
        return "redirect:/deposit/" + id;
    }

    private Long resolveTenantId(HttpSession session) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            Object sessionTenantId = session.getAttribute("tenantId");
            if (sessionTenantId instanceof Number n) tenantId = n.longValue();
            else if (sessionTenantId instanceof String s && !s.isBlank())
                tenantId = Long.valueOf(s);
        }
        if (tenantId == null) throw new IllegalStateException("Tenant context not set");
        return tenantId;
    }
}
