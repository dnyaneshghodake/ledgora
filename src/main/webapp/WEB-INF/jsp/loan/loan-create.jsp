<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-plus-circle"></i> New Loan — Disbursement</h3>
    <a href="${pageContext.request.contextPath}/loan/dashboard" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i> Dashboard</a>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}"><div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div></c:if>
<c:if test="${not empty error}"><div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div></c:if>

<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-file-earmark-text"></i> Loan Sanction & Disbursement</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/loan/create"
              onsubmit="return confirm('Confirm loan disbursement of ₹' + this.principalAmount.value + '? This will create a new loan account and post to the voucher engine.');">

            <div class="row g-3 mb-3">
                <div class="col-md-6">
                    <label class="form-label fw-bold">Loan Product <span class="text-danger">*</span></label>
                    <select name="productId" class="form-select" required>
                        <option value="">— Select Product —</option>
                        <c:forEach var="p" items="${products}">
                            <option value="${p.id}">
                                <c:out value="${p.productCode}"/> — <c:out value="${p.productName}"/>
                                (${p.interestRate}% ${p.interestType}, ${p.tenureMonths}m)
                            </option>
                        </c:forEach>
                    </select>
                    <small class="text-muted">Product defines interest rate, tenure, and GL mappings</small>
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-bold">Linked Customer Account <span class="text-danger">*</span></label>
                    <select name="linkedAccountId" class="form-select" required>
                        <option value="">— Select Account —</option>
                        <c:forEach var="a" items="${accounts}">
                            <option value="${a.id}">
                                <c:out value="${a.accountNumber}"/> — <c:out value="${a.accountName}"/>
                                (<c:out value="${a.accountType}"/>)
                            </option>
                        </c:forEach>
                    </select>
                    <small class="text-muted">Customer's operating account for EMI debit / disbursement credit</small>
                </div>
            </div>

            <div class="row g-3 mb-3">
                <div class="col-md-4">
                    <label class="form-label fw-bold">Principal Amount (₹) <span class="text-danger">*</span></label>
                    <input type="number" name="principalAmount" step="0.01" min="1" class="form-control" required
                           placeholder="e.g. 500000"/>
                    <small class="text-muted">Sanctioned loan amount to be disbursed</small>
                </div>
            </div>

            <hr/>
            <div class="d-flex justify-content-between align-items-center">
                <div class="text-muted">
                    <i class="bi bi-info-circle"></i>
                    Disbursement creates a Loan Asset (DR Loan Asset GL, CR Customer Account).
                    EMI schedule is auto-generated using reducing balance method.
                </div>
                <div>
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <a href="${pageContext.request.contextPath}/loan/dashboard" class="btn btn-outline-secondary me-2">Cancel</a>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Disburse Loan</button>
                </div>
            </div>
        </form>
    </div>
</div>

<c:if test="${not empty products}">
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-list-ul"></i> Available Loan Products</h5></div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-sm table-hover">
                <thead class="table-light">
                    <tr><th>Code</th><th>Product Name</th><th>Rate</th><th>Type</th><th>Tenure</th><th>Compounding</th><th>NPA Threshold</th></tr>
                </thead>
                <tbody>
                    <c:forEach var="p" items="${products}">
                    <tr>
                        <td><code><c:out value="${p.productCode}"/></code></td>
                        <td><c:out value="${p.productName}"/></td>
                        <td><c:out value="${p.interestRate}"/>%</td>
                        <td><c:out value="${p.interestType}"/></td>
                        <td><c:out value="${p.tenureMonths}"/> months</td>
                        <td><c:out value="${p.compoundingFrequency}"/></td>
                        <td><c:out value="${p.npaDaysThreshold}"/> days</td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>
</c:if>

<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    Loan disbursement follows RBI Master Directions on Lending. All postings via voucher engine.
    Amortization schedule generated using reducing balance EMI formula per Finacle standards.
</div>

<%@ include file="../layout/footer.jsp" %>
