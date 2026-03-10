<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-shield-exclamation"></i> Hard Transaction Ceiling Monitor</h3>
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
            <strong>RBI Governance Control:</strong> Absolute transaction ceiling cannot be bypassed by any role.
            No runtime configuration override. Repeated violations may indicate fraud attempt or misconfigured channel limits.
        </div>
    </div>
</div>

<%-- Section A: KPI Card --%>
<div class="row g-3 mb-4">
    <div class="col-md-4">
        <div class="card shadow border-start border-4 ${todayViolationCount > 0 ? 'border-danger' : 'border-success'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Hard Limit Violations Today</small>
                <h2 class="mb-0 ${todayViolationCount > 0 ? 'text-danger' : 'text-success'}">
                    <c:out value="${todayViolationCount}"/>
                </h2>
                <small class="text-muted">Business Date: <c:out value="${businessDate}"/></small>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow border-start border-4 border-secondary">
            <div class="card-body text-center">
                <small class="text-muted d-block">Recent Violations (All Time)</small>
                <h2 class="mb-0 text-secondary">
                    <c:out value="${last20Violations.size()}"/>
                </h2>
                <small class="text-muted">Showing last 20</small>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow border-start border-4 ${todayViolationCount == 0 ? 'border-success' : 'border-warning'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Enforcement Status</small>
                <c:choose>
                    <c:when test="${todayViolationCount == 0}">
                        <h4 class="mb-0"><span class="badge bg-success"><i class="bi bi-check-circle-fill"></i> CLEAN</span></h4>
                    </c:when>
                    <c:when test="${todayViolationCount <= 3}">
                        <h4 class="mb-0"><span class="badge bg-warning text-dark"><i class="bi bi-exclamation-triangle-fill"></i> MONITOR</span></h4>
                    </c:when>
                    <c:otherwise>
                        <h4 class="mb-0"><span class="badge bg-danger"><i class="bi bi-x-octagon-fill"></i> ALERT</span></h4>
                    </c:otherwise>
                </c:choose>
                <small class="text-muted">Based on today's count</small>
            </div>
        </div>
    </div>
</div>

<%-- Today's status alert --%>
<c:if test="${todayViolationCount > 0}">
    <div class="alert alert-danger border-0 shadow-sm mb-3">
        <div class="d-flex align-items-center">
            <i class="bi bi-exclamation-octagon-fill fs-3 me-3"></i>
            <div>
                <strong><c:out value="${todayViolationCount}"/> hard ceiling violation(s) detected today</strong>
                <p class="mb-0 mt-1">
                    Transactions exceeding the absolute maximum amount were blocked.
                    Review the violations below. If repeated from the same user or channel, escalate to Compliance.
                </p>
            </div>
        </div>
    </div>
</c:if>

<%-- Section B: Recent Violations Table --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0"><i class="bi bi-list-check"></i> Recent Hard Ceiling Violations (Last 20)</h5>
        <span class="badge bg-secondary"><c:out value="${last20Violations.size()}"/> shown</span>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty last20Violations}">
                <div class="table-responsive">
                    <table class="table table-hover table-sm">
                        <thead class="table-light">
                            <tr>
                                <th>Timestamp</th>
                                <th>User</th>
                                <th>Entity</th>
                                <th>Details</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="log" items="${last20Violations}">
                            <tr>
                                <td><small><c:out value="${log.timestamp}"/></small></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty log.username}"><c:out value="${log.username}"/></c:when>
                                        <c:when test="${log.userId != null}">User #<c:out value="${log.userId}"/></c:when>
                                        <c:otherwise><span class="text-muted">System</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><span class="badge bg-secondary"><c:out value="${log.entity}"/></span></td>
                                <td><small class="text-break"><c:out value="${log.details}"/></small></td>
                                <td><span class="badge bg-danger">BLOCKED</span></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-shield-check" style="font-size: 3rem;"></i>
                    <p class="mt-2">No hard ceiling violations recorded. All transactions within limits.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- Governance Footer --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    Absolute transaction ceiling is enforced by HardTransactionCeilingService BEFORE any persistence.
    No role (including ADMIN) can bypass. Violations logged to audit_logs with action HARD_LIMIT_EXCEEDED.
    Metric: ledgora.hard_limit.blocked. This dashboard is read-only.
</div>

<%@ include file="../layout/footer.jsp" %>
