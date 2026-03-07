<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-clock-history"></i> Transaction History - <code>${accountNumber}</code></h3>
    <a href="${pageContext.request.contextPath}/transactions" class="btn btn-secondary">Back</a>
</div>

<div class="row">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0">Transactions</h5></div>
            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead class="table-light">
                        <tr><th>Ref</th><th>Type</th><th>Amount</th><th>Status</th><th>Date</th><th>Actions</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="txn" items="${transactions}">
                            <tr>
                                <td><code>${txn.transactionRef}</code></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${txn.transactionType == 'DEPOSIT'}"><span class="badge bg-success">DEPOSIT</span></c:when>
                                        <c:when test="${txn.transactionType == 'WITHDRAWAL'}"><span class="badge bg-danger">WITHDRAWAL</span></c:when>
                                        <c:otherwise><span class="badge bg-info">TRANSFER</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="fw-bold">${txn.amount} ${txn.currency}</td>
                                <td><span class="badge bg-success">${txn.status}</span></td>
                                <td>${txn.createdAt}</td>
                                <td><a href="${pageContext.request.contextPath}/transactions/${txn.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty transactions}">
                            <tr><td colspan="6" class="text-center text-muted py-4">No transactions found</td></tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-journal-text"></i> Ledger Entries</h5></div>
            <div class="card-body" style="max-height: 500px; overflow-y: auto;">
                <c:forEach var="entry" items="${ledgerEntries}">
                    <div class="border rounded p-2 mb-2">
                        <div class="d-flex justify-content-between">
                            <span class="badge ${entry.entryType == 'DEBIT' ? 'bg-danger' : 'bg-success'}">${entry.entryType}</span>
                            <strong>${entry.amount}</strong>
                        </div>
                        <small class="text-muted">GL: ${entry.glAccountCode}</small><br>
                        <small>${entry.narration}</small>
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
