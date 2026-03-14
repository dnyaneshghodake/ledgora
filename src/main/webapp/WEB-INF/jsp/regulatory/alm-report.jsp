<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-bar-chart-steps"></i> ALM — Structural Liquidity Statement</h3>
    <div>
        <span class="badge bg-dark me-2"><i class="bi bi-calendar3"></i> <c:out value="${businessDate}"/></span>
        <span class="eod-verified"><i class="bi bi-check-circle-fill"></i> EOD Verified</span>
        <a href="${pageContext.request.contextPath}/regulatory/dashboard" class="btn btn-sm btn-outline-secondary ms-2"><i class="bi bi-arrow-left"></i> Dashboard</a>
    </div>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<c:choose>
    <c:when test="${almAvailable}">
        <%-- Risk Alert Banner --%>
        <c:if test="${almData.hasStructuralLiquidityRisk}">
            <div class="alert alert-danger"><i class="bi bi-exclamation-octagon-fill"></i> <strong>Structural Liquidity Risk:</strong> <c:out value="${almData.riskAssessment}"/></div>
        </c:if>

        <%-- Summary KPIs --%>
        <div class="row g-3 mb-4">
            <div class="col-md-3"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Total Assets</small><h4 class="mb-0"><fmt:formatNumber value="${almData.totalAssets}" maxFractionDigits="0"/></h4></div></div>
            <div class="col-md-3"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Total Liabilities</small><h4 class="mb-0"><fmt:formatNumber value="${almData.totalLiabilities}" maxFractionDigits="0"/></h4></div></div>
            <div class="col-md-3"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Overall Gap</small><h4 class="mb-0 ${almData.overallGap < 0 ? 'text-danger' : 'text-success'}"><fmt:formatNumber value="${almData.overallGap}" maxFractionDigits="0"/></h4></div></div>
            <div class="col-md-3"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Risk Level</small><c:choose><c:when test="${almData.hasStructuralLiquidityRisk}"><span class="badge bg-danger fs-6">ELEVATED</span></c:when><c:otherwise><span class="badge bg-success fs-6">LOW</span></c:otherwise></c:choose></div></div>
        </div>

        <%-- Maturity Bucket Table --%>
        <div class="card shadow mb-4 reg-card">
            <div class="card-header bg-white"><h5 class="mb-0">Maturity Bucket Analysis (RBI-mandated 8 buckets)</h5></div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-sm table-hover reg-table">
                        <thead class="table-light">
                            <tr>
                                <th>Maturity Bucket</th>
                                <th class="text-end">Assets (Inflow)</th>
                                <th class="text-end">Liabilities (Outflow)</th>
                                <th class="text-end">Gap</th>
                                <th class="text-end">Cumulative Gap</th>
                                <th class="text-end">Gap Ratio %</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="bucket" items="${almData.buckets}">
                            <tr>
                                <td><c:out value="${bucket.bucketName}"/></td>
                                <td class="text-end"><fmt:formatNumber value="${bucket.assets}" maxFractionDigits="0"/></td>
                                <td class="text-end"><fmt:formatNumber value="${bucket.liabilities}" maxFractionDigits="0"/></td>
                                <td class="text-end ${bucket.gap < 0 ? 'text-danger fw-bold' : ''}"><fmt:formatNumber value="${bucket.gap}" maxFractionDigits="0"/></td>
                                <td class="text-end ${bucket.cumulativeGap < 0 ? 'text-danger fw-bold' : ''}"><fmt:formatNumber value="${bucket.cumulativeGap}" maxFractionDigits="0"/></td>
                                <td class="text-end ${bucket.gapRatioPercent < -15 ? 'text-danger fw-bold' : ''}"><c:out value="${bucket.gapRatioPercent}"/>%</td>
                            </tr>
                            </c:forEach>
                        </tbody>
                        <tfoot class="table-light fw-bold">
                            <tr>
                                <td>TOTAL</td>
                                <td class="text-end"><fmt:formatNumber value="${almData.totalAssets}" maxFractionDigits="0"/></td>
                                <td class="text-end"><fmt:formatNumber value="${almData.totalLiabilities}" maxFractionDigits="0"/></td>
                                <td class="text-end ${almData.overallGap < 0 ? 'text-danger' : ''}"><fmt:formatNumber value="${almData.overallGap}" maxFractionDigits="0"/></td>
                                <td colspan="2"></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>

        <%-- Risk Assessment --%>
        <div class="card shadow mb-4">
            <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-clipboard2-data"></i> Risk Assessment</h5></div>
            <div class="card-body">
                <p class="mb-0"><c:out value="${almData.riskAssessment}"/></p>
            </div>
        </div>

        <c:if test="${almSnapshot != null}">
        <div class="audit-disclaimer mt-3"><i class="bi bi-shield-lock"></i> Snapshot: <c:out value="${almSnapshot.generatedAt}"/> | Hash: <code>${almSnapshot.hashChecksum.substring(0, 16)}...</code> | RBI ALM Ref: DBR.No.BP.BC.21/21.04.098/2017-18</div>
        </c:if>
    </c:when>
    <c:otherwise>
        <div class="alert alert-warning"><i class="bi bi-exclamation-triangle"></i> ALM snapshot not available for <c:out value="${businessDate}"/>. Run EOD to generate.</div>
    </c:otherwise>
</c:choose>

<%@ include file="../layout/footer.jsp" %>
