<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-check2-square"></i> Settlement Details</h3>
    <a href="${pageContext.request.contextPath}/settlements" class="btn btn-secondary">Back</a>
</div>

<div class="row justify-content-center">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-body">
                <table class="table table-borderless">
                    <tr><td class="text-muted" width="200">Reference</td><td><code class="fs-5">${settlement.settlementRef}</code></td></tr>
                    <tr><td class="text-muted">Settlement Date</td><td>${settlement.settlementDate}</td></tr>
                    <tr><td class="text-muted">Status</td><td><span class="badge bg-success fs-6">${settlement.status}</span></td></tr>
                    <tr><td class="text-muted">Total Debit</td><td class="text-danger fs-5">${settlement.totalDebit}</td></tr>
                    <tr><td class="text-muted">Total Credit</td><td class="text-success fs-5">${settlement.totalCredit}</td></tr>
                    <tr><td class="text-muted">Net Amount</td><td class="fw-bold fs-4 text-primary">${settlement.netAmount}</td></tr>
                    <tr><td class="text-muted">Transaction Count</td><td>${settlement.transactionCount}</td></tr>
                    <tr><td class="text-muted">Remarks</td><td>${settlement.remarks}</td></tr>
                    <tr><td class="text-muted">Processed By</td><td><c:if test="${settlement.processedBy != null}">${settlement.processedBy.username}</c:if></td></tr>
                    <tr><td class="text-muted">Created</td><td>${settlement.createdAt}</td></tr>
                    <tr><td class="text-muted">Completed</td><td>${settlement.completedAt}</td></tr>
                </table>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout/footer.jsp" %>
