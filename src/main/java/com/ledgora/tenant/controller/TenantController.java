package com.ledgora.tenant.controller;

import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Controller for tenant switching (for MULTI tenant users).
 */
@Controller
@RequestMapping("/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/switch/{tenantId}")
    public String switchTenantLegacy(@PathVariable Long tenantId,
                                     RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error",
                "Direct tenant switch URL is disabled. Please use the tenant switch menu.");
        return "redirect:/dashboard";
    }

    @PostMapping("/switch")
    public String switchTenant(@RequestParam Long tenantId, HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            Object tenantScope = session.getAttribute("tenantScope");
            if (!"MULTI".equals(tenantScope)) {
                redirectAttributes.addFlashAttribute("error", "Tenant switch is not allowed for this user.");
                return "redirect:/dashboard";
            }

            Object availableTenantsObj = session.getAttribute("availableTenants");
            if (availableTenantsObj instanceof List<?> availableTenants) {
                boolean allowed = availableTenants.stream().anyMatch(t -> {
                    if (t instanceof Tenant tenant) {
                        return Objects.equals(tenant.getId(), tenantId);
                    }
                    if (t instanceof Map<?, ?> tenantMap) {
                        Object id = tenantMap.get("id");
                        return id != null && Objects.equals(String.valueOf(id), String.valueOf(tenantId));
                    }
                    return false;
                });
                if (!allowed) {
                    redirectAttributes.addFlashAttribute("error", "You are not authorized to switch to this tenant.");
                    return "redirect:/dashboard";
                }
            }

            Tenant tenant = tenantService.getTenantById(tenantId);
            // Update session
            session.setAttribute("tenantId", tenant.getId());
            session.setAttribute("tenantName", tenant.getTenantName());
            session.setAttribute("tenantCode", tenant.getTenantCode());
            session.setAttribute("businessDate", tenant.getCurrentBusinessDate().toString());
            session.setAttribute("businessDateStatus", tenant.getDayStatus().name());
            // Update ThreadLocal context
            TenantContextHolder.setTenantId(tenant.getId());
            redirectAttributes.addFlashAttribute("message", "Switched to tenant: " + tenant.getTenantName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to switch tenant: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}
