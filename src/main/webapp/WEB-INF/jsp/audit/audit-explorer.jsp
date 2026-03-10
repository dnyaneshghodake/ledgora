<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-clock-history"></i> Enterprise Audit Log Explorer</h3>
    <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Dashboard</a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Governance Banner --%>
<div class="alert alert-dark border-0 mb-4">
    <div class="d-flex align-items-center">
        <i class="bi bi-shield-lock-fill fs-4 me-3"></i>
        <div>
            <strong>RBI CBS Governance:</strong> All financial and governance events are audit logged and immutable.
            Audit records cannot be modified or deleted. This explorer is read-only.
        </div>
    </div>
</div>

<%-- Section A: Filter Panel --%>
<div class="card shadow mb-4">
    <div class="card-header bg-light"><h6 class="mb-0"><i class="bi bi-funnel"></i> Search &amp; Filter</h6></div>
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/audit/explorer" class="row g-2">
            <div class="col-md-2">
                <label class="form-label">Date From</label>
                <input type="date" name="dateFrom" class="form-control" value="<c:out value='${filterDateFrom}'/>"/>
            </div>
            <div class="col-md-2">
                <label class="form-label">Date To</label>
                <input type="date" name="dateTo" class="form-control" value="<c:out value='${filterDateTo}'/>"/>
            </div>
            <div class="col-md-2">
                <label class="form-label">Action</label>
                <input type="text" name="action" class="form-control" placeholder="e.g. VOUCHER_POSTED" value="<c:out value='${filterAction}'/>"/>
            </div>
            <div class="col-md-2">
                <label class="form-label">Username</label>
                <input type="text" name="username" class="form-control" placeholder="e.g. teller1" value="<c:out value='${filterUsername}'/>"/>
            </div>
            <div class="col-md-2">
                <label class="form-label">Entity Type</label>
                <input type="text" name="entityType" class="form-control" placeholder="e.g. VOUCHER" value="<c:out value='${filterEntityType}'/>"/>
            </div>
            <div class="col-md-2">
                <label class="form-label">Entity ID</label>
                <input type="number" name="entityId" class="form-control" placeholder="e.g. 42" value="<c:out value='${filterEntityId}'/>"/>
            </div>
            <div class="col-12 mt-2">
                <button type="submit" class="btn btn-primary"><i class="bi bi-search"></i> Search</button>
                <a href="${pageContext.request.contextPath}/audit/explorer" class="btn btn-outline-secondary ms-2"><i class="bi bi-x-circle"></i> Clear Filters</a>
                <small class="text-muted ms-3"><c:out value="${totalElements}"/> result(s) found</small>
            </div>
        </form>
    </div>
</div>

<%-- Section B: Audit Table --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0"><i class="bi bi-journal-text"></i> Audit Events</h5>
        <span class="badge bg-secondary"><c:out value="${totalElements}"/> total</span>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty auditLogs}">
                <div class="table-responsive">
                    <table class="table table-hover table-sm">
                        <thead class="table-light">
                            <tr>
                                <th>Timestamp</th>
                                <th>Username</th>
                                <th>Action</th>
                                <th>Entity</th>
                                <th>Entity ID</th>
                                <th>Old Value</th>
                                <th>New Value</th>
                                <th>IP Address</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="log" items="${auditLogs}">
                            <tr>
                                <td><small><c:out value="${log.timestamp}"/></small></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty log.username}"><c:out value="${log.username}"/></c:when>
                                        <c:when test="${log.userId != null}">UID:<c:out value="${log.userId}"/></c:when>
                                        <c:otherwise><span class="text-muted">System</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><span class="badge bg-secondary"><c:out value="${log.action}"/></span></td>
                                <td><c:out value="${log.entity}"/></td>
                                <td><code><c:out value="${log.entityId}"/></code></td>
                                <td>
                                    <c:if test="${not empty log.oldValue}">
                                        <small class="text-break" title="<c:out value='${log.oldValue}'/>">
                                            <c:choose>
                                                <c:when test="${log.oldValue.length() > 60}"><c:out value="${log.oldValue.substring(0, 60)}"/>...</c:when>
                                                <c:otherwise><c:out value="${log.oldValue}"/></c:otherwise>
                                            </c:choose>
                                        </small>
                                    </c:if>
                                </td>
                                <td>
                                    <c:if test="${not empty log.newValue}">
                                        <small class="text-break" title="<c:out value='${log.newValue}'/>">
                                            <c:choose>
                                                <c:when test="${log.newValue.length() > 60}"><c:out value="${log.newValue.substring(0, 60)}"/>...</c:when>
                                                <c:otherwise><c:out value="${log.newValue}"/></c:otherwise>
                                            </c:choose>
                                        </small>
                                    </c:if>
                                </td>
                                <td><small class="text-muted"><c:out value="${log.ipAddress}"/></small></td>
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
                            <a class="page-link" href="${pageContext.request.contextPath}/audit/explorer?page=${currentPage - 1}&size=${pageSize}&dateFrom=${filterDateFrom}&dateTo=${filterDateTo}&action=${filterAction}&username=${filterUsername}&entityType=${filterEntityType}&entityId=${filterEntityId}">Previous</a>
                        </li>
                        <c:forEach begin="0" end="${totalPages - 1}" var="i">
                            <c:if test="${i == 0 || i == totalPages - 1 || (i >= currentPage - 2 && i <= currentPage + 2)}">
                                <li class="page-item ${i == currentPage ? 'active' : ''}">
                                    <a class="page-link" href="${pageContext.request.contextPath}/audit/explorer?page=${i}&size=${pageSize}&dateFrom=${filterDateFrom}&dateTo=${filterDateTo}&action=${filterAction}&username=${filterUsername}&entityType=${filterEntityType}&entityId=${filterEntityId}"><c:out value="${i + 1}"/></a>
                                </li>
                            </c:if>
                        </c:forEach>
                        <li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
                            <a class="page-link" href="${pageContext.request.contextPath}/audit/explorer?page=${currentPage + 1}&size=${pageSize}&dateFrom=${filterDateFrom}&dateTo=${filterDateTo}&action=${filterAction}&username=${filterUsername}&entityType=${filterEntityType}&entityId=${filterEntityId}">Next</a>
                        </li>
                    </ul>
                </nav>
                </c:if>

                <div class="text-muted text-center mt-2">
                    <small>Page <c:out value="${currentPage + 1}"/> of <c:out value="${totalPages}"/> &mdash; <c:out value="${totalElements}"/> total events</small>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-journal-x" style="font-size: 3rem;"></i>
                    <p class="mt-2">No audit events found matching the current filters.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- Governance Footer --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    All audit records are immutable. System of Record: audit_logs table.
    Tenant isolation enforced on all queries. This explorer is read-only.
</div>

<%@ include file="../layout/footer.jsp" %>
