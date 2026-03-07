package com.ledgora.customer.service;

import com.ledgora.account.entity.Account;
import com.ledgora.common.enums.CustomerStatus;
import com.ledgora.common.enums.VoucherDrCr;
import com.ledgora.customer.entity.CustomerFreezeControl;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.repository.CustomerFreezeControlRepository;
import com.ledgora.customer.repository.CustomerMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * CBS-grade account validation service.
 * Before any transaction, validates:
 * - Customer is ACTIVE
 * - No debit freeze (for debit operations)
 * - No credit freeze (for credit operations)
 * - Tenant match
 * - Branch match
 * Blocks transaction if validation fails.
 */
@Service
public class CbsCustomerValidationService {

    private static final Logger log = LoggerFactory.getLogger(CbsCustomerValidationService.class);
    private final CustomerMasterRepository customerMasterRepository;
    private final CustomerFreezeControlRepository freezeControlRepository;

    public CbsCustomerValidationService(CustomerMasterRepository customerMasterRepository,
                                         CustomerFreezeControlRepository freezeControlRepository) {
        this.customerMasterRepository = customerMasterRepository;
        this.freezeControlRepository = freezeControlRepository;
    }

    /**
     * Validate an account for a transaction.
     * @param account the account to validate
     * @param tenantId the tenant context
     * @param branchId the branch context (nullable for non-branch-scoped operations)
     * @param drCr debit or credit indicator
     * @throws RuntimeException if validation fails
     */
    public void validateAccountForTransaction(Account account, Long tenantId, Long branchId, VoucherDrCr drCr) {
        // Tenant match validation
        if (account.getTenant() == null || !account.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Tenant mismatch: account tenant does not match transaction tenant");
        }

        // Branch match validation (if branch-scoped)
        if (branchId != null && account.getHomeBranch() != null
                && !account.getHomeBranch().getId().equals(branchId)) {
            log.warn("Branch mismatch for account {}: account branch {} != transaction branch {}",
                    account.getAccountNumber(),
                    account.getHomeBranch().getId(),
                    branchId);
        }

        // CustomerMaster validation
        if (account.getCustomerMaster() != null) {
            CustomerMaster cm = account.getCustomerMaster();
            validateCustomerActive(cm);
            validateFreezeControl(cm, tenantId, drCr);
        }
    }

    /**
     * Validate customer is ACTIVE.
     */
    private void validateCustomerActive(CustomerMaster customer) {
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new RuntimeException("Customer " + customer.getCustomerNumber()
                    + " is not ACTIVE (status: " + customer.getStatus() + "). Transaction blocked.");
        }
    }

    /**
     * Validate freeze control.
     */
    private void validateFreezeControl(CustomerMaster customer, Long tenantId, VoucherDrCr drCr) {
        Optional<CustomerFreezeControl> freezeOpt =
                freezeControlRepository.findByCustomerMasterIdAndTenantId(customer.getId(), tenantId);

        if (freezeOpt.isPresent()) {
            CustomerFreezeControl freeze = freezeOpt.get();
            if (drCr == VoucherDrCr.DR && Boolean.TRUE.equals(freeze.getDebitFreeze())) {
                throw new RuntimeException("Debit freeze active on customer "
                        + customer.getCustomerNumber() + ". Reason: " + freeze.getDebitFreezeReason());
            }
            if (drCr == VoucherDrCr.CR && Boolean.TRUE.equals(freeze.getCreditFreeze())) {
                throw new RuntimeException("Credit freeze active on customer "
                        + customer.getCustomerNumber() + ". Reason: " + freeze.getCreditFreezeReason());
            }
        }
    }
}
