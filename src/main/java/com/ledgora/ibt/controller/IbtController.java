package com.ledgora.ibt.controller;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.clearing.entity.InterBranchTransfer;
import com.ledgora.clearing.repository.InterBranchTransferRepository;
import com.ledgora.clearing.service.IbtService;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.InterBranchTransferStatus;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.entity.Transaction;
import com.ledgora.transaction.service.TransactionService;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.repository.VoucherRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Inter-Branch Transfer (IBT) UI screens.
 *
 * <p>This controller provides a dedicated UI for cross-branch transfers. It delegates entirely to
 * the existing TransactionService.transfer() which auto-detects cross-branch transfers and routes
 * them through the 4-voucher IBC clearing flow.
 *
 * <p>No new business logic — pure UI wiring over existing services.
 */
@Controller
@RequestMapping("/ibt")
public class IbtController {

    private final TransactionService transactionService;
    private final TenantService tenantService;
    private final AccountRepository accountRepository;
    private final InterBranchTransferRepository ibtRepository;
    private final VoucherRepository voucherRepository;
    private final IbtService ibtService;
    private final BranchRepository branchRepository;

    public IbtController(
            TransactionService transactionService,
            TenantService tenantService,
            AccountRepository accountRepository,
            InterBranchTransferRepository ibtRepository,
            VoucherRepository voucherRepository,
            IbtService ibtService,
            BranchRepository branchRepository) {
        this.transactionService = transactionService;
        this.tenantService = tenantService;
        this.accountRepository = accountRepository;
        this.ibtRepository = ibtRepository;
        this.voucherRepository = voucherRepository;
        this.ibtService = ibtService;
        this.branchRepository = branchRepository;
    }

