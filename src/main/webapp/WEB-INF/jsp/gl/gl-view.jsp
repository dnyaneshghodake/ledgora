<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-diagram-3"></i> GL Account Details</h3>
    <div>
        <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
        <a href="${pageContext.request.contextPath}/gl/${gl.id}/edit" class="btn btn-outline-secondary"><i class="bi bi-pencil"></i> Edit</a>
        </c:if>
        <a href="${pageContext.request.contextPath}/gl" class="btn btn-outline-primary"><i class="bi bi-arrow-left"></i> Back</a>
    </div>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Main Content Section --%>
<div class="row">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-body">
                <table class="table table-borderless">
                    <tr><td class="text-muted" width="200">GL Code</td><td><code class="fs-5"><c:out value="${gl.glCode}"/></code></td></tr>
                    <tr><td class="text-muted">Name</td><td><strong><c:out value="${gl.glName}"/></strong></td></tr>
                    <tr><td class="text-muted">Description</td><td><c:out value="${gl.description}"/></td></tr>
                    <tr><td class="text-muted">Account Type</td><td><span class="badge bg-info"><c:out value="${gl.accountType}"/></span></td></tr>
                    <tr><td class="text-muted">Level</td><td><c:out value="${gl.level}"/></td></tr>
                    <tr><td class="text-muted">Normal Balance</td><td><span class="badge ${gl.normalBalance == 'DEBIT' ? 'bg-danger' : 'bg-success'}"><c:out value="${gl.normalBalance}"/></span></td></tr>
                    <tr><td class="text-muted">Balance</td><td class="fs-4 fw-bold"><c:out value="${gl.balance}"/></td></tr>
                    <tr><td class="text-muted">Status</td><td><c:choose><c:when test="${gl.isActive}"><span class="badge bg-success">Active</span></c:when><c:otherwise><span class="badge bg-secondary">Inactive</span></c:otherwise></c:choose></td></tr>
                    <tr><td class="text-muted">Parent</td><td><c:if test="${gl.parent != null}"><code><c:out value="${gl.parent.glCode}"/></code> - <c:out value="${gl.parent.glName}"/></c:if><c:if test="${gl.parent == null}">Root Account</c:if></td></tr>
                </table>
                <form method="post" action="${pageContext.request.contextPath}/gl/${gl.id}/toggle" class="d-inline">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    <button type="submit" class="btn btn-outline-warning btn-sm">
                        <c:choose><c:when test="${gl.isActive}">Deactivate</c:when><c:otherwise>Activate</c:otherwise></c:choose>
                    </button>
                </form>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0">Child Accounts</h5></div>
            <div class="card-body">
                <c:forEach var="child" items="${children}">
                    <div class="border rounded p-2 mb-2">
                        <a href="${pageContext.request.contextPath}/gl/${child.id}" class="text-decoration-none">
                            <code><c:out value="${child.glCode}"/></code> - <c:out value="${child.glName}"/>
                        </a>
                        <br><small class="text-muted"><c:out value="${child.accountType}"/> | Balance: <c:out value="${child.balance}"/></small>
                    </div>
                </c:forEach>
                <c:if test="${empty children}">
                    <p class="text-muted text-center">No child accounts</p>
                </c:if>
            </div>
        </div>
    </div>
</div>

<%-- Audit Info Section --%>
<c:set var="auditCreatedBy" value="${gl.createdBy}" scope="request"/>
<c:set var="auditCreatedAt" value="${gl.createdAt}" scope="request"/>
<c:set var="auditUpdatedAt" value="${gl.updatedAt}" scope="request"/>
<c:set var="auditEntityType" value="GL Account" scope="request"/>
<c:set var="auditEntityId" value="${gl.glCode}" scope="request"/>
<%@ include file="../layout/audit-info.jsp" %>

<%@ include file="../layout/footer.jsp" %>
