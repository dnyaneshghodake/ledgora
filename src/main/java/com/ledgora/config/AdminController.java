package com.ledgora.config;

import com.ledgora.audit.entity.AuditLog;
import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.Role;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.RoleRepository;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.branch.entity.Branch;
import com.ledgora.branch.repository.BranchRepository;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    private final RoleRepository roleRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(TenantService tenantService,
                           BranchRepository branchRepository,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           AuditService auditService,
                           PasswordEncoder passwordEncoder) {
        this.tenantService = tenantService;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
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

    // ── User Management ── (PART 8)

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("roles", roles);
        return "admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        model.addAttribute("editUser", userOpt.get());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id,
                           @RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam(required = false) String branchCode,
                           @RequestParam(required = false) String password,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setFullName(fullName);
            user.setEmail(email);
            user.setBranchCode(branchCode);
            if (password != null && !password.isBlank()) {
                user.setPassword(passwordEncoder.encode(password));
            }
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("message", "User updated: " + user.getUsername());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setIsActive(!user.getIsActive());
            userRepository.save(user);
            String status = user.getIsActive() ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("message", "User " + status + ": " + user.getUsername());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to toggle user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            userRepository.delete(user);
            redirectAttributes.addFlashAttribute("message", "User deleted: " + user.getUsername());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ── Role Management ── (PART 8)

    @GetMapping("/roles")
    public String listRoles(Model model) {
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("roles", roles);
        return "admin/users";
    }

    @PostMapping("/roles/{id}/edit")
    public String editRole(@PathVariable Long id,
                           @RequestParam String description,
                           RedirectAttributes redirectAttributes) {
        try {
            Role role = roleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            role.setDescription(description);
            roleRepository.save(role);
            redirectAttributes.addFlashAttribute("message", "Role updated: " + role.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update role: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/roles/{id}/delete")
    public String deleteRole(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Role role = roleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            roleRepository.delete(role);
            redirectAttributes.addFlashAttribute("message", "Role deleted: " + role.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete role: " + e.getMessage());
        }
        return "redirect:/admin/users";
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
