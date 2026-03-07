<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-wallet2"></i> Accounts</h3>
    <a href="${pageContext.request.contextPath}/accounts/create" class="btn btn-primary">
        <i class="bi bi-plus-circle"></i> Create Account
    </a>
</div>

<div class="card shadow mb-4">
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/accounts" class="row g-3">
            <div class="col-md-4">
                <input type="text" name="search" class="form-control" placeholder="Search by customer name..." value="${search}">
            </div>
            <div class="col-md-3">
                <select name="status" class="form-select">
                    <option value="">All Statuses</option>
                    <c:forEach var="s" items="${accountStatuses}">
                        <option value="${s}" ${selectedStatus == s.name() ? 'selected' : ''}>${s}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-3">
                <select name="type" class="form-select">
                    <option value="">All Types</option>
                    <c:forEach var="t" items="${accountTypes}">
                        <option value="${t}" ${selectedType == t.name() ? 'selected' : ''}>${t}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-outline-primary w-100"><i class="bi bi-search"></i> Filter</button>
            </div>
        </form>
    </div>
</div>

<div class="card shadow">
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr>
                    <th>Account Number</th>
                    <th>Account Name</th>
                    <th>Customer</th>
                    <th>Type</th>
                    <th>Status</th>
                    <th>Balance</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="account" items="${accounts}">
                    <tr>
                        <td><code>${account.accountNumber}</code></td>
                        <td>${account.accountName}</td>
                        <td>${account.customerName}</td>
                        <td><span class="badge bg-info">${account.accountType}</span></td>
                        <td>
                            <c:choose>
                                <c:when test="${account.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                                <c:when test="${account.status == 'SUSPENDED'}"><span class="badge bg-warning">SUSPENDED</span></c:when>
                                <c:when test="${account.status == 'CLOSED'}"><span class="badge bg-danger">CLOSED</span></c:when>
                                <c:otherwise><span class="badge bg-secondary">${account.status}</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="fw-bold">${account.balance} ${account.currency}</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/accounts/${account.id}" class="btn btn-sm btn-outline-primary">
                                <i class="bi bi-eye"></i>
                            </a>
                            <a href="${pageContext.request.contextPath}/accounts/${account.id}/edit" class="btn btn-sm btn-outline-secondary">
                                <i class="bi bi-pencil"></i>
                            </a>
                            <a href="${pageContext.request.contextPath}/transactions/history/${account.accountNumber}" class="btn btn-sm btn-outline-info">
                                <i class="bi bi-clock-history"></i>
                            </a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty accounts}">
                    <tr><td colspan="7" class="text-center text-muted py-4">No accounts found</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
