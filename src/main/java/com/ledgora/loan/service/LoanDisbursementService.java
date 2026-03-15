package com.ledgora.loan.service;

import com.ledgora.account.entity.Account;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.loan.dto.LoanSchedulePreviewDTO;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanDisbursement;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.entity.LoanSchedule;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanDisbursementRepository;
import com.ledgora.loan.repository.LoanScheduleRepository;
import com.ledgora.loan.validation.EmiCalculator;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loan Disbursement Service — creates loan account and generates amortization schedule.
 *
 * <p>RBI Master Directions on Lending:
 *
 * <ul>
 *   <li>Disbursement creates a Loan Asset (DR Loan Asset GL, CR Customer Account)
 *   <li>All postings via voucher engine — no direct balance mutation
 *   <li>Amortization schedule generated using reducing balance EMI formula
 *   <li>Loan status set to ACTIVE with NPA classification STANDARD
 * </ul>
 *
 * <p>Accounting entry:
 *
 * <pre>
 *   DR Loan Asset GL (Asset increases)
 *   CR Customer Account (Customer receives funds)
 * </pre>
 */
@Service
public class LoanDisbursementService {

    private static final Logger log = LoggerFactory.getLogger(LoanDisbursementService.class);

    private final LoanAccountRepository loanAccountRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final LoanDisbursementRepository loanDisbursementRepository;
    private final AccountRepository accountRepository;
    private final VoucherService voucherService;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final GeneralLedgerRepository glRepository;
    private final TenantService tenantService;
    private final AuditService auditService;

    public LoanDisbursementService(
            LoanAccountRepository loanAccountRepository,
            LoanScheduleRepository loanScheduleRepository,
            LoanDisbursementRepository loanDisbursementRepository,
            AccountRepository accountRepository,
            VoucherService voucherService,
            BranchRepository branchRepository,
            UserRepository userRepository,
            GeneralLedgerRepository glRepository,
            TenantService tenantService,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.loanDisbursementRepository = loanDisbursementRepository;
        this.accountRepository = accountRepository;
        this.voucherService = voucherService;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.glRepository = glRepository;
        this.tenantService = tenantService;
        this.auditService = auditService;
    }

    /**
     * Disburse a loan — creates LoanAccount, generates amortization schedule, and posts
     * disbursement voucher pair via the voucher engine.
     *
     * <p>Accounting entry (CBS-grade, voucher-driven):
     *
     * <pre>
     *   DR Loan Asset GL        (product.glLoanAsset — asset increases)
     *   CR Customer Account     (customerAccount — funds credited to borrower)
     * </pre>
     *
     * <p>Voucher lifecycle: create → system-authorize → post → LedgerEntry (immutable).
     *
     * @return the created LoanAccount with generated schedule
     */
    @Transactional
    public LoanAccount disburse(
            Tenant tenant,
            LoanProduct product,
            Account customerAccount,
            String loanAccountNumber,
            BigDecimal principalAmount) {

        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_LOAN_AMOUNT", "Loan principal must be positive");
        }

