<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-lock"></i> Lien Management</h3>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty success}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${success}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<%-- Create Lien Form --%>
<div class="card shadow mb-4">
    <div class="card-header"><h5 class="mb-0"><i class="bi bi-plus-circle"></i> Create Lien</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/liens/create">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <div class="row g-3">
                <div class="col-md-2">
                    <label class="form-label">Account ID</label>
                    <input type="number" name="accountId" class="form-control" required/>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Lien Amount</label>
                    <input type="number" name="lienAmount" class="form-control" step="0.01" min="0.01" required/>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Lien Type</label>
                    <select name="lienType" class="form-select" required>
                        <c:forEach var="t" items="${lienTypes}">
                            <option value="${t}"><c:out value="${t}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Start Date</label>
                    <input type="date" name="startDate" class="form-control" required/>
                </div>
                <div class="col-md-2">
                    <label class="form-label">End Date</label>
                    <input type="date" name="endDate" class="form-control"/>
                </div>
                <div class="col-md-2 d-flex align-items-end">
                    <button type="submit" class="btn btn-primary w-100"><i class="bi bi-send"></i> Submit</button>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Reference</label>
                    <input type="text" name="lienReference" class="form-control" maxlength="50"/>
                </div>
                <div class="col-md-8">
                    <label class="form-label">Remarks</label>
                    <input type="text" name="remarks" class="form-control" maxlength="500"/>
                </div>
            </div>
        </form>
    </div>
</div>

<%-- Pending Lien Approvals --%>
<c:if test="${not empty pendingLiens}">
<div class="card shadow mb-4">
    <div class="card-header bg-warning text-dark"><h5 class="mb-0"><i class="bi bi-hourglass-split"></i> Pending Lien Approvals</h5></div>
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>Account</th><th>Amount</th><th>Type</th><th>Start</th><th>End</th><th>Status</th><th>Actions</th></tr>
            </thead>
            <tbody>
                <c:forEach var="l" items="${pendingLiens}">
                    <tr>
                        <td><c:out value="${l.account.accountNumber}"/></td>
                        <td class="fw-bold"><c:out value="${l.lienAmount}"/></td>
                        <td><span class="badge bg-info"><c:out value="${l.lienType}"/></span></td>
                        <td><c:out value="${l.startDate}"/></td>
                        <td><c:out value="${l.endDate}"/></td>
                        <td><span class="badge bg-warning"><c:out value="${l.status}"/></span></td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/liens/approve/${l.id}" style="display:inline;">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <button type="submit" class="btn btn-sm btn-success"><i class="bi bi-check-circle"></i> Approve</button>
                            </form>
                            <form method="post" action="${pageContext.request.contextPath}/liens/release/${l.id}" style="display:inline;">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <button type="submit" class="btn btn-sm btn-outline-danger"><i class="bi bi-unlock"></i> Release</button>
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
