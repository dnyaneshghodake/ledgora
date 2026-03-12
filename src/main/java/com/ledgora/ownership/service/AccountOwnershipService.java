package com.ledgora.ownership.service;

import com.ledgora.account.entity.Account;
import com.ledgora.account.repository.AccountRepository;
import com.ledgora.approval.service.ApprovalService;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.common.enums.AccountType;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.common.enums.OwnershipType;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.repository.CustomerMasterRepository;
import com.ledgora.ownership.entity.AccountOwnership;
import com.ledgora.ownership.repository.AccountOwnershipRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing Customer-Account ownership relationships. Enforces ownership rules per RBI
 * guidelines.
 */
@Service
public class AccountOwnershipService {

    private static final Logger log = LoggerFactory.getLogger(AccountOwnershipService.class);
    private final AccountOwnershipRepository ownershipRepository;
    private final AccountRepository accountRepository;
    private final CustomerMasterRepository customerMasterRepository;
    private final TenantService tenantService;
    private final ApprovalService approvalService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public AccountOwnershipService(
            AccountOwnershipRepository ownershipRepository,
            AccountRepository accountRepository,
            CustomerMasterRepository customerMasterRepository,
            TenantService tenantService,
            ApprovalService approvalService,
            AuditService auditService,
            UserRepository userRepository) {
        this.ownershipRepository = ownershipRepository;
        this.accountRepository = accountRepository;
        this.customerMasterRepository = customerMasterRepository;
        this.tenantService = tenantService;
        this.approvalService = approvalService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /** Create ownership link (maker step). Requires approval. */
    @Transactional
    public AccountOwnership createOwnership(
            Long tenantId,
            Long accountId,
            Long customerMasterId,
            OwnershipType ownershipType,
            BigDecimal ownershipPercentage,
            boolean isOperational) {
        Account account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        CustomerMaster customer =
                customerMasterRepository
                        .findById(customerMasterId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Customer not found: " + customerMasterId));

        // No cross-tenant linking
        if (!account.getTenant().getId().equals(tenantId)
                || !customer.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Cross-tenant account-customer linking is not allowed");
        }

        // GL_ACCOUNT & INTERNAL_ACCOUNT cannot link to customers
        if (account.getAccountType() == AccountType.GL_ACCOUNT
                || account.getAccountType() == AccountType.INTERNAL_ACCOUNT) {
            throw new RuntimeException(
                    "GL_ACCOUNT and INTERNAL_ACCOUNT cannot be linked to customers");
        }

        // SAVINGS/CURRENT require at least one PRIMARY owner
        if (ownershipType != OwnershipType.PRIMARY) {
            AccountType acctType = account.getAccountType();
            if (acctType == AccountType.SAVINGS || acctType == AccountType.CURRENT) {
                List<AccountOwnership> primaries =
                        ownershipRepository.findApprovedByAccountIdAndOwnershipType(
                                accountId, OwnershipType.PRIMARY);
                if (primaries.isEmpty()) {
                    throw new RuntimeException(
                            "SAVINGS/CURRENT accounts require a PRIMARY owner before adding other ownership types");
                }
            }
        }

        // Validate ownership percentage
        if (ownershipPercentage.compareTo(BigDecimal.ZERO) <= 0
                || ownershipPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new RuntimeException("Ownership percentage must be between 0 and 100");
        }

        // Check total ownership doesn't exceed 100%
        BigDecimal currentTotal =
                ownershipRepository.sumApprovedOwnershipPercentageByAccountId(accountId);
        if (currentTotal.add(ownershipPercentage).compareTo(new BigDecimal("100")) > 0) {
            throw new RuntimeException(
                    "Total ownership percentage would exceed 100%. Current: " + currentTotal);
        }

        Tenant tenant = tenantService.getTenantById(tenantId);
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot create ownership: maker identity could not be resolved");
        }

        AccountOwnership ownership =
                AccountOwnership.builder()
                        .tenant(tenant)
                        .account(account)
                        .customerMaster(customer)
                        .ownershipType(ownershipType)
                        .ownershipPercentage(ownershipPercentage)
                        .isOperational(isOperational)
                        .approvalStatus(MakerCheckerStatus.PENDING)
                        .createdBy(currentUser)
                        .build();

        AccountOwnership saved = ownershipRepository.save(ownership);

        approvalService.submitForApproval(
                "ACCOUNT_OWNERSHIP",
                saved.getId(),
                "Ownership: account="
                        + account.getAccountNumber()
                        + " customer="
                        + customer.getCustomerNumber()
                        + " type="
                        + ownershipType
                        + " pct="
                        + ownershipPercentage);

        auditService.logEvent(
                currentUser.getId(),
                "OWNERSHIP_CREATE",
                "ACCOUNT_OWNERSHIP",
                saved.getId(),
                "Ownership created for account " + account.getAccountNumber(),
                null);

        log.info(
                "Ownership created (PENDING): account={}, customer={}, type={}",
                account.getAccountNumber(),
                customer.getCustomerNumber(),
                ownershipType);
        return saved;
    }

    @Transactional
    public AccountOwnership approveOwnership(Long ownershipId) {
        AccountOwnership ownership =
                ownershipRepository
                        .findById(ownershipId)
                        .orElseThrow(
                                () -> new RuntimeException("Ownership not found: " + ownershipId));

        if (ownership.getApprovalStatus() != MakerCheckerStatus.PENDING) {
            throw new RuntimeException("Ownership is not pending approval");
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot approve ownership: approver identity could not be resolved");
        }
        if (ownership.getCreatedBy() != null
                && ownership.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException(
                    "Cannot approve your own ownership request (maker-checker violation)");
        }

        ownership.setApprovalStatus(MakerCheckerStatus.APPROVED);
        ownership.setApprovedBy(currentUser);
        AccountOwnership saved = ownershipRepository.save(ownership);

        auditService.logEvent(
                currentUser.getId(),
                "OWNERSHIP_APPROVE",
                "ACCOUNT_OWNERSHIP",
                saved.getId(),
                "Ownership approved for account "
                        + saved.getAccount().getAccountNumber()
                        + " by "
                        + currentUser.getUsername(),
                null);
        return saved;
    }

    @Transactional
    public AccountOwnership rejectOwnership(Long ownershipId) {
        AccountOwnership ownership =
                ownershipRepository
                        .findById(ownershipId)
                        .orElseThrow(
                                () -> new RuntimeException("Ownership not found: " + ownershipId));

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot reject ownership: reviewer identity could not be resolved");
        }

        ownership.setApprovalStatus(MakerCheckerStatus.REJECTED);
        AccountOwnership saved = ownershipRepository.save(ownership);

        auditService.logEvent(
                currentUser.getId(),
                "OWNERSHIP_REJECT",
                "ACCOUNT_OWNERSHIP",
                saved.getId(),
                "Ownership rejected by " + currentUser.getUsername(),
                null);
        return saved;
    }

    public List<AccountOwnership> getOwnershipsByAccount(Long accountId, Long tenantId) {
        return ownershipRepository.findApprovedByAccountIdAndTenantId(accountId, tenantId);
    }

    public List<AccountOwnership> getOwnershipsByCustomer(Long customerMasterId, Long tenantId) {
        return ownershipRepository.findApprovedByCustomerMasterIdAndTenantId(
                customerMasterId, tenantId);
    }

    public List<AccountOwnership> getPendingOwnerships(Long tenantId) {
        return ownershipRepository.findByTenantIdAndApprovalStatus(
                tenantId, MakerCheckerStatus.PENDING);
    }

    private User getCurrentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
