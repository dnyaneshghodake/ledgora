<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-cash-coin"></i> Loan Detail — <c:out value="${loan.loanAccountNumber}"/></h3>
    <a href="${pageContext.request.contextPath}/loan/list" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back to List</a>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}"><div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div></c:if>
<c:if test="${not empty error}"><div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div></c:if>

<c:if test="${loan != null}">
<%-- Sanction Details --%>
<div class="card shadow mb-4 reg-card">
    <div class="card-header bg-white"><h5 class="mb-0">Sanction Details</h5></div>
    <div class="card-body">
        <div class="row">
            <div class="col-md-3"><small class="text-muted d-block">Loan Account</small><strong><c:out value="${loan.loanAccountNumber}"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">Principal Amount</small><strong><fmt:formatNumber value="${loan.principalAmount}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">Outstanding Principal</small><strong><fmt:formatNumber value="${loan.outstandingPrincipal}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">Status</small>
                <c:choose>
                    <c:when test="${loan.status == 'ACTIVE'}"><span class="badge bg-success fs-6">ACTIVE</span></c:when>
                    <c:when test="${loan.status == 'NPA'}"><span class="badge bg-danger fs-6">NPA</span></c:when>
                    <c:when test="${loan.status == 'CLOSED'}"><span class="badge bg-secondary fs-6">CLOSED</span></c:when>
                    <c:when test="${loan.status == 'WRITTEN_OFF'}"><span class="badge" style="background-color:#8b0000;color:#fff;font-size:0.9rem;">WRITTEN OFF</span></c:when>
                </c:choose>
            </div>
        </div>
        <div class="row mt-3">
            <div class="col-md-3"><small class="text-muted d-block">Disbursement Date</small><c:out value="${loan.disbursementDate}"/></div>
            <div class="col-md-3"><small class="text-muted d-block">Maturity Date</small><c:out value="${loan.maturityDate}"/></div>
            <div class="col-md-3"><small class="text-muted d-block">DPD</small><span class="${loan.dpd > 90 ? 'text-danger fw-bold' : ''}"><c:out value="${loan.dpd}"/> days</span></div>
            <div class="col-md-3"><small class="text-muted d-block">NPA Classification</small>
                <c:choose>
                    <c:when test="${loan.npaClassification == 'STANDARD'}"><span class="badge bg-success">Standard</span></c:when>
                    <c:when test="${loan.npaClassification == 'SUBSTANDARD'}"><span class="badge bg-warning text-dark">Substandard</span></c:when>
                    <c:when test="${loan.npaClassification == 'DOUBTFUL'}"><span class="badge bg-danger">Doubtful</span></c:when>
                    <c:when test="${loan.npaClassification == 'LOSS'}"><span class="badge" style="background-color:#8b0000;color:#fff;">Loss</span></c:when>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%-- Interest Accrual Summary --%>
<div class="card shadow mb-4 reg-card">
    <div class="card-header bg-white"><h5 class="mb-0">Interest & Provision</h5></div>
    <div class="card-body">
        <div class="row">
            <div class="col-md-4"><small class="text-muted d-block">Accrued Interest</small><h5><fmt:formatNumber value="${loan.accruedInterest}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></h5></div>
            <div class="col-md-4"><small class="text-muted d-block">Provision Amount</small><h5><fmt:formatNumber value="${loan.provisionAmount}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></h5></div>
            <div class="col-md-4"><small class="text-muted d-block">NPA Date</small><h5><c:out value="${loan.npaDate != null ? loan.npaDate : 'N/A'}"/></h5></div>
        </div>
    </div>
</div>

<%-- Repayment Form (Admin/Manager only) --%>
<c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
<c:if test="${loan.status == 'ACTIVE' || loan.status == 'NPA'}">
<div class="card shadow mb-4 border-primary">
    <div class="card-header bg-primary bg-opacity-10"><h5 class="mb-0"><i class="bi bi-credit-card"></i> Process EMI Payment</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/loan/${loan.id}/repay" onsubmit="return confirm('Confirm EMI payment? This will be posted to the voucher engine.');">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Principal Component</label>
                    <input type="number" name="principalAmount" step="0.01" min="0" class="form-control" required/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Interest Component</label>
                    <input type="number" name="interestAmount" step="0.01" min="0" class="form-control" required/>
                </div>
                <div class="col-md-4 d-flex align-items-end">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Post Payment</button>
                </div>
            </div>
        </form>
    </div>
</div>
</c:if>
</c:if>

<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i> All loan data linked to immutable ledger entries. EMI payments posted via voucher engine. NPA per RBI IRAC (90-day DPD).
</div>
</c:if>

<c:if test="${loan == null}">
    <div class="alert alert-warning"><i class="bi bi-exclamation-triangle"></i> Loan not found.</div>
</c:if>

<%@ include file="../layout/footer.jsp" %>
