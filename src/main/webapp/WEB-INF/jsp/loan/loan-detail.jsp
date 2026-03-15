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
<%-- Product & Sanction Details (Finacle Loan Inquiry) --%>
<div class="card shadow mb-4 reg-card">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-file-earmark-text"></i> Sanction & Product Details</h5></div>
    <div class="card-body">
        <div class="row">
            <div class="col-md-3"><small class="text-muted d-block">Loan Account</small><strong><c:out value="${loan.loanAccountNumber}"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">Borrower</small><strong><c:out value="${loan.borrowerName}"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">Linked Account</small><strong><c:out value="${loan.linkedAccount.accountNumber}"/></strong></div>
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
            <div class="col-md-3"><small class="text-muted d-block">Product</small><c:out value="${loan.loanProduct.productName}"/> (<c:out value="${loan.loanProduct.productCode}"/>)</div>
            <div class="col-md-3"><small class="text-muted d-block">Interest Rate</small><c:out value="${loan.interestRate != null ? loan.interestRate : loan.loanProduct.interestRate}"/>% p.a. (<c:out value="${loan.loanProduct.interestType}"/>)</div>
            <div class="col-md-3"><small class="text-muted d-block">Tenure</small><c:out value="${loan.loanProduct.tenureMonths}"/> months</div>
            <div class="col-md-3"><small class="text-muted d-block">EMI Amount</small><strong><c:if test="${loan.emiAmount != null}"><fmt:formatNumber value="${loan.emiAmount}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></c:if><c:if test="${loan.emiAmount == null}">N/A</c:if></strong></div>
        </div>
        <div class="row mt-3">
            <div class="col-md-3"><small class="text-muted d-block">Principal Amount</small><strong><fmt:formatNumber value="${loan.principalAmount}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">Outstanding Principal</small><strong class="${loan.outstandingPrincipal.compareTo(loan.principalAmount) < 0 ? 'text-success' : ''}"><fmt:formatNumber value="${loan.outstandingPrincipal}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">Currency</small><c:out value="${loan.currency}"/></div>
            <div class="col-md-3"><small class="text-muted d-block">Repaid</small><strong class="text-success"><fmt:formatNumber value="${loan.principalAmount - loan.outstandingPrincipal}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></strong></div>
        </div>
        <div class="row mt-3">
            <div class="col-md-3"><small class="text-muted d-block">Disbursement Date</small><c:out value="${loan.disbursementDate}"/></div>
            <div class="col-md-3"><small class="text-muted d-block">Maturity Date</small><c:out value="${loan.maturityDate}"/></div>
            <div class="col-md-3"><small class="text-muted d-block">DPD</small><span class="${loan.dpd > 90 ? 'text-danger fw-bold' : loan.dpd > 0 ? 'text-warning fw-bold' : ''}"><c:out value="${loan.dpd}"/> days</span></div>
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

<%-- Repayment Schedule (Finacle LACHST — Loan Account History) --%>
<c:if test="${not empty schedule}">
<div class="card shadow mb-4 reg-card">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-calendar-check"></i> Repayment Schedule</h5></div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-sm table-hover">
                <thead class="table-light">
                    <tr>
                        <th>#</th>
                        <th>Due Date</th>
                        <th class="text-end">EMI</th>
                        <th class="text-end">Principal</th>
                        <th class="text-end">Interest</th>
                        <th class="text-end">Outstanding After</th>
                        <th>Status</th>
                        <th class="text-end">Paid</th>
                        <th>Paid Date</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="inst" items="${schedule}">
                    <tr class="${inst.status == 'OVERDUE' ? 'table-danger' : inst.status == 'PAID' ? 'table-success' : inst.status == 'PARTIALLY_PAID' ? 'table-warning' : ''}">
                        <td><c:out value="${inst.installmentNumber}"/></td>
                        <td><c:out value="${inst.dueDate}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${inst.emiAmount}" maxFractionDigits="2"/></td>
                        <td class="text-end"><fmt:formatNumber value="${inst.principalComponent}" maxFractionDigits="2"/></td>
                        <td class="text-end"><fmt:formatNumber value="${inst.interestComponent}" maxFractionDigits="2"/></td>
                        <td class="text-end"><fmt:formatNumber value="${inst.outstandingPrincipal}" maxFractionDigits="2"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${inst.status == 'PAID'}"><span class="badge bg-success">Paid</span></c:when>
                                <c:when test="${inst.status == 'OVERDUE'}"><span class="badge bg-danger">Overdue</span></c:when>
                                <c:when test="${inst.status == 'DUE'}"><span class="badge bg-warning text-dark">Due</span></c:when>
                                <c:when test="${inst.status == 'PARTIALLY_PAID'}"><span class="badge bg-info">Partial</span></c:when>
                                <c:when test="${inst.status == 'WRITTEN_OFF'}"><span class="badge bg-dark">Written Off</span></c:when>
                                <c:otherwise><span class="badge bg-secondary">Scheduled</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="text-end"><c:if test="${inst.paidAmount != null && inst.paidAmount > 0}"><fmt:formatNumber value="${inst.paidAmount}" maxFractionDigits="2"/></c:if></td>
                        <td><c:out value="${inst.paidDate != null ? inst.paidDate : ''}"/></td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>
</c:if>

<%-- Repayment Form (Admin/Manager only) --%>
<c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
<c:if test="${loan.status == 'ACTIVE' || loan.status == 'NPA'}">
<div class="card shadow mb-4 border-primary">
    <div class="card-header bg-primary bg-opacity-10"><h5 class="mb-0"><i class="bi bi-credit-card"></i> Process EMI Payment</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/loan/${loan.id}/repay" onsubmit="return confirm('Confirm EMI payment of ₹' + (parseFloat(this.principalAmount.value) + parseFloat(this.interestAmount.value)).toFixed(2) + '? This will be posted to the voucher engine.');">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Principal Component</label>
                    <input type="number" name="principalAmount" step="0.01" min="0" max="${loan.outstandingPrincipal}" class="form-control" required/>
                    <small class="text-muted">Max: <fmt:formatNumber value="${loan.outstandingPrincipal}" maxFractionDigits="2"/></small>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Interest Component</label>
                    <input type="number" name="interestAmount" step="0.01" min="0" max="${loan.accruedInterest}" class="form-control" required/>
                    <small class="text-muted">Max: <fmt:formatNumber value="${loan.accruedInterest}" maxFractionDigits="2"/></small>
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
