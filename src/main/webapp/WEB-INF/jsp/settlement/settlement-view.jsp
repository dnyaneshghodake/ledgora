<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

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
                    <tr><td class="text-muted">Settlement Date</td><td>${settlement.businessDate}</td></tr>
                    <tr><td class="text-muted">Status</td><td><span class="badge bg-success fs-6">${settlement.status}</span></td></tr>
                    <tr><td class="text-muted">Transaction Count</td><td>${settlement.transactionCount}</td></tr>
                    <tr><td class="text-muted">Remarks</td><td>${settlement.remarks}</td></tr>
                    <tr><td class="text-muted">Processed By</td><td><c:if test="${settlement.processedBy != null}">${settlement.processedBy.username}</c:if></td></tr>
                    <tr><td class="text-muted">Started</td><td>${settlement.startTime}</td></tr>
                    <tr><td class="text-muted">Completed</td><td>${settlement.endTime}</td></tr>
                    <tr><td class="text-muted">Created</td><td>${settlement.createdAt}</td></tr>
                </table>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
