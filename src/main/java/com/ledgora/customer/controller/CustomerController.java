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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize(
            "hasAnyRole('MAKER', 'CHECKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
    public String listCustomers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "kycStatus", required = false) String kycStatus,
            @RequestParam(value = "approvalStatus", required = false) String approvalStatus,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {
        // Build query string for pagination links (preserves filters)
        StringBuilder qs = new StringBuilder();
        if (search != null && !search.isEmpty()) qs.append("&search=").append(search);
        if (kycStatus != null && !kycStatus.isEmpty()) qs.append("&kycStatus=").append(kycStatus);

        if (search != null && !search.isEmpty()) {
            List<Customer> customers = customerService.searchByName(search);
            model.addAttribute("customers", customers);
            model.addAttribute("search", search);
        } else if (kycStatus != null && !kycStatus.isEmpty()) {
            List<Customer> customers = customerService.getByKycStatus(kycStatus);
            model.addAttribute("customers", customers);
            model.addAttribute("selectedKycStatus", kycStatus);
        } else if (approvalStatus != null && !approvalStatus.isEmpty()) {
            // Checker pending-approval queue filter
            try {
                MakerCheckerStatus statusEnum = MakerCheckerStatus.valueOf(approvalStatus);
                List<Customer> customers = customerService.getByApprovalStatus(statusEnum);
                model.addAttribute("customers", customers);
                model.addAttribute("selectedApprovalStatus", approvalStatus);
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", "Invalid approval status: " + approvalStatus);
                model.addAttribute("customers", List.of());
            }
        } else {
            org.springframework.data.domain.Page<Customer> customerPage =
                    customerService.getAllCustomersPaged(page, size);
            model.addAttribute("customers", customerPage.getContent());
            model.addAttribute("currentPage", customerPage.getNumber());
            model.addAttribute("totalPages", customerPage.getTotalPages());
            model.addAttribute("totalElements", customerPage.getTotalElements());
            model.addAttribute("pageSize", size);
        }
        // Pending count for checker badge
        try {
            model.addAttribute("pendingApprovalCount", customerService.countPendingApproval());
        } catch (Exception ignored) {
            model.addAttribute("pendingApprovalCount", 0L);
        }
        model.addAttribute("baseUrl", "/customers");
        model.addAttribute("queryString", qs.toString());
        model.addAttribute("freezeLevels", FreezeLevel.values());
        model.addAttribute("approvalStatuses", MakerCheckerStatus.values());
        return "customer/customers";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('MAKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
    public String createForm(Model model) {
        model.addAttribute("customerDTO", new CustomerDTO());
        model.addAttribute("freezeLevels", FreezeLevel.values());
        return "customer/customer-create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('MAKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
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
    @PreAuthorize(
            "hasAnyRole('MAKER', 'CHECKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
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
            model.addAttribute("linkedAccounts", List.of());
        }
        model.addAttribute("freezeLevels", FreezeLevel.values());
        // Load audit logs for this customer (tenant-isolated)
        try {
            Long tenantId = com.ledgora.tenant.context.TenantContextHolder.getRequiredTenantId();
            List<AuditLog> allLogs =
                    auditLogRepository.findByTenantIdAndEntityAndEntityId(tenantId, "CUSTOMER", id);
            // Freeze history
            List<Map<String, Object>> freezeHistory =
                    allLogs.stream()
                            .filter(
                                    log ->
                                            log.getAction() != null
                                                    && log.getAction().contains("FREEZE"))
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
            // Audit trail preview: last 5 entries across all actions
            List<AuditLog> auditPreview =
                    allLogs.stream()
                            .sorted(
                                    java.util.Comparator.comparing(
                                                    AuditLog::getTimestamp,
                                                    java.util.Comparator.nullsLast(
                                                            java.util.Comparator.reverseOrder()))
                                            .thenComparing(
                                                    java.util.Comparator.comparingLong(
                                                                    AuditLog::getId)
                                                            .reversed()))
                            .limit(5)
                            .collect(Collectors.toList());
            model.addAttribute("auditPreview", auditPreview);
        } catch (Exception e) {
            model.addAttribute("freezeHistory", List.of());
            model.addAttribute("auditPreview", List.of());
        }
        return "customer/customer-view";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MAKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
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
                        // CBS fields — must be round-tripped so they aren't cleared on save
                        .customerType(customer.getCustomerType())
                        .panNumber(customer.getPanNumber())
                        .aadhaarNumber(customer.getAadhaarNumber())
                        .gstNumber(customer.getGstNumber())
                        .riskCategory(customer.getRiskCategory())
                        .approvalStatus(
                                customer.getApprovalStatus() != null
                                        ? customer.getApprovalStatus().name()
                                        : null)
                        // Risk derivation fields — must be round-tripped for re-derivation on save
                        .annualIncome(customer.getAnnualIncome())
                        .occupation(customer.getOccupation())
                        .isPep(customer.getIsPep())
                        .build();
        model.addAttribute("customerDTO", dto);
        model.addAttribute("freezeLevels", FreezeLevel.values());
        // Audit metadata for CIF Snapshot "Last Updated" display (read-only, no business logic)
        model.addAttribute("auditUpdatedAt", customer.getUpdatedAt());
        model.addAttribute(
                "auditLastModifiedBy",
                customer.getCreatedBy() != null ? customer.getCreatedBy().getUsername() : "--");
        return "customer/customer-edit";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MAKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
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
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Customer updated and submitted for re-approval (PENDING). "
                            + "A checker must approve before the record is active.");
            return "redirect:/customers/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("freezeLevels", FreezeLevel.values());
            return "customer/customer-edit";
        }
    }

    @PostMapping("/{id}/kyc")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS')")
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
    @PreAuthorize("hasAnyRole('MAKER', 'ADMIN', 'MANAGER', 'OPERATIONS')")
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
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String approveCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.approveCustomer(id);
            redirectAttributes.addFlashAttribute("message", "Customer approved successfully");
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Approval conflict: this customer was already actioned by another session. Please refresh.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MANAGER')")
    public String rejectCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.rejectCustomer(id);
            redirectAttributes.addFlashAttribute("message", "Customer rejected");
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Rejection conflict: this customer was already actioned by another session. Please refresh.");
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
    @PreAuthorize(
            "hasAnyRole('MAKER', 'CHECKER', 'TELLER', 'ADMIN', 'MANAGER', 'OPERATIONS', 'AUDITOR')")
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
