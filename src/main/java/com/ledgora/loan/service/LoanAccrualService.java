package com.ledgora.loan.service;

import com.ledgora.account.entity.Account;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
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
    private final VoucherService voucherService;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public LoanAccrualService(
            LoanAccountRepository loanAccountRepository,
            VoucherService voucherService,
            BranchRepository branchRepository,
            UserRepository userRepository,
            TenantRepository tenantRepository,
            AuditService auditService) {
        this.loanAccountRepository = loanAccountRepository;
        this.voucherService = voucherService;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    /**
     * Accrue daily interest for all performing loans of a tenant.
     *
     * <p>RBI IRAC: Only ACTIVE loans accrue interest. NPA/WRITTEN_OFF/CLOSED are skipped.
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

        var activeLoans = loanAccountRepository.findActiveByTenantId(tenantId);
        int accrued = 0;
        int skippedAlreadyAccrued = 0;

        for (LoanAccount loan : activeLoans) {
            if (loan.getStatus() != LoanStatus.ACTIVE) {
                continue; // defensive — query should only return ACTIVE
            }
            if (loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
                continue; // fully repaid
            }

            // Idempotency: skip if already accrued for this business date
            if (businessDate.equals(loan.getLastAccrualDate())) {
                skippedAlreadyAccrued++;
                continue;
            }

            // Use loan-level rate (overridable per RBI FPC), fallback to product rate
            LoanProduct product = loan.getLoanProduct();
            BigDecimal effectiveRate = loan.getInterestRate() != null
                    ? loan.getInterestRate() : product.getInterestRate();
            BigDecimal dailyRate = EmiCalculator.dailyRate(effectiveRate);

            BigDecimal dailyInterest =
                    loan.getOutstandingPrincipal()
                            .multiply(dailyRate, MathContext.DECIMAL128)
                            .setScale(4, RoundingMode.HALF_UP);

            if (dailyInterest.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Update accrued interest tracking on the loan account
            loan.setAccruedInterest(loan.getAccruedInterest().add(dailyInterest));
            loan.setLastAccrualDate(businessDate);
            loanAccountRepository.save(loan);

            // ── VOUCHER ENGINE: Post accrual (DR Interest Receivable, CR Interest Income) ──
            postAccrualVouchers(tenant, loan, product, dailyInterest, businessDate);

            accrued++;
            log.debug(
                    "Interest accrued: loan={} principal={} rate={}% daily={} total_accrued={}",
                    loan.getLoanAccountNumber(),
                    loan.getOutstandingPrincipal(),
                    product.getInterestRate(),
                    dailyInterest,
                    loan.getAccruedInterest());
        }

        if (skippedAlreadyAccrued > 0) {
            log.info(
                    "Loan accrual idempotency: skipped {} loans already accrued for date {} (tenant {})",
                    skippedAlreadyAccrued,
                    businessDate,
                    tenantId);
        }

        if (accrued > 0) {
            auditService.logEvent(
                    null,
                    "LOAN_INTEREST_ACCRUAL",
                    "LOAN_BATCH",
                    null,
                    "Daily interest accrued for "
                            + accrued
                            + " loans (tenant "
                            + tenantId
                            + ") date "
                            + businessDate,
                    null);
            log.info("Loan interest accrued: {} loans for tenant {}", accrued, tenantId);
        }

        return accrued;
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
            Tenant tenant, LoanAccount loan, LoanProduct product,
            BigDecimal dailyInterest, LocalDate businessDate) {
        try {
            Account customerAccount = loan.getLinkedAccount();
            Branch branch = customerAccount.getBranch() != null
                    ? customerAccount.getBranch()
                    : branchRepository.findByTenantId(tenant.getId()).stream()
                            .findFirst()
                            .orElseThrow(() -> new BusinessException("NO_BRANCH",
                                    "No branch configured for tenant " + tenant.getTenantCode()));

            User systemUser = userRepository.findByUsername("SYSTEM_AUTO")
                    .orElseThrow(() -> new BusinessException(
                            "SYSTEM_USER_MISSING",
                            "SYSTEM_AUTO user not configured. Accrual voucher posting blocked."));

            String batchCode = "LOAN-ACCR-" + loan.getLoanAccountNumber()
                    + "-" + businessDate.toString().replace("-", "");

            Voucher[] pair = voucherService.createVoucherPair(
                    tenant,
                    branch, customerAccount, product.getGlInterestReceivable(),
                    branch, customerAccount, product.getGlInterestIncome(),
                    dailyInterest,
                    loan.getCurrency(),
                    businessDate,
                    batchCode,
                    systemUser,
                    "Loan accrual DR: " + loan.getLoanAccountNumber()
                            + " interest=" + dailyInterest,
                    "Loan accrual CR: " + loan.getLoanAccountNumber()
                            + " interest income=" + dailyInterest);

            voucherService.systemAuthorizeVoucher(pair[0].getId(), systemUser);
            voucherService.systemAuthorizeVoucher(pair[1].getId(), systemUser);
            voucherService.postVoucher(pair[0].getId());
            voucherService.postVoucher(pair[1].getId());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Accrual voucher posting failed for loan {}: {}",
                    loan.getLoanAccountNumber(), e.getMessage(), e);
            throw new BusinessException(
                    "VOUCHER_POSTING_FAILED",
                    "Accrual voucher posting failed for loan "
                            + loan.getLoanAccountNumber() + ": " + e.getMessage());
        }
    }
}
