<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-receipt"></i> Transaction Details</h3>
    <a href="${pageContext.request.contextPath}/transactions" class="btn btn-secondary">Back</a>
</div>

<div class="row">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0">Transaction Information</h5></div>
            <div class="card-body">
                <table class="table table-borderless">
                    <tr><td class="text-muted" width="200">Reference</td><td><code class="fs-5">${transaction.transactionRef}</code></td></tr>
                    <tr><td class="text-muted">Type</td><td>
                        <c:choose>
                            <c:when test="${transaction.transactionType == 'DEPOSIT'}"><span class="badge bg-success fs-6">DEPOSIT</span></c:when>
                            <c:when test="${transaction.transactionType == 'WITHDRAWAL'}"><span class="badge bg-danger fs-6">WITHDRAWAL</span></c:when>
                            <c:when test="${transaction.transactionType == 'TRANSFER'}"><span class="badge bg-info fs-6">TRANSFER</span></c:when>
                            <c:otherwise><span class="badge bg-secondary fs-6">${transaction.transactionType}</span></c:otherwise>
                        </c:choose>
                    </td></tr>
                    <tr><td class="text-muted">Status</td><td><span class="badge bg-success">${transaction.status}</span></td></tr>
                    <tr><td class="text-muted">Amount</td><td><span class="fs-4 fw-bold text-primary">${transaction.amount} ${transaction.currency}</span></td></tr>
                    <tr><td class="text-muted">Source Account</td><td><c:if test="${transaction.sourceAccount != null}"><code>${transaction.sourceAccount.accountNumber}</code> - ${transaction.sourceAccount.customerName}</c:if><c:if test="${transaction.sourceAccount == null}">N/A</c:if></td></tr>
                    <tr><td class="text-muted">Destination Account</td><td><c:if test="${transaction.destinationAccount != null}"><code>${transaction.destinationAccount.accountNumber}</code> - ${transaction.destinationAccount.customerName}</c:if><c:if test="${transaction.destinationAccount == null}">N/A</c:if></td></tr>
                    <tr><td class="text-muted">Description</td><td>${transaction.description}</td></tr>
                    <tr><td class="text-muted">Narration</td><td>${transaction.narration}</td></tr>
                    <tr><td class="text-muted">Performed By</td><td><c:if test="${transaction.performedBy != null}">${transaction.performedBy.username}</c:if></td></tr>
                    <tr><td class="text-muted">Date</td><td>${transaction.createdAt}</td></tr>
                </table>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-journal-text"></i> Ledger Entries</h5></div>
            <div class="card-body">
                <c:forEach var="entry" items="${ledgerEntries}">
                    <div class="border rounded p-2 mb-2">
                        <div class="d-flex justify-content-between">
                            <span class="badge ${entry.entryType == 'DEBIT' ? 'bg-danger' : 'bg-success'}">${entry.entryType}</span>
                            <strong>${entry.amount}</strong>
                        </div>
                        <small class="text-muted">GL: ${entry.glAccountCode}</small><br>
                        <small>${entry.narration}</small><br>
                        <small class="text-muted">Balance After: ${entry.balanceAfter}</small>
                    </div>
                </c:forEach>
                <c:if test="${empty ledgerEntries}">
                    <p class="text-muted text-center">No ledger entries</p>
                </c:if>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
