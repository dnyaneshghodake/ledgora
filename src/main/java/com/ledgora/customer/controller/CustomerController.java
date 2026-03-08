package com.ledgora.customer.controller;

import com.ledgora.customer.dto.CustomerDTO;
import com.ledgora.customer.entity.Customer;
import com.ledgora.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * PART 1: Customer controller for JSP-based customer management.
 */
@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String listCustomers(@RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "kycStatus", required = false) String kycStatus,
                                Model model) {
        if (search != null && !search.isEmpty()) {
            model.addAttribute("customers", customerService.searchByName(search));
            model.addAttribute("search", search);
        } else if (kycStatus != null && !kycStatus.isEmpty()) {
            model.addAttribute("customers", customerService.getByKycStatus(kycStatus));
            model.addAttribute("selectedKycStatus", kycStatus);
        } else {
            model.addAttribute("customers", customerService.getAllCustomers());
        }
        return "customer/customers";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("customerDTO", new CustomerDTO());
        return "customer/customer-create";
    }

    @PostMapping("/create")
    public String createCustomer(@Valid @ModelAttribute("customerDTO") CustomerDTO dto,
                                 BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "customer/customer-create";
        }
        try {
            Customer customer = customerService.createCustomer(dto);
            redirectAttributes.addFlashAttribute("message",
                    "Customer created: " + customer.getFirstName() + " " + customer.getLastName());
            return "redirect:/customers";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "customer/customer-create";
        }
    }

    @GetMapping("/{id}")
    public String viewCustomer(@PathVariable Long id, Model model) {
        Customer customer = customerService.getCustomerById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        model.addAttribute("customer", customer);
        return "customer/customer-view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.getCustomerById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        CustomerDTO dto = CustomerDTO.builder()
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
        return "customer/customer-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateCustomer(@PathVariable Long id,
                                 @Valid @ModelAttribute("customerDTO") CustomerDTO dto,
                                 BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "customer/customer-edit";
        }
        try {
            customerService.updateCustomer(id, dto);
            redirectAttributes.addFlashAttribute("message", "Customer updated successfully");
            return "redirect:/customers/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "customer/customer-edit";
        }
    }

    @PostMapping("/{id}/kyc")
    public String updateKycStatus(@PathVariable Long id, @RequestParam String kycStatus,
                                  RedirectAttributes redirectAttributes) {
        try {
            customerService.updateKycStatus(id, kycStatus);
            redirectAttributes.addFlashAttribute("message", "KYC status updated to " + kycStatus);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + id;
    }
}
