package com.ledgora.dashboard.controller;

import com.ledgora.dashboard.dto.DashboardDTO;
import com.ledgora.dashboard.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard controller — extracts the authenticated user's roles from the
 * SecurityContext and passes them to the JSP view so that role-based
 * conditional rendering can work via JSTL.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardDTO dashboard = dashboardService.getDashboardData();
        model.addAttribute("dashboard", dashboard);

        // Extract roles from SecurityContext and pass to JSP for role-aware rendering
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            List<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            model.addAttribute("userRoles", roles);

            // Convenience booleans for JSP conditional checks
            model.addAttribute("isAdmin",    roles.contains("ROLE_ADMIN"));
            model.addAttribute("isManager",  roles.contains("ROLE_MANAGER"));
            model.addAttribute("isTeller",   roles.contains("ROLE_TELLER"));
            model.addAttribute("isCustomer", roles.contains("ROLE_CUSTOMER"));
        }

        return "dashboard/dashboard";
    }
}
