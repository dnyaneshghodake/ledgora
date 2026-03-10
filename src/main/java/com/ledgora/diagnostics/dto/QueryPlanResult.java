package com.ledgora.diagnostics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a single query execution plan analysis.
 *
 * <p>Captures the raw execution plan text, detected risk indicators, and an overall risk level.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryPlanResult {

    /** Human-readable name identifying which critical query this is. */
    private String queryName;

    /** The SQL that was analyzed (with EXPLAIN prepended). */
    private String sql;

    /** Raw execution plan output from the database engine. */
    private String executionPlan;

    /** Risk classification: LOW, MEDIUM, HIGH. */
    private String riskLevel;

    /** Specific risk indicators found in the plan (e.g., "TABLE SCAN detected"). */
    private String riskDetails;

    /** Whether an index was detected in the plan. */
    private boolean indexUsed;

    /** Whether a full table scan was detected. */
    private boolean tableScanDetected;
}
