package com.ledgora.config;

import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * CBS Admin Controller - manages tenant, branch, user, and audit screens.
 * All endpoints require ADMIN role.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final TenantService tenantService;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminController(TenantService tenantService,
                           BranchRepository branchRepository,
                           UserRepository userRepository,
                           AuditService auditService) {
        this.tenantService = tenantService;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ── Tenant Management ──

    @GetMapping("/tenants")
    public String listTenants(Model model) {
        List<Tenant> tenants = tenantService.getAllTenants();
        model.addAttribute("tenants", tenants);
        return "admin/tenants";
    }

    @PostMapping("/tenants/create")
    public String createTenant(@RequestParam String tenantCode,
                               @RequestParam String tenantName,
                               RedirectAttributes redirectAttributes) {
        try {
            tenantService.createTenant(tenantCode, tenantName, LocalDate.now());
            redirectAttributes.addFlashAttribute("message", "Tenant created: " + tenantName);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create tenant: " + e.getMessage());
        }
        return "redirect:/admin/tenants";
    }

    // ── Branch Management ──

    @GetMapping("/branches")
    public String listBranches(Model model) {
        List<Branch> branches = branchRepository.findAll();
        model.addAttribute("branches", branches);
        return "admin/branches";
    }

    @PostMapping("/branches/create")
    public String createBranch(@RequestParam String branchCode,
                               @RequestParam String name,
                               @RequestParam(required = false) String address,
                               RedirectAttributes redirectAttributes) {
        try {
            if (branchRepository.findByBranchCode(branchCode).isPresent()) {
                throw new RuntimeException("Branch code already exists: " + branchCode);
            }
            Branch branch = Branch.builder()
                    .branchCode(branchCode)
                    .name(name)
                    .address(address)
                    .isActive(true)
                    .build();
            branchRepository.save(branch);
            redirectAttributes.addFlashAttribute("message", "Branch created: " + name);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create branch: " + e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    // ── User Management ──

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    // ── Audit Logs ──

    @GetMapping("/audit")
    public String viewAuditLogs(@RequestParam(required = false) String entity, Model model) {
        List<AuditLog> logs;
        if (entity != null && !entity.isBlank()) {
            logs = auditService.getByEntity(entity);
            model.addAttribute("filterEntity", entity);
        } else {
            logs = auditService.getAuditLogs();
        }
        model.addAttribute("auditLogs", logs);
        return "admin/audit";
    }
}
