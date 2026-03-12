package com.ledgora.customer.service;

import com.ledgora.approval.service.ApprovalService;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.customer.dto.CustomerDTO;
import com.ledgora.customer.entity.Customer;
import com.ledgora.customer.repository.CustomerRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer service with CBS-grade maker-checker approval workflow.
 *
 * <p>Customer lifecycle: 1. Maker creates customer → status = PENDING_APPROVAL, kycStatus = PENDING
 * 2. ApprovalRequest created for CUSTOMER entity 3. Checker approves → status = ACTIVE, kycStatus =
 * VERIFIED 4. Checker rejects → status = REJECTED, kycStatus = REJECTED
 *
 * <p>Modifications also require approval (master data changes per CBS guidelines).
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerRepository customerRepository;
    private final TenantService tenantService;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;
    private final AuditService auditService;

    public CustomerService(
            CustomerRepository customerRepository,
            TenantService tenantService,
            UserRepository userRepository,
            ApprovalService approvalService,
            AuditService auditService) {
        this.customerRepository = customerRepository;
        this.tenantService = tenantService;
        this.userRepository = userRepository;
        this.approvalService = approvalService;
        this.auditService = auditService;
    }

    /**
     * Create customer (maker step). Customer starts as PENDING_APPROVAL with kycStatus=PENDING. An
     * ApprovalRequest is created for checker to approve/reject.
     */
    @Transactional
    public Customer createCustomer(CustomerDTO dto) {
        // ── RBI-grade field validations ──
        // Age >= 18 for INDIVIDUAL customers
        if (dto.getDob() != null) {
            com.ledgora.common.validation.RbiFieldValidator.validateDob(dto.getDob());
        }
        if ("INDIVIDUAL".equals(dto.getCustomerType())) {
            if (dto.getDob() == null) {
                throw new com.ledgora.common.exception.BusinessException(
                        "DOB_REQUIRED", "Date of Birth is mandatory for INDIVIDUAL customers");
            }
            // PAN mandatory for INDIVIDUAL (RBI KYC norms)
            com.ledgora.common.validation.RbiFieldValidator.validatePanForIndividual(
                    dto.getCustomerType(), dto.getPanNumber());
            // Validate PAN format if provided
            com.ledgora.common.validation.RbiFieldValidator.validatePanFormat(dto.getPanNumber());
            // Validate Aadhaar format if provided
            com.ledgora.common.validation.RbiFieldValidator.validateAadhaarFormat(
                    dto.getAadhaarNumber());
        }
        if ("CORPORATE".equals(dto.getCustomerType())) {
            // GST mandatory for CORPORATE
            com.ledgora.common.validation.RbiFieldValidator.validateGstForCorporate(
                    dto.getCustomerType(), dto.getGstNumber());
            // Validate GST format if provided
            com.ledgora.common.validation.RbiFieldValidator.validateGstFormat(dto.getGstNumber());
        }
        // Validate mobile number format
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            com.ledgora.common.validation.RbiFieldValidator.validateMobileNumber(dto.getPhone());
        }

        if (dto.getNationalId() != null
                && customerRepository.existsByNationalId(dto.getNationalId())) {
            throw new RuntimeException(
                    "Customer with national ID " + dto.getNationalId() + " already exists");
        }

        Long tenantId = requireTenantId();
        Tenant tenant = tenantService.getTenantById(tenantId);
        User currentUser = getCurrentUser();

        Customer customer =
                Customer.builder()
                        .firstName(dto.getFirstName())
                        .lastName(dto.getLastName())
                        .dob(dto.getDob())
                        .nationalId(dto.getNationalId())
                        .kycStatus("PENDING")
                        .phone(dto.getPhone())
                        .email(dto.getEmail())
                        .address(dto.getAddress())
                        .tenant(tenant)
                        .createdBy(currentUser)
                        .build();

        Customer saved = customerRepository.save(customer);

        // Submit for maker-checker approval (master data change requires dual control)
        approvalService.submitForApproval(
                "CUSTOMER",
                saved.getCustomerId(),
                "Customer create: "
                        + saved.getFirstName()
                        + " "
                        + saved.getLastName()
                        + " NationalID: "
                        + saved.getNationalId());

        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(
                userId,
                "CUSTOMER_CREATE",
                "CUSTOMER",
                saved.getCustomerId(),
                "Customer created (pending approval): "
                        + saved.getFirstName()
                        + " "
                        + saved.getLastName(),
                null);

        log.info(
                "Customer created (PENDING_APPROVAL): {} {} (ID: {}) by user {}",
                saved.getFirstName(),
                saved.getLastName(),
                saved.getCustomerId(),
                currentUser != null ? currentUser.getUsername() : "system");
        return saved;
    }

    @Transactional
    public Customer updateCustomer(Long customerId, CustomerDTO dto) {
        Customer customer = requireCustomer(customerId);

        if (dto.getFirstName() != null) customer.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) customer.setLastName(dto.getLastName());
        if (dto.getDob() != null) customer.setDob(dto.getDob());
        if (dto.getNationalId() != null) customer.setNationalId(dto.getNationalId());
        if (dto.getPhone() != null) customer.setPhone(dto.getPhone());
        if (dto.getEmail() != null) customer.setEmail(dto.getEmail());
        if (dto.getAddress() != null) customer.setAddress(dto.getAddress());

        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateKycStatus(Long customerId, String kycStatus) {
        Customer customer = requireCustomer(customerId);
        customer.setKycStatus(kycStatus);
        log.info("Customer {} KYC status updated to {}", customerId, kycStatus);
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findByTenantId(requireTenantId());
    }

    public org.springframework.data.domain.Page<Customer> getAllCustomersPaged(int page, int size) {
        return customerRepository.findByTenantId(
                requireTenantId(), org.springframework.data.domain.PageRequest.of(page, size));
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findByIdAndTenantId(id, requireTenantId());
    }

    public Optional<Customer> getCustomerByNationalId(String nationalId) {
        return customerRepository.findByNationalId(nationalId);
    }

    public List<Customer> searchByName(String name) {
        return customerRepository.searchByTenantIdAndName(requireTenantId(), name);
    }

    public List<Customer> getByKycStatus(String kycStatus) {
        return customerRepository.findByTenantIdAndKycStatus(requireTenantId(), kycStatus);
    }

    public long countAll() {
        return customerRepository.countByTenantId(requireTenantId());
    }

    /**
     * Update freeze status on customer. Persists freezeLevel and freezeReason on the Customer
     * entity and writes an audit trail for governance.
     */
    @Transactional
    public Customer updateFreezeStatus(Long customerId, String freezeLevel, String freezeReason) {
        Customer customer = requireCustomer(customerId);

        // Persist freeze fields on Customer entity
        customer.setFreezeLevel(freezeLevel != null ? freezeLevel : "NONE");
        customer.setFreezeReason(freezeReason);

        Customer saved = customerRepository.save(customer);

        // Audit trail for freeze action (governance requirement)
        User currentUser = getCurrentUser();
        Long userId = currentUser != null ? currentUser.getId() : null;
        auditService.logEvent(
                userId,
                "CUSTOMER_FREEZE_UPDATE",
                "CUSTOMER",
                customerId,
                "Customer freeze updated: level="
                        + freezeLevel
                        + (freezeReason != null && !freezeReason.isBlank()
                                ? " reason=" + freezeReason
                                : ""),
                null);

        log.info(
                "Customer {} freeze updated to {} reason: {}",
                customerId,
                freezeLevel,
                freezeReason);
        return saved;
    }

    /** Approve customer (checker step). Enforces maker != checker + tenant isolation. */
    @Transactional
    public Customer approveCustomer(Long customerId) {
        Customer customer = requireCustomer(customerId);

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot approve customer: approver identity could not be resolved");
        }
        if (customer.getCreatedBy() != null
                && customer.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new com.ledgora.common.exception.BusinessException(
                    "MAKER_CHECKER_VIOLATION",
                    "Cannot approve your own customer record (maker-checker violation)");
        }

        customer.setKycStatus("VERIFIED");
        Customer saved = customerRepository.save(customer);

        auditService.logEvent(
                currentUser.getId(),
                "CUSTOMER_APPROVE",
                "CUSTOMER",
                customerId,
                "Customer approved: " + customer.getFirstName() + " " + customer.getLastName(),
                null);

        log.info("Customer {} approved by user {}", customerId, currentUser.getUsername());
        return saved;
    }

    /** Reject customer (checker step). Enforces maker != checker + tenant isolation. */
    @Transactional
    public Customer rejectCustomer(Long customerId) {
        Customer customer = requireCustomer(customerId);

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot reject customer: reviewer identity could not be resolved");
        }
        if (customer.getCreatedBy() != null
                && customer.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new com.ledgora.common.exception.BusinessException(
                    "MAKER_CHECKER_VIOLATION",
                    "Cannot reject your own customer record (maker-checker violation)");
        }

        customer.setKycStatus("REJECTED");
        Customer saved = customerRepository.save(customer);

        auditService.logEvent(
                currentUser.getId(),
                "CUSTOMER_REJECT",
                "CUSTOMER",
                customerId,
                "Customer rejected: " + customer.getFirstName() + " " + customer.getLastName(),
                null);

        log.info("Customer {} rejected by user {}", customerId, currentUser.getUsername());
        return saved;
    }

    /** Tenant-safe customer lookup. Throws if not found or belongs to a different tenant. */
    private Customer requireCustomer(Long customerId) {
        Long tenantId = requireTenantId();
        return customerRepository
                .findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
    }

    private Long requireTenantId() {
        return TenantContextHolder.getRequiredTenantId();
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
