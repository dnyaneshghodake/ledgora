<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-clipboard-check"></i> Approval Request #${approval.id}</h3>
    <a href="${pageContext.request.contextPath}/approvals" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left"></i> Back
    </a>
</div>

<div class="card shadow">
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-6"><strong>Entity Type:</strong> ${approval.entityType}</div>
            <div class="col-md-6"><strong>Entity ID:</strong> ${approval.entityId}</div>
            <div class="col-md-6"><strong>Requested By:</strong> ${approval.requestedBy != null ? approval.requestedBy.fullName : 'System'}</div>
            <div class="col-md-6">
                <strong>Status:</strong>
                <c:choose>
                    <c:when test="${approval.status == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                    <c:when test="${approval.status == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                    <c:when test="${approval.status == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                </c:choose>
            </div>
            <div class="col-md-6"><strong>Created:</strong> ${approval.createdAt}</div>
            <div class="col-md-6"><strong>Approved/Rejected At:</strong> ${approval.approvedAt}</div>
            <div class="col-md-6"><strong>Approved By:</strong> ${approval.approvedBy != null ? approval.approvedBy.fullName : '-'}</div>
            <div class="col-12"><strong>Request Data:</strong> <pre class="mt-1">${approval.requestData}</pre></div>
            <div class="col-12"><strong>Remarks:</strong> ${approval.remarks}</div>
        </div>

        <c:if test="${approval.status == 'PENDING'}">
            <hr>
            <div class="row g-3">
                <div class="col-md-6">
                    <form method="post" action="${pageContext.request.contextPath}/approvals/${approval.id}/approve">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        <div class="mb-2">
                            <input type="text" name="remarks" class="form-control" placeholder="Approval remarks..." />
                        </div>
                        <button type="submit" class="btn btn-success"><i class="bi bi-check-circle"></i> Approve</button>
                    </form>
                </div>
                <div class="col-md-6">
                    <form method="post" action="${pageContext.request.contextPath}/approvals/${approval.id}/reject">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        <div class="mb-2">
                            <input type="text" name="remarks" class="form-control" placeholder="Rejection reason..." />
                        </div>
                        <button type="submit" class="btn btn-danger"><i class="bi bi-x-circle"></i> Reject</button>
                    </form>
                </div>
            </div>
        </c:if>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
