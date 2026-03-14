package com.ledgora.teller.service;

import com.ledgora.common.exception.BusinessException;
import com.ledgora.teller.entity.CashDenominationTxn;
import com.ledgora.teller.entity.CashDifferenceLog;
import com.ledgora.teller.entity.TellerSession;
import com.ledgora.teller.entity.VaultMaster;
import com.ledgora.teller.repository.CashDenominationTxnRepository;
import com.ledgora.teller.repository.CashDifferenceLogRepository;
import com.ledgora.teller.repository.TellerSessionRepository;
import com.ledgora.teller.repository.VaultMasterRepository;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.service.TenantService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * TellerReportService — CBS-grade teller operations reporting.
 *
 * <p>Provides data for:
 *
 * <ul>
 *   <li>Teller Cash Position Report (per teller per day)
 *   <li>Vault Position Report (per branch)
 *   <li>Daily Cash Movement (credits/debits summary)
 *   <li>Denomination Summary (aggregated denomination breakdown)
 *   <li>Cash Short/Excess Report (reconciliation mismatches)
 * </ul>
 */
@Service
public class TellerReportService {

    private final TellerSessionRepository tellerSessionRepository;
    private final VaultMasterRepository vaultMasterRepository;
    private final CashDenominationTxnRepository cashDenominationTxnRepository;
    private final CashDifferenceLogRepository cashDifferenceLogRepository;
    private final TenantService tenantService;

    public TellerReportService(
            TellerSessionRepository tellerSessionRepository,
            VaultMasterRepository vaultMasterRepository,
            CashDenominationTxnRepository cashDenominationTxnRepository,
            CashDifferenceLogRepository cashDifferenceLogRepository,
            TenantService tenantService) {
        this.tellerSessionRepository = tellerSessionRepository;
        this.vaultMasterRepository = vaultMasterRepository;
        this.cashDenominationTxnRepository = cashDenominationTxnRepository;
        this.cashDifferenceLogRepository = cashDifferenceLogRepository;
        this.tenantService = tenantService;
    }

