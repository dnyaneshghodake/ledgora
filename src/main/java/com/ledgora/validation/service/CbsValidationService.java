package com.ledgora.validation.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.batch.service.BatchService;
import com.ledgora.calendar.service.BankCalendarService;
import com.ledgora.common.enums.AccountStatus;
import com.ledgora.common.enums.FreezeLevel;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.common.enums.TransactionChannel;
import com.ledgora.common.enums.VoucherDrCr;
import com.ledgora.common.exception.BusinessException;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Centralized CBS Validation Service.
 * Provides universal pre-checks for all CBS operations:
 * - Business day is OPEN
 * - Batch is available for the channel/date
 * - User has required role
 * - Account freeze level allows the operation
 * - Tenant context is set
 * - GL account exists and is active
 * - Holiday calendar allows the operation
 *
 * Returns a list of validation errors (empty = all passed).
 * Also provides throwing variants for use in transactional flows.
 */
@Service
public class CbsValidationService {

    private static final Logger log = LoggerFactory.getLogger(CbsValidationService.class);

    private final TenantService tenantService;
    private final BankCalendarService bankCalendarService;
    private final BatchService batchService;
    private final AccountRepository accountRepository;
    private final GeneralLedgerRepository glRepository;

    public CbsValidationService(TenantService tenantService,
                                 BankCalendarService bankCalendarService,
                                 BatchService batchService,
                                 AccountRepository accountRepository,
                                 GeneralLedgerRepository glRepository) {
        this.tenantService = tenantService;
        this.bankCalendarService = bankCalendarService;
        this.batchService = batchService;
        this.accountRepository = accountRepository;
        this.glRepository = glRepository;
    }

    // ===== Composite validation =====

    /**
     * Run all standard pre-transaction validations.
     * Returns empty list if all checks pass.
     */
    public List<String> validatePreTransaction(Long tenantId, Account account,
                                                VoucherDrCr drCr, TransactionChannel channel) {
        List<String> errors = new ArrayList<>();
        errors.addAll(validateTenantContext());
        errors.addAll(validateBusinessDayOpen(tenantId));

        LocalDate businessDate = tenantService.getCurrentBusinessDate(tenantId);
        errors.addAll(validateHolidayCalendar(tenantId, businessDate, channel));
        errors.addAll(validateAccountActive(account));
        errors.addAll(validateAccountApproved(account));
        errors.addAll(validateAccountFreezeLevel(account, drCr));
        return errors;
    }

    /**
     * Throwing variant: throws BusinessException if any pre-transaction check fails.
     */
    public void requirePreTransaction(Long tenantId, Account account,
                                       VoucherDrCr drCr, TransactionChannel channel) {
        List<String> errors = validatePreTransaction(tenantId, account, drCr, channel);
        if (!errors.isEmpty()) {
            throw new BusinessException("PRE_TRANSACTION_VALIDATION_FAILED",
                    String.join("; ", errors));
        }
    }

    // ===== Individual validations =====

    /**
     * Validate that tenant context is set.
     */
    public List<String> validateTenantContext() {
        List<String> errors = new ArrayList<>();
        try {
            TenantContextHolder.getRequiredTenantId();
        } catch (IllegalStateException e) {
            errors.add("Tenant context is not set. Cannot proceed.");
        }
        return errors;
    }

    /**
     * Validate that the tenant's business day is OPEN.
     */
    public List<String> validateBusinessDayOpen(Long tenantId) {
        List<String> errors = new ArrayList<>();
        try {
            tenantService.validateBusinessDayOpen(tenantId);
        } catch (Exception e) {
            errors.add("Business day is not OPEN for tenant " + tenantId + ": " + e.getMessage());
        }
        return errors;
    }

    /**
     * Throwing variant for business day check.
     */
    public void requireBusinessDayOpen(Long tenantId) {
        tenantService.validateBusinessDayOpen(tenantId);
    }

    /**
     * Validate that the holiday calendar allows operations on the given date/channel.
     */
    public List<String> validateHolidayCalendar(Long tenantId, LocalDate businessDate,
                                                 TransactionChannel channel) {
        List<String> errors = new ArrayList<>();
        try {
            if (channel == null) {
                channel = TransactionChannel.TELLER;
            }
            bankCalendarService.validateTransactionAllowed(tenantId, businessDate, channel);
        } catch (Exception e) {
            errors.add("Holiday calendar blocks this operation: " + e.getMessage());
        }
        return errors;
    }

