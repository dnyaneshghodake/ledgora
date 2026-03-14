package com.ledgora.admin.controller;

import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.repository.AuditLogRepository;
import com.ledgora.auth.entity.Role;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.RoleRepository;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.tenant.service.TenantService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin management controller.
 *
 * <p>Routes:
 *
 * <ul>
 *   <li>GET /admin/users — User management
 *   <li>GET /admin/branches — Branch management
 *   <li>GET /admin/tenants — Tenant configuration
 *   <li>GET /admin/audit — Audit log viewer
 * </ul>
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final TenantRepository tenantRepository;
    private final AuditLogRepository auditLogRepository;
    private final TenantService tenantService;

    public AdminController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            BranchRepository branchRepository,
            TenantRepository tenantRepository,
            AuditLogRepository auditLogRepository,
            TenantService tenantService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.tenantRepository = tenantRepository;
        this.auditLogRepository = auditLogRepository;
        this.tenantService = tenantService;
    }

    /** User management — list all users with roles. */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String userManagement(
            @RequestParam(value = "tab", required = false, defaultValue = "users") String tab,
            Model model) {
        List<User> users = userRepository.findAll();
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("roles", roles);
        model.addAttribute("activeTab", tab);
        model.addAttribute("userCount", users.size());
        return "admin/users";
    }

    /** Branch management — list all branches. */
    @GetMapping("/branches")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String branchManagement(Model model) {
        List<Branch> branches = branchRepository.findAll();
        model.addAttribute("branches", branches);
        model.addAttribute("branchCount", branches.size());
        return "admin/branches";
    }

    /** Tenant configuration — list all tenants (SUPER_ADMIN / ADMIN only). */
    @GetMapping("/tenants")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String tenantConfiguration(Model model) {
        List<Tenant> tenants = tenantRepository.findAll();
        model.addAttribute("tenants", tenants);
        model.addAttribute("tenantCount", tenants.size());
        return "admin/tenants";
    }

    /** Create a new tenant. Maps all entity attributes from the onboarding form. */
    @PostMapping("/tenants/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String createTenant(
            @RequestParam String tenantCode,
            @RequestParam String tenantName,
            @RequestParam(required = false) String regulatoryCode,
            @RequestParam(required = false, defaultValue = "INR") String baseCurrency,
            @RequestParam(required = false, defaultValue = "IN") String country,
            @RequestParam(required = false, defaultValue = "Asia/Kolkata") String timezone,
            @RequestParam(required = false) String effectiveFrom,
            @RequestParam(required = false, defaultValue = "false") Boolean multiBranchEnabled,
            @RequestParam(required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            java.time.LocalDate bizDate =
                    com.ledgora.config.seeder.SeederDateUtil.nextWeekday();
            Tenant tenant = tenantService.createTenant(tenantCode, tenantName, bizDate);
            // Set additional RBI/CBS fields not covered by the base createTenant method
            tenant.setRegulatoryCode(regulatoryCode);
            tenant.setBaseCurrency(baseCurrency);
            tenant.setCountry(country);
            tenant.setTimezone(timezone);
            tenant.setMultiBranchEnabled(multiBranchEnabled);
            tenant.setRemarks(remarks);
            if (effectiveFrom != null && !effectiveFrom.isBlank()) {
                tenant.setEffectiveFrom(java.time.LocalDate.parse(effectiveFrom));
            }
            tenantRepository.save(tenant);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Tenant created: " + tenant.getTenantCode() + " — " + tenant.getTenantName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tenants";
    }

    /** Create a new branch. Maps all entity attributes from the branch form. */
    @PostMapping("/branches/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String createBranch(
            @RequestParam String branchCode,
            @RequestParam String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) String ifscCode,
            @RequestParam(required = false) String micrCode,
            @RequestParam(required = false, defaultValue = "BRANCH") String branchType,
            @RequestParam(required = false) String contactPhone,
            RedirectAttributes redirectAttributes) {
        try {
            if (branchRepository.existsByBranchCode(branchCode)) {
                throw new RuntimeException("Branch code already exists: " + branchCode);
            }
            // Resolve tenant from current session context
            Long tenantId = com.ledgora.tenant.context.TenantContextHolder.getTenantId();
            Tenant tenant = tenantId != null ? tenantService.getTenantById(tenantId) : null;
            Branch branch =
                    Branch.builder()
                            .branchCode(branchCode)
                            .branchName(name)
                            .name(name)
                            .address(address)
                            .city(city)
                            .state(state)
                            .pincode(pincode)
                            .ifscCode(ifscCode)
                            .micrCode(micrCode)
                            .branchType(branchType)
                            .contactPhone(contactPhone)
                            .tenant(tenant)
                            .isActive(true)
                            .build();
            branchRepository.save(branch);
            redirectAttributes.addFlashAttribute(
                    "message", "Branch created: " + branchCode + " — " + name);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    /** Activate a branch. */
    @PostMapping("/branches/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String activateBranch(
            @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Branch branch =
                    branchRepository.findById(id).orElseThrow(() -> new RuntimeException("Branch not found"));
            branch.setIsActive(true);
            branchRepository.save(branch);
            redirectAttributes.addFlashAttribute("message", "Branch activated: " + branch.getBranchCode());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    /** Deactivate a branch. */
    @PostMapping("/branches/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String deactivateBranch(
            @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Branch branch =
                    branchRepository.findById(id).orElseThrow(() -> new RuntimeException("Branch not found"));
            branch.setIsActive(false);
            branchRepository.save(branch);
            redirectAttributes.addFlashAttribute("message", "Branch deactivated: " + branch.getBranchCode());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    /** Activate a tenant (INITIALIZING/INACTIVE → ACTIVE). */
    @PostMapping("/tenants/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String activateTenant(
            @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            tenantService.activateTenant(id);
            redirectAttributes.addFlashAttribute("message", "Tenant activated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tenants";
    }

    /** Deactivate a tenant (ACTIVE → INACTIVE). Business day must be CLOSED. */
    @PostMapping("/tenants/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String deactivateTenant(
            @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            tenantService.deactivateTenant(id);
            redirectAttributes.addFlashAttribute("message", "Tenant deactivated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tenants";
    }

    /** Audit log viewer — paginated, filterable. */
    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'SUPER_ADMIN')")
    public String auditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @RequestParam(value = "entity", required = false) String entity,
            Model model) {
        Page<AuditLog> auditPage;
        if (entity != null && !entity.isBlank()) {
            auditPage =
                    auditLogRepository.findByEntityOrderByTimestampDesc(
                            entity, PageRequest.of(page, size));
            model.addAttribute("filterEntity", entity);
        } else {
            auditPage =
                    auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
        }
        model.addAttribute("auditLogs", auditPage.getContent());
        model.addAttribute("currentPage", auditPage.getNumber());
        model.addAttribute("totalPages", auditPage.getTotalPages());
        model.addAttribute("totalElements", auditPage.getTotalElements());
        return "admin/audit";
    }
}
