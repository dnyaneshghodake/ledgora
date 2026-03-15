<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-calculator"></i> Trial Balance</h3>
    <div>
        <span class="badge bg-dark me-2"><i class="bi bi-calendar3"></i> <c:out value="${businessDate}"/></span>
        <span class="eod-verified"><i class="bi bi-check-circle-fill"></i> EOD Verified</span>
        <a href="${pageContext.request.contextPath}/regulatory/dashboard" class="btn btn-sm btn-outline-secondary ms-2"><i class="bi bi-arrow-left"></i> Dashboard</a>
    </div>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<c:choose>
    <c:when test="${tbAvailable}">
        <div class="row g-3 mb-4">
            <div class="col-md-4"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Total Debit</small><h4 class="mb-0"><fmt:formatNumber value="${tbData.totalDebits}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></h4></div></div>
            <div class="col-md-4"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Total Credit</small><h4 class="mb-0"><fmt:formatNumber value="${tbData.totalCredits}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></h4></div></div>
            <div class="col-md-4"><div class="reg-kpi-card text-center"><small class="text-muted d-block">Status</small><c:choose><c:when test="${tbData.balanced}"><span class="badge bg-success fs-6">Balanced</span></c:when><c:otherwise><span class="badge bg-danger fs-6">IMBALANCE</span></c:otherwise></c:choose></div></div>
        </div>
        <div class="card shadow">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-sm table-hover reg-table">
                        <thead class="table-light">
                            <tr><th>GL Code</th><th>GL Name</th><th>Type</th><th class="text-end">Debit Balance</th><th class="text-end">Credit Balance</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="line" items="${tbData.lines}">
                            <tr>
                                <td><code><c:out value="${line.glCode}"/></code></td>
                                <td><c:out value="${line.glName}"/></td>
                                <td><span class="badge bg-light text-dark"><c:out value="${line.accountType}"/></span></td>
                                <td class="text-end"><fmt:formatNumber value="${line.debitBalance}" maxFractionDigits="2"/></td>
                                <td class="text-end"><fmt:formatNumber value="${line.creditBalance}" maxFractionDigits="2"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                        <tfoot class="table-light fw-bold">
                            <tr>
                                <td colspan="3">TOTAL</td>
                                <td class="text-end"><fmt:formatNumber value="${tbData.totalDebits}" maxFractionDigits="2"/></td>
                                <td class="text-end"><fmt:formatNumber value="${tbData.totalCredits}" maxFractionDigits="2"/></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>
        <c:if test="${tbSnapshot != null}">
        <div class="audit-disclaimer mt-3"><i class="bi bi-shield-lock"></i> Snapshot: <c:out value="${tbSnapshot.generatedAt}"/> | Hash: <code>${tbSnapshot.hashChecksum.substring(0, 16)}...</code> | Status: FINAL</div>
        </c:if>
    </c:when>
    <c:otherwise>
        <div class="alert alert-warning"><i class="bi bi-exclamation-triangle"></i> Trial Balance snapshot not available for <c:out value="${businessDate}"/>. Run EOD to generate.</div>
    </c:otherwise>
</c:choose>

<%@ include file="../layout/footer.jsp" %>
