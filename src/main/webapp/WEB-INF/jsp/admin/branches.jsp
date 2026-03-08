<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="container-fluid">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2><i class="bi bi-geo-alt"></i> Branch Management</h2>
    </div>

    <div class="card mb-4">
        <div class="card-header bg-primary text-white">
            <h5 class="mb-0"><i class="bi bi-plus-circle"></i> Create New Branch</h5>
        </div>
        <div class="card-body">
            <form method="post" action="${pageContext.request.contextPath}/admin/branches/create" class="row g-3">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="col-md-3">
                    <label for="branchCode" class="form-label">Branch Code</label>
                    <input type="text" class="form-control" id="branchCode" name="branchCode" required placeholder="e.g. BR003">
                </div>
                <div class="col-md-4">
                    <label for="name" class="form-label">Branch Name</label>
                    <input type="text" class="form-control" id="name" name="name" required placeholder="e.g. Midtown Branch">
                </div>
                <div class="col-md-3">
                    <label for="address" class="form-label">Address</label>
                    <input type="text" class="form-control" id="address" name="address" placeholder="e.g. 3rd Avenue">
                </div>
                <div class="col-md-2 d-flex align-items-end">
                    <button type="submit" class="btn btn-primary"><i class="bi bi-plus"></i> Create</button>
                </div>
            </form>
        </div>
    </div>

    <div class="card">
        <div class="card-header"><h5 class="mb-0">All Branches</h5></div>
        <div class="card-body">
            <table class="table table-striped table-hover">
                <thead class="table-dark">
                    <tr>
                        <th>ID</th>
                        <th>Branch Code</th>
                        <th>Name</th>
                        <th>Address</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="branch" items="${branches}">
                        <tr>
                            <td>${branch.id}</td>
                            <td><code>${branch.branchCode}</code></td>
                            <td>${branch.name}</td>
                            <td>${branch.address}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${branch.isActive}"><span class="badge bg-success">Active</span></c:when>
                                    <c:otherwise><span class="badge bg-danger">Inactive</span></c:otherwise>
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