    // ===== IBT Create Form =====

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN', 'MANAGER', 'TELLER')")
    public String createForm(Model model) {
        return "ibt/ibt-create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN', 'MANAGER', 'TELLER')")
    public String createIbt(
            @RequestParam String sourceAccountNumber,
            @RequestParam String destinationAccountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String narration,
            RedirectAttributes redirectAttributes) {
        try {
            // Basic validation
            if (sourceAccountNumber == null || sourceAccountNumber.isBlank()
                    || destinationAccountNumber == null || destinationAccountNumber.isBlank()) {
                redirectAttributes.addFlashAttribute(
                        "error", "Both source and destination accounts are required.");
                return "redirect:/ibt/create";
            }
            if (sourceAccountNumber.equals(destinationAccountNumber)) {
                redirectAttributes.addFlashAttribute(
                        "error", "Source and destination accounts cannot be the same.");
                return "redirect:/ibt/create";
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute(
                        "error", "Amount must be greater than zero.");
                return "redirect:/ibt/create";
            }
            if (amount.scale() > 2) {
                redirectAttributes.addFlashAttribute(
                        "error", "Amount cannot have more than 2 decimal places.");
                return "redirect:/ibt/create";
            }

            // Validate account number format
            if (!sourceAccountNumber.matches("^[A-Za-z0-9\\-]+$")
                    || !destinationAccountNumber.matches("^[A-Za-z0-9\\-]+$")) {
                redirectAttributes.addFlashAttribute("error", "Invalid account number format.");
                return "redirect:/ibt/create";
            }

            // Pre-check: accounts must be at different branches
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            Account sourceAccount =
                    accountRepository
                            .findByAccountNumberAndTenantId(sourceAccountNumber, tenantId)
                            .orElse(null);
            Account destAccount =
                    accountRepository
                            .findByAccountNumberAndTenantId(destinationAccountNumber, tenantId)
                            .orElse(null);

            if (sourceAccount == null || destAccount == null) {
                redirectAttributes.addFlashAttribute("error", "One or both accounts not found.");
                return "redirect:/ibt/create";
            }

            if (sourceAccount.getBranch() != null && destAccount.getBranch() != null
                    && sourceAccount.getBranch().getId().equals(destAccount.getBranch().getId())) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Both accounts are at the same branch ("
                                + sourceAccount.getBranch().getBranchCode()
                                + "). Use the standard Transfer screen for same-branch transfers.");
                return "redirect:/ibt/create";
            }

            // Delegate to existing transfer flow — TransactionService auto-detects
            // cross-branch and routes through 4-voucher IBC clearing
            TransactionDTO dto =
                    TransactionDTO.builder()
                            .transactionType("TRANSFER")
                            .sourceAccountNumber(sourceAccountNumber)
                            .destinationAccountNumber(destinationAccountNumber)
                            .amount(amount)
                            .currency("INR")
                            .channel(TransactionChannel.TELLER.name())
                            .description(
                                    "Inter-Branch Transfer: "
                                            + sourceAccountNumber + " → " + destinationAccountNumber)
                            .narration(narration != null ? narration : "Inter-Branch Transfer")
                            .build();

            Transaction txn = transactionService.transfer(dto);

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Inter-Branch Transfer initiated. Ref: " + txn.getTransactionRef());
            return "redirect:/ibt/" + txn.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "IBT creation failed: " + e.getMessage());
            return "redirect:/ibt/create";
        }
    }

    // ===== IBT List (canonical: InterBranchTransfer aggregate, paginated) =====

    /**
     * List inter-branch transfers from the InterBranchTransfer table (authoritative lifecycle
     * entity). Does NOT derive from Transaction table or infer cross-branch via branch comparison.
     *
     * <p>Supports optional filters: status, businessDate, fromBranchId, toBranchId. Paginated with
     * default sort by createdAt descending.
     */
    @GetMapping
    @PreAuthorize(
            "hasAnyRole('MAKER', 'CHECKER', 'OPERATIONS', 'ADMIN', 'MANAGER', 'AUDITOR')")
    @Transactional(readOnly = true)
    public String listIbt(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate businessDate,
            @RequestParam(required = false) Long fromBranchId,
            @RequestParam(required = false) Long toBranchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Parse status filter (ignore invalid values gracefully)
        InterBranchTransferStatus filterStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                filterStatus = InterBranchTransferStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {
                // Invalid status param — treat as no filter
            }
        }

        // Select the appropriate repository query based on which filters are active.
        // InterBranchTransfer is the canonical aggregate — no Transaction derivation.
        // Build a JPA specification so filters can be combined without adding many repository
        // methods.
        org.springframework.data.jpa.domain.Specification<InterBranchTransfer> spec =
                (root, query, cb) -> cb.equal(root.get("tenant").get("id"), tenantId);

        if (filterStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filterStatus));
        }
        if (businessDate != null) {
            spec =
                    spec.and(
                            (root, query, cb) -> cb.equal(root.get("businessDate"), businessDate));
        }
        if (fromBranchId != null) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.equal(root.get("fromBranch").get("id"), fromBranchId));
        }
        if (toBranchId != null) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.equal(root.get("toBranch").get("id"), toBranchId));
        }

        Page<InterBranchTransfer> transferPage = ibtRepository.findAll(spec, pageable);

        // Model attributes for the view
        model.addAttribute("transferPage", transferPage);
        model.addAttribute("transfers", transferPage.getContent());
        model.addAttribute("currentPage", transferPage.getNumber());
        model.addAttribute("totalPages", transferPage.getTotalPages());
        model.addAttribute("totalElements", transferPage.getTotalElements());
        model.addAttribute("pageSize", size);

        // Filter state for form re-population
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedBusinessDate", businessDate);
        model.addAttribute("selectedFromBranchId", fromBranchId);
        model.addAttribute("selectedToBranchId", toBranchId);

        // Reference data for dropdowns
        model.addAttribute("statuses", InterBranchTransferStatus.values());
        model.addAttribute("branches", branchRepository.findAll());

        return "ibt/ibt-list";
    }

    // ===== IBT Detail (aggregate root: InterBranchTransfer) =====

    /**
     * View IBT detail using InterBranchTransfer as the aggregate root. Accepts either an IBT ID
     * (direct navigation from list) or a Transaction ID (redirect from POST /ibt/create). Uses
     * JOIN FETCH queries to eliminate N+1:
     *
     * <ul>
     *   <li>Query 1: findByIdWithGraph — IBT + branches + transaction + users (1 SELECT)
     *   <li>Query 2: findByTransactionIdWithGraph — vouchers + branch + account + ledgerEntry (1
     *       SELECT)
     * </ul>
     *
     * Total: 2 SQL SELECTs. Zero lazy loading in JSP.
     */
    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('MAKER', 'CHECKER', 'OPERATIONS', 'ADMIN', 'MANAGER', 'AUDITOR')")
    @Transactional(readOnly = true)
    public String viewIbt(@PathVariable Long id, Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        // Query 1: Try to load as IBT ID first (direct navigation from list screen)
        InterBranchTransfer ibt = ibtRepository.findByIdWithGraph(id).orElse(null);

        // If not found by IBT ID, try as Transaction ID (redirect from POST /ibt/create)
        if (ibt == null) {
            ibt =
                    ibtRepository
                            .findByReferenceTransactionIdAndTenantId(id, tenantId)
                            .flatMap(found -> ibtRepository.findByIdWithGraph(found.getId()))
                            .orElse(null);
        }

        if (ibt == null) {
            model.addAttribute(
                    "error", "Inter-Branch Transfer not found for ID: " + id);
            return "ibt/ibt-detail";
        }

        // Tenant isolation check
        if (!ibt.getTenant().getId().equals(tenantId)) {
            model.addAttribute(
                    "error", "Access denied: IBT does not belong to current tenant.");
            return "ibt/ibt-detail";
        }

        model.addAttribute("ibt", ibt);

        // Query 2: Fetch vouchers with all associations (N+1 prevention)
        List<Voucher> vouchers = List.of();
        Transaction transaction = ibt.getReferenceTransaction();
        if (transaction != null) {
            vouchers = voucherRepository.findByTransactionIdWithGraph(transaction.getId());
            model.addAttribute("transaction", transaction);
        }

        model.addAttribute("vouchers", vouchers);
        model.addAttribute("voucherCount", vouchers.size());

        // Separate vouchers by branch for grouped display
        String fromBranchCode =
                ibt.getFromBranch() != null ? ibt.getFromBranch().getBranchCode() : "";
        String toBranchCode =
                ibt.getToBranch() != null ? ibt.getToBranch().getBranchCode() : "";
        List<Voucher> branchAVouchers =
                vouchers.stream()
                        .filter(
                                v ->
                                        v.getBranch() != null
                                                && fromBranchCode.equals(
                                                        v.getBranch().getBranchCode()))
                        .toList();
        List<Voucher> branchBVouchers =
                vouchers.stream()
                        .filter(
                                v ->
                                        v.getBranch() != null
                                                && toBranchCode.equals(
                                                        v.getBranch().getBranchCode()))
                        .toList();
        model.addAttribute("branchAVouchers", branchAVouchers);
        model.addAttribute("branchBVouchers", branchBVouchers);

        // Clearing GL balances (lightweight query — no lazy loading concern)
        List<Account> clearingAccounts = ibtService.getIbcClearingAccounts(tenantId);
        model.addAttribute("clearingAccounts", clearingAccounts);

        return "ibt/ibt-detail";
    }

    // ===== IBT Reconciliation Dashboard =====

    /**
     * CBS-grade operational dashboard for inter-branch clearing reconciliation. Read-only — does
     * NOT mutate any records. Uses CLEARING_ACCOUNT type balances (not voucher derivation).
     *
     * <p>KPIs: unsettled count, failed count, clearing GL net, oldest unsettled for aging.
     */
    @GetMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('OPERATIONS', 'ADMIN', 'MANAGER', 'AUDITOR')")
    @Transactional(readOnly = true)
    public String reconciliation(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        // KPI 1: Total unsettled transfers (INITIATED + SENT + RECEIVED)
        Set<InterBranchTransferStatus> unsettledStatuses =
                Set.of(
                        InterBranchTransferStatus.INITIATED,
                        InterBranchTransferStatus.SENT,
                        InterBranchTransferStatus.RECEIVED);
        long totalUnsettled = ibtRepository.countByTenantIdAndStatusIn(tenantId, unsettledStatuses);

        // KPI 2: Failed transfers
        long failedCount =
                ibtRepository.countByTenantIdAndStatusIn(
                        tenantId, Set.of(InterBranchTransferStatus.FAILED));

        // KPI 3: Clearing GL net balance (from CLEARING_ACCOUNT type — NOT voucher derivation)
        BigDecimal clearingNet =
                accountRepository.sumBalanceByTenantIdAndAccountType(
                        tenantId, AccountType.CLEARING_ACCOUNT);
        boolean clearingBalanced = clearingNet.compareTo(BigDecimal.ZERO) == 0;

        // Aging: oldest unsettled IBTs (eagerly fetched with branches)
        List<InterBranchTransfer> allUnsettled =
                ibtRepository.findOldestUnsettledByTenantId(tenantId);
        // Limit to top 5 for display (query is ordered ASC by createdAt)
        List<InterBranchTransfer> oldestUnsettled =
                allUnsettled.size() > 5 ? allUnsettled.subList(0, 5) : allUnsettled;

        // Per-branch clearing account balances for drill-down
        List<Account> clearingAccounts = ibtService.getIbcClearingAccounts(tenantId);

        // Business date for aging calculation
        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);

        model.addAttribute("totalUnsettled", totalUnsettled);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("clearingNet", clearingNet);
        model.addAttribute("clearingBalanced", clearingBalanced);
        model.addAttribute("oldestUnsettled", oldestUnsettled);
        model.addAttribute("clearingAccounts", clearingAccounts);
        model.addAttribute("businessDate", businessDate);

        return "ibt/ibt-reconciliation";
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
            throw new IllegalStateException("Tenant context is not set for IBT operation");
        }
        return tenantId;
    }
}
