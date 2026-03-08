<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="../layout/header.jsp" %>

<div class="container-fluid">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2><i class="bi bi-shield-check"></i> Audit Logs</h2>
    </div>

    <div class="card mb-4">
        <div class="card-header">
            <h5 class="mb-0">Filter</h5>
        </div>
        <div class="card-body">
            <form method="get" action="${pageContext.request.contextPath}/admin/audit" class="row g-3">
                <div class="col-md-4">
                    <label for="entity" class="form-label">Entity Type</label>
                    <select class="form-select" id="entity" name="entity">
                        <option value="">All</option>
                        <option value="TRANSACTION" ${filterEntity == 'TRANSACTION' ? 'selected' : ''}>Transaction</option>
                        <option value="ACCOUNT" ${filterEntity == 'ACCOUNT' ? 'selected' : ''}>Account</option>
                        <option value="VOUCHER" ${filterEntity == 'VOUCHER' ? 'selected' : ''}>Voucher</option>
                        <option value="USER" ${filterEntity == 'USER' ? 'selected' : ''}>User</option>
                        <option value="SETTLEMENT" ${filterEntity == 'SETTLEMENT' ? 'selected' : ''}>Settlement</option>
                        <option value="APPROVAL" ${filterEntity == 'APPROVAL' ? 'selected' : ''}>Approval</option>
                    </select>
                </div>
                <div class="col-md-2 d-flex align-items-end">
                    <button type="submit" class="btn btn-primary"><i class="bi bi-filter"></i> Filter</button>
                </div>
            </form>
        </div>
    </div>

    <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
            <h5 class="mb-0">Audit Trail <span class="badge bg-secondary">${totalEntries} total</span></h5>
            <small class="text-muted">Page ${currentPage + 1} of ${totalPages > 0 ? totalPages : 1}</small>
        </div>
        <div class="card-body">
            <div class="table-responsive">
                <table class="table table-striped table-hover table-sm">
                    <thead class="table-dark">
                        <tr>
                            <th>ID</th>
                            <th>Timestamp</th>
                            <th>Action</th>
                            <th>Entity</th>
                            <th>Entity ID</th>
                            <th>User ID</th>
                            <th>Details</th>
                            <th>IP Address</th>
                            <th>User Agent</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="log" items="${auditLogs}">
                            <tr>
                                <td>${log.id}</td>
                                <td><small>${log.timestamp}</small></td>
                                <td><span class="badge bg-info"><c:out value="${log.action}"/></span></td>
                                <td><code><c:out value="${log.entity}"/></code></td>
                                <td><c:out value="${log.entityId}"/></td>
                                <td><c:out value="${log.userId}"/></td>
                                <td><small><c:out value="${log.details}"/></small></td>
                                <td><small><c:out value="${log.ipAddress}"/></small></td>
                                <td><small title="${fn:escapeXml(log.userAgent)}"><c:out value="${log.userAgent}"/></small></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <%-- Pagination Controls --%>
            <c:if test="${totalPages > 1}">
            <nav aria-label="Audit log pagination" class="mt-3">
                <ul class="pagination pagination-sm justify-content-center mb-0">
                    <li class="page-item ${currentPage == 0 ? 'disabled' : ''}">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/audit?page=${currentPage - 1}<c:if test='${not empty filterEntity}'>&entity=${filterEntity}</c:if>">&laquo; Prev</a>
                    </li>
                    <c:forEach begin="0" end="${totalPages - 1}" var="i">
                        <c:if test="${i == 0 || i == totalPages - 1 || (i >= currentPage - 2 && i <= currentPage + 2)}">
                        <li class="page-item ${i == currentPage ? 'active' : ''}">
                            <a class="page-link" href="${pageContext.request.contextPath}/admin/audit?page=${i}<c:if test='${not empty filterEntity}'>&entity=${filterEntity}</c:if>">${i + 1}</a>
                        </li>
                        </c:if>
                    </c:forEach>
                    <li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/audit?page=${currentPage + 1}<c:if test='${not empty filterEntity}'>&entity=${filterEntity}</c:if>">Next &raquo;</a>
                    </li>
                </ul>
            </nav>
            </c:if>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
