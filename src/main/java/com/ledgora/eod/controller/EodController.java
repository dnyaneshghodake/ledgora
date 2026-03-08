package com.ledgora.eod.controller;

import com.ledgora.eod.service.EodValidationService;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.service.TenantService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for End of Day (EOD) UI screens.
 * Routes: /eod/validate, /eod/run, /eod/status
 */
@Controller
@RequestMapping("/eod")
public class EodController {

    private final EodValidationService eodValidationService;
    private final TenantService tenantService;

    public EodController(EodValidationService eodValidationService, TenantService tenantService) {
        this.eodValidationService = eodValidationService;
        this.tenantService = tenantService;
    }

    @GetMapping("/validate")
    public String validateEod(Model model, HttpSession session) {
        Tenant tenant = resolveCurrentTenant(session);
        List<String> errors = eodValidationService.validateEod(tenant.getId(), tenant.getCurrentBusinessDate());
        model.addAttribute("businessDate", tenant.getCurrentBusinessDate());
        model.addAttribute("businessDateStatus", tenant.getDayStatus().name());
        model.addAttribute("tenantName", tenant.getTenantName());
        model.addAttribute("validationErrors", errors);

        // Backward-compatible flags used by existing JSP
        model.addAttribute("allVouchersPosted", errors.stream().noneMatch(e -> e.toLowerCase().contains("voucher")));
        model.addAttribute("branchBalanced", errors.stream().noneMatch(e -> e.toLowerCase().contains("branch")));
        model.addAttribute("clearingGlZero", errors.stream().noneMatch(e -> e.toLowerCase().contains("clearing")));
        model.addAttribute("noPendingAuth", errors.stream().noneMatch(e -> e.toLowerCase().contains("pending")));
        return "eod/eod-validate";
    }

    @GetMapping("/run")
    public String runEodForm(Model model, HttpSession session) {
        Tenant tenant = resolveCurrentTenant(session);
        model.addAttribute("businessDate", tenant.getCurrentBusinessDate());
        model.addAttribute("businessDateStatus", tenant.getDayStatus().name());
        model.addAttribute("tenantName", tenant.getTenantName());
        return "eod/eod-run";
    }

    @PostMapping("/run")
    public String runEod(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Tenant tenant = resolveCurrentTenant(session);
            eodValidationService.runEod(tenant.getId());
            Tenant refreshed = tenantService.getTenantById(tenant.getId());
            session.setAttribute("businessDate", String.valueOf(refreshed.getCurrentBusinessDate()));
            session.setAttribute("businessDateStatus", refreshed.getDayStatus().name());
            redirectAttributes.addFlashAttribute("message",
                    "EOD completed successfully. New business date: " + refreshed.getCurrentBusinessDate());
            return "redirect:/eod/status";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "EOD failed: " + e.getMessage());
            return "redirect:/eod/run";
        }
    }

    @GetMapping("/status")
    public String businessDateStatus(Model model, HttpSession session) {
        Tenant tenant = resolveCurrentTenant(session);
        LocalDate businessDate = tenant.getCurrentBusinessDate();
        model.addAttribute("businessDate", businessDate);
        model.addAttribute("businessDateStatus", tenant.getDayStatus().name());
        model.addAttribute("tenantName", tenant.getTenantName());
        model.addAttribute("canRunEod", eodValidationService.canRunEod(tenant.getId()));
        return "eod/eod-status";
    }

    private Tenant resolveCurrentTenant(HttpSession session) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            Object sessionTenantId = session.getAttribute("tenantId");
            if (sessionTenantId instanceof Number n) {
                tenantId = n.longValue();
            } else if (sessionTenantId instanceof String s && !s.isBlank()) {
                tenantId = Long.valueOf(s);
            }
        }
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set for EOD operation");
        }
        return tenantService.getTenantById(tenantId);
    }
}
