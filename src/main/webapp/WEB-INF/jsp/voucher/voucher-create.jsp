<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-plus-square"></i> Create Voucher</h3>
    <a href="${pageContext.request.contextPath}/vouchers" class="btn btn-secondary">
        <i class="bi bi-arrow-left"></i> Back to Voucher Inquiry
    </a>
</div>

<div class="card shadow cbs-lockable">
    <div class="card-header bg-white"><h5 class="mb-0">New Voucher Entry</h5></div>
    <div class="card-body">
        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>
        <form method="post" action="${pageContext.request.contextPath}/vouchers/create" id="voucherForm">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <div class="row g-3">
                <div class="col-md-6">
                    <label class="form-label">Debit Account</label>
                    <input type="text" name="debitAccountNumber" class="form-control" placeholder="Enter debit account number" required />
                </div>
                <div class="col-md-6">
                    <label class="form-label">Credit Account</label>
                    <input type="text" name="creditAccountNumber" class="form-control" placeholder="Enter credit account number" required />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Amount</label>
                    <input type="number" name="amount" class="form-control" step="0.01" min="0.01" placeholder="0.00" required />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Currency</label>
                    <select name="currency" class="form-select">
                        <option value="INR">INR</option>
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                        <option value="GBP">GBP</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Voucher Type</label>
                    <select name="voucherType" class="form-select">
                        <option value="TRANSFER">Transfer</option>
                        <option value="CASH">Cash</option>
                        <option value="CLEARING">Clearing</option>
                        <option value="ADJUSTMENT">Adjustment</option>
                    </select>
                </div>
                <div class="col-12">
                    <label class="form-label">Narration</label>
                    <textarea name="narration" class="form-control" rows="2" placeholder="Enter transaction narration"></textarea>
                </div>
            </div>
            <div class="mt-4">
                <button type="submit" class="btn btn-primary">
                    <i class="bi bi-check-circle"></i> Submit Voucher
                </button>
                <button type="reset" class="btn btn-outline-secondary ms-2">
                    <i class="bi bi-arrow-counterclockwise"></i> Reset
                </button>
            </div>
        </form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
