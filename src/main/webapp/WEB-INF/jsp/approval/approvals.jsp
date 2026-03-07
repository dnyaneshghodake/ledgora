<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-clipboard-check"></i> Approval Requests
        <c:if test="${pendingCount > 0}">
            <span class="badge bg-warning">${pendingCount} pending</span>
        </c:if>
    </h3>
    <a href="${pageContext.request.contextPath}/approvals/pending" class="btn btn-warning">
        <i class="bi bi-hourglass-split"></i> View Pending
    </a>
</div>

<div class="card shadow">
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr>
                    <th>ID</th>
                    <th>Entity Type</th>
                    <th>Entity ID</th>
                    <th>Requested By</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="approval" items="${approvals}">
                    <tr>
                        <td>${approval.id}</td>
                        <td><span class="badge bg-info">${approval.entityType}</span></td>
                        <td>${approval.entityId}</td>
                        <td>${approval.requestedBy != null ? approval.requestedBy.fullName : 'System'}</td>
                        <td>
                            <c:choose>
                                <c:when test="${approval.status == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                                <c:when test="${approval.status == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                                <c:when test="${approval.status == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                            </c:choose>
                        </td>
                        <td>${approval.createdAt}</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/approvals/${approval.id}" class="btn btn-sm btn-outline-primary">
                                <i class="bi bi-eye"></i>
                            </a>
                            <c:if test="${approval.status == 'PENDING'}">
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${approval.id}/approve" style="display:inline;">
                                    <button type="submit" class="btn btn-sm btn-success"><i class="bi bi-check"></i></button>
                                </form>
                                <form method="post" action="${pageContext.request.contextPath}/approvals/${approval.id}/reject" style="display:inline;">
                                    <button type="submit" class="btn btn-sm btn-danger"><i class="bi bi-x"></i></button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty approvals}">
                    <tr><td colspan="7" class="text-center text-muted py-4">No approval requests found</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
