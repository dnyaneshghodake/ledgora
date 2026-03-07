<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-person"></i> Customer Details</h3>
    <div>
        <a href="${pageContext.request.contextPath}/customers/${customer.customerId}/edit" class="btn btn-outline-secondary">
            <i class="bi bi-pencil"></i> Edit
        </a>
        <a href="${pageContext.request.contextPath}/customers" class="btn btn-outline-primary">
            <i class="bi bi-arrow-left"></i> Back
        </a>
    </div>
</div>

<div class="card shadow">
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-6"><strong>Customer ID:</strong> ${customer.customerId}</div>
            <div class="col-md-6"><strong>Name:</strong> ${customer.firstName} ${customer.lastName}</div>
            <div class="col-md-6"><strong>National ID:</strong> <code>${customer.nationalId}</code></div>
            <div class="col-md-6"><strong>Date of Birth:</strong> ${customer.dob}</div>
            <div class="col-md-6"><strong>Email:</strong> ${customer.email}</div>
            <div class="col-md-6"><strong>Phone:</strong> ${customer.phone}</div>
            <div class="col-12"><strong>Address:</strong> ${customer.address}</div>
            <div class="col-md-6">
                <strong>KYC Status:</strong>
                <c:choose>
                    <c:when test="${customer.kycStatus == 'VERIFIED'}"><span class="badge bg-success">VERIFIED</span></c:when>
                    <c:when test="${customer.kycStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                    <c:when test="${customer.kycStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                    <c:otherwise><span class="badge bg-secondary">${customer.kycStatus}</span></c:otherwise>
                </c:choose>
            </div>
            <div class="col-md-6"><strong>Created:</strong> ${customer.createdAt}</div>
        </div>
        <hr>
        <h5>Update KYC Status</h5>
        <form method="post" action="${pageContext.request.contextPath}/customers/${customer.customerId}/kyc" class="row g-2">
            <div class="col-md-4">
                <select name="kycStatus" class="form-select">
                    <option value="PENDING">PENDING</option>
                    <option value="VERIFIED">VERIFIED</option>
                    <option value="REJECTED">REJECTED</option>
                </select>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-primary">Update</button>
            </div>
        </form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
