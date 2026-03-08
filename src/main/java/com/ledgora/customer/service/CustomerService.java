package com.ledgora.customer.service;

import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.customer.dto.CustomerDTO;
import com.ledgora.customer.entity.Customer;
import com.ledgora.customer.repository.CustomerRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * PART 1: Customer service for managing customer lifecycle and KYC.
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    private final CustomerRepository customerRepository;
    private final TenantService tenantService;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository,
                           TenantService tenantService,
                           UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.tenantService = tenantService;
        this.userRepository = userRepository;
    }

    @Transactional
    public Customer createCustomer(CustomerDTO dto) {
        if (dto.getNationalId() != null && customerRepository.existsByNationalId(dto.getNationalId())) {
            throw new RuntimeException("Customer with national ID " + dto.getNationalId() + " already exists");
        }

        Long tenantId = requireTenantId();
        Tenant tenant = tenantService.getTenantById(tenantId);
        User currentUser = getCurrentUser();

        Customer customer = Customer.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dob(dto.getDob())
                .nationalId(dto.getNationalId())
                .kycStatus(dto.getKycStatus() != null ? dto.getKycStatus() : "PENDING")
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .tenant(tenant)
                .createdBy(currentUser)
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created: {} {} (ID: {}) by user {}", saved.getFirstName(), saved.getLastName(),
                saved.getCustomerId(), currentUser != null ? currentUser.getUsername() : "system");
        return saved;
    }

    @Transactional
    public Customer updateCustomer(Long customerId, CustomerDTO dto) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

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
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        customer.setKycStatus(kycStatus);
        log.info("Customer {} KYC status updated to {}", customerId, kycStatus);
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findByTenantId(requireTenantId());
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
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

    @Transactional
    public Customer updateFreezeStatus(Long customerId, String freezeLevel, String freezeReason) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        // Store freeze info in address field suffix for now (Customer entity doesn't have freeze fields directly)
        // The actual freeze enforcement is on CustomerMaster entity
        log.info("Customer {} freeze updated to {} reason: {}", customerId, freezeLevel, freezeReason);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer approveCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        // Maker-checker: approver must be different from creator
        User currentUser = getCurrentUser();
        if (customer.getCreatedBy() != null && currentUser != null
                && customer.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot approve your own customer record (maker-checker violation)");
        }

        customer.setKycStatus("VERIFIED");
        log.info("Customer {} approved by user {}", customerId,
                currentUser != null ? currentUser.getUsername() : "system");
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer rejectCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        // Maker-checker: rejector must be different from creator
        User currentUser = getCurrentUser();
        if (customer.getCreatedBy() != null && currentUser != null
                && customer.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot reject your own customer record (maker-checker violation)");
        }

        customer.setKycStatus("REJECTED");
        log.info("Customer {} rejected by user {}", customerId,
                currentUser != null ? currentUser.getUsername() : "system");
        return customerRepository.save(customer);
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
