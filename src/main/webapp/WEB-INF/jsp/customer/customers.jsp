<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-people"></i> Customers</h3>
    <a href="${pageContext.request.contextPath}/customers/create" class="btn btn-primary">
        <i class="bi bi-plus-circle"></i> Add Customer
    </a>
</div>

<div class="card shadow mb-4">
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/customers" class="row g-3">
            <div class="col-md-5">
                <input type="text" name="search" class="form-control" placeholder="Search by name..." value="${search}">
            </div>
            <div class="col-md-4">
                <select name="kycStatus" class="form-select">
                    <option value="">All KYC Status</option>
                    <option value="PENDING" ${selectedKycStatus == 'PENDING' ? 'selected' : ''}>PENDING</option>
                    <option value="VERIFIED" ${selectedKycStatus == 'VERIFIED' ? 'selected' : ''}>VERIFIED</option>
                    <option value="REJECTED" ${selectedKycStatus == 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                </select>
            </div>
            <div class="col-md-3">
                <button type="submit" class="btn btn-outline-primary w-100"><i class="bi bi-search"></i> Filter</button>
            </div>
        </form>
    </div>
</div>

<div class="card shadow">
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>National ID</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>KYC Status</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="customer" items="${customers}">
                    <tr>
                        <td>${customer.customerId}</td>
                        <td>${customer.firstName} ${customer.lastName}</td>
                        <td><code>${customer.nationalId}</code></td>
                        <td>${customer.email}</td>
                        <td>${customer.phone}</td>
                        <td>
                            <c:choose>
                                <c:when test="${customer.kycStatus == 'VERIFIED'}"><span class="badge bg-success">VERIFIED</span></c:when>
                                <c:when test="${customer.kycStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                                <c:when test="${customer.kycStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                                <c:otherwise><span class="badge bg-secondary">${customer.kycStatus}</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <a href="${pageContext.request.contextPath}/customers/${customer.customerId}" class="btn btn-sm btn-outline-primary">
                                <i class="bi bi-eye"></i>
                            </a>
                            <a href="${pageContext.request.contextPath}/customers/${customer.customerId}/edit" class="btn btn-sm btn-outline-secondary">
                                <i class="bi bi-pencil"></i>
                            </a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty customers}">
                    <tr><td colspan="7" class="text-center text-muted py-4">No customers found</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
