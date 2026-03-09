<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-arrow-left-right"></i> Transactions</h3>
    <div>
        <a href="${pageContext.request.contextPath}/transactions/deposit" class="btn btn-success"><i class="bi bi-cash-stack"></i> Deposit</a>
        <a href="${pageContext.request.contextPath}/transactions/withdraw" class="btn btn-warning"><i class="bi bi-cash-coin"></i> Withdraw</a>
        <a href="${pageContext.request.contextPath}/transactions/transfer" class="btn btn-primary"><i class="bi bi-arrow-left-right"></i> Transfer</a>
    </div>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success"><c:out value="${message}"/></div>
</c:if>

<%-- Search / Filter Section --%>
<div class="card shadow mb-4">
    <div class="card-header bg-light"><h6 class="mb-0"><i class="bi bi-funnel"></i> Search &amp; Filter</h6></div>
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/transactions" class="row g-2">
            <div class="col-md-4">
                <input type="text" name="accountId" class="form-control" placeholder="Account ID" value="<c:out value="${param.accountId}"/>"/>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-outline-primary w-100"><i class="bi bi-search"></i> Search</button>
            </div>
        </form>
    </div>
</div>

<div class="card shadow">
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty transactions}">
                <div class="table-responsive">
                    <table class="table table-hover table-sm">
                        <thead class="table-light">
                            <tr><th>ID</th><th>Date</th><th>Type</th><th>Account</th><th>Amount</th><th>Status</th><th>Actions</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="tx" items="${transactions}">
                            <tr>
                                <td><c:out value="${tx.id}"/></td>
                                <td><small><c:out value="${tx.transactionDate}"/></small></td>
                                <td><c:out value="${tx.transactionType}"/></td>
                                <td><code><c:out value="${tx.accountNumber}"/></code></td>
                                <td class="fw-bold"><c:out value="${tx.amount}"/></td>
                                <td><span class="badge bg-success"><c:out value="${tx.status}"/></span></td>
                                <td><a href="${pageContext.request.contextPath}/transactions/${tx.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-arrow-left-right" style="font-size: 3rem;"></i>
                    <p class="mt-2">No transactions found.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
