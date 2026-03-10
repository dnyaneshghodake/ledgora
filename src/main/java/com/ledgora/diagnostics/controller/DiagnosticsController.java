package com.ledgora.diagnostics.controller;

import com.ledgora.diagnostics.dto.ConcurrencyAuditResult;
import com.ledgora.diagnostics.dto.EnterpriseCertificationReport;
import com.ledgora.diagnostics.dto.QueryPlanResult;
import com.ledgora.diagnostics.service.CertificationRunnerService;
import com.ledgora.diagnostics.service.ConcurrencyAuditService;
import com.ledgora.diagnostics.service.QueryPlanAnalyzerService;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Query plan diagnostics endpoint. Active ONLY in the "stress" Spring profile.
 *
 * <p>Analyzes execution plans for all critical CBS queries (EOD, IBT, suspense, audit) and returns
 * structured risk assessments. Used to validate that production performance indexes are effective.
 *
 * <p>Read-only. Does not execute the actual queries — only explains them. ADMIN role required.
 */
@RestController
@RequestMapping("/diagnostics")
@Profile("stress")
public class DiagnosticsController {

    private final QueryPlanAnalyzerService queryPlanAnalyzer;
    private final ConcurrencyAuditService concurrencyAuditService;
    private final CertificationRunnerService certificationRunner;

    public DiagnosticsController(
            QueryPlanAnalyzerService queryPlanAnalyzer,
            ConcurrencyAuditService concurrencyAuditService,
            CertificationRunnerService certificationRunner) {
        this.queryPlanAnalyzer = queryPlanAnalyzer;
        this.concurrencyAuditService = concurrencyAuditService;
        this.certificationRunner = certificationRunner;
    }

    /**
     * Analyze execution plans for all critical CBS queries.
     *
     * @return list of query plan results with risk classifications
     */
    @GetMapping("/query-plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<QueryPlanResult>> analyzeQueryPlans() {
        List<QueryPlanResult> results = queryPlanAnalyzer.analyzeAllCriticalQueries();
        return ResponseEntity.ok(results);
    }

    /**
     * Run all concurrency model validation checks against current database state.
     *
     * <p>Designed to run after stress tests to verify no invariant was violated. Returns 11
     * individual check results with pass/fail and violation details.
     */
    @GetMapping("/concurrency-audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConcurrencyAuditResult> runConcurrencyAudit() {
        ConcurrencyAuditResult result = concurrencyAuditService.runAllChecks();
        return ResponseEntity.ok(result);
    }

    /**
     * Run full Enterprise CBS Certification: 6-step pipeline covering load generation, EOD, crash
     * simulation, financial integrity, concurrency audit, and performance grading.
     *
     * <p>This is a long-running operation (minutes). Returns FAIL / PASS / ENTERPRISE_READY.
     */
    @PostMapping("/certification/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnterpriseCertificationReport> runCertification(
            @RequestBody Map<String, Object> params) {
        Long tenantId = ((Number) params.getOrDefault("tenantId", 1)).longValue();
        EnterpriseCertificationReport report = certificationRunner.runFullCertification(tenantId);
        return ResponseEntity.ok(report);
    }
}
