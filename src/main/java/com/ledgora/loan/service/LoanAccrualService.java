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
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.validation.EmiCalculator;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.voucher.entity.Voucher;
import com.ledgora.voucher.service.VoucherService;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI-compliant daily interest accrual for performing loans.
 *
 * <p>RBI Master Circular on Income Recognition (IRAC):
 *
 * <ul>
 *   <li>Interest on ACTIVE (performing) loans accrued on accrual basis
 *   <li>Interest on NPA loans: recognition STOPS — no accrual entry
 *   <li>Daily interest = outstandingPrincipal × (annualRate / 365)
 * </ul>
 *
 * <p>Accounting entry (via voucher engine — NOT direct balance mutation):
 *
 * <pre>
 *   DR Interest Receivable GL (Asset)
 *   CR Interest Income GL (Revenue)
 * </pre>
 *
 * <p>Called during EOD Phase VALIDATED, BEFORE statement snapshot generation.
 */
@Service
public class LoanAccrualService {

    private static final Logger log = LoggerFactory.getLogger(LoanAccrualService.class);
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private final LoanAccountRepository loanAccountRepository;
    private final AccountRepository accountRepository;
    private final VoucherService voucherService;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public LoanAccrualService(
            LoanAccountRepository loanAccountRepository,
            AccountRepository accountRepository,
            VoucherService voucherService,
            BranchRepository branchRepository,
            UserRepository userRepository,
            TenantRepository tenantRepository,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.accountRepository = accountRepository;
        this.voucherService = voucherService;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    /**
     * Accrue daily interest for all performing and NPA loans of a tenant.
     *
     * <p>RBI IRAC dual-track accrual:
     *
     * <ul>
     *   <li>ACTIVE loans: DR Interest Receivable, CR Interest Income (recognized)
     *   <li>NPA loans: DR Accrued Interest, CR Interest Suspense (NOT recognized as income)
     * </ul>
     *
     * @return number of loans accrued
     */
    @Transactional
    public int accrueDailyInterest(Long tenantId) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        LocalDate businessDate = tenant.getCurrentBusinessDate();

        // Process ACTIVE + NPA loans (NPA loans accrue to suspense per RBI IRAC)
        var allLoans = loanAccountRepository.findActiveAndNpaByTenantId(tenantId);
        int accrued = 0;
        int npaSuspenseAccrued = 0;
        int skippedAlreadyAccrued = 0;

        for (LoanAccount loan : allLoans) {
            if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.NPA) {
                continue;
            }
            if (loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
                continue; // fully repaid
            }

            // Moratorium: skip accrual during moratorium period
            if (loan.getMoratoriumEndDate() != null
                    && businessDate.isBefore(loan.getMoratoriumEndDate())) {
                continue;
            }

            // Idempotency: skip if already accrued for this business date
            if (businessDate.equals(loan.getLastAccrualDate())) {
                skippedAlreadyAccrued++;
                continue;
            }

            // Use loan-level rate (overridable per RBI FPC), fallback to product rate
            LoanProduct product = loan.getLoanProduct();
            BigDecimal effectiveRate =
                    loan.getInterestRate() != null
                            ? loan.getInterestRate()
                            : product.getInterestRate();
            BigDecimal dailyRate = EmiCalculator.dailyRate(effectiveRate);

            BigDecimal dailyInterest =
                    loan.getOutstandingPrincipal()
                            .multiply(dailyRate, MathContext.DECIMAL128)
                            .setScale(4, RoundingMode.HALF_UP);

            if (dailyInterest.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (loan.getStatus() == LoanStatus.NPA) {
                // ── NPA SUSPENSE ACCRUAL ──
                // RBI IRAC: Interest on NPA loans accrued to suspense, NOT income.
                // GL: DR Interest Receivable (memorandum), CR Interest Suspense
                // This is a contra entry — interest is tracked but NOT recognized as income.
                loan.setInterestReversed(loan.getInterestReversed().add(dailyInterest));
                loan.setLastAccrualDate(businessDate);
                loanAccountRepository.save(loan);

                // Post NPA suspense vouchers to maintain GL trail
                // DR Interest Receivable (suspense asset), CR Interest Receivable (suspense contra)
                // NOTE: In a full Finacle implementation, separate Interest Suspense GL accounts
                // would be used. For now, the entity-level interestReversed tracks the suspense
                // amount, and the voucher creates the GL trail for audit purposes.
                try {
                    postNpaSuspenseVouchers(tenant, loan, product, dailyInterest, businessDate);
                } catch (Exception e) {
                    // NPA suspense voucher failure should not block accrual — log and continue
                    log.warn(
                            "NPA suspense voucher posting failed for loan {} (accrual recorded): {}",
                            loan.getLoanAccountNumber(),
                            e.getMessage());
                }

                npaSuspenseAccrued++;

                log.debug(
                        "NPA suspense accrual: loan={} daily={} totalSuspense={}",
                        loan.getLoanAccountNumber(),
                        dailyInterest,
                        loan.getInterestReversed());
            } else {
                // ── STANDARD ACCRUAL (ACTIVE loans) ──
                // GL: DR Interest Receivable, CR Interest Income
                loan.setAccruedInterest(loan.getAccruedInterest().add(dailyInterest));
                loan.setLastAccrualDate(businessDate);
                loanAccountRepository.save(loan);

                // ── VOUCHER ENGINE: Post accrual ──
                postAccrualVouchers(tenant, loan, product, dailyInterest, businessDate);

                accrued++;
                log.debug(
                        "Interest accrued: loan={} principal={} rate={}% daily={} total_accrued={}",
                        loan.getLoanAccountNumber(),
                        loan.getOutstandingPrincipal(),
                        effectiveRate,
                        dailyInterest,
                        loan.getAccruedInterest());
            }
        }

        if (skippedAlreadyAccrued > 0) {
            log.info(
                    "Loan accrual idempotency: skipped {} loans already accrued for date {} (tenant {})",
                    skippedAlreadyAccrued,
                    businessDate,
                    tenantId);
        }

        if (accrued > 0 || npaSuspenseAccrued > 0) {
            auditService.logEvent(
                    null,
                    "LOAN_INTEREST_ACCRUAL",
                    "LOAN_BATCH",
                    null,
                    "Daily interest accrued for "
                            + accrued
                            + " performing + "
                            + npaSuspenseAccrued
                            + " NPA suspense loans (tenant "
                            + tenantId
                            + ") date "
                            + businessDate,
                    null);
            log.info(
                    "Loan interest accrued: {} performing + {} NPA suspense for tenant {}",
                    accrued,
                    npaSuspenseAccrued,
                    tenantId);
        }

        return accrued + npaSuspenseAccrued;
    }

