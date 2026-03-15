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
        <%-- Step 1: Preview form — posts to /loan/preview for schedule simulation (Finacle LACSMNT) --%>
        <form method="post" action="${pageContext.request.contextPath}/loan/preview">

            <div class="row g-3 mb-3">
                <div class="col-md-6">
                    <label class="form-label fw-bold">Loan Product <span class="text-danger">*</span></label>
                    <select name="productId" class="form-select" required>
                        <option value="">— Select Product —</option>
                        <c:forEach var="p" items="${products}">
                            <option value="${p.id}" ${p.id == selectedProductId ? 'selected' : ''}>
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
                            <option value="${a.id}" ${a.id == selectedAccountId ? 'selected' : ''}>
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
                           placeholder="e.g. 500000" value="${principalAmount != null ? principalAmount : ''}"/>
                    <small class="text-muted">Sanctioned loan amount to be disbursed</small>
                </div>
            </div>

            <hr/>
            <div class="d-flex justify-content-between align-items-center">
                <div class="text-muted">
                    <i class="bi bi-info-circle"></i>
                    Preview computes the full EMI amortization schedule without creating the loan.
                    Review the schedule below before confirming disbursement.
                </div>
                <div>
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <a href="${pageContext.request.contextPath}/loan/dashboard" class="btn btn-outline-secondary me-2">Cancel</a>
                    <button type="submit" class="btn btn-outline-primary"><i class="bi bi-calculator"></i> Preview Schedule</button>
                </div>
            </div>
        </form>
    </div>
</div>

<%-- Step 2: Schedule Preview + Confirm Disbursement (Finacle LACSMNT pre-disbursement view) --%>
<c:if test="${not empty preview}">
<div class="card shadow mb-4 border-success">
    <div class="card-header bg-success bg-opacity-10">
        <h5 class="mb-0"><i class="bi bi-calendar-check"></i> Amortization Schedule Preview</h5>
    </div>
    <div class="card-body">
        <div class="row mb-3">
            <div class="col-md-3"><small class="text-muted d-block">Product</small><strong><c:out value="${preview.productName}"/> (<c:out value="${preview.productCode}"/>)</strong></div>
            <div class="col-md-2"><small class="text-muted d-block">Rate</small><strong><c:out value="${preview.interestRate}"/>% p.a. (<c:out value="${preview.interestType}"/>)</strong></div>
            <div class="col-md-2"><small class="text-muted d-block">Tenure</small><strong><c:out value="${preview.tenureMonths}"/> months</strong></div>
            <div class="col-md-2"><small class="text-muted d-block">EMI</small><strong><fmt:formatNumber value="${preview.emiAmount}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">Principal</small><strong><fmt:formatNumber value="${preview.principalAmount}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></strong></div>
        </div>
        <div class="row mb-3">
            <div class="col-md-3"><small class="text-muted d-block">Total Interest Payable</small><strong><fmt:formatNumber value="${preview.totalInterestPayable}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">Total Amount Payable</small><strong><fmt:formatNumber value="${preview.totalAmountPayable}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">First EMI Date</small><strong><c:out value="${preview.firstEmiDate}"/></strong></div>
            <div class="col-md-3"><small class="text-muted d-block">Last EMI Date</small><strong><c:out value="${preview.lastEmiDate}"/></strong></div>
        </div>

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
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="inst" items="${preview.installments}">
                    <tr>
                        <td><c:out value="${inst.number}"/></td>
                        <td><c:out value="${inst.dueDate}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${inst.emiAmount}" maxFractionDigits="2"/></td>
                        <td class="text-end"><fmt:formatNumber value="${inst.principalComponent}" maxFractionDigits="2"/></td>
                        <td class="text-end"><fmt:formatNumber value="${inst.interestComponent}" maxFractionDigits="2"/></td>
                        <td class="text-end"><fmt:formatNumber value="${inst.outstandingAfter}" maxFractionDigits="2"/></td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>

        <hr/>
        <%-- Confirm Disbursement form — posts to /loan/create with hidden fields --%>
        <form method="post" action="${pageContext.request.contextPath}/loan/create"
              onsubmit="return confirm('Confirm loan disbursement of ₹${principalAmount} to account ${selectedAccountNumber} (${selectedAccountName})? This will create a new loan account and post to the voucher engine.');">
            <input type="hidden" name="productId" value="${selectedProductId}"/>
            <input type="hidden" name="linkedAccountId" value="${selectedAccountId}"/>
            <input type="hidden" name="principalAmount" value="${principalAmount}"/>
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <div class="d-flex justify-content-between align-items-center">
                <div class="text-muted">
                    <i class="bi bi-info-circle"></i>
                    Review the schedule above. Modify inputs and re-preview, or confirm to create the loan.
                    Disbursement: DR Loan Asset GL, CR Customer Account via voucher engine.
                </div>
                <div>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Confirm Disbursement</button>
                </div>
            </div>
        </form>
    </div>
</div>
</c:if>

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
