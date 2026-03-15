package com.ledgora.loan.service;

import com.ledgora.common.exception.BusinessException;
import com.ledgora.loan.entity.LoanAccount;
import com.ledgora.loan.entity.LoanSchedule;
import com.ledgora.loan.entity.RepaymentTransaction;
import com.ledgora.loan.repository.LoanAccountRepository;
import com.ledgora.loan.repository.LoanScheduleRepository;
import com.ledgora.loan.repository.RepaymentTransactionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Loan Account Service — CBS-grade loan account inquiry and lifecycle.
 *
 * <p>Read-only account operations (no financial mutations — those go through
 * dedicated services like LoanDisbursementService, LoanEmiPaymentService):
 *
 * <ul>
 *   <li>Account lookup with tenant isolation
 *   <li>Schedule retrieval (Finacle LACHST)
 *   <li>Repayment history retrieval
 *   <li>Portfolio queries
 * </ul>
 */
@Service
public class LoanAccountService {

    private final LoanAccountRepository loanAccountRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final RepaymentTransactionRepository repaymentTransactionRepository;

    public LoanAccountService(
            LoanAccountRepository loanAccountRepository,
            LoanScheduleRepository loanScheduleRepository,
            RepaymentTransactionRepository repaymentTransactionRepository) {
        this.loanAccountRepository = loanAccountRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.repaymentTransactionRepository = repaymentTransactionRepository;
    }

    /** Get loan by ID with eager-fetched product and linked account (for UI detail view). */
    public Optional<LoanAccount> getLoanWithDetails(Long loanId) {
        return loanAccountRepository.findByIdWithProductAndAccount(loanId);
    }

    /** Get loan by ID with tenant isolation. */
    public LoanAccount getLoan(Long loanId, Long tenantId) {
        LoanAccount loan =
                loanAccountRepository
                        .findByIdWithProductAndAccount(loanId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "LOAN_NOT_FOUND",
                                                "Loan account not found: " + loanId));
        if (!loan.getTenant().getId().equals(tenantId)) {
            throw new BusinessException("LOAN_NOT_FOUND", "Loan account not found: " + loanId);
        }
        return loan;
    }

    /** Get repayment schedule for a loan (Finacle LACHST). */
    public List<LoanSchedule> getSchedule(Long loanId) {
        return loanScheduleRepository.findByLoanAccountIdOrderByInstallmentNumberAsc(loanId);
    }

    /** Get repayment transaction history for a loan. */
    public List<RepaymentTransaction> getRepaymentHistory(Long loanId) {
        return repaymentTransactionRepository.findByLoanAccountIdOrderByPaymentDateDesc(loanId);
    }

    /** Get all active + NPA loans for a tenant (portfolio view). */
    public List<LoanAccount> getPortfolio(Long tenantId) {
        return loanAccountRepository.findActiveAndNpaByTenantId(tenantId);
    }

    /** Get loan by account number. */
    public Optional<LoanAccount> getLoanByAccountNumber(String accountNumber) {
        return loanAccountRepository.findByLoanAccountNumber(accountNumber);
    }
}
