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
                    <label class="form-label cbs-field-required">Debit Account</label>
                    <div class="input-group">
                        <input type="text" name="debitAccountNumber" id="debitAccountNumber" class="form-control" readonly
                               placeholder="Use lookup to select" required />
                        <button type="button" class="btn btn-outline-primary" onclick="openAccountLookup('debitAccountNumber','debitAccountName')" title="Search Account">
                            <i class="bi bi-search"></i>
                        </button>
                    </div>
                    <input type="hidden" id="debitAccountName"/>
                    <small id="debitAcctInfo" class="text-muted"></small>
                </div>
                <div class="col-md-6">
                    <label class="form-label cbs-field-required">Credit Account</label>
                    <div class="input-group">
                        <input type="text" name="creditAccountNumber" id="creditAccountNumber" class="form-control" readonly
                               placeholder="Use lookup to select" required />
                        <button type="button" class="btn btn-outline-primary" onclick="openAccountLookup('creditAccountNumber','creditAccountName')" title="Search Account">
                            <i class="bi bi-search"></i>
                        </button>
                    </div>
                    <input type="hidden" id="creditAccountName"/>
                    <small id="creditAcctInfo" class="text-muted"></small>
                </div>
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Amount</label>
                    <input type="number" name="amount" class="form-control" step="0.01" min="0.01" placeholder="0.00" required
                           id="voucherAmount"/>
                    <div id="voucherAmtError" class="cbs-inline-error">Amount must be greater than zero.</div>
                </div>
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Currency</label>
                    <select name="currency" class="form-select" required>
                        <option value="INR">INR</option>
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                        <option value="GBP">GBP</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Voucher Type</label>
                    <select name="voucherType" class="form-select" required>
                        <option value="TRANSFER">Transfer</option>
                        <option value="CASH">Cash</option>
                        <option value="CLEARING">Clearing</option>
                        <option value="ADJUSTMENT">Adjustment</option>
                    </select>
                </div>
                <div class="col-12">
                    <label class="form-label">Narration</label>
                    <textarea name="narration" class="form-control" rows="2" maxlength="500" placeholder="Enter transaction narration"></textarea>
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
