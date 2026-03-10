package com.ledgora.customer.controller;

import com.ledgora.account.service.AccountService;
import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.common.enums.FreezeLevel;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.customer.dto.CustomerDTO;
import com.ledgora.customer.entity.Customer;
import com.ledgora.customer.service.CustomerService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Customer controller for JSP-based customer management. Supports RBI-grade fields: customerType,
 * freeze, approval, tax profile.
 */
@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final AccountService accountService;
    private final AuditLogRepository auditLogRepository;

    public CustomerController(
            CustomerService customerService,
            AccountService accountService,
            AuditLogRepository auditLogRepository) {
        this.customerService = customerService;
        this.accountService = accountService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String listCustomers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "kycStatus", required = false) String kycStatus,
            @RequestParam(value = "approvalStatus", required = false) String approvalStatus,
            @RequestParam(value = "status", required = false) String status,
            Model model) {
        List<Customer> customers;
        if (search != null && !search.isEmpty()) {
            customers = customerService.searchByName(search);
            model.addAttribute("search", search);
        } else if (kycStatus != null && !kycStatus.isEmpty()) {
            customers = customerService.getByKycStatus(kycStatus);
            model.addAttribute("selectedKycStatus", kycStatus);
        } else {
            customers = customerService.getAllCustomers();
        }
        model.addAttribute("customers", customers);
        model.addAttribute("freezeLevels", FreezeLevel.values());
        model.addAttribute("approvalStatuses", MakerCheckerStatus.values());
        return "customer/customers";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("customerDTO", new CustomerDTO());
        model.addAttribute("freezeLevels", FreezeLevel.values());
        return "customer/customer-create";
    }

    @PostMapping("/create")
    public String createCustomer(
            @Valid @ModelAttribute("customerDTO") CustomerDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("freezeLevels", FreezeLevel.values());
            return "customer/customer-create";
        }
        try {
            Customer customer = customerService.createCustomer(dto);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Customer created (PENDING approval): "
                            + customer.getFirstName()
                            + " "
                            + customer.getLastName());
            return "redirect:/customers";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("freezeLevels", FreezeLevel.values());
            return "customer/customer-create";
        }
    }

    @GetMapping("/{id}")
    public String viewCustomer(@PathVariable Long id, Model model) {
        Customer customer =
                customerService
                        .getCustomerById(id)
                        .orElseThrow(() -> new RuntimeException("Customer not found"));
        model.addAttribute("customer", customer);
        // Load linked accounts for this customer
        try {
            model.addAttribute("linkedAccounts", accountService.getAccountsByCustomerId(id));
        } catch (Exception e) {
            // If method not available, set empty list
            model.addAttribute("linkedAccounts", List.of());
        }
        model.addAttribute("freezeLevels", FreezeLevel.values());
        // Load freeze history from audit logs
        try {
            List<AuditLog> freezeLogs =
                    auditLogRepository.findByEntityAndEntityId("CUSTOMER", id).stream()
                            .filter(
                                    log ->
                                            log.getAction() != null
                                                    && log.getAction().contains("FREEZE"))
                            .collect(Collectors.toList());
            List<Map<String, Object>> freezeHistory =
                    freezeLogs.stream()
                            .map(
                                    log -> {
                                        Map<String, Object> entry = new HashMap<>();
                                        entry.put("timestamp", log.getTimestamp());
                                        entry.put("action", log.getAction());
                                        entry.put(
                                                "username",
                                                log.getUsername() != null
                                                        ? log.getUsername()
                                                        : "System");
                                        entry.put("checker", "--");
                                        entry.put("details", log.getDetails());
                                        return entry;
                                    })
                            .collect(Collectors.toList());
            model.addAttribute("freezeHistory", freezeHistory);
        } catch (Exception e) {
            model.addAttribute("freezeHistory", List.of());
        }
        return "customer/customer-view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Customer customer =
                customerService
                        .getCustomerById(id)
                        .orElseThrow(() -> new RuntimeException("Customer not found"));
        CustomerDTO dto =
                CustomerDTO.builder()
                        .customerId(customer.getCustomerId())
                        .firstName(customer.getFirstName())
                        .lastName(customer.getLastName())
                        .dob(customer.getDob())
                        .nationalId(customer.getNationalId())
                        .kycStatus(customer.getKycStatus())
                        .phone(customer.getPhone())
                        .email(customer.getEmail())
                        .address(customer.getAddress())
                        .build();
        model.addAttribute("customerDTO", dto);
        model.addAttribute("freezeLevels", FreezeLevel.values());
        return "customer/customer-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateCustomer(
            @PathVariable Long id,
            @Valid @ModelAttribute("customerDTO") CustomerDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("freezeLevels", FreezeLevel.values());
            return "customer/customer-edit";
        }
        try {
            customerService.updateCustomer(id, dto);
            redirectAttributes.addFlashAttribute("message", "Customer updated successfully");
            return "redirect:/customers/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("freezeLevels", FreezeLevel.values());
            return "customer/customer-edit";
        }
    }

    @PostMapping("/{id}/kyc")
    public String updateKycStatus(
            @PathVariable Long id,
            @RequestParam String kycStatus,
            RedirectAttributes redirectAttributes) {
        try {
            customerService.updateKycStatus(id, kycStatus);
            redirectAttributes.addFlashAttribute("message", "KYC status updated to " + kycStatus);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/freeze")
    public String freezeCustomer(
            @PathVariable Long id,
            @RequestParam String freezeLevel,
            @RequestParam String freezeReason,
            RedirectAttributes redirectAttributes) {
        try {
            customerService.updateFreezeStatus(id, freezeLevel, freezeReason);
            redirectAttributes.addFlashAttribute(
                    "message", "Customer freeze updated to " + freezeLevel);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approveCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.approveCustomer(id);
            redirectAttributes.addFlashAttribute("message", "Customer approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/reject")
    public String rejectCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.rejectCustomer(id);
            redirectAttributes.addFlashAttribute("message", "Customer rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + id;
    }

    /**
     * AJAX endpoint for customer lookup - returns JSON (safe projection to avoid lazy-loading
     * issues)
     */
    @GetMapping("/api/search")
    @ResponseBody
    public List<Map<String, Object>> searchCustomersApi(@RequestParam("q") String query) {
        return customerService.searchByName(query).stream()
                .map(
                        c -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("customerId", c.getCustomerId());
                            m.put("firstName", c.getFirstName());
                            m.put("lastName", c.getLastName());
                            m.put("nationalId", c.getNationalId());
                            m.put("kycStatus", c.getKycStatus());
                            m.put("phone", c.getPhone());
                            m.put("email", c.getEmail());
                            return m;
                        })
                .collect(Collectors.toList());
    }
}
