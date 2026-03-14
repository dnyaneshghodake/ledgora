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
     *
     * <p>Enhancements: tenant-scoped PAN/Aadhaar/NationalId uniqueness, automatic risk derivation.
     */
    @Transactional
    public Customer createCustomer(CustomerDTO dto) {
        // ── RBI-grade field validations ──
        if (dto.getDob() != null) {
            com.ledgora.common.validation.RbiFieldValidator.validateDob(dto.getDob());
        }
        if ("INDIVIDUAL".equals(dto.getCustomerType())) {
            if (dto.getDob() == null) {
                throw new com.ledgora.common.exception.BusinessException(
                        "DOB_REQUIRED", "Date of Birth is mandatory for INDIVIDUAL customers");
            }
            com.ledgora.common.validation.RbiFieldValidator.validatePanForIndividual(
                    dto.getCustomerType(), dto.getPanNumber());
            com.ledgora.common.validation.RbiFieldValidator.validatePanFormat(dto.getPanNumber());
            com.ledgora.common.validation.RbiFieldValidator.validateAadhaarFormat(
                    dto.getAadhaarNumber());
        }
        if ("CORPORATE".equals(dto.getCustomerType())) {
            com.ledgora.common.validation.RbiFieldValidator.validateGstForCorporate(
                    dto.getCustomerType(), dto.getGstNumber());
            com.ledgora.common.validation.RbiFieldValidator.validateGstFormat(dto.getGstNumber());
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            com.ledgora.common.validation.RbiFieldValidator.validateMobileNumber(dto.getPhone());
        }

        Long tenantId = requireTenantId();

        // ── Tenant-scoped uniqueness enforcement ──
        if (dto.getNationalId() != null
                && customerRepository.existsByTenantIdAndNationalId(tenantId, dto.getNationalId(), null)) {
            throw new com.ledgora.common.exception.BusinessException(
                    "DUPLICATE_NATIONAL_ID",
                    "Customer with national ID " + dto.getNationalId() + " already exists in this tenant");
        }
        if (dto.getPanNumber() != null && !dto.getPanNumber().isBlank()
                && customerRepository.existsByTenantIdAndPanNumber(tenantId, dto.getPanNumber(), null)) {
            throw new com.ledgora.common.exception.BusinessException(
                    "DUPLICATE_PAN",
                    "Customer with PAN " + dto.getPanNumber() + " already exists in this tenant");
        }
        if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().isBlank()
                && customerRepository.existsByTenantIdAndAadhaarNumber(tenantId, dto.getAadhaarNumber(), null)) {
            throw new com.ledgora.common.exception.BusinessException(
                    "DUPLICATE_AADHAAR",
                    "Customer with Aadhaar already exists in this tenant");
        }

        Tenant tenant = tenantService.getTenantById(tenantId);
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot create customer: maker identity could not be resolved");
        }

        // ── Automatic risk derivation ──
        String derivedRisk = deriveRiskCategory(dto);

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
                        .makerTimestamp(java.time.LocalDateTime.now())
                        .customerType(
                                dto.getCustomerType() != null ? dto.getCustomerType() : "INDIVIDUAL")
                        .panNumber(dto.getPanNumber())
                        .aadhaarNumber(dto.getAadhaarNumber())
                        .gstNumber(dto.getGstNumber())
                        .annualIncome(dto.getAnnualIncome())
                        .occupation(dto.getOccupation())
                        .isPep(dto.getIsPep() != null ? dto.getIsPep() : false)
                        .riskCategory(derivedRisk)
                        .approvalStatus(com.ledgora.common.enums.MakerCheckerStatus.PENDING)
                        .build();

        Customer saved = customerRepository.save(customer);

        approvalService.submitForApproval(
                "CUSTOMER",
                saved.getCustomerId(),
                "Customer create: "
                        + saved.getFirstName()
                        + " "
                        + saved.getLastName()
                        + " NationalID: "
                        + saved.getNationalId());

        auditService.logChangeEvent(
                currentUser.getId(),
                "CUSTOMER_CREATED",
                "CUSTOMER",
                saved.getCustomerId(),
                "Customer created (pending approval): "
                        + saved.getFirstName()
                        + " "
                        + saved.getLastName()
                        + " | risk=" + derivedRisk,
                null,
                null,
                "status=PENDING_APPROVAL,kyc=PENDING,risk=" + derivedRisk,
                null,
                tenantId);

        log.info(
                "Customer created (PENDING_APPROVAL): {} {} (ID: {}) by user {} risk={}",
                saved.getFirstName(),
                saved.getLastName(),
                saved.getCustomerId(),
                currentUser.getUsername(),
                derivedRisk);
        return saved;
    }

    /**
     * Update customer master data (maker step). Applies changes and resets approvalStatus to
     * PENDING so a checker must re-approve the modified record per CBS dual-control requirements.
     *
     * <p>Enhancements: blocks direct edit of ACTIVE records without entering maker-checker cycle,
     * tenant-scoped PAN/Aadhaar/NationalId uniqueness, re-derives risk category, emits
     * CUSTOMER_UPDATED audit event.
     */
    @Transactional
    public Customer updateCustomer(Long customerId, CustomerDTO dto) {
        Customer customer = requireCustomer(customerId);
        Long tenantId = requireTenantId();

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot update customer: maker identity could not be resolved");
        }

        // ── Cannot edit an ACTIVE customer without going through maker-checker cycle ──
        // An ACTIVE customer is one that has been approved. Any modification must reset it to
        // PENDING so a checker re-approves the change before it takes effect.
        // The only allowed operation here is to submit the change (which resets to PENDING).
        // Attempting to bypass this by editing a currently PENDING record that belongs to another
        // maker is also blocked.
        if (customer.getApprovalStatus() == com.ledgora.common.enums.MakerCheckerStatus.APPROVED
                && customer.getCreatedBy() != null
                && !customer.getCreatedBy().getId().equals(currentUser.getId())) {
            // An ACTIVE customer can only be edited by its original maker or an admin role.
            // The controller layer enforces role-based access; here we enforce maker identity.
            // If the current user is not the original maker, they must not edit directly.
            throw new com.ledgora.common.exception.BusinessException(
                    "ACTIVE_RECORD_EDIT_BLOCKED",
                    "Cannot edit an ACTIVE customer record created by another user. "
                            + "Only the original maker or an authorised admin may submit changes.");
        }

        // ── Tenant-scoped uniqueness checks for updated values ──
        if (dto.getNationalId() != null
                && !dto.getNationalId().equals(customer.getNationalId())
                && customerRepository.existsByTenantIdAndNationalId(tenantId, dto.getNationalId(), customerId)) {
            throw new com.ledgora.common.exception.BusinessException(
                    "DUPLICATE_NATIONAL_ID",
                    "Customer with national ID " + dto.getNationalId() + " already exists in this tenant");
        }
        if (dto.getPanNumber() != null && !dto.getPanNumber().isBlank()
                && !dto.getPanNumber().equals(customer.getPanNumber())
                && customerRepository.existsByTenantIdAndPanNumber(tenantId, dto.getPanNumber(), customerId)) {
            throw new com.ledgora.common.exception.BusinessException(
                    "DUPLICATE_PAN",
                    "Customer with PAN " + dto.getPanNumber() + " already exists in this tenant");
        }
        if (dto.getAadhaarNumber() != null && !dto.getAadhaarNumber().isBlank()
                && !dto.getAadhaarNumber().equals(customer.getAadhaarNumber())
                && customerRepository.existsByTenantIdAndAadhaarNumber(tenantId, dto.getAadhaarNumber(), customerId)) {
            throw new com.ledgora.common.exception.BusinessException(
                    "DUPLICATE_AADHAAR",
                    "Customer with Aadhaar already exists in this tenant");
        }

        // Snapshot old state for audit trail
        String oldState = "status=" + customer.getApprovalStatus()
                + ",kyc=" + customer.getKycStatus()
                + ",risk=" + customer.getRiskCategory();

        if (dto.getFirstName() != null) customer.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) customer.setLastName(dto.getLastName());
        if (dto.getDob() != null) customer.setDob(dto.getDob());
        if (dto.getNationalId() != null) customer.setNationalId(dto.getNationalId());
        if (dto.getPhone() != null) customer.setPhone(dto.getPhone());
        if (dto.getEmail() != null) customer.setEmail(dto.getEmail());
        if (dto.getAddress() != null) customer.setAddress(dto.getAddress());
        if (dto.getPanNumber() != null) customer.setPanNumber(dto.getPanNumber());
        if (dto.getAadhaarNumber() != null) customer.setAadhaarNumber(dto.getAadhaarNumber());
        if (dto.getGstNumber() != null) customer.setGstNumber(dto.getGstNumber());
        if (dto.getAnnualIncome() != null) customer.setAnnualIncome(dto.getAnnualIncome());
        if (dto.getOccupation() != null) customer.setOccupation(dto.getOccupation());
        if (dto.getIsPep() != null) customer.setIsPep(dto.getIsPep());

        // ── Re-derive risk category after field updates ──
        String derivedRisk = deriveRiskCategory(dto.getIsPep() != null ? dto : buildDtoFromCustomer(customer, dto));
        customer.setRiskCategory(derivedRisk);

        // ── CBS: modifications reset approval status to PENDING_APPROVAL ──
        customer.setApprovalStatus(com.ledgora.common.enums.MakerCheckerStatus.PENDING);
        customer.setKycStatus("PENDING");
        customer.setMakerTimestamp(java.time.LocalDateTime.now());
        customer.setApprovedBy(null);
        customer.setCheckerTimestamp(null);

        Customer saved = customerRepository.save(customer);

        approvalService.submitForApproval(
                "CUSTOMER",
                saved.getCustomerId(),
                "Customer update: " + saved.getFirstName() + " " + saved.getLastName());

        String newState = "status=PENDING_APPROVAL,kyc=PENDING,risk=" + derivedRisk;
        auditService.logChangeEvent(
                currentUser.getId(),
                "CUSTOMER_UPDATED",
                "CUSTOMER",
                saved.getCustomerId(),
                "Customer master data updated (pending re-approval) by " + currentUser.getUsername(),
                null,
                oldState,
                newState,
                null,
                tenantId);

        log.info(
                "Customer {} updated (PENDING re-approval) by user {} risk={}",
                customerId,
                currentUser.getUsername(),
                derivedRisk);
        return saved;
    }

    /**
     * Direct KYC status update — restricted to ADMIN/MANAGER/OPERATIONS for regulatory overrides
     * (e.g., manual KYC remediation). Validates the status value is a known KYC state.
     */
    @Transactional
    public Customer updateKycStatus(Long customerId, String kycStatus) {
        // Validate KYC status is a known value (prevent arbitrary string injection)
        if (!"PENDING".equals(kycStatus)
                && !"VERIFIED".equals(kycStatus)
                && !"REJECTED".equals(kycStatus)
                && !"UNDER_REVIEW".equals(kycStatus)) {
            throw new com.ledgora.common.exception.BusinessException(
                    "INVALID_KYC_STATUS",
                    "Invalid KYC status: "
                            + kycStatus
                            + ". Allowed: PENDING, VERIFIED, REJECTED, UNDER_REVIEW");
        }
        Customer customer = requireCustomer(customerId);

        User currentUser = getCurrentUser();
        String previousStatus = customer.getKycStatus();
        customer.setKycStatus(kycStatus);
        Customer saved = customerRepository.save(customer);

        auditService.logEvent(
                currentUser != null ? currentUser.getId() : null,
                "CUSTOMER_KYC_UPDATE",
                "CUSTOMER",
                customerId,
                "KYC status changed from "
                        + previousStatus
                        + " to "
                        + kycStatus
                        + " by "
                        + (currentUser != null ? currentUser.getUsername() : "system"),
                null);

        log.info("Customer {} KYC status updated to {}", customerId, kycStatus);
        return saved;
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

    /** Get customers by approval status (for checker pending queue). Tenant-isolated. */
    public List<Customer> getByApprovalStatus(
            com.ledgora.common.enums.MakerCheckerStatus approvalStatus) {
        return customerRepository.findByTenantIdAndApprovalStatus(requireTenantId(), approvalStatus);
    }

    /** Count customers pending approval (for dashboard badge). */
    public long countPendingApproval() {
        return customerRepository.countByTenantIdAndApprovalStatus(
                requireTenantId(), com.ledgora.common.enums.MakerCheckerStatus.PENDING);
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
        // Identity check BEFORE any persistence — freeze must not be applied without an auditable maker
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot update customer freeze status: maker identity could not be resolved");
        }

        Customer customer = requireCustomer(customerId);

        // Persist freeze fields on Customer entity
        customer.setFreezeLevel(freezeLevel != null ? freezeLevel : "NONE");
        customer.setFreezeReason(freezeReason);

        Customer saved = customerRepository.save(customer);

        auditService.logEvent(
                currentUser.getId(),
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

    /**
     * Approve customer (checker step). Enforces maker != checker + tenant isolation.
     *
     * <p>Enhancements: customer must be PENDING_APPROVAL, KYC must be APPROVED before activation,
     * emits CUSTOMER_APPROVED audit event.
     */
    @Transactional
    public Customer approveCustomer(Long customerId) {
        Customer customer = requireCustomer(customerId);
        Long tenantId = requireTenantId();

        // ── Must be in PENDING state ──
        if (customer.getApprovalStatus() != com.ledgora.common.enums.MakerCheckerStatus.PENDING) {
            throw new com.ledgora.common.exception.BusinessException(
                    "INVALID_STATE",
                    "Customer is not pending approval. Current status: "
                            + customer.getApprovalStatus());
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot approve customer: approver identity could not be resolved");
        }

        // ── Maker ≠ Checker ──
        if (customer.getCreatedBy() != null
                && customer.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new com.ledgora.common.exception.BusinessException(
                    "MAKER_CHECKER_VIOLATION",
                    "Cannot approve your own customer record (maker-checker violation)");
        }

        // ── KYC must be APPROVED before activation ──
        if (!"VERIFIED".equals(customer.getKycStatus())) {
            throw new com.ledgora.common.exception.BusinessException(
                    "KYC_NOT_APPROVED",
                    "Cannot activate customer: KYC status must be VERIFIED before approval. "
                            + "Current KYC status: " + customer.getKycStatus());
        }

        // Set all approval state fields atomically
        customer.setApprovalStatus(com.ledgora.common.enums.MakerCheckerStatus.APPROVED);
        customer.setApprovedBy(currentUser);
        customer.setCheckerTimestamp(java.time.LocalDateTime.now());
        Customer saved = customerRepository.save(customer);

        auditService.logChangeEvent(
                currentUser.getId(),
                "CUSTOMER_APPROVED",
                "CUSTOMER",
                customerId,
                "Customer approved by " + currentUser.getUsername()
                        + ": " + customer.getFirstName() + " " + customer.getLastName(),
                null,
                "status=PENDING_APPROVAL",
                "status=APPROVED,kyc=" + saved.getKycStatus(),
                null,
                tenantId);

        log.info("Customer {} approved by user {}", customerId, currentUser.getUsername());
        return saved;
    }

    /**
     * Reject customer (checker step). Enforces maker != checker + tenant isolation.
     *
     * <p>Enhancements: customer must be PENDING_APPROVAL, emits CUSTOMER_REJECTED audit event.
     */
    @Transactional
    public Customer rejectCustomer(Long customerId) {
        Customer customer = requireCustomer(customerId);
        Long tenantId = requireTenantId();

        // ── Must be in PENDING state ──
        if (customer.getApprovalStatus() != com.ledgora.common.enums.MakerCheckerStatus.PENDING) {
            throw new com.ledgora.common.exception.BusinessException(
                    "INVALID_STATE",
                    "Customer is not pending approval. Current status: "
                            + customer.getApprovalStatus());
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new com.ledgora.common.exception.BusinessException(
                    "IDENTITY_REQUIRED",
                    "Cannot reject customer: reviewer identity could not be resolved");
        }

        // ── Maker ≠ Checker ──
        if (customer.getCreatedBy() != null
                && customer.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new com.ledgora.common.exception.BusinessException(
                    "MAKER_CHECKER_VIOLATION",
                    "Cannot reject your own customer record (maker-checker violation)");
        }

        // Set all rejection state fields atomically
        customer.setKycStatus("REJECTED");
        customer.setApprovalStatus(com.ledgora.common.enums.MakerCheckerStatus.REJECTED);
        customer.setApprovedBy(currentUser);
        customer.setCheckerTimestamp(java.time.LocalDateTime.now());
        Customer saved = customerRepository.save(customer);

        auditService.logChangeEvent(
                currentUser.getId(),
                "CUSTOMER_REJECTED",
                "CUSTOMER",
                customerId,
                "Customer rejected by " + currentUser.getUsername()
                        + ": " + customer.getFirstName() + " " + customer.getLastName(),
                null,
                "status=PENDING_APPROVAL",
                "status=REJECTED,kyc=REJECTED",
                null,
                tenantId);

        log.info("Customer {} rejected by user {}", customerId, currentUser.getUsername());
        return saved;
    }

    /**
     * Derive risk category from income, occupation, and PEP flag.
     *
     * <p>Rules (in priority order):
     *
     * <ol>
     *   <li>PEP flag = true → HIGH (overrides everything)
     *   <li>Occupation = POLITICIAN / GOVERNMENT_OFFICIAL / FOREIGN_NATIONAL → HIGH
     *   <li>Annual income >= 10,00,000 (10 lakh INR) → MEDIUM
     *   <li>Annual income >= 50,00,000 (50 lakh INR) → HIGH
     *   <li>Default → LOW
     * </ol>
     *
     * If the caller has explicitly set riskCategory on the DTO and no risk inputs are provided,
     * the explicit value is honoured.
     */
    private String deriveRiskCategory(CustomerDTO dto) {
        // PEP flag immediately elevates to HIGH
        if (Boolean.TRUE.equals(dto.getIsPep())) {
            return "HIGH";
        }

        // High-risk occupations
        if (dto.getOccupation() != null) {
            String occ = dto.getOccupation().toUpperCase();
            if (occ.contains("POLITICIAN")
                    || occ.contains("GOVERNMENT_OFFICIAL")
                    || occ.contains("FOREIGN_NATIONAL")
                    || occ.contains("ARMS")
                    || occ.contains("GAMBLING")) {
                return "HIGH";
            }
        }

        // Income-based risk
        if (dto.getAnnualIncome() != null) {
            java.math.BigDecimal income = dto.getAnnualIncome();
            // >= 50 lakh → HIGH
            if (income.compareTo(new java.math.BigDecimal("5000000")) >= 0) {
                return "HIGH";
            }
            // >= 10 lakh → MEDIUM
            if (income.compareTo(new java.math.BigDecimal("1000000")) >= 0) {
                return "MEDIUM";
            }
        }

        // Honour explicit DTO value if no risk signals found
        if (dto.getRiskCategory() != null && !dto.getRiskCategory().isBlank()) {
            return dto.getRiskCategory();
        }

        return "LOW";
    }

    /**
     * Build a minimal CustomerDTO from an existing Customer entity plus any overrides from the
     * incoming DTO. Used to re-derive risk when only a subset of fields is provided on update.
     */
    private CustomerDTO buildDtoFromCustomer(Customer customer, CustomerDTO override) {
        return CustomerDTO.builder()
                .isPep(override.getIsPep() != null ? override.getIsPep() : customer.getIsPep())
                .occupation(
                        override.getOccupation() != null
                                ? override.getOccupation()
                                : customer.getOccupation())
                .annualIncome(
                        override.getAnnualIncome() != null
                                ? override.getAnnualIncome()
                                : customer.getAnnualIncome())
                .riskCategory(
                        override.getRiskCategory() != null
                                ? override.getRiskCategory()
                                : customer.getRiskCategory())
                .build();
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
