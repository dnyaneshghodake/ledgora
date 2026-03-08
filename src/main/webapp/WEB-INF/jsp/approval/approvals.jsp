<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-clipboard-check"></i> Unified Approval Queue
        <c:if test="${pendingCount > 0}">
            <span class="badge bg-warning">${pendingCount} pending</span>
        </c:if>
    </h3>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<%-- Summary Cards Row --%>
<div class="row mb-4">
    <div class="col-md-2">
        <div class="card border-start border-4 border-primary shadow-sm">
            <div class="card-body py-2">
                <small class="text-muted">Total Pending</small>
                <h4 class="mb-0">${pendingCount}</h4>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card border-start border-4 border-info shadow-sm">
            <div class="card-body py-2">
                <small class="text-muted">Customers</small>
                <h4 class="mb-0"><c:out value="${pendingCustomers.size()}" default="0"/></h4>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card border-start border-4 border-success shadow-sm">
            <div class="card-body py-2">
                <small class="text-muted">Accounts</small>
                <h4 class="mb-0"><c:out value="${pendingAccounts.size()}" default="0"/></h4>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card border-start border-4 border-warning shadow-sm">
            <div class="card-body py-2">
                <small class="text-muted">Liens</small>
                <h4 class="mb-0"><c:out value="${pendingLiens.size()}" default="0"/></h4>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card border-start border-4 border-secondary shadow-sm">
            <div class="card-body py-2">
                <small class="text-muted">Calendar</small>
                <h4 class="mb-0"><c:out value="${pendingCalendar.size()}" default="0"/></h4>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card border-start border-4 border-danger shadow-sm">
            <div class="card-body py-2">
                <small class="text-muted">Transactions</small>
                <h4 class="mb-0"><c:out value="${pendingTransactions.size()}" default="0"/></h4>
            </div>
        </div>
    </div>
</div>

<%-- Category Tabs --%>
<ul class="nav nav-tabs mb-3" role="tablist">
    <li class="nav-item"><a class="nav-link ${empty param.type || param.type == 'ALL' ? 'active' : ''}" data-bs-toggle="tab" href="#tab-all">All Pending</a></li>
    <li class="nav-item"><a class="nav-link ${param.type == 'CUSTOMER' ? 'active' : ''}" data-bs-toggle="tab" href="#tab-customers">Customers <c:if test="${not empty pendingCustomers}"><span class="badge bg-info">${pendingCustomers.size()}</span></c:if></a></li>
    <li class="nav-item"><a class="nav-link ${param.type == 'ACCOUNT' ? 'active' : ''}" data-bs-toggle="tab" href="#tab-accounts">Accounts <c:if test="${not empty pendingAccounts}"><span class="badge bg-success">${pendingAccounts.size()}</span></c:if></a></li>
    <li class="nav-item"><a class="nav-link ${param.type == 'LIEN' ? 'active' : ''}" data-bs-toggle="tab" href="#tab-liens">Liens <c:if test="${not empty pendingLiens}"><span class="badge bg-warning">${pendingLiens.size()}</span></c:if></a></li>
    <li class="nav-item"><a class="nav-link ${param.type == 'CALENDAR' ? 'active' : ''}" data-bs-toggle="tab" href="#tab-calendar">Calendar <c:if test="${not empty pendingCalendar}"><span class="badge bg-secondary">${pendingCalendar.size()}</span></c:if></a></li>
    <li class="nav-item"><a class="nav-link ${param.type == 'TRANSACTION' ? 'active' : ''}" data-bs-toggle="tab" href="#tab-transactions">High-Value Txns <c:if test="${not empty pendingTransactions}"><span class="badge bg-danger">${pendingTransactions.size()}</span></c:if></a></li>
</ul>

<div class="tab-content">

