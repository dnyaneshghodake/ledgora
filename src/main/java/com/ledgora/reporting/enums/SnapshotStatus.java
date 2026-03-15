package com.ledgora.reporting.enums;

/**
 * Financial statement snapshot lifecycle status.
 *
 * <p>RBI Audit Trail: DRAFT snapshots may be regenerated (e.g., after late postings). FINAL
 * snapshots are immutable — any modification requires a new snapshot with audit justification.
 */
public enum SnapshotStatus {
    /** Generated but not yet validated/committed. May be overwritten. */
    DRAFT,
    /** Validated, committed, and immutable. Forms part of the regulatory record. */
    FINAL
}
