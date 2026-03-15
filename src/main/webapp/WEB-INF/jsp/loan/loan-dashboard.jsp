<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-cash-coin"></i> Loan Portfolio Dashboard</h3>
    <div>
        <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
        <a href="${pageContext.request.contextPath}/loan/create" class="btn btn-sm btn-primary"><i class="bi bi-plus-circle"></i> New Loan</a>
        </c:if>
        <a href="${pageContext.request.contextPath}/loan/list" class="btn btn-sm btn-outline-primary"><i class="bi bi-list-ul"></i> Full Portfolio</a>
        <a href="${pageContext.request.contextPath}/loan/npa-monitor" class="btn btn-sm btn-outline-danger"><i class="bi bi-exclamation-triangle"></i> NPA Monitor</a>
    </div>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Portfolio Summary Cards --%>
<div class="row g-3 mb-4">
    <div class="col-md-3">
        <div class="card shadow border-start border-4 border-primary">
            <div class="card-body text-center">
                <small class="text-muted d-block">Total Portfolio</small>
                <h3 class="mb-0"><fmt:formatNumber value="${totalPortfolio}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></h3>
                <small class="text-muted"><c:out value="${totalLoans}"/> loans</small>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow border-start border-4 border-info">
            <div class="card-body text-center">
                <small class="text-muted d-block">Total Outstanding</small>
                <h3 class="mb-0"><fmt:formatNumber value="${totalOutstanding}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></h3>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow border-start border-4 ${npaCount > 0 ? 'border-danger' : 'border-success'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">NPA Count</small>
                <h3 class="mb-0 ${npaCount > 0 ? 'text-danger' : 'text-success'}"><c:out value="${npaCount}"/></h3>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow border-start border-4 ${npaPercent > 5 ? 'border-danger' : 'border-success'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">NPA %</small>
                <h3 class="mb-0 ${npaPercent > 5 ? 'text-danger' : 'text-success'}"><c:out value="${npaPercent}"/>%</h3>
            </div>
        </div>
    </div>
</div>

<%-- Asset Classification (RBI IRAC) --%>
<div class="card shadow mb-4 reg-card">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-pie-chart"></i> Asset Classification (RBI IRAC)</h5></div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-sm reg-table">
                <thead class="table-light">
                    <tr><th>Classification</th><th class="text-end">Count</th><th class="text-end">Outstanding</th><th>Provision Rate</th><th>Status</th></tr>
                </thead>
                <tbody>
                    <tr>
                        <td><span class="badge bg-success">STANDARD</span></td>
                        <td class="text-end"><c:out value="${standardCount}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${standardAmt}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></td>
                        <td>0.40%</td>
                        <td><span class="badge bg-success">Performing</span></td>
                    </tr>
                    <tr>
                        <td><span class="badge bg-warning text-dark">SUBSTANDARD</span></td>
                        <td class="text-end"><c:out value="${substandardCount}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${substandardAmt}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></td>
                        <td>15.00%</td>
                        <td><span class="badge bg-warning text-dark">NPA</span></td>
                    </tr>
                    <tr>
                        <td><span class="badge bg-danger">DOUBTFUL</span></td>
                        <td class="text-end"><c:out value="${doubtfulCount}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${doubtfulAmt}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></td>
                        <td>25.00%</td>
                        <td><span class="badge bg-danger">NPA</span></td>
                    </tr>
                    <tr>
                        <td><span class="badge" style="background-color:#8b0000;color:#fff;">LOSS</span></td>
                        <td class="text-end"><c:out value="${lossCount}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${lossAmt}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></td>
                        <td>100.00%</td>
                        <td><span class="badge" style="background-color:#8b0000;color:#fff;">NPA</span></td>
                    </tr>
                </tbody>
                <tfoot class="table-light fw-bold">
                    <tr>
                        <td>TOTAL</td>
                        <td class="text-end"><c:out value="${totalLoans}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${totalOutstanding}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></td>
                        <td colspan="2"></td>
                    </tr>
                </tfoot>
            </table>
        </div>
    </div>
</div>

<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    Loan portfolio data derived from LoanAccount entities linked to immutable ledger entries.
    NPA classification follows RBI IRAC norms (90-day DPD threshold). Provisioning per RBI Master Circular.
</div>

<%@ include file="../layout/footer.jsp" %>