    /** Teller Cash Position: all sessions for the current tenant on a given business date. */
    public List<Map<String, Object>> getTellerCashPositionReport(LocalDate businessDate) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<TellerSession> sessions =
                tellerSessionRepository.findByTenantIdAndBusinessDate(tenantId, businessDate);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TellerSession s : sessions) {
            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", s.getId());
            row.put("businessDate", s.getBusinessDate());
            row.put("state", s.getState() != null ? s.getState().name() : "--");
            row.put("openingBalance", s.getOpeningBalance());
            row.put("currentBalance", s.getCurrentBalance());
            row.put("totalCredit", s.getTotalCreditToday());
            row.put("totalDebit", s.getTotalDebitToday());
            try {
                row.put(
                        "tellerName",
                        s.getTeller() != null && s.getTeller().getUser() != null
                                ? s.getTeller().getUser().getFullName()
                                : "--");
                row.put("branchCode", s.getBranch() != null ? s.getBranch().getBranchCode() : "--");
            } catch (Exception e) {
                row.put("tellerName", "--");
                row.put("branchCode", "--");
            }
            rows.add(row);
        }
        return rows;
    }

    /** Vault Position: current balance and holding limit for all vaults in the current tenant. */
    public List<Map<String, Object>> getVaultPositionReport() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<VaultMaster> vaults = vaultMasterRepository.findByTenantId(tenantId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (VaultMaster v : vaults) {
            Map<String, Object> row = new HashMap<>();
            row.put("vaultId", v.getId());
            row.put("currentBalance", v.getCurrentBalance());
            row.put("holdingLimit", v.getHoldingLimit());
            row.put("dualCustody", v.getDualCustodyFlag());
            try {
                row.put("branchCode", v.getBranch() != null ? v.getBranch().getBranchCode() : "--");
                row.put("branchName", v.getBranch() != null ? v.getBranch().getName() : "--");
            } catch (Exception e) {
                row.put("branchCode", "--");
                row.put("branchName", "--");
            }
            BigDecimal utilization = BigDecimal.ZERO;
            if (v.getHoldingLimit() != null
                    && v.getHoldingLimit().compareTo(BigDecimal.ZERO) > 0
                    && v.getCurrentBalance() != null) {
                utilization =
                        v.getCurrentBalance()
                                .multiply(new BigDecimal("100"))
                                .divide(v.getHoldingLimit(), 2, java.math.RoundingMode.HALF_UP);
            }
            row.put("utilization", utilization);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Daily Cash Movement: aggregated credits and debits across all teller sessions for a business
     * date.
     */
    public Map<String, Object> getDailyCashMovementReport(LocalDate businessDate) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<TellerSession> sessions =
                tellerSessionRepository.findByTenantIdAndBusinessDate(tenantId, businessDate);
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalOpening = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;
        int sessionCount = 0;
        for (TellerSession s : sessions) {
            totalCredits =
                    totalCredits.add(
                            s.getTotalCreditToday() != null
                                    ? s.getTotalCreditToday()
                                    : BigDecimal.ZERO);
            totalDebits =
                    totalDebits.add(
                            s.getTotalDebitToday() != null
                                    ? s.getTotalDebitToday()
                                    : BigDecimal.ZERO);
            totalOpening =
                    totalOpening.add(
                            s.getOpeningBalance() != null
                                    ? s.getOpeningBalance()
                                    : BigDecimal.ZERO);
            totalCurrent =
                    totalCurrent.add(
                            s.getCurrentBalance() != null
                                    ? s.getCurrentBalance()
                                    : BigDecimal.ZERO);
            sessionCount++;
        }
        Map<String, Object> report = new HashMap<>();
        report.put("businessDate", businessDate);
        report.put("sessionCount", sessionCount);
        report.put("totalCredits", totalCredits);
        report.put("totalDebits", totalDebits);
        report.put("totalOpening", totalOpening);
        report.put("totalCurrent", totalCurrent);
        report.put("netMovement", totalCredits.subtract(totalDebits));
        return report;
    }

    /**
     * Denomination Summary: aggregated denomination breakdown for all transactions in a session.
     * Includes tenant isolation check to prevent IDOR (CWE-639).
     */
    public List<Map<String, Object>> getDenominationSummary(Long sessionId) {
        // Tenant isolation: verify session belongs to current tenant before returning data
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        TellerSession session =
                tellerSessionRepository
                        .findById(sessionId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "NO_TELLER_SESSION",
                                                "No teller session found: " + sessionId));
        if (session.getTenant() == null || !session.getTenant().getId().equals(tenantId)) {
            throw new BusinessException(
                    "TENANT_MISMATCH",
                    "Session does not belong to current tenant. sessionId=" + sessionId);
        }

        List<CashDenominationTxn> txns = cashDenominationTxnRepository.findBySessionId(sessionId);
        Map<BigDecimal, int[]> denomMap = new HashMap<>();
        for (CashDenominationTxn t : txns) {
            denomMap.computeIfAbsent(t.getDenominationValue(), k -> new int[] {0})[0] +=
                    (t.getCount() != null ? t.getCount() : 0);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        denomMap.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .forEach(
                        entry -> {
                            Map<String, Object> row = new HashMap<>();
                            row.put("denomination", entry.getKey());
                            row.put("totalCount", entry.getValue()[0]);
                            row.put(
                                    "totalAmount",
                                    entry.getKey()
                                            .multiply(BigDecimal.valueOf(entry.getValue()[0])));
                            rows.add(row);
                        });
        return rows;
    }

    /** Cash Short/Excess Report: all reconciliation mismatches for a business date. */
    public List<Map<String, Object>> getCashShortExcessReport(LocalDate businessDate) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<TellerSession> sessions =
                tellerSessionRepository.findByTenantIdAndBusinessDate(tenantId, businessDate);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TellerSession s : sessions) {
            List<CashDifferenceLog> diffs = cashDifferenceLogRepository.findBySessionId(s.getId());
            for (CashDifferenceLog d : diffs) {
                Map<String, Object> row = new HashMap<>();
                row.put("sessionId", s.getId());
                row.put("businessDate", s.getBusinessDate());
                row.put("declaredAmount", d.getDeclaredAmount());
                row.put("systemAmount", d.getSystemAmount());
                row.put("difference", d.getDifference());
                row.put("type", d.getType() != null ? d.getType().name() : "--");
                row.put("resolved", d.getResolvedFlag());
                try {
                    row.put(
                            "tellerName",
                            s.getTeller() != null && s.getTeller().getUser() != null
                                    ? s.getTeller().getUser().getFullName()
                                    : "--");
                    row.put(
                            "branchCode",
                            s.getBranch() != null ? s.getBranch().getBranchCode() : "--");
                } catch (Exception e) {
                    row.put("tellerName", "--");
                    row.put("branchCode", "--");
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
