package com.ledgora.diagnostics.service;

import com.ledgora.diagnostics.dto.QueryPlanResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Analyzes execution plans for critical CBS queries to detect performance risks.
 *
 * <p>Active only in the "stress" profile. Uses JDBC {@code EXPLAIN} to capture query plans from the
 * database engine (H2 or PostgreSQL compatible). Parses plan output for risk indicators: TABLE SCAN,
 * SEQ SCAN, missing index usage.
 *
 * <p>Critical queries cover: EOD validation, IBT reconciliation, suspense dashboard, clearing
 * engine, hard ceiling monitoring, and audit exploration.
 *
 * <p>Does NOT modify any data. Pure diagnostic read-only.
 */
@Service
@Profile("stress")
public class QueryPlanAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(QueryPlanAnalyzerService.class);

    /** Risk indicators that suggest a full table scan (case-insensitive matching). */
    private static final String[] HIGH_RISK_PATTERNS = {
        "TABLE SCAN", "SEQ SCAN", "FULL SCAN", "tableScan"
    };

    /** Indicators that an index is being used. */
    private static final String[] INDEX_PATTERNS = {
        "INDEX", "index", "COVERING", "RANGE SCAN", "PUBLIC."
    };

    private final JdbcTemplate jdbcTemplate;

    public QueryPlanAnalyzerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Analyze all critical query execution plans.
     *
     * @return list of plan analysis results with risk classification
     */
    public List<QueryPlanResult> analyzeAllCriticalQueries() {
        Map<String, String> criticalQueries = getCriticalQueries();
        List<QueryPlanResult> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : criticalQueries.entrySet()) {
            QueryPlanResult result = analyzeQuery(entry.getKey(), entry.getValue());
            results.add(result);

            if ("HIGH".equals(result.getRiskLevel())) {
                log.warn(
                        "QUERY PLAN RISK HIGH: {} — {}",
                        result.getQueryName(),
                        result.getRiskDetails());
            }
        }

        long highCount = results.stream().filter(r -> "HIGH".equals(r.getRiskLevel())).count();
        long medCount = results.stream().filter(r -> "MEDIUM".equals(r.getRiskLevel())).count();
        log.info(
                "Query plan analysis complete: {} queries, {} HIGH risk, {} MEDIUM risk",
                results.size(),
                highCount,
                medCount);

        return results;
    }

    /**
     * Analyze a single query's execution plan.
     *
     * @param queryName human-readable identifier
     * @param sql the SQL to explain (WITHOUT the EXPLAIN prefix)
     * @return analysis result with risk classification
     */
    public QueryPlanResult analyzeQuery(String queryName, String sql) {
        String explainSql = "EXPLAIN " + sql;
        String planText;

        try {
            List<String> planRows = jdbcTemplate.queryForList(explainSql, String.class);
            planText = String.join("\n", planRows);
        } catch (Exception e) {
            // Some EXPLAIN variants return structured data — try EXPLAIN PLAN FOR (H2)
            try {
                List<Map<String, Object>> planMaps =
                        jdbcTemplate.queryForList("EXPLAIN PLAN FOR " + sql);
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> row : planMaps) {
                    row.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
                }
                planText = sb.toString();
            } catch (Exception e2) {
                planText = "EXPLAIN failed: " + e.getMessage();
            }
        }

        // Classify risk
        String planUpper = planText.toUpperCase();
        boolean tableScan = false;
        boolean indexUsed = false;
        List<String> risks = new ArrayList<>();

        for (String pattern : HIGH_RISK_PATTERNS) {
            if (planUpper.contains(pattern.toUpperCase())) {
                tableScan = true;
                risks.add("TABLE SCAN detected (" + pattern + ")");
            }
        }

        for (String pattern : INDEX_PATTERNS) {
            if (planUpper.contains(pattern.toUpperCase())) {
                indexUsed = true;
            }
        }

        String riskLevel;
        if (tableScan && !indexUsed) {
            riskLevel = "HIGH";
        } else if (tableScan && indexUsed) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        String riskDetails = risks.isEmpty() ? "No risk indicators detected" : String.join("; ", risks);

        return QueryPlanResult.builder()
                .queryName(queryName)
                .sql(sql)
                .executionPlan(planText)
                .riskLevel(riskLevel)
                .riskDetails(riskDetails)
                .indexUsed(indexUsed)
                .tableScanDetected(tableScan)
                .build();
    }

    /**
     * Critical queries that must be validated for index usage.
     *
     * <p>Each entry maps a human-readable name to the SQL that will be EXPLAINed. These correspond
     * to the queries executed by governance dashboards, EOD validation, and audit exploration.
     */
    private Map<String, String> getCriticalQueries() {
        Map<String, String> queries = new LinkedHashMap<>();

        // EOD: posted voucher balance check
        queries.put(
                "EOD-VoucherDebitSum",
                "SELECT COALESCE(SUM(v.total_debit), 0) FROM vouchers v "
                        + "WHERE v.tenant_id = 1 AND v.posting_date = CURRENT_DATE "
                        + "AND v.post_flag = 'Y' AND v.cancel_flag = 'N'");

        // EOD/Clearing: clearing GL net balance
        queries.put(
                "ClearingGL-NetBalance",
                "SELECT COALESCE(SUM(a.balance), 0) FROM accounts a "
                        + "WHERE a.tenant_id = 1 AND a.account_type = 'CLEARING_ACCOUNT'");

        // EOD/Suspense: suspense GL net balance
        queries.put(
                "SuspenseGL-NetBalance",
                "SELECT COALESCE(SUM(a.balance), 0) FROM accounts a "
                        + "WHERE a.tenant_id = 1 AND a.account_type = 'SUSPENSE_ACCOUNT'");

        // IBT Reconciliation: unsettled count
        queries.put(
                "IBT-UnsettledCount",
                "SELECT COUNT(*) FROM inter_branch_transfers t "
                        + "WHERE t.tenant_id = 1 AND t.status IN ('INITIATED', 'SENT', 'RECEIVED')");

        // Suspense Dashboard: open case count
        queries.put(
                "Suspense-OpenCaseCount",
                "SELECT COUNT(*) FROM suspense_cases sc "
                        + "WHERE sc.tenant_id = 1 AND sc.status = 'OPEN'");

        // Audit Explorer: tenant + action + timestamp range
        queries.put(
                "AuditExplorer-ActionFilter",
                "SELECT COUNT(*) FROM audit_logs al "
                        + "WHERE al.tenant_id = 1 AND al.action = 'HARD_LIMIT_EXCEEDED' "
                        + "AND al.timestamp >= '2026-01-01' AND al.timestamp <= '2026-12-31'");

        return queries;
    }
}
