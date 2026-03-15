package com.ledgora.loan.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.validation.LoanBusinessValidator;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loan Write-Off Service — removes unrecoverable loans from the asset book.
 *
 * <p>RBI Master Circular on Prudential Norms — Write-Off:
 *
 * <ul>
 *   <li>Only LOSS-classified loans may be written off
 *   <li>Provision must cover 100% of outstanding before write-off
 *   <li>Write-off removes the loan from active asset GL
 *   <li>Recovery after write-off is possible (posted as income)
 * </ul>
 *
 * <p>Accounting entry:
 *
 * <pre>
 *   DR Loan Provision GL (reduces provision — contra liability)
 *   CR Loan Asset GL (removes loan from asset book)
 * </pre>
 *
 * <p>After write-off: status → WRITTEN_OFF, outstanding → 0, provision → 0.
 */
@Service
public class LoanWriteOffService {

    private static final Logger log = LoggerFactory.getLogger(LoanWriteOffService.class);

    private final LoanAccountRepository loanAccountRepository;
    private final AccountRepository accountRepository;
    private final VoucherService voucherService;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final GeneralLedgerRepository glRepository;
    private final TenantService tenantService;
    private final AuditService auditService;

    public LoanWriteOffService(
            LoanAccountRepository loanAccountRepository,
            AccountRepository accountRepository,
            VoucherService voucherService,
            BranchRepository branchRepository,
            UserRepository userRepository,
            GeneralLedgerRepository glRepository,
            TenantService tenantService,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.accountRepository = accountRepository;
        this.voucherService = voucherService;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.glRepository = glRepository;
        this.tenantService = tenantService;
        this.auditService = auditService;
    }

    /**
     * Write off a loan — removes from asset book.
     *
     * <p>Pre-conditions:
     *
     * <ul>
     *   <li>Loan must be NPA with LOSS classification
     *   <li>Provision must cover 100% of outstanding
     * </ul>
     *
     * <p>GL posting via voucher engine: DR Provision GL, CR NPA Loan Asset GL.
     */
    @Transactional
    public LoanAccount writeOff(Long loanAccountId) {
        LoanAccount loan =
                loanAccountRepository
                        .findById(loanAccountId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LOAN_NOT_FOUND",
                                                "Loan account not found: " + loanAccountId));

        // Centralized validation via LoanBusinessValidator
        Long tenantId = TenantContextHolder.getTenantId();
        LoanBusinessValidator.validateTenantOwnership(loan, tenantId);

