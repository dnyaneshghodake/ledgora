<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-arrow-left-right"></i> Transactions</h3>
    <div>
        <a href="${pageContext.request.contextPath}/transactions/deposit" class="btn btn-success btn-sm"><i class="bi bi-arrow-down-circle"></i> Deposit</a>
        <a href="${pageContext.request.contextPath}/transactions/withdraw" class="btn btn-danger btn-sm"><i class="bi bi-arrow-up-circle"></i> Withdraw</a>
        <a href="${pageContext.request.contextPath}/transactions/transfer" class="btn btn-info btn-sm"><i class="bi bi-arrow-left-right"></i> Transfer</a>
    </div>
</div>

<div class="card shadow mb-3">
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/transactions" class="row g-3">
            <div class="col-md-8">
                <input type="text" name="accountNumber" class="form-control" placeholder="Filter by account number..." value="${accountNumber}">
            </div>
            <div class="col-md-4">
                <button type="submit" class="btn btn-outline-primary"><i class="bi bi-search"></i> Search</button>
                <a href="${pageContext.request.contextPath}/transactions" class="btn btn-outline-secondary">Clear</a>
            </div>
        </form>
    </div>
</div>

<div class="card shadow">
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr>
                    <th>Ref</th>
                    <th>Type</th>
                    <th>Amount</th>
                    <th>Source</th>
                    <th>Destination</th>
                    <th>Status</th>
                    <th>Description</th>
                    <th>Date</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="txn" items="${transactions}">
                    <tr>
                        <td><code>${txn.transactionRef}</code></td>
                        <td>
                            <c:choose>
                                <c:when test="${txn.transactionType == 'DEPOSIT'}"><span class="badge bg-success">DEPOSIT</span></c:when>
                                <c:when test="${txn.transactionType == 'WITHDRAWAL'}"><span class="badge bg-danger">WITHDRAWAL</span></c:when>
                                <c:when test="${txn.transactionType == 'TRANSFER'}"><span class="badge bg-info">TRANSFER</span></c:when>
                                <c:otherwise><span class="badge bg-secondary">${txn.transactionType}</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="fw-bold">${txn.amount} ${txn.currency}</td>
                        <td><c:if test="${txn.sourceAccount != null}"><code>${txn.sourceAccount.accountNumber}</code></c:if><c:if test="${txn.sourceAccount == null}">-</c:if></td>
                        <td><c:if test="${txn.destinationAccount != null}"><code>${txn.destinationAccount.accountNumber}</code></c:if><c:if test="${txn.destinationAccount == null}">-</c:if></td>
                        <td><span class="badge bg-success">${txn.status}</span></td>
                        <td>${txn.description}</td>
                        <td>${txn.createdAt}</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/transactions/${txn.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty transactions}">
                    <tr><td colspan="9" class="text-center text-muted py-4">No transactions found</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="layout/footer.jsp" %>
