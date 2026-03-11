package com.ledgora.customer.controller;

import com.ledgora.account.service.AccountService;
import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.common.enums.FreezeLevel;
import com.ledgora.common.enums.MakerCheckerStatus;
import com.ledgora.customer.dto.CustomerDTO;
import com.ledgora.customer.entity.Customer;
import com.ledgora.customer.service.CustomerService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Finacle-grade Customer Master controller. Serves the 7-tab customer-master.jsp with full data
 * wiring. Reads from Customer entity, AuditLog, and AccountService for linked accounts.
 */
@Controller
@RequestMapping("/customers")
public class CustomerMasterController {

    private final CustomerService customerService;
    private final AccountService accountService;
    private final AuditLogRepository auditLogRepository;

    public CustomerMasterController(
            CustomerService customerService,
            AccountService accountService,
            AuditLogRepository auditLogRepository) {
        this.customerService = customerService;
        this.accountService = accountService;
        this.auditLogRepository = auditLogRepository;
    }

    /** Customer Master — 7-tab Finacle-style view for a single customer. */
    @GetMapping("/{id}/master")
    public String viewCustomerMaster(@PathVariable Long id, Model model) {
        Customer customer =
                customerService
                        .getCustomerById(id)
                        .orElseThrow(() -> new RuntimeException("Customer not found: " + id));

        // ── Header fields ──
        model.addAttribute("customer", customer);
        model.addAttribute("customerId", customer.getCustomerId());
        model.addAttribute(
                "tenantName",
                customer.getTenant() != null ? customer.getTenant().getTenantName() : "--");
        model.addAttribute(
                "makerCheckerStatus",
                customer.getKycStatus() != null ? customer.getKycStatus() : "PENDING");

        // ── Tab 1: General Info ──
        model.addAttribute("firstName", customer.getFirstName());
        model.addAttribute("lastName", customer.getLastName());
        model.addAttribute("dob", customer.getDob());
        model.addAttribute("nationalId", customer.getNationalId());
        model.addAttribute("kycStatus", customer.getKycStatus());

        // ── Tab 2: Contact Info ──
        model.addAttribute("phone", customer.getPhone());
        model.addAttribute("email", customer.getEmail());
        model.addAttribute("address", customer.getAddress());

        // ── Tab 3: KYC & Identity — read from customer fields ──

        // ── Tab 4: Tax Profile — placeholder (managed via TaxProfileController) ──

        // ── Tab 5: Freeze Control ──
        model.addAttribute("freezeLevels", FreezeLevel.values());

        // Freeze history from audit logs
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
                                        entry.put("details", log.getDetails());
                                        return entry;
                                    })
                            .collect(Collectors.toList());
            model.addAttribute("freezeHistory", freezeHistory);
        } catch (Exception e) {
            model.addAttribute("freezeHistory", List.of());
        }

        // ── Tab 6: Linked Accounts ──
        try {
            model.addAttribute("linkedAccounts", accountService.getAccountsByCustomerId(id));
        } catch (Exception e) {
            model.addAttribute("linkedAccounts", List.of());
        }

        // ── Tab 7: Audit & Approval ──
        model.addAttribute(
                "createdByUsername",
                customer.getCreatedBy() != null ? customer.getCreatedBy().getUsername() : "--");
        model.addAttribute("createdAt", customer.getCreatedAt());

        // Enum values for dropdowns
        model.addAttribute("makerCheckerStatuses", MakerCheckerStatus.values());

        return "customer/customer-master";
    }

    /** Save customer master fields (General + Contact tabs). */
    @PostMapping("/{id}/master/save")
    public String saveCustomerMaster(
            @PathVariable Long id,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String nationalId,
            RedirectAttributes redirectAttributes) {
        try {
            CustomerDTO dto =
                    CustomerDTO.builder()
                            .firstName(firstName)
                            .lastName(lastName)
                            .phone(phone)
                            .email(email)
                            .address(address)
                            .nationalId(nationalId)
                            .build();
            customerService.updateCustomer(id, dto);
            redirectAttributes.addFlashAttribute("message", "Customer master saved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + id + "/master";
    }

    /** Update freeze on customer (maker step). */
    @PostMapping("/{id}/master/freeze")
    public String updateFreeze(
            @PathVariable Long id,
            @RequestParam String freezeLevel,
            @RequestParam String freezeReason,
            RedirectAttributes redirectAttributes) {
        try {
            customerService.updateFreezeStatus(id, freezeLevel, freezeReason);
            redirectAttributes.addFlashAttribute(
                    "message", "Freeze level updated to " + freezeLevel);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + id + "/master#tabFreeze";
    }

    /** Approve customer from master view (checker step). */
    @PostMapping("/{id}/master/approve")
    public String approveCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.approveCustomer(id);
            redirectAttributes.addFlashAttribute("message", "Customer approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + id + "/master#tabAudit";
    }

    /** Reject customer from master view (checker step). */
    @PostMapping("/{id}/master/reject")
    public String rejectCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.rejectCustomer(id);
            redirectAttributes.addFlashAttribute("message", "Customer rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + id + "/master#tabAudit";
    }

    /** Update KYC status from master view. */
    @PostMapping("/{id}/master/kyc")
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
        return "redirect:/customers/" + id + "/master#tabKyc";
    }
}