        // CBS Tier-1: validate business day is OPEN before financial operations
        Long effectiveTenantId = tenantId != null ? tenantId : loan.getTenant().getId();
        tenantService.validateBusinessDayOpen(effectiveTenantId);

        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new BusinessException("LOAN_CLOSED", "Cannot write off a closed loan");
        }
        if (loan.getStatus() == LoanStatus.WRITTEN_OFF) {
            throw new BusinessException("LOAN_ALREADY_WRITTEN_OFF", "Loan is already written off");
        }
        if (loan.getStatus() != LoanStatus.NPA) {
            throw new BusinessException(
                    "LOAN_NOT_NPA",
                    "Only NPA loans can be written off. Current status: " + loan.getStatus());
        }
        if (loan.getNpaClassification() != NpaClassification.LOSS) {
            throw new BusinessException(
                    "LOAN_NOT_LOSS",
                    "Only LOSS-classified loans can be written off. Current: "
                            + loan.getNpaClassification());
        }

        // Verify provision covers outstanding
        if (loan.getProvisionAmount().compareTo(loan.getOutstandingPrincipal()) < 0) {
            throw new BusinessException(
                    "INSUFFICIENT_PROVISION",
                    "Provision ("
                            + loan.getProvisionAmount()
                            + ") must cover outstanding ("
                            + loan.getOutstandingPrincipal()
                            + ") before write-off");
        }

        BigDecimal writtenOffAmount = loan.getOutstandingPrincipal();

        // ── VOUCHER ENGINE: Post write-off (DR Provision GL, CR Loan Asset GL) ──
        postWriteOffVouchers(loan, writtenOffAmount);

        // Zero out the loan
        loan.setOutstandingPrincipal(BigDecimal.ZERO);
        loan.setAccruedInterest(BigDecimal.ZERO);
        loan.setProvisionAmount(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.WRITTEN_OFF);

        loan = loanAccountRepository.save(loan);

        auditService.logEvent(
                null,
                "LOAN_WRITTEN_OFF",
                "LOAN_ACCOUNT",
                loan.getId(),
                "Loan "
                        + loan.getLoanAccountNumber()
                        + " written off. Amount="
                        + writtenOffAmount
                        + " (100% provisioned, removed from asset book)",
                null);

        log.info(
                "LOAN WRITE-OFF: {} amount={} (removed from asset book)",
                loan.getLoanAccountNumber(),
                writtenOffAmount);

        return loan;
    }

    /**
     * Record recovery on a written-off loan.
     *
     * <p>RBI: Recovery after write-off is posted as income (not principal reduction). The loan
     * remains WRITTEN_OFF — recovery is a separate income event.
     *
     * <p>GL: DR Cash/Customer Account, CR Recovery Income
     *
     * @param loanAccountId the written-off loan
     * @param recoveryAmount amount recovered
     * @return the loan account (status unchanged — still WRITTEN_OFF)
     */
    @Transactional
    public LoanAccount recordRecovery(Long loanAccountId, BigDecimal recoveryAmount) {
        if (recoveryAmount == null || recoveryAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_RECOVERY", "Recovery amount must be positive");
        }

        LoanAccount loan =
                loanAccountRepository
                        .findById(loanAccountId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LOAN_NOT_FOUND",
                                                "Loan account not found: " + loanAccountId));

        Long tenantId = TenantContextHolder.getTenantId();
        LoanBusinessValidator.validateTenantOwnership(loan, tenantId);

        if (loan.getStatus() != LoanStatus.WRITTEN_OFF) {
            throw new BusinessException(
                    "NOT_WRITTEN_OFF",
                    "Recovery can only be recorded on written-off loans. Current status: "
                            + loan.getStatus());
        }

        // Recovery is posted as income — loan status remains WRITTEN_OFF
        // GL: DR Customer Account GL, CR Interest Income GL (recovery income)
        postRecoveryVouchers(loan, recoveryAmount);

        auditService.logEvent(
                null,
                "LOAN_RECOVERY_POST_WRITEOFF",
                "LOAN_ACCOUNT",
                loan.getId(),
                "Recovery recorded on written-off loan "
                        + loan.getLoanAccountNumber()
                        + ": amount="
                        + recoveryAmount
                        + " (posted as recovery income)",
                null);

        log.info(
                "LOAN RECOVERY: {} amount={} (post write-off recovery income)",
                loan.getLoanAccountNumber(),
                recoveryAmount);

        return loan;
    }

    /**
     * Post write-off voucher pair: DR Provision GL, CR Loan Asset GL.
     *
     * <p>Uses up the provision to remove the loan from the asset book.
     */
    private void postWriteOffVouchers(LoanAccount loan, BigDecimal amount) {
        try {
            Tenant tenant = loan.getTenant();
            LoanProduct product = loan.getLoanProduct();
            Account customerAccount = loan.getLinkedAccount();

            Branch branch =
                    customerAccount.getBranch() != null
                            ? customerAccount.getBranch()
                            : branchRepository.findByTenantId(tenant.getId()).stream()
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new BusinessException(
                                                            "NO_BRANCH",
                                                            "No branch configured"));

            User systemUser =
                    userRepository
                            .findByUsername("SYSTEM_AUTO")
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    "SYSTEM_USER_MISSING",
                                                    "SYSTEM_AUTO user not configured."));

            LocalDate businessDate =
                    tenantService.getCurrentBusinessDate(
                            tenant.getId());
            String batchCode = "LOAN-WOFF-" + loan.getLoanAccountNumber();

            // CBS/Finacle: Write-off is a GL-to-GL entry — must NOT affect customer balance.
            // DR leg targets internal Provision account, CR leg targets internal NPA Loan Asset
            Account provisionAccount =
                    resolveInternalAccount(
                            tenant, product.getGlProvision().getGlCode(), branch,
                            "INT-PROV-" + tenant.getTenantCode(),
                            "Loan Provision Internal Account");
            Account npaLoanAssetAccount =
                    resolveInternalAccount(
                            tenant, product.getGlNpaLoanAsset().getGlCode(), branch,
                            "INT-NPA-" + tenant.getTenantCode(),
                            "NPA Loan Asset Internal Account");

            // DR Provision GL (use up provision), CR NPA Loan Asset GL (remove from asset book)
            Voucher[] pair =
                    voucherService.createVoucherPair(
                            tenant,
                            branch,
                            provisionAccount,
                            product.getGlProvision(), // DR — Provision GL
                            branch,
                            npaLoanAssetAccount,
                            product.getGlNpaLoanAsset(), // CR — NPA Loan Asset GL
                            amount,
                            loan.getCurrency(),
                            businessDate,
                            batchCode,
                            systemUser,
                            "Write-off DR: " + loan.getLoanAccountNumber() + " provision=" + amount,
                            "Write-off CR: "
                                    + loan.getLoanAccountNumber()
                                    + " asset removed="
                                    + amount);

            voucherService.systemAuthorizeVoucher(pair[0].getId(), systemUser);
            voucherService.systemAuthorizeVoucher(pair[1].getId(), systemUser);
            voucherService.postVoucher(pair[0].getId());
            voucherService.postVoucher(pair[1].getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Write-off voucher posting failed for loan {}: {}",
                    loan.getLoanAccountNumber(),
                    e.getMessage(),
                    e);
            throw new BusinessException(
                    "VOUCHER_POSTING_FAILED",
                    "Write-off voucher posting failed: " + e.getMessage());
        }
    }

    /**
     * Post recovery voucher pair: DR Customer Account GL, CR Interest Income GL.
     *
     * <p>Recovery after write-off is posted as income — the loan remains WRITTEN_OFF.
     */
    private void postRecoveryVouchers(LoanAccount loan, BigDecimal amount) {
        try {
            Tenant tenant = loan.getTenant();
            LoanProduct product = loan.getLoanProduct();
            Account customerAccount = loan.getLinkedAccount();

            Branch branch =
                    customerAccount.getBranch() != null
                            ? customerAccount.getBranch()
                            : branchRepository.findByTenantId(tenant.getId()).stream()
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new BusinessException(
                                                            "NO_BRANCH",
                                                            "No branch configured"));

            User systemUser =
                    userRepository
                            .findByUsername("SYSTEM_AUTO")
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    "SYSTEM_USER_MISSING",
                                                    "SYSTEM_AUTO user not configured."));

            LocalDate businessDate =
                    tenantService.getCurrentBusinessDate(
                            tenant.getId());
            String batchCode = "LOAN-RECV-" + loan.getLoanAccountNumber();

            // CBS/Finacle: Recovery DR targets customer account (receives cash),
            // CR targets internal income account (income recognized).
            GeneralLedger customerGl = resolveCustomerAccountGl(customerAccount);
            Account incomeAccount =
                    resolveInternalAccount(
                            tenant, product.getGlInterestIncome().getGlCode(), branch,
                            "INT-INTINC-" + tenant.getTenantCode(),
                            "Interest Income Internal Account");

            Voucher[] pair =
                    voucherService.createVoucherPair(
                            tenant,
                            branch,
                            customerAccount,
                            customerGl, // DR — Customer Account GL (cash received)
                            branch,
                            incomeAccount,
                            product.getGlInterestIncome(), // CR — Recovery Income GL
                            amount,
                            loan.getCurrency(),
                            businessDate,
                            batchCode,
                            systemUser,
                            "Recovery DR: " + loan.getLoanAccountNumber() + " cash=" + amount,
                            "Recovery CR: "
                                    + loan.getLoanAccountNumber()
                                    + " income="
                                    + amount);

            voucherService.systemAuthorizeVoucher(pair[0].getId(), systemUser);
            voucherService.systemAuthorizeVoucher(pair[1].getId(), systemUser);
            voucherService.postVoucher(pair[0].getId());
            voucherService.postVoucher(pair[1].getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Recovery voucher posting failed for loan {}: {}",
                    loan.getLoanAccountNumber(),
                    e.getMessage(),
                    e);
            throw new BusinessException(
                    "VOUCHER_POSTING_FAILED",
                    "Recovery voucher posting failed: " + e.getMessage());
        }
    }

    /**
     * Resolve or auto-create an internal account for a given GL code.
     */
    private Account resolveInternalAccount(
            Tenant tenant, String glCode, Branch branch, String accountNumber, String accountName) {
        return accountRepository
                .findFirstByTenantIdAndGlAccountCode(tenant.getId(), glCode)
                .orElseGet(
                        () -> {
                            Account internalAccount =
                                    Account.builder()
                                            .tenant(tenant)
                                            .accountNumber(accountNumber)
                                            .accountName(accountName)
                                            .accountType(AccountType.INTERNAL_ACCOUNT)
                                            .status(AccountStatus.ACTIVE)
                                            .approvalStatus(MakerCheckerStatus.APPROVED)
                                            .balance(BigDecimal.ZERO)
                                            .currency("INR")
                                            .glAccountCode(glCode)
                                            .branch(branch)
                                            .homeBranch(branch)
                                            .build();
                            internalAccount = accountRepository.save(internalAccount);
                            log.info(
                                    "Auto-created internal account: {} GL={}",
                                    internalAccount.getAccountNumber(),
                                    glCode);
                            return internalAccount;
                        });
    }

    private GeneralLedger resolveCustomerAccountGl(Account account) {
        String glCode = account.getGlAccountCode();
        if (glCode == null || glCode.isBlank()) {
            throw new BusinessException(
                    "GL_MAPPING_MISSING",
                    "Customer account "
                            + account.getAccountNumber()
                            + " has no GL account code.");
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
}
