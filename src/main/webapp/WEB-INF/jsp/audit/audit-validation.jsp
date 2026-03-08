<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-shield-check"></i> Audit Diagnostic Dashboard</h3>
    <span class="badge bg-dark"><i class="bi bi-lock"></i> AUDITOR ACCESS ONLY</span>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<div class="row mb-4">
    <div class="col-12">
        <div class="alert alert-info">
            <i class="bi bi-info-circle"></i> This is a read-only internal control visibility dashboard. All statuses reflect the current system configuration and enforcement state.
        </div>
    </div>
</div>

<%-- Control Status Cards --%>
<div class="row g-4 mb-4">
    <%-- Freeze Enforcement --%>
    <div class="col-md-4">
        <div class="card shadow h-100">
            <div class="card-header bg-white">
                <h6 class="mb-0"><i class="bi bi-snow"></i> Freeze Enforcement</h6>
            </div>
            <div class="card-body">
                <div class="d-flex align-items-center mb-2">
                    <c:choose>
                        <c:when test="${freezeEnforcementActive}">
                            <span class="badge bg-success me-2">ACTIVE</span>
                            <span class="text-success">Freeze checks enforced on all debit transactions</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-danger me-2">INACTIVE</span>
                            <span class="text-danger">Freeze enforcement not detected</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <small class="text-muted">Frozen accounts/customers are blocked from debit operations</small>
                <hr>
                <div class="row text-center">
                    <div class="col-6">
                        <small class="text-muted">Frozen Customers</small>
                        <h5 class="mb-0">${frozenCustomerCount}</h5>
                    </div>
                    <div class="col-6">
                        <small class="text-muted">Frozen Accounts</small>
                        <h5 class="mb-0">${frozenAccountCount}</h5>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <%-- Holiday Enforcement --%>
    <div class="col-md-4">
        <div class="card shadow h-100">
            <div class="card-header bg-white">
                <h6 class="mb-0"><i class="bi bi-calendar-x"></i> Holiday Enforcement</h6>
            </div>
            <div class="card-body">
                <div class="d-flex align-items-center mb-2">
                    <c:choose>
                        <c:when test="${holidayEnforcementActive}">
                            <span class="badge bg-success me-2">ACTIVE</span>
                            <span class="text-success">Holiday calendar enforced</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-warning me-2">NOT CONFIGURED</span>
                            <span class="text-warning">Holiday enforcement not detected</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <small class="text-muted">Transactions blocked on bank holidays per calendar configuration</small>
                <hr>
                <div class="row text-center">
                    <div class="col-6">
                        <small class="text-muted">Holidays Defined</small>
                        <h5 class="mb-0">${holidayCount}</h5>
                    </div>
                    <div class="col-6">
                        <small class="text-muted">Today Holiday?</small>
                        <h5 class="mb-0">
                            <c:choose>
                                <c:when test="${todayIsHoliday}"><span class="text-danger">YES</span></c:when>
                                <c:otherwise><span class="text-success">NO</span></c:otherwise>
                            </c:choose>
                        </h5>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <%-- Maker-Checker Enforcement --%>
    <div class="col-md-4">
        <div class="card shadow h-100">
            <div class="card-header bg-white">
                <h6 class="mb-0"><i class="bi bi-people-fill"></i> Maker-Checker Enforcement</h6>
            </div>
            <div class="card-body">
                <div class="d-flex align-items-center mb-2">
                    <c:choose>
                        <c:when test="${makerCheckerActive}">
                            <span class="badge bg-success me-2">ACTIVE</span>
                            <span class="text-success">Dual-control approval enforced</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-danger me-2">INACTIVE</span>
                            <span class="text-danger">Maker-checker not enforced</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <small class="text-muted">High-value transactions require separate maker and checker</small>
                <hr>
                <div class="row text-center">
                    <div class="col-4">
                        <small class="text-muted">Pending</small>
                        <h5 class="mb-0 text-warning">${pendingApprovalCount}</h5>
                    </div>
                    <div class="col-4">
                        <small class="text-muted">Approved</small>
                        <h5 class="mb-0 text-success">${approvedCount}</h5>
                    </div>
                    <div class="col-4">
                        <small class="text-muted">Rejected</small>
                        <h5 class="mb-0 text-danger">${rejectedCount}</h5>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="row g-4 mb-4">
    <%-- Ledger Immutability --%>
    <div class="col-md-4">
        <div class="card shadow h-100">
            <div class="card-header bg-white">
                <h6 class="mb-0"><i class="bi bi-file-earmark-lock"></i> Ledger Immutability</h6>
            </div>
            <div class="card-body">
                <div class="d-flex align-items-center mb-2">
                    <c:choose>
                        <c:when test="${ledgerImmutable}">
                            <span class="badge bg-success me-2">ENFORCED</span>
                            <span class="text-success">Ledger entries are append-only and immutable</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-danger me-2">VIOLATION</span>
                            <span class="text-danger">Ledger mutability detected</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <small class="text-muted">All financial entries follow double-entry accounting. Corrections via reversal entries only.</small>
                <hr>
                <div class="text-center">
                    <small class="text-muted">Total Ledger Entries</small>
                    <h5 class="mb-0">${totalLedgerEntries}</h5>
                </div>
            </div>
        </div>
    </div>

    <%-- Batch Status --%>
    <div class="col-md-4">
        <div class="card shadow h-100">
            <div class="card-header bg-white">
                <h6 class="mb-0"><i class="bi bi-gear-wide-connected"></i> Batch Processing Status</h6>
            </div>
            <div class="card-body">
                <div class="d-flex align-items-center mb-2">
                    <c:choose>
                        <c:when test="${batchStatus == 'COMPLETED'}">
                            <span class="badge bg-success me-2">COMPLETED</span>
                            <span class="text-success">Last batch run completed successfully</span>
                        </c:when>
                        <c:when test="${batchStatus == 'RUNNING'}">
                            <span class="badge bg-info me-2">RUNNING</span>
                            <span class="text-info">Batch currently in progress</span>
                        </c:when>
                        <c:when test="${batchStatus == 'FAILED'}">
                            <span class="badge bg-danger me-2">FAILED</span>
                            <span class="text-danger">Last batch run failed</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-secondary me-2">N/A</span>
                            <span class="text-muted">No batch information available</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <small class="text-muted">EOD/BOD batch processing for interest calculation, account maintenance</small>
                <hr>
                <div class="text-center">
                    <small class="text-muted">Last Run</small>
                    <h5 class="mb-0"><c:out value="${lastBatchRun}" default="N/A"/></h5>
                </div>
            </div>
        </div>
    </div>

    <%-- Business Date Status --%>
    <div class="col-md-4">
        <div class="card shadow h-100">
            <div class="card-header bg-white">
                <h6 class="mb-0"><i class="bi bi-calendar-check"></i> Business Date Status</h6>
            </div>
            <div class="card-body">
                <div class="d-flex align-items-center mb-2">
                    <c:choose>
                        <c:when test="${businessDateCurrent}">
                            <span class="badge bg-success me-2">CURRENT</span>
                            <span class="text-success">Business date is aligned with system date</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-warning me-2">MISALIGNED</span>
                            <span class="text-warning">Business date does not match system date</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <small class="text-muted">Business date drives all transaction posting and reporting</small>
                <hr>
                <div class="row text-center">
                    <div class="col-6">
                        <small class="text-muted">Business Date</small>
                        <h5 class="mb-0"><c:out value="${businessDate}" default="N/A"/></h5>
                    </div>
                    <div class="col-6">
                        <small class="text-muted">System Date</small>
                        <h5 class="mb-0"><c:out value="${systemDate}" default="N/A"/></h5>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Audit Summary Table --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white">
        <h5 class="mb-0"><i class="bi bi-table"></i> Governance Control Summary</h5>
    </div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-bordered table-sm mb-0">
                <thead class="table-light">
                    <tr><th>Control</th><th>Status</th><th>Description</th><th>Last Verified</th></tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Account Freeze Enforcement</td>
                        <td><c:choose><c:when test="${freezeEnforcementActive}"><span class="badge bg-success">PASS</span></c:when><c:otherwise><span class="badge bg-danger">FAIL</span></c:otherwise></c:choose></td>
                        <td>Frozen accounts blocked from debit operations</td>
                        <td><small><c:out value="${systemDate}"/></small></td>
                    </tr>
                    <tr>
                        <td>Holiday Calendar Enforcement</td>
                        <td><c:choose><c:when test="${holidayEnforcementActive}"><span class="badge bg-success">PASS</span></c:when><c:otherwise><span class="badge bg-warning">WARN</span></c:otherwise></c:choose></td>
                        <td>Transactions blocked on defined holidays</td>
                        <td><small><c:out value="${systemDate}"/></small></td>
                    </tr>
                    <tr>
                        <td>Maker-Checker Dual Control</td>
                        <td><c:choose><c:when test="${makerCheckerActive}"><span class="badge bg-success">PASS</span></c:when><c:otherwise><span class="badge bg-danger">FAIL</span></c:otherwise></c:choose></td>
                        <td>High-value operations require separate approval</td>
                        <td><small><c:out value="${systemDate}"/></small></td>
                    </tr>
                    <tr>
                        <td>Ledger Immutability</td>
                        <td><c:choose><c:when test="${ledgerImmutable}"><span class="badge bg-success">PASS</span></c:when><c:otherwise><span class="badge bg-danger">FAIL</span></c:otherwise></c:choose></td>
                        <td>All ledger entries append-only, corrections via reversal</td>
                        <td><small><c:out value="${systemDate}"/></small></td>
                    </tr>
                    <tr>
                        <td>Batch Processing</td>
                        <td><c:choose><c:when test="${batchStatus == 'COMPLETED'}"><span class="badge bg-success">PASS</span></c:when><c:when test="${batchStatus == 'RUNNING'}"><span class="badge bg-info">RUNNING</span></c:when><c:otherwise><span class="badge bg-warning">WARN</span></c:otherwise></c:choose></td>
                        <td>EOD/BOD batch runs completing successfully</td>
                        <td><small><c:out value="${lastBatchRun}" default="N/A"/></small></td>
                    </tr>
                    <tr>
                        <td>Business Date Alignment</td>
                        <td><c:choose><c:when test="${businessDateCurrent}"><span class="badge bg-success">PASS</span></c:when><c:otherwise><span class="badge bg-warning">WARN</span></c:otherwise></c:choose></td>
                        <td>Business date matches system date</td>
                        <td><small><c:out value="${systemDate}"/></small></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%-- Audit Disclaimer --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    This dashboard is for internal audit and governance visibility only. All data is read-only. No sensitive customer data is displayed.
</div>

<%@ include file="../layout/footer.jsp" %>
