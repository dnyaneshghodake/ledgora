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
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin management controller.
 * Routes:
 *   GET /admin/users      — User management
 *   GET /admin/branches   — Branch management
 *   GET /admin/tenants    — Tenant configuration
 *   GET /admin/audit      — Audit log viewer
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

    public AdminController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            BranchRepository branchRepository,
            TenantRepository tenantRepository,
            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.tenantRepository = tenantRepository;
        this.auditLogRepository = auditLogRepository;
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
        return "admin/admin-users";
    }

    /** Branch management — list all branches. */
    @GetMapping("/branches")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String branchManagement(Model model) {
        List<Branch> branches = branchRepository.findAll();
        model.addAttribute("branches", branches);
        model.addAttribute("branchCount", branches.size());
        return "admin/admin-branches";
    }

    /** Tenant configuration — list all tenants (SUPER_ADMIN / ADMIN only). */
    @GetMapping("/tenants")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String tenantConfiguration(Model model) {
        List<Tenant> tenants = tenantRepository.findAll();
        model.addAttribute("tenants", tenants);
        model.addAttribute("tenantCount", tenants.size());
        return "admin/admin-tenants";
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
            auditPage = auditLogRepository.findByEntityOrderByTimestampDesc(
                    entity, PageRequest.of(page, size));
            model.addAttribute("filterEntity", entity);
        } else {
            auditPage = auditLogRepository.findAllByOrderByTimestampDesc(
                    PageRequest.of(page, size));
        }
        model.addAttribute("auditLogs", auditPage.getContent());
        model.addAttribute("currentPage", auditPage.getNumber());
        model.addAttribute("totalPages", auditPage.getTotalPages());
        model.addAttribute("totalElements", auditPage.getTotalElements());
        return "admin/admin-audit";
    }
}