    /**
     * Validate that an open batch exists for the tenant/channel/date.
     */
    public List<String> validateBatchAvailable(Long tenantId, TransactionChannel channel,
                                                LocalDate businessDate) {
        List<String> errors = new ArrayList<>();
        try {
            batchService.getOrCreateOpenBatch(tenantId, channel, businessDate);
        } catch (Exception e) {
            errors.add("No open batch available: " + e.getMessage());
        }
        return errors;
    }

    /**
     * Validate that the account is ACTIVE.
     */
    public List<String> validateAccountActive(Account account) {
        List<String> errors = new ArrayList<>();
        if (account == null) {
            errors.add("Account is null");
            return errors;
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            errors.add("Account " + account.getAccountNumber()
                    + " is not active. Status: " + account.getStatus());
        }
        return errors;
    }

    /**
     * Validate that the account is approved (maker-checker).
     */
    public List<String> validateAccountApproved(Account account) {
        List<String> errors = new ArrayList<>();
        if (account == null) {
            errors.add("Account is null");
            return errors;
        }
        if (account.getApprovalStatus() != null
                && account.getApprovalStatus() != MakerCheckerStatus.APPROVED) {
            errors.add("Account " + account.getAccountNumber()
                    + " is not approved. Approval status: " + account.getApprovalStatus());
        }
        return errors;
    }

    /**
     * Validate account freeze level allows the requested operation direction.
     */
    public List<String> validateAccountFreezeLevel(Account account, VoucherDrCr drCr) {
        List<String> errors = new ArrayList<>();
        if (account == null) {
            errors.add("Account is null");
            return errors;
        }
        FreezeLevel freezeLevel = account.getFreezeLevel();
        if (freezeLevel == null || freezeLevel == FreezeLevel.NONE) {
            return errors;
        }
        if (freezeLevel == FreezeLevel.FULL) {
            errors.add("Account " + account.getAccountNumber()
                    + " is fully frozen. Reason: " + account.getFreezeReason());
        }
        if (freezeLevel == FreezeLevel.DEBIT_ONLY && drCr == VoucherDrCr.DR) {
            errors.add("Account " + account.getAccountNumber()
                    + " has debit freeze active. Reason: " + account.getFreezeReason());
        }
        if (freezeLevel == FreezeLevel.CREDIT_ONLY && drCr == VoucherDrCr.CR) {
            errors.add("Account " + account.getAccountNumber()
                    + " has credit freeze active. Reason: " + account.getFreezeReason());
        }
        return errors;
    }

    /**
     * Validate that the current user has at least one of the required roles.
     */
    public List<String> validateUserRole(String... requiredRoles) {
        List<String> errors = new ArrayList<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            errors.add("User is not authenticated");
            return errors;
        }
        boolean hasRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> Arrays.asList(requiredRoles).contains(authority));
        if (!hasRole) {
            errors.add("User " + auth.getName() + " does not have any of the required roles: "
                    + String.join(", ", requiredRoles));
        }
        return errors;
    }

    /**
     * Throwing variant for role check.
     */
    public void requireUserRole(String... requiredRoles) {
        List<String> errors = validateUserRole(requiredRoles);
        if (!errors.isEmpty()) {
            throw new BusinessException("INSUFFICIENT_ROLE",
                    String.join("; ", errors));
        }
    }

    /**
     * Validate that a GL account code exists and is active.
     */
    public List<String> validateGlAccountActive(String glAccountCode) {
        List<String> errors = new ArrayList<>();
        if (glAccountCode == null || glAccountCode.isBlank()) {
            errors.add("GL account code is required");
            return errors;
        }
        GeneralLedger gl = glRepository.findByGlCode(glAccountCode).orElse(null);
        if (gl == null) {
            errors.add("GL account not found: " + glAccountCode);
        } else if (!Boolean.TRUE.equals(gl.getIsActive())) {
            errors.add("GL account " + glAccountCode + " is not active");
        }
        return errors;
    }

    /**
     * Validate that the maker and checker are different users (maker-checker enforcement).
     */
    public List<String> validateMakerCheckerDifferent(Long makerId, Long checkerId) {
        List<String> errors = new ArrayList<>();
        if (makerId != null && checkerId != null && makerId.equals(checkerId)) {
            errors.add("Maker and checker must be different users (maker-checker violation)");
        }
        return errors;
    }

    /**
     * Throwing variant for maker-checker check.
     */
    public void requireMakerCheckerDifferent(Long makerId, Long checkerId) {
        List<String> errors = validateMakerCheckerDifferent(makerId, checkerId);
        if (!errors.isEmpty()) {
            throw new BusinessException("MAKER_CHECKER_VIOLATION",
                    String.join("; ", errors));
        }
    }
}
