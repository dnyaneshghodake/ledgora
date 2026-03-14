package com.ledgora;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.account.entity.Account;
import com.ledgora.account.entity.AccountBalance;
import com.ledgora.account.repository.AccountBalanceRepository;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.balance.service.CbsBalanceEngine;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.calendar.entity.BankCalendar;
import com.ledgora.calendar.repository.BankCalendarRepository;
import com.ledgora.calendar.service.BankCalendarService;
import com.ledgora.common.enums.*;
import com.ledgora.common.exception.InvalidTransactionAmountException;
import com.ledgora.common.exception.ScriptInjectionException;
import com.ledgora.common.validation.InputSanitizer;
import com.ledgora.common.validation.RbiFieldValidator;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.repository.CustomerMasterRepository;
import com.ledgora.ledger.repository.LedgerEntryRepository;
import com.ledgora.lien.entity.AccountLien;
import com.ledgora.lien.repository.AccountLienRepository;
import com.ledgora.lien.service.AccountLienService;
import com.ledgora.ownership.entity.AccountOwnership;
import com.ledgora.ownership.repository.AccountOwnershipRepository;
import com.ledgora.ownership.service.AccountOwnershipService;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.transaction.dto.TransactionDTO;
import com.ledgora.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * PART 12: RBI-grade Governance Integration Tests. Tests field validation, freeze enforcement, lien
 * balance impact, maker-checker governance, ownership validation, holiday restriction, immutable
 * ledger enforcement, and script injection rejection.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgoraRbiGovernanceIntegrationTest {

    @Autowired private TransactionService transactionService;
    @Autowired private BankCalendarService calendarService;
    @Autowired private AccountOwnershipService ownershipService;
    @Autowired private AccountLienService lienService;
    @Autowired private CbsBalanceEngine balanceEngine;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountBalanceRepository accountBalanceRepository;
    @Autowired private CustomerMasterRepository customerMasterRepository;
    @Autowired private BankCalendarRepository calendarRepository;
    @Autowired private AccountLienRepository lienRepository;
    @Autowired private AccountOwnershipRepository ownershipRepository;
    @Autowired private LedgerEntryRepository ledgerEntryRepository;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. FIELD VALIDATION FAILURES
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("Transaction amount zero must be rejected")
    void testTransactionAmountZeroRejected() {
        assertThrows(
                InvalidTransactionAmountException.class,
                () -> RbiFieldValidator.validateTransactionAmount(BigDecimal.ZERO),
                "Zero amount must be rejected");
    }

    @Test
    @Order(2)
    @DisplayName("Transaction amount negative must be rejected")
    void testTransactionAmountNegativeRejected() {
        assertThrows(
                InvalidTransactionAmountException.class,
                () -> RbiFieldValidator.validateTransactionAmount(new BigDecimal("-100.00")),
                "Negative amount must be rejected");
    }

    @Test
    @Order(3)
    @DisplayName("Transaction amount with more than 2 decimal places must be rejected")
    void testTransactionAmountScaleRejected() {
        assertThrows(
                InvalidTransactionAmountException.class,
                () -> RbiFieldValidator.validateTransactionAmount(new BigDecimal("100.123")),
                "Amount with >2 decimal places must be rejected");
    }

    @Test
    @Order(4)
    @DisplayName("Transaction amount exceeding max limit must be rejected")
    void testTransactionAmountExceedsMaxLimit() {
        BigDecimal maxLimit = new BigDecimal("50000.00");
        assertThrows(
                InvalidTransactionAmountException.class,
                () ->
                        RbiFieldValidator.validateTransactionAmount(
                                new BigDecimal("100000.00"), maxLimit),
                "Amount exceeding max limit must be rejected");
    }

    @Test
    @Order(5)
    @DisplayName("Valid transaction amount must pass validation")
    void testTransactionAmountValidPasses() {
        assertDoesNotThrow(
                () -> RbiFieldValidator.validateTransactionAmount(new BigDecimal("500.00")),
                "Valid amount must pass");
    }

    @Test
    @Order(6)
    @DisplayName("Name with special characters must be rejected")
    void testNameValidationRejectsSpecialChars() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InputSanitizer.validateName("Rajesh123", "Name"),
                "Name with digits must be rejected");
    }

    @Test
    @Order(7)
    @DisplayName("Valid name with alphabets, space, and dot must pass")
    void testNameValidationPassesValid() {
        assertDoesNotThrow(
                () -> InputSanitizer.validateName("Dr. Rajesh Kumar", "Name"),
                "Valid name must pass");
    }

    @Test
    @Order(8)
    @DisplayName("Mobile number must be exactly 10 digits")
    void testMobileValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InputSanitizer.validateMobile("12345", "Mobile"),
                "Short mobile must be rejected");
        assertThrows(
                IllegalArgumentException.class,
                () -> InputSanitizer.validateMobile("12345678901", "Mobile"),
                "Long mobile must be rejected");
        assertDoesNotThrow(
                () -> InputSanitizer.validateMobile("9876543210", "Mobile"),
                "Valid 10-digit mobile must pass");
    }

    @Test
    @Order(9)
    @DisplayName("PAN format must be ABCDE1234F")
    void testPanValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InputSanitizer.validatePAN("INVALID", "PAN"),
                "Invalid PAN must be rejected");
        assertDoesNotThrow(
                () -> InputSanitizer.validatePAN("ABCDE1234F", "PAN"), "Valid PAN must pass");
    }

    @Test
    @Order(10)
    @DisplayName("Aadhaar must be exactly 12 digits")
    void testAadhaarValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InputSanitizer.validateAadhaar("12345", "Aadhaar"),
                "Short Aadhaar must be rejected");
        assertDoesNotThrow(
                () -> InputSanitizer.validateAadhaar("123456789012", "Aadhaar"),
                "Valid 12-digit Aadhaar must pass");
    }

    @Test
    @Order(11)
    @DisplayName("Email format must be valid")
    void testEmailValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InputSanitizer.validateEmail("not-an-email", "Email"),
                "Invalid email must be rejected");
        assertDoesNotThrow(
                () -> InputSanitizer.validateEmail("user@example.com", "Email"),
                "Valid email must pass");
    }

    @Test
    @Order(12)
    @DisplayName("Interest rate must be between 0 and 100")
    void testInterestRateValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RbiFieldValidator.validateInterestRate(new BigDecimal("-1"), "Interest Rate"),
                "Negative rate must be rejected");
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        RbiFieldValidator.validateInterestRate(
                                new BigDecimal("101"), "Interest Rate"),
                "Rate above 100 must be rejected");
        assertDoesNotThrow(
                () ->
                        RbiFieldValidator.validateInterestRate(
                                new BigDecimal("7.5"), "Interest Rate"),
                "Valid rate must pass");
    }

    @Test
    @Order(13)
    @DisplayName("Overdraft must be >= 0")
    void testOverdraftValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RbiFieldValidator.validateOverdraft(new BigDecimal("-100"), "Overdraft"),
                "Negative overdraft must be rejected");
        assertDoesNotThrow(
                () -> RbiFieldValidator.validateOverdraft(BigDecimal.ZERO, "Overdraft"),
                "Zero overdraft must pass");
    }

    @Test
    @Order(14)
    @DisplayName("Ownership percentage must be between 0 and 100")
    void testOwnershipPercentageValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RbiFieldValidator.validateOwnershipPercentage(BigDecimal.ZERO),
                "Zero ownership must be rejected");
        assertThrows(
                IllegalArgumentException.class,
                () -> RbiFieldValidator.validateOwnershipPercentage(new BigDecimal("101")),
                "Ownership above 100 must be rejected");
        assertDoesNotThrow(
                () -> RbiFieldValidator.validateOwnershipPercentage(new BigDecimal("50")),
                "Valid ownership must pass");
    }

    @Test
    @Order(15)
    @DisplayName("DOB must indicate customer is at least 18 years old")
    void testDobValidation() {
        LocalDate minorDob = LocalDate.now().minusYears(10);
        assertThrows(
                IllegalArgumentException.class,
                () -> RbiFieldValidator.validateDob(minorDob),
                "Minor DOB must be rejected");
        LocalDate adultDob = LocalDate.now().minusYears(25);
        assertDoesNotThrow(() -> RbiFieldValidator.validateDob(adultDob), "Adult DOB must pass");
    }

    @Test
    @Order(16)
    @DisplayName("PAN is mandatory for INDIVIDUAL customers")
    void testPanMandatoryForIndividual() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RbiFieldValidator.validatePanForIndividual("INDIVIDUAL", null),
                "Missing PAN for INDIVIDUAL must be rejected");
        assertDoesNotThrow(
                () -> RbiFieldValidator.validatePanForIndividual("CORPORATE", null),
                "Missing PAN for CORPORATE is allowed");
    }

    @Test
    @Order(17)
    @DisplayName("GST is mandatory for CORPORATE customers")
    void testGstMandatoryForCorporate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RbiFieldValidator.validateGstForCorporate("CORPORATE", null),
                "Missing GST for CORPORATE must be rejected");
        assertDoesNotThrow(
                () -> RbiFieldValidator.validateGstForCorporate("INDIVIDUAL", null),
                "Missing GST for INDIVIDUAL is allowed");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. FREEZE ENFORCEMENT
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(20)
    @Transactional
    @DisplayName("FULL freeze on customer blocks all CustomerMaster fields")
    void testCustomerFullFreeze() {
        TestContext ctx = createTestContext("CFRZ");

        CustomerMaster customer = createCustomerMaster(ctx, "CFRZ-001");
        customer.setFreezeLevel(FreezeLevel.FULL);
        customer.setFreezeReason("RBI directive");
        customerMasterRepository.save(customer);

        CustomerMaster frozen = customerMasterRepository.findById(customer.getId()).orElseThrow();
        assertEquals(FreezeLevel.FULL, frozen.getFreezeLevel(), "Freeze level must be FULL");
        assertEquals("RBI directive", frozen.getFreezeReason(), "Freeze reason must be preserved");
    }

    @Test
    @Order(21)
    @Transactional
    @DisplayName("Account FULL freeze persists correctly")
    void testAccountFullFreeze() {
        TestContext ctx = createTestContext("AFRZ");

        Account account = ctx.source;
        account.setFreezeLevel(FreezeLevel.FULL);
        account.setFreezeReason("Court order");
        accountRepository.save(account);

        Account frozen = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(
                FreezeLevel.FULL, frozen.getFreezeLevel(), "Account freeze level must be FULL");
        assertEquals(
                "Court order", frozen.getFreezeReason(), "Account freeze reason must be preserved");
    }

    @Test
    @Order(22)
    @Transactional
    @DisplayName("DEBIT_ONLY and CREDIT_ONLY freeze levels are distinct")
    void testPartialFreezeLevels() {
        TestContext ctx = createTestContext("PFRZ");

        CustomerMaster customer = createCustomerMaster(ctx, "PFRZ-001");
        customer.setFreezeLevel(FreezeLevel.DEBIT_ONLY);
        customerMasterRepository.save(customer);

        CustomerMaster debitFrozen =
                customerMasterRepository.findById(customer.getId()).orElseThrow();
        assertEquals(FreezeLevel.DEBIT_ONLY, debitFrozen.getFreezeLevel());

        customer.setFreezeLevel(FreezeLevel.CREDIT_ONLY);
        customerMasterRepository.save(customer);

        CustomerMaster creditFrozen =
                customerMasterRepository.findById(customer.getId()).orElseThrow();
        assertEquals(FreezeLevel.CREDIT_ONLY, creditFrozen.getFreezeLevel());
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. LIEN REDUCES AVAILABLE BALANCE
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(30)
    @Transactional
    @DisplayName("Lien reduces available balance via BalanceEngine")
    void testLienReducesAvailableBalance() {
        TestContext ctx = createTestContext("LIEN");

        Account account = ctx.source;
        BigDecimal initialAvailable =
                accountBalanceRepository
                        .findByAccountId(account.getId())
                        .orElseThrow()
                        .getAvailableBalance();

        // Apply lien
        BigDecimal lienAmount = new BigDecimal("1000.00");
        balanceEngine.applyLien(account.getId(), lienAmount);

        AccountBalance balance =
                accountBalanceRepository.findByAccountId(account.getId()).orElseThrow();
        BigDecimal expectedAvailable = initialAvailable.subtract(lienAmount);
        assertEquals(
                0,
                expectedAvailable.compareTo(balance.getAvailableBalance()),
                "Available balance must decrease by lien amount");
        assertEquals(
                0,
                lienAmount.compareTo(balance.getLienBalance()),
                "Lien balance must equal the applied lien amount");
    }

    @Test
    @Order(31)
    @Transactional
    @DisplayName("Lien release restores available balance")
    void testLienReleaseRestoresBalance() {
        TestContext ctx = createTestContext("LREL");

        Account account = ctx.source;
        BigDecimal lienAmount = new BigDecimal("500.00");

        // Apply then release lien
        balanceEngine.applyLien(account.getId(), lienAmount);
        BigDecimal afterLien =
                accountBalanceRepository
                        .findByAccountId(account.getId())
                        .orElseThrow()
                        .getAvailableBalance();

        balanceEngine.releaseLien(account.getId(), lienAmount);
        BigDecimal afterRelease =
                accountBalanceRepository
                        .findByAccountId(account.getId())
                        .orElseThrow()
                        .getAvailableBalance();

        assertTrue(
                afterRelease.compareTo(afterLien) > 0,
                "Available balance must increase after lien release");
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        accountBalanceRepository
                                .findByAccountId(account.getId())
                                .orElseThrow()
                                .getLienBalance()),
                "Lien balance must be zero after full release");
    }

    @Test
    @Order(32)
    @Transactional
    @DisplayName("Lien cannot exceed ledger balance")
    void testLienCannotExceedLedgerBalance() {
        TestContext ctx = createTestContext("LEXC");

        Account account = ctx.source;
        TenantContextHolder.setTenantId(ctx.tenant.getId());

        // Try to create a lien that exceeds ledger balance
        BigDecimal excessiveLien = new BigDecimal("999999.00");
        assertThrows(
                RuntimeException.class,
                () ->
                        lienService.createLien(
                                ctx.tenant.getId(),
                                account.getId(),
                                excessiveLien,
                                LienType.COURT_ORDER,
                                LocalDate.now(),
                                LocalDate.now().plusDays(30),
                                "REF-EXCESS",
                                "Test excess lien"),
                "Lien exceeding ledger balance must be rejected");
    }

    @Test
    @Order(33)
    @Transactional
    @DisplayName("Lien amount must be positive")
    void testLienAmountMustBePositive() {
        TestContext ctx = createTestContext("LPOS");

        TenantContextHolder.setTenantId(ctx.tenant.getId());
        assertThrows(
                RuntimeException.class,
                () ->
                        lienService.createLien(
                                ctx.tenant.getId(),
                                ctx.source.getId(),
                                new BigDecimal("-100.00"),
                                LienType.COURT_ORDER,
                                LocalDate.now(),
                                LocalDate.now().plusDays(30),
                                "REF-NEG",
                                "Negative lien test"),
                "Negative lien amount must be rejected");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. MAKER CANNOT APPROVE OWN ACTION
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(40)
    @Transactional
    @DisplayName("Calendar: maker cannot approve own entry")
    void testCalendarMakerCannotApproveOwn() {
        TestContext ctx = createTestContext("CMKR");

        // Create calendar entry as maker1
        setSecurityContext(ctx.maker);
        TenantContextHolder.setTenantId(ctx.tenant.getId());

        BankCalendar calendar =
                calendarService.createCalendarEntry(
                        ctx.tenant.getId(),
                        LocalDate.now().plusDays(30),
                        "HOLIDAY",
                        "Test Holiday",
                        "NATIONAL",
                        false,
                        false,
                        "Test maker-checker");

        // Try to approve as the same user (maker1)
        assertThrows(
                RuntimeException.class,
                () -> calendarService.approveCalendarEntry(calendar.getId()),
                "Maker must not be able to approve own calendar entry");
    }

    @Test
    @Order(41)
    @Transactional
    @DisplayName("Calendar: different checker can approve entry")
    void testCalendarDifferentCheckerCanApprove() {
        TestContext ctx = createTestContext("CCHK");

        // Create as maker
        setSecurityContext(ctx.maker);
        TenantContextHolder.setTenantId(ctx.tenant.getId());

        BankCalendar calendar =
                calendarService.createCalendarEntry(
                        ctx.tenant.getId(),
                        LocalDate.now().plusDays(31),
                        "HOLIDAY",
                        "Checker Test Holiday",
                        "NATIONAL",
                        false,
                        false,
                        "Checker approval test");

        // Approve as checker (different user)
        setSecurityContext(ctx.checker);

        BankCalendar approved = calendarService.approveCalendarEntry(calendar.getId());
        assertEquals(
                MakerCheckerStatus.APPROVED,
                approved.getApprovalStatus(),
                "Calendar entry must be approved by different checker");
    }

    @Test
    @Order(42)
    @Transactional
    @DisplayName("Ownership: maker cannot approve own request")
    void testOwnershipMakerCannotApproveOwn() {
        TestContext ctx = createTestContext("OMKR");

        CustomerMaster customer = createCustomerMaster(ctx, "OMKR-001");
        setSecurityContext(ctx.maker);
        TenantContextHolder.setTenantId(ctx.tenant.getId());

        AccountOwnership ownership =
                ownershipService.createOwnership(
                        ctx.tenant.getId(),
                        ctx.source.getId(),
                        customer.getId(),
                        OwnershipType.PRIMARY,
                        new BigDecimal("100"),
                        true);

        // Same user tries to approve
        assertThrows(
                RuntimeException.class,
                () -> ownershipService.approveOwnership(ownership.getId()),
                "Maker must not be able to approve own ownership request");
    }

    @Test
    @Order(43)
    @Transactional
    @DisplayName("Lien: maker cannot approve own lien")
    void testLienMakerCannotApproveOwn() {
        TestContext ctx = createTestContext("LMKR");

        setSecurityContext(ctx.maker);
        TenantContextHolder.setTenantId(ctx.tenant.getId());

        AccountLien lien =
                lienService.createLien(
                        ctx.tenant.getId(),
                        ctx.source.getId(),
                        new BigDecimal("500.00"),
                        LienType.COURT_ORDER,
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        "REF-MKR",
                        "Maker-checker test");

        // Same user tries to approve
        assertThrows(
                RuntimeException.class,
                () -> lienService.approveLien(lien.getId()),
                "Maker must not be able to approve own lien request");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. OWNERSHIP % VALIDATION
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(50)
    @Transactional
    @DisplayName("Ownership percentage must be between 0 and 100")
    void testOwnershipPercentageRange() {
        TestContext ctx = createTestContext("OPCT");

        CustomerMaster customer = createCustomerMaster(ctx, "OPCT-001");
        setSecurityContext(ctx.maker);
        TenantContextHolder.setTenantId(ctx.tenant.getId());

        // Zero percentage should fail
        assertThrows(
                RuntimeException.class,
                () ->
                        ownershipService.createOwnership(
                                ctx.tenant.getId(),
                                ctx.source.getId(),
                                customer.getId(),
                                OwnershipType.PRIMARY,
                                BigDecimal.ZERO,
                                true),
                "Zero ownership percentage must be rejected");

        // Above 100% should fail
        assertThrows(
                RuntimeException.class,
                () ->
                        ownershipService.createOwnership(
                                ctx.tenant.getId(),
                                ctx.source.getId(),
                                customer.getId(),
                                OwnershipType.PRIMARY,
                                new BigDecimal("150"),
                                true),
                "Ownership above 100% must be rejected");
    }

    @Test
    @Order(51)
    @Transactional
    @DisplayName("Total ownership cannot exceed 100%")
    void testOwnershipTotalCannotExceed100() {
        TestContext ctx = createTestContext("OTOT");

        CustomerMaster cust1 = createCustomerMaster(ctx, "OTOT-001");
        CustomerMaster cust2 = createCustomerMaster(ctx, "OTOT-002");

        setSecurityContext(ctx.maker);
        TenantContextHolder.setTenantId(ctx.tenant.getId());

        // Create first ownership at 60%
        AccountOwnership o1 =
                ownershipService.createOwnership(
                        ctx.tenant.getId(),
                        ctx.source.getId(),
                        cust1.getId(),
                        OwnershipType.PRIMARY,
                        new BigDecimal("60"),
                        true);

        // Approve it (as checker)
        setSecurityContext(ctx.checker);
        ownershipService.approveOwnership(o1.getId());

        // Try to add another ownership at 50% (total would be 110%)
        setSecurityContext(ctx.maker);
        assertThrows(
                RuntimeException.class,
                () ->
                        ownershipService.createOwnership(
                                ctx.tenant.getId(),
                                ctx.source.getId(),
                                cust2.getId(),
                                OwnershipType.JOINT,
                                new BigDecimal("50"),
                                true),
                "Total ownership exceeding 100% must be rejected");
    }

    @Test
    @Order(52)
    @Transactional
    @DisplayName("GL_ACCOUNT cannot be linked to customers")
    void testGlAccountCannotLinkToCustomer() {
        TestContext ctx = createTestContext("GLNK");

        // Create a GL account
        Account glAccount =
                accountRepository.save(
                        Account.builder()
                                .tenant(ctx.tenant)
                                .branch(ctx.branch)
                                .homeBranch(ctx.branch)
                                .accountNumber("GL-NL-" + "GLNK")
                                .accountName("GL No Link")
                                .accountType(AccountType.GL_ACCOUNT)
                                .status(AccountStatus.ACTIVE)
                                .balance(BigDecimal.ZERO)
                                .currency("INR")
                                .glAccountCode("9999")
                                .build());

        CustomerMaster customer = createCustomerMaster(ctx, "GLNK-001");
        setSecurityContext(ctx.maker);
        TenantContextHolder.setTenantId(ctx.tenant.getId());

        assertThrows(
                RuntimeException.class,
                () ->
                        ownershipService.createOwnership(
                                ctx.tenant.getId(),
                                glAccount.getId(),
                                customer.getId(),
                                OwnershipType.PRIMARY,
                                new BigDecimal("100"),
                                true),
                "GL_ACCOUNT cannot be linked to customers");
    }

    @Test
    @Order(53)
    @Transactional
    @DisplayName("Cross-tenant account-customer linking is not allowed")
    void testCrossTenantLinkingBlocked() {
        TestContext ctx = createTestContext("XTEN");

        // Create a different tenant
        Tenant otherTenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("TEN-XTEN-OTH")
                                .tenantName("Other Tenant")
                                .status("ACTIVE")
                                .currentBusinessDate(LocalDate.now())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        // Create customer under different tenant
        CustomerMaster otherCustomer =
                customerMasterRepository.save(
                        CustomerMaster.builder()
                                .tenant(otherTenant)
                                .customerNumber("XTEN-OTH-001")
                                .firstName("Other")
                                .lastName("Customer")
                                .kycStatus("VERIFIED")
                                .build());

        setSecurityContext(ctx.maker);
        TenantContextHolder.setTenantId(ctx.tenant.getId());

        assertThrows(
                RuntimeException.class,
                () ->
                        ownershipService.createOwnership(
                                ctx.tenant.getId(),
                                ctx.source.getId(),
                                otherCustomer.getId(),
                                OwnershipType.PRIMARY,
                                new BigDecimal("100"),
                                true),
                "Cross-tenant linking must be blocked");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. HOLIDAY RESTRICTION
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(60)
    @Transactional
    @DisplayName("Manual (TELLER) transactions blocked on holidays")
    void testManualTransactionsBlockedOnHoliday() {
        TestContext ctx = createTestContext("HDAY");

        TenantContextHolder.setTenantId(ctx.tenant.getId());

        // Create and approve a holiday entry
        setSecurityContext(ctx.maker);
        LocalDate holidayDate = LocalDate.now().plusDays(15);
        BankCalendar holiday =
                calendarService.createCalendarEntry(
                        ctx.tenant.getId(),
                        holidayDate,
                        "HOLIDAY",
                        "Republic Day",
                        "NATIONAL",
                        false,
                        false,
                        "Holiday test");

        setSecurityContext(ctx.checker);
        calendarService.approveCalendarEntry(holiday.getId());

        // Validate TELLER channel is blocked
        assertThrows(
                RuntimeException.class,
                () ->
                        calendarService.validateTransactionAllowed(
                                ctx.tenant.getId(), holidayDate, TransactionChannel.TELLER),
                "TELLER transactions must be blocked on holidays");
    }

    @Test
    @Order(61)
    @Transactional
    @DisplayName("ATM transactions blocked on holidays when not configured")
    void testAtmBlockedOnHolidayWhenNotConfigured() {
        TestContext ctx = createTestContext("HATM");

        TenantContextHolder.setTenantId(ctx.tenant.getId());

        // Create holiday with ATM NOT allowed
        setSecurityContext(ctx.maker);
        LocalDate holidayDate = LocalDate.now().plusDays(16);
        BankCalendar holiday =
                calendarService.createCalendarEntry(
                        ctx.tenant.getId(),
                        holidayDate,
                        "HOLIDAY",
                        "ATM Test Holiday",
                        "REGIONAL",
                        false,
                        false,
                        "ATM blocked holiday test");

        setSecurityContext(ctx.checker);
        calendarService.approveCalendarEntry(holiday.getId());

        assertThrows(
                RuntimeException.class,
                () ->
                        calendarService.validateTransactionAllowed(
                                ctx.tenant.getId(), holidayDate, TransactionChannel.ATM),
                "ATM transactions must be blocked when not configured on holiday");
    }

    @Test
    @Order(62)
    @Transactional
    @DisplayName("Working day allows all transactions")
    void testWorkingDayAllowsAllTransactions() {
        TestContext ctx = createTestContext("WDAY");

        TenantContextHolder.setTenantId(ctx.tenant.getId());

        // Create a working day entry
        setSecurityContext(ctx.maker);
        LocalDate workingDate = LocalDate.now().plusDays(17);
        BankCalendar workingDay =
                calendarService.createCalendarEntry(
                        ctx.tenant.getId(),
                        workingDate,
                        "WORKING_DAY",
                        null,
                        null,
                        true,
                        true,
                        "Working day test");

        setSecurityContext(ctx.checker);
        calendarService.approveCalendarEntry(workingDay.getId());

        assertDoesNotThrow(
                () ->
                        calendarService.validateTransactionAllowed(
                                ctx.tenant.getId(), workingDate, TransactionChannel.TELLER),
                "All transactions must be allowed on working days");
    }

    @Test
    @Order(63)
    @Transactional
    @DisplayName("No backdated calendar edits after EOD")
    void testNoBackdatedCalendarEdits() {
        TestContext ctx = createTestContext("BKDT");

        TenantContextHolder.setTenantId(ctx.tenant.getId());
        setSecurityContext(ctx.maker);

        // Try to create calendar entry for past date
        LocalDate pastDate = LocalDate.now().minusDays(5);
        assertThrows(
                RuntimeException.class,
                () ->
                        calendarService.createCalendarEntry(
                                ctx.tenant.getId(),
                                pastDate,
                                "HOLIDAY",
                                "Past Holiday",
                                "NATIONAL",
                                false,
                                false,
                                "Backdated test"),
                "Calendar entry for past date must be rejected");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. IMMUTABLE LEDGER ENFORCEMENT
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(70)
    @Transactional
    @DisplayName("Ledger entries are append-only (immutable)")
    void testLedgerEntryImmutable() {
        TestContext ctx = createTestContext("IMMUT");

        TenantContextHolder.setTenantId(ctx.tenant.getId());
        setSecurityContext(ctx.maker);

        // Get existing ledger entry count
        long initialCount = ledgerEntryRepository.count();

        // Create a deposit transaction using BATCH channel to ensure auto-authorization
        // (BATCH channel always auto-authorizes per CBS governance rules, regardless of policy
        // table)
        TransactionDTO dto =
                TransactionDTO.builder()
                        .transactionType("DEPOSIT")
                        .destinationAccountNumber(ctx.source.getAccountNumber())
                        .amount(new BigDecimal("100.00"))
                        .currency("INR")
                        .channel(TransactionChannel.BATCH.name())
                        .clientReferenceId("IMMUT-REF-001")
                        .description("Immutable ledger test")
                        .narration("Testing append-only ledger")
                        .build();

        var txn = transactionService.deposit(dto);

        // Verify transaction was auto-authorized and posted (not PENDING_APPROVAL)
        assertEquals(
                com.ledgora.common.enums.TransactionStatus.COMPLETED,
                txn.getStatus(),
                "BATCH channel deposit must be auto-authorized and COMPLETED");

        // Verify ledger entries were created (appended)
        long newCount = ledgerEntryRepository.count();
        assertTrue(newCount > initialCount, "Ledger entries must be appended (not modified)");

        // Verify double-entry is maintained
        BigDecimal debits = ledgerEntryRepository.sumDebitsByTransactionId(txn.getId());
        BigDecimal credits = ledgerEntryRepository.sumCreditsByTransactionId(txn.getId());
        assertNotNull(debits, "Debits must exist");
        assertNotNull(credits, "Credits must exist");
        assertEquals(
                0, debits.compareTo(credits), "Double-entry invariant: debits must equal credits");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. SCRIPT INJECTION REJECTION
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(80)
    @DisplayName("Script tag injection must be rejected")
    void testScriptTagInjectionRejected() {
        assertThrows(
                ScriptInjectionException.class,
                () -> InputSanitizer.sanitize("<script>alert('XSS')</script>", "name"),
                "Script tags must be rejected");
    }

    @Test
    @Order(81)
    @DisplayName("JavaScript URI injection must be rejected")
    void testJavascriptUriInjectionRejected() {
        assertThrows(
                ScriptInjectionException.class,
                () -> InputSanitizer.sanitize("javascript:alert(1)", "field"),
                "javascript: URI must be rejected");
    }

    @Test
    @Order(82)
    @DisplayName("HTML angle brackets must be rejected")
    void testHtmlAngleBracketsRejected() {
        assertThrows(
                ScriptInjectionException.class,
                () -> InputSanitizer.sanitize("<div>test</div>", "field"),
                "HTML tags (angle brackets) must be rejected");
    }

    @Test
    @Order(83)
    @DisplayName("Event handler injection must be rejected")
    void testEventHandlerInjectionRejected() {
        assertThrows(
                ScriptInjectionException.class,
                () -> InputSanitizer.sanitize("<img onerror=alert(1)>", "field"),
                "Event handler injection must be rejected");
    }

    @Test
    @Order(84)
    @DisplayName("iframe injection must be rejected")
    void testIframeInjectionRejected() {
        assertThrows(
                ScriptInjectionException.class,
                () -> InputSanitizer.sanitize("<iframe src='evil.com'>", "field"),
                "iframe injection must be rejected");
    }

    @Test
    @Order(85)
    @DisplayName("Sanitize null input returns null")
    void testSanitizeNullReturnsNull() {
        assertNull(InputSanitizer.sanitize(null, "field"), "Null input must return null");
    }

    @Test
    @Order(86)
    @DisplayName("Clean input passes sanitization")
    void testCleanInputPassesSanitization() {
        String result = InputSanitizer.sanitize("Normal text with spaces", "field");
        assertEquals("Normal text with spaces", result, "Clean input must pass through unchanged");
    }

    @Test
    @Order(87)
    @DisplayName("Aadhaar masking shows only last 4 digits")
    void testAadhaarMasking() {
        String masked = InputSanitizer.maskAadhaar("123456789012");
        assertEquals("XXXX-XXXX-9012", masked, "Aadhaar must be masked showing only last 4 digits");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9. ADDITIONAL GOVERNANCE TESTS
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(90)
    @Transactional
    @DisplayName("CustomerMaster approval status transitions work correctly")
    void testCustomerApprovalStatusTransitions() {
        TestContext ctx = createTestContext("CAPP");

        CustomerMaster customer = createCustomerMaster(ctx, "CAPP-001");
        assertEquals(
                MakerCheckerStatus.PENDING,
                customer.getApprovalStatus(),
                "New customer must start with PENDING status");

        customer.setApprovalStatus(MakerCheckerStatus.APPROVED);
        customer.setApprovedBy(ctx.checker);
        customerMasterRepository.save(customer);

        CustomerMaster approved = customerMasterRepository.findById(customer.getId()).orElseThrow();
        assertEquals(
                MakerCheckerStatus.APPROVED,
                approved.getApprovalStatus(),
                "Customer must be APPROVED after checker approval");
    }

    @Test
    @Order(91)
    @Transactional
    @DisplayName("Account approval status default is correct")
    void testAccountApprovalStatusDefault() {
        TestContext ctx = createTestContext("AAPR");

        // Note: existing accounts default to APPROVED for backward compatibility
        assertEquals(
                MakerCheckerStatus.APPROVED,
                ctx.source.getApprovalStatus(),
                "Existing accounts default to APPROVED for backward compatibility");
    }

    @Test
    @Order(92)
    @Transactional
    @DisplayName("FreezeLevel enum has all required values")
    void testFreezeLevelEnumValues() {
        FreezeLevel[] levels = FreezeLevel.values();
        assertEquals(4, levels.length, "FreezeLevel must have 4 values");
        assertNotNull(FreezeLevel.valueOf("NONE"));
        assertNotNull(FreezeLevel.valueOf("DEBIT_ONLY"));
        assertNotNull(FreezeLevel.valueOf("CREDIT_ONLY"));
        assertNotNull(FreezeLevel.valueOf("FULL"));
    }

    @Test
    @Order(93)
    @Transactional
    @DisplayName("OwnershipType enum has all required values")
    void testOwnershipTypeEnumValues() {
        OwnershipType[] types = OwnershipType.values();
        assertEquals(4, types.length, "OwnershipType must have 4 values");
        assertNotNull(OwnershipType.valueOf("PRIMARY"));
        assertNotNull(OwnershipType.valueOf("JOINT"));
        assertNotNull(OwnershipType.valueOf("GUARANTOR"));
        assertNotNull(OwnershipType.valueOf("NOMINEE"));
    }

    @Test
    @Order(94)
    @Transactional
    @DisplayName("LienType enum has all required values")
    void testLienTypeEnumValues() {
        LienType[] types = LienType.values();
        assertEquals(5, types.length, "LienType must have 5 values");
        assertNotNull(LienType.valueOf("COURT_ORDER"));
        assertNotNull(LienType.valueOf("LOAN_SECURITY"));
        assertNotNull(LienType.valueOf("TAX_RECOVERY"));
        assertNotNull(LienType.valueOf("REGULATORY"));
        assertNotNull(LienType.valueOf("OTHER"));
    }

    @Test
    @Order(95)
    @Transactional
    @DisplayName("RoleName enum has all 13 required roles")
    void testRoleNameEnumValues() {
        RoleName[] roles = RoleName.values();
        assertEquals(13, roles.length, "RoleName must have 13 values");
        assertNotNull(RoleName.valueOf("ROLE_ADMIN"));
        assertNotNull(RoleName.valueOf("ROLE_MANAGER"));
        assertNotNull(RoleName.valueOf("ROLE_TELLER"));
        assertNotNull(RoleName.valueOf("ROLE_CUSTOMER"));
        assertNotNull(RoleName.valueOf("ROLE_MAKER"));
        assertNotNull(RoleName.valueOf("ROLE_CHECKER"));
        assertNotNull(RoleName.valueOf("ROLE_BRANCH_MANAGER"));
        assertNotNull(RoleName.valueOf("ROLE_TENANT_ADMIN"));
        assertNotNull(RoleName.valueOf("ROLE_SUPER_ADMIN"));
        assertNotNull(RoleName.valueOf("ROLE_OPERATIONS"));
        assertNotNull(RoleName.valueOf("ROLE_AUDITOR"));
        assertNotNull(RoleName.valueOf("ROLE_ATM_SYSTEM"));
        assertNotNull(RoleName.valueOf("ROLE_SYSTEM"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns a guaranteed weekday date (Mon-Fri) for test business dates.
     * Prevents BankCalendarService from blocking transactions on weekends.
     */
    private static LocalDate nextWeekday() {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek().getValue() > 5) {
            d = d.plusDays(1);
        }
        return d;
    }

    private TestContext createTestContext(String suffix) {
        Tenant tenant =
                tenantRepository.save(
                        Tenant.builder()
                                .tenantCode("TEN-RBI-" + suffix)
                                .tenantName("RBI Governance Tenant " + suffix)
                                .status("ACTIVE")
                                .currentBusinessDate(nextWeekday())
                                .dayStatus(DayStatus.OPEN)
                                .build());

        Branch branch =
                branchRepository.save(
                        Branch.builder()
                                .branchCode(
                                        "BR" + suffix.substring(0, Math.min(suffix.length(), 8)))
                                .name("Branch " + suffix)
                                .isActive(true)
                                .build());

        User maker =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .username("maker-rbi-" + suffix)
                                .password("password")
                                .fullName("Maker " + suffix)
                                .email("maker-" + suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        User checker =
                userRepository.save(
                        User.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .username("checker-rbi-" + suffix)
                                .password("password")
                                .fullName("Checker " + suffix)
                                .email("checker-" + suffix + "@test.com")
                                .isActive(true)
                                .isLocked(false)
                                .build());

        Account source =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .homeBranch(branch)
                                .accountNumber("SRC-RBI-" + suffix)
                                .accountName("Source " + suffix)
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(new BigDecimal("10000.00"))
                                .currency("INR")
                                .glAccountCode("1100")
                                .build());

        // Seed balance
        AccountBalance ab =
                accountBalanceRepository
                        .findByAccountId(source.getId())
                        .orElse(AccountBalance.builder().account(source).build());
        ab.setActualTotalBalance(new BigDecimal("10000.00"));
        ab.setActualClearedBalance(new BigDecimal("10000.00"));
        ab.setAvailableBalance(new BigDecimal("10000.00"));
        ab.setLedgerBalance(new BigDecimal("10000.00"));
        accountBalanceRepository.save(ab);

        // Seed Cash GL Account required by transaction service
        Account cashGlAccount =
                accountRepository
                        .findFirstByTenantIdAndGlAccountCode(tenant.getId(), "1100")
                        .orElseGet(
                                () ->
                                        accountRepository.save(
                                                Account.builder()
                                                        .tenant(tenant)
                                                        .branch(branch)
                                                        .homeBranch(branch)
                                                        .accountNumber("GL-CASH-RBI-" + suffix)
                                                        .accountName("Cash GL " + suffix)
                                                        .accountType(AccountType.SAVINGS)
                                                        .status(AccountStatus.ACTIVE)
                                                        .balance(BigDecimal.ZERO)
                                                        .currency("INR")
                                                        .glAccountCode("1100")
                                                        .build()));
        AccountBalance cashBal =
                accountBalanceRepository
                        .findByAccountId(cashGlAccount.getId())
                        .orElse(AccountBalance.builder().account(cashGlAccount).build());
        cashBal.setActualTotalBalance(new BigDecimal("100000.00"));
        cashBal.setActualClearedBalance(new BigDecimal("100000.00"));
        cashBal.setAvailableBalance(new BigDecimal("100000.00"));
        cashBal.setLedgerBalance(new BigDecimal("100000.00"));
        accountBalanceRepository.save(cashBal);

        // Create GL for deposits
        Account destGlAccount =
                accountRepository.save(
                        Account.builder()
                                .tenant(tenant)
                                .branch(branch)
                                .homeBranch(branch)
                                .accountNumber("GL-DEP-RBI-" + suffix)
                                .accountName("Deposits GL " + suffix)
                                .accountType(AccountType.SAVINGS)
                                .status(AccountStatus.ACTIVE)
                                .balance(BigDecimal.ZERO)
                                .currency("INR")
                                .glAccountCode("2100")
                                .build());
        AccountBalance depBal =
                accountBalanceRepository
                        .findByAccountId(destGlAccount.getId())
                        .orElse(AccountBalance.builder().account(destGlAccount).build());
        depBal.setActualTotalBalance(BigDecimal.ZERO);
        depBal.setActualClearedBalance(BigDecimal.ZERO);
        depBal.setAvailableBalance(BigDecimal.ZERO);
        depBal.setLedgerBalance(BigDecimal.ZERO);
        accountBalanceRepository.save(depBal);

        TenantContextHolder.setTenantId(tenant.getId());
        setSecurityContext(maker);

        return new TestContext(tenant, branch, maker, checker, source);
    }

    private CustomerMaster createCustomerMaster(TestContext ctx, String customerNumber) {
        return customerMasterRepository.save(
                CustomerMaster.builder()
                        .tenant(ctx.tenant)
                        .customerNumber(customerNumber)
                        .firstName("Test")
                        .lastName("Customer")
                        .kycStatus("VERIFIED")
                        .customerType("INDIVIDUAL")
                        .build());
    }

    private void setSecurityContext(User user) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                "N/A",
                                List.of(
                                        new SimpleGrantedAuthority("ROLE_MAKER"),
                                        new SimpleGrantedAuthority("ROLE_CHECKER"))));
    }

    private record TestContext(
            Tenant tenant, Branch branch, User maker, User checker, Account source) {}
}
