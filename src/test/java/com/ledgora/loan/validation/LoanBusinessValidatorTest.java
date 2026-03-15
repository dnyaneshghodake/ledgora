package com.ledgora.loan.validation;

import static org.junit.jupiter.api.Assertions.*;

import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.enums.LoanStatus;
import com.ledgora.tenant.entity.Tenant;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Loan Business Validator Test — validates CBS-grade pre-condition checks.
 *
 * <p>Tests null checks, negative amounts, zero-total payments, boundary conditions, status gates,
 * and tenant isolation.
 */
class LoanBusinessValidatorTest {

    private LoanAccount buildLoan(
            BigDecimal outstanding, BigDecimal accrued, LoanStatus status, Long tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        return LoanAccount.builder()
                .id(1L)
                .tenant(tenant)
                .outstandingPrincipal(outstanding)
                .accruedInterest(accrued)
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("validatePrincipal")
    class ValidatePrincipal {

        @Test
        void nullPrincipal_throws() {
            assertThrows(
                    BusinessException.class, () -> LoanBusinessValidator.validatePrincipal(null));
        }

        @Test
        void zeroPrincipal_throws() {
            assertThrows(
                    BusinessException.class,
                    () -> LoanBusinessValidator.validatePrincipal(BigDecimal.ZERO));
        }

        @Test
        void negativePrincipal_throws() {
            assertThrows(
                    BusinessException.class,
                    () -> LoanBusinessValidator.validatePrincipal(new BigDecimal("-1")));
        }

        @Test
        void positivePrincipal_passes() {
            assertDoesNotThrow(
                    () -> LoanBusinessValidator.validatePrincipal(new BigDecimal("100000")));
        }
    }

    @Nested
    @DisplayName("validateLoanOperational")
    class ValidateLoanOperational {

        @Test
        void closedLoan_throws() {
            LoanAccount loan = buildLoan(BigDecimal.ZERO, BigDecimal.ZERO, LoanStatus.CLOSED, 1L);
            BusinessException ex =
                    assertThrows(
                            BusinessException.class,
                            () -> LoanBusinessValidator.validateLoanOperational(loan));
            assertEquals("LOAN_CLOSED", ex.getErrorCode());
        }

        @Test
        void writtenOffLoan_throws() {
            LoanAccount loan =
                    buildLoan(BigDecimal.ZERO, BigDecimal.ZERO, LoanStatus.WRITTEN_OFF, 1L);
            BusinessException ex =
                    assertThrows(
                            BusinessException.class,
                            () -> LoanBusinessValidator.validateLoanOperational(loan));
            assertEquals("LOAN_WRITTEN_OFF", ex.getErrorCode());
        }

        @Test
        void activeLoan_passes() {
            LoanAccount loan =
                    buildLoan(
                            new BigDecimal("100000"), new BigDecimal("500"), LoanStatus.ACTIVE, 1L);
            assertDoesNotThrow(() -> LoanBusinessValidator.validateLoanOperational(loan));
        }

        @Test
        void npaLoan_passes() {
            LoanAccount loan =
                    buildLoan(new BigDecimal("100000"), new BigDecimal("500"), LoanStatus.NPA, 1L);
            assertDoesNotThrow(() -> LoanBusinessValidator.validateLoanOperational(loan));
        }
    }

    @Nested
    @DisplayName("validateEmiPayment")
    class ValidateEmiPayment {

        private final LoanAccount loan =
                buildLoan(new BigDecimal("100000"), new BigDecimal("5000"), LoanStatus.ACTIVE, 1L);

        @Test
        void nullPrincipal_throws() {
            assertThrows(
                    BusinessException.class,
                    () ->
                            LoanBusinessValidator.validateEmiPayment(
                                    null, new BigDecimal("1000"), loan));
        }

        @Test
        void nullInterest_throws() {
            assertThrows(
                    BusinessException.class,
                    () ->
                            LoanBusinessValidator.validateEmiPayment(
                                    new BigDecimal("1000"), null, loan));
        }

        @Test
        void negativePrincipal_throws() {
            assertThrows(
                    BusinessException.class,
                    () ->
                            LoanBusinessValidator.validateEmiPayment(
                                    new BigDecimal("-1"), new BigDecimal("1000"), loan));
        }

        @Test
        void zeroTotal_throws() {
            BusinessException ex =
                    assertThrows(
                            BusinessException.class,
                            () ->
                                    LoanBusinessValidator.validateEmiPayment(
                                            BigDecimal.ZERO, BigDecimal.ZERO, loan));
            assertEquals("INVALID_EMI_PARAMS", ex.getErrorCode());
        }

        @Test
        void interestExceedsAccrued_throws() {
            BusinessException ex =
                    assertThrows(
                            BusinessException.class,
                            () ->
                                    LoanBusinessValidator.validateEmiPayment(
                                            new BigDecimal("1000"), new BigDecimal("6000"), loan));
            assertEquals("INTEREST_EXCEEDS_ACCRUED", ex.getErrorCode());
        }

        @Test
        void principalExceedsOutstanding_throws() {
            BusinessException ex =
                    assertThrows(
                            BusinessException.class,
                            () ->
                                    LoanBusinessValidator.validateEmiPayment(
                                            new BigDecimal("200000"),
                                            new BigDecimal("1000"),
                                            loan));
            assertEquals("PRINCIPAL_EXCEEDS_OUTSTANDING", ex.getErrorCode());
        }

        @Test
        void validPayment_passes() {
            assertDoesNotThrow(
                    () ->
                            LoanBusinessValidator.validateEmiPayment(
                                    new BigDecimal("10000"), new BigDecimal("2000"), loan));
        }

        @Test
        void exactOutstandingPayment_passes() {
            assertDoesNotThrow(
                    () ->
                            LoanBusinessValidator.validateEmiPayment(
                                    new BigDecimal("100000"), new BigDecimal("5000"), loan));
        }
    }

    @Nested
    @DisplayName("validateTenantOwnership")
    class ValidateTenantOwnership {

        @Test
        void matchingTenant_passes() {
            LoanAccount loan =
                    buildLoan(new BigDecimal("100000"), BigDecimal.ZERO, LoanStatus.ACTIVE, 1L);
            assertDoesNotThrow(() -> LoanBusinessValidator.validateTenantOwnership(loan, 1L));
        }

        @Test
        void mismatchedTenant_throws() {
            LoanAccount loan =
                    buildLoan(new BigDecimal("100000"), BigDecimal.ZERO, LoanStatus.ACTIVE, 1L);
            assertThrows(
                    BusinessException.class,
                    () -> LoanBusinessValidator.validateTenantOwnership(loan, 2L));
        }

        @Test
        void nullTenantId_passes() {
            LoanAccount loan =
                    buildLoan(new BigDecimal("100000"), BigDecimal.ZERO, LoanStatus.ACTIVE, 1L);
            assertDoesNotThrow(() -> LoanBusinessValidator.validateTenantOwnership(loan, null));
        }
    }
}
