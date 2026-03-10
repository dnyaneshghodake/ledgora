package com.ledgora.diagnostics.controller;

import com.ledgora.diagnostics.dto.QueryPlanResult;
import com.ledgora.diagnostics.service.QueryPlanAnalyzerService;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

    public DiagnosticsController(QueryPlanAnalyzerService queryPlanAnalyzer) {
        this.queryPlanAnalyzer = queryPlanAnalyzer;
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
}
