package com.ledgora.auth.controller;

import com.ledgora.audit.service.AuditService;
import com.ledgora.auth.entity.Role;
import com.ledgora.auth.entity.User;
import com.ledgora.auth.repository.RoleRepository;
import com.ledgora.auth.repository.UserRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * User & Role Management Controller — CBS-grade admin operations.
 *
 * <p>Finacle-grade user lifecycle management:
 *
 * <ul>
 *   <li>GET /admin/users — user list with role badges (tenant-scoped)
 *   <li>GET /admin/users/create — new user form
 *   <li>POST /admin/users/create — create user with role assignment
 *   <li>GET /admin/users/{id}/edit — edit user form
 *   <li>POST /admin/users/{id}/edit — update user details + roles
 *   <li>POST /admin/users/{id}/toggle — activate/deactivate user
 *   <li>POST /admin/users/{id}/unlock — unlock locked user
 *   <li>GET /admin/users?tab=roles — role registry view
 * </ul>
 *
 * <p>Access: ADMIN, TENANT_ADMIN, SUPER_ADMIN only. All mutations are audit-logged.
 */
@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private static final Logger log = LoggerFactory.getLogger(UserManagementController.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserManagementController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /** User list — tenant-scoped, with optional role tab. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String userList(
            @RequestParam(required = false) String tab, Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);

        List<User> users = userRepository.findByTenantId(tenantId);
        List<Role> allRoles = roleRepository.findAll();

        model.addAttribute("users", users);
        model.addAttribute("allRoles", allRoles);
        model.addAttribute("tab", tab != null ? tab : "users");

        return "admin/users";
    }

    /** New user form. */
    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String createForm(Model model) {
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    /** Create new user with role assignment. */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) List<Long> roleIds,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);

        if (userRepository.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("error", "Username already exists: " + username);
            return "redirect:/admin/users/create";
        }

        User user =
                User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(password))
                        .fullName(fullName)
                        .email(email)
                        .phone(phone)
                        .branchCode(branchCode)
                        .isActive(true)
                        .isLocked(false)
                        .build();

        // Set tenant from session
        com.ledgora.tenant.entity.Tenant tenant = new com.ledgora.tenant.entity.Tenant();
        tenant.setId(tenantId);
        user.setTenant(tenant);

        // Assign roles
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
            user.setRoles(roles);
        }

        user = userRepository.save(user);

        auditService.logEvent(
                null,
                "USER_CREATED",
                "USER",
                user.getId(),
                "User " + username + " created with roles: " + user.getRoles(),
                null);

        redirectAttributes.addFlashAttribute(
                "message", "User " + username + " created successfully");
        return "redirect:/admin/users";
    }

    /** Edit user form. */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String editForm(@PathVariable Long id, Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        User user = userRepository.findById(id).orElse(null);
        if (user == null
                || user.getTenant() == null
                || !user.getTenant().getId().equals(tenantId)) {
            model.addAttribute("error", "User not found");
            return "admin/users";
        }
        model.addAttribute("editUser", user);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    /** Update user details + roles. */
    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) List<Long> roleIds,
            @RequestParam(required = false) String newPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        User user = userRepository.findById(id).orElse(null);
        if (user == null
                || user.getTenant() == null
                || !user.getTenant().getId().equals(tenantId)) {
            redirectAttributes.addFlashAttribute("error", "User not found or access denied");
            return "redirect:/admin/users";
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setBranchCode(branchCode);

        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        if (roleIds != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
            user.setRoles(roles);
        }

        userRepository.save(user);

        auditService.logEvent(
                null,
                "USER_UPDATED",
                "USER",
                user.getId(),
                "User " + user.getUsername() + " updated. Roles: " + user.getRoles(),
                null);

        redirectAttributes.addFlashAttribute("message", "User " + user.getUsername() + " updated");
        return "redirect:/admin/users";
    }

    /** Toggle user active/inactive. */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String toggleUser(
            @PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        User user = userRepository.findById(id).orElse(null);
        if (user == null
                || user.getTenant() == null
                || !user.getTenant().getId().equals(tenantId)) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);

        String action = user.getIsActive() ? "ACTIVATED" : "DEACTIVATED";
        auditService.logEvent(
                null,
                "USER_" + action,
                "USER",
                user.getId(),
                "User " + user.getUsername() + " " + action.toLowerCase(),
                null);

        redirectAttributes.addFlashAttribute(
                "message", "User " + user.getUsername() + " " + action.toLowerCase());
        return "redirect:/admin/users";
    }

    /** Unlock a locked user (failed login attempts). */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN', 'SUPER_ADMIN')")
    public String unlockUser(
            @PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        User user = userRepository.findById(id).orElse(null);
        if (user == null
                || user.getTenant() == null
                || !user.getTenant().getId().equals(tenantId)) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        user.setIsLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        auditService.logEvent(
                null,
                "USER_UNLOCKED",
                "USER",
                user.getId(),
                "User " + user.getUsername() + " unlocked by admin",
                null);

        redirectAttributes.addFlashAttribute("message", "User " + user.getUsername() + " unlocked");
        return "redirect:/admin/users";
    }

    private Long resolveTenantId(HttpSession session) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            Object sessionTenantId = session.getAttribute("tenantId");
            if (sessionTenantId instanceof Number n) tenantId = n.longValue();
            else if (sessionTenantId instanceof String s && !s.isBlank())
                tenantId = Long.valueOf(s);
        }
        if (tenantId == null) throw new IllegalStateException("Tenant context not set");
        return tenantId;
    }
}