    /**
     * Post NPA suspense accrual voucher pair.
     *
     * <p>RBI IRAC: NPA interest is NOT recognized as income. The entry uses the Interest Receivable
     * GL as both DR and CR (memorandum entry) to create a GL trail without income recognition. In a
     * full implementation, a dedicated Interest Suspense GL would be the CR leg.
     *
     * <pre>
     *   DR Interest Receivable GL (suspense — memorandum asset)
     *   CR Interest Receivable GL (suspense — contra, NOT income)
     * </pre>
     */
    private void postNpaSuspenseVouchers(
            Tenant tenant,
            LoanAccount loan,
            LoanProduct product,
            BigDecimal dailyInterest,
            LocalDate businessDate) {
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
                                                        "No branch configured for tenant "
                                                                + tenant.getTenantCode()));

        User systemUser =
                userRepository
                        .findByUsername("SYSTEM_AUTO")
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "SYSTEM_USER_MISSING",
                                                "SYSTEM_AUTO user not configured."));

        String batchCode =
                "LOAN-NPA-SUSP-"
                        + loan.getLoanAccountNumber()
                        + "-"
                        + businessDate.toString().replace("-", "");

        // NPA suspense: DR Interest Receivable, CR Interest Receivable (memorandum)
        // In production with dedicated suspense GL, CR would use glInterestSuspense
        Voucher[] pair =
                voucherService.createVoucherPair(
                        tenant,
                        branch,
                        customerAccount,
                        product.getGlInterestReceivable(), // DR — suspense receivable
                        branch,
                        customerAccount,
                        product.getGlInterestReceivable(), // CR — suspense contra
                        dailyInterest,
                        loan.getCurrency(),
                        businessDate,
                        batchCode,
                        systemUser,
                        "NPA suspense accrual DR: "
                                + loan.getLoanAccountNumber()
                                + " interest="
                                + dailyInterest,
                        "NPA suspense accrual CR: "
                                + loan.getLoanAccountNumber()
                                + " suspense="
                                + dailyInterest);

        voucherService.systemAuthorizeVoucher(pair[0].getId(), systemUser);
        voucherService.systemAuthorizeVoucher(pair[1].getId(), systemUser);
        voucherService.postVoucher(pair[0].getId());
        voucherService.postVoucher(pair[1].getId());
    }

    /**
     * Post accrual voucher pair via the voucher engine.
     *
     * <p>CBS-grade double-entry per RBI IRAC:
     *
     * <pre>
     *   DR Interest Receivable GL (Asset — accrued but unrealized)
     *   CR Interest Income GL (Revenue — recognized on accrual basis)
     * </pre>
     */
    private void postAccrualVouchers(
            Tenant tenant,
            LoanAccount loan,
            LoanProduct product,
            BigDecimal dailyInterest,
            LocalDate businessDate) {
        try {
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
                                                            "No branch configured for tenant "
                                                                    + tenant.getTenantCode()));

            User systemUser =
                    userRepository
                            .findByUsername("SYSTEM_AUTO")
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    "SYSTEM_USER_MISSING",
                                                    "SYSTEM_AUTO user not configured. Accrual voucher posting blocked."));

            String batchCode =
                    "LOAN-ACCR-"
                            + loan.getLoanAccountNumber()
                            + "-"
                            + businessDate.toString().replace("-", "");

            // CBS/Finacle: Accrual is a GL-to-GL entry — must NOT affect customer balance.
            // DR leg targets internal Interest Receivable account
            // CR leg targets internal Interest Income account
            Account intReceivableAccount =
                    resolveInternalAccount(
                            tenant,
                            product.getGlInterestReceivable().getGlCode(),
                            branch,
                            "INT-INTRECV-" + tenant.getTenantCode(),
                            "Interest Receivable Internal Account");
            Account intIncomeAccount =
                    resolveInternalAccount(
                            tenant,
                            product.getGlInterestIncome().getGlCode(),
                            branch,
                            "INT-INTINC-" + tenant.getTenantCode(),
                            "Interest Income Internal Account");

            Voucher[] pair =
                    voucherService.createVoucherPair(
                            tenant,
                            branch,
                            intReceivableAccount,
                            product.getGlInterestReceivable(),
                            branch,
                            intIncomeAccount,
                            product.getGlInterestIncome(),
                            dailyInterest,
                            loan.getCurrency(),
                            businessDate,
                            batchCode,
                            systemUser,
                            "Loan accrual DR: "
                                    + loan.getLoanAccountNumber()
                                    + " interest="
                                    + dailyInterest,
                            "Loan accrual CR: "
                                    + loan.getLoanAccountNumber()
                                    + " interest income="
                                    + dailyInterest);

            voucherService.systemAuthorizeVoucher(pair[0].getId(), systemUser);
            voucherService.systemAuthorizeVoucher(pair[1].getId(), systemUser);
            voucherService.postVoucher(pair[0].getId());
            voucherService.postVoucher(pair[1].getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Accrual voucher posting failed for loan {}: {}",
                    loan.getLoanAccountNumber(),
                    e.getMessage(),
                    e);
            throw new BusinessException(
                    "VOUCHER_POSTING_FAILED",
                    "Accrual voucher posting failed for loan "
                            + loan.getLoanAccountNumber()
                            + ": "
                            + e.getMessage());
        }
    }

    /**
     * Resolve or auto-create an internal account for a given GL code.
     *
     * <p>CBS/Finacle pattern: GL-level voucher legs need dedicated internal accounts so the
     * VoucherService updates the correct account balance per leg. If the internal account doesn't
     * exist for this tenant + GL code, it's auto-created as INTERNAL_ACCOUNT type.
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
}
