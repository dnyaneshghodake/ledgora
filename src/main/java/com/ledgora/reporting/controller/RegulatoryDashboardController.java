package com.ledgora.reporting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgora.eod.entity.EodProcess;
import com.ledgora.eod.repository.EodProcessRepository;
import com.ledgora.reporting.entity.RegulatorySnapshot;
import com.ledgora.reporting.enums.SnapshotStatus;
import com.ledgora.reporting.repository.RegulatorySnapshotRepository;
import com.ledgora.reporting.service.RegulatorySnapshotService;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Finacle-grade Regulatory Dashboard Controller.
 *
 * <p>All data loaded from FINAL snapshot tables only — NO recomputation in UI layer. Tenant-scoped,
 * RBAC-enforced, audit-defensible.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>GET /regulatory/dashboard — combined CRAR + ALM + Trial Balance summary
 *   <li>GET /regulatory/trial-balance — detailed trial balance view
 *   <li>GET /regulatory/crar — detailed CRAR report
 *   <li>GET /regulatory/alm — detailed ALM liquidity report
 *   <li>POST /regulatory/regenerate — admin-only snapshot regeneration
 * </ul>
 */
@Controller
@RequestMapping("/regulatory")
public class RegulatoryDashboardController {

    private static final Logger log = LoggerFactory.getLogger(RegulatoryDashboardController.class);

    private final RegulatorySnapshotRepository snapshotRepository;
    private final RegulatorySnapshotService snapshotService;
    private final TenantRepository tenantRepository;
    private final EodProcessRepository eodProcessRepository;
    private final ObjectMapper objectMapper;

    public RegulatoryDashboardController(
            RegulatorySnapshotRepository snapshotRepository,
            RegulatorySnapshotService snapshotService,
            TenantRepository tenantRepository,
            EodProcessRepository eodProcessRepository,
            ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.snapshotService = snapshotService;
        this.tenantRepository = tenantRepository;
        this.eodProcessRepository = eodProcessRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'OPERATIONS', 'RISK')")
    public String dashboard(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            model.addAttribute("error", "Tenant not found");
            return "regulatory/dashboard";
        }

        LocalDate bizDate = tenant.getCurrentBusinessDate().minusDays(1);
        model.addAttribute("businessDate", bizDate);
        model.addAttribute("tenantName", tenant.getTenantName());

        // Load CRAR snapshot
        loadSnapshot(model, tenantId, bizDate, RegulatorySnapshotService.TYPE_CRAR, "crar");

        // Load ALM snapshot
        loadSnapshot(model, tenantId, bizDate, RegulatorySnapshotService.TYPE_ALM, "alm");

        // Load Trial Balance snapshot
        loadSnapshot(model, tenantId, bizDate, RegulatorySnapshotService.TYPE_TRIAL_BALANCE, "tb");

        return "regulatory/dashboard";
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'OPERATIONS')")
    public String trialBalance(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            model.addAttribute("error", "Tenant not found");
            model.addAttribute("tbAvailable", false);
            return "regulatory/trial-balance";
        }

        LocalDate bizDate = tenant.getCurrentBusinessDate().minusDays(1);
        model.addAttribute("businessDate", bizDate);
        model.addAttribute("tenantName", tenant.getTenantName());

        loadSnapshot(model, tenantId, bizDate, RegulatorySnapshotService.TYPE_TRIAL_BALANCE, "tb");

        return "regulatory/trial-balance";
    }

    @GetMapping("/crar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK')")
    public String crar(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            model.addAttribute("error", "Tenant not found");
            model.addAttribute("crarAvailable", false);
            return "regulatory/crar-report";
        }

        LocalDate bizDate = tenant.getCurrentBusinessDate().minusDays(1);
        model.addAttribute("businessDate", bizDate);
        model.addAttribute("tenantName", tenant.getTenantName());

        loadSnapshot(model, tenantId, bizDate, RegulatorySnapshotService.TYPE_CRAR, "crar");

        return "regulatory/crar-report";
    }

    @GetMapping("/alm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'RISK', 'OPERATIONS')")
    public String alm(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            model.addAttribute("error", "Tenant not found");
            model.addAttribute("almAvailable", false);
            return "regulatory/alm-report";
        }

        LocalDate bizDate = tenant.getCurrentBusinessDate().minusDays(1);
        model.addAttribute("businessDate", bizDate);
        model.addAttribute("tenantName", tenant.getTenantName());

        loadSnapshot(model, tenantId, bizDate, RegulatorySnapshotService.TYPE_ALM, "alm");

        return "regulatory/alm-report";
    }

    @PostMapping("/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    public String regenerate(
            @RequestParam String type, HttpSession session, RedirectAttributes redirectAttributes) {
        Long tenantId = resolveTenantId(session);
        try {
            Tenant tenant =
                    tenantRepository
                            .findById(tenantId)
                            .orElseThrow(() -> new RuntimeException("Tenant not found"));
            // Use previous business date (current date is the next open day)
            LocalDate snapshotDate = tenant.getCurrentBusinessDate().minusDays(1);
            snapshotService.generateAllSnapshots(tenantId, snapshotDate);
            redirectAttributes.addFlashAttribute(
                    "message", "Regulatory snapshots regenerated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Regeneration failed: " + e.getMessage());
        }
        return "redirect:/regulatory/dashboard";
    }

    /** Load a FINAL snapshot and parse its JSON into the model. */
    @SuppressWarnings("unchecked")
    private void loadSnapshot(
            Model model, Long tenantId, LocalDate bizDate, String type, String prefix) {
        try {
            RegulatorySnapshot snapshot =
                    snapshotRepository
                            .findByTenantIdAndBusinessDateAndReportType(tenantId, bizDate, type)
                            .orElse(null);

            if (snapshot != null && snapshot.getStatus() == SnapshotStatus.FINAL) {
                Map<String, Object> data =
                        objectMapper.readValue(snapshot.getJsonPayload(), Map.class);
                model.addAttribute(prefix + "Data", data);
                model.addAttribute(prefix + "Snapshot", snapshot);
                model.addAttribute(prefix + "Available", true);
            } else {
                model.addAttribute(prefix + "Available", false);
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to load {} snapshot for tenant {} date {}: {}",
                    type,
                    tenantId,
                    bizDate,
                    e.getMessage());
            model.addAttribute(prefix + "Available", false);
        }
    }

    private Long resolveTenantId(HttpSession session) {
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
            throw new IllegalStateException("Tenant context not set for regulatory dashboard");
        }
        return tenantId;
    }
}
