<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-diagram-3"></i> GL Account Details</h3>
    <div>
        <a href="${pageContext.request.contextPath}/gl/${gl.id}/edit" class="btn btn-outline-secondary"><i class="bi bi-pencil"></i> Edit</a>
        <a href="${pageContext.request.contextPath}/gl" class="btn btn-secondary">Back</a>
    </div>
</div>

<div class="row">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-body">
                <table class="table table-borderless">
                    <tr><td class="text-muted" width="200">GL Code</td><td><code class="fs-5">${gl.glCode}</code></td></tr>
                    <tr><td class="text-muted">Name</td><td><strong>${gl.glName}</strong></td></tr>
                    <tr><td class="text-muted">Description</td><td>${gl.description}</td></tr>
                    <tr><td class="text-muted">Account Type</td><td><span class="badge bg-info">${gl.accountType}</span></td></tr>
                    <tr><td class="text-muted">Level</td><td>${gl.level}</td></tr>
                    <tr><td class="text-muted">Normal Balance</td><td><span class="badge ${gl.normalBalance == 'DEBIT' ? 'bg-danger' : 'bg-success'}">${gl.normalBalance}</span></td></tr>
                    <tr><td class="text-muted">Balance</td><td class="fs-4 fw-bold">${gl.balance}</td></tr>
                    <tr><td class="text-muted">Status</td><td><c:choose><c:when test="${gl.isActive}"><span class="badge bg-success">Active</span></c:when><c:otherwise><span class="badge bg-secondary">Inactive</span></c:otherwise></c:choose></td></tr>
                    <tr><td class="text-muted">Parent</td><td><c:if test="${gl.parent != null}"><code>${gl.parent.glCode}</code> - ${gl.parent.glName}</c:if><c:if test="${gl.parent == null}">Root Account</c:if></td></tr>
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
                            <code>${child.glCode}</code> - ${child.glName}
                        </a>
                        <br><small class="text-muted">${child.accountType} | Balance: ${child.balance}</small>
                    </div>
                </c:forEach>
                <c:if test="${empty children}">
                    <p class="text-muted text-center">No child accounts</p>
                </c:if>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
