package com.ledgora.voucher.controller;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.service.BatchService;
import com.ledgora.branch.entity.Branch;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.enums.VoucherDrCr;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.ledger.entity.LedgerEntry;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.repository.VoucherRepository;
import com.ledgora.voucher.service.VoucherService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller for the Voucher lifecycle UI screens.
 * Routes: /vouchers, /vouchers/create, /vouchers/pending, /vouchers/posted, /vouchers/cancelled
 *
 * Voucher lifecycle: Create (Maker) -> Authorize (Checker) -> Post -> (Cancel via reversal)
 * Vouchers created here remain in PENDING authorization state (authFlag=N)
 * until a checker authorizes them via the pending vouchers screen.
 */
@Controller
@RequestMapping("/vouchers")
public class VoucherController {

    private final VoucherService voucherService;
    private final VoucherRepository voucherRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;
    private final GeneralLedgerRepository glRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final BatchService batchService;

    public VoucherController(VoucherService voucherService,
                             VoucherRepository voucherRepository,
                             LedgerEntryRepository ledgerEntryRepository,
                             AccountRepository accountRepository,
                             GeneralLedgerRepository glRepository,
                             UserRepository userRepository,
                             TenantService tenantService,
                             BatchService batchService) {
        this.voucherService = voucherService;
        this.voucherRepository = voucherRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
        this.glRepository = glRepository;
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.batchService = batchService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'AUDITOR', 'ADMIN', 'MANAGER', 'TELLER', 'OPERATIONS')")
    public String voucherInquiry(Model model) {
        // Voucher inquiry - search/filter view
        return "voucher/voucher-inquiry";
    }

