<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-sunrise"></i> Day Begin Ceremony</h3>
    <span class="badge bg-${businessDateStatus == 'CLOSED' ? 'danger' : businessDateStatus == 'OPEN' ? 'success' : 'warning'} fs-6">
        ${businessDateStatus}
    </span>
</div>

<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<div class="row">
    <div class="col-md-8">
        <div class="card shadow mb-4">
            <div class="card-header bg-primary text-white">
                <h5 class="mb-0"><i class="bi bi-shield-check"></i> Day Begin Pre-Checks</h5>
            </div>
            <div class="card-body">
                <p class="text-muted mb-3">
                    Business Date: <strong>${businessDate}</strong> &mdash; Tenant: <strong><c:out value="${tenantName}"/></strong>
                </p>

                <c:choose>
                    <c:when test="${empty validationResults}">
                        <div class="alert alert-success">
                            <i class="bi bi-check-circle-fill"></i> All pre-checks passed. Ready to open the day.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <table class="table table-sm">
                            <thead class="table-light">
                                <tr><th>Status</th><th>Check Result</th></tr>
                            </thead>
                            <tbody>
                                <c:forEach var="result" items="${validationResults}">
                                <tr>
                                    <td>
                                        <c:choose>
                                            <c:when test="${result.toLowerCase().contains('warning')}">
                                                <span class="badge bg-warning text-dark"><i class="bi bi-exclamation-triangle"></i> Warning</span>
                                            </c:when>
                                            <c:when test="${result.toLowerCase().contains('blocked')}">
                                                <span class="badge bg-danger"><i class="bi bi-x-circle"></i> Blocked</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-info"><i class="bi bi-info-circle"></i> Info</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><c:out value="${result}"/></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <div class="col-md-4">
        <div class="card shadow mb-4">
            <div class="card-header bg-light">
                <h5 class="mb-0"><i class="bi bi-play-circle"></i> Open Day</h5>
            </div>
            <div class="card-body text-center">
                <c:choose>
                    <c:when test="${canOpenDay}">
                        <p class="text-success fw-bold">All critical checks passed.</p>
                        <form method="post" action="${pageContext.request.contextPath}/eod/day-begin">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                            <button type="submit" class="btn btn-success btn-lg w-100"
                                    onclick="return confirm('Open business date ${businessDate}? This will allow transactions.')">
                                <i class="bi bi-sunrise"></i> Open Day
                            </button>
                        </form>
                    </c:when>
                    <c:when test="${businessDateStatus == 'OPEN'}">
                        <p class="text-info">Business day is already <strong>OPEN</strong>.</p>
                        <a href="${pageContext.request.contextPath}/eod/status" class="btn btn-outline-primary w-100">
                            <i class="bi bi-info-circle"></i> View Status
                        </a>
                    </c:when>
                    <c:otherwise>
                        <p class="text-danger">Critical pre-checks failed. Resolve the issues above before opening the day.</p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <div class="card shadow">
            <div class="card-header bg-light">
                <h5 class="mb-0"><i class="bi bi-clock-history"></i> Day Lifecycle</h5>
            </div>
            <div class="card-body">
                <small class="text-muted">
                    <strong>CBS Day Flow:</strong><br>
                    CLOSED &rarr; <em>Day Begin</em> &rarr; OPEN &rarr; <em>EOD</em> &rarr; DAY_CLOSING &rarr; CLOSED
                </small>
            </div>
        </div>
    </div>
</div>

<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    Day Begin is an audited action. Only authorized DBO users can open the business day.
</div>

<%@ include file="../layout/footer.jsp" %>
