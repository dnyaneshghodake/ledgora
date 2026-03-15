package com.ledgora.reporting.controller;

import com.ledgora.reporting.entity.FinancialStatementSnapshot;
import com.ledgora.reporting.enums.SnapshotStatus;
import com.ledgora.reporting.enums.StatementType;
import com.ledgora.reporting.repository.FinancialStatementSnapshotRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Finacle-grade Financial Statement UI Controller.
 *
 * <p>P&L and Balance Sheet derive STRICTLY from FinancialStatementSnapshot (FINAL). NO
 * recomputation in UI. Snapshot data includes section breakdowns, totals, and validation flags.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /financial/pl — Profit & Loss Statement
 *   <li>GET /financial/balance-sheet — Balance Sheet
 * </ul>
 */
@Controller
@RequestMapping("/financial")
public class FinancialStatementController {

    private static final Logger log =
            LoggerFactory.getLogger(FinancialStatementController.class);

    private final FinancialStatementSnapshotRepository snapshotRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    public FinancialStatementController(
            FinancialStatementSnapshotRepository snapshotRepository,
            TenantRepository tenantRepository,
            ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.tenantRepository = tenantRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/pl")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'OPERATIONS')")
    public String profitLoss(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            model.addAttribute("error", "Tenant not found");
            model.addAttribute("pnlAvailable", false);
            return "financial/profit-loss";
        }

        LocalDate bizDate = tenant.getCurrentBusinessDate().minusDays(1);
        model.addAttribute("businessDate", bizDate);
        model.addAttribute("tenantName", tenant.getTenantName());

        loadSnapshot(model, tenantId, bizDate, StatementType.PNL, "pnl");
        return "financial/profit-loss";
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR', 'OPERATIONS')")
    public String balanceSheet(Model model, HttpSession session) {
        Long tenantId = resolveTenantId(session);
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            model.addAttribute("error", "Tenant not found");
            model.addAttribute("bsAvailable", false);
            return "financial/balance-sheet";
        }

        LocalDate bizDate = tenant.getCurrentBusinessDate().minusDays(1);
        model.addAttribute("businessDate", bizDate);
        model.addAttribute("tenantName", tenant.getTenantName());

        loadSnapshot(model, tenantId, bizDate, StatementType.BALANCE_SHEET, "bs");
        return "financial/balance-sheet";
    }

    @SuppressWarnings("unchecked")
    private void loadSnapshot(
            Model model, Long tenantId, LocalDate bizDate,
            StatementType type, String prefix) {
        try {
            FinancialStatementSnapshot snapshot =
                    snapshotRepository
                            .findByTenantIdAndBusinessDateAndStatementTypeAndStatus(
                                    tenantId, bizDate, type, SnapshotStatus.FINAL)
                            .orElse(null);

            if (snapshot != null) {
                Map<String, Object> data =
                        objectMapper.readValue(snapshot.getJsonPayload(), Map.class);
                model.addAttribute(prefix + "Data", data);
                model.addAttribute(prefix + "Snapshot", snapshot);
                model.addAttribute(prefix + "Available", true);
            } else {
                model.addAttribute(prefix + "Available", false);
            }
        } catch (Exception e) {
            log.warn("Failed to load {} snapshot: {}", type, e.getMessage());
            model.addAttribute(prefix + "Available", false);
        }
    }

    private Long resolveTenantId(HttpSession session) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            Object sessionTenantId = session.getAttribute("tenantId");
            if (sessionTenantId instanceof Number n) tenantId = n.longValue();
            else if (sessionTenantId instanceof String s && !s.isBlank()) tenantId = Long.valueOf(s);
        }
        if (tenantId == null) throw new IllegalStateException("Tenant context not set");
        return tenantId;
    }
}