    /**
     * Voucher detail view: shows header, linked transaction, ledger entries,
     * batch reference, maker/checker, and derived status timeline.
     *
     * Accessible by MAKER (own vouchers), CHECKER, AUDITOR (read-only), ADMIN, MANAGER.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'AUDITOR', 'ADMIN', 'MANAGER', 'TELLER', 'OPERATIONS')")
    public String viewVoucher(@PathVariable Long id, Model model) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Voucher voucher = voucherService.getVoucher(id, tenantId);
        model.addAttribute("voucher", voucher);
        model.addAttribute("voucherStatus", voucher.getStatus());

        // Linked transaction
        if (voucher.getTransaction() != null) {
            model.addAttribute("transaction", voucher.getTransaction());
        }

        // Ledger entry linked to this voucher (if posted)
        if (voucher.getLedgerEntry() != null) {
            model.addAttribute("ledgerEntry", voucher.getLedgerEntry());
        }

        // All vouchers for the same transaction (the DR+CR pair)
        if (voucher.getTransaction() != null) {
            List<Voucher> relatedVouchers = voucherRepository.findByTransactionId(
                    voucher.getTransaction().getId());
            model.addAttribute("relatedVouchers", relatedVouchers);
        }

        // Maker/Checker usernames (eagerly resolve to avoid LazyInitializationException in JSP)
        String makerUsername = "";
        try { if (voucher.getMaker() != null) makerUsername = voucher.getMaker().getUsername(); } catch (Exception e) { /* lazy */ }
        model.addAttribute("makerUsername", makerUsername);

        String checkerUsername = "";
        try { if (voucher.getChecker() != null) checkerUsername = voucher.getChecker().getUsername(); } catch (Exception e) { /* lazy */ }
        model.addAttribute("checkerUsername", checkerUsername);

        // Batch info
        model.addAttribute("batchCode", voucher.getBatchCode());

        return "voucher/voucher-view";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN', 'MANAGER', 'TELLER')")
    public String createVoucherForm(Model model) {
        return "voucher/voucher-create";
    }

    /**
     * PART 4: Handle voucher creation form POST.
     * Creates a pair of vouchers (DR + CR) in PENDING authorization state.
     * Vouchers must be authorized by a checker before they can be posted.
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN', 'MANAGER', 'TELLER')")
    public String createVoucher(@RequestParam String debitAccountNumber,
                                @RequestParam String creditAccountNumber,
                                @RequestParam BigDecimal amount,
                                @RequestParam(defaultValue = "INR") String currency,
                                @RequestParam(defaultValue = "TRANSFER") String voucherType,
                                @RequestParam(required = false) String narration,
                                RedirectAttributes redirectAttributes) {
        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("error", "Amount must be greater than zero.");
                return "redirect:/vouchers/create";
            }
            if (debitAccountNumber == null || debitAccountNumber.isBlank()
                    || creditAccountNumber == null || creditAccountNumber.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Both debit and credit accounts are required.");
                return "redirect:/vouchers/create";
            }
            if (debitAccountNumber.equals(creditAccountNumber)) {
                redirectAttributes.addFlashAttribute("error", "Debit and credit accounts cannot be the same.");
                return "redirect:/vouchers/create";
            }

            // Resolve tenant context
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            tenantService.validateBusinessDayOpen(tenantId);
            Tenant tenant = tenantService.getTenantById(tenantId);
            LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);

            // Resolve accounts with tenant isolation
            Account debitAccount = accountRepository.findByAccountNumberAndTenantId(debitAccountNumber, tenantId)
                    .orElseThrow(() -> new RuntimeException("Debit account not found: " + debitAccountNumber));
            Account creditAccount = accountRepository.findByAccountNumberAndTenantId(creditAccountNumber, tenantId)
                    .orElseThrow(() -> new RuntimeException("Credit account not found: " + creditAccountNumber));

            // Resolve GL accounts from account GL codes
            GeneralLedger debitGl = resolveGl(debitAccount);
            GeneralLedger creditGl = resolveGl(creditAccount);

            // Resolve maker (current user)
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User maker = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            // Get or create batch
            TransactionBatch batch = batchService.getOrCreateOpenBatch(
                    tenantId, TransactionChannel.TELLER, businessDate);
            String batchCode = batch.getBatchCode() != null ? batch.getBatchCode() : "BATCH-" + batch.getId();

            // Create DR voucher (debit leg)
            Branch debitBranch = resolveBranch(debitAccount, maker);
            Voucher drVoucher = voucherService.createVoucher(
                    tenant, debitBranch, debitAccount, debitGl, VoucherDrCr.DR,
                    amount, amount, currency, businessDate, businessDate,
                    batchCode, 1, maker,
                    narration != null ? narration : "Voucher DR: " + debitAccountNumber);

            // Create CR voucher (credit leg)
            Branch creditBranch = resolveBranch(creditAccount, maker);
            Voucher crVoucher = voucherService.createVoucher(
                    tenant, creditBranch, creditAccount, creditGl, VoucherDrCr.CR,
                    amount, amount, currency, businessDate, businessDate,
                    batchCode, 1, maker,
                    narration != null ? narration : "Voucher CR: " + creditAccountNumber);

            redirectAttributes.addFlashAttribute("message",
                    "Voucher pair created (PENDING authorization). DR scroll #" + drVoucher.getScrollNo()
                    + ", CR scroll #" + crVoucher.getScrollNo()
                    + " for " + currency + " " + amount
                    + " from " + debitAccountNumber + " to " + creditAccountNumber + ".");
            return "redirect:/vouchers/pending";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Voucher creation failed: " + e.getMessage());
            return "redirect:/vouchers/create";
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String pendingVouchers(Model model) {
        // Show vouchers with PENDING status awaiting authorization
        return "voucher/voucher-pending";
    }

    /**
     * Authorize a pending voucher (checker action).
     * Checker must not be the same as maker (enforced by VoucherService).
     */
    @PostMapping("/{id}/authorize")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String authorizeVoucher(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User checker = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            Voucher authorized = voucherService.authorizeVoucher(id, checker);
            redirectAttributes.addFlashAttribute("message",
                    "Voucher " + authorized.getVoucherNumber() + " authorized by " + username);
            return "redirect:/vouchers/pending";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Authorization failed: " + e.getMessage());
            return "redirect:/vouchers/pending";
        }
    }

    /**
     * Reject (cancel) a pending voucher (checker action).
     * Creates a reversal voucher per CBS cancel pattern.
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String rejectVoucher(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "Rejected by checker") String reason,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User checker = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            Voucher reversal = voucherService.cancelVoucher(id, checker, reason);
            redirectAttributes.addFlashAttribute("message",
                    "Voucher " + id + " rejected. Reversal voucher: " + reversal.getVoucherNumber());
            return "redirect:/vouchers/pending";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Rejection failed: " + e.getMessage());
            return "redirect:/vouchers/pending";
        }
    }

    @GetMapping("/posted")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'AUDITOR', 'ADMIN', 'MANAGER', 'TELLER', 'OPERATIONS')")
    public String postedVouchers(Model model) {
        // Show vouchers with POSTED status
        return "voucher/voucher-posted";
    }

    @GetMapping("/cancelled")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER', 'AUDITOR', 'ADMIN', 'MANAGER', 'TELLER', 'OPERATIONS')")
    public String cancelledVouchers(Model model) {
        // Show cancelled/reversed vouchers
        return "voucher/voucher-cancelled";
    }

    private GeneralLedger resolveGl(Account account) {
        if (account.getGlAccountCode() == null || account.getGlAccountCode().isBlank()) {
            return null;
        }
        return glRepository.findByGlCode(account.getGlAccountCode()).orElse(null);
    }

    private Branch resolveBranch(Account account, User currentUser) {
        if (account.getBranch() != null) {
            return account.getBranch();
        }
        if (account.getHomeBranch() != null) {
            return account.getHomeBranch();
        }
        if (currentUser != null && currentUser.getBranch() != null) {
            return currentUser.getBranch();
        }
        throw new RuntimeException("No branch mapping found for account " + account.getAccountNumber());
    }
}