<%-- ALL Pending Tab --%>
<div class="tab-pane fade ${empty param.type || param.type == 'ALL' ? 'show active' : ''}" id="tab-all">
    <div class="card shadow">
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light">
                    <tr><th>ID</th><th>Entity Type</th><th>Entity ID</th><th>Requested By</th><th>Status</th><th>Created</th><th>Actions</th></tr>
                </thead>
                <tbody>
                    <c:forEach var="approval" items="${approvals}">
                    <tr>
                        <td>${approval.id}</td>
                        <td><span class="badge bg-info">${approval.entityType}</span></td>
                        <td>${approval.entityId}</td>
                        <td><c:out value="${approval.requestedBy != null ? approval.requestedBy.fullName : 'System'}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${approval.status == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                                <c:when test="${approval.status == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                                <c:when test="${approval.status == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                            </c:choose>
                        </td>
                        <td><small>${approval.createdAt}</small></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/approvals/${approval.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i> View</a>
                            <c:if test="${approval.status == 'PENDING'}">
                                <%-- Maker cannot approve own records --%>
                                <c:if test="${approval.requestedBy == null || approval.requestedBy.id != sessionScope.userId}">
                                    <form method="post" action="${pageContext.request.contextPath}/approvals/${approval.id}/approve" style="display:inline;">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                        <button type="submit" class="btn btn-sm btn-success" onclick="return confirm('Approve this request?')"><i class="bi bi-check-lg"></i> Approve</button>
                                    </form>
                                    <form method="post" action="${pageContext.request.contextPath}/approvals/${approval.id}/reject" style="display:inline;">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                        <button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Reject this request?')"><i class="bi bi-x-lg"></i> Reject</button>
                                    </form>
                                </c:if>
                                <c:if test="${approval.requestedBy != null && approval.requestedBy.id == sessionScope.userId}">
                                    <span class="badge bg-light text-dark border" title="Maker cannot approve own records"><i class="bi bi-lock"></i> Own Request</span>
                                </c:if>
                            </c:if>
                        </td>
                    </tr>
                    </c:forEach>
                    <c:if test="${empty approvals}">
                        <tr><td colspan="7" class="text-center text-muted py-4"><i class="bi bi-clipboard-check" style="font-size: 2rem;"></i><br>No approval requests found</td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%-- Pending Customers Tab --%>
<div class="tab-pane fade ${param.type == 'CUSTOMER' ? 'show active' : ''}" id="tab-customers">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-people"></i> Pending Customer Approvals</h5></div>
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light"><tr><th>ID</th><th>Entity ID</th><th>Requested By</th><th>Created</th><th>Actions</th></tr></thead>
                <tbody>
                    <c:forEach var="a" items="${pendingCustomers}">
                    <tr>
                        <td>${a.id}</td>
                        <td><a href="${pageContext.request.contextPath}/customers/${a.entityId}">${a.entityId}</a></td>
                        <td><c:out value="${a.requestedBy != null ? a.requestedBy.fullName : 'System'}"/></td>
                        <td><small>${a.createdAt}</small></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/approvals/${a.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a>
                            <c:if test="${a.requestedBy == null || a.requestedBy.id != sessionScope.userId}">
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${a.id}/approve" style="display:inline;"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="btn btn-sm btn-success" onclick="return confirm('Approve?')"><i class="bi bi-check-lg"></i></button></form>
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${a.id}/reject" style="display:inline;"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Reject?')"><i class="bi bi-x-lg"></i></button></form>
                            </c:if>
                            <c:if test="${a.requestedBy != null && a.requestedBy.id == sessionScope.userId}"><span class="badge bg-light text-dark border"><i class="bi bi-lock"></i> Own</span></c:if>
                        </td>
                    </tr>
                    </c:forEach>
                    <c:if test="${empty pendingCustomers}"><tr><td colspan="5" class="text-center text-muted py-3">No pending customer approvals</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%-- Pending Accounts Tab --%>
<div class="tab-pane fade ${param.type == 'ACCOUNT' ? 'show active' : ''}" id="tab-accounts">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-wallet2"></i> Pending Account Approvals</h5></div>
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light"><tr><th>ID</th><th>Entity ID</th><th>Requested By</th><th>Created</th><th>Actions</th></tr></thead>
                <tbody>
                    <c:forEach var="a" items="${pendingAccounts}">
                    <tr>
                        <td>${a.id}</td>
                        <td><a href="${pageContext.request.contextPath}/accounts/${a.entityId}">${a.entityId}</a></td>
                        <td><c:out value="${a.requestedBy != null ? a.requestedBy.fullName : 'System'}"/></td>
                        <td><small>${a.createdAt}</small></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/approvals/${a.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a>
                            <c:if test="${a.requestedBy == null || a.requestedBy.id != sessionScope.userId}">
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${a.id}/approve" style="display:inline;"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="btn btn-sm btn-success" onclick="return confirm('Approve?')"><i class="bi bi-check-lg"></i></button></form>
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${a.id}/reject" style="display:inline;"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Reject?')"><i class="bi bi-x-lg"></i></button></form>
                            </c:if>
                            <c:if test="${a.requestedBy != null && a.requestedBy.id == sessionScope.userId}"><span class="badge bg-light text-dark border"><i class="bi bi-lock"></i> Own</span></c:if>
                        </td>
                    </tr>
                    </c:forEach>
                    <c:if test="${empty pendingAccounts}"><tr><td colspan="5" class="text-center text-muted py-3">No pending account approvals</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%-- Pending Liens Tab --%>
<div class="tab-pane fade ${param.type == 'LIEN' ? 'show active' : ''}" id="tab-liens">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-lock"></i> Pending Lien Approvals</h5></div>
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light"><tr><th>ID</th><th>Entity ID</th><th>Requested By</th><th>Created</th><th>Actions</th></tr></thead>
                <tbody>
                    <c:forEach var="a" items="${pendingLiens}">
                    <tr>
                        <td>${a.id}</td><td>${a.entityId}</td>
                        <td><c:out value="${a.requestedBy != null ? a.requestedBy.fullName : 'System'}"/></td>
                        <td><small>${a.createdAt}</small></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/approvals/${a.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a>
                            <c:if test="${a.requestedBy == null || a.requestedBy.id != sessionScope.userId}">
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${a.id}/approve" style="display:inline;"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="btn btn-sm btn-success" onclick="return confirm('Approve?')"><i class="bi bi-check-lg"></i></button></form>
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${a.id}/reject" style="display:inline;"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Reject?')"><i class="bi bi-x-lg"></i></button></form>
                            </c:if>
                            <c:if test="${a.requestedBy != null && a.requestedBy.id == sessionScope.userId}"><span class="badge bg-light text-dark border"><i class="bi bi-lock"></i> Own</span></c:if>
                        </td>
                    </tr>
                    </c:forEach>
                    <c:if test="${empty pendingLiens}"><tr><td colspan="5" class="text-center text-muted py-3">No pending lien approvals</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%-- Pending Calendar Tab --%>
<div class="tab-pane fade ${param.type == 'CALENDAR' ? 'show active' : ''}" id="tab-calendar">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-calendar3"></i> Pending Calendar Approvals</h5></div>
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light"><tr><th>ID</th><th>Entity ID</th><th>Requested By</th><th>Created</th><th>Actions</th></tr></thead>
                <tbody>
                    <c:forEach var="a" items="${pendingCalendar}">
                    <tr>
                        <td>${a.id}</td><td>${a.entityId}</td>
                        <td><c:out value="${a.requestedBy != null ? a.requestedBy.fullName : 'System'}"/></td>
                        <td><small>${a.createdAt}</small></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/approvals/${a.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a>
                            <c:if test="${a.requestedBy == null || a.requestedBy.id != sessionScope.userId}">
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${a.id}/approve" style="display:inline;"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="btn btn-sm btn-success" onclick="return confirm('Approve?')"><i class="bi bi-check-lg"></i></button></form>
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${a.id}/reject" style="display:inline;"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Reject?')"><i class="bi bi-x-lg"></i></button></form>
                            </c:if>
                            <c:if test="${a.requestedBy != null && a.requestedBy.id == sessionScope.userId}"><span class="badge bg-light text-dark border"><i class="bi bi-lock"></i> Own</span></c:if>
                        </td>
                    </tr>
                    </c:forEach>
                    <c:if test="${empty pendingCalendar}"><tr><td colspan="5" class="text-center text-muted py-3">No pending calendar approvals</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%-- Pending High-Value Transactions Tab --%>
<div class="tab-pane fade ${param.type == 'TRANSACTION' ? 'show active' : ''}" id="tab-transactions">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-shield-exclamation"></i> Pending High-Value Transaction Approvals</h5></div>
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light"><tr><th>ID</th><th>Entity ID</th><th>Requested By</th><th>Details</th><th>Created</th><th>Actions</th></tr></thead>
                <tbody>
                    <c:forEach var="a" items="${pendingTransactions}">
                    <tr>
                        <td>${a.id}</td><td>${a.entityId}</td>
                        <td><c:out value="${a.requestedBy != null ? a.requestedBy.fullName : 'System'}"/></td>
                        <td><small class="text-muted"><c:out value="${a.requestData}" default="--"/></small></td>
                        <td><small>${a.createdAt}</small></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/approvals/${a.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a>
                            <c:if test="${a.requestedBy == null || a.requestedBy.id != sessionScope.userId}">
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${a.id}/approve" style="display:inline;"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="btn btn-sm btn-success" onclick="return confirm('Approve high-value txn?')"><i class="bi bi-check-lg"></i></button></form>
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${a.id}/reject" style="display:inline;"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Reject?')"><i class="bi bi-x-lg"></i></button></form>
                            </c:if>
                            <c:if test="${a.requestedBy != null && a.requestedBy.id == sessionScope.userId}"><span class="badge bg-light text-dark border"><i class="bi bi-lock"></i> Own</span></c:if>
                        </td>
                    </tr>
                    </c:forEach>
                    <c:if test="${empty pendingTransactions}"><tr><td colspan="6" class="text-center text-muted py-3">No pending high-value transaction approvals</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

</div>

<%-- Governance Note --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    Maker-Checker enforcement: A maker cannot approve their own records. All approval actions are logged in the audit trail.
</div>

<%@ include file="../layout/footer.jsp" %>
