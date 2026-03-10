<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-speedometer2"></i> Velocity Fraud Risk Monitor</h3>
    <div>
        <a href="${pageContext.request.contextPath}/risk/hard-ceiling" class="btn btn-outline-secondary"><i class="bi bi-shield-exclamation"></i> Hard Ceiling</a>
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
            <strong>RBI Fraud Risk Management:</strong> Velocity breaches auto-freeze accounts to UNDER_REVIEW pending investigation.
            60-minute rolling window. Account freeze + FraudAlert + audit event on breach.
            Metric: <code>ledgora.velocity.blocked</code>.
        </div>
    </div>
</div>

<%-- Section A: Risk Pressure Indicator --%>
<div class="row g-3 mb-4">
    <div class="col-md-12">
        <div class="card shadow">
            <div class="card-body text-center py-4">
                <small class="text-muted d-block mb-2">Fraud Pressure Level</small>
                <c:choose>
                    <c:when test="${fraudPressureLevel == 'LOW'}">
                        <h2><span class="badge bg-success px-4 py-2"><i class="bi bi-check-circle-fill me-2"></i>LOW</span></h2>
                        <small class="text-muted">No open velocity alerts. System operating normally.</small>
                    </c:when>
                    <c:when test="${fraudPressureLevel == 'MEDIUM'}">
                        <h2><span class="badge bg-warning text-dark px-4 py-2"><i class="bi bi-exclamation-triangle-fill me-2"></i>MEDIUM</span></h2>
                        <small class="text-warning">1&ndash;5 open velocity alerts. Monitor closely.</small>
                    </c:when>
                    <c:otherwise>
                        <h2><span class="badge bg-danger px-4 py-2"><i class="bi bi-x-octagon-fill me-2"></i>HIGH</span></h2>
                        <small class="text-danger">More than 5 open velocity alerts. Immediate investigation required.</small>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%-- Section B: KPI Cards --%>
<div class="row g-3 mb-4">
    <div class="col-md-6">
        <div class="card shadow border-start border-4 ${openFraudAlerts > 0 ? 'border-danger' : 'border-success'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Open Fraud Alerts</small>
                <h2 class="mb-0 ${openFraudAlerts > 0 ? 'text-danger' : 'text-success'}">
                    <c:out value="${openFraudAlerts}"/>
                </h2>
                <small class="text-muted">Velocity breach alerts pending review</small>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card shadow border-start border-4 ${frozenAccountsCount > 0 ? 'border-warning' : 'border-success'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Accounts Under Review</small>
                <h2 class="mb-0 ${frozenAccountsCount > 0 ? 'text-warning' : 'text-success'}">
                    <c:out value="${frozenAccountsCount}"/>
                </h2>
                <small class="text-muted">Frozen by velocity engine (UNDER_REVIEW)</small>
            </div>
        </div>
    </div>
</div>

<%-- Section C: Recent Alerts Table --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0"><i class="bi bi-bell"></i> Recent Fraud Alerts (Last 20)</h5>
        <span class="badge bg-secondary"><c:out value="${recentAlerts.size()}"/> shown</span>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty recentAlerts}">
                <div class="table-responsive">
                    <table class="table table-hover table-sm">
                        <thead class="table-light">
                            <tr>
                                <th>Alert ID</th>
                                <th>Account</th>
                                <th>Type</th>
                                <th>Observed</th>
                                <th>Threshold</th>
                                <th>Status</th>
                                <th>Created At</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="alert" items="${recentAlerts}">
                            <tr>
                                <td><code><c:out value="${alert.id}"/></code></td>
                                <td><code><c:out value="${alert.accountNumber}"/></code></td>
                                <td><span class="badge bg-secondary"><c:out value="${alert.alertType}"/></span></td>
                                <td>
                                    <c:if test="${alert.observedCount != null}">
                                        <c:out value="${alert.observedCount}"/> txns
                                    </c:if>
                                    <c:if test="${alert.observedAmount != null}">
                                        / <c:out value="${alert.observedAmount}"/>
                                    </c:if>
                                </td>
                                <td><c:out value="${alert.thresholdValue}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${alert.status == 'OPEN'}"><span class="badge bg-danger">OPEN</span></c:when>
                                        <c:when test="${alert.status == 'ACKNOWLEDGED'}"><span class="badge bg-warning text-dark">ACKNOWLEDGED</span></c:when>
                                        <c:when test="${alert.status == 'RESOLVED'}"><span class="badge bg-success">RESOLVED</span></c:when>
                                        <c:when test="${alert.status == 'FALSE_POSITIVE'}"><span class="badge bg-info">FALSE POSITIVE</span></c:when>
                                        <c:otherwise><span class="badge bg-light text-dark"><c:out value="${alert.status}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><small><c:out value="${alert.createdAt}"/></small></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-shield-check" style="font-size: 3rem;"></i>
                    <p class="mt-2">No fraud alerts recorded. Velocity controls operating normally.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- Governance Footer --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    Velocity breaches auto-freeze accounts pending investigation. VelocityFraudEngine queries 60-minute
    transaction window per account. On breach: block transaction, freeze to UNDER_REVIEW, create FraudAlert,
    emit ledgora.velocity.blocked metric, log VELOCITY_BREACH audit event. This dashboard is read-only.
</div>

<%@ include file="../layout/footer.jsp" %>
