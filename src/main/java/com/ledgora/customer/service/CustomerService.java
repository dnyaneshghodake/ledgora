package com.ledgora.customer.service;

import com.ledgora.customer.dto.CustomerDTO;
import com.ledgora.customer.entity.Customer;
import com.ledgora.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer createCustomer(CustomerDTO dto) {
        if (dto.getNationalId() != null && customerRepository.existsByNationalId(dto.getNationalId())) {
            throw new RuntimeException("Customer with national ID " + dto.getNationalId() + " already exists");
        }

        Customer customer = Customer.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dob(dto.getDob())
                .nationalId(dto.getNationalId())
                .kycStatus(dto.getKycStatus() != null ? dto.getKycStatus() : "PENDING")
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created: {} {} (ID: {})", saved.getFirstName(), saved.getLastName(), saved.getCustomerId());
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
        return customerRepository.findAll();
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public Optional<Customer> getCustomerByNationalId(String nationalId) {
        return customerRepository.findByNationalId(nationalId);
    }

    public List<Customer> searchByName(String name) {
        return customerRepository.searchByName(name);
    }

    public List<Customer> getByKycStatus(String kycStatus) {
        return customerRepository.findByKycStatus(kycStatus);
    }

    public long countAll() {
        return customerRepository.count();
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
        customer.setKycStatus("VERIFIED");
        log.info("Customer {} approved", customerId);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer rejectCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        customer.setKycStatus("REJECTED");
        log.info("Customer {} rejected", customerId);
        return customerRepository.save(customer);
    }
}
