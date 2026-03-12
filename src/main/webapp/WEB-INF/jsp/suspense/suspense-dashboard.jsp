<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-exclamation-diamond"></i> Suspense Governance Dashboard</h3>
    <div>
        <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Dashboard</a>
    </div>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Governance Banner --%>
<div class="alert alert-dark border-0 mb-4">
    <div class="d-flex align-items-center">
        <i class="bi bi-shield-lock-fill fs-4 me-3"></i>
        <div>
            <strong>RBI CBS Control:</strong> Suspense GL must be reconciled daily.
            Open suspense beyond T+1 requires operations review.
            EOD blocks on non-zero suspense GL balance.
        </div>
    </div>
</div>

<%-- Section A: KPI Cards --%>
<div class="row g-3 mb-4">
    <div class="col-md-3">
        <div class="card shadow border-start border-4 ${openCaseCount > 0 ? 'border-warning' : 'border-success'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Open Suspense Cases</small>
                <h2 class="mb-0 ${openCaseCount > 0 ? 'text-warning' : 'text-success'}">
                    <c:out value="${openCaseCount}"/>
                </h2>
                <small class="text-muted">Pending resolution</small>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow border-start border-4 border-info">
            <div class="card-body text-center">
                <small class="text-muted d-block">Resolved Cases</small>
                <h2 class="mb-0 text-info">
                    <c:out value="${resolvedCaseCount}"/>
                </h2>
                <small class="text-muted">Successfully cleared</small>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow border-start border-4 ${suspenseBalanced ? 'border-success' : 'border-danger'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Suspense GL Net</small>
                <h2 class="mb-0 ${suspenseBalanced ? 'text-success' : 'text-danger'}">
                    <c:out value="${suspenseGlNetBalance}"/>
                </h2>
                <small class="text-muted">SUSPENSE_ACCOUNT sum</small>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow border-start border-4 ${openCaseCount == 0 ? 'border-success' : 'border-warning'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Open Exposure</small>
                <h2 class="mb-0 ${openCaseCount == 0 ? 'text-success' : 'text-warning'}">
                    <c:out value="${totalOpenSuspenseAmount}"/>
                </h2>
                <small class="text-muted">Sum of open case amounts</small>
            </div>
        </div>
    </div>
</div>

<%-- Section B: GL Control Status --%>
<c:choose>
    <c:when test="${!suspenseBalanced}">
        <div class="alert alert-danger border-0 shadow-sm mb-3">
            <div class="d-flex align-items-center">
                <i class="bi bi-exclamation-octagon-fill fs-3 me-3"></i>
                <div>
                    <strong>SUSPENSE GL MUST NET TO ZERO BEFORE EOD</strong>
                    <p class="mb-0 mt-1">
                        Suspense GL net balance is <strong><c:out value="${suspenseGlNetBalance}"/></strong>.
                        EOD will block until all suspense accounts are cleared.
                        Resolve or reverse all open suspense cases.
                    </p>
                </div>
            </div>
        </div>
    </c:when>
    <c:when test="${exposureMismatch && openCaseCount > 0}">
        <div class="alert alert-warning border-0 shadow-sm mb-3">
            <div class="d-flex align-items-center">
                <i class="bi bi-exclamation-triangle-fill fs-3 me-3"></i>
                <div>
                    <strong>EXPOSURE MISMATCH</strong>
                    <p class="mb-0 mt-1">
                        Suspense GL balance (<strong><c:out value="${suspenseGlNetBalance}"/></strong>)
                        does not match open case exposure (<strong><c:out value="${totalOpenSuspenseAmount}"/></strong>).
                        Investigate discrepancy — possible untracked suspense entries.
                    </p>
                </div>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="alert alert-success border-0 shadow-sm mb-3">
            <div class="d-flex align-items-center">
                <i class="bi bi-shield-fill-check fs-3 me-3"></i>
                <div>
                    <strong>Suspense Governance Healthy</strong>
                    <p class="mb-0 mt-1">Suspense GL is balanced. No open cases pending. EOD suspense gate: PASS.</p>
                </div>
            </div>
        </div>
    </c:otherwise>
</c:choose>

<%-- Section C: Aging Table — Oldest Open Cases --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0"><i class="bi bi-hourglass-split"></i> Oldest Open Suspense Cases (Top 10)</h5>
        <span class="badge bg-secondary"><c:out value="${openCaseCount}"/> total open</span>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty oldestOpenCases}">
                <div class="table-responsive">
                    <table class="table table-hover table-sm">
                        <thead class="table-light">
                            <tr>
                                <th>Case ID</th>
                                <th>Transaction ID</th>
                                <th>Amount</th>
                                <th>Reason Code</th>
                                <th>Business Date</th>
                                <th>Age (days)</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="sc" items="${oldestOpenCases}">
                            <c:set var="scBizDate" value="${sc.businessDate}"/>
                            <%
                                java.time.LocalDate scDate = (java.time.LocalDate) pageContext.getAttribute("scBizDate");
                                java.time.LocalDate refDate = (java.time.LocalDate) request.getAttribute("businessDate");
                                long ageDays = 0;
                                if (scDate != null && refDate != null) {
                                    ageDays = java.time.temporal.ChronoUnit.DAYS.between(scDate, refDate);
                                }
                                pageContext.setAttribute("ageDays", ageDays);
                            %>
                            <tr class="${ageDays >= 3 ? 'table-danger' : (ageDays >= 2 ? 'table-warning' : (ageDays >= 1 ? 'table-info' : ''))}">
                                <td><code><c:out value="${sc.id}"/></code></td>
                                <td>
                                    <c:if test="${sc.originalTransaction != null}">
                                        <a href="${pageContext.request.contextPath}/transactions/${sc.originalTransaction.id}/view" title="360° View">
                                            <code><c:out value="${sc.originalTransaction.transactionRef}"/></code>
                                        </a>
                                    </c:if>
                                </td>
                                <td class="fw-bold"><c:out value="${sc.amount}"/> <small class="text-muted"><c:out value="${sc.currency}"/></small></td>
                                <td><span class="badge bg-secondary"><c:out value="${sc.reasonCode}"/></span></td>
                                <td><c:out value="${sc.businessDate}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${ageDays >= 3}">
                                            <span class="badge bg-danger"><c:out value="${ageDays}"/>d</span>
                                            <small class="text-danger ms-1">CRITICAL</small>
                                        </c:when>
                                        <c:when test="${ageDays >= 2}">
                                            <span class="badge bg-warning text-dark"><c:out value="${ageDays}"/>d</span>
                                            <small class="text-warning ms-1">ESCALATE</small>
                                        </c:when>
                                        <c:when test="${ageDays >= 1}">
                                            <span class="badge bg-info"><c:out value="${ageDays}"/>d</span>
                                            <small class="text-info ms-1">REVIEW</small>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary"><c:out value="${ageDays}"/>d</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td><span class="badge bg-warning text-dark">OPEN</span></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <%-- Aging Legend --%>
                <div class="d-flex gap-3 mt-2">
                    <small><span class="badge bg-secondary">T+0</span> Same day</small>
                    <small><span class="badge bg-info">T+1</span> Review</small>
                    <small><span class="badge bg-warning text-dark">T+2</span> Escalate</small>
                    <small><span class="badge bg-danger">T+3+</span> Critical</small>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-check-circle" style="font-size: 3rem;"></i>
                    <p class="mt-2">No open suspense cases. All clear.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- Governance Footer --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    RBI CBS Control: Suspense GL must be reconciled daily. Open suspense beyond T+1 requires operations review.
    This dashboard is read-only. All data sourced from SUSPENSE_ACCOUNT balances and SuspenseCase lifecycle states.
    No voucher derivation. No record mutation.
</div>

<%@ include file="../layout/footer.jsp" %>
