package com.ledgora.clearing.controller;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.clearing.entity.InterBranchTransfer;
import com.ledgora.clearing.repository.InterBranchTransferRepository;
import com.ledgora.clearing.service.IbtService;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.InterBranchTransferStatus;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Clearing Settlement Engine UI — operational interface for clearing reconciliation and settlement
 * monitoring prior to EOD.
 *
 * <p>Read-only. Does NOT execute settlement. Does NOT mutate any records.
 *
 * <p>CBS Control: All inter-branch clearing must be settled and clearing GL must net to zero before
 * the EOD DATE_ADVANCED phase.
 *
 * <p>Performance: Max 3 SELECTs. No N+1. No lazy loading in JSP.
 *
 * <ul>
 *   <li>Query 1: countByTenantIdAndStatusIn (unsettled + failed — 2 COUNTs)
 *   <li>Query 2: sumBalanceByTenantIdAndAccountType (clearing GL net)
 *   <li>Query 3: findOldestUnsettledByTenantId (JOIN FETCH for table)
 * </ul>
 */
@Controller
@RequestMapping("/clearing")
public class ClearingEngineController {

    private final InterBranchTransferRepository ibtRepository;
    private final AccountRepository accountRepository;
    private final IbtService ibtService;
    private final TenantService tenantService;

    public ClearingEngineController(
            InterBranchTransferRepository ibtRepository,
            AccountRepository accountRepository,
            IbtService ibtService,
            TenantService tenantService) {
        this.ibtRepository = ibtRepository;
        this.accountRepository = accountRepository;
        this.ibtService = ibtService;
        this.tenantService = tenantService;
    }

    @GetMapping("/engine")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    public String engine(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        // Query 1a: Unsettled count (INITIATED + SENT + RECEIVED)
        Set<InterBranchTransferStatus> unsettledStatuses =
                Set.of(
                        InterBranchTransferStatus.INITIATED,
                        InterBranchTransferStatus.SENT,
                        InterBranchTransferStatus.RECEIVED);
        long unsettledCount = ibtRepository.countByTenantIdAndStatusIn(tenantId, unsettledStatuses);

        // Query 1b: Failed count
        long failedCount =
                ibtRepository.countByTenantIdAndStatusIn(
                        tenantId, Set.of(InterBranchTransferStatus.FAILED));

        // Query 2: Clearing GL net balance (CLEARING_ACCOUNT type — NOT voucher derivation)
        BigDecimal clearingNetBalance =
                accountRepository.sumBalanceByTenantIdAndAccountType(
                        tenantId, AccountType.CLEARING_ACCOUNT);
        boolean clearingBalanced = clearingNetBalance.compareTo(BigDecimal.ZERO) == 0;

        // Settlement readiness: all clearing settled AND GL balanced
        boolean settlementReady = unsettledCount == 0 && clearingBalanced;

        // Query 3: Unsettled transfers with branches eagerly fetched (N+1 prevention)
        List<InterBranchTransfer> allUnsettled =
                ibtRepository.findOldestUnsettledByTenantId(tenantId);
        List<InterBranchTransfer> unsettledTransfers =
                allUnsettled.size() > 20 ? allUnsettled.subList(0, 20) : allUnsettled;

        // Per-branch clearing account balances for drill-down
        List<Account> clearingAccounts = ibtService.getIbcClearingAccounts(tenantId);

        // Business date for aging calculation
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);

        model.addAttribute("unsettledCount", unsettledCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("clearingNetBalance", clearingNetBalance);
        model.addAttribute("clearingBalanced", clearingBalanced);
        model.addAttribute("settlementReady", settlementReady);
        model.addAttribute("unsettledTransfers", unsettledTransfers);
        model.addAttribute("clearingAccounts", clearingAccounts);
        model.addAttribute("businessDate", businessDate);

        return "clearing/clearing-engine";
    }

    /** Resolve tenant ID from TenantContextHolder or session. */
    private Long resolveTenantId(HttpSession session) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            Object sessionTenantId = session.getAttribute("tenantId");
            if (sessionTenantId instanceof Number n) {
                tenantId = n.longValue();
            } else if (sessionTenantId instanceof String s && !s.isBlank()) {
                tenantId = Long.valueOf(s);
            }
        }
        if (tenantId == null) {
            throw new IllegalStateException(
                    "Tenant context is not set for clearing engine operation");
        }
        return tenantId;
    }
}
