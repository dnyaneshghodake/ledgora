<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-shield-check"></i> Capital Adequacy (CRAR) — Basel III</h3>
    <div>
        <span class="badge bg-dark me-2"><i class="bi bi-calendar3"></i> <c:out value="${businessDate}"/></span>
        <span class="eod-verified"><i class="bi bi-check-circle-fill"></i> EOD Verified</span>
        <a href="${pageContext.request.contextPath}/regulatory/dashboard" class="btn btn-sm btn-outline-secondary ms-2"><i class="bi bi-arrow-left"></i> Dashboard</a>
    </div>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<c:choose>
    <c:when test="${crarAvailable}">
        <%-- CRAR Summary --%>
        <div class="row g-3 mb-4">
            <div class="col-md-2"><div class="reg-kpi-card text-center"><small class="text-muted d-block">CRAR %</small><h2 class="mb-0 ${crarData.meetsMinimumCrar ? 'text-success' : 'text-danger'}"><c:out value="${crarData.crarPercent}"/>%</h2></div></div>
            <div class="col-md-2"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Tier 1 %</small><h4 class="mb-0"><c:out value="${crarData.tier1Percent}"/>%</h4></div></div>
            <div class="col-md-2"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Tier 1 Capital</small><h5 class="mb-0"><fmt:formatNumber value="${crarData.tier1Capital}" maxFractionDigits="0"/></h5></div></div>
            <div class="col-md-2"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Tier 2 Capital</small><h5 class="mb-0"><fmt:formatNumber value="${crarData.tier2Capital}" maxFractionDigits="0"/></h5></div></div>
            <div class="col-md-2"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Total Capital</small><h5 class="mb-0"><fmt:formatNumber value="${crarData.totalCapital}" maxFractionDigits="0"/></h5></div></div>
            <div class="col-md-2"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Total RWA</small><h5 class="mb-0"><fmt:formatNumber value="${crarData.totalRwa}" maxFractionDigits="0"/></h5></div></div>
        </div>

        <%-- Compliance Status --%>
        <div class="alert ${crarData.meetsMinimumCrar ? 'alert-success' : 'alert-danger'} mb-4">
            <i class="bi ${crarData.meetsMinimumCrar ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill'}"></i>
            <strong>Compliance:</strong> <c:out value="${crarData.complianceStatus}"/>
        </div>

        <%-- Capital Breakdown --%>
        <div class="card shadow mb-4 reg-card">
            <div class="card-header bg-white"><h5 class="mb-0">Capital Breakdown</h5></div>
            <div class="card-body">
                <table class="table table-sm">
                    <tr><td>Equity Capital (CET1)</td><td class="text-end"><fmt:formatNumber value="${crarData.equityCapital}" maxFractionDigits="2"/></td></tr>
                    <tr><td>Retained Earnings</td><td class="text-end"><fmt:formatNumber value="${crarData.retainedEarnings}" maxFractionDigits="2"/></td></tr>
                    <tr class="fw-bold"><td>Tier 1 Capital</td><td class="text-end"><fmt:formatNumber value="${crarData.tier1Capital}" maxFractionDigits="2"/></td></tr>
                    <tr><td>General Provisions (Tier 2)</td><td class="text-end"><fmt:formatNumber value="${crarData.generalProvisions}" maxFractionDigits="2"/></td></tr>
                    <tr><td>Tier 2 Capital (capped)</td><td class="text-end"><fmt:formatNumber value="${crarData.tier2Capital}" maxFractionDigits="2"/></td></tr>
                    <tr class="fw-bold table-primary"><td>Total Capital</td><td class="text-end"><fmt:formatNumber value="${crarData.totalCapital}" maxFractionDigits="2"/></td></tr>
                </table>
            </div>
        </div>

        <%-- RWA Breakdown --%>
        <div class="card shadow mb-4 reg-card">
            <div class="card-header bg-white"><h5 class="mb-0">Risk-Weighted Assets (Standardised Approach)</h5></div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-sm table-hover reg-table">
                        <thead class="table-light">
                            <tr><th>GL Code</th><th>GL Name</th><th>Asset Class</th><th class="text-end">Exposure</th><th class="text-end">Risk Weight %</th><th class="text-end">RWA</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="line" items="${crarData.rwaBreakdown}">
                            <tr>
                                <td><code><c:out value="${line.glCode}"/></code></td>
                                <td><c:out value="${line.glName}"/></td>
                                <td><span class="badge bg-light text-dark"><c:out value="${line.assetClass}"/></span></td>
                                <td class="text-end"><fmt:formatNumber value="${line.exposure}" maxFractionDigits="0"/></td>
                                <td class="text-end"><c:out value="${line.riskWeight}"/>%</td>
                                <td class="text-end"><fmt:formatNumber value="${line.rwa}" maxFractionDigits="0"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                        <tfoot class="table-light fw-bold">
                            <tr><td colspan="5">TOTAL RWA</td><td class="text-end"><fmt:formatNumber value="${crarData.totalRwa}" maxFractionDigits="0"/></td></tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>

        <c:if test="${crarSnapshot != null}">
        <div class="audit-disclaimer mt-3"><i class="bi bi-shield-lock"></i> Snapshot: <c:out value="${crarSnapshot.generatedAt}"/> | Hash: <code>${crarSnapshot.hashChecksum.substring(0, 16)}...</code> | RBI Min CRAR: 9% | RBI Min Tier 1: 7%</div>
        </c:if>
    </c:when>
    <c:otherwise>
        <div class="alert alert-warning"><i class="bi bi-exclamation-triangle"></i> CRAR snapshot not available for <c:out value="${businessDate}"/>. Run EOD to generate.</div>
    </c:otherwise>
</c:choose>

<%@ include file="../layout/footer.jsp" %>
