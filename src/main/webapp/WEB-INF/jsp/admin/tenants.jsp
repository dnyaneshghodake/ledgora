<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="container-fluid">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2><i class="bi bi-building"></i> Tenant Management</h2>
    </div>

    <div class="card mb-4">
        <div class="card-header bg-primary text-white">
            <h5 class="mb-0"><i class="bi bi-plus-circle"></i> Create New Tenant</h5>
        </div>
        <div class="card-body">
            <form method="post" action="${pageContext.request.contextPath}/admin/tenants/create" class="row g-3">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="col-md-4">
                    <label for="tenantCode" class="form-label">Tenant Code</label>
                    <input type="text" class="form-control" id="tenantCode" name="tenantCode" required placeholder="e.g. TENANT-003">
                </div>
                <div class="col-md-4">
                    <label for="tenantName" class="form-label">Tenant Name</label>
                    <input type="text" class="form-control" id="tenantName" name="tenantName" required placeholder="e.g. Partner Bank">
                </div>
                <div class="col-md-4 d-flex align-items-end">
                    <button type="submit" class="btn btn-primary"><i class="bi bi-plus"></i> Create Tenant</button>
                </div>
            </form>
        </div>
    </div>

    <div class="card">
        <div class="card-header"><h5 class="mb-0">All Tenants</h5></div>
        <div class="card-body">
            <table class="table table-striped table-hover">
                <thead class="table-dark">
                    <tr>
                        <th>ID</th>
                        <th>Tenant Code</th>
                        <th>Tenant Name</th>
                        <th>Status</th>
                        <th>Business Date</th>
                        <th>Day Status</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="tenant" items="${tenants}">
                        <tr>
                            <td>${tenant.id}</td>
                            <td><code><c:out value="${tenant.tenantCode}"/></code></td>
                            <td><c:out value="${tenant.tenantName}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${tenant.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                                    <c:otherwise><span class="badge bg-secondary">${tenant.status}</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>${tenant.currentBusinessDate}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${tenant.dayStatus == 'OPEN'}"><span class="badge bg-success">OPEN</span></c:when>
                                    <c:when test="${tenant.dayStatus == 'DAY_CLOSING'}"><span class="badge bg-warning text-dark">DAY_CLOSING</span></c:when>
                                    <c:otherwise><span class="badge bg-danger">${tenant.dayStatus}</span></c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
