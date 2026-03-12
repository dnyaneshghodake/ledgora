<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-people"></i> Customer Master</h3>
    <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
    <a href="${pageContext.request.contextPath}/customers/create" class="btn btn-primary"><i class="bi bi-person-plus"></i> Add Customer</a>
    </c:if>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<%-- Search / Filter Section --%>
<div class="card shadow mb-4">
    <div class="card-header bg-light"><h6 class="mb-0"><i class="bi bi-funnel"></i> Search &amp; Filter</h6></div>
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/customers" class="row g-2">
            <div class="col-md-3">
                <input type="text" name="search" class="form-control" placeholder="Search by Name or ID" value="<c:out value="${param.search}"/>"/>
            </div>
            <div class="col-md-2">
                <select name="kycStatus" class="form-select">
                    <option value="">All KYC Status</option>
                    <option value="PENDING" ${param.kycStatus == 'PENDING' ? 'selected' : ''}>PENDING</option>
                    <option value="VERIFIED" ${param.kycStatus == 'VERIFIED' ? 'selected' : ''}>VERIFIED</option>
                    <option value="REJECTED" ${param.kycStatus == 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                </select>
            </div>
            <div class="col-md-2">
                <select name="approvalStatus" class="form-select">
                    <option value="">All Approval</option>
                    <option value="PENDING" ${param.approvalStatus == 'PENDING' ? 'selected' : ''}>PENDING</option>
                    <option value="APPROVED" ${param.approvalStatus == 'APPROVED' ? 'selected' : ''}>APPROVED</option>
                    <option value="REJECTED" ${param.approvalStatus == 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                </select>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-outline-primary w-100"><i class="bi bi-search"></i> Search</button>
            </div>
            <div class="col-md-2">
                <a href="${pageContext.request.contextPath}/customers" class="btn btn-outline-secondary w-100">Reset</a>
            </div>
        </form>
    </div>
</div>

<div class="card shadow">
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty customers}">
                <%@ include file="../layout/record-count.jsp" %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle">
                        <thead class="table-light">
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>National ID</th>
                                <th>Mobile</th>
                                <th>KYC Status</th>
                                <th>Approval</th>
                                <th>Created</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="c" items="${customers}">
                            <tr>
                                <td><c:out value="${c.customerId}"/></td>
                                <td><c:out value="${c.firstName}"/> <c:out value="${c.lastName}"/></td>
                                <td><code><c:out value="${c.nationalId}"/></code></td>
                                <td><c:out value="${c.phone}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${c.kycStatus == 'VERIFIED'}"><span class="badge bg-success">VERIFIED</span></c:when>
                                        <c:when test="${c.kycStatus == 'PENDING'}"><span class="badge bg-warning text-dark">PENDING</span></c:when>
                                        <c:when test="${c.kycStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary"><c:out value="${c.kycStatus}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${c.approvalStatus == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                                        <c:when test="${c.approvalStatus == 'PENDING'}"><span class="badge bg-warning text-dark">PENDING</span></c:when>
                                        <c:when test="${c.approvalStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary"><c:out value="${c.approvalStatus}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><small><c:out value="${c.createdAt}"/></small></td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/customers/${c.customerId}" class="btn btn-sm btn-outline-primary" title="View"><i class="bi bi-eye"></i></a>
                                    <a href="${pageContext.request.contextPath}/customers/${c.customerId}/master" class="btn btn-sm btn-outline-info" title="Customer Master"><i class="bi bi-person-badge"></i></a>
                                    <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
                                    <a href="${pageContext.request.contextPath}/customers/${c.customerId}/edit" class="btn btn-sm btn-outline-secondary" title="Edit"><i class="bi bi-pencil"></i></a>
                                    </c:if>
                                </td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <%-- Pagination --%>
                <%@ include file="../layout/pagination.jsp" %>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-people" style="font-size: 3rem;"></i>
                    <p class="mt-2">No customers found.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