        // CBS Tier-1: validate business day is OPEN before financial operations
        tenantService.validateBusinessDayOpen(tenant.getId());

        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenant.getId());
        LocalDate maturityDate = businessDate.plusMonths(product.getTenureMonths());

        // Compute EMI via centralized calculator (eliminates 3x duplication)
        BigDecimal emiAmount =
                EmiCalculator.computeEmi(
                        principalAmount, product.getInterestRate(), product.getTenureMonths());

        // Denormalize borrower name from linked account for quick search/display
        String borrowerName =
                customerAccount.getCustomerName() != null
                        ? customerAccount.getCustomerName()
                        : customerAccount.getAccountName();

        LoanAccount loan =
                LoanAccount.builder()
                        .tenant(tenant)
                        .loanProduct(product)
                        .linkedAccount(customerAccount)
                        .loanAccountNumber(loanAccountNumber)
                        .principalAmount(principalAmount)
                        .outstandingPrincipal(principalAmount)
                        .accruedInterest(BigDecimal.ZERO)
                        .emiAmount(emiAmount)
                        .interestRate(product.getInterestRate())
                        .currency(
                                customerAccount.getCurrency() != null
                                        ? customerAccount.getCurrency()
                                        : "INR")
                        .borrowerName(borrowerName)
                        .dpd(0)
                        .status(LoanStatus.ACTIVE)
                        .npaClassification(NpaClassification.STANDARD)
                        .provisionAmount(BigDecimal.ZERO)
                        .disbursementDate(businessDate)
                        .maturityDate(maturityDate)
                        .build();
        loan = loanAccountRepository.save(loan);

        // Generate and persist amortization schedule (Finacle LACSMNT — schedule at disbursement)
        List<LoanSchedule> schedule = generateAmortizationSchedule(loan, product, businessDate);
        loanScheduleRepository.saveAll(schedule);

        // ══════════════════════════════════════════════════════════════════════
        // CBS/RBI/FINACLE TIER-1 ACCOUNTING PRINCIPLE:
        //   Voucher → LedgerEntry (immutable) = SOURCE OF TRUTH
        //   LoanAccount.outstandingPrincipal = DERIVED CACHE
        //
        // For disbursement, the LoanAccount entity is created with initial state
        // (outstandingPrincipal = principalAmount) as a cache. The voucher posting
        // below creates the immutable LedgerEntry which is the primary accounting
        // record. If voucher posting fails, @Transactional rolls back both the
        // entity creation and the voucher — maintaining consistency.
        //
        // The true loan outstanding is always derivable from:
        //   SUM(DEBIT on Loan Asset GL) - SUM(CREDIT on Loan Asset GL)
        // ══════════════════════════════════════════════════════════════════════
        postDisbursementVouchers(
                tenant, product, customerAccount, loan, principalAmount, businessDate);

        // ── CBS AUDIT: Record immutable LoanDisbursement tranche ──
        loanDisbursementRepository.save(
                LoanDisbursement.builder()
                        .tenant(tenant)
                        .loanAccount(loan)
                        .trancheNumber(1)
                        .disbursementAmount(principalAmount)
                        .disbursementDate(businessDate)
                        .status("DISBURSED")
                        .disbursedBy(com.ledgora.tenant.context.TenantContextHolder.getUsername())
                        .remarks("Initial disbursement: " + loanAccountNumber)
                        .build());

        auditService.logEvent(
                null,
                "LOAN_DISBURSED",
                "LOAN_ACCOUNT",
                loan.getId(),
                "Loan "
                        + loanAccountNumber
                        + " disbursed: principal="
                        + principalAmount
                        + " product="
                        + product.getProductCode()
                        + " tenure="
                        + product.getTenureMonths()
                        + "m rate="
                        + product.getInterestRate()
                        + "%",
                null);

        log.info(
                "Loan disbursed: {} principal={} rate={}% tenure={}m schedule={}",
                loanAccountNumber,
                principalAmount,
                product.getInterestRate(),
                product.getTenureMonths(),
                schedule.size());

        return loan;
    }

    /**
     * Post disbursement voucher pair via the voucher engine.
     *
     * <p>CBS-grade double-entry: DR Loan Asset GL, CR Customer Account GL. Vouchers are
     * system-auto-authorized and posted atomically.
     *
     * <p>Accounting entry:
     *
     * <pre>
     *   DR Loan Asset GL        (bank's asset increases — money lent out)
     *   CR Customer Account GL  (customer's deposit liability — funds credited)
     * </pre>
     */
    private void postDisbursementVouchers(
            Tenant tenant,
            LoanProduct product,
            Account customerAccount,
            LoanAccount loan,
            BigDecimal amount,
            LocalDate businessDate) {
        try {
            // Resolve branch from customer account (or default to first tenant branch)
            Branch branch = resolveBranch(customerAccount, tenant);

            // Resolve system maker user for automated voucher creation
            User systemUser =
                    userRepository
                            .findByUsername("SYSTEM_AUTO")
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    "SYSTEM_USER_MISSING",
                                                    "SYSTEM_AUTO user not configured. Loan voucher posting blocked."));

            // CBS/Finacle: DR leg targets the internal loan asset account (not customer account).
            // CR leg targets the customer account. This ensures:
            // - Loan asset account balance INCREASES (debit on asset = increase)
            // - Customer account balance INCREASES (credit on liability = increase)
            // Using the same account for both legs would net to zero balance change.
            Account loanAssetAccount =
                    resolveLoanAssetAccount(tenant, product.getGlLoanAsset(), branch);
            GeneralLedger customerGl = resolveCustomerAccountGl(customerAccount);

            String batchCode = "LOAN-DISB-" + loan.getLoanAccountNumber();

            // Create DR/CR voucher pair atomically
            // DR leg: Internal Loan Asset Account (with Loan Asset GL)
            // CR leg: Customer Account (with Customer's deposit GL)
            Voucher[] pair =
                    voucherService.createVoucherPair(
                            tenant,
                            branch,
                            loanAssetAccount,
                            product.getGlLoanAsset(), // DR leg — Loan Asset GL
                            branch,
                            customerAccount,
                            customerGl, // CR leg — Customer's deposit GL
                            amount,
                            loan.getCurrency(),
                            businessDate,
                            batchCode,
                            systemUser,
                            "Loan disbursement DR: "
                                    + loan.getLoanAccountNumber()
                                    + " principal="
                                    + amount,
                            "Loan disbursement CR: "
                                    + loan.getLoanAccountNumber()
                                    + " credit to "
                                    + customerAccount.getAccountNumber());

            // System-authorize both vouchers
            voucherService.systemAuthorizeVoucher(pair[0].getId(), systemUser);
            voucherService.systemAuthorizeVoucher(pair[1].getId(), systemUser);

            // Post both vouchers (creates immutable LedgerEntry records)
            voucherService.postVoucher(pair[0].getId());
            voucherService.postVoucher(pair[1].getId());

            log.info(
                    "Disbursement vouchers posted: DR={} CR={} for loan {}",
                    pair[0].getVoucherNumber(),
                    pair[1].getVoucherNumber(),
                    loan.getLoanAccountNumber());

        } catch (BusinessException e) {
            throw e; // re-throw business exceptions
        } catch (Exception e) {
            log.error(
                    "Disbursement voucher posting failed for loan {}: {}",
                    loan.getLoanAccountNumber(),
                    e.getMessage(),
                    e);
            // Voucher posting failure should not silently succeed — fail the disbursement
            throw new BusinessException(
                    "VOUCHER_POSTING_FAILED",
                    "Disbursement voucher posting failed for loan "
                            + loan.getLoanAccountNumber()
                            + ": "
                            + e.getMessage());
        }
    }

    /** Resolve branch for voucher posting — uses account's branch or first tenant branch. */
    private Branch resolveBranch(Account account, Tenant tenant) {
        if (account.getBranch() != null) {
            return account.getBranch();
        }
        List<Branch> branches = branchRepository.findByTenantId(tenant.getId());
        if (branches.isEmpty()) {
            throw new BusinessException(
                    "NO_BRANCH", "No branch configured for tenant " + tenant.getTenantCode());
        }
        return branches.get(0);
    }

    /**
     * Resolve or create an internal loan asset account for the DR leg of loan vouchers.
     *
     * <p>CBS/Finacle pattern: Each GL-level operation needs a dedicated internal account (like the
     * Cash GL account used by TransactionService). The loan asset account is an INTERNAL_ACCOUNT
     * mapped to the loan product's glLoanAsset GL code. If it doesn't exist, it's auto-created.
     *
     * <p>This ensures the VoucherService updates the correct account balance for each leg:
     *
     * <ul>
     *   <li>DR leg → internal loan asset account balance increases (asset grows)
     *   <li>CR leg → customer account balance increases (receives funds)
     * </ul>
     */
    private Account resolveLoanAssetAccount(Tenant tenant, GeneralLedger loanAssetGl, Branch branch) {
        String glCode = loanAssetGl.getGlCode();
        return accountRepository
                .findFirstByTenantIdAndGlAccountCode(tenant.getId(), glCode)
                .orElseGet(
                        () -> {
                            // Auto-create internal loan asset account for this tenant
                            Account internalAccount =
                                    Account.builder()
                                            .tenant(tenant)
                                            .accountNumber("INT-LOAN-" + tenant.getTenantCode())
                                            .accountName("Loan Asset Internal Account")
                                            .accountType(AccountType.INTERNAL_ACCOUNT)
                                            .status(AccountStatus.ACTIVE)
                                            .approvalStatus(MakerCheckerStatus.APPROVED)
                                            .balance(java.math.BigDecimal.ZERO)
                                            .currency("INR")
                                            .glAccountCode(glCode)
                                            .branch(branch)
                                            .homeBranch(branch)
                                            .build();
                            internalAccount = accountRepository.save(internalAccount);
                            log.info(
                                    "Auto-created internal loan asset account: {} GL={}",
                                    internalAccount.getAccountNumber(),
                                    glCode);
                            return internalAccount;
                        });
    }

    /**
     * Resolve the GL account for a customer account from its glAccountCode.
     *
     * <p>CBS rule: every customer account MUST have a valid GL mapping. The GL code on the account
     * identifies the deposit liability GL (e.g., Savings GL, Current GL) used as the contra leg
     * for loan disbursement, repayment, and other operations.
     *
     * @throws BusinessException if GL code is missing or GL account not found
     */
    private GeneralLedger resolveCustomerAccountGl(Account account) {
        String glCode = account.getGlAccountCode();
        if (glCode == null || glCode.isBlank()) {
            throw new BusinessException(
                    "GL_MAPPING_MISSING",
                    "Customer account "
                            + account.getAccountNumber()
                            + " has no GL account code. CBS requires valid GL mapping for all postings.");
        }
        return glRepository
                .findByGlCode(glCode)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        "GL_ACCOUNT_NOT_FOUND",
                                        "GL account "
                                                + glCode
                                                + " not found for account "
                                                + account.getAccountNumber()));
    }

    /**
     * Generate reducing balance EMI amortization schedule.
     *
     * <p>EMI formula: EMI = P × r × (1+r)^n / ((1+r)^n - 1) where P = principal, r = monthly rate,
     * n = tenure months.
     */
    private List<LoanSchedule> generateAmortizationSchedule(
            LoanAccount loan, LoanProduct product, LocalDate startDate) {
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal annualRate = product.getInterestRate();
        int tenureMonths = product.getTenureMonths();

        BigDecimal monthlyRate = EmiCalculator.monthlyRate(annualRate);
        BigDecimal emi = EmiCalculator.computeEmi(principal, annualRate, tenureMonths);

        List<LoanSchedule> schedule = new ArrayList<>();
        BigDecimal remaining = principal;

        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal interestComponent =
                    remaining
                            .multiply(monthlyRate, MathContext.DECIMAL128)
                            .setScale(4, RoundingMode.HALF_UP);
            BigDecimal principalComponent = emi.subtract(interestComponent);

            // Last installment adjustment to avoid rounding residual
            if (i == tenureMonths) {
                principalComponent = remaining;
                emi = principalComponent.add(interestComponent);
            }

            remaining = remaining.subtract(principalComponent);

            LoanSchedule installment =
                    LoanSchedule.builder()
                            .loanAccount(loan)
                            .account(loan.getLinkedAccount())
                            .installmentNumber(i)
                            .dueDate(startDate.plusMonths(i))
                            .principalComponent(principalComponent)
                            .interestComponent(interestComponent)
                            .emiAmount(emi)
                            .outstandingPrincipal(
                                    remaining.compareTo(BigDecimal.ZERO) < 0
                                            ? BigDecimal.ZERO
                                            : remaining)
                            .build();
            schedule.add(installment);
        }

        return schedule;
    }

    /**
     * Preview amortization schedule without persisting — Finacle LACSMNT pre-disbursement view.
     *
     * <p>Called by the controller's preview step so the maker can review the full repayment
     * schedule (EMI, principal/interest split per installment, total interest payable) before
     * confirming disbursement. No database writes occur.
     *
     * @param product the selected loan product
     * @param principalAmount the proposed principal
     * @param startDate the business date (disbursement date)
     * @return DTO with full schedule and summary KPIs
     */
    public LoanSchedulePreviewDTO previewSchedule(
            LoanProduct product, BigDecimal principalAmount, LocalDate startDate) {

        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_LOAN_AMOUNT", "Loan principal must be positive");
        }

        int tenureMonths = product.getTenureMonths();
        BigDecimal annualRate = product.getInterestRate();

        BigDecimal monthlyRate = EmiCalculator.monthlyRate(annualRate);
        BigDecimal emi = EmiCalculator.computeEmi(principalAmount, annualRate, tenureMonths);

        List<LoanSchedulePreviewDTO.Installment> installments = new ArrayList<>();
        BigDecimal remaining = principalAmount;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal interestComponent =
                    remaining
                            .multiply(monthlyRate, MathContext.DECIMAL128)
                            .setScale(4, RoundingMode.HALF_UP);
            BigDecimal principalComponent = emi.subtract(interestComponent);

            if (i == tenureMonths) {
                principalComponent = remaining;
            }

            remaining = remaining.subtract(principalComponent);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remaining = BigDecimal.ZERO;
            }
            totalInterest = totalInterest.add(interestComponent);

            BigDecimal installmentEmi = principalComponent.add(interestComponent);

            installments.add(
                    LoanSchedulePreviewDTO.Installment.builder()
                            .number(i)
                            .dueDate(startDate.plusMonths(i))
                            .emiAmount(installmentEmi)
                            .principalComponent(principalComponent)
                            .interestComponent(interestComponent)
                            .outstandingAfter(remaining)
                            .build());
        }

        return LoanSchedulePreviewDTO.builder()
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .interestRate(annualRate)
                .interestType(product.getInterestType().name())
                .tenureMonths(tenureMonths)
                .principalAmount(principalAmount)
                .emiAmount(emi)
                .totalInterestPayable(totalInterest)
                .totalAmountPayable(principalAmount.add(totalInterest))
                .firstEmiDate(startDate.plusMonths(1))
                .lastEmiDate(startDate.plusMonths(tenureMonths))
                .installments(installments)
                .build();
    }
}
