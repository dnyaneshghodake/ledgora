<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-wallet2"></i> Account Details</h3>
    <div>
        <a href="${pageContext.request.contextPath}/accounts/${account.id}/edit" class="btn btn-outline-secondary">
            <i class="bi bi-pencil"></i> Edit
        </a>
        <a href="${pageContext.request.contextPath}/transactions/history/${account.accountNumber}" class="btn btn-outline-info">
            <i class="bi bi-clock-history"></i> Transaction History
        </a>
        <a href="${pageContext.request.contextPath}/accounts" class="btn btn-secondary">Back</a>
    </div>
</div>

<div class="row">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0">Account Information</h5></div>
            <div class="card-body">
                <table class="table table-borderless">
                    <tr><td class="text-muted" width="200">Account Number</td><td><code class="fs-5">${account.accountNumber}</code></td></tr>
                    <tr><td class="text-muted">Account Name</td><td>${account.accountName}</td></tr>
                    <tr><td class="text-muted">Type</td><td><span class="badge bg-info">${account.accountType}</span></td></tr>
                    <tr><td class="text-muted">Status</td><td>
                        <c:choose>
                            <c:when test="${account.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                            <c:when test="${account.status == 'SUSPENDED'}"><span class="badge bg-warning">SUSPENDED</span></c:when>
                            <c:otherwise><span class="badge bg-danger">${account.status}</span></c:otherwise>
                        </c:choose>
                    </td></tr>
                    <tr><td class="text-muted">Balance</td><td><span class="fs-4 fw-bold text-primary">${account.balance} ${account.currency}</span></td></tr>
                    <tr><td class="text-muted">Branch Code</td><td>${account.branchCode}</td></tr>
                    <tr><td class="text-muted">GL Account Code</td><td>${account.glAccountCode}</td></tr>
                    <tr><td class="text-muted">Created</td><td>${account.createdAt}</td></tr>
                </table>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0">Customer Info</h5></div>
            <div class="card-body">
                <p><strong>${account.customerName}</strong></p>
                <p><i class="bi bi-envelope"></i> ${account.customerEmail}</p>
                <p><i class="bi bi-telephone"></i> ${account.customerPhone}</p>
            </div>
        </div>
        <div class="card shadow mt-3">
            <div class="card-header bg-white"><h5 class="mb-0">Quick Actions</h5></div>
            <div class="card-body d-grid gap-2">
                <a href="${pageContext.request.contextPath}/transactions/deposit?account=${account.accountNumber}" class="btn btn-outline-success btn-sm">Deposit</a>
                <a href="${pageContext.request.contextPath}/transactions/withdraw?account=${account.accountNumber}" class="btn btn-outline-danger btn-sm">Withdraw</a>
                <form method="post" action="${pageContext.request.contextPath}/accounts/${account.id}/status" class="d-inline">
                    <c:if test="${account.status == 'ACTIVE'}">
                        <input type="hidden" name="status" value="SUSPENDED"/>
                        <button type="submit" class="btn btn-outline-warning btn-sm w-100" onclick="return confirm('Suspend this account?')">Suspend</button>
                    </c:if>
                    <c:if test="${account.status == 'SUSPENDED'}">
                        <input type="hidden" name="status" value="ACTIVE"/>
                        <button type="submit" class="btn btn-outline-success btn-sm w-100">Reactivate</button>
                    </c:if>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
