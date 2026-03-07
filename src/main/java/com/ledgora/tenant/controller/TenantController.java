package com.ledgora.tenant.controller;

import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String switchTenant(@PathVariable Long tenantId, HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
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
