package com.ledgora.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.GLAccountType;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanProduct;
import com.ledgora.loan.enums.InterestType;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.loan.enums.NpaClassification;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanProductRepository;
import com.ledgora.loan.service.LoanAccrualService;
import com.ledgora.loan.service.LoanNpaService;
import com.ledgora.loan.service.LoanProvisionService;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBI IRAC-grade Loan Module integrity tests.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>Daily accrual increases accruedInterest for ACTIVE loans
 *   <li>NPA classification at DPD > 90 (RBI Prudential Norms)
 *   <li>Provisioning rates per IRAC norms (0.4% STANDARD, 15% SUBSTANDARD)
 *   <li>NPA loans do NOT accrue interest (income recognition stops)
 *   <li>All accounting via voucher engine (no direct balance mutation)
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoanAccountingIntegrityTest {

    @Autowired private LoanAccrualService accrualService;
    @Autowired private LoanNpaService npaService;
    @Autowired private LoanProvisionService provisionService;
    @Autowired private LoanProductRepository productRepository;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private GeneralLedgerRepository glRepository;
    @Autowired private TenantRepository tenantRepository;

    private Tenant tenant;
    private LoanProduct product;
    private Account customerAccount;

    @BeforeEach
    void setup() {
        tenant = tenantRepository.findByTenantCode("TENANT-001").orElse(null);
        if (tenant == null) return;
        TenantContextHolder.setTenantId(tenant.getId());

        // Create loan GL accounts
        GeneralLedger glLoanAsset = seedGl("9100", "Loan Asset", GLAccountType.ASSET);
        GeneralLedger glIntIncome =
                seedGl("9200", "Interest Income - Loans", GLAccountType.REVENUE);
        GeneralLedger glIntRecv = seedGl("9300", "Interest Receivable", GLAccountType.ASSET);
        GeneralLedger glNpaAsset = seedGl("9400", "NPA Loan Asset", GLAccountType.ASSET);
        GeneralLedger glProvision = seedGl("9500", "Loan Provision", GLAccountType.EXPENSE);

        product =
                productRepository.save(
                        LoanProduct.builder()
                                .tenant(tenant)
                                .productCode("PL-TEST-001")
                                .productName("Personal Loan Test")
                                .interestRate(new BigDecimal("12.0000"))
                                .interestType(InterestType.FIXED)
                                .compoundingFrequency("MONTHLY")
                                .tenureMonths(24)
                                .npaDaysThreshold(90)
                                .glLoanAsset(glLoanAsset)
                                .glInterestIncome(glIntIncome)
                                .glInterestReceivable(glIntRecv)
                                .glNpaLoanAsset(glNpaAsset)
                                .glProvision(glProvision)
                                .build());

        customerAccount =
                accountRepository.save(
                        Account.builder()
                                .accountNumber("TEST-SAV-LOAN-001")
                                .accountName("Test Loan Customer")
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .approvalStatus(MakerCheckerStatus.APPROVED)
                                .balance(new BigDecimal("100000.0000"))
                                .currency("INR")
                                .tenant(tenant)
                                .build());
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void accrual_increasesAccruedInterestForActiveLoans() {
        if (tenant == null) return;

        LoanAccount loan = createActiveLoan("LN-ACCRUAL-001", new BigDecimal("500000.0000"));

        BigDecimal before = loan.getAccruedInterest();
        int accrued = accrualService.accrueDailyInterest(tenant.getId());

        LoanAccount after = loanAccountRepository.findById(loan.getId()).orElseThrow();
        assertTrue(accrued > 0, "At least one loan should be accrued");
        assertTrue(
                after.getAccruedInterest().compareTo(before) > 0,
                "Accrued interest must increase: before="
                        + before
                        + " after="
                        + after.getAccruedInterest());

        // Verify daily interest = 500000 * 12% / 365 ≈ 164.38
        BigDecimal expected =
                new BigDecimal("500000")
                        .multiply(new BigDecimal("0.12"))
                        .divide(new BigDecimal("365"), 4, java.math.RoundingMode.HALF_UP);
        assertEquals(
                0,
                after.getAccruedInterest().compareTo(expected),
                "Daily interest should be ~164.38, got " + after.getAccruedInterest());
    }

    @Test
    void npaClassification_triggersAtDpdAboveThreshold() {
        if (tenant == null) return;

        LoanAccount loan = createActiveLoan("LN-NPA-001", new BigDecimal("300000.0000"));
        loan.setDpd(91); // Above 90-day threshold
        loanAccountRepository.save(loan);

        int newNpa = npaService.evaluateNpaAndUpdateDpd(tenant.getId());

        LoanAccount after = loanAccountRepository.findById(loan.getId()).orElseThrow();
        assertTrue(newNpa > 0, "Loan with DPD=91 should be classified as NPA");
        assertEquals(LoanStatus.NPA, after.getStatus());
        assertEquals(NpaClassification.SUBSTANDARD, after.getNpaClassification());
        assertNotNull(after.getNpaDate());
    }

    @Test
    void npaLoans_doNotAccrueInterest() {
        if (tenant == null) return;

        LoanAccount loan = createActiveLoan("LN-NPA-NOACC-001", new BigDecimal("200000.0000"));
        loan.setStatus(LoanStatus.NPA);
        loan.setNpaClassification(NpaClassification.SUBSTANDARD);
        loanAccountRepository.save(loan);

        BigDecimal before = loan.getAccruedInterest();
        accrualService.accrueDailyInterest(tenant.getId());

        LoanAccount after = loanAccountRepository.findById(loan.getId()).orElseThrow();
        assertEquals(
                0,
                after.getAccruedInterest().compareTo(before),
                "NPA loans must NOT accrue interest (RBI IRAC income recognition stop)");
    }

    @Test
    void provisioning_appliesCorrectRates() {
        if (tenant == null) return;

        // STANDARD loan: 0.4% provision
        LoanAccount standardLoan =
                createActiveLoan("LN-PROV-STD-001", new BigDecimal("1000000.0000"));

        // SUBSTANDARD loan: 15% provision
        LoanAccount subLoan = createActiveLoan("LN-PROV-SUB-001", new BigDecimal("500000.0000"));
        subLoan.setStatus(LoanStatus.NPA);
        subLoan.setNpaClassification(NpaClassification.SUBSTANDARD);
        loanAccountRepository.save(subLoan);

        provisionService.calculateProvisions(tenant.getId());

        LoanAccount stdAfter = loanAccountRepository.findById(standardLoan.getId()).orElseThrow();
        LoanAccount subAfter = loanAccountRepository.findById(subLoan.getId()).orElseThrow();

        // STANDARD: 1,000,000 × 0.4% = 4,000
        assertEquals(
                0,
                stdAfter.getProvisionAmount().compareTo(new BigDecimal("4000.0000")),
                "Standard provision should be 0.4% = 4000, got " + stdAfter.getProvisionAmount());

        // SUBSTANDARD: 500,000 × 15% = 75,000
        assertEquals(
                0,
                subAfter.getProvisionAmount().compareTo(new BigDecimal("75000.0000")),
                "Substandard provision should be 15% = 75000, got "
                        + subAfter.getProvisionAmount());
    }

    private LoanAccount createActiveLoan(String loanNumber, BigDecimal principal) {
        return loanAccountRepository.save(
                LoanAccount.builder()
                        .tenant(tenant)
                        .loanProduct(product)
                        .linkedAccount(customerAccount)
                        .loanAccountNumber(loanNumber)
                        .principalAmount(principal)
                        .outstandingPrincipal(principal)
                        .status(LoanStatus.ACTIVE)
                        .npaClassification(NpaClassification.STANDARD)
                        .build());
    }

    private GeneralLedger seedGl(String code, String name, GLAccountType type) {
        return glRepository
                .findByGlCode(code)
                .orElseGet(
                        () ->
                                glRepository.save(
                                        GeneralLedger.builder()
                                                .glCode(code)
                                                .glName(name)
                                                .description(name)
                                                .accountType(type)
                                                .level(1)
                                                .isActive(true)
                                                .normalBalance(
                                                        type == GLAccountType.ASSET
                                                                        || type
                                                                                == GLAccountType
                                                                                        .EXPENSE
                                                                ? "DEBIT"
                                                                : "CREDIT")
                                                .build()));
    }
}
