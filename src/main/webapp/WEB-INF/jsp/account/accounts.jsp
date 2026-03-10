<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-wallet2"></i> Account Master</h3>
    <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
    <a href="${pageContext.request.contextPath}/accounts/create" class="btn btn-primary"><i class="bi bi-plus-circle"></i> Open Account</a>
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
        <form method="get" action="${pageContext.request.contextPath}/accounts" class="row g-2">
            <div class="col-md-3">
                <input type="text" name="search" class="form-control" placeholder="Search by Account No or Name" value="<c:out value="${param.search}"/>"/>
            </div>
            <div class="col-md-2">
                <select name="status" class="form-select">
                    <option value="">All Status</option>
                    <option value="ACTIVE" ${param.status == 'ACTIVE' ? 'selected' : ''}>ACTIVE</option>
                    <option value="INACTIVE" ${param.status == 'INACTIVE' ? 'selected' : ''}>INACTIVE</option>
                    <option value="FROZEN" ${param.status == 'FROZEN' ? 'selected' : ''}>FROZEN</option>
                    <option value="CLOSED" ${param.status == 'CLOSED' ? 'selected' : ''}>CLOSED</option>
                </select>
            </div>
            <div class="col-md-2">
                <select name="accountType" class="form-select">
                    <option value="">All Types</option>
                    <c:forEach var="at" items="${accountTypes}">
                        <option value="${at}" ${param.accountType == at ? 'selected' : ''}><c:out value="${at}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-outline-primary w-100"><i class="bi bi-search"></i> Search</button>
            </div>
            <div class="col-md-2">
                <a href="${pageContext.request.contextPath}/accounts" class="btn btn-outline-secondary w-100">Reset</a>
            </div>
        </form>
    </div>
</div>

<div class="card shadow">
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty accounts}">
                <%@ include file="../layout/record-count.jsp" %>
                <div class="table-responsive">
                    <table class="table table-hover align-middle">
                        <thead class="table-light">
                            <tr>
                                <th>Account Number</th>
                                <th>Account Name</th>
                                <th>Type</th>
                                <th>Customer</th>
                                <th>Status</th>
                                <th>Freeze</th>
                                <th>Balance</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="a" items="${accounts}">
                            <tr>
                                <td><code><c:out value="${a.accountNumber}"/></code></td>
                                <td><c:out value="${a.accountName}"/></td>
                                <td><span class="badge bg-info"><c:out value="${a.accountType}"/></span></td>
                                <td><c:out value="${a.customerName}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                                        <c:when test="${a.status == 'FROZEN'}"><span class="badge bg-danger">FROZEN</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary"><c:out value="${a.status}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.freezeLevel != null && a.freezeLevel != 'NONE'}"><span class="badge bg-danger"><c:out value="${a.freezeLevel}"/></span></c:when>
                                        <c:otherwise><span class="badge bg-success">NONE</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="fw-bold"><c:out value="${a.balance}"/> <c:out value="${a.currency}"/></td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/accounts/${a.id}" class="btn btn-sm btn-outline-primary" title="View"><i class="bi bi-eye"></i></a>
                                    <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
                                    <a href="${pageContext.request.contextPath}/accounts/${a.id}/edit" class="btn btn-sm btn-outline-secondary" title="Edit"><i class="bi bi-pencil"></i></a>
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
                    <i class="bi bi-wallet2" style="font-size: 3rem;"></i>
                    <p class="mt-2">No accounts found.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
