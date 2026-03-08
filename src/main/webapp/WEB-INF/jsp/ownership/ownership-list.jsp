<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-people"></i> Account Ownership Management</h3>
</div>

<c:if test="${not empty success}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${success}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<!-- Create Ownership Form -->
<div class="card shadow mb-4">
    <div class="card-header"><h5 class="mb-0"><i class="bi bi-plus-circle"></i> Create Ownership Link</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/ownership/create">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <div class="row g-3">
                <div class="col-md-3">
                    <label class="form-label">Account ID</label>
                    <input type="number" name="accountId" class="form-control" required/>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Customer ID</label>
                    <input type="number" name="customerMasterId" class="form-control" required/>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Ownership Type</label>
                    <select name="ownershipType" class="form-select" required>
                        <c:forEach var="t" items="${ownershipTypes}">
                            <option value="${t}"><c:out value="${t}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Percentage</label>
                    <input type="number" name="ownershipPercentage" class="form-control" step="0.01" min="0.01" max="100" value="100" required/>
                </div>
                <div class="col-md-2 d-flex align-items-end">
                    <button type="submit" class="btn btn-primary w-100"><i class="bi bi-send"></i> Submit</button>
                </div>
            </div>
        </form>
    </div>
</div>

<!-- Pending Approvals -->
<c:if test="${not empty pendingOwnerships}">
<div class="card shadow mb-4">
    <div class="card-header bg-warning text-dark"><h5 class="mb-0"><i class="bi bi-hourglass-split"></i> Pending Ownership Approvals</h5></div>
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>Account</th><th>Customer</th><th>Type</th><th>Percentage</th><th>Operational</th><th>Actions</th></tr>
            </thead>
            <tbody>
                <c:forEach var="o" items="${pendingOwnerships}">
                    <tr>
                        <td><c:out value="${o.account.accountNumber}"/></td>
                        <td><c:out value="${o.customerMaster.customerNumber}"/></td>
                        <td><span class="badge bg-info"><c:out value="${o.ownershipType}"/></span></td>
                        <td><c:out value="${o.ownershipPercentage}"/>%</td>
                        <td>${o.isOperational ? 'Yes' : 'No'}</td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/ownership/approve/${o.id}" style="display:inline;">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <button type="submit" class="btn btn-sm btn-success"><i class="bi bi-check-circle"></i> Approve</button>
                            </form>
                            <form method="post" action="${pageContext.request.contextPath}/ownership/reject/${o.id}" style="display:inline;">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <button type="submit" class="btn btn-sm btn-danger"><i class="bi bi-x-circle"></i> Reject</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>
</c:if>

<%@ include file="../layout/footer.jsp" %>
