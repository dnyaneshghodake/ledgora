package com.ledgora.customer.controller;

import com.ledgora.customer.entity.Customer;
import com.ledgora.customer.entity.CustomerMaster;
import com.ledgora.customer.entity.CustomerTaxProfile;
import com.ledgora.customer.repository.CustomerMasterRepository;
import com.ledgora.customer.repository.CustomerRepository;
import com.ledgora.customer.repository.CustomerTaxProfileRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Controller for managing Customer Tax Profiles (PAN, Aadhaar, GST, TDS). */
@Controller
@RequestMapping("/tax-profiles")
public class TaxProfileController {

    private final CustomerRepository customerRepository;
    private final CustomerMasterRepository customerMasterRepository;
    private final CustomerTaxProfileRepository taxProfileRepository;
    private final TenantService tenantService;

    public TaxProfileController(
            CustomerRepository customerRepository,
            CustomerMasterRepository customerMasterRepository,
            CustomerTaxProfileRepository taxProfileRepository,
            TenantService tenantService) {
        this.customerRepository = customerRepository;
        this.customerMasterRepository = customerMasterRepository;
        this.taxProfileRepository = taxProfileRepository;
        this.tenantService = tenantService;
    }

    @GetMapping("/create")
    public String showForm(@RequestParam("customerId") Long customerId, Model model) {
        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(
                                () -> new RuntimeException("Customer not found: " + customerId));
        model.addAttribute("customer", customer);

        Long tenantId = TenantContextHolder.getTenantId();
        // Try to find existing tax profile via CustomerMaster
        Optional<CustomerMaster> cmOpt =
                customerMasterRepository.findByTenantIdAndNationalId(
                        tenantId, customer.getNationalId());
        if (cmOpt.isPresent()) {
            Optional<CustomerTaxProfile> existing =
                    taxProfileRepository.findByCustomerMasterIdAndTenantId(
                            cmOpt.get().getId(), tenantId);
            if (existing.isPresent()) {
                model.addAttribute("taxProfile", existing.get());
                model.addAttribute("isEdit", true);
            }
        }
        if (!model.containsAttribute("taxProfile")) {
            // Pre-fill PAN from customer nationalId
            CustomerTaxProfile tp = new CustomerTaxProfile();
            tp.setPanNumber(customer.getNationalId());
            model.addAttribute("taxProfile", tp);
            model.addAttribute("isEdit", false);
        }
        return "customer/tax-profile-form";
    }

    @PostMapping("/save")
    public String saveTaxProfile(
            @RequestParam("customerId") Long customerId,
            @RequestParam(value = "panNumber", required = false) String panNumber,
            @RequestParam(value = "aadhaarNumber", required = false) String aadhaarNumber,
            @RequestParam(value = "gstNumber", required = false) String gstNumber,
            @RequestParam(value = "tdsApplicable", defaultValue = "false") Boolean tdsApplicable,
            @RequestParam(value = "tdsRate", defaultValue = "0") BigDecimal tdsRate,
            @RequestParam(value = "taxResidencyStatus", defaultValue = "RESIDENT")
                    String taxResidencyStatus,
            RedirectAttributes redirectAttributes) {
        try {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            Tenant tenant = tenantService.getTenantById(tenantId);

            Customer customer =
                    customerRepository
                            .findById(customerId)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Customer not found: " + customerId));

            // Find or create CustomerMaster by nationalId
            CustomerMaster cm =
                    customerMasterRepository
                            .findByTenantIdAndNationalId(tenantId, customer.getNationalId())
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "No CBS Customer Master found for national ID: "
                                                            + customer.getNationalId()
                                                            + ". Please create the customer in CBS Customer Master first."));

            // Find or create tax profile
            CustomerTaxProfile tp =
                    taxProfileRepository
                            .findByCustomerMasterIdAndTenantId(cm.getId(), tenantId)
                            .orElseGet(
                                    () ->
                                            CustomerTaxProfile.builder()
                                                    .tenant(tenant)
                                                    .customerMaster(cm)
                                                    .build());

            tp.setPanNumber(panNumber);
            tp.setAadhaarNumber(aadhaarNumber);
            tp.setGstNumber(gstNumber);
            tp.setTdsApplicable(tdsApplicable);
            tp.setTdsRate(tdsRate);
            tp.setTaxResidencyStatus(taxResidencyStatus);

            taxProfileRepository.save(tp);

            redirectAttributes.addFlashAttribute("message", "Tax profile saved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers/" + customerId;
    }
}
