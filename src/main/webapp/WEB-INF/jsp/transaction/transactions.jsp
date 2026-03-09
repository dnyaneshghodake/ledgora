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
                <input type="text" name="accountNumber" class="form-control" placeholder="Account Number" value="<c:out value="${param.accountNumber}"/>"/>
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
                                <td><small><c:out value="${tx.businessDate != null ? tx.businessDate : tx.createdAt}"/></small></td>
                                <td><c:out value="${tx.transactionType}"/></td>
                                <td>
                                    <c:if test="${tx.sourceAccount != null}"><code><c:out value="${tx.sourceAccount.accountNumber}"/></code></c:if>
                                    <c:if test="${tx.sourceAccount != null && tx.destinationAccount != null}"> &rarr; </c:if>
                                    <c:if test="${tx.destinationAccount != null}"><code><c:out value="${tx.destinationAccount.accountNumber}"/></code></c:if>
                                </td>
                                <td class="fw-bold"><c:out value="${tx.amount}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${tx.status == 'COMPLETED'}"><span class="badge bg-success">COMPLETED</span></c:when>
                                        <c:when test="${tx.status == 'PENDING_APPROVAL'}"><span class="badge bg-warning text-dark">PENDING APPROVAL</span></c:when>
                                        <c:when test="${tx.status == 'APPROVED'}"><span class="badge bg-info">APPROVED</span></c:when>
                                        <c:when test="${tx.status == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                                        <c:when test="${tx.status == 'REVERSED'}"><span class="badge bg-secondary">REVERSED</span></c:when>
                                        <c:when test="${tx.status == 'FAILED'}"><span class="badge bg-dark">FAILED</span></c:when>
                                        <c:otherwise><span class="badge bg-light text-dark"><c:out value="${tx.status}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
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
