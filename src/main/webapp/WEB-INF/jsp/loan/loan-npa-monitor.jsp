<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-exclamation-triangle"></i> NPA Monitor — RBI IRAC</h3>
    <a href="${pageContext.request.contextPath}/loan/dashboard" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i> Dashboard</a>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<%-- NPA Loans --%>
<div class="card shadow mb-4 reg-card">
    <div class="card-header bg-white"><h5 class="mb-0 text-danger"><i class="bi bi-x-octagon"></i> Non-Performing Assets (DPD > 90)</h5></div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty npaLoans}">
                <div class="table-responsive">
                    <table class="table table-sm table-hover reg-table">
                        <thead class="table-light">
                            <tr><th>Loan #</th><th class="text-end">Outstanding</th><th>DPD</th><th>Category</th><th>Provision %</th><th class="text-end">Provision Amt</th><th>NPA Date</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="loan" items="${npaLoans}">
                            <tr>
                                <td><code><a href="${pageContext.request.contextPath}/loan/${loan.id}"><c:out value="${loan.loanAccountNumber}"/></a></code></td>
                                <td class="text-end"><fmt:formatNumber value="${loan.outstandingPrincipal}" maxFractionDigits="0"/></td>
                                <td class="text-danger fw-bold"><c:out value="${loan.dpd}"/></td>
                                <td><c:choose>
                                    <c:when test="${loan.npaClassification == 'SUBSTANDARD'}"><span class="badge bg-warning text-dark">Substandard</span></c:when>
                                    <c:when test="${loan.npaClassification == 'DOUBTFUL'}"><span class="badge bg-danger">Doubtful</span></c:when>
                                    <c:when test="${loan.npaClassification == 'LOSS'}"><span class="badge" style="background-color:#8b0000;color:#fff;">Loss</span></c:when>
                                </c:choose></td>
                                <td><c:out value="${loan.npaClassification.provisionRate}"/>%</td>
                                <td class="text-end"><fmt:formatNumber value="${loan.provisionAmount}" maxFractionDigits="0"/></td>
                                <td><c:out value="${loan.npaDate}"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-success"><i class="bi bi-check-circle" style="font-size:2rem;"></i><p>No NPA loans. All assets performing.</p></div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- At-Risk Loans (DPD > 0 but < 90) --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0 text-warning"><i class="bi bi-exclamation-triangle"></i> At-Risk Loans (DPD > 0, approaching NPA)</h5></div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty atRiskLoans}">
                <div class="table-responsive">
                    <table class="table table-sm table-hover">
                        <thead class="table-light">
                            <tr><th>Loan #</th><th class="text-end">Outstanding</th><th>DPD</th><th>Days to NPA</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="loan" items="${atRiskLoans}">
                            <tr>
                                <td><code><a href="${pageContext.request.contextPath}/loan/${loan.id}"><c:out value="${loan.loanAccountNumber}"/></a></code></td>
                                <td class="text-end"><fmt:formatNumber value="${loan.outstandingPrincipal}" maxFractionDigits="0"/></td>
                                <td class="text-warning fw-bold"><c:out value="${loan.dpd}"/></td>
                                <td>${90 - loan.dpd} days</td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-3 text-muted">No at-risk loans.</div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i> NPA classification per RBI Prudential Norms (90-day DPD). Provisioning: Standard 0.4%, Substandard 15%, Doubtful 25%, Loss 100%.
</div>

<%@ include file="../layout/footer.jsp" %>
